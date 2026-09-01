package com.bbxtudios.immichtv.ui.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import com.bbxtudios.immichtv.R
import com.bbxtudios.immichtv.AppContainer
import com.bbxtudios.immichtv.data.model.NavTab
import com.bbxtudios.immichtv.data.model.ViewAsset
import com.bbxtudios.immichtv.ui.components.SidebarNav
import com.bbxtudios.immichtv.ui.components.ToastHost
import com.bbxtudios.immichtv.ui.screens.albums.AlbumsScreen
import com.bbxtudios.immichtv.ui.screens.albums.AlbumsViewModel
import com.bbxtudios.immichtv.ui.screens.folders.FoldersScreen
import com.bbxtudios.immichtv.ui.screens.folders.FoldersViewModel
import com.bbxtudios.immichtv.ui.screens.login.LoginScreen
import com.bbxtudios.immichtv.ui.screens.memories.MemoriesScreen
import com.bbxtudios.immichtv.ui.screens.memories.MemoriesViewModel
import com.bbxtudios.immichtv.ui.screens.player.VideoPlayerScreen
import com.bbxtudios.immichtv.ui.screens.random.RandomScreen
import com.bbxtudios.immichtv.ui.screens.random.RandomViewModel
import com.bbxtudios.immichtv.ui.screens.settings.SettingsScreen
import com.bbxtudios.immichtv.ui.screens.settings.SettingsViewModel
import com.bbxtudios.immichtv.ui.screens.viewer.PhotoViewerScreen
import com.bbxtudios.immichtv.ui.screens.viewer.SlideshowMode
import com.bbxtudios.immichtv.ui.theme.BackgroundDark
import com.bbxtudios.immichtv.util.ProvideAppLanguage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class ScreenMode {
    object Login : ScreenMode()
    object Tabs : ScreenMode()
    data class PhotoViewer(
        val assets: List<ViewAsset>,
        val initialIndex: Int,
        val initialSlideshowMode: SlideshowMode = SlideshowMode.NONE,
        val customIntervalSeconds: Int? = null
    ) : ScreenMode()
    data class VideoPlayer(
        val assets: List<ViewAsset>,
        val initialIndex: Int,
        val slideshowMode: SlideshowMode = SlideshowMode.NONE,
        val customIntervalSeconds: Int? = null
    ) : ScreenMode()
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val immichRepo = remember { AppContainer.immichRepository }
    val settingsRepo = remember { AppContainer.settingsRepository }

    var selectedTab by remember { mutableStateOf(NavTab.FOLDERS) }
    var screenMode by remember { mutableStateOf<ScreenMode>(ScreenMode.Tabs) }
    var lastViewedAssetId by remember { mutableStateOf<String?>(null) }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    var focusTrigger by remember { mutableIntStateOf(0) }

    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastJob by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun showToast(msg: String) {
        toastJob?.cancel()
        toastMessage = msg
        toastJob = coroutineScope.launch {
            delay(2500)
            toastMessage = null
        }
    }

    // ViewModels principales
    val foldersViewModel: FoldersViewModel = viewModel(
        factory = FoldersViewModel.Factory(immichRepo)
    )
    val albumsViewModel: AlbumsViewModel = viewModel(
        factory = AlbumsViewModel.Factory(immichRepo)
    )
    val randomViewModel: RandomViewModel = viewModel(
        factory = RandomViewModel.Factory(immichRepo)
    )
    val memoriesViewModel: MemoriesViewModel = viewModel(
        factory = MemoriesViewModel.Factory(immichRepo)
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(context.applicationContext, settingsRepo, immichRepo)
    )

    val currentServerUrl by settingsRepo.serverUrl.collectAsState(initial = null)
    val currentApiKey by settingsRepo.apiKey.collectAsState(initial = null)
    val appLanguageSetting by settingsViewModel.appLanguage.collectAsState()

    var hasDeterminedInitialRoute by remember { mutableStateOf(false) }

    LaunchedEffect(currentServerUrl, currentApiKey) {
        val url = currentServerUrl
        val key = currentApiKey
        if (url != null && key != null && !hasDeterminedInitialRoute) {
            hasDeterminedInitialRoute = true
            if (url.isBlank() || key.isBlank()) {
                screenMode = ScreenMode.Login
            } else {
                screenMode = ScreenMode.Tabs
            }
        }
    }

    val showMetadataSetting by settingsViewModel.showMetadata.collectAsState()
    val showCameraInfoSetting by settingsViewModel.showCameraInfo.collectAsState()
    val showExifDetailsSetting by settingsViewModel.showExifDetails.collectAsState()
    val showLocationInfoSetting by settingsViewModel.showLocationInfo.collectAsState()
    val showDateInfoSetting by settingsViewModel.showDateInfo.collectAsState()
    val showCounterSetting by settingsViewModel.showCounter.collectAsState()
    val showVideoSpecsSetting by settingsViewModel.showVideoSpecs.collectAsState()
    val preloadingSetting by settingsViewModel.preloading.collectAsState()
    val slideshowIntervalSetting by settingsViewModel.slideshowInterval.collectAsState()
    val loopSlideshowSetting by settingsViewModel.loopSlideshow.collectAsState()
    val loopVideoSetting by settingsViewModel.loopVideo.collectAsState()
    val zoomLevelSetting by settingsViewModel.zoomLevel.collectAsState()
    val animFocusSetting by settingsViewModel.animFocus.collectAsState()
    val cardShadowsSetting by settingsViewModel.cardShadows.collectAsState()

    ProvideAppLanguage(languageCode = appLanguageSetting) {
        val exitText = stringResource(R.string.exit_double_press)

        // Manejo global del botón atrás (Back)
        BackHandler {
            when (screenMode) {
                is ScreenMode.Login -> {
                    val now = System.currentTimeMillis()
                    if (now - lastBackPressTime < 2000) {
                        (context as? Activity)?.finish()
                    } else {
                        lastBackPressTime = now
                        showToast(exitText)
                    }
                }
                is ScreenMode.PhotoViewer, is ScreenMode.VideoPlayer -> {
                    screenMode = ScreenMode.Tabs
                }
                ScreenMode.Tabs -> {
                    if (selectedTab == NavTab.FOLDERS) {
                        val handled = foldersViewModel.navigateUp()
                        if (!handled) {
                            val now = System.currentTimeMillis()
                            if (now - lastBackPressTime < 2000) {
                                (context as? Activity)?.finish()
                            } else {
                                lastBackPressTime = now
                                showToast(exitText)
                            }
                        }
                    } else if (selectedTab == NavTab.ALBUMS) {
                        val handled = albumsViewModel.navigateUp()
                        if (!handled) {
                            val now = System.currentTimeMillis()
                            if (now - lastBackPressTime < 2000) {
                                (context as? Activity)?.finish()
                            } else {
                                lastBackPressTime = now
                                showToast(exitText)
                            }
                        }
                    } else {
                        val now = System.currentTimeMillis()
                        if (now - lastBackPressTime < 2000) {
                            (context as? Activity)?.finish()
                        } else {
                            lastBackPressTime = now
                            showToast(exitText)
                        }
                    }
                }
            }
        }

        Box(modifier = modifier.fillMaxSize().background(BackgroundDark)) {
            if (screenMode is ScreenMode.Login) {
                LoginScreen(
                    initialServerUrl = currentServerUrl ?: "",
                    initialApiKey = currentApiKey ?: "",
                    onValidateCredentials = { url, key ->
                        val result = settingsViewModel.testConnection(url, key)
                        if (result.isSuccess) {
                            true to context.getString(R.string.toast_server_ok)
                        } else {
                            false to (result.exceptionOrNull()?.message ?: context.getString(R.string.login_error_connection))
                        }
                    },
                    onLoginSuccess = { url, key ->
                        settingsViewModel.updateServerUrl(url)
                        settingsViewModel.updateApiKey(key)
                        foldersViewModel.refresh()
                        albumsViewModel.loadAlbums()
                        randomViewModel.loadRandomAssets()
                        memoriesViewModel.loadMemories()
                        screenMode = ScreenMode.Tabs
                    }
                )
            } else {
                // --- 1. PANTALLA PRINCIPAL CON SIDEBAR NAVEGACIÓN ---
                Row(modifier = Modifier.fillMaxSize()) {
                    SidebarNav(
                        selectedTab = selectedTab,
                        onTabSelected = { tab ->
                            selectedTab = tab
                        },
                        onRequestContentFocus = {
                            focusTrigger++
                        },
                        animFocus = animFocusSetting,
                        cardShadows = cardShadowsSetting
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        when (selectedTab) {
                            NavTab.FOLDERS -> FoldersScreen(
                                viewModel = foldersViewModel,
                                lastViewedAssetId = lastViewedAssetId,
                                focusTrigger = focusTrigger,
                                onAssetClick = { assets, index ->
                                    val asset = assets.getOrNull(index)
                                    lastViewedAssetId = asset?.id
                                    if (asset != null && asset.isVideo) {
                                        screenMode = ScreenMode.VideoPlayer(assets, index)
                                    } else {
                                        screenMode = ScreenMode.PhotoViewer(assets, index)
                                    }
                                },
                                onStartSlideshow = { assets, startIndex, mode, intervalSec ->
                                    val asset = assets.getOrNull(startIndex)
                                    if (asset != null) lastViewedAssetId = asset.id
                                    if (asset != null && asset.isVideo) {
                                        screenMode = ScreenMode.VideoPlayer(
                                            assets = assets,
                                            initialIndex = startIndex,
                                            slideshowMode = mode,
                                            customIntervalSeconds = intervalSec
                                        )
                                    } else {
                                        screenMode = ScreenMode.PhotoViewer(
                                            assets = assets,
                                            initialIndex = startIndex,
                                            initialSlideshowMode = mode,
                                            customIntervalSeconds = intervalSec
                                        )
                                    }
                                }
                            )

                            NavTab.ALBUMS -> AlbumsScreen(
                                viewModel = albumsViewModel,
                                lastViewedAssetId = lastViewedAssetId,
                                focusTrigger = focusTrigger,
                                onAssetClick = { assets, index ->
                                    val asset = assets.getOrNull(index)
                                    lastViewedAssetId = asset?.id
                                    if (asset != null && asset.isVideo) {
                                        screenMode = ScreenMode.VideoPlayer(assets, index)
                                    } else {
                                        screenMode = ScreenMode.PhotoViewer(assets, index)
                                    }
                                },
                                onStartSlideshow = { assets, startIndex, mode, intervalSec ->
                                    val asset = assets.getOrNull(startIndex)
                                    if (asset != null) lastViewedAssetId = asset.id
                                    if (asset != null && asset.isVideo) {
                                        screenMode = ScreenMode.VideoPlayer(
                                            assets = assets,
                                            initialIndex = startIndex,
                                            slideshowMode = mode,
                                            customIntervalSeconds = intervalSec
                                        )
                                    } else {
                                        screenMode = ScreenMode.PhotoViewer(
                                            assets = assets,
                                            initialIndex = startIndex,
                                            initialSlideshowMode = mode,
                                            customIntervalSeconds = intervalSec
                                        )
                                    }
                                }
                            )

                            NavTab.RANDOM -> RandomScreen(
                                viewModel = randomViewModel,
                                lastViewedAssetId = lastViewedAssetId,
                                focusTrigger = focusTrigger,
                                onAssetClick = { assets, index ->
                                    val asset = assets.getOrNull(index)
                                    lastViewedAssetId = asset?.id
                                    if (asset != null && asset.isVideo) {
                                        screenMode = ScreenMode.VideoPlayer(assets, index)
                                    } else {
                                        screenMode = ScreenMode.PhotoViewer(assets, index)
                                    }
                                }
                            )

                            NavTab.MEMORIES -> MemoriesScreen(
                                viewModel = memoriesViewModel,
                                lastViewedAssetId = lastViewedAssetId,
                                focusTrigger = focusTrigger,
                                onAssetClick = { assets, index ->
                                    val asset = assets.getOrNull(index)
                                    lastViewedAssetId = asset?.id
                                    if (asset != null && asset.isVideo) {
                                        screenMode = ScreenMode.VideoPlayer(assets, index)
                                    } else {
                                        screenMode = ScreenMode.PhotoViewer(assets, index)
                                    }
                                }
                            )

                            NavTab.SETTINGS -> SettingsScreen(
                                viewModel = settingsViewModel,
                                focusTrigger = focusTrigger,
                                onShowToast = { showToast(it) },
                                onLogout = {
                                    screenMode = ScreenMode.Login
                                }
                            )
                        }
                    }
                }

                // --- 2. CAPA OVERLAY DE VISOR DE FOTOS / REPRODUCTOR DE VÍDEO ---
                when (val mode = screenMode) {
                    is ScreenMode.PhotoViewer -> {
                        PhotoViewerScreen(
                            assets = mode.assets,
                            initialIndex = mode.initialIndex,
                            initialSlideshowMode = mode.initialSlideshowMode,
                            showMetadata = showMetadataSetting,
                            showCameraInfo = showCameraInfoSetting,
                            showExifDetails = showExifDetailsSetting,
                            showLocationInfo = showLocationInfoSetting,
                            showDateInfo = showDateInfoSetting,
                            showCounter = showCounterSetting,
                            preloading = preloadingSetting,
                            defaultZoomLevel = zoomLevelSetting,
                            slideshowIntervalSeconds = mode.customIntervalSeconds ?: slideshowIntervalSetting,
                            loopSlideshow = loopSlideshowSetting,
                            onAssetChanged = { currentIdx ->
                                val asset = mode.assets.getOrNull(currentIdx)
                                if (asset != null) lastViewedAssetId = asset.id
                            },
                            onClose = { lastIndex ->
                                val asset = mode.assets.getOrNull(lastIndex)
                                if (asset != null) lastViewedAssetId = asset.id
                                screenMode = ScreenMode.Tabs
                                focusTrigger++
                            },
                            onNavigateToVideo = { index, navSlideshowMode, intervalSec ->
                                val asset = mode.assets.getOrNull(index)
                                if (asset != null) lastViewedAssetId = asset.id
                                screenMode = ScreenMode.VideoPlayer(
                                    assets = mode.assets,
                                    initialIndex = index,
                                    slideshowMode = navSlideshowMode,
                                    customIntervalSeconds = intervalSec
                                )
                            },
                            onShowToast = { showToast(it) }
                        )
                    }

                    is ScreenMode.VideoPlayer -> {
                        VideoPlayerScreen(
                            assets = mode.assets,
                            initialIndex = mode.initialIndex,
                            loopVideo = loopVideoSetting,
                            showMetadata = showMetadataSetting,
                            showCameraInfo = showCameraInfoSetting,
                            showLocationInfo = showLocationInfoSetting,
                            showDateInfo = showDateInfoSetting,
                            showCounter = showCounterSetting,
                            showVideoSpecs = showVideoSpecsSetting,
                            initialSlideshowMode = mode.slideshowMode,
                            slideshowIntervalSeconds = mode.customIntervalSeconds ?: slideshowIntervalSetting,
                            loopSlideshow = loopSlideshowSetting,
                            onAssetChanged = { currentIdx ->
                                val asset = mode.assets.getOrNull(currentIdx)
                                if (asset != null) lastViewedAssetId = asset.id
                            },
                            onClose = { lastIndex ->
                                val asset = mode.assets.getOrNull(lastIndex)
                                if (asset != null) lastViewedAssetId = asset.id
                                screenMode = ScreenMode.Tabs
                                focusTrigger++
                            },
                            onNavigateToPhoto = { index, navSlideshowMode, intervalSec ->
                                val asset = mode.assets.getOrNull(index)
                                if (asset != null) lastViewedAssetId = asset.id
                                screenMode = ScreenMode.PhotoViewer(
                                    assets = mode.assets,
                                    initialIndex = index,
                                    initialSlideshowMode = navSlideshowMode,
                                    customIntervalSeconds = intervalSec
                                )
                            },
                            onShowToast = { showToast(it) }
                        )
                    }

                    ScreenMode.Tabs, ScreenMode.Login -> Unit
                }
            }

            // Host de notificaciones flotantes (Toasts)
            ToastHost(
                message = toastMessage,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
