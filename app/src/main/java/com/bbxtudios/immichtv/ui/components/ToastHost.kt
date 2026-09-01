package com.bbxtudios.immichtv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.bbxtudios.immichtv.ui.theme.BackgroundElevated
import com.bbxtudios.immichtv.ui.theme.FocusHighlight
import com.bbxtudios.immichtv.ui.theme.TextPrimary

@Composable
fun ToastHost(
    message: String?,
    modifier: Modifier = Modifier
) {
    if (!message.isNullOrBlank()) {
        Popup(
            alignment = Alignment.BottomCenter,
            properties = PopupProperties(
                focusable = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 40.dp)
                    .shadow(20.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(BackgroundElevated.copy(alpha = 0.96f))
                    .border(2.dp, FocusHighlight, RoundedCornerShape(24.dp))
                    .padding(horizontal = 28.dp, vertical = 14.dp)
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                )
            }
        }
    }
}
