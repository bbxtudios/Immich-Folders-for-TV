package com.bbxtudios.immichtv.ui.components

import android.view.KeyEvent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bbxtudios.immichtv.R
import com.bbxtudios.immichtv.data.model.NavTab
import com.bbxtudios.immichtv.ui.theme.BackgroundDark
import com.bbxtudios.immichtv.ui.theme.BackgroundElevated
import com.bbxtudios.immichtv.ui.theme.BackgroundSurface
import com.bbxtudios.immichtv.ui.theme.FocusGlow
import com.bbxtudios.immichtv.ui.theme.FocusHighlight
import com.bbxtudios.immichtv.ui.theme.ImmichBlue
import com.bbxtudios.immichtv.ui.theme.TextMuted
import com.bbxtudios.immichtv.ui.theme.TextPrimary

@Composable
fun SidebarNav(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    onRequestContentFocus: () -> Unit,
    animFocus: Boolean = true,
    cardShadows: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(96.dp)
            .fillMaxHeight(),
        color = BackgroundSurface,
        tonalElevation = if (cardShadows) 6.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            BackgroundSurface,
                            BackgroundDark.copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(vertical = 20.dp, horizontal = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cabecera con Logo y Menú Principal
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo de la App sin círculo azul de fondo
                Image(
                    painter = painterResource(id = R.drawable.loading_logo),
                    contentDescription = "Immich TV Logo",
                    modifier = Modifier.size(46.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Pestañas de Navegación
                SidebarNavButton(
                    tab = NavTab.FOLDERS,
                    icon = Icons.Rounded.Folder,
                    label = androidx.compose.ui.res.stringResource(R.string.nav_folders),
                    isSelected = selectedTab == NavTab.FOLDERS,
                    animFocus = animFocus,
                    cardShadows = cardShadows,
                    onClick = {
                        onTabSelected(NavTab.FOLDERS)
                        onRequestContentFocus()
                    },
                    onRightKey = onRequestContentFocus
                )

                Spacer(modifier = Modifier.height(12.dp))

                SidebarNavButton(
                    tab = NavTab.ALBUMS,
                    icon = Icons.Rounded.PhotoAlbum,
                    label = androidx.compose.ui.res.stringResource(R.string.nav_albums),
                    isSelected = selectedTab == NavTab.ALBUMS,
                    animFocus = animFocus,
                    cardShadows = cardShadows,
                    onClick = {
                        onTabSelected(NavTab.ALBUMS)
                        onRequestContentFocus()
                    },
                    onRightKey = onRequestContentFocus
                )

                Spacer(modifier = Modifier.height(12.dp))

                SidebarNavButton(
                    tab = NavTab.RANDOM,
                    icon = Icons.Rounded.AutoAwesome,
                    label = androidx.compose.ui.res.stringResource(R.string.nav_random),
                    isSelected = selectedTab == NavTab.RANDOM,
                    animFocus = animFocus,
                    cardShadows = cardShadows,
                    onClick = {
                        onTabSelected(NavTab.RANDOM)
                        onRequestContentFocus()
                    },
                    onRightKey = onRequestContentFocus
                )

                Spacer(modifier = Modifier.height(12.dp))

                SidebarNavButton(
                    tab = NavTab.MEMORIES,
                    icon = Icons.Rounded.History,
                    label = androidx.compose.ui.res.stringResource(R.string.nav_memories),
                    isSelected = selectedTab == NavTab.MEMORIES,
                    animFocus = animFocus,
                    cardShadows = cardShadows,
                    onClick = {
                        onTabSelected(NavTab.MEMORIES)
                        onRequestContentFocus()
                    },
                    onRightKey = onRequestContentFocus
                )
            }

            // Ajustes al fondo
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SidebarNavButton(
                    tab = NavTab.SETTINGS,
                    icon = Icons.Rounded.Settings,
                    label = androidx.compose.ui.res.stringResource(R.string.nav_settings),
                    isSelected = selectedTab == NavTab.SETTINGS,
                    animFocus = animFocus,
                    cardShadows = cardShadows,
                    onClick = {
                        onTabSelected(NavTab.SETTINGS)
                        onRequestContentFocus()
                    },
                    onRightKey = onRequestContentFocus
                )
            }
        }
    }
}

@Composable
private fun SidebarNavButton(
    tab: NavTab,
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    animFocus: Boolean = true,
    cardShadows: Boolean = true,
    onClick: () -> Unit,
    onRightKey: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused && animFocus) 1.08f else 1.0f,
        label = "navItemScale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFocused -> ImmichBlue
            isSelected -> BackgroundElevated
            else -> Color.Transparent
        },
        label = "navItemBg"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isFocused -> TextPrimary
            isSelected -> FocusHighlight
            else -> TextMuted
        },
        label = "navItemColor"
    )

    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.05f)
            .then(if (animFocus) Modifier.scale(scale) else Modifier)
            .then(
                if (cardShadows && isFocused) {
                    Modifier.shadow(
                        elevation = 10.dp,
                        shape = shape,
                        spotColor = FocusGlow
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(backgroundColor)
            .border(
                width = if (isFocused) 2.5.dp else if (isSelected) 1.5.dp else 0.dp,
                color = if (isFocused) FocusHighlight else if (isSelected) Color(0x44FFFFFF) else Color.Transparent,
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            onRightKey()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            onClick()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp,
                    color = contentColor
                ),
                maxLines = 1
            )
        }
    }
}
