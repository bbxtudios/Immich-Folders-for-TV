package com.bbxtudios.immichtv.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

object LocaleHelper {

    const val LANG_AUTO = "auto"
    const val LANG_SPANISH = "es"
    const val LANG_ENGLISH = "en"

    /**
     * Resuelve el Locale efectivo para la app según el código configurado.
     * Si es "auto":
     *   - Si el idioma del sistema es español (es), devuelve Locale("es").
     *   - Para cualquier otro país/idioma del sistema, devuelve Locale("en") como fallback internacional.
     */
    fun resolveLocale(languageCode: String): Locale {
        return when (languageCode.lowercase().trim()) {
            LANG_SPANISH -> Locale("es")
            LANG_ENGLISH -> Locale("en")
            else -> {
                // "auto" o no definido: comprobar idioma del sistema operativo
                val systemLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    LocaleList.getDefault().get(0) ?: Locale.getDefault()
                } else {
                    Locale.getDefault()
                }

                if (systemLocale.language.equals("es", ignoreCase = true)) {
                    Locale("es")
                } else {
                    Locale("en") // Inglés por defecto para el resto de países
                }
            }
        }
    }

    /**
     * Aplica el Locale a nivel de sistema, proceso JVM y configuración de recursos del Context y Application.
     */
    fun applyLocale(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            config.setLocales(localeList)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }

        try {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        } catch (_: Exception) {}

        try {
            @Suppress("DEPRECATION")
            context.applicationContext?.resources?.updateConfiguration(config, context.resources.displayMetrics)
        } catch (_: Exception) {}

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        } else {
            context
        }
    }
}

/**
 * Envoltorio composable que inyecta dinámicamente el Locale seleccionado en todo el árbol de Compose
 * permitiendo cambiar el idioma en tiempo real sin reiniciar la aplicación.
 */
@Composable
fun ProvideAppLanguage(
    languageCode: String,
    content: @Composable () -> Unit
) {
    val currentContext = LocalContext.current
    val targetLocale = remember(languageCode) {
        LocaleHelper.resolveLocale(languageCode)
    }

    val localizedContext = remember(currentContext, targetLocale) {
        LocaleHelper.applyLocale(currentContext, targetLocale)
    }

    val localizedConfiguration = remember(targetLocale, localizedContext) {
        val config = Configuration(localizedContext.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(targetLocale)
            config.setLocales(LocaleList(targetLocale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = targetLocale
        }
        config
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedConfiguration
    ) {
        content()
    }
}

/**
 * Componente Dialog que garantiza que los recursos del diálogo usen la configuración localizada activa.
 */
@Composable
fun LocalizedDialog(
    onDismissRequest: () -> Unit,
    properties: androidx.compose.ui.window.DialogProperties = androidx.compose.ui.window.DialogProperties(),
    content: @Composable () -> Unit
) {
    val currentContext = LocalContext.current
    val currentConfig = LocalConfiguration.current

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        CompositionLocalProvider(
            LocalContext provides currentContext,
            LocalConfiguration provides currentConfig
        ) {
            content()
        }
    }
}
