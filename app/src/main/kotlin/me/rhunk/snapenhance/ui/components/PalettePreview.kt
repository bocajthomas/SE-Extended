package me.rhunk.snapenhance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import me.rhunk.snapenhance.common.data.theme.AppPalette
import me.rhunk.snapenhance.common.data.theme.ThemeMode


// Old theme box component
@Composable
fun ThemeBox (
    theme: AppPalette,
    isSelected: Boolean,
    selectedThemeMode: ThemeMode,
    onThemeSelected: (AppPalette) -> Unit
) {
    val previewColors = when (selectedThemeMode) {
        ThemeMode.LIGHT -> theme.lightColors
        ThemeMode.DARK -> theme.darkColors
        ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) theme.darkColors else theme.lightColors
    }

    MaterialTheme(colorScheme = previewColors) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .clickable (
                    onClick = { onThemeSelected(theme) },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
                .background(
                    color = colorScheme.surfaceColorAtElevation(3.dp)
                )
                .size(60.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .fillMaxSize(0.75f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Spacer(modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.5f)
                        .background(color = colorScheme.primaryContainer)
                    )

                    Spacer(modifier = Modifier
                        .fillMaxSize()
                        .background(color = colorScheme.tertiaryContainer)
                    )
                }

                Spacer(modifier = Modifier
                    .fillMaxSize(0.5f)
                    .clip(CircleShape)
                    .background(
                        color = if (isSelected) {
                            colorScheme.onPrimary
                        } else {
                            colorScheme.primary
                        }
                    )
                )

                if (isSelected) {
                    Icon(
                        modifier = Modifier.size(36.dp),
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun PalettePreview (
    theme: AppPalette,
    isSelected: Boolean,
    selectedThemeMode: ThemeMode,
    onThemeSelected: (AppPalette) -> Unit
) {
    val previewColors = when (selectedThemeMode) {
        ThemeMode.LIGHT -> theme.lightColors
        ThemeMode.DARK -> theme.darkColors
        ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) theme.darkColors else theme.lightColors
    }

    MaterialTheme(colorScheme = previewColors) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .border(
                    width = 4.dp,
                    color = if (isSelected) {
                        colorScheme.primary
                    } else {
                        DividerDefaults.color
                    },
                    shape = RoundedCornerShape(17.dp),
                )
                .padding(4.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(colorScheme.background)
                .clickable(onClick = { onThemeSelected(theme) }),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.8f)
                        .weight(0.7f)
                        .padding(end = 4.dp)
                        .background(
                            color = colorScheme.onSurface,
                            shape = MaterialTheme.shapes.small,
                        ),
                )

                Box(
                    modifier = Modifier.weight(0.3f),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = colorScheme.primary,
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .padding(start = 8.dp, top = 2.dp)
                    .background(
                        color = DividerDefaults.color,
                        shape = MaterialTheme.shapes.small,
                    )
                    .fillMaxWidth(0.5f)
                    .aspectRatio(2f / 3f),
            ) {
                Row(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(width = 24.dp, height = 16.dp)
                        .clip(RoundedCornerShape(5.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(12.dp)
                            .background(colorScheme.tertiary),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(12.dp)
                            .background(colorScheme.secondary),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Surface(
                    color = colorScheme.surfaceContainer,
                ) {
                    Row(
                        modifier = Modifier
                            .height(32.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(17.dp)
                                .background(
                                    color = colorScheme.primary,
                                    shape = CircleShape,
                                ),
                        )
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .alpha(0.6f)
                                .height(17.dp)
                                .weight(1f)
                                .background(
                                    color = colorScheme.onSurface,
                                    shape = MaterialTheme.shapes.small,
                                ),
                        )
                    }
                }
            }
        }
    }
}