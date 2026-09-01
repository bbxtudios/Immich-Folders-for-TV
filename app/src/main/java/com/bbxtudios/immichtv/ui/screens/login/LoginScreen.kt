package com.bbxtudios.immichtv.ui.screens.login

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bbxtudios.immichtv.R
import com.bbxtudios.immichtv.ui.screens.settings.SettingTvButton
import com.bbxtudios.immichtv.ui.theme.AccentCyan
import com.bbxtudios.immichtv.ui.theme.BackgroundDark
import com.bbxtudios.immichtv.ui.theme.BackgroundElevated
import com.bbxtudios.immichtv.ui.theme.BackgroundSurface
import com.bbxtudios.immichtv.ui.theme.FocusHighlight
import com.bbxtudios.immichtv.ui.theme.ImmichBlue
import com.bbxtudios.immichtv.ui.theme.TextPrimary
import com.bbxtudios.immichtv.ui.theme.TextSecondary
import com.bbxtudios.immichtv.util.LocalPairingServer
import com.bbxtudios.immichtv.util.QrCodeGenerator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface PairingState {
    object Waiting : PairingState
    object Validating : PairingState
    object Success : PairingState
    data class Error(val message: String) : PairingState
}

@Composable
fun LoginScreen(
    initialServerUrl: String = "",
    initialApiKey: String = "",
    onValidateCredentials: suspend (serverUrl: String, apiKey: String) -> Pair<Boolean, String>,
    onLoginSuccess: (serverUrl: String, apiKey: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    var manualServerUrl by remember { mutableStateOf(initialServerUrl) }
    var manualApiKey by remember { mutableStateOf(initialApiKey) }
    var isValidatingManual by remember { mutableStateOf(false) }
    var manualStatus by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    var pairingState by remember { mutableStateOf<PairingState>(PairingState.Waiting) }
    var pairingUrl by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    // Requesters de foco para navegación bidireccional D-Pad perfecta
    val qrCardFocusRequester = remember { FocusRequester() }
    val urlFieldFocusRequester = remember { FocusRequester() }
    val apiKeyFieldFocusRequester = remember { FocusRequester() }
    val submitBtnFocusRequester = remember { FocusRequester() }

    val qrInteractionSource = remember { MutableInteractionSource() }
    val isQrFocused by qrInteractionSource.collectIsFocusedAsState()

    // Iniciar servidor local de emparejamiento QR
    val pairingServer = remember {
        LocalPairingServer(port = 8888) { serverUrl, apiKey ->
            pairingState = PairingState.Validating
            val (success, message) = onValidateCredentials(serverUrl, apiKey)
            if (success) {
                pairingState = PairingState.Success
                coroutineScope.launch {
                    delay(1000)
                    onLoginSuccess(serverUrl, apiKey)
                }
                true to "OK"
            } else {
                pairingState = PairingState.Error(message)
                false to message
            }
        }
    }

    DisposableEffect(Unit) {
        val url = pairingServer.getPairingUrl()
        pairingUrl = url
        qrBitmap = QrCodeGenerator.generateQrBitmap(url, size = 512)
        pairingServer.start(coroutineScope)

        onDispose {
            pairingServer.stop()
        }
    }

    LaunchedEffect(Unit) {
        delay(150)
        try {
            qrCardFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 32.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Encabezado Superior
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "Logo Immich TV",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = stringResource(R.string.login_header_title),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = stringResource(R.string.login_header_subtitle),
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Cuerpo Split en 2 Columnas Simultáneas
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // --- COLUMNA 1: VINCULACIÓN CON QR (RECOMENDADO) ---
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .focusRequester(qrCardFocusRequester)
                        .focusable(interactionSource = qrInteractionSource)
                        .scale(if (isQrFocused) 1.02f else 1.0f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(BackgroundElevated)
                        .border(
                            width = if (isQrFocused) 3.dp else 1.5.dp,
                            color = if (isQrFocused) FocusHighlight else ImmichBlue.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown) {
                                if (keyEvent.key == Key.DirectionRight) {
                                    urlFieldFocusRequester.requestFocus()
                                    true
                                } else false
                            } else false
                        }
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.QrCodeScanner,
                                contentDescription = null,
                                tint = if (isQrFocused) FocusHighlight else AccentCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.login_opt1_title),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isQrFocused) FocusHighlight else AccentCyan
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Código QR
                        Box(
                            modifier = Modifier
                                .size(175.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (qrBitmap != null) {
                                Image(
                                    bitmap = qrBitmap!!,
                                    contentDescription = "Código QR de vinculación",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                CircularProgressIndicator(color = ImmichBlue)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = pairingUrl,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = FocusHighlight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = stringResource(R.string.login_opt1_desc),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                fontSize = 11.5.sp
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val statusText = when (val s = pairingState) {
                            PairingState.Waiting -> stringResource(R.string.login_qr_waiting)
                            PairingState.Validating -> stringResource(R.string.login_qr_validating)
                            PairingState.Success -> stringResource(R.string.login_qr_success)
                            is PairingState.Error -> s.message
                        }

                        // Estado en vivo
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (pairingState is PairingState.Success) Color(0xFF4CAF50) else AccentCyan,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }

                // --- COLUMNA 2: CONFIGURACIÓN MANUAL EN TV ---
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(BackgroundSurface)
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Tv,
                                contentDescription = null,
                                tint = FocusHighlight,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.login_opt2_title),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = manualServerUrl,
                            onValueChange = { manualServerUrl = it },
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(urlFieldFocusRequester)
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        if (keyEvent.key == Key.DirectionLeft) {
                                            qrCardFocusRequester.requestFocus()
                                            true
                                        } else if (keyEvent.key == Key.DirectionDown) {
                                            apiKeyFieldFocusRequester.requestFocus()
                                            true
                                        } else false
                                    } else false
                                }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = manualApiKey,
                            onValueChange = { manualApiKey = it },
                            label = { Text(stringResource(R.string.login_api_key)) },
                            leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null, tint = FocusHighlight) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FocusHighlight,
                                unfocusedBorderColor = Color(0x44FFFFFF),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(apiKeyFieldFocusRequester)
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        if (keyEvent.key == Key.DirectionLeft) {
                                            qrCardFocusRequester.requestFocus()
                                            true
                                        } else if (keyEvent.key == Key.DirectionUp) {
                                            urlFieldFocusRequester.requestFocus()
                                            true
                                        } else if (keyEvent.key == Key.DirectionDown) {
                                            submitBtnFocusRequester.requestFocus()
                                            true
                                        } else false
                                    } else false
                                }
                        )

                        if (manualStatus != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val (isSuccess, msg) = manualStatus!!
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSuccess) Color(0x224CAF50) else Color(0x22F44336))
                                    .border(1.dp, if (isSuccess) Color(0xFF4CAF50) else Color(0xFFF44336), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isSuccess) Icons.Rounded.Check else Icons.Rounded.Warning,
                                        contentDescription = null,
                                        tint = if (isSuccess) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isSuccess) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val testingText = stringResource(R.string.login_btn_testing)
                        val testConnectText = stringResource(R.string.login_btn_test_connect)
                        val validationEmptyText = stringResource(R.string.login_validation_empty)
                        val successText = stringResource(R.string.login_success)

                        Box(
                            modifier = Modifier
                                .focusRequester(submitBtnFocusRequester)
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        if (keyEvent.key == Key.DirectionLeft) {
                                            qrCardFocusRequester.requestFocus()
                                            true
                                        } else if (keyEvent.key == Key.DirectionUp) {
                                            apiKeyFieldFocusRequester.requestFocus()
                                            true
                                        } else false
                                    } else false
                                }
                        ) {
                            SettingTvButton(
                                text = if (isValidatingManual) testingText else testConnectText,
                                icon = Icons.Rounded.Check,
                                onClick = {
                                    if (!isValidatingManual) {
                                        if (manualServerUrl.isBlank() || manualApiKey.isBlank()) {
                                            manualStatus = false to validationEmptyText
                                            return@SettingTvButton
                                        }
                                        isValidatingManual = true
                                        manualStatus = null
                                        coroutineScope.launch {
                                            val (success, message) = onValidateCredentials(manualServerUrl.trim(), manualApiKey.trim())
                                            isValidatingManual = false
                                            if (success) {
                                                manualStatus = true to successText
                                                delay(800)
                                                onLoginSuccess(manualServerUrl.trim(), manualApiKey.trim())
                                            } else {
                                                manualStatus = false to "Error: $message"
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
