package com.bbxtudios.immichtv.ui.util

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec

/**
 * BringIntoViewSpec con margen de seguridad para Android TV.
 * Al mantener un margen de al menos el 75% del tamaño del elemento por arriba y por abajo,
 * la fila adyacente SIEMPRE está compuesta y visible en pantalla.
 * Esto garantiza que al pulsar D-Pad Arriba/Abajo, Compose mantenga matemáticamente
 * la columna X exacta sin saltar a la primera o última columna.
 */
@OptIn(ExperimentalFoundationApi::class)
val InstantBringIntoViewSpec = object : BringIntoViewSpec {
    override val scrollAnimationSpec: AnimationSpec<Float> = snap()

    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        // En Android TV, centrar verticalmente el elemento enfocado garantiza que
        // las filas adyacentes (superior e inferior) siempre estén completamente compuestas
        // y visibles, manteniendo la columna X exacta de forma perfecta.
        val itemCenter = offset + (size / 2f)
        val containerCenter = containerSize / 2f
        return itemCenter - containerCenter
    }
}

@OptIn(ExperimentalFoundationApi::class)
val SmoothBringIntoViewSpec = object : BringIntoViewSpec {
    override val scrollAnimationSpec: AnimationSpec<Float> = androidx.compose.animation.core.tween(durationMillis = 150)

    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val itemCenter = offset + (size / 2f)
        val containerCenter = containerSize / 2f
        return itemCenter - containerCenter
    }
}
