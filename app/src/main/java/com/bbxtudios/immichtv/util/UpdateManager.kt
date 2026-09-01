package com.bbxtudios.immichtv.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

sealed class UpdateState {
    object Idle : UpdateState()
    data class Downloading(val progress: Int) : UpdateState()
    object Installing : UpdateState()
    data class Success(val message: String) : UpdateState()
    data class Error(val error: String) : UpdateState()
}

data class GitHubReleaseInfo(
    val tagName: String,
    val name: String,
    val body: String,
    val apkDownloadUrl: String?,
    val publishedAt: String
)

class UpdateManager(private val context: Context) {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    companion object {
        const val DEFAULT_UPDATE_URL = "https://toolphin.com/app/immich-tv.apk"
        const val GITHUB_RELEASES_API = "https://api.github.com/repos/bbxtudios/Immich-Folders-for-TV/releases"
    }

    /**
     * Consulta la última versión publicada en GitHub Releases y comprueba si es más reciente que la instalada
     */
    suspend fun checkForGitHubUpdate(currentVersionName: String): Pair<Boolean, GitHubReleaseInfo?> = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_RELEASES_API)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 15000
            connection.setRequestProperty("User-Agent", "Immich-TV-App")
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                return@withContext false to null
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val jsonString = reader.use { it.readText() }
            connection.disconnect()

            val releasesArray = org.json.JSONArray(jsonString)
            if (releasesArray.length() == 0) {
                return@withContext false to null
            }

            val json = releasesArray.getJSONObject(0)
            val tagName = json.optString("tag_name", "")
            val name = json.optString("name", tagName)
            val body = json.optString("body", "")
            val publishedAt = json.optString("published_at", "")

            var apkUrl: String? = null
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url").ifBlank { null }
                            ?: asset.optString("url").ifBlank { null }
                        break
                    }
                }
            }

            val releaseInfo = GitHubReleaseInfo(
                tagName = tagName,
                name = name,
                body = body,
                apkDownloadUrl = apkUrl,
                publishedAt = publishedAt
            )

            val isNewer = isNewerVersion(tagName, currentVersionName)
            isNewer to releaseInfo
        } catch (e: Exception) {
            e.printStackTrace()
            false to null
        }
    }

    /**
     * Compara semánticamente dos versiones (ej. "v1.0.26" vs "1.0.0")
     */
    fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
        try {
            val cleanRemote = remoteTag.trim().removePrefix("v").removePrefix("V")
            val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V")

            val remoteParts = cleanRemote.split(".").mapNotNull { it.takeWhile { char -> char.isDigit() }.toIntOrNull() }
            val currentParts = cleanCurrent.split(".").mapNotNull { it.takeWhile { char -> char.isDigit() }.toIntOrNull() }

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        } catch (_: Exception) {
            return false
        }
    }

    suspend fun downloadAndInstall(apkUrl: String = DEFAULT_UPDATE_URL) = withContext(Dispatchers.IO) {
        try {
            _updateState.value = UpdateState.Downloading(0)

            var currentUrl = apkUrl
            var connection: HttpURLConnection? = null
            var redirectCount = 0
            val maxRedirects = 7

            while (redirectCount < maxRedirects) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "Immich-TV-App")
                    setRequestProperty("Accept", "*/*")
                }
                connection.connect()

                val code = connection.responseCode
                if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                    code == HttpURLConnection.HTTP_MOVED_TEMP ||
                    code == HttpURLConnection.HTTP_SEE_OTHER ||
                    code == 307 || code == 308
                ) {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (!location.isNullOrBlank()) {
                        currentUrl = if (location.startsWith("http")) location else URL(URL(currentUrl), location).toString()
                        redirectCount++
                        continue
                    }
                }
                break
            }

            val finalConn = connection ?: throw Exception("No se pudo establecer conexión")
            val responseCode = finalConn.responseCode
            if (responseCode !in 200..299) {
                _updateState.value = UpdateState.Error("Error de descarga (HTTP $responseCode)")
                finalConn.disconnect()
                return@withContext
            }

            val fileLength = finalConn.contentLength
            val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(updateDir, "immich-tv.apk")

            if (apkFile.exists()) {
                apkFile.delete()
            }

            val input: InputStream = finalConn.inputStream
            val output = FileOutputStream(apkFile)

            val buffer = ByteArray(16384)
            var total: Long = 0
            var count: Int
            var lastProgress = 0

            while (input.read(buffer).also { count = it } != -1) {
                total += count
                if (fileLength > 0) {
                    val progress = ((total * 100) / fileLength).toInt().coerceIn(0, 100)
                    if (progress != lastProgress) {
                        lastProgress = progress
                        _updateState.value = UpdateState.Downloading(progress)
                    }
                }
                output.write(buffer, 0, count)
            }

            output.flush()
            output.close()
            input.close()
            finalConn.disconnect()

            _updateState.value = UpdateState.Installing

            withContext(Dispatchers.Main) {
                installApk(apkFile)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            _updateState.value = UpdateState.Error(e.message ?: "Error al descargar la actualización")
        }
    }

    fun installApk(apkFile: File) {
        try {
            if (!apkFile.exists()) {
                _updateState.value = UpdateState.Error("El archivo de actualización no existe")
                return
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            // Otorgar permisos de lectura a los gestores de instalación de paquetes
            val resInfoList = context.packageManager.queryIntentActivities(installIntent, 0)
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(packageName, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // En Android 8+ si no tiene permiso, abrir la pantalla de permiso y luego intentar lanzar
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val manageIntent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(manageIntent)
                    _updateState.value = UpdateState.Success("Concede el permiso para continuar la instalación")
                    return
                }
            }

            context.startActivity(installIntent)
            _updateState.value = UpdateState.Success("Instalador iniciado")
        } catch (e: Exception) {
            e.printStackTrace()
            _updateState.value = UpdateState.Error("Error al iniciar instalador: ${e.message}")
        }
    }

    fun resetState() {
        _updateState.value = UpdateState.Idle
    }
}
