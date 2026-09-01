package com.bbxtudios.immichtv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.bbxtudios.immichtv.data.model.FolderItem
import com.bbxtudios.immichtv.ui.theme.AccentAmber
import androidx.compose.ui.graphics.graphicsLayer
import com.bbxtudios.immichtv.ui.theme.BackgroundElevated
import com.bbxtudios.immichtv.ui.theme.BackgroundSurface
import com.bbxtudios.immichtv.ui.theme.FocusGlow
import com.bbxtudios.immichtv.ui.theme.FocusHighlight
import com.bbxtudios.immichtv.ui.theme.TextPrimary

private val FolderCardShape = RoundedCornerShape(14.dp)

@Composable
fun FolderCard(
    folder: FolderItem,
    onClick: () -> Unit,
    animFocus: Boolean = true,
    cardShadows: Boolean = true,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale = if (isFocused && animFocus) 1.05f else 1.0f

    val shape = FolderCardShape

    BoxWithConstraints(
        modifier = modifier
            .then(
                if (animFocus) {
                    Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                } else {
                    Modifier
                }
            )
            .then(
                if (cardShadows && isFocused) {
                    Modifier.shadow(12.dp, shape, spotColor = FocusGlow)
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    if (isFocused) {
                        listOf(Color(0xFF26324D), Color(0xFF192133))
                    } else {
                        listOf(BackgroundElevated, BackgroundSurface)
                    }
                )
            )
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) FocusHighlight else Color(0x33FFFFFF),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Dimensiones calculadas 100% en base a los porcentajes del tamaño disponible
        val cardWidth = maxWidth
        val cardHeight = maxHeight

        val iconBoxSize = (cardHeight * 0.44f).coerceIn(30.dp, 80.dp)
        val iconSize = iconBoxSize * 0.70f
        val iconCorner = (iconBoxSize * 0.25f).coerceIn(6.dp, 16.dp)

        val fontSize = (cardWidth.value * 0.082f).coerceIn(10.5f, 17f).sp
        val lineHeight = fontSize * 1.25f

        val verticalPadding = (cardHeight * 0.06f).coerceIn(4.dp, 12.dp)
        val horizontalPadding = (cardWidth * 0.06f).coerceIn(6.dp, 14.dp)
        val spacerHeight = (cardHeight * 0.04f).coerceIn(2.dp, 8.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(iconBoxSize)
                    .clip(RoundedCornerShape(iconCorner))
                    .background(
                        Brush.radialGradient(
                            listOf(
                                AccentAmber.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Folder,
                    contentDescription = folder.name,
                    tint = if (isFocused) AccentAmber else AccentAmber.copy(alpha = 0.9f),
                    modifier = Modifier.size(iconSize)
                )
            }

            Spacer(modifier = Modifier.height(spacerHeight))

            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    color = TextPrimary
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}




