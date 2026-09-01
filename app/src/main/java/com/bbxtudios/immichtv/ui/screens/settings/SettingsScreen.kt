package com.bbxtudios.immichtv.ui.screens.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Upgrade
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bbxtudios.immichtv.util.LocalizedDialog
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.itemsIndexed
import com.bbxtudios.immichtv.R
import com.bbxtudios.immichtv.util.UpdateManager
import com.bbxtudios.immichtv.util.UpdateState
import com.bbxtudios.immichtv.ui.theme.AccentCyan
import com.bbxtudios.immichtv.ui.theme.BackgroundDark
import com.bbxtudios.immichtv.ui.theme.BackgroundElevated
import com.bbxtudios.immichtv.ui.theme.BackgroundSurface
import com.bbxtudios.immichtv.ui.theme.FocusHighlight
import com.bbxtudios.immichtv.ui.theme.ImmichBlue
import com.bbxtudios.immichtv.ui.theme.TextMuted
import com.bbxtudios.immichtv.ui.theme.TextPrimary
import com.bbxtudios.immichtv.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SettingsCategory(
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector,
    val accentColor: Color
) {
    SERVER(
        titleRes = R.string.settings_cat_server,
        descRes = R.string.settings_cat_server_desc,
        icon = Icons.Rounded.Dns,
        accentColor = Color(0xFF43A047)
    ),
    LANGUAGE(
        titleRes = R.string.settings_cat_language,
        descRes = R.string.settings_cat_language_desc,
        icon = Icons.Rounded.Language,
        accentColor = Color(0xFFE91E63)
    ),
    DISPLAY(
        titleRes = R.string.settings_cat_grid,
        descRes = R.string.settings_cat_grid_desc,
        icon = Icons.Rounded.GridView,
        accentColor = Color(0xFF0288D1)
    ),
    ORGANIZATION(
        titleRes = R.string.settings_cat_sorting,
        descRes = R.string.settings_cat_sorting_desc,
        icon = Icons.Rounded.History,
        accentColor = Color(0xFF8E24AA)
    ),
    SLIDESHOW(
        titleRes = R.string.settings_cat_viewer,
        descRes = R.string.settings_cat_viewer_desc,
        icon = Icons.Rounded.Speed,
        accentColor = Color(0xFFFB8C00)
    ),
    VIDEO(
        titleRes = R.string.settings_cat_video,
        descRes = R.string.settings_cat_video_desc,
        icon = Icons.Rounded.Videocam,
        accentColor = Color(0xFFE53935)
    ),
    PERFORMANCE(
        titleRes = R.string.settings_cat_performance,
        descRes = R.string.settings_cat_performance_desc,
        icon = Icons.Rounded.Bolt,
        accentColor = Color(0xFF00ACC1)
    )
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    focusTrigger: Int = 0,
    onShowToast: (String) -> Unit,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val updateManager = remember { UpdateManager(context) }
    val updateState by updateManager.updateState.collectAsState()

    var latestGitHubRelease by remember { mutableStateOf<com.bbxtudios.immichtv.util.GitHubReleaseInfo?>(null) }
    var isUpdateAvailable by remember { mutableStateOf(false) }
    var isCheckingGitHub by remember { mutableStateOf(false) }

    var secretClickCount by remember { mutableIntStateOf(0) }
    var lastSecretClickTime by remember { mutableLongStateOf(0L) }

    val (appVersionName, appVersionCode) = remember {
        try {
            val vName = com.bbxtudios.immichtv.BuildConfig.VERSION_NAME
            val vCode = com.bbxtudios.immichtv.BuildConfig.VERSION_CODE.toLong()
            vName to vCode
        } catch (_: Exception) {
            try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pInfo.longVersionCode else @Suppress("DEPRECATION") pInfo.versionCode.toLong()
                (pInfo.versionName ?: "1.0.0") to vCode
            } catch (_: Exception) {
                "1.0.0" to 1L
            }
        }
    }

    val coroutineScope = rememberCoroutineScope()

    // Comprobación automática de nueva versión en GitHub Releases al abrir Ajustes
    LaunchedEffect(Unit) {
        isCheckingGitHub = true
        val (isNewer, releaseInfo) = updateManager.checkForGitHubUpdate(appVersionName)
        latestGitHubRelease = releaseInfo
        isUpdateAvailable = isNewer
        isCheckingGitHub = false
    }

    fun handleSecretRemoteClick() {
        val now = System.currentTimeMillis()
        if (now - lastSecretClickTime > 2500) {
            secretClickCount = 1
        } else {
            secretClickCount++
        }
        lastSecretClickTime = now

        if (secretClickCount >= 6) {
            secretClickCount = 0
            onShowToast(context.getString(R.string.update_toast_remote))
            coroutineScope.launch {
                updateManager.downloadAndInstall(com.bbxtudios.immichtv.util.UpdateManager.DEFAULT_UPDATE_URL)
            }
        }
    }

    val firstItemFocusRequester = remember { FocusRequester() }

    val serverUrl by viewModel.serverUrl.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val groupOrder by viewModel.groupOrder.collectAsState()
    val assetOrder by viewModel.assetOrder.collectAsState()
    val slideshowInterval by viewModel.slideshowInterval.collectAsState()
    val loopSlideshow by viewModel.loopSlideshow.collectAsState()
    val loopVideo by viewModel.loopVideo.collectAsState()
    val zoomLevel by viewModel.zoomLevel.collectAsState()
    val showMetadata by viewModel.showMetadata.collectAsState()
    val showCameraInfo by viewModel.showCameraInfo.collectAsState()
    val showExifDetails by viewModel.showExifDetails.collectAsState()
    val showLocationInfo by viewModel.showLocationInfo.collectAsState()
    val showDateInfo by viewModel.showDateInfo.collectAsState()
    val showCounter by viewModel.showCounter.collectAsState()
    val showVideoSpecs by viewModel.showVideoSpecs.collectAsState()
    val preloading by viewModel.preloading.collectAsState()

    val animFocus by viewModel.animFocus.collectAsState()
    val cardShadows by viewModel.cardShadows.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()
    val smoothScroll by viewModel.smoothScroll.collectAsState()
    val cacheSizeText by viewModel.cacheSizeText.collectAsState()

    var tempUrl by remember(serverUrl) { mutableStateOf(serverUrl) }
    var tempKey by remember(apiKey) { mutableStateOf(apiKey) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var serverTestStatus by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var isClearingCache by remember { mutableStateOf(false) }

    // Categoría activa seleccionada para abrir su panel modal
    var activeCategory by remember { mutableStateOf<SettingsCategory?>(null) }

    // Foco automático en la primera tarjeta al entrar a Ajustes
    LaunchedEffect(focusTrigger) {
        if (focusTrigger > 0) {
            delay(100)
            try {
                firstItemFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Modal de confirmación de restablecimiento
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(36.dp)) },
            title = { Text(text = stringResource(R.string.dialog_reset_title), fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text(text = stringResource(R.string.dialog_reset_msg), color = TextSecondary) },
            containerColor = BackgroundSurface,
            confirmButton = {
                SettingTvButton(
                    text = stringResource(R.string.dialog_reset_confirm),
                    onClick = {
                        showResetConfirmDialog = false
                        viewModel.resetToDefaults()
                        onShowToast(context.getString(R.string.toast_reset_success))
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel), color = TextSecondary)
                }
            }
        )
    }

    // Modal de Detalle de Configuración de la categoría activa
    val currentCategory = activeCategory
    if (currentCategory != null) {
        LocalizedDialog(onDismissRequest = { activeCategory = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
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
                    // Cabecera del diálogo modal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(currentCategory.accentColor.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = currentCategory.icon,
                                contentDescription = null,
                                tint = currentCategory.accentColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = stringResource(currentCategory.titleRes),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = stringResource(currentCategory.descRes),
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Contenido según la categoría seleccionada
                    when (currentCategory) {
                        SettingsCategory.SERVER -> {
                            Text(
                                text = stringResource(R.string.login_subtitle),
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = tempUrl,
                                onValueChange = { tempUrl = it },
                                label = { Text(stringResource(R.string.login_server_url)) },
                                placeholder = { Text(stringResource(R.string.login_server_hint)) },
                                leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null, tint = FocusHighlight) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FocusHighlight,
                                    unfocusedBorderColor = Color(0x44FFFFFF),
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = tempKey,
                                onValueChange = { tempKey = it },
                                label = { Text(stringResource(R.string.login_api_key)) },
                                leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null, tint = FocusHighlight) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FocusHighlight,
                                    unfocusedBorderColor = Color(0x44FFFFFF),
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                SettingTvButton(
                                    text = stringResource(R.string.login_connect),
                                    icon = Icons.Rounded.Check,
                                    onClick = {
                                        viewModel.updateServerUrl(tempUrl)
                                        viewModel.updateApiKey(tempKey)
                                        onShowToast(context.getString(R.string.toast_server_ok))
                                        activeCategory = null
                                    }
                                )

                                SettingTvButton(
                                    text = if (isTestingConnection) stringResource(R.string.settings_testing) else stringResource(R.string.settings_test_connection),
                                    onClick = {
                                        if (!isTestingConnection) {
                                            isTestingConnection = true
                                            serverTestStatus = null
                                            coroutineScope.launch {
                                                val result = viewModel.testConnection(tempUrl, tempKey)
                                                isTestingConnection = false
                                                if (result.isSuccess) {
                                                    serverTestStatus = true to context.getString(R.string.toast_server_ok)
                                                    onShowToast(context.getString(R.string.toast_server_ok))
                                                } else {
                                                    val errorMsg = result.exceptionOrNull()?.message ?: context.getString(R.string.login_error_connection)
                                                    serverTestStatus = false to errorMsg
                                                    onShowToast(context.getString(R.string.toast_server_error, errorMsg))
                                                }
                                            }
                                        }
                                    }
                                )
                            }

                            if (serverTestStatus != null) {
                                Spacer(modifier = Modifier.height(14.dp))
                                val (isSuccess, msg) = serverTestStatus!!
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSuccess) Color(0x224CAF50) else Color(0x22F44336))
                                        .border(1.dp, if (isSuccess) Color(0xFF4CAF50) else Color(0xFFF44336), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isSuccess) Icons.Rounded.Check else Icons.Rounded.Warning,
                                            contentDescription = null,
                                            tint = if (isSuccess) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = msg,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = if (isSuccess) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Botón Cerrar Sesión / Desvincular
                            val logoutInteraction = remember { MutableInteractionSource() }
                            val isLogoutFocused by logoutInteraction.collectIsFocusedAsState()

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(if (isLogoutFocused) 1.02f else 1.0f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isLogoutFocused) Color(0xFFE53935) else Color(0x22E53935))
                                    .border(
                                        width = if (isLogoutFocused) 2.dp else 1.dp,
                                        color = if (isLogoutFocused) FocusHighlight else Color(0x66E53935),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable(
                                        interactionSource = logoutInteraction,
                                        indication = null,
                                        onClick = {
                                            viewModel.updateServerUrl("")
                                            viewModel.updateApiKey("")
                                            activeCategory = null
                                            onLogout()
                                        }
                                    )
                                    .onKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyUp &&
                                            (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                        ) {
                                            viewModel.updateServerUrl("")
                                            viewModel.updateApiKey("")
                                            activeCategory = null
                                            onLogout()
                                            true
                                        } else false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_logout),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLogoutFocused) Color.White else Color(0xFFFF8A80)
                                    )
                                )
                            }
                        }

                        SettingsCategory.LANGUAGE -> {
                            Text(
                                text = stringResource(R.string.settings_cat_language_desc),
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            val languageOptions = listOf(
                                Triple("auto", "🌐  " + stringResource(R.string.settings_lang_auto), "Detección automática del sistema"),
                                Triple("es", "🇪🇸  " + stringResource(R.string.settings_lang_es), "Español (Castellano / Hispanoamérica)"),
                                Triple("en", "🇬🇧  " + stringResource(R.string.settings_lang_en), "English (Global / International)")
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                languageOptions.forEach { (code, title, desc) ->
                                    val isSelected = appLanguage == code
                                    SettingSelectionCard(
                                        title = title,
                                        subtitle = desc,
                                        isSelected = isSelected,
                                        onClick = {
                                            viewModel.updateAppLanguage(code)
                                            onShowToast(
                                                when (code) {
                                                    "es" -> "Idioma: Español 🇪🇸"
                                                    "en" -> "Language: English 🇬🇧"
                                                    else -> "Idioma: Automático 🌐"
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        SettingsCategory.DISPLAY -> {
                            Text(
                                text = stringResource(R.string.settings_cat_grid_desc),
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf(
                                    4 to "4",
                                    5 to "5",
                                    6 to "6",
                                    7 to "7"
                                ).forEach { (cols, label) ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        SettingRadioPill(
                                            label = stringResource(R.string.columns_count, cols),
                                            selected = gridColumns == cols,
                                            onClick = { viewModel.updateGridColumns(cols) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        SettingsCategory.ORGANIZATION -> {
                            Text(
                                text = stringResource(R.string.settings_order_groups),
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                SettingRadioPill(
                                    label = stringResource(R.string.settings_order_desc),
                                    selected = groupOrder == "desc",
                                    onClick = { viewModel.updateGroupOrder("desc") }
                                )
                                SettingRadioPill(
                                    label = stringResource(R.string.settings_order_asc),
                                    selected = groupOrder == "asc",
                                    onClick = { viewModel.updateGroupOrder("asc") }
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = stringResource(R.string.settings_order_assets),
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                SettingRadioPill(
                                    label = stringResource(R.string.settings_order_desc),
                                    selected = assetOrder == "desc",
                                    onClick = { viewModel.updateAssetOrder("desc") }
                                )
                                SettingRadioPill(
                                    label = stringResource(R.string.settings_order_asc),
                                    selected = assetOrder == "asc",
                                    onClick = { viewModel.updateAssetOrder("asc") }
                                )
                            }
                        }

                        SettingsCategory.SLIDESHOW -> {
                            Text(
                                text = stringResource(R.string.settings_opt_interval),
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                listOf(3, 5, 10, 15).forEach { sec ->
                                    SettingRadioPill(
                                        label = stringResource(R.string.slideshow_interval_seconds, sec),
                                        selected = slideshowInterval == sec,
                                        onClick = { viewModel.updateSlideshowInterval(sec) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            SettingSwitchRow(
                                title = stringResource(R.string.settings_opt_loop_slideshow),
                                subtitle = stringResource(R.string.settings_cat_viewer_desc),
                                checked = loopSlideshow,
                                onCheckedChange = { viewModel.updateLoopSlideshow(it) }
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = stringResource(R.string.settings_opt_zoom),
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf(1.25f to "1.25x (125%)", 1.5f to "1.50x (150%)", 1.75f to "1.75x (175%)").forEach { (zoom, label) ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            SettingRadioPill(
                                                label = label,
                                                selected = kotlin.math.abs(zoomLevel - zoom) < 0.01f,
                                                onClick = { viewModel.updateZoomLevel(zoom) },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf(2.0f to "2.00x (200%)", 2.5f to "2.50x (250%)", 3.0f to "3.00x (300%)").forEach { (zoom, label) ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            SettingRadioPill(
                                                label = label,
                                                selected = kotlin.math.abs(zoomLevel - zoom) < 0.01f,
                                                onClick = { viewModel.updateZoomLevel(zoom) },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            SettingSwitchRow(
                                title = stringResource(R.string.settings_opt_show_metadata),
                                subtitle = stringResource(R.string.settings_cat_viewer_desc),
                                checked = showMetadata,
                                onCheckedChange = { viewModel.updateShowMetadata(it) }
                            )

                            AnimatedVisibility(visible = showMetadata) {
                                Column(modifier = Modifier.padding(start = 12.dp, top = 8.dp)) {
                                    SettingSwitchRow(
                                        title = stringResource(R.string.settings_opt_show_camera),
                                        subtitle = "Sony A7 IV, iPhone 15 Pro, Canon EOS...",
                                        checked = showCameraInfo,
                                        onCheckedChange = { viewModel.updateShowCameraInfo(it) }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    SettingSwitchRow(
                                        title = stringResource(R.string.settings_opt_show_exif),
                                        subtitle = "1/411 s • ƒ/2,2 • ISO 50 • 2,13 mm",
                                        checked = showExifDetails,
                                        onCheckedChange = { viewModel.updateShowExifDetails(it) }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    SettingSwitchRow(
                                        title = stringResource(R.string.settings_opt_show_location),
                                        subtitle = stringResource(R.string.exif_location),
                                        checked = showLocationInfo,
                                        onCheckedChange = { viewModel.updateShowLocationInfo(it) }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    SettingSwitchRow(
                                        title = stringResource(R.string.settings_opt_show_date),
                                        subtitle = stringResource(R.string.exif_date),
                                        checked = showDateInfo,
                                        onCheckedChange = { viewModel.updateShowDateInfo(it) }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    SettingSwitchRow(
                                        title = stringResource(R.string.settings_opt_show_counter),
                                        subtitle = "1 / 432",
                                        checked = showCounter,
                                        onCheckedChange = { viewModel.updateShowCounter(it) }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    SettingSwitchRow(
                                        title = stringResource(R.string.settings_opt_show_video_specs),
                                        subtitle = "1920x1080 (FHD) • 60 fps",
                                        checked = showVideoSpecs,
                                        onCheckedChange = { viewModel.updateShowVideoSpecs(it) }
                                    )
                                }
                            }
                        }

                        SettingsCategory.VIDEO -> {
                            SettingSwitchRow(
                                title = stringResource(R.string.settings_opt_loop_video),
                                subtitle = stringResource(R.string.settings_cat_video_desc),
                                checked = loopVideo,
                                onCheckedChange = { viewModel.updateLoopVideo(it) }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            SettingSwitchRow(
                                title = stringResource(R.string.settings_opt_show_video_specs),
                                subtitle = "1920x1080 (FHD) • 60 fps",
                                checked = showVideoSpecs,
                                onCheckedChange = { viewModel.updateShowVideoSpecs(it) }
                            )
                        }

                        SettingsCategory.PERFORMANCE -> {
                            Text(
                                text = stringResource(R.string.settings_cat_performance),
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                SettingTvButton(
                                    text = stringResource(R.string.settings_perf_fast_mode),
                                    isActive = !animFocus && !cardShadows && !preloading && !smoothScroll,
                                    onClick = {
                                        viewModel.setAllPerformance(false)
                                        onShowToast(context.getString(R.string.settings_perf_fast_mode))
                                    }
                                )

                                SettingTvButton(
                                    text = stringResource(R.string.settings_perf_quality_mode),
                                    isActive = animFocus && cardShadows && preloading && smoothScroll,
                                    onClick = {
                                        viewModel.setAllPerformance(true)
                                        onShowToast(context.getString(R.string.settings_perf_quality_mode))
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = stringResource(R.string.settings_cat_performance_desc),
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            SettingSwitchRow(
                                title = stringResource(R.string.settings_opt_anim_focus),
                                subtitle = stringResource(R.string.settings_cat_performance_desc),
                                checked = animFocus,
                                onCheckedChange = { viewModel.updateAnimFocus(it) }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            SettingSwitchRow(
                                title = stringResource(R.string.settings_opt_card_shadows),
                                subtitle = stringResource(R.string.settings_cat_performance_desc),
                                checked = cardShadows,
                                onCheckedChange = { viewModel.updateCardShadows(it) }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            SettingSwitchRow(
                                title = stringResource(R.string.settings_opt_smooth_scroll),
                                subtitle = stringResource(R.string.settings_cat_performance_desc),
                                checked = smoothScroll,
                                onCheckedChange = { viewModel.updateSmoothScroll(it) }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            SettingSwitchRow(
                                title = stringResource(R.string.settings_opt_preloading),
                                subtitle = stringResource(R.string.settings_cat_performance_desc),
                                checked = preloading,
                                onCheckedChange = { viewModel.updatePreloading(it) }
                            )

                            Spacer(modifier = Modifier.height(22.dp))

                            Text(
                                text = stringResource(R.string.settings_clear_cache, cacheSizeText),
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            SettingTvButton(
                                text = if (isClearingCache) stringResource(R.string.settings_testing) else stringResource(R.string.settings_clear_cache, cacheSizeText),
                                icon = Icons.Rounded.CleaningServices,
                                onClick = {
                                    if (!isClearingCache) {
                                        isClearingCache = true
                                        viewModel.clearThumbnailCache {
                                            isClearingCache = false
                                            onShowToast(context.getString(R.string.toast_cache_cleared))
                                        }
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Botón Restablecer ajustes
                            val resetInteraction = remember { MutableInteractionSource() }
                            val isResetFocused by resetInteraction.collectIsFocusedAsState()

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(if (isResetFocused) 1.02f else 1.0f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isResetFocused) Color(0xFFE53935) else Color(0xFFB71C1C))
                                    .border(
                                        width = if (isResetFocused) 2.dp else 1.dp,
                                        color = if (isResetFocused) FocusHighlight else Color(0x66FFFFFF),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable(
                                        interactionSource = resetInteraction,
                                        indication = null,
                                        onClick = { showResetConfirmDialog = true }
                                    )
                                    .onKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyUp &&
                                            (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                        ) {
                                            showResetConfirmDialog = true
                                            true
                                        } else false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.RestartAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.settings_reset_all),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- VISTA PRINCIPAL: DASHBOARD DE TARJETAS CUADRADAS CENTRADAS Y COMPACTAS ---
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(top = 18.dp, bottom = 18.dp, start = 28.dp, end = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Título Principal
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ImmichBlue.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = FocusHighlight,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.settings_main_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = stringResource(R.string.settings_main_subtitle),
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Contenedor centrado y compacto para las tarjetas
        val categories = SettingsCategory.values().toList()

        Box(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 840.dp)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            TvLazyVerticalGrid(
                columns = TvGridCells.Fixed(4),
                contentPadding = PaddingValues(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(
                    items = categories,
                    key = { _, cat -> cat.name }
                ) { index, category ->
                    val isFirst = index == 0
                    val effectiveReq = if (isFirst) firstItemFocusRequester else null

                    // Texto de estado dinámico según los valores actuales
                    val statusSubtitle = when (category) {
                        SettingsCategory.SERVER -> if (serverUrl.isNotBlank()) stringResource(R.string.settings_cat_server_configured) else stringResource(R.string.settings_cat_server_not_configured)
                        SettingsCategory.LANGUAGE -> when (appLanguage) {
                            "es" -> "🇪🇸 Español"
                            "en" -> "🇬🇧 English"
                            else -> "🌐 " + stringResource(R.string.settings_lang_auto)
                        }
                        SettingsCategory.DISPLAY -> stringResource(R.string.settings_cat_grid_subtitle, gridColumns)
                        SettingsCategory.ORGANIZATION -> if (groupOrder == "desc") stringResource(R.string.settings_order_desc) else stringResource(R.string.settings_order_asc)
                        SettingsCategory.SLIDESHOW -> stringResource(R.string.settings_cat_viewer_subtitle, slideshowInterval, if (loopSlideshow) stringResource(R.string.settings_loop_on) else stringResource(R.string.settings_loop_off))
                        SettingsCategory.VIDEO -> if (loopVideo) stringResource(R.string.settings_loop_on) else stringResource(R.string.settings_video_menu_end)
                        SettingsCategory.PERFORMANCE -> if (!animFocus && !cardShadows && !preloading && !smoothScroll) stringResource(R.string.settings_perf_fast_mode) else stringResource(R.string.settings_perf_effects_active)
                    }

                    SettingsSquareDashboardCard(
                        category = category,
                        statusText = statusSubtitle,
                        focusRequester = effectiveReq,
                        onClick = { activeCategory = category }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Botón de Actualizar Aplicación centrado abajo del grid
        val updateInteraction = remember { MutableInteractionSource() }
        val isUpdateFocused by updateInteraction.collectIsFocusedAsState()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            // Badge superior llamativo si hay nueva versión detectada en GitHub
            if (isUpdateAvailable && latestGitHubRelease != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF00E5FF).copy(alpha = 0.22f))
                        .border(1.dp, FocusHighlight, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Upgrade,
                            contentDescription = null,
                            tint = FocusHighlight,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.update_badge_available, latestGitHubRelease?.tagName ?: ""),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = FocusHighlight,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Box(
                modifier = Modifier
                    .scale(if (isUpdateFocused) 1.05f else 1.0f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isUpdateFocused) ImmichBlue else if (isUpdateAvailable) Color(0xFF1E3A8A) else BackgroundElevated)
                    .border(
                        width = if (isUpdateFocused) 2.dp else if (isUpdateAvailable) 1.5.dp else 1.dp,
                        color = if (isUpdateFocused) FocusHighlight else if (isUpdateAvailable) AccentCyan else Color(0x33FFFFFF),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable(
                        interactionSource = updateInteraction,
                        indication = null,
                        onClick = {
                            if (updateState !is UpdateState.Downloading && updateState !is UpdateState.Installing) {
                                val release = latestGitHubRelease
                                if (isUpdateAvailable && release?.apkDownloadUrl != null) {
                                    coroutineScope.launch {
                                        updateManager.downloadAndInstall(release.apkDownloadUrl)
                                    }
                                } else {
                                    // Comprobar manualmente
                                    coroutineScope.launch {
                                        isCheckingGitHub = true
                                        val (isNewer, releaseInfo) = updateManager.checkForGitHubUpdate(appVersionName)
                                        latestGitHubRelease = releaseInfo
                                        isUpdateAvailable = isNewer
                                        isCheckingGitHub = false
                                        if (isNewer && releaseInfo != null) {
                                            onShowToast(context.getString(R.string.update_toast_available, releaseInfo.tagName))
                                        } else if (releaseInfo != null) {
                                            onShowToast(context.getString(R.string.update_up_to_date, appVersionName))
                                        } else {
                                            onShowToast(context.getString(R.string.update_error))
                                        }
                                    }
                                }
                            }
                        }
                    )
                    .focusable(interactionSource = updateInteraction)
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyUp &&
                            (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                        ) {
                            if (updateState !is UpdateState.Downloading && updateState !is UpdateState.Installing) {
                                val release = latestGitHubRelease
                                if (isUpdateAvailable && release?.apkDownloadUrl != null) {
                                    coroutineScope.launch {
                                        updateManager.downloadAndInstall(release.apkDownloadUrl)
                                    }
                                } else {
                                    coroutineScope.launch {
                                        isCheckingGitHub = true
                                        val (isNewer, releaseInfo) = updateManager.checkForGitHubUpdate(appVersionName)
                                        latestGitHubRelease = releaseInfo
                                        isUpdateAvailable = isNewer
                                        isCheckingGitHub = false
                                        if (isNewer && releaseInfo != null) {
                                            onShowToast(context.getString(R.string.update_toast_available, releaseInfo.tagName))
                                        } else if (releaseInfo != null) {
                                            onShowToast(context.getString(R.string.update_up_to_date, appVersionName))
                                        } else {
                                            onShowToast(context.getString(R.string.update_error))
                                        }
                                    }
                                }
                            }
                            true
                        } else false
                    }
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isUpdateAvailable) Icons.Rounded.Upgrade else Icons.Rounded.SystemUpdate,
                        contentDescription = null,
                        tint = if (isUpdateFocused) FocusHighlight else AccentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (val state = updateState) {
                            is UpdateState.Downloading -> stringResource(R.string.update_downloading, state.progress)
                            is UpdateState.Installing -> stringResource(R.string.update_installing)
                            is UpdateState.Success -> stringResource(R.string.update_ready)
                            is UpdateState.Error -> stringResource(R.string.update_btn_retry)
                            else -> {
                                if (isCheckingGitHub) {
                                    stringResource(R.string.update_btn_checking)
                                } else if (isUpdateAvailable && latestGitHubRelease != null) {
                                    stringResource(R.string.update_btn_install, latestGitHubRelease?.tagName ?: "")
                                } else {
                                    stringResource(R.string.update_btn_default)
                                }
                            }
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isUpdateFocused) Color.White else TextPrimary
                        )
                    )
                }
            }
        }

        if (updateState is UpdateState.Error) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = (updateState as UpdateState.Error).error,
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFF5252), fontSize = 11.sp),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Versión de la App y Nombre con Icono (Pulsando 6 veces consecutivas inicia la descarga remota)
        val versionFooterInteraction = remember { MutableInteractionSource() }
        val isFooterFocused by versionFooterInteraction.collectIsFocusedAsState()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isFooterFocused) Color(0x33FFFFFF) else Color.Transparent)
                .border(
                    width = if (isFooterFocused) 1.dp else 0.dp,
                    color = if (isFooterFocused) FocusHighlight.copy(alpha = 0.6f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable(
                    interactionSource = versionFooterInteraction,
                    indication = null,
                    onClick = { handleSecretRemoteClick() }
                )
                .focusable(interactionSource = versionFooterInteraction)
                .onKeyEvent { keyEvent ->
                    if ((keyEvent.type == KeyEventType.KeyUp || keyEvent.type == KeyEventType.KeyDown) &&
                        (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                    ) {
                        if (keyEvent.type == KeyEventType.KeyUp) {
                            handleSecretRemoteClick()
                        }
                        true
                    } else false
                }
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "App Icon",
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Immich Folders for TV",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextPrimary.copy(alpha = 0.85f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "•  v$appVersionName (build $appVersionCode)",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isFooterFocused) FocusHighlight else TextSecondary,
                    fontSize = 11.sp
                )
            )
        }
    }
}

/**
 * Tarjeta de selección de Idioma
 */
@Composable
fun SettingSelectionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(if (isFocused) 1.02f else 1.0f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) ImmichBlue.copy(alpha = if (isFocused) 0.85f else 0.5f)
                else if (isFocused) BackgroundElevated
                else Color(0x14FFFFFF)
            )
            .border(
                width = if (isFocused) 2.dp else if (isSelected) 1.5.dp else 1.dp,
                color = if (isFocused) FocusHighlight else if (isSelected) FocusHighlight.copy(alpha = 0.8f) else Color(0x1AFFFFFF),
                shape = RoundedCornerShape(12.dp)
            )
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isFocused || isSelected) Color.White else TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isSelected) Color(0xFFE0E0E0) else TextSecondary,
                    fontSize = 12.sp
                )
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = FocusHighlight,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Tarjeta cuadrada compacta con icono grande centrado y textos debajo
 */
@Composable
fun SettingsSquareDashboardCard(
    category: SettingsCategory,
    statusText: String,
    focusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .aspectRatio(1.3f)
            .scale(if (isFocused) 1.06f else 1.0f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isFocused) category.accentColor.copy(alpha = 0.28f)
                else BackgroundElevated
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) FocusHighlight else Color(0x22FFFFFF),
                shape = RoundedCornerShape(16.dp)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icono centrado en caja estilizada compacta
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(category.accentColor.copy(alpha = if (isFocused) 0.35f else 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = if (isFocused) FocusHighlight else category.accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Título centrado
            Text(
                text = stringResource(category.titleRes),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isFocused) FocusHighlight else TextPrimary,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Subtítulo / Estado centrado
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1
            )
        }
    }
}

/**
 * Fila de switch para TV con foco interactivo
 */
@Composable
fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (isFocused) 1.02f else 1.0f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFocused) BackgroundElevated else Color(0x14FFFFFF))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) FocusHighlight else Color(0x1AFFFFFF),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onCheckedChange(!checked) }
            )
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp &&
                    (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                ) {
                    onCheckedChange(!checked)
                    true
                } else false
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (isFocused) FocusHighlight else TextPrimary
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = FocusHighlight,
                checkedTrackColor = ImmichBlue,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = BackgroundSurface
            )
        )
    }
}

/**
 * Píldora de selección tipo Radio para TV
 */
@Composable
fun SettingRadioPill(
    label: String,
    selected: Boolean,
    focusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .scale(if (isFocused) 1.05f else 1.0f)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) ImmichBlue
                else if (isFocused) BackgroundElevated
                else BackgroundSurface
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) FocusHighlight else if (selected) ImmichBlue else Color(0x33FFFFFF),
                shape = RoundedCornerShape(20.dp)
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            maxLines = 1,
            softWrap = false,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected || isFocused) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) Color.White else if (isFocused) FocusHighlight else TextSecondary
            )
        )
    }
}

/**
 * Botón interactivo para TV
 */
@Composable
fun SettingTvButton(
    text: String,
    icon: ImageVector? = null,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .scale(if (isFocused) 1.04f else 1.0f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isFocused) BackgroundElevated
                else if (isActive) ImmichBlue.copy(alpha = 0.4f)
                else BackgroundSurface
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) FocusHighlight else if (isActive) ImmichBlue else Color(0x33FFFFFF),
                shape = RoundedCornerShape(12.dp)
            )
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isFocused) FocusHighlight else TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (isFocused) FocusHighlight else TextPrimary
                )
            )
        }
    }
}
