package com.bbxtudios.immichtv.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bbxtudios.immichtv.R

@Composable
fun LoadingSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spinnerTransition")
    
    // Animación pro-spin de Cordova:
    // 0%: 0deg -> 45%: 360deg -> 55%: 360deg (pausa) -> 100%: 720deg
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 720f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                0f at 0 using FastOutSlowInEasing
                360f at 1350 using FastOutSlowInEasing
                360f at 1650
                720f at 3000 using FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.loading_logo),
            contentDescription = "Cargando...",
            modifier = Modifier
                .size(size)
                .graphicsLayer { rotationZ = rotation }
        )
    }
}
