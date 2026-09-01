package com.bbxtudios.immichtv.ui.screens.player

import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.bbxtudios.immichtv.AppContainer
import com.bbxtudios.immichtv.GlobalKeyHandler
import com.bbxtudios.immichtv.data.model.ViewAsset
import com.bbxtudios.immichtv.data.model.formatVideoSpecs
import com.bbxtudios.immichtv.ui.components.LoadingSpinner
import com.bbxtudios.immichtv.ui.theme.AccentCyan
import com.bbxtudios.immichtv.ui.theme.BackgroundElevated
import com.bbxtudios.immichtv.ui.theme.FocusHighlight
import com.bbxtudios.immichtv.ui.theme.ImmichBlue
import com.bbxtudios.immichtv.ui.theme.OverlayDark
import com.bbxtudios.immichtv.ui.theme.TextPrimary
import com.bbxtudios.immichtv.ui.theme.TextSecondary
import androidx.compose.ui.res.stringResource
import com.bbxtudios.immichtv.R
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale

import com.bbxtudios.immichtv.ui.screens.viewer.SlideshowMode

enum class ControlFocusLevel {
    HIDDEN,
    TIMELINE,
    BUTTONS
}

enum class PlayerButtonTarget {
    REPLAY_10,
    PLAY_PAUSE,
    FORWARD_10,
    SPEED
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    assets: List<ViewAsset>,
    initialIndex: Int,
    loopVideo: Boolean,
    showMetadata: Boolean,
    showCameraInfo: Boolean = true,
    showLocationInfo: Boolean = true,
    showDateInfo: Boolean = true,
    showCounter: Boolean = true,
    showVideoSpecs: Boolean = true,
    initialSlideshowMode: SlideshowMode = SlideshowMode.NONE,
    slideshowIntervalSeconds: Int = 3,
    loopSlideshow: Boolean = true,
    onClose: (lastIndex: Int) -> Unit,
    onNavigateToPhoto: (index: Int, mode: SlideshowMode, intervalSeconds: Int) -> Unit,
    onShowToast: (String) -> Unit,
    onAssetChanged: (index: Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, (assets.size - 1).coerceAtLeast(0))) }
    var detailedAsset by remember { mutableStateOf<ViewAsset?>(null) }
    val baseAsset = assets.getOrNull(currentIndex)
    val currentAsset = detailedAsset ?: baseAsset
    var slideshowMode by remember { mutableStateOf(initialSlideshowMode) }

    val strPaused = stringResource(R.string.toast_slideshow_paused)
    val strStartedAll = stringResource(R.string.toast_slideshow_started_all, slideshowIntervalSeconds)
    val strEnded = stringResource(R.string.toast_slideshow_ended)
    val strNoMore = stringResource(R.string.toast_slideshow_no_more)
    val strFirstItem = stringResource(R.string.toast_first_item)
    val strLastItem = stringResource(R.string.toast_last_item)

    // Notificar índice activo en tiempo real para preservar siempre el foco
    LaunchedEffect(currentIndex) {
        onAssetChanged(currentIndex)
    }

    // Notificación inicial al entrar en pase de diapositivas
    LaunchedEffect(Unit) {
        if (initialSlideshowMode == SlideshowMode.ALL) {
            onShowToast(strStartedAll)
        }
    }

    // Carga de metadatos detallados en segundo plano si faltan
    LaunchedEffect(baseAsset?.id) {
        detailedAsset = null
        val id = baseAsset?.id
        if (id != null && baseAsset.exifMake == null && baseAsset.exifCity == null) {
            try {
                val detail = AppContainer.immichRepository.getAssetDetail(id)
                if (detail != null) {
                    detailedAsset = detail
                }
            } catch (_: Exception) {}
        }
    }
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var isEnded by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(1L) }
    var controlFocusLevel by remember { mutableStateOf(ControlFocusLevel.HIDDEN) }
    var selectedButton by remember { mutableStateOf(PlayerButtonTarget.PLAY_PAUSE) }
    var userInteractionTrigger by remember { mutableLongStateOf(0L) }

    fun resetControlsTimer() {
        userInteractionTrigger = System.currentTimeMillis()
    }

    val availableSpeeds = remember { listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f) }
    var currentSpeedIndex by remember { mutableIntStateOf(2) } // 1.0f por defecto

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = if (loopVideo && slideshowMode == SlideshowMode.NONE) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        }
    }

    // Motor de avance automático para Pase de Diapositivas
    fun advanceSlideshow() {
        if (assets.isEmpty()) return
        var foundNext = false
        for (step in 1..assets.size) {
            val nextIdx = (currentIndex + step) % assets.size
            if (!loopSlideshow && nextIdx < currentIndex) {
                slideshowMode = SlideshowMode.NONE
                onClose(currentIndex)
                onShowToast(strEnded)
                return
            }
            val nextAsset = assets[nextIdx]
            if (nextAsset.isVideo) {
                currentIndex = nextIdx
                isEnded = false
                controlFocusLevel = ControlFocusLevel.HIDDEN
                foundNext = true
                break
            } else {
                onNavigateToPhoto(nextIdx, slideshowMode, slideshowIntervalSeconds)
                foundNext = true
                break
            }
        }
        if (!foundNext) {
            slideshowMode = SlideshowMode.NONE
            onShowToast(strNoMore)
        }
    }

    fun navigateToNext() {
        if (slideshowMode != SlideshowMode.NONE) {
            slideshowMode = SlideshowMode.NONE
            onShowToast(strPaused)
        }
        if (currentIndex < assets.size - 1) {
            val targetIdx = currentIndex + 1
            isEnded = false
            controlFocusLevel = ControlFocusLevel.HIDDEN
            if (assets[targetIdx].isVideo) {
                currentIndex = targetIdx
            } else {
                onNavigateToPhoto(targetIdx, SlideshowMode.NONE, slideshowIntervalSeconds)
            }
        } else if (loopSlideshow && assets.isNotEmpty()) {
            val targetIdx = 0
            isEnded = false
            controlFocusLevel = ControlFocusLevel.HIDDEN
            if (assets[targetIdx].isVideo) {
                currentIndex = targetIdx
            } else {
                onNavigateToPhoto(targetIdx, SlideshowMode.NONE, slideshowIntervalSeconds)
            }
        } else {
            onShowToast(strLastItem)
        }
    }

    fun navigateToPrevious() {
        if (slideshowMode != SlideshowMode.NONE) {
            slideshowMode = SlideshowMode.NONE
            onShowToast(strPaused)
        }
        if (currentIndex > 0) {
            val targetIdx = currentIndex - 1
            isEnded = false
            controlFocusLevel = ControlFocusLevel.HIDDEN
            if (assets[targetIdx].isVideo) {
                currentIndex = targetIdx
            } else {
                onNavigateToPhoto(targetIdx, SlideshowMode.NONE, slideshowIntervalSeconds)
            }
        } else if (loopSlideshow && assets.isNotEmpty()) {
            val targetIdx = assets.size - 1
            isEnded = false
            controlFocusLevel = ControlFocusLevel.HIDDEN
            if (assets[targetIdx].isVideo) {
                currentIndex = targetIdx
            } else {
                onNavigateToPhoto(targetIdx, SlideshowMode.NONE, slideshowIntervalSeconds)
            }
        } else {
            onShowToast(strFirstItem)
        }
    }

    // CAPTURA GLOBAL DE TECLADO / MANDO DE TV INFALIBLE
    DisposableEffect(controlFocusLevel, isEnded, isPlaying, selectedButton, currentSpeedIndex) {
        GlobalKeyHandler.listener = { event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (controlFocusLevel != ControlFocusLevel.HIDDEN) {
                    resetControlsTimer()
                }
                when (event.keyCode) {
                    KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                        if (controlFocusLevel != ControlFocusLevel.HIDDEN) {
                            controlFocusLevel = ControlFocusLevel.HIDDEN
                            true
                        } else {
                            onClose(currentIndex)
                            true
                        }
                    }

                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        if (isEnded) {
                            exoPlayer.seekTo(0)
                            exoPlayer.play()
                            isEnded = false
                            isPlaying = true
                        } else {
                            when (controlFocusLevel) {
                                ControlFocusLevel.HIDDEN -> {
                                    if (isPlaying) {
                                        exoPlayer.pause()
                                        controlFocusLevel = ControlFocusLevel.TIMELINE
                                    } else {
                                        exoPlayer.play()
                                    }
                                }
                                ControlFocusLevel.TIMELINE -> {
                                    if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                }
                                ControlFocusLevel.BUTTONS -> {
                                    when (selectedButton) {
                                        PlayerButtonTarget.REPLAY_10 -> {
                                            val p = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                            exoPlayer.seekTo(p)
                                            currentPosition = p
                                        }
                                        PlayerButtonTarget.PLAY_PAUSE -> {
                                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                        }
                                        PlayerButtonTarget.FORWARD_10 -> {
                                            val p = (exoPlayer.currentPosition + 10000).coerceAtMost(duration)
                                            exoPlayer.seekTo(p)
                                            currentPosition = p
                                        }
                                        PlayerButtonTarget.SPEED -> {
                                            currentSpeedIndex = (currentSpeedIndex + 1) % availableSpeeds.size
                                            val speed = availableSpeeds[currentSpeedIndex]
                                            exoPlayer.setPlaybackSpeed(speed)
                                            onShowToast("Velocidad: ${speed}x")
                                        }
                                    }
                                }
                            }
                        }
                        true
                    }

                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                        true
                    }

                    KeyEvent.KEYCODE_DPAD_UP -> {
                        when (controlFocusLevel) {
                            ControlFocusLevel.BUTTONS -> {
                                controlFocusLevel = ControlFocusLevel.TIMELINE
                            }
                            ControlFocusLevel.TIMELINE -> {
                                controlFocusLevel = ControlFocusLevel.HIDDEN
                            }
                            ControlFocusLevel.HIDDEN -> {
                                controlFocusLevel = ControlFocusLevel.TIMELINE
                            }
                        }
                        true
                    }

                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        when (controlFocusLevel) {
                            ControlFocusLevel.HIDDEN -> {
                                controlFocusLevel = ControlFocusLevel.TIMELINE
                            }
                            ControlFocusLevel.TIMELINE -> {
                                controlFocusLevel = ControlFocusLevel.BUTTONS
                                selectedButton = PlayerButtonTarget.PLAY_PAUSE
                            }
                            ControlFocusLevel.BUTTONS -> {
                                controlFocusLevel = ControlFocusLevel.HIDDEN
                            }
                        }
                        true
                    }

                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (isEnded) {
                            navigateToNext()
                        } else {
                            when (controlFocusLevel) {
                                ControlFocusLevel.HIDDEN -> {
                                    navigateToNext()
                                }
                                ControlFocusLevel.TIMELINE -> {
                                    val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(duration)
                                    exoPlayer.seekTo(newPos)
                                    currentPosition = newPos
                                }
                                ControlFocusLevel.BUTTONS -> {
                                    selectedButton = when (selectedButton) {
                                        PlayerButtonTarget.REPLAY_10 -> PlayerButtonTarget.PLAY_PAUSE
                                        PlayerButtonTarget.PLAY_PAUSE -> PlayerButtonTarget.FORWARD_10
                                        PlayerButtonTarget.FORWARD_10 -> PlayerButtonTarget.SPEED
                                        PlayerButtonTarget.SPEED -> PlayerButtonTarget.SPEED
                                    }
                                }
                            }
                        }
                        true
                    }

                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (isEnded) {
                            navigateToPrevious()
                        } else {
                            when (controlFocusLevel) {
                                ControlFocusLevel.HIDDEN -> {
                                    navigateToPrevious()
                                }
                                ControlFocusLevel.TIMELINE -> {
                                    val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                    exoPlayer.seekTo(newPos)
                                    currentPosition = newPos
                                }
                                ControlFocusLevel.BUTTONS -> {
                                    selectedButton = when (selectedButton) {
                                        PlayerButtonTarget.SPEED -> PlayerButtonTarget.FORWARD_10
                                        PlayerButtonTarget.FORWARD_10 -> PlayerButtonTarget.PLAY_PAUSE
                                        PlayerButtonTarget.PLAY_PAUSE -> PlayerButtonTarget.REPLAY_10
                                        PlayerButtonTarget.REPLAY_10 -> PlayerButtonTarget.REPLAY_10
                                    }
                                }
                            }
                        }
                        true
                    }

                    else -> false
                }
            } else {
                false
            }
        }

        onDispose {
            GlobalKeyHandler.listener = null
        }
    }

    // Sincronizar fuente de vídeo
    LaunchedEffect(currentAsset?.id) {
        val videoUrl = currentAsset?.videoPlaybackUrl ?: currentAsset?.url
        if (videoUrl != null) {
            isBuffering = true
            isEnded = false
            currentPosition = 0L
            duration = 1L

            val mediaItem = MediaItem.fromUri(videoUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    // Listener de eventos del reproductor
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> isBuffering = true
                    Player.STATE_READY -> {
                        isBuffering = false
                        duration = exoPlayer.duration.coerceAtLeast(1L)
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        if (slideshowMode != SlideshowMode.NONE) {
                            advanceSlideshow()
                        } else if (loopVideo) {
                            exoPlayer.seekTo(0)
                            exoPlayer.play()
                        } else {
                            isEnded = true
                            isPlaying = false
                        }
                    }
                    Player.STATE_IDLE -> Unit
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                onShowToast("Error al reproducir vídeo")
                if (slideshowMode != SlideshowMode.NONE) {
                    advanceSlideshow()
                }
            }
        }

        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Bucle de actualización de posición
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition
            delay(500)
        }
    }

    // Auto-ocultar controles tras 5 segundos de inactividad (se reinicia con cada tecla o toque)
    LaunchedEffect(controlFocusLevel, isPlaying, userInteractionTrigger, selectedButton) {
        if (controlFocusLevel != ControlFocusLevel.HIDDEN && isPlaying) {
            delay(5000)
            controlFocusLevel = ControlFocusLevel.HIDDEN
        }
    }

    // Formateadores de metadatos EXIF
    val formattedDate = remember(currentAsset?.createdAt) {
        currentAsset?.createdAt?.let { dateStr ->
            try {
                val cleanStr = dateStr.replace("Z", "+0000").replace("+00:00", "+0000")
                val patterns = listOf(
                    "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS",
                    "yyyy-MM-dd'T'HH:mm:ssZ",
                    "yyyy-MM-dd'T'HH:mm:ss",
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd"
                )
                var parsedDate: java.util.Date? = null
                for (p in patterns) {
                    try {
                        parsedDate = SimpleDateFormat(p, Locale.US).parse(cleanStr)
                        if (parsedDate != null) break
                    } catch (_: Exception) {}
                }
                if (parsedDate != null) {
                    val locale = Locale.getDefault()
                    val isSpanish = locale.language.startsWith("es")
                    val pattern = if (isSpanish) "d 'de' MMMM 'de' yyyy, HH:mm" else "MMMM d, yyyy, HH:mm"
                    SimpleDateFormat(pattern, locale).format(parsedDate)
                } else {
                    dateStr.substringBefore('T')
                }
            } catch (e: Exception) {
                dateStr
            }
        } ?: ""
    }

    val cameraInfo = remember(currentAsset) {
        listOfNotNull(
            currentAsset?.exifMake?.takeIf { it.isNotBlank() },
            currentAsset?.exifModel?.takeIf { it.isNotBlank() }
        ).distinct().joinToString(" ")
    }

    val locationInfo = remember(currentAsset) {
        listOfNotNull(
            currentAsset?.exifCity?.takeIf { it.isNotBlank() },
            currentAsset?.exifCountry?.takeIf { it.isNotBlank() }
        ).distinct().joinToString(", ")
    }

    val videoSpecs = remember(currentAsset, exoPlayer.videoFormat) {
        val effectiveWidth = currentAsset?.width?.takeIf { it > 0 } ?: exoPlayer.videoFormat?.width?.takeIf { it > 0 }
        val effectiveHeight = currentAsset?.height?.takeIf { it > 0 } ?: exoPlayer.videoFormat?.height?.takeIf { it > 0 }
        val effectiveFps = currentAsset?.fps?.takeIf { it > 0.0 } ?: exoPlayer.videoFormat?.frameRate?.toDouble()?.takeIf { it > 0.0 }
        formatVideoSpecs(effectiveWidth, effectiveHeight, effectiveFps)
    }

    val isControlsVisible = controlFocusLevel != ControlFocusLevel.HIDDEN

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        controlFocusLevel = if (controlFocusLevel == ControlFocusLevel.HIDDEN) {
                            ControlFocusLevel.TIMELINE
                        } else {
                            ControlFocusLevel.HIDDEN
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (totalDrag > 80f) {
                            navigateToPrevious()
                        } else if (totalDrag < -80f) {
                            navigateToNext()
                        }
                        totalDrag = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                    }
                )
            }
    ) {
        // --- SURFACE DE VÍDEO CON EXOPLAYER ---
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // --- SPINNER DE BUFFERING ---
        if (isBuffering) {
            LoadingSpinner(
                modifier = Modifier.align(Alignment.Center),
                size = 64.dp
            )
        }

        // --- PANTALLA / BOTÓN DE REPETIR VÍDEO AL TERMINAR ---
        AnimatedVisibility(
            visible = isEnded,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(OverlayDark)
                    .border(2.dp, FocusHighlight, RoundedCornerShape(20.dp))
                    .padding(32.dp)
            ) {
                Text(
                    text = stringResource(R.string.player_video_ended),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón Anterior
                    IconButtonWithFocus(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        text = stringResource(R.string.player_prev),
                        onClick = { navigateToPrevious() }
                    )

                    // Botón Repetir (Centrado)
                    val replayDesc = stringResource(R.string.player_replay)
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(ImmichBlue)
                            .border(3.dp, FocusHighlight, CircleShape)
                            .clickable {
                                exoPlayer.seekTo(0)
                                exoPlayer.play()
                                isEnded = false
                                isPlaying = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Replay,
                            contentDescription = replayDesc,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Botón Siguiente
                    IconButtonWithFocus(
                        icon = Icons.AutoMirrored.Rounded.ArrowForward,
                        text = stringResource(R.string.player_next),
                        onClick = { navigateToNext() }
                    )
                }
            }
        }

        // --- OVERLAY EXIF COMPACTO (Esquina Inferior Izquierda) ---
        if (showMetadata && currentAsset != null && !isControlsVisible && !isEnded) {
            val hasAnyExif = (showDateInfo && formattedDate.isNotBlank()) ||
                    (showCameraInfo && cameraInfo.isNotBlank()) ||
                    (showLocationInfo && locationInfo.isNotBlank()) ||
                    (showVideoSpecs && videoSpecs.isNotBlank())

            if (hasAnyExif) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(OverlayDark)
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    // Fecha
                    if (showDateInfo && formattedDate.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarToday,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    // Cámara / Modelo
                    if (showCameraInfo && cameraInfo.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.PhotoCamera,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cameraInfo,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            )
                        }
                    }

                    // Resolución y FPS
                    if (showVideoSpecs && videoSpecs.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Videocam,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = videoSpecs,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = TextPrimary.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    // Ubicación
                    if (showLocationInfo && locationInfo.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = locationInfo,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            )
                        }
                    }
                }
            }
        }

        // --- CONTADOR DISCRETO (Esquina Inferior Derecha) ---
        if (showCounter && !isEnded && !isControlsVisible && assets.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "${currentIndex + 1} / ${assets.size}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = TextPrimary
                    )
                )
            }
        }

        // --- BARRA INFERIOR DE CONTROLES (LÍNEA DE TIEMPO) ---
        AnimatedVisibility(
            visible = controlFocusLevel != ControlFocusLevel.HIDDEN && !isEnded,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val isTimelineFocused = controlFocusLevel == ControlFocusLevel.TIMELINE

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xF50A0E1A))
                        )
                    )
                    .padding(horizontal = 32.dp, vertical = 18.dp)
            ) {
                // Chip flotante indicador de salto cuando la línea de tiempo tiene foco
                AnimatedVisibility(
                    visible = isTimelineFocused,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xE6161D2B))
                                .border(1.dp, FocusHighlight.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 16.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Replay10,
                                    contentDescription = null,
                                    tint = FocusHighlight,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "◄  ${formatTime(currentPosition)} / ${formatTime(duration)}  ►",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Forward10,
                                    contentDescription = null,
                                    tint = FocusHighlight,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Slider de Progreso limpio y elegante
                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = { newPos ->
                        resetControlsTimer()
                        currentPosition = newPos.toLong()
                        exoPlayer.seekTo(newPos.toLong())
                    },
                    valueRange = 0f..duration.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = if (isTimelineFocused) FocusHighlight else Color.White,
                        activeTrackColor = if (isTimelineFocused) FocusHighlight else ImmichBlue,
                        inactiveTrackColor = Color(0x44FFFFFF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Fila de Tiempo y Botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Rebobinar 10s
                        val isReplayFocused = controlFocusLevel == ControlFocusLevel.BUTTONS && selectedButton == PlayerButtonTarget.REPLAY_10
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .scale(if (isReplayFocused) 1.15f else 1.0f)
                                .clip(CircleShape)
                                .background(if (isReplayFocused) ImmichBlue else BackgroundElevated)
                                .border(
                                    width = if (isReplayFocused) 2.5.dp else 1.dp,
                                    color = if (isReplayFocused) FocusHighlight else Color(0x33FFFFFF),
                                    shape = CircleShape
                                )
                                .clickable {
                                    resetControlsTimer()
                                    val p = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                    exoPlayer.seekTo(p)
                                    currentPosition = p
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Replay10,
                                contentDescription = stringResource(R.string.player_replay_10),
                                tint = if (isReplayFocused) Color.White else TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // 2. Reproducir / Pausar
                        val isPlayFocused = controlFocusLevel == ControlFocusLevel.BUTTONS && selectedButton == PlayerButtonTarget.PLAY_PAUSE
                        val playPauseDesc = if (isPlaying) stringResource(R.string.player_pause) else stringResource(R.string.player_play)
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .scale(if (isPlayFocused) 1.15f else 1.0f)
                                .clip(CircleShape)
                                .background(if (isPlayFocused) ImmichBlue else Color(0xFF2A364F))
                                .border(
                                    width = if (isPlayFocused) 2.5.dp else 1.dp,
                                    color = if (isPlayFocused) FocusHighlight else Color(0x44FFFFFF),
                                    shape = CircleShape
                                )
                                .clickable {
                                    resetControlsTimer()
                                    if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = playPauseDesc,
                                tint = if (isPlayFocused) Color.White else FocusHighlight,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // 3. Avanzar 10s
                        val isForwardFocused = controlFocusLevel == ControlFocusLevel.BUTTONS && selectedButton == PlayerButtonTarget.FORWARD_10
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .scale(if (isForwardFocused) 1.15f else 1.0f)
                                .clip(CircleShape)
                                .background(if (isForwardFocused) ImmichBlue else BackgroundElevated)
                                .border(
                                    width = if (isForwardFocused) 2.5.dp else 1.dp,
                                    color = if (isForwardFocused) FocusHighlight else Color(0x33FFFFFF),
                                    shape = CircleShape
                                )
                                .clickable {
                                    resetControlsTimer()
                                    val p = (exoPlayer.currentPosition + 10000).coerceAtMost(duration)
                                    exoPlayer.seekTo(p)
                                    currentPosition = p
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Forward10,
                                contentDescription = stringResource(R.string.player_forward_10),
                                tint = if (isForwardFocused) Color.White else TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // 4. Selector de Velocidad
                        val isSpeedFocused = controlFocusLevel == ControlFocusLevel.BUTTONS && selectedButton == PlayerButtonTarget.SPEED
                        val speed = availableSpeeds[currentSpeedIndex]
                        val speedToastTemplate = stringResource(R.string.player_speed_toast, "%s")
                        Box(
                            modifier = Modifier
                                .scale(if (isSpeedFocused) 1.12f else 1.0f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSpeedFocused) ImmichBlue else BackgroundElevated)
                                .border(
                                    width = if (isSpeedFocused) 2.5.dp else 1.dp,
                                    color = if (isSpeedFocused) FocusHighlight else Color(0x33FFFFFF),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    resetControlsTimer()
                                    currentSpeedIndex = (currentSpeedIndex + 1) % availableSpeeds.size
                                    val newSpeed = availableSpeeds[currentSpeedIndex]
                                    exoPlayer.setPlaybackSpeed(newSpeed)
                                    onShowToast(speedToastTemplate.replace("%s", newSpeed.toString()))
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${speed}x",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSpeedFocused || speed != 1.0f) FocusHighlight else TextPrimary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconButtonWithFocus(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .scale(if (isFocused) 1.1f else 1.0f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFocused) FocusHighlight else BackgroundElevated)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color.White else Color(0x33FFFFFF),
                shape = RoundedCornerShape(12.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = if (isFocused) Color.Black else TextPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isFocused) Color.Black else TextPrimary
                )
            )
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}
