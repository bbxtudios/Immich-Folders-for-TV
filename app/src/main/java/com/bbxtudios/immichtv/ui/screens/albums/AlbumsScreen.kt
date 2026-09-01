package com.bbxtudios.immichtv.ui.screens.albums

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bbxtudios.immichtv.util.LocalizedDialog
import java.util.Locale
import androidx.compose.ui.zIndex
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.itemsIndexed
import androidx.tv.foundation.lazy.grid.rememberTvLazyGridState
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bbxtudios.immichtv.AppContainer
import com.bbxtudios.immichtv.data.model.AlbumDetail
import com.bbxtudios.immichtv.data.model.AlbumItem
import com.bbxtudios.immichtv.data.model.GridItem
import com.bbxtudios.immichtv.data.model.ViewAsset
import com.bbxtudios.immichtv.ui.components.AssetCard
import com.bbxtudios.immichtv.ui.components.DateGroupHeader
import com.bbxtudios.immichtv.ui.components.LoadingSpinner
import com.bbxtudios.immichtv.ui.screens.settings.SettingRadioPill
import com.bbxtudios.immichtv.ui.screens.settings.SettingTvButton
import com.bbxtudios.immichtv.ui.screens.viewer.SlideshowMode
import com.bbxtudios.immichtv.ui.theme.AccentCyan
import com.bbxtudios.immichtv.ui.theme.BackgroundDark
import com.bbxtudios.immichtv.ui.theme.BackgroundElevated
import com.bbxtudios.immichtv.ui.theme.BackgroundSurface
import com.bbxtudios.immichtv.ui.theme.FocusHighlight
import com.bbxtudios.immichtv.ui.theme.ImmichBlue
import com.bbxtudios.immichtv.ui.theme.TextMuted
import com.bbxtudios.immichtv.ui.theme.TextPrimary
import com.bbxtudios.immichtv.ui.theme.TextSecondary
import androidx.compose.ui.res.stringResource
import com.bbxtudios.immichtv.R
import com.bbxtudios.immichtv.ui.util.InstantBringIntoViewSpec
import com.bbxtudios.immichtv.ui.util.SmoothBringIntoViewSpec
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AlbumsScreen(
    viewModel: AlbumsViewModel,
    lastViewedAssetId: String? = null,
    focusTrigger: Int = 0,
    onAssetClick: (assets: List<ViewAsset>, index: Int) -> Unit,
    onStartSlideshow: (assets: List<ViewAsset>, startIndex: Int, mode: SlideshowMode, intervalSeconds: Int) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val lastExitedAlbumId = viewModel.getLastExitedAlbumId()

    val settingsRepo = remember { AppContainer.settingsRepository }
    val gridColumns by settingsRepo.gridColumns.collectAsState(initial = 5)
    val animFocus by settingsRepo.animFocus.collectAsState(initial = true)
    val cardShadows by settingsRepo.cardShadows.collectAsState(initial = true)
    val smoothScroll by settingsRepo.smoothScroll.collectAsState(initial = true)
    val slideshowIntervalSetting by settingsRepo.slideshowInterval.collectAsState(initial = 3)

    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberTvLazyGridState()

    var showSlideshowDialog by remember { mutableStateOf(false) }
    var slideshowContentMode by remember { mutableStateOf(SlideshowMode.PHOTOS_ONLY) }
    var slideshowFromStart by remember { mutableStateOf(false) }
    var tempIntervalSeconds by remember(slideshowIntervalSetting) { mutableIntStateOf(slideshowIntervalSetting) }

    var showCalendarDialog by remember { mutableStateOf(false) }
    var showGridDialog by remember { mutableStateOf(false) }

    val firstItemFocusRequester = remember { FocusRequester() }
    val restoredItemFocusRequester = remember { FocusRequester() }
    val topBarFocusRequester = remember { FocusRequester() }
    var lastDpadMoveTime by remember { mutableLongStateOf(0L) }

    var renderedAlbumId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState, focusTrigger, lastViewedAssetId) {
        if (uiState is AlbumsUiState.InsideAlbum) {
            val album = (uiState as AlbumsUiState.InsideAlbum).album
            val isAlbumChange = renderedAlbumId != album.id

            delay(60)
            try {
                if (isAlbumChange) {
                    renderedAlbumId = album.id
                    gridState.scrollToItem(0)
                    delay(50)
                    firstItemFocusRequester.requestFocus()
                } else if (lastViewedAssetId != null && album.allAssets.any { it.id == lastViewedAssetId }) {
                    val targetAssetIdx = album.allAssets.indexOfFirst { it.id == lastViewedAssetId }
                    if (targetAssetIdx >= 0) {
                        gridState.scrollToItem(targetAssetIdx)
                        delay(50)
                        restoredItemFocusRequester.requestFocus()
                    }
                } else if (focusTrigger > 0) {
                    firstItemFocusRequester.requestFocus()
                }
            } catch (_: Exception) {}
        } else if (uiState is AlbumsUiState.AlbumList) {
            renderedAlbumId = null
            delay(60)
            if (lastExitedAlbumId != null) {
                val albums = (uiState as AlbumsUiState.AlbumList).albums
                val idx = albums.indexOfFirst { it.id == lastExitedAlbumId }
                if (idx >= 0) {
                    gridState.scrollToItem(idx)
                    delay(50)
                    restoredItemFocusRequester.requestFocus()
                } else {
                    firstItemFocusRequester.requestFocus()
                }
                viewModel.clearLastExitedAlbumId()
            } else {
                firstItemFocusRequester.requestFocus()
            }
        }
    }

    // --- MODAL 1: INICIAR PASE DE DIAPOSITIVAS (IDÉNTICO A CARPETAS) ---
    if (showSlideshowDialog && uiState is AlbumsUiState.InsideAlbum) {
        val album = (uiState as AlbumsUiState.InsideAlbum).album
        val targetFiles = if (slideshowContentMode == SlideshowMode.PHOTOS_ONLY) {
            album.allAssets.filter { !it.isVideo }
        } else {
            album.allAssets
        }

        val previewAsset = if (slideshowFromStart) {
            targetFiles.firstOrNull()
        } else {
            val current = if (lastViewedAssetId != null) targetFiles.firstOrNull { it.id == lastViewedAssetId } else null
            current ?: targetFiles.firstOrNull()
        }

        LocalizedDialog(onDismissRequest = { showSlideshowDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(BackgroundDark)
                    .border(2.dp, FocusHighlight.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .padding(28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFFB8C00).copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFFFB8C00),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.slideshow_dialog_title),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = stringResource(R.string.slideshow_type_label),
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = stringResource(R.string.slideshow_type_label) + ":",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingRadioPill(
                            label = stringResource(R.string.slideshow_type_photos),
                            selected = slideshowContentMode == SlideshowMode.PHOTOS_ONLY,
                            onClick = { slideshowContentMode = SlideshowMode.PHOTOS_ONLY }
                        )
                        SettingRadioPill(
                            label = stringResource(R.string.slideshow_type_all),
                            selected = slideshowContentMode == SlideshowMode.ALL,
                            onClick = { slideshowContentMode = SlideshowMode.ALL }
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = stringResource(R.string.slideshow_start_label) + ":",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SettingRadioPill(
                                label = stringResource(R.string.slideshow_start_current),
                                selected = !slideshowFromStart,
                                onClick = { slideshowFromStart = false }
                            )
                            SettingRadioPill(
                                label = stringResource(R.string.slideshow_start_first),
                                selected = slideshowFromStart,
                                onClick = { slideshowFromStart = true }
                            )
                        }

                        if (previewAsset != null) {
                            Box(
                                modifier = Modifier
                                    .width(130.dp)
                                    .height(80.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BackgroundElevated)
                                    .border(1.5.dp, FocusHighlight.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(previewAsset.thumbnailUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .background(Color(0xCC000000))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (slideshowFromStart) stringResource(R.string.slideshow_start_first) else stringResource(R.string.slideshow_start_current),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (previewAsset.isVideo) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(20.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xCC000000)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = stringResource(R.string.slideshow_interval_label) + ":",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(3, 5, 10, 15).forEach { sec ->
                            SettingRadioPill(
                                label = stringResource(R.string.slideshow_interval_seconds, sec),
                                selected = tempIntervalSeconds == sec,
                                onClick = { tempIntervalSeconds = sec }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    SettingTvButton(
                        text = stringResource(R.string.slideshow_start_btn) + " (${tempIntervalSeconds}s)",
                        icon = Icons.Rounded.PlayArrow,
                        onClick = {
                            if (targetFiles.isNotEmpty()) {
                                val startIdx = if (slideshowFromStart) 0 else {
                                    val currentAssetIdx = if (lastViewedAssetId != null) {
                                        targetFiles.firstOrNull { it.id == lastViewedAssetId }?.let { targetFiles.indexOf(it) } ?: -1
                                    } else -1
                                    if (currentAssetIdx >= 0) currentAssetIdx else 0
                                }
                                showSlideshowDialog = false
                                onStartSlideshow(targetFiles, startIdx, slideshowContentMode, tempIntervalSeconds)
                            }
                        }
                    )
                }
            }
        }
    }

    // --- MODAL 2: SALTO A FECHA (CALENDARIO) CON SCROLL AUTOMÁTICO (IDÉNTICO A CARPETAS) ---
    if (showCalendarDialog && uiState is AlbumsUiState.InsideAlbum) {
        val album = (uiState as AlbumsUiState.InsideAlbum).album
        val dateGroups = album.dateGroups
        LocalizedDialog(onDismissRequest = { showCalendarDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.84f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(BackgroundDark)
                    .border(2.dp, FocusHighlight.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .padding(28.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(AccentCyan.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.calendar_dialog_title),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = stringResource(R.string.calendar_dialog_subtitle),
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (dateGroups.isEmpty()) {
                        Text(stringResource(R.string.calendar_no_dates), color = TextSecondary)
                    } else {
                        val scroll = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .verticalScroll(scroll),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            dateGroups.forEach { group ->
                                val itemInteraction = remember { MutableInteractionSource() }
                                val isFocused by itemInteraction.collectIsFocusedAsState()

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .scale(if (isFocused) 1.02f else 1.0f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isFocused) ImmichBlue else BackgroundElevated)
                                        .border(
                                            width = if (isFocused) 2.dp else 1.dp,
                                            color = if (isFocused) FocusHighlight else Color(0x22FFFFFF),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable(
                                            interactionSource = itemInteraction,
                                            indication = null,
                                            onClick = {
                                                showCalendarDialog = false
                                                coroutineScope.launch {
                                                    val firstAsset = group.assets.firstOrNull()
                                                    if (firstAsset != null) {
                                                        // Encontrar el índice del DateHeader o del primer asset
                                                        var targetIndex = 0
                                                        for (g in dateGroups) {
                                                            if (g.dateKey == group.dateKey) {
                                                                break
                                                            }
                                                            targetIndex += 1 + g.assets.size
                                                        }
                                                        gridState.scrollToItem(targetIndex)
                                                    }
                                                }
                                            }
                                        )
                                        .onKeyEvent { keyEvent ->
                                            if (keyEvent.type == KeyEventType.KeyUp &&
                                                (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                            ) {
                                                showCalendarDialog = false
                                                coroutineScope.launch {
                                                    val firstAsset = group.assets.firstOrNull()
                                                    if (firstAsset != null) {
                                                        var targetIndex = 0
                                                        for (g in dateGroups) {
                                                            if (g.dateKey == group.dateKey) {
                                                                break
                                                            }
                                                            targetIndex += 1 + g.assets.size
                                                        }
                                                        gridState.scrollToItem(targetIndex)
                                                    }
                                                }
                                                true
                                            } else false
                                        }
                                        .focusable(interactionSource = itemInteraction)
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = AppContainer.immichRepository.formatDateGroupTitle(group.dateKey, Locale.getDefault()),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                        )
                                        Text(
                                            text = stringResource(R.string.items_count, group.assets.size),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = TextSecondary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- MODAL 3: SELECTOR DE CUADRÍCULA (IDÉNTICO A CARPETAS) ---
    if (showGridDialog) {
        LocalizedDialog(onDismissRequest = { showGridDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(BackgroundDark)
                    .border(2.dp, FocusHighlight.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .padding(28.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF0288D1).copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GridView,
                                contentDescription = null,
                                tint = Color(0xFF0288D1),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.columns_dialog_title),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = stringResource(R.string.settings_cat_grid_desc),
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(
                            4 to "4",
                            5 to "5",
                            6 to "6",
                            7 to "7"
                        ).forEach { (cols, _) ->
                            Box(modifier = Modifier.weight(1f)) {
                                SettingRadioPill(
                                    label = stringResource(R.string.columns_count, cols),
                                    selected = gridColumns == cols,
                                    onClick = {
                                        showGridDialog = false
                                        coroutineScope.launch {
                                            settingsRepo.setGridColumns(cols)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(top = 16.dp, start = 20.dp, end = 20.dp)
    ) {
        // BARRA SUPERIOR (IDÉNTICA EN ESTRUCTURA Y BOTONES A FOLDERS SCREEN)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            when (val state = uiState) {
                is AlbumsUiState.InsideAlbum -> {
                    // Izquierda: Botón Volver + Título
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val backInteraction = remember { MutableInteractionSource() }
                        val isBackFocused by backInteraction.collectIsFocusedAsState()

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isBackFocused) ImmichBlue else BackgroundElevated)
                                .border(
                                    width = if (isBackFocused) 2.dp else 1.dp,
                                    color = if (isBackFocused) FocusHighlight else Color(0x33FFFFFF),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .focusRequester(topBarFocusRequester)
                                .clickable(
                                    interactionSource = backInteraction,
                                    indication = null,
                                    onClick = { viewModel.navigateUp() }
                                )
                                .focusable(interactionSource = backInteraction)
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyUp &&
                                        (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                    ) {
                                        viewModel.navigateUp()
                                        true
                                    } else false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = stringResource(R.string.player_prev),
                                    tint = if (isBackFocused) FocusHighlight else TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.player_prev),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isBackFocused) FocusHighlight else TextPrimary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = state.album.name,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.items_count_detail, state.album.assetCount),
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }

                    // Derecha: Fechas | Cuadrícula | Diapositivas | Recargar
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // 1. Botón Fechas
                        val calInteraction = remember { MutableInteractionSource() }
                        val isCalFocused by calInteraction.collectIsFocusedAsState()

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCalFocused) ImmichBlue else BackgroundElevated)
                                .border(
                                    width = if (isCalFocused) 2.dp else 1.dp,
                                    color = if (isCalFocused) FocusHighlight else Color(0x33FFFFFF),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable(
                                    interactionSource = calInteraction,
                                    indication = null,
                                    onClick = { showCalendarDialog = true }
                                )
                                .focusable(interactionSource = calInteraction)
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyUp &&
                                        (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                    ) {
                                        showCalendarDialog = true
                                        true
                                    } else false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.CalendarMonth,
                                    contentDescription = stringResource(R.string.action_calendar),
                                    tint = AccentCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.action_calendar),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                )
                            }
                        }

                        // 2. Botón Cuadrícula
                        val gridInteraction = remember { MutableInteractionSource() }
                        val isGridFocused by gridInteraction.collectIsFocusedAsState()

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isGridFocused) ImmichBlue else BackgroundElevated)
                                .border(
                                    width = if (isGridFocused) 2.dp else 1.dp,
                                    color = if (isGridFocused) FocusHighlight else Color(0x33FFFFFF),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable(
                                    interactionSource = gridInteraction,
                                    indication = null,
                                    onClick = { showGridDialog = true }
                                )
                                .focusable(interactionSource = gridInteraction)
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyUp &&
                                        (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                    ) {
                                        showGridDialog = true
                                        true
                                    } else false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.GridView,
                                    contentDescription = stringResource(R.string.action_columns),
                                    tint = FocusHighlight,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.columns_count, gridColumns),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                )
                            }
                        }

                        // 3. Botón Diapositivas
                        if (state.album.allAssets.isNotEmpty()) {
                            val slideInteraction = remember { MutableInteractionSource() }
                            val isSlideFocused by slideInteraction.collectIsFocusedAsState()

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSlideFocused) ImmichBlue else BackgroundElevated)
                                    .border(
                                        width = if (isSlideFocused) 2.dp else 1.dp,
                                        color = if (isSlideFocused) FocusHighlight else Color(0x33FFFFFF),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable(
                                        interactionSource = slideInteraction,
                                        indication = null,
                                        onClick = { showSlideshowDialog = true }
                                    )
                                    .focusable(interactionSource = slideInteraction)
                                    .onKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyUp &&
                                            (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                        ) {
                                            showSlideshowDialog = true
                                            true
                                        } else false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = stringResource(R.string.action_slideshow),
                                        tint = Color(0xFFFB8C00),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.action_slideshow),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                    )
                                }
                            }
                        }

                        // 4. Botón Recargar
                        val refreshInteractionSource = remember { MutableInteractionSource() }
                        val isRefreshFocused by refreshInteractionSource.collectIsFocusedAsState()

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isRefreshFocused) ImmichBlue else BackgroundElevated)
                                .border(
                                    width = if (isRefreshFocused) 2.dp else 1.dp,
                                    color = if (isRefreshFocused) FocusHighlight else Color(0x33FFFFFF),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable(
                                    interactionSource = refreshInteractionSource,
                                    indication = null,
                                    onClick = { viewModel.reload() }
                                )
                                .focusable(interactionSource = refreshInteractionSource)
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyUp &&
                                        (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                    ) {
                                        viewModel.reload()
                                        true
                                    } else false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = stringResource(R.string.retry),
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.retry),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                )
                            }
                        }
                    }
                }

                else -> {
                    // Vista General de Lista de Álbumes
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoAlbum,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.albums_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        if (uiState is AlbumsUiState.AlbumList) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "(${stringResource(R.string.items_count, (uiState as AlbumsUiState.AlbumList).albums.size)})",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                            )
                        }
                    }

                    // Botón Recargar
                    val refreshInteractionSource = remember { MutableInteractionSource() }
                    val isRefreshFocused by refreshInteractionSource.collectIsFocusedAsState()

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isRefreshFocused) ImmichBlue else BackgroundElevated)
                            .border(
                                width = if (isRefreshFocused) 2.dp else 1.dp,
                                color = if (isRefreshFocused) FocusHighlight else Color(0x33FFFFFF),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .focusRequester(topBarFocusRequester)
                            .clickable(
                                interactionSource = refreshInteractionSource,
                                indication = null,
                                onClick = { viewModel.loadAlbums(isRefresh = true) }
                            )
                            .focusable(interactionSource = refreshInteractionSource)
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyUp &&
                                    (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                ) {
                                    viewModel.loadAlbums(isRefresh = true)
                                    true
                                } else false
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = stringResource(R.string.btn_reload),
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.btn_reload),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            )
                        }
                    }
                }
            }
        }

        // CONTENIDO PRINCIPAL SEGÚN ESTADO (IDÉNTICO A FOLDERS SCREEN CON TV LAZY VERTICAL GRID Y GRIDITEM)
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is AlbumsUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        LoadingSpinner(size = 56.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        val loadingText = if (state.albumName != null) {
                            stringResource(R.string.loading_folder, state.albumName)
                        } else {
                            stringResource(R.string.loading_content)
                        }
                        Text(
                            text = loadingText,
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                    }
                }

                is AlbumsUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.error_loading),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        ElevatedButton(
                            onClick = { viewModel.reload() },
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = ImmichBlue,
                                contentColor = TextPrimary
                            )
                        ) {
                            Text(text = stringResource(R.string.retry), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is AlbumsUiState.AlbumList -> {
                    val albums = state.albums
                    if (albums.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.empty_albums),
                                style = MaterialTheme.typography.bodyLarge.copy(color = TextSecondary)
                            )
                        }
                    } else {
                        @OptIn(ExperimentalFoundationApi::class)
                        CompositionLocalProvider(
                            LocalBringIntoViewSpec provides (if (!smoothScroll) InstantBringIntoViewSpec else SmoothBringIntoViewSpec)
                        ) {
                            TvLazyVerticalGrid(
                                state = gridState,
                                columns = TvGridCells.Fixed(gridColumns),
                                pivotOffsets = PivotOffsets(parentFraction = 0.25f, childFraction = 0.0f),
                                contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp, start = 4.dp, end = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .onPreviewKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown &&
                                            (keyEvent.key == Key.DirectionDown || keyEvent.key == Key.DirectionUp)
                                        ) {
                                            val now = System.currentTimeMillis()
                                            if (now - lastDpadMoveTime < 60L) {
                                                return@onPreviewKeyEvent true
                                            }
                                            lastDpadMoveTime = now
                                        }
                                        false
                                    }
                            ) {
                                itemsIndexed(
                                    items = albums,
                                    key = { _, album -> album.id }
                                ) { index, album ->
                                    val isFirstFocusable = index == 0
                                    val isFirstRow = index < gridColumns
                                    val isTargetAlbum = album.id == lastExitedAlbumId
                                    val effectiveReq = when {
                                        isTargetAlbum    -> restoredItemFocusRequester
                                        isFirstFocusable -> firstItemFocusRequester
                                        else             -> null
                                    }

                                    val rowFocusModifier = Modifier.focusProperties {
                                        if (isFirstRow) {
                                            up = topBarFocusRequester
                                        }
                                    }

                                    AlbumCard(
                                        album = album,
                                        animFocus = animFocus,
                                        cardShadows = cardShadows,
                                        focusRequester = effectiveReq,
                                        onClick = { viewModel.openAlbum(album.id) },
                                        modifier = Modifier
                                            .then(rowFocusModifier)
                                            .fillMaxWidth()
                                            .aspectRatio(1.05f)
                                    )
                                }
                            }
                        }
                    }
                }

                is AlbumsUiState.InsideAlbum -> {
                    val album = state.album
                    if (album.allAssets.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.empty_media),
                                style = MaterialTheme.typography.titleMedium.copy(color = TextMuted)
                            )
                        }
                    } else {
                        val currentLocale = Locale.getDefault()
                        val gridItems = remember(album, currentLocale) {
                            buildList<GridItem> {
                                album.dateGroups.forEach { group ->
                                    val dateTitle = AppContainer.immichRepository.formatDateGroupTitle(group.dateKey, currentLocale)
                                    if (dateTitle.isNotBlank() && group.assets.isNotEmpty()) {
                                        add(GridItem.DateHeader(title = dateTitle, dateKey = group.dateKey))
                                    }
                                    group.assets.forEach { asset ->
                                        val idx = album.allAssets.indexOfFirst { it.id == asset.id }.coerceAtLeast(0)
                                        add(GridItem.Asset(asset, idx))
                                    }
                                }
                            }
                        }

                        val firstFocusableIndex = remember(gridItems) {
                            gridItems.indexOfFirst { it is GridItem.Asset }
                        }

                        val assetIdToGroupTitle = remember(album.dateGroups, currentLocale) {
                            buildMap<String, String> {
                                for (group in album.dateGroups) {
                                    val dateTitle = AppContainer.immichRepository.formatDateGroupTitle(group.dateKey, currentLocale)
                                    for (asset in group.assets) {
                                        put(asset.id, dateTitle)
                                    }
                                }
                            }
                        }

                        val currentStickyDateTitle by remember(album.dateGroups, assetIdToGroupTitle) {
                            derivedStateOf {
                                try {
                                    val visibleItems = gridState.layoutInfo.visibleItemsInfo
                                    if (visibleItems.isEmpty()) return@derivedStateOf null
                                    val firstVisibleAsset = visibleItems.firstOrNull { it.contentType == "asset" } ?: return@derivedStateOf null
                                    assetIdToGroupTitle[firstVisibleAsset.key.toString()]
                                } catch (_: Exception) {
                                    null
                                }
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            @OptIn(ExperimentalFoundationApi::class)
                            CompositionLocalProvider(
                                LocalBringIntoViewSpec provides (if (!smoothScroll) InstantBringIntoViewSpec else SmoothBringIntoViewSpec)
                            ) {
                                TvLazyVerticalGrid(
                                    state = gridState,
                                    columns = TvGridCells.Fixed(gridColumns),
                                    pivotOffsets = PivotOffsets(parentFraction = 0.25f, childFraction = 0.0f),
                                    contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp, start = 4.dp, end = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .onPreviewKeyEvent { keyEvent ->
                                            if (keyEvent.type == KeyEventType.KeyDown &&
                                                (keyEvent.key == Key.DirectionDown || keyEvent.key == Key.DirectionUp)
                                            ) {
                                                val now = System.currentTimeMillis()
                                                if (now - lastDpadMoveTime < 60L) {
                                                    return@onPreviewKeyEvent true
                                                }
                                                lastDpadMoveTime = now
                                            }
                                            false
                                        }
                                ) {
                                    itemsIndexed(
                                        items = gridItems,
                                        key = { _, item ->
                                            when (item) {
                                                is GridItem.Folder     -> item.item.originalPath
                                                is GridItem.DateHeader -> "header_${item.dateKey}"
                                                is GridItem.Asset      -> item.item.id
                                            }
                                        },
                                        span = { _, item ->
                                            when (item) {
                                                is GridItem.DateHeader -> androidx.tv.foundation.lazy.grid.TvGridItemSpan(maxLineSpan)
                                                else -> androidx.tv.foundation.lazy.grid.TvGridItemSpan(1)
                                            }
                                        },
                                        contentType = { _, item ->
                                            when (item) {
                                                is GridItem.Folder     -> "folder"
                                                is GridItem.DateHeader -> "header"
                                                is GridItem.Asset      -> "asset"
                                            }
                                        }
                                    ) { index, item ->
                                        val isFirstFocusable = index == firstFocusableIndex
                                        val isFirstRow = index < gridColumns
                                        val isTargetAsset = item is GridItem.Asset && item.item.id == lastViewedAssetId
                                        val effectiveReq = when {
                                            isTargetAsset    -> restoredItemFocusRequester
                                            isFirstFocusable -> firstItemFocusRequester
                                            else             -> null
                                        }

                                        val rowFocusModifier = Modifier.focusProperties {
                                            if (isFirstRow) {
                                                up = topBarFocusRequester
                                            }
                                        }

                                        when (item) {
                                            is GridItem.DateHeader -> DateGroupHeader(
                                                title = AppContainer.immichRepository.formatDateGroupTitle(item.dateKey, Locale.getDefault()),
                                                modifier = Modifier.focusProperties { canFocus = false }
                                            )
                                            is GridItem.Asset -> AssetCard(
                                                asset = item.item,
                                                onClick = { onAssetClick(album.allAssets, item.indexInFiles) },
                                                focusRequester = effectiveReq,
                                                animFocus = animFocus,
                                                cardShadows = cardShadows,
                                                modifier = Modifier
                                                    .then(rowFocusModifier)
                                                    .then(if (effectiveReq != null) Modifier.focusRequester(effectiveReq) else Modifier)
                                                    .fillMaxWidth()
                                                    .aspectRatio(1.0f)
                                            )
                                            else -> {}
                                        }
                                    }
                                }
                            }

                            // Sticky Date Header Flotante
                            val stickyTitle = currentStickyDateTitle
                            if (stickyTitle != null) {
                                Box(
                                    modifier = Modifier
                                        .focusProperties { canFocus = false }
                                        .align(Alignment.TopCenter)
                                        .padding(top = 8.dp)
                                        .zIndex(20f)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xE61E2638))
                                        .border(1.dp, Color(0x444488FF), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 20.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = stickyTitle,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumCard(
    album: AlbumItem,
    animFocus: Boolean,
    cardShadows: Boolean,
    focusRequester: FocusRequester?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused && animFocus) 1.05f else 1.0f,
        animationSpec = tween(150),
        label = "albumCardScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(BackgroundSurface)
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) FocusHighlight else Color(0x22FFFFFF),
                shape = RoundedCornerShape(14.dp)
            )
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp &&
                    (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                ) {
                    onClick()
                    true
                } else false
            }
    ) {
        if (!album.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(album.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = album.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(BackgroundElevated, ImmichBlue.copy(alpha = 0.4f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhotoAlbum,
                    contentDescription = null,
                    tint = AccentCyan.copy(alpha = 0.6f),
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                        startY = 100f
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isFocused) FocusHighlight else TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.items_count, album.assetCount),
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )
        }
    }
}
