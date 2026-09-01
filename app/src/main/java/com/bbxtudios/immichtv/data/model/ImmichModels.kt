package com.bbxtudios.immichtv.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MetadataSearchRequest(
    val originalPath: String? = null,
    val albumIds: List<String>? = null,
    val withExif: Boolean = true,
    val size: Int = 5000,
    val page: Int = 1
)

@Serializable
data class RandomSearchRequest(
    val size: Int = 200,
    val withExif: Boolean = true
)

@Serializable
data class SearchResponse(
    val assets: AssetSearchWrapper
)

@Serializable
data class AssetSearchWrapper(
    val items: List<AssetResponse> = emptyList(),
    val total: Int = 0,
    val count: Int = 0
)

@Serializable
data class AssetResponse(
    val id: String,
    val originalFileName: String? = null,
    val originalPath: String? = null,
    val type: String? = "IMAGE", // "IMAGE", "VIDEO"
    val duration: kotlinx.serialization.json.JsonElement? = null,
    val fileCreatedAt: String? = null,
    val localDateTime: String? = null,
    val createdAt: String? = null,
    val exifInfo: ExifInfo? = null,
    val width: Int? = null,
    val height: Int? = null,
    val fps: Double? = null
)

@Serializable
data class ExifInfo(
    val make: String? = null,
    val model: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val dateTimeOriginal: String? = null,
    val fNumber: Double? = null,
    val focalLength: Double? = null,
    val iso: Int? = null,
    val exposureTime: String? = null,
    val lensModel: String? = null,
    val exifImageWidth: Int? = null,
    val exifImageHeight: Int? = null,
    val fps: Double? = null
)

@Serializable
data class MemoryItemResponse(
    val id: String,
    val memoryAt: String? = null,
    val createdAt: String? = null,
    val assets: List<AssetResponse> = emptyList()
)

@Serializable
data class AlbumResponse(
    val id: String,
    val albumName: String = "",
    val description: String? = null,
    val assetCount: Int = 0,
    val albumThumbnailAssetId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val assets: List<AssetResponse> = emptyList()
)

@Serializable
data class ServerPingResponse(
    val res: String? = null
)

// --- Modelos de Dominio para la UI ---

enum class NavTab {
    FOLDERS,
    ALBUMS,
    RANDOM,
    MEMORIES,
    SETTINGS
}

@Immutable
data class FolderItem(
    val name: String,
    val originalPath: String
)

@Immutable
data class ViewAsset(
    val id: String,
    val name: String,
    val originalPath: String,
    val isVideo: Boolean,
    val url: String,
    val thumbnailUrl: String,
    val fullsizeUrl: String? = null,
    val videoPlaybackUrl: String? = null,
    val durationText: String? = null,
    val createdAt: String? = null,
    val exifMake: String? = null,
    val exifModel: String? = null,
    val exifCity: String? = null,
    val exifCountry: String? = null,
    val exifFNumber: Double? = null,
    val exifFocalLength: Double? = null,
    val exifIso: Int? = null,
    val exifExposureTime: String? = null,
    val exifLensModel: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val fps: Double? = null
)

fun formatVideoSpecs(width: Int?, height: Int?, fps: Double?): String {
    val parts = mutableListOf<String>()

    if (width != null && height != null && width > 0 && height > 0) {
        val maxDim = maxOf(width, height)
        val minDim = minOf(width, height)

        val standardTag = when {
            maxDim >= 7000 || minDim >= 4000 -> "8K"
            maxDim in 3600..4300 || minDim in 2000..2300 -> "4K"
            maxDim in 2400..2800 || minDim in 1350..1600 -> "2K"
            maxDim in 1800..2100 || minDim in 1000..1150 -> "FHD"
            maxDim in 1200..1350 || minDim in 700..750 -> "HD"
            maxDim in 640..854 || minDim in 450..500 -> "SD"
            else -> null
        }

        val resText = if (standardTag != null) {
            "${width}x${height} ($standardTag)"
        } else {
            "${width}x${height}"
        }
        parts.add(resText)
    }

    if (fps != null && fps > 0.0) {
        val fpsText = if (fps % 1.0 == 0.0) {
            String.format(java.util.Locale.US, "%.0f fps", fps)
        } else {
            String.format(java.util.Locale.US, "%.2f fps", fps).replace(".00 fps", " fps")
        }
        parts.add(fpsText)
    }

    return parts.joinToString("   •   ")
}

@Immutable
data class DateGroup(
    val dateKey: String, // "YYYY-MM-DD"
    val formattedTitle: String,
    val assets: List<ViewAsset>
)

@Immutable
data class FolderContent(
    val folders: List<FolderItem>,
    val files: List<ViewAsset>,
    val dateGroups: List<DateGroup>
)

@Immutable
data class AlbumItem(
    val id: String,
    val name: String,
    val description: String? = null,
    val assetCount: Int = 0,
    val thumbnailUrl: String? = null
)

@Immutable
data class AlbumDetail(
    val id: String,
    val name: String,
    val description: String? = null,
    val assetCount: Int = 0,
    val dateGroups: List<DateGroup> = emptyList(),
    val allAssets: List<ViewAsset> = emptyList()
)

@Immutable
data class MemoryGroup(
    val year: Int,
    val title: String,
    val assets: List<ViewAsset>
)

/**
 * Elemento unificado para el LazyVerticalGrid de FoldersScreen.
 * Al tener una sola lista homogénea de GridItem, Compose mantiene la
 * coordenada X de foco de forma perfecta al navegar verticalmente con el D-Pad.
 * El índice de asset se precalcula una vez para evitar O(n) indexOf en cada composición.
 */
@Immutable
sealed class GridItem {
    @Immutable
    data class Folder(val item: FolderItem) : GridItem()
    @Immutable
    data class DateHeader(val title: String, val dateKey: String) : GridItem()
    @Immutable
    data class Asset(val item: ViewAsset, val indexInFiles: Int) : GridItem()
}
