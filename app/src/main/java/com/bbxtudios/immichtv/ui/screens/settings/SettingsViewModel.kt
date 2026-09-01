package com.bbxtudios.immichtv.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import coil.Coil
import coil.imageLoader
import com.bbxtudios.immichtv.data.repository.ImmichRepository
import com.bbxtudios.immichtv.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class SettingsViewModel(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val immichRepository: ImmichRepository
) : ViewModel() {

    val serverUrl: StateFlow<String> = settingsRepository.serverUrl.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsRepository.DEFAULT_SERVER_URL
    )

    val apiKey: StateFlow<String> = settingsRepository.apiKey.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsRepository.DEFAULT_API_KEY
    )

    val groupOrder: StateFlow<String> = settingsRepository.groupOrder.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsRepository.DEFAULT_GROUP_ORDER
    )

    val assetOrder: StateFlow<String> = settingsRepository.assetOrder.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsRepository.DEFAULT_ASSET_ORDER
    )

    val slideshowInterval: StateFlow<Int> = settingsRepository.slideshowInterval.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsRepository.DEFAULT_SLIDESHOW_INTERVAL
    )

    val loopSlideshow: StateFlow<Boolean> = settingsRepository.loopSlideshow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsRepository.DEFAULT_LOOP_SLIDESHOW
    )

    val loopVideo: StateFlow<Boolean> = settingsRepository.loopVideo.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsRepository.DEFAULT_LOOP_VIDEO
    )

    val zoomLevel: StateFlow<Float> = settingsRepository.zoomLevel.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsRepository.DEFAULT_ZOOM_LEVEL
    )

    val showMetadata: StateFlow<Boolean> = settingsRepository.showMetadata.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsRepository.DEFAULT_SHOW_METADATA
    )

    val showCameraInfo: StateFlow<Boolean> = settingsRepository.showCameraInfo.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsRepository.DEFAULT_SHOW_CAMERA_INFO
    )

    val showExifDetails: StateFlow<Boolean> = settingsRepository.showExifDetails.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsRepository.DEFAULT_SHOW_EXIF_DETAILS
    )

    val showLocationInfo: StateFlow<Boolean> = settingsRepository.showLocationInfo.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsRepository.DEFAULT_SHOW_LOCATION_INFO
    )

    val showDateInfo: StateFlow<Boolean> = settingsRepository.showDateInfo.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsRepository.DEFAULT_SHOW_DATE_INFO
    )

    val showCounter: StateFlow<Boolean> = settingsRepository.showCounter.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsRepository.DEFAULT_SHOW_COUNTER
    )

    val showVideoSpecs: StateFlow<Boolean> = settingsRepository.showVideoSpecs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsRepository.DEFAULT_SHOW_VIDEO_SPECS
    )

    val preloading: StateFlow<Boolean> = settingsRepository.preloading.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsRepository.DEFAULT_PRELOADING
    )

    val animFocus: StateFlow<Boolean> = settingsRepository.animFocus.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsRepository.DEFAULT_ANIM_FOCUS
    )

    val cardShadows: StateFlow<Boolean> = settingsRepository.cardShadows.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsRepository.DEFAULT_CARD_SHADOWS
    )

    val gridColumns: StateFlow<Int> = settingsRepository.gridColumns.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsRepository.DEFAULT_GRID_COLUMNS
    )

    val smoothScroll: StateFlow<Boolean> = settingsRepository.smoothScroll.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsRepository.DEFAULT_SMOOTH_SCROLL
    )

    val appLanguage: StateFlow<String> = settingsRepository.appLanguage.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsRepository.DEFAULT_APP_LANGUAGE
    )

    private val _cacheSizeText = MutableStateFlow("Calculando...")
    val cacheSizeText: StateFlow<String> = _cacheSizeText.asStateFlow()

    init {
        refreshCacheSize()
    }

    fun refreshCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            _cacheSizeText.value = calculateCacheSize()
        }
    }

    private fun calculateCacheSize(): String {
        return try {
            val imageCacheDir = File(context.cacheDir, "image_cache")
            val coilCacheDir = File(context.cacheDir, "coil")
            val totalBytes = getFolderSize(imageCacheDir) + getFolderSize(coilCacheDir)
            formatBytes(totalBytes)
        } catch (_: Exception) {
            "0 MB"
        }
    }

    private fun getFolderSize(dir: File): Long {
        if (!dir.exists()) return 0L
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) getFolderSize(file) else file.length()
        }
        return size
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun clearThumbnailCache(onCleared: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Limpiar caché de memoria y disco de Coil
                val imageLoader = context.imageLoader
                imageLoader.memoryCache?.clear()
                imageLoader.diskCache?.clear()

                // 2. Limpiar carpetas temporales de cache manual
                val imageCacheDir = File(context.cacheDir, "image_cache")
                if (imageCacheDir.exists()) imageCacheDir.deleteRecursively()

                val coilCacheDir = File(context.cacheDir, "coil")
                if (coilCacheDir.exists()) coilCacheDir.deleteRecursively()

                // 3. Limpiar caché en memoria de thumbnails del repositorio
                immichRepository.clearCache()

                // 4. Recalcular tamaño
                refreshCacheSize()

                withContext(Dispatchers.Main) {
                    onCleared()
                }
            } catch (_: Exception) {
                refreshCacheSize()
            }
        }
    }

    fun updateServerUrl(url: String) {
        viewModelScope.launch {
            try {
                settingsRepository.setServerUrl(url)
                immichRepository.invalidateService()
            } catch (_: Exception) {}
        }
    }

    fun updateApiKey(key: String) {
        viewModelScope.launch {
            try {
                settingsRepository.setApiKey(key)
                immichRepository.invalidateService()
            } catch (_: Exception) {}
        }
    }

    fun updateGroupOrder(order: String) {
        viewModelScope.launch { try { settingsRepository.setGroupOrder(order) } catch (_: Exception) {} }
    }

    fun updateAssetOrder(order: String) {
        viewModelScope.launch { try { settingsRepository.setAssetOrder(order) } catch (_: Exception) {} }
    }

    fun updateAppLanguage(lang: String) {
        viewModelScope.launch { try { settingsRepository.setAppLanguage(lang) } catch (_: Exception) {} }
    }

    fun updateSlideshowInterval(seconds: Int) {
        viewModelScope.launch { try { settingsRepository.setSlideshowInterval(seconds) } catch (_: Exception) {} }
    }

    fun updateLoopSlideshow(loop: Boolean) {
        viewModelScope.launch { try { settingsRepository.setLoopSlideshow(loop) } catch (_: Exception) {} }
    }

    fun updateLoopVideo(loop: Boolean) {
        viewModelScope.launch { try { settingsRepository.setLoopVideo(loop) } catch (_: Exception) {} }
    }

    fun updateZoomLevel(zoom: Float) {
        viewModelScope.launch { try { settingsRepository.setZoomLevel(zoom) } catch (_: Exception) {} }
    }

    fun updateShowMetadata(show: Boolean) {
        viewModelScope.launch { try { settingsRepository.setShowMetadata(show) } catch (_: Exception) {} }
    }

    fun updateShowCameraInfo(show: Boolean) {
        viewModelScope.launch { try { settingsRepository.setShowCameraInfo(show) } catch (_: Exception) {} }
    }

    fun updateShowExifDetails(show: Boolean) {
        viewModelScope.launch { try { settingsRepository.setShowExifDetails(show) } catch (_: Exception) {} }
    }

    fun updateShowLocationInfo(show: Boolean) {
        viewModelScope.launch { try { settingsRepository.setShowLocationInfo(show) } catch (_: Exception) {} }
    }

    fun updateShowDateInfo(show: Boolean) {
        viewModelScope.launch { try { settingsRepository.setShowDateInfo(show) } catch (_: Exception) {} }
    }

    fun updateShowCounter(show: Boolean) {
        viewModelScope.launch { try { settingsRepository.setShowCounter(show) } catch (_: Exception) {} }
    }

    fun updateShowVideoSpecs(show: Boolean) {
        viewModelScope.launch { try { settingsRepository.setShowVideoSpecs(show) } catch (_: Exception) {} }
    }

    fun updatePreloading(preload: Boolean) {
        viewModelScope.launch { try { settingsRepository.setPreloading(preload) } catch (_: Exception) {} }
    }

    fun updateAnimFocus(anim: Boolean) {
        viewModelScope.launch { try { settingsRepository.setAnimFocus(anim) } catch (_: Exception) {} }
    }

    fun updateCardShadows(shadows: Boolean) {
        viewModelScope.launch { try { settingsRepository.setCardShadows(shadows) } catch (_: Exception) {} }
    }

    fun updateGridColumns(columns: Int) {
        viewModelScope.launch { try { settingsRepository.setGridColumns(columns) } catch (_: Exception) {} }
    }

    fun updateSmoothScroll(smooth: Boolean) {
        viewModelScope.launch { try { settingsRepository.setSmoothScroll(smooth) } catch (_: Exception) {} }
    }

    fun setAllPerformance(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setAllPerformance(enabled)
            } catch (_: Exception) {}
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            try {
                settingsRepository.resetDefaults()
                immichRepository.clearCache()
                refreshCacheSize()
            } catch (_: Exception) {}
        }
    }

    suspend fun testConnection(url: String, key: String): Result<Boolean> {
        return immichRepository.testConnection(url, key)
    }

    class Factory(
        private val context: Context,
        private val settingsRepository: SettingsRepository,
        private val immichRepository: ImmichRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(context, settingsRepository, immichRepository) as T
        }
    }
}

