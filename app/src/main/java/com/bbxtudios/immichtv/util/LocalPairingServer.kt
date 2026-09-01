package com.bbxtudios.immichtv.util

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder

class LocalPairingServer(
    private val port: Int = 8888,
    private val onCredentialsReceived: suspend (serverUrl: String, apiKey: String) -> Pair<Boolean, String>
) {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    companion object {
        private const val TAG = "LocalPairingServer"

        fun getLocalIpAddress(): String {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val networkInterface = interfaces.nextElement()
                    if (networkInterface.isLoopback || !networkInterface.isUp) continue
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (address is Inet4Address && !address.isLoopbackAddress) {
                            val ip = address.hostAddress ?: ""
                            if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                                return ip
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error obteniendo IP local", e)
            }
            return "127.0.0.1"
        }
    }

    fun getPairingUrl(): String {
        val ip = getLocalIpAddress()
        return "http://$ip:$port"
    }

    fun start(scope: CoroutineScope) {
        stop()
        serverJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(port)
                Log.d(TAG, "Servidor de emparejamiento iniciado en ${getPairingUrl()}")

                while (isActive && serverSocket != null && !serverSocket!!.isClosed) {
                    val clientSocket = try {
                        serverSocket!!.accept()
                    } catch (e: Exception) {
                        break
                    }
                    launch(Dispatchers.IO) {
                        handleClient(clientSocket)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en ServerSocket", e)
            }
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
            serverSocket = null
            serverJob?.cancel()
            serverJob = null
        } catch (e: Exception) {
            Log.e(TAG, "Error cerrando servidor", e)
        }
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val out = PrintWriter(socket.getOutputStream(), true)

            val requestLine = reader.readLine() ?: return@withContext
            val tokens = requestLine.split(" ")
            if (tokens.size < 2) return@withContext

            val method = tokens[0]
            val path = tokens[1]

            // Leer cabeceras para obtener Content-Length
            var contentLength = 0
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrBlank()) break
                if (line!!.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = line!!.substringAfter(":").trim().toIntOrNull() ?: 0
                }
            }

            if (method == "GET" && (path == "/" || path.startsWith("/?"))) {
                sendHtmlResponse(out)
            } else if (method == "POST" && path == "/submit") {
                val bodyChars = CharArray(contentLength)
                var read = 0
                while (read < contentLength) {
                    val count = reader.read(bodyChars, read, contentLength - read)
                    if (count == -1) break
                    read += count
                }
                val body = String(bodyChars, 0, read)

                var serverUrl = ""
                var apiKey = ""

                try {
                    if (body.trim().startsWith("{")) {
                        val json = JSONObject(body)
                        serverUrl = json.optString("serverUrl", "")
                        apiKey = json.optString("apiKey", "")
                    } else {
                        val pairs = body.split("&")
                        for (pair in pairs) {
                            val parts = pair.split("=")
                            if (parts.size == 2) {
                                val key = URLDecoder.decode(parts[0], "UTF-8")
                                val value = URLDecoder.decode(parts[1], "UTF-8")
                                if (key == "serverUrl") serverUrl = value
                                if (key == "apiKey") apiKey = value
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parseando cuerpo POST", e)
                }

                if (serverUrl.isBlank() || apiKey.isBlank()) {
                    sendJsonResponse(out, 400, false, "Debes rellenar tanto la URL del servidor como la API Key.")
                } else {
                    val (success, message) = onCredentialsReceived(serverUrl.trim(), apiKey.trim())
                    if (success) {
                        sendJsonResponse(out, 200, true, "¡Conexión establecida con éxito! La TV se ha configurado correctamente.")
                    } else {
                        sendJsonResponse(out, 400, false, message)
                    }
                }
            } else {
                out.print("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n")
                out.flush()
            }
            socket.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error manejando cliente", e)
        }
    }

    private fun sendJsonResponse(out: PrintWriter, statusCode: Int, success: Boolean, message: String) {
        val statusText = if (statusCode == 200) "OK" else "Bad Request"
        val json = JSONObject().apply {
            put("success", success)
            put("message", message)
        }.toString()

        val responseBytes = json.toByteArray(Charsets.UTF_8)
        out.print("HTTP/1.1 $statusCode $statusText\r\n")
        out.print("Content-Type: application/json; charset=UTF-8\r\n")
        out.print("Content-Length: ${responseBytes.size}\r\n")
        out.print("Access-Control-Allow-Origin: *\r\n")
        out.print("Connection: close\r\n\r\n")
        out.print(json)
        out.flush()
    }

    private fun sendHtmlResponse(out: PrintWriter) {
        val html = """
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Connect Immich to Android TV</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
        body { background: #0B0E14; color: #F0F4F8; display: flex; justify-content: center; align-items: center; min-height: 100vh; padding: 20px; }
        .card { background: #161B22; border: 1px solid #30363D; border-radius: 20px; padding: 30px 24px; max-width: 440px; width: 100%; box-shadow: 0 10px 40px rgba(0,0,0,0.6); position: relative; }
        .lang-switch { position: absolute; top: 18px; right: 18px; display: flex; gap: 6px; }
        .lang-btn { background: #21262D; border: 1px solid #30363D; color: #8B949E; border-radius: 8px; padding: 4px 8px; font-size: 12px; font-weight: 600; cursor: pointer; }
        .lang-btn.active { background: #2196F3; color: #FFF; border-color: #2196F3; }
        .header { text-align: center; margin-bottom: 24px; margin-top: 10px; }
        .icon { width: 56px; height: 56px; background: rgba(33, 150, 243, 0.15); border-radius: 16px; display: inline-flex; align-items: center; justify-content: center; margin-bottom: 12px; }
        .icon svg { fill: #2196F3; width: 32px; height: 32px; }
        h1 { font-size: 22px; font-weight: 700; color: #FFF; margin-bottom: 6px; }
        p.subtitle { font-size: 14px; color: #8B949E; line-height: 1.4; }
        .form-group { margin-bottom: 18px; text-align: left; }
        label { display: block; font-size: 13px; font-weight: 600; color: #C9D1D9; margin-bottom: 8px; }
        input { width: 100%; background: #0D1117; border: 1px solid #30363D; border-radius: 12px; padding: 14px 16px; color: #FFF; font-size: 15px; outline: none; transition: border-color 0.2s; }
        input:focus { border-color: #2196F3; }
        button#submitBtn { width: 100%; background: #2196F3; color: #FFF; border: none; border-radius: 12px; padding: 15px; font-size: 16px; font-weight: 700; cursor: pointer; transition: background 0.2s, transform 0.1s; margin-top: 8px; }
        button#submitBtn:hover { background: #1976D2; }
        button#submitBtn:active { transform: scale(0.98); }
        button#submitBtn:disabled { background: #30363D; color: #8B949E; cursor: not-allowed; }
        .alert { margin-top: 18px; padding: 12px 14px; border-radius: 10px; font-size: 14px; display: none; line-height: 1.4; }
        .alert.success { background: rgba(46, 160, 67, 0.15); border: 1px solid #2EA043; color: #3FB950; display: block; }
        .alert.error { background: rgba(248, 81, 73, 0.15); border: 1px solid #F85149; color: #FF7B72; display: block; }
        .help-box { margin-top: 22px; border-top: 1px solid #21262D; padding-top: 16px; font-size: 12px; color: #8B949E; text-align: left; }
        .help-box strong { color: #C9D1D9; }
        .help-box ol { margin-left: 18px; margin-top: 6px; }
        .help-box li { margin-bottom: 4px; }
    </style>
</head>
<body>
    <div class="card">
        <div class="lang-switch">
            <button class="lang-btn" id="btnEs" onclick="setLang('es')">ES</button>
            <button class="lang-btn" id="btnEn" onclick="setLang('en')">EN</button>
        </div>

        <div class="header">
            <div class="icon">
                <svg viewBox="0 0 24 24"><path d="M21 3H3c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h5v2h8v-2h5c1.1 0 1.99-.9 1.99-2L23 5c0-1.1-.9-2-2-2zm0 14H3V5h18v12z"/></svg>
            </div>
            <h1 id="titleTxt">Conectar con tu TV</h1>
            <p class="subtitle" id="subtitleTxt">Pega los datos de tu servidor Immich para vincular la app en tu televisión instantáneamente.</p>
        </div>

        <form id="pairForm">
            <div class="form-group">
                <label for="serverUrl" id="labelUrl">URL del Servidor Immich:</label>
                <input type="url" id="serverUrl" name="serverUrl" placeholder="http://192.168.1.81:2283" required autocomplete="off" autocorrect="off" autocapitalize="off">
            </div>

            <div class="form-group">
                <label for="apiKey" id="labelKey">API Key de Immich:</label>
                <input type="text" id="apiKey" name="apiKey" placeholder="Pega tu API Key..." required autocomplete="off" autocorrect="off" autocapitalize="off">
            </div>

            <button type="submit" id="submitBtn">Vincular Televisión</button>
            <div id="alertBox" class="alert"></div>
        </form>

        <div class="help-box">
            <strong id="helpTitle">¿Cómo obtener tu API Key en Immich?</strong>
            <ol>
                <li id="helpStep1">Abre Immich en tu navegador o app móvil.</li>
                <li id="helpStep2">Ve a <strong>Configuración de la Cuenta</strong> &gt; <strong>Claves de API</strong>.</li>
                <li id="helpStep3">Haz clic en <strong>Nueva clave de API</strong>, cópiala y pégala arriba.</li>
            </ol>
        </div>
    </div>

    <script>
        const i18n = {
            es: {
                title: 'Conectar con tu TV',
                subtitle: 'Pega los datos de tu servidor Immich para vincular la app en tu televisión instantáneamente.',
                labelUrl: 'URL del Servidor Immich:',
                placeholderUrl: 'http://192.168.1.81:2283 o https://...',
                labelKey: 'API Key de Immich:',
                placeholderKey: 'Pega aquí tu clave de API...',
                btnSubmit: 'Vincular Televisión',
                btnVerifying: 'Verificando con la TV...',
                btnConnected: '¡Conectado!',
                btnRetry: 'Reintentar Vinculación',
                successMsg: '🎉 <strong>¡Vinculado con éxito!</strong><br>Mira la pantalla de tu TV, ya ha entrado a tu servidor.',
                errorDefault: 'Error al conectar con el servidor Immich. Revisa la URL y la API Key.',
                errorComm: 'Error de comunicación con la TV. Asegúrate de estar conectado a la misma red WiFi.',
                helpTitle: '¿Cómo obtener tu API Key en Immich?',
                helpStep1: 'Abre Immich en tu navegador o app móvil.',
                helpStep2: 'Ve a <strong>Configuración de la Cuenta</strong> &gt; <strong>Claves de API</strong>.',
                helpStep3: 'Haz clic en <strong>Nueva clave de API</strong>, cópiala y pégala arriba.'
            },
            en: {
                title: 'Connect to your TV',
                subtitle: 'Paste your Immich server details to pair the app on your TV instantly.',
                labelUrl: 'Immich Server URL:',
                placeholderUrl: 'http://192.168.1.81:2283 or https://...',
                labelKey: 'Immich API Key:',
                placeholderKey: 'Paste your API Key here...',
                btnSubmit: 'Pair with TV',
                btnVerifying: 'Verifying with TV...',
                btnConnected: 'Connected!',
                btnRetry: 'Retry Pairing',
                successMsg: '🎉 <strong>Paired successfully!</strong><br>Check your TV screen, it is now logged into your server.',
                errorDefault: 'Error connecting to Immich server. Check the URL and API Key.',
                errorComm: 'Communication error with the TV. Make sure you are on the same Wi-Fi network.',
                helpTitle: 'How to get your Immich API Key?',
                helpStep1: 'Open Immich in your browser or mobile app.',
                helpStep2: 'Go to <strong>Account Settings</strong> &gt; <strong>API Keys</strong>.',
                helpStep3: 'Click on <strong>New API Key</strong>, copy it and paste it above.'
            }
        };

        let currentLang = ((navigator.language || '').toLowerCase().startsWith('es')) ? 'es' : 'en';

        function setLang(lang) {
            currentLang = lang;
            const t = i18n[lang];
            document.getElementById('btnEs').className = 'lang-btn' + (lang === 'es' ? ' active' : '');
            document.getElementById('btnEn').className = 'lang-btn' + (lang === 'en' ? ' active' : '');
            document.getElementById('titleTxt').innerText = t.title;
            document.getElementById('subtitleTxt').innerText = t.subtitle;
            document.getElementById('labelUrl').innerText = t.labelUrl;
            document.getElementById('serverUrl').placeholder = t.placeholderUrl;
            document.getElementById('labelKey').innerText = t.labelKey;
            document.getElementById('apiKey').placeholder = t.placeholderKey;
            document.getElementById('submitBtn').innerText = t.btnSubmit;
            document.getElementById('helpTitle').innerText = t.helpTitle;
            document.getElementById('helpStep1').innerHTML = t.helpStep1;
            document.getElementById('helpStep2').innerHTML = t.helpStep2;
            document.getElementById('helpStep3').innerHTML = t.helpStep3;
        }

        setLang(currentLang);

        const form = document.getElementById('pairForm');
        const submitBtn = document.getElementById('submitBtn');
        const alertBox = document.getElementById('alertBox');

        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const serverUrl = document.getElementById('serverUrl').value.trim();
            const apiKey = document.getElementById('apiKey').value.trim();
            const t = i18n[currentLang];

            submitBtn.disabled = true;
            submitBtn.innerText = t.btnVerifying;
            alertBox.style.display = 'none';

            try {
                const response = await fetch('/submit', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ serverUrl, apiKey })
                });

                const data = await response.json();

                if (response.ok && data.success) {
                    alertBox.className = 'alert success';
                    alertBox.innerHTML = t.successMsg;
                    submitBtn.innerText = t.btnConnected;
                } else {
                    alertBox.className = 'alert error';
                    alertBox.innerText = data.message || t.errorDefault;
                    submitBtn.disabled = false;
                    submitBtn.innerText = t.btnRetry;
                }
            } catch (err) {
                alertBox.className = 'alert error';
                alertBox.innerText = t.errorComm;
                submitBtn.disabled = false;
                submitBtn.innerText = t.btnRetry;
            }
        });
    </script>
</body>
</html>
        """.trimIndent()

        val responseBytes = html.toByteArray(Charsets.UTF_8)
        out.print("HTTP/1.1 200 OK\r\n")
        out.print("Content-Type: text/html; charset=UTF-8\r\n")
        out.print("Content-Length: ${responseBytes.size}\r\n")
        out.print("Connection: close\r\n\r\n")
        out.print(html)
        out.flush()
    }
}
