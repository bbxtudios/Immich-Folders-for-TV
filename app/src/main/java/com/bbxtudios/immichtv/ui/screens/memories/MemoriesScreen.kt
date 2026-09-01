package com.bbxtudios.immichtv.ui.screens.memories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.itemsIndexed
import androidx.tv.foundation.lazy.grid.rememberTvLazyGridState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.CompositionLocalProvider
import com.bbxtudios.immichtv.ui.util.InstantBringIntoViewSpec
import com.bbxtudios.immichtv.ui.util.SmoothBringIntoViewSpec
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SentimentDissatisfied
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.bbxtudios.immichtv.R
import com.bbxtudios.immichtv.data.model.ViewAsset
import com.bbxtudios.immichtv.ui.components.AssetCard
import com.bbxtudios.immichtv.ui.components.DateGroupHeader
import com.bbxtudios.immichtv.ui.components.LoadingSpinner
import com.bbxtudios.immichtv.ui.theme.AccentCyan
import com.bbxtudios.immichtv.ui.theme.BackgroundDark
import com.bbxtudios.immichtv.ui.theme.BackgroundElevated
import com.bbxtudios.immichtv.ui.theme.FocusHighlight
import com.bbxtudios.immichtv.ui.theme.ImmichBlue
import com.bbxtudios.immichtv.ui.theme.TextMuted
import com.bbxtudios.immichtv.ui.theme.TextPrimary
import com.bbxtudios.immichtv.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun MemoriesScreen(
    viewModel: MemoriesViewModel,
    lastViewedAssetId: String?,
    focusTrigger: Int = 0,
    onAssetClick: (List<ViewAsset>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val settingsRepo = remember { com.bbxtudios.immichtv.AppContainer.settingsRepository }
    val gridColumns by settingsRepo.gridColumns.collectAsState(initial = 5)
    val animFocus by settingsRepo.animFocus.collectAsState(initial = true)
    val cardShadows by settingsRepo.cardShadows.collectAsState(initial = true)
    val smoothScroll by settingsRepo.smoothScroll.collectAsState(initial = true)

    val gridState = rememberTvLazyGridState()
    val firstItemFocusRequester = remember { FocusRequester() }
    val restoredItemFocusRequester = remember { FocusRequester() }
    val topBarFocusRequester = remember { FocusRequester() }
    var lastDpadMoveTime by remember { mutableLongStateOf(0L) }

    // Enviar foco al primer elemento cuando se pulsa en el menú lateral
    LaunchedEffect(focusTrigger) {
        if (focusTrigger > 0) {
            delay(60)
            try {
                firstItemFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Auto-foco cuando los datos terminan de cargar o restaurar foto vista
    LaunchedEffect(uiState, focusTrigger, lastViewedAssetId) {
        if (uiState is MemoriesUiState.Success) {
            val allAssets = (uiState as MemoriesUiState.Success).flatAssets
            delay(60)
            try {
                if (lastViewedAssetId != null && allAssets.any { it.id == lastViewedAssetId }) {
                    val idx = allAssets.indexOfFirst { it.id == lastViewedAssetId }
                    if (idx >= 0) {
                        gridState.scrollToItem((idx - 1).coerceAtLeast(0))
                        delay(50)
                        restoredItemFocusRequester.requestFocus()
                    }
                } else {
                    firstItemFocusRequester.requestFocus()
                }
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(top = 16.dp, start = 16.dp, end = 24.dp)
    ) {
        // Cabecera con título y botón de recarga
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = null,
                    tint = FocusHighlight,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.memories_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            // Botón de recarga
            val refreshInteraction = remember { MutableInteractionSource() }
            val isRefreshFocused by refreshInteraction.collectIsFocusedAsState()

            Box(
                modifier = Modifier
                    .focusRequester(topBarFocusRequester)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isRefreshFocused) ImmichBlue else BackgroundElevated)
                    .border(
                        width = if (isRefreshFocused) 2.dp else 1.dp,
                        color = if (isRefreshFocused) FocusHighlight else Color(0x33FFFFFF),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable(
                        interactionSource = refreshInteraction,
                        indication = null,
                        onClick = { viewModel.loadMemories() }
                    )
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyUp &&
                            (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                        ) {
                            viewModel.loadMemories()
                            true
                        } else {
                            false
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.retry),
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.retry),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }
            }
        }

        // Contenido
        when (val state = uiState) {
            is MemoriesUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LoadingSpinner(size = 72.dp)
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(text = stringResource(R.string.loading_content), color = TextSecondary)
                    }
                }
            }

            is MemoriesUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(imageVector = Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(60.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = stringResource(R.string.error_loading), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = state.message, color = TextSecondary)
                        Spacer(modifier = Modifier.height(24.dp))
                        ElevatedButton(
                            onClick = { viewModel.loadMemories() },
                            colors = ButtonDefaults.elevatedButtonColors(containerColor = ImmichBlue, contentColor = TextPrimary)
                        ) {
                            Text(stringResource(R.string.retry), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            is MemoriesUiState.Success -> {
                if (state.groups.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Rounded.SentimentDissatisfied, contentDescription = null, tint = TextMuted, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.memories_empty),
                                style = MaterialTheme.typography.titleMedium.copy(color = TextMuted)
                            )
                        }
                    }
                } else {
                    val allAssets = state.flatAssets
                    val firstAssetOverall = allAssets.firstOrNull()

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
                        // itemsIndexed entrega el índice precalculado → O(1), sin indexOf por frame
                        itemsIndexed(
                            items = allAssets,
                            key = { _, asset -> asset.id },
                            contentType = { _, _ -> "asset" }
                        ) { index, asset ->
                            val isFirst = index == 0
                            val isFirstRow = index < gridColumns
                            val effectiveReq = if (asset.id == lastViewedAssetId) restoredItemFocusRequester else if (isFirst) firstItemFocusRequester else null

                            val rowFocusModifier = Modifier.focusProperties {
                                if (isFirstRow) {
                                    up = topBarFocusRequester
                                }
                            }

                            AssetCard(
                                asset = asset,
                                onClick = { onAssetClick(allAssets, index) },
                                focusRequester = effectiveReq,
                                animFocus = animFocus,
                                cardShadows = cardShadows,
                                modifier = Modifier
                                    .then(rowFocusModifier)
                                    .fillMaxWidth()
                                    .aspectRatio(1.0f)
                            )
                        }
                    }
                }
            }
        }
    }
}
}
