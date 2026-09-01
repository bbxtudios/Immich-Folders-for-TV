package com.bbxtudios.immichtv.ui.screens.viewer

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.size.Precision
import coil.size.Size
import coil.request.ImageRequest
import com.bbxtudios.immichtv.AppContainer
import com.bbxtudios.immichtv.GlobalKeyHandler
import com.bbxtudios.immichtv.data.model.ViewAsset
import com.bbxtudios.immichtv.ui.theme.AccentCyan
import com.bbxtudios.immichtv.ui.theme.BackgroundElevated
import com.bbxtudios.immichtv.ui.theme.FocusHighlight
import com.bbxtudios.immichtv.ui.theme.ImmichBlue
import com.bbxtudios.immichtv.ui.theme.OverlayDark
import com.bbxtudios.immichtv.ui.theme.TextPrimary
import com.bbxtudios.immichtv.ui.theme.TextSecondary
import androidx.compose.ui.res.stringResource
import com.bbxtudios.immichtv.R
import com.bbxtudios.immichtv.ui.components.LoadingSpinner
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale

enum class SlideshowMode {
    NONE,
    PHOTOS_ONLY,
    ALL
}

@Composable
fun PhotoViewerScreen(
    assets: List<ViewAsset>,
    initialIndex: Int,
    initialSlideshowMode: SlideshowMode = SlideshowMode.NONE,
    showMetadata: Boolean,
    showCameraInfo: Boolean = true,
    showExifDetails: Boolean = true,
    showLocationInfo: Boolean = true,
    showDateInfo: Boolean = true,
    showCounter: Boolean = true,
    preloading: Boolean,
    defaultZoomLevel: Float = 1.75f,
    slideshowIntervalSeconds: Int,
    loopSlideshow: Boolean,
    onClose: (lastIndex: Int) -> Unit,
    onNavigateToVideo: (index: Int, mode: SlideshowMode, intervalSeconds: Int) -> Unit,
    onShowToast: (String) -> Unit,
    onAssetChanged: (index: Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, (assets.size - 1).coerceAtLeast(0))) }
    var detailedAsset by remember { mutableStateOf<ViewAsset?>(null) }
    val baseAsset = assets.getOrNull(currentIndex)
    val currentAsset = detailedAsset ?: baseAsset

    // Notificar índice activo en tiempo real para preservar siempre el foco
    LaunchedEffect(currentIndex) {
        onAssetChanged(currentIndex)
    }

    // Carga de metadatos detallados en segundo plano si faltan
    LaunchedEffect(baseAsset?.id) {
        detailedAsset = null
        val id = baseAsset?.id
        if (id != null && (baseAsset.exifMake == null && baseAsset.exifCity == null && baseAsset.exifFNumber == null)) {
            try {
                val detail = AppContainer.immichRepository.getAssetDetail(id)
                if (detail != null) {
                    detailedAsset = detail
                }
            } catch (_: Exception) {}
        }
    }

    var isZoomed by remember { mutableStateOf(false) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    val zoomScale = defaultZoomLevel
    val panStep = 90f
    val maxPanX = (800f * (zoomScale - 1f)).coerceAtLeast(300f)
    val maxPanY = (550f * (zoomScale - 1f)).coerceAtLeast(200f)

    val strPaused = stringResource(R.string.toast_slideshow_paused)
    val strStartedPhotos = stringResource(R.string.toast_slideshow_started_photos, slideshowIntervalSeconds)
    val strStartedAll = stringResource(R.string.toast_slideshow_started_all)
    val strEnded = stringResource(R.string.toast_slideshow_ended)
    val strNoMore = stringResource(R.string.toast_slideshow_no_more)

    val animatedScale by animateFloatAsState(
        targetValue = if (isZoomed) zoomScale else 1f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "zoomScale"
    )
    val animatedPanX by animateFloatAsState(
        targetValue = if (isZoomed) panX else 0f,
        animationSpec = tween(durationMillis = 150, easing = LinearOutSlowInEasing),
        label = "panX"
    )
    val animatedPanY by animateFloatAsState(
        targetValue = if (isZoomed) panY else 0f,
        animationSpec = tween(durationMillis = 150, easing = LinearOutSlowInEasing),
        label = "panY"
    )

    var showTopMenu by remember { mutableStateOf(false) }
    var selectedTopMenuIndex by remember { mutableIntStateOf(0) } // 0: Solo Fotos, 1: Fotos y Vídeos
    var slideshowMode by remember { mutableStateOf(initialSlideshowMode) }
    var isImageLoading by remember { mutableStateOf(true) }

    // Notificación toast al arrancar con modo inicial de diapositivas
    LaunchedEffect(Unit) {
        if (initialSlideshowMode == SlideshowMode.PHOTOS_ONLY) {
            onShowToast(strStartedPhotos)
        } else if (initialSlideshowMode == SlideshowMode.ALL) {
            onShowToast(strStartedAll)
        }
    }

    fun goToNext() {
        if (slideshowMode != SlideshowMode.NONE) {
            slideshowMode = SlideshowMode.NONE
            onShowToast(strPaused)
        }
        if (currentIndex < assets.size - 1) {
            val targetIdx = currentIndex + 1
            if (assets[targetIdx].isVideo) {
                onNavigateToVideo(targetIdx, SlideshowMode.NONE, slideshowIntervalSeconds)
            } else {
                currentIndex = targetIdx
                isZoomed = false
                panX = 0f
                panY = 0f
            }
        } else if (loopSlideshow && assets.isNotEmpty()) {
            val targetIdx = 0
            if (assets[targetIdx].isVideo) {
                onNavigateToVideo(targetIdx, SlideshowMode.NONE, slideshowIntervalSeconds)
            } else {
                currentIndex = targetIdx
                isZoomed = false
                panX = 0f
                panY = 0f
            }
        }
    }

    fun goToPrevious() {
        if (slideshowMode != SlideshowMode.NONE) {
            slideshowMode = SlideshowMode.NONE
            onShowToast(strPaused)
        }
        if (currentIndex > 0) {
            val targetIdx = currentIndex - 1
            if (assets[targetIdx].isVideo) {
                onNavigateToVideo(targetIdx, SlideshowMode.NONE, slideshowIntervalSeconds)
            } else {
                currentIndex = targetIdx
                isZoomed = false
                panX = 0f
                panY = 0f
            }
        } else if (loopSlideshow && assets.isNotEmpty()) {
            val targetIdx = assets.size - 1
            if (assets[targetIdx].isVideo) {
                onNavigateToVideo(targetIdx, SlideshowMode.NONE, slideshowIntervalSeconds)
            } else {
                currentIndex = targetIdx
                isZoomed = false
                panX = 0f
                panY = 0f
            }
        }
    }

    // CAPTURA GLOBAL DE TECLADO / MANDO DE TV INFALIBLE
    DisposableEffect(Unit) {
        GlobalKeyHandler.listener = { event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        if (isZoomed) {
                            panY = (panY + panStep).coerceIn(-maxPanY, maxPanY)
                        } else if (!showTopMenu) {
                            showTopMenu = true
                            selectedTopMenuIndex = 0
                        }
                        true
                    }

                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (isZoomed) {
                            panY = (panY - panStep).coerceIn(-maxPanY, maxPanY)
                        } else if (showTopMenu) {
                            showTopMenu = false
                        }
                        true
                    }

                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (showTopMenu) {
                            selectedTopMenuIndex = 0
                        } else if (isZoomed) {
                            panX = (panX + panStep).coerceIn(-maxPanX, maxPanX)
                        } else {
                            goToPrevious()
                        }
                        true
                    }

                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (showTopMenu) {
                            selectedTopMenuIndex = 1
                        } else if (isZoomed) {
                            panX = (panX - panStep).coerceIn(-maxPanX, maxPanX)
                        } else {
                            goToNext()
                        }
                        true
                    }

                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        if (event.repeatCount == 0) {
                            if (showTopMenu) {
                                showTopMenu = false
                                if (selectedTopMenuIndex == 0) {
                                    slideshowMode = SlideshowMode.PHOTOS_ONLY
                                    onShowToast(strStartedPhotos)
                                } else {
                                    slideshowMode = SlideshowMode.ALL
                                    onShowToast(strStartedAll)
                                }
                            } else {
                                isZoomed = !isZoomed
                                if (!isZoomed) {
                                    panX = 0f
                                    panY = 0f
                                }
                            }
                        }
                        true
                    }

                    KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                        if (event.repeatCount == 0) {
                            if (showTopMenu) {
                                showTopMenu = false
                            } else if (slideshowMode != SlideshowMode.NONE) {
                                slideshowMode = SlideshowMode.NONE
                                onShowToast(strPaused)
                            } else if (isZoomed) {
                                isZoomed = false
                                panX = 0f
                                panY = 0f
                            } else {
                                onClose(currentIndex)
                            }
                        }
                        true
                    }

                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        if (event.repeatCount == 0) {
                            if (slideshowMode == SlideshowMode.NONE) {
                                slideshowMode = SlideshowMode.PHOTOS_ONLY
                                onShowToast(strStartedPhotos)
                            } else {
                                slideshowMode = SlideshowMode.NONE
                                onShowToast(strPaused)
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

    // Precarga de fotos adyacentes
    LaunchedEffect(currentIndex, preloading) {
        if (preloading && assets.isNotEmpty()) {
            val nextIdx = (currentIndex + 1) % assets.size
            val prevIdx = (currentIndex - 1 + assets.size) % assets.size
            listOf(assets[nextIdx], assets[prevIdx]).forEach { item ->
                if (!item.isVideo) {
                    val request = ImageRequest.Builder(context)
                        .data(item.url)
                        .size(Size.ORIGINAL)
                        .precision(Precision.INEXACT)
                        .build()
                    context.imageLoader.enqueue(request)
                }
            }
        }
    }
    // Precarga de la versión Fullsize en 4K Ultra HD para zoom instantáneo
    LaunchedEffect(currentAsset?.id) {
        val fullUrl = currentAsset?.fullsizeUrl
        if (!fullUrl.isNullOrBlank()) {
            val request = ImageRequest.Builder(context)
                .data(fullUrl)
                .size(3840, 2160)
                .precision(Precision.INEXACT)
                .allowHardware(true)
                .build()
            context.imageLoader.enqueue(request)
        }
    }

    // Motor de Pase de Diapositivas
    LaunchedEffect(currentIndex, slideshowMode) {
        if (slideshowMode != SlideshowMode.NONE && assets.isNotEmpty()) {
            delay(slideshowIntervalSeconds * 1000L)

            var foundNext = false
            for (step in 1..assets.size) {
                val nextIdx = (currentIndex + step) % assets.size
                if (!loopSlideshow && nextIdx < currentIndex) {
                    slideshowMode = SlideshowMode.NONE
                    onShowToast(strEnded)
                    return@LaunchedEffect
                }

                val nextAsset = assets[nextIdx]
                if (slideshowMode == SlideshowMode.PHOTOS_ONLY) {
                    if (!nextAsset.isVideo) {
                        currentIndex = nextIdx
                        isZoomed = false
                        panX = 0f
                        panY = 0f
                        foundNext = true
                        break
                    }
                } else {
                    if (nextAsset.isVideo) {
                        onNavigateToVideo(nextIdx, slideshowMode, slideshowIntervalSeconds)
                    } else {
                        currentIndex = nextIdx
                        isZoomed = false
                        panX = 0f
                        panY = 0f
                    }
                    foundNext = true
                    break
                }
            }

            if (!foundNext) {
                slideshowMode = SlideshowMode.NONE
                onShowToast(strNoMore)
            }
        }
    }

    // Formateadores de metadatos EXIF
    val formattedDate = remember(currentAsset?.createdAt) {
        currentAsset?.createdAt?.let { dateStr ->
            try {
                val cleanStr = dateStr.substringBefore('.').substringBefore('Z')
                val patterns = listOf(
                    "yyyy-MM-dd'T'HH:mm:ss",
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy:MM:dd HH:mm:ss",
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
        ).joinToString(" ").ifBlank {
            currentAsset?.exifModel ?: currentAsset?.exifMake ?: ""
        }
    }

    val shootingDetails = remember(currentAsset) {
        listOfNotNull(
            currentAsset?.exifFocalLength?.let { "${it.toInt()}mm" },
            currentAsset?.exifFNumber?.let { "f/$it" },
            currentAsset?.exifExposureTime?.takeIf { it.isNotBlank() },
            currentAsset?.exifIso?.let { "ISO $it" }
        ).joinToString(" · ")
    }

    val locationInfo = remember(currentAsset) {
        listOfNotNull(
            currentAsset?.exifCity?.takeIf { it.isNotBlank() },
            currentAsset?.exifCountry?.takeIf { it.isNotBlank() }
        ).joinToString(", ")
    }

    val dimensionsInfo = remember(currentAsset) {
        if (currentAsset?.width != null && currentAsset.height != null && currentAsset.width > 0 && currentAsset.height > 0) {
            val mp = (currentAsset.width.toLong() * currentAsset.height.toLong()) / 1_000_000.0
            if (mp >= 1.0) {
                "${currentAsset.width} × ${currentAsset.height} (${String.format(Locale.US, "%.1f", mp)} MP)"
            } else {
                "${currentAsset.width} × ${currentAsset.height}"
            }
        } else {
            ""
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isZoomed) {
                if (isZoomed) {
                    var totalDrag = Offset.Zero
                    detectDragGestures(
                        onDragStart = { totalDrag = Offset.Zero },
                        onDragEnd = {},
                        onDrag = { change, dragAmount ->
                            change.consume()
                            totalDrag += dragAmount
                            panX = (panX + dragAmount.x).coerceIn(-maxPanX, maxPanX)
                            panY = (panY + dragAmount.y).coerceIn(-maxPanY, maxPanY)
                        }
                    )
                } else {
                    var totalDrag = Offset.Zero
                    detectDragGestures(
                        onDragStart = { totalDrag = Offset.Zero },
                        onDragEnd = {
                            if (totalDrag.x > 100) goToPrevious()
                            else if (totalDrag.x < -100) goToNext()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            totalDrag += dragAmount
                        }
                    )
                }
            }
    ) {
        // --- IMAGEN PRINCIPAL (Nítida y Ultra Fluida con Fullsize) ---
        if (currentAsset != null) {
            val activeImageUrl = if (isZoomed && !currentAsset.fullsizeUrl.isNullOrBlank()) {
                currentAsset.fullsizeUrl
            } else {
                currentAsset.url
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(activeImageUrl)
                        .size(3840, 2160)
                        .precision(Precision.INEXACT)
                        .allowHardware(true)
                        .allowRgb565(false)
                        .crossfade(100)
                        .placeholderMemoryCacheKey(currentAsset.url)
                        .listener(
                            onStart = { isImageLoading = true },
                            onSuccess = { _, _ -> isImageLoading = false },
                            onError = { _, _ -> isImageLoading = false }
                        )
                        .build(),
                    contentDescription = currentAsset.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = animatedScale
                            scaleY = animatedScale
                            translationX = animatedPanX
                            translationY = animatedPanY
                        }
                )

                // Spinner de carga si es necesario
                if (isImageLoading) {
                    LoadingSpinner(
                        modifier = Modifier.align(Alignment.Center),
                        size = 56.dp
                    )
                }
            }
        }

        // --- MINI-MAPA DE ZOOM ---
        AnimatedVisibility(
            visible = isZoomed,
            enter = fadeIn(tween(250)) + scaleIn(initialScale = 0.8f),
            exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.8f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp, end = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 136.dp, height = 90.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.82f))
                    .border(1.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
            ) {
                AsyncImage(
                    model = currentAsset?.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                val viewportW = (136f / zoomScale).dp
                val viewportH = (90f / zoomScale).dp
                val normX = (panX / maxPanX).coerceIn(-1f, 1f)
                val normY = (panY / maxPanY).coerceIn(-1f, 1f)
                val offsetX = (-(normX * ((136 - 136 / zoomScale) / 2))).dp
                val offsetY = (-(normY * ((90 - 90 / zoomScale) / 2))).dp

                Box(
                    modifier = Modifier
                        .size(width = viewportW, height = viewportH)
                        .align(Alignment.Center)
                        .offset(x = offsetX, y = offsetY)
                        .border(2.dp, FocusHighlight, RoundedCornerShape(3.dp))
                        .background(FocusHighlight.copy(alpha = 0.28f))
                )
            }
        }

        // --- OVERLAY EXIF PURO (Esquina Inferior Izquierda) ---
        if (showMetadata && currentAsset != null && !isZoomed) {
            val hasAnyExif = (showDateInfo && formattedDate.isNotBlank()) ||
                    (showCameraInfo && cameraInfo.isNotBlank()) ||
                    (showExifDetails && shootingDetails.isNotBlank()) ||
                    (showLocationInfo && locationInfo.isNotBlank())

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

                    // Parámetros de Disparo (Velocidad, Apertura, ISO, Focal)
                    if (showExifDetails && shootingDetails.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = shootingDetails,
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
        if (showCounter && !isZoomed && assets.isNotEmpty()) {
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

        // --- MENÚ SUPERIOR DESPLEGABLE (PASES DE DIAPOSITIVAS) ---
        AnimatedVisibility(
            visible = showTopMenu,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(OverlayDark)
                    .border(1.5.dp, FocusHighlight, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.slideshow_menu_title),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = FocusHighlight,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SlideshowOptionCard(
                        text = stringResource(R.string.slideshow_type_photos),
                        icon = Icons.Rounded.Collections,
                        isSelected = selectedTopMenuIndex == 0,
                        onClick = {
                            showTopMenu = false
                            slideshowMode = SlideshowMode.PHOTOS_ONLY
                            onShowToast(strStartedPhotos)
                        }
                    )

                    SlideshowOptionCard(
                        text = stringResource(R.string.slideshow_type_all),
                        icon = Icons.Rounded.PhotoLibrary,
                        isSelected = selectedTopMenuIndex == 1,
                        onClick = {
                            showTopMenu = false
                            slideshowMode = SlideshowMode.ALL
                            onShowToast(strStartedAll)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SlideshowOptionCard(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .scale(if (isSelected) 1.08f else 1.0f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) ImmichBlue else BackgroundElevated)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) FocusHighlight else Color(0x33FFFFFF),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else TextSecondary
                )
            )
        }
    }
}

