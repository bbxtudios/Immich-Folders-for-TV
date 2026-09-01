package com.bbxtudios.immichtv.data.repository

import com.bbxtudios.immichtv.data.api.ImmichApiService
import com.bbxtudios.immichtv.data.api.NetworkModule
import com.bbxtudios.immichtv.data.model.AlbumDetail
import com.bbxtudios.immichtv.data.model.AlbumItem
import com.bbxtudios.immichtv.data.model.AlbumResponse
import com.bbxtudios.immichtv.data.model.AssetResponse
import com.bbxtudios.immichtv.data.model.DateGroup
import com.bbxtudios.immichtv.data.model.FolderContent
import com.bbxtudios.immichtv.data.model.FolderItem
import com.bbxtudios.immichtv.data.model.MemoryGroup
import com.bbxtudios.immichtv.data.model.MetadataSearchRequest
import com.bbxtudios.immichtv.data.model.RandomSearchRequest
import com.bbxtudios.immichtv.data.model.ViewAsset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ImmichRepository(
    private val settingsRepository: SettingsRepository
) {
    private var cachedPaths: List<String>? = null

    // Caché de carpetas ya visitadas: clave = "path|groupOrder|assetOrder"
    private val folderCache = mutableMapOf<String, FolderContent>()

    private suspend fun getUrlAndKey(): Pair<String, String> {
        val url = settingsRepository.serverUrl.first().trimEnd('/')
        val key = settingsRepository.apiKey.first()
        return Pair(url, key)
    }

    private suspend fun getService(): ImmichApiService {
        val (url, key) = getUrlAndKey()
        return NetworkModule.getApiService(url, key)
    }

    fun invalidateService() {
        cachedPaths = null
        folderCache.clear()
    }

    fun clearCache() {
        cachedPaths = null
        folderCache.clear()
    }

    /**
     * Devuelve un Flow<FolderContent> que emite progresivamente:
     * - Primer emit: carpetas + fotos de la página 1 (usuario ve contenido al instante)
     * - Siguientes emits: fotos acumuladas de páginas 2 y 3
     * - Si la carpeta está en caché, emite al instante sin red
     */
    fun getFolderContent(
        path: String,
        groupOrder: String = "desc",
        assetOrder: String = "desc"
    ): Flow<FolderContent> = flow {
        val cacheKey = "$path|$groupOrder|$assetOrder"

        // Si tenemos caché, emitimos al instante y terminamos
        folderCache[cacheKey]?.let { cached ->
            emit(cached)
            return@flow
        }

        val service = getService()
        val (baseUrl, apiKey) = getUrlAndKey()

        // 1. Obtener lista completa de rutas únicas (Caché local)
        if (cachedPaths == null) {
            cachedPaths = try {
                service.getUniquePaths()
            } catch (e: Exception) {
                emptyList()
            }
        }
        val allPaths = cachedPaths.orEmpty()

        // 2. Caso Raíz: path == "" o path == "/"
        if (path == "" || path == "/") {
            val topLevelFolders = allPaths
                .mapNotNull { p ->
                    val parts = p.split('/').filter { it.isNotBlank() }
                    parts.firstOrNull()
                }
                .distinct()
                .sorted()
                .map { name -> FolderItem(name = name, originalPath = "/$name") }
            val content = FolderContent(folders = topLevelFolders, files = emptyList(), dateGroups = emptyList())
            folderCache[cacheKey] = content
            emit(content)
            return@flow
        }

        // 3. Subcarpetas: cálculo exacto por profundidad
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        val currentDepth = normalizedPath.split('/').filter { it.isNotBlank() }.size
        val subFoldersSet = mutableSetOf<String>()

        for (fullPath in allPaths) {
            val normalizedFullPath = if (fullPath.startsWith("/")) fullPath else "/$fullPath"
            if (normalizedFullPath.startsWith("$normalizedPath/")) {
                val parts = normalizedFullPath.split('/').filter { it.isNotBlank() }
                if (parts.size > currentDepth) {
                    subFoldersSet.add(parts[currentDepth])
                }
            }
        }

        val folders = subFoldersSet.sorted().map { name ->
            FolderItem(name = name, originalPath = "$normalizedPath/$name")
        }

        // 4. Carga progresiva: emitir tras cada página
        val apiPath = if (path.startsWith("/")) path.substring(1) else path
        val allAssets = mutableListOf<AssetResponse>()
        val seenIds = mutableSetOf<String>()
        val normalizedTargetFolder = path.trim('/').lowercase()
        var lastContent: FolderContent? = null

        for (currentPage in 1..3) {
            val response = try {
                service.searchMetadata(
                    MetadataSearchRequest(
                        originalPath = apiPath,
                        withExif = true,
                        size = 1000,
                        page = currentPage
                    )
                )
            } catch (e: Exception) {
                null
            }

            if (response == null || response.assets.items.isEmpty()) break

            val items = response.assets.items
            var newItemsCount = 0
            for (item in items) {
                if (seenIds.add(item.id)) {
                    allAssets.add(item)
                    newItemsCount++
                }
            }

            if (newItemsCount == 0) break

            // Emitir resultado parcial: el usuario ve las fotos al instante
            val content = buildFolderContent(
                folders, allAssets, normalizedTargetFolder,
                baseUrl, apiKey, groupOrder, assetOrder
            )
            lastContent = content
            emit(content)

            // Parar si ya tenemos todos los items o la página vino incompleta (última página)
            if (response.assets.total > 0 && allAssets.size >= response.assets.total) break
            if (items.size < 250) break
        }

        // Guardar resultado final en caché para acceso instantáneo si vuelven a esta carpeta
        lastContent?.let { folderCache[cacheKey] = it }
    }

    /**
     * Construye un FolderContent a partir de la lista acumulada de assets.
     * Se llama tras cada página para permitir la carga progresiva.
     */
    private fun buildFolderContent(
        folders: List<FolderItem>,
        allAssets: List<AssetResponse>,
        normalizedTargetFolder: String,
        baseUrl: String,
        apiKey: String,
        groupOrder: String,
        assetOrder: String
    ): FolderContent {
        val filesInThisFolder = mutableListOf<ViewAsset>()

        for (asset in allAssets) {
            val assetPath = asset.originalPath ?: ""
            val lastSlash = assetPath.lastIndexOf('/')
            val assetFolder = if (lastSlash >= 0) assetPath.substring(0, lastSlash).trim('/').lowercase() else ""

            if (assetFolder == normalizedTargetFolder || (normalizedTargetFolder.isEmpty() && lastSlash < 0)) {
                val isVideo = asset.type == "VIDEO"
                val rawPath = assetPath
                val fileName = asset.originalFileName ?: if (lastSlash >= 0) rawPath.substring(lastSlash + 1) else "Foto_${asset.id.take(8)}"
                filesInThisFolder.add(
                    ViewAsset(
                        id = asset.id,
                        name = if (lastSlash >= 0) rawPath.substring(lastSlash + 1) else fileName,
                        originalPath = rawPath,
                        isVideo = isVideo,
                        url = if (isVideo) "$baseUrl/api/assets/${asset.id}/original?apiKey=$apiKey" else "$baseUrl/api/assets/${asset.id}/thumbnail?size=preview&apiKey=$apiKey",
                        thumbnailUrl = "$baseUrl/api/assets/${asset.id}/thumbnail?size=thumbnail&apiKey=$apiKey",
                        fullsizeUrl = if (isVideo) null else "$baseUrl/api/assets/${asset.id}/thumbnail?size=fullsize&apiKey=$apiKey",
                        videoPlaybackUrl = if (isVideo) "$baseUrl/api/assets/${asset.id}/video/playback?apiKey=$apiKey" else null,
                        durationText = if (isVideo) formatVideoDuration(asset.duration) else null,
                        createdAt = asset.fileCreatedAt ?: asset.localDateTime ?: asset.createdAt ?: asset.exifInfo?.dateTimeOriginal,
                        exifMake = asset.exifInfo?.make,
                        exifModel = asset.exifInfo?.model,
                        exifCity = asset.exifInfo?.city,
                        exifCountry = asset.exifInfo?.country,
                        exifFNumber = asset.exifInfo?.fNumber,
                        exifFocalLength = asset.exifInfo?.focalLength,
                        exifIso = asset.exifInfo?.iso,
                        exifExposureTime = asset.exifInfo?.exposureTime,
                        exifLensModel = asset.exifInfo?.lensModel,
                        width = asset.exifInfo?.exifImageWidth ?: asset.width,
                        height = asset.exifInfo?.exifImageHeight ?: asset.height,
                        fps = asset.exifInfo?.fps ?: asset.fps
                    )
                )
            }
        }

        val sortedFiles = if (assetOrder == "asc") {
            filesInThisFolder.sortedBy { it.createdAt ?: "" }
        } else {
            filesInThisFolder.sortedByDescending { it.createdAt ?: "" }
        }

        val groupedMap = linkedMapOf<String, MutableList<ViewAsset>>()
        for (file in sortedFiles) {
            val groupKey = extractDateGroupKey(file.createdAt)
            groupedMap.getOrPut(groupKey) { mutableListOf() }.add(file)
        }

        val sortedGroupKeys = if (groupOrder == "asc") {
            groupedMap.keys.sorted()
        } else {
            groupedMap.keys.sortedDescending()
        }

        val dateGroups = sortedGroupKeys.map { key ->
            DateGroup(
                dateKey = key,
                formattedTitle = formatDateGroupTitle(key),
                assets = groupedMap[key] ?: emptyList()
            )
        }

        return FolderContent(folders = folders, files = sortedFiles, dateGroups = dateGroups)
    }

    suspend fun getAssetDetail(id: String): ViewAsset? {
        return try {
            val service = getService()
            val (baseUrl, apiKey) = getUrlAndKey()
            val asset = service.getAssetDetail(id)
            val isVideo = asset.type == "VIDEO"
            val rawPath = asset.originalPath ?: ""
            val lastSlash = rawPath.lastIndexOf('/')
            val fileName = asset.originalFileName ?: if (lastSlash >= 0) rawPath.substring(lastSlash + 1) else "Foto_${asset.id.take(8)}"
            ViewAsset(
                id = asset.id,
                name = if (lastSlash >= 0) rawPath.substring(lastSlash + 1) else fileName,
                originalPath = rawPath,
                isVideo = isVideo,
                url = if (isVideo) "$baseUrl/api/assets/${asset.id}/original?apiKey=$apiKey" else "$baseUrl/api/assets/${asset.id}/thumbnail?size=preview&apiKey=$apiKey",
                thumbnailUrl = "$baseUrl/api/assets/${asset.id}/thumbnail?size=thumbnail&apiKey=$apiKey",
                fullsizeUrl = if (isVideo) null else "$baseUrl/api/assets/${asset.id}/thumbnail?size=fullsize&apiKey=$apiKey",
                videoPlaybackUrl = if (isVideo) "$baseUrl/api/assets/${asset.id}/video/playback?apiKey=$apiKey" else null,
                durationText = if (isVideo) formatVideoDuration(asset.duration) else null,
                createdAt = asset.fileCreatedAt ?: asset.localDateTime ?: asset.createdAt ?: asset.exifInfo?.dateTimeOriginal,
                exifMake = asset.exifInfo?.make,
                exifModel = asset.exifInfo?.model,
                exifCity = asset.exifInfo?.city,
                exifCountry = asset.exifInfo?.country,
                exifFNumber = asset.exifInfo?.fNumber,
                exifFocalLength = asset.exifInfo?.focalLength,
                exifIso = asset.exifInfo?.iso,
                exifExposureTime = asset.exifInfo?.exposureTime,
                exifLensModel = asset.exifInfo?.lensModel,
                width = asset.exifInfo?.exifImageWidth ?: asset.width,
                height = asset.exifInfo?.exifImageHeight ?: asset.height,
                fps = asset.exifInfo?.fps ?: asset.fps
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getRandomAssets(count: Int = 1500): List<ViewAsset> {
        val service = getService()
        val (baseUrl, apiKey) = getUrlAndKey()

        // Soporte Dual: Intentar primero v3 (POST /search/random) y si falla, fallback a v2 (GET /assets/random)
        val assets = try {
            service.searchRandom(RandomSearchRequest(size = count, withExif = true))
        } catch (e: Exception) {
            try {
                service.getRandomAssets(count)
            } catch (_: Exception) {
                emptyList()
            }
        }

        return assets.map { asset ->
            val isVideo = asset.type == "VIDEO"
            val rawPath = asset.originalPath ?: ""
            val lastSlash = rawPath.lastIndexOf('/')
            val fileName = asset.originalFileName ?: if (lastSlash >= 0) rawPath.substring(lastSlash + 1) else "Foto_${asset.id.take(8)}"
            ViewAsset(
                id = asset.id,
                name = if (lastSlash >= 0) rawPath.substring(lastSlash + 1) else fileName,
                originalPath = rawPath,
                isVideo = isVideo,
                url = if (isVideo) "$baseUrl/api/assets/${asset.id}/original?apiKey=$apiKey" else "$baseUrl/api/assets/${asset.id}/thumbnail?size=preview&apiKey=$apiKey",
                thumbnailUrl = "$baseUrl/api/assets/${asset.id}/thumbnail?size=thumbnail&apiKey=$apiKey",
                fullsizeUrl = if (isVideo) null else "$baseUrl/api/assets/${asset.id}/thumbnail?size=fullsize&apiKey=$apiKey",
                videoPlaybackUrl = if (isVideo) "$baseUrl/api/assets/${asset.id}/video/playback?apiKey=$apiKey" else null,
                durationText = if (isVideo) formatVideoDuration(asset.duration) else null,
                createdAt = asset.fileCreatedAt ?: asset.localDateTime ?: asset.createdAt ?: asset.exifInfo?.dateTimeOriginal,
                exifMake = asset.exifInfo?.make,
                exifModel = asset.exifInfo?.model,
                exifCity = asset.exifInfo?.city,
                exifCountry = asset.exifInfo?.country,
                exifFNumber = asset.exifInfo?.fNumber,
                exifFocalLength = asset.exifInfo?.focalLength,
                exifIso = asset.exifInfo?.iso,
                exifExposureTime = asset.exifInfo?.exposureTime,
                exifLensModel = asset.exifInfo?.lensModel,
                width = asset.exifInfo?.exifImageWidth ?: asset.width,
                height = asset.exifInfo?.exifImageHeight ?: asset.height,
                fps = asset.exifInfo?.fps ?: asset.fps
            )
        }
    }

    suspend fun getMemories(): List<MemoryGroup> {
        val service = getService()
        val (baseUrl, apiKey) = getUrlAndKey()

        val today = Calendar.getInstance()
        val currentMonth = today.get(Calendar.MONTH)
        val currentDay = today.get(Calendar.DAY_OF_MONTH)
        val currentYear = today.get(Calendar.YEAR)

        val memoriesList = try {
            service.getMemories()
        } catch (e: Exception) {
            emptyList()
        }

        if (memoriesList.isEmpty()) {
            return emptyList()
        }

        val todayMemories = memoriesList.filter { memory ->
            val dateStr = memory.memoryAt ?: memory.createdAt
            if (!dateStr.isNullOrBlank() && dateStr.length >= 10) {
                try {
                    val parts = dateStr.substring(0, 10).split('-')
                    if (parts.size == 3) {
                        val m = (parts[1].toIntOrNull() ?: -1) - 1
                        val d = parts[2].toIntOrNull() ?: -1
                        m == currentMonth && d == currentDay
                    } else true
                } catch (_: Exception) {
                    true
                }
            } else true
        }

        if (todayMemories.isEmpty()) {
            return emptyList()
        }

        val allAssets = mutableListOf<AssetResponse>()
        for (memory in todayMemories) {
            try {
                val detail = service.getMemoryDetail(memory.id)
                allAssets.addAll(detail.assets)
            } catch (_: Exception) {}
        }

        val byYear = allAssets.groupBy { asset ->
            val dateStr = asset.fileCreatedAt ?: asset.localDateTime ?: asset.createdAt
            dateStr?.substringBefore('-')?.toIntOrNull() ?: currentYear
        }

        return byYear.entries
            .sortedByDescending { it.key }
            .map { (year, assetsList) ->
                val yearsAgo = currentYear - year
                val title = when (yearsAgo) {
                    0 -> "Este año"
                    1 -> "Tal día como hoy hace 1 año"
                    else -> "Tal día como hoy hace $yearsAgo años"
                }

                val viewAssets = assetsList.map { asset ->
                    val isVideo = asset.type == "VIDEO"
                    val rawPath = asset.originalPath ?: ""
                    val lastSlash = rawPath.lastIndexOf('/')
                    val fileName = asset.originalFileName ?: if (lastSlash >= 0) rawPath.substring(lastSlash + 1) else "Foto_${asset.id.take(8)}"
                    ViewAsset(
                        id = asset.id,
                        name = if (lastSlash >= 0) rawPath.substring(lastSlash + 1) else fileName,
                        originalPath = rawPath,
                        isVideo = isVideo,
                        url = if (isVideo) "$baseUrl/api/assets/${asset.id}/original?apiKey=$apiKey" else "$baseUrl/api/assets/${asset.id}/thumbnail?size=preview&apiKey=$apiKey",
                        thumbnailUrl = "$baseUrl/api/assets/${asset.id}/thumbnail?size=thumbnail&apiKey=$apiKey",
                        fullsizeUrl = if (isVideo) null else "$baseUrl/api/assets/${asset.id}/thumbnail?size=fullsize&apiKey=$apiKey",
                        videoPlaybackUrl = if (isVideo) "$baseUrl/api/assets/${asset.id}/video/playback?apiKey=$apiKey" else null,
                        durationText = if (isVideo) formatVideoDuration(asset.duration) else null,
                        createdAt = asset.fileCreatedAt ?: asset.localDateTime ?: asset.createdAt ?: asset.exifInfo?.dateTimeOriginal,
                        exifMake = asset.exifInfo?.make,
                        exifModel = asset.exifInfo?.model,
                        exifCity = asset.exifInfo?.city,
                        exifCountry = asset.exifInfo?.country,
                        exifFNumber = asset.exifInfo?.fNumber,
                        exifFocalLength = asset.exifInfo?.focalLength,
                        exifIso = asset.exifInfo?.iso,
                        exifExposureTime = asset.exifInfo?.exposureTime,
                        exifLensModel = asset.exifInfo?.lensModel,
                        width = asset.exifInfo?.exifImageWidth ?: asset.width,
                        height = asset.exifInfo?.exifImageHeight ?: asset.height,
                        fps = asset.exifInfo?.fps ?: asset.fps
                    )
                }

                MemoryGroup(
                    year = year,
                    title = title,
                    assets = viewAssets
                )
            }
    }

    suspend fun getAlbums(): List<AlbumItem> {
        val service = getService()
        val (baseUrl, apiKey) = getUrlAndKey()

        val albums = try {
            service.getAlbums()
        } catch (_: Exception) {
            emptyList()
        }

        return albums.map { album ->
            val thumbUrl = if (!album.albumThumbnailAssetId.isNullOrBlank()) {
                "$baseUrl/api/assets/${album.albumThumbnailAssetId}/thumbnail?size=thumbnail&apiKey=$apiKey"
            } else if (album.assets.isNotEmpty()) {
                "$baseUrl/api/assets/${album.assets.first().id}/thumbnail?size=thumbnail&apiKey=$apiKey"
            } else null

            AlbumItem(
                id = album.id,
                name = album.albumName.ifBlank { "Álbum sin título" },
                description = album.description,
                assetCount = album.assetCount.coerceAtLeast(album.assets.size),
                thumbnailUrl = thumbUrl
            )
        }.sortedBy { it.name.lowercase() }
    }

    suspend fun getAlbumDetail(albumId: String, groupOrder: String = "desc", assetOrder: String = "desc"): AlbumDetail {
        val service = getService()
        val (baseUrl, apiKey) = getUrlAndKey()

        val album = try {
            service.getAlbumDetail(albumId, withoutAssets = false)
        } catch (e: Exception) {
            AlbumResponse(id = albumId)
        }

        // Si el endpoint de álbum no devuelve assets (Immich v1.106+ o v2.0+), buscamos por albumIds
        val rawAssets: List<AssetResponse> = if (album.assets.isNotEmpty()) {
            album.assets
        } else {
            try {
                val searchRes = service.searchMetadata(
                    MetadataSearchRequest(
                        albumIds = listOf(albumId),
                        withExif = true,
                        size = 5000
                    )
                )
                searchRes.assets.items
            } catch (_: Exception) {
                emptyList()
            }
        }

        val filesInAlbum = rawAssets.map { asset ->
            val isVideo = asset.type == "VIDEO"
            val rawPath = asset.originalPath ?: ""
            val lastSlash = rawPath.lastIndexOf('/')
            val fileName = asset.originalFileName ?: if (lastSlash >= 0) rawPath.substring(lastSlash + 1) else "Foto_${asset.id.take(8)}"
            ViewAsset(
                id = asset.id,
                name = if (lastSlash >= 0) rawPath.substring(lastSlash + 1) else fileName,
                originalPath = rawPath,
                isVideo = isVideo,
                url = if (isVideo) "$baseUrl/api/assets/${asset.id}/original?apiKey=$apiKey" else "$baseUrl/api/assets/${asset.id}/thumbnail?size=preview&apiKey=$apiKey",
                thumbnailUrl = "$baseUrl/api/assets/${asset.id}/thumbnail?size=thumbnail&apiKey=$apiKey",
                fullsizeUrl = if (isVideo) null else "$baseUrl/api/assets/${asset.id}/thumbnail?size=fullsize&apiKey=$apiKey",
                videoPlaybackUrl = if (isVideo) "$baseUrl/api/assets/${asset.id}/video/playback?apiKey=$apiKey" else null,
                durationText = if (isVideo) formatVideoDuration(asset.duration) else null,
                createdAt = asset.fileCreatedAt ?: asset.localDateTime ?: asset.createdAt ?: asset.exifInfo?.dateTimeOriginal,
                exifMake = asset.exifInfo?.make,
                exifModel = asset.exifInfo?.model,
                exifCity = asset.exifInfo?.city,
                exifCountry = asset.exifInfo?.country,
                exifFNumber = asset.exifInfo?.fNumber,
                exifFocalLength = asset.exifInfo?.focalLength,
                exifIso = asset.exifInfo?.iso,
                exifExposureTime = asset.exifInfo?.exposureTime,
                exifLensModel = asset.exifInfo?.lensModel,
                width = asset.exifInfo?.exifImageWidth ?: asset.width,
                height = asset.exifInfo?.exifImageHeight ?: asset.height,
                fps = asset.exifInfo?.fps ?: asset.fps
            )
        }

        val sortedFiles = if (assetOrder == "asc") {
            filesInAlbum.sortedBy { it.createdAt ?: "" }
        } else {
            filesInAlbum.sortedByDescending { it.createdAt ?: "" }
        }

        val groupedMap = linkedMapOf<String, MutableList<ViewAsset>>()
        for (file in sortedFiles) {
            val groupKey = extractDateGroupKey(file.createdAt)
            groupedMap.getOrPut(groupKey) { mutableListOf() }.add(file)
        }

        val sortedDateKeys = if (groupOrder == "asc") {
            groupedMap.keys.sortedWith(Comparator { a, b ->
                if (a == "Fecha desconocida") 1
                else if (b == "Fecha desconocida") -1
                else a.compareTo(b)
            })
        } else {
            groupedMap.keys.sortedWith(Comparator { a, b ->
                if (a == "Fecha desconocida") 1
                else if (b == "Fecha desconocida") -1
                else b.compareTo(a)
            })
        }

        val dateGroups = sortedDateKeys.map { key ->
            DateGroup(
                dateKey = key,
                formattedTitle = formatDateGroupTitle(key),
                assets = groupedMap[key] ?: emptyList()
            )
        }

        return AlbumDetail(
            id = album.id,
            name = album.albumName.ifBlank { "Álbum sin título" },
            description = album.description,
            assetCount = sortedFiles.size,
            dateGroups = dateGroups,
            allAssets = sortedFiles
        )
    }

    suspend fun testConnection(url: String, key: String): Result<Boolean> {
        return try {
            val service = NetworkModule.getApiService(url, key)
            val response = service.getServerVersion()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Error de conexión: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractDateGroupKey(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return "Fecha desconocida"
        return try {
            if (isoDate.length >= 10) isoDate.substring(0, 10) else "Fecha desconocida"
        } catch (e: Exception) {
            "Fecha desconocida"
        }
    }

    private val dateTitleCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    fun formatDateGroupTitle(key: String, locale: Locale = Locale.getDefault()): String {
        if (key.isBlank() || key == "Fecha desconocida") {
            return if (locale.language.startsWith("es")) "Fecha desconocida" else "Unknown date"
        }
        val cacheKey = "${locale.language}_$key"
        return dateTitleCache.getOrPut(cacheKey) {
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val date = inputFormat.parse(key)
                if (date != null) {
                    val pattern = if (locale.language.startsWith("es")) "EEEE, d 'de' MMMM 'de' yyyy" else "EEEE, MMMM d, yyyy"
                    val outputFormat = SimpleDateFormat(pattern, locale)
                    val formatted = outputFormat.format(date)
                    formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
                } else {
                    key
                }
            } catch (e: Exception) {
                key
            }
        }
    }

    private fun formatVideoDuration(durationJson: kotlinx.serialization.json.JsonElement?): String? {
        if (durationJson == null) return null
        return try {
            val content = durationJson.toString().trim('"', ' ')
            if (content.isEmpty() || content == "null") return null

            if (content.contains(':')) {
                // Formato string tipo "0:00:08.554" o "00:01:30" o "1:22:33"
                val clean = content.substringBefore('.')
                val parts = clean.split(':')
                if (parts.size == 3) {
                    val h = parts[0].toIntOrNull() ?: 0
                    val m = parts[1].toIntOrNull() ?: 0
                    val s = parts[2].toIntOrNull() ?: 0
                    return if (h > 0) {
                        String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
                    } else {
                        String.format(Locale.getDefault(), "%d:%02d", m, s)
                    }
                } else if (parts.size == 2) {
                    val m = parts[0].toIntOrNull() ?: 0
                    val s = parts[1].toIntOrNull() ?: 0
                    return String.format(Locale.getDefault(), "%d:%02d", m, s)
                }
            }

            // Formato numérico (segundos con decimales o milisegundos)
            val num = content.toDoubleOrNull() ?: return null
            if (num <= 0.0) return "0:00"

            val totalSeconds = when {
                content.contains('.') && num < 1000.0 -> Math.round(num)
                num >= 1000.0 -> Math.round(num / 1000.0)
                else -> Math.round(num)
            }

            val h = totalSeconds / 3600
            val m = (totalSeconds % 3600) / 60
            val s = totalSeconds % 60

            if (h > 0) {
                String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
            } else {
                String.format(Locale.getDefault(), "%d:%02d", m, s)
            }
        } catch (_: Exception) {
            null
        }
    }
}
