package me.rhunk.snapenhance.common.ui.theme.palettes

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import me.rhunk.snapenhance.common.data.theme.AppPalette

private val purrDeepViolet = Color(0xFF261F58)
private val purrPanel = Color(0xFF322B5E)
private val purrGlowPrimary = Color(0xFF8C7BFF)
private val purrGlowSecondary = Color(0xFF5FD8FF)
private val purrTextSecondary = Color(0xFFD9D3FF)
private val purrLightBackground = Color(0xFFF7F6FF)
private val purrLightContainer = Color(0xFFEDE9FF)

val PurrAurora = AppPalette(
    key = "purraurora",
    lightColors = lightColorScheme(
        primary = Color(0xFF5C4B99),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE5DEFF),
        onPrimaryContainer = Color(0xFF17005B),
        secondary = Color(0xFF00687A),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFABEDFF),
        onSecondaryContainer = Color(0xFF001F26),
        tertiary = Color(0xFF7A5292),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFF7D8FF),
        onTertiaryContainer = Color(0xFF2E004E),
        error = Color(0xFFBA1A1A),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = purrLightBackground,
        onBackground = purrDeepViolet,
        surface = purrLightBackground,
        onSurface = purrDeepViolet,
        surfaceVariant = purrLightContainer,
        onSurfaceVariant = purrDeepViolet,
        outline = Color(0xFF79757F),
        outlineVariant = Color(0xFFC9C4D0),
        scrim = Color.Black,
        inverseSurface = purrDeepViolet,
        inverseOnSurface = Color(0xFFF2F0F7),
        inversePrimary = purrGlowPrimary,
        surfaceDim = Color(0xFFDCD9E0),
        surfaceBright = Color.White,
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF2EFFF),
        surfaceContainer = Color(0xFFEBE6FF),
        surfaceContainerHigh = Color(0xFFE2DBFF),
        surfaceContainerHighest = Color(0xFFD9D1FF)
    ),

    darkColors = darkColorScheme(
        primary = purrGlowPrimary,
        onPrimary = Color(0xFF261F58),
        primaryContainer = purrPanel,
        onPrimaryContainer = purrTextSecondary,
        secondary = purrGlowSecondary,
        onSecondary = Color(0xFF003642),
        secondaryContainer = Color(0xFF004E5E),
        onSecondaryContainer = Color(0xFFA1EFFF),
        tertiary = purrTextSecondary,
        onTertiary = Color(0xFF302A6D),
        tertiaryContainer = Color(0xFF474086),
        onTertiaryContainer = Color(0xFFFFD9FF),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = purrDeepViolet,
        onBackground = Color.White,
        surface = purrDeepViolet,
        onSurface = Color.White,
        surfaceVariant = purrPanel,
        onSurfaceVariant = purrTextSecondary,
        outline = Color(0xFF938F99),
        outlineVariant = Color(0xFF48454E),
        scrim = Color.Black,
        inverseSurface = Color(0xFFE5E1E9),
        inverseOnSurface = Color(0xFF1B1B23),
        inversePrimary = Color(0xFF5C4B99),
        surfaceDim = Color(0xFF12121A),
        surfaceBright = Color(0xFF3D3682),
        surfaceContainerLowest = Color(0xFF1A1640),
        surfaceContainerLow = Color(0xFF2D256A),
        surfaceContainer = purrPanel,
        surfaceContainerHigh = Color(0xFF3D3682),
        surfaceContainerHighest = Color(0xFF4C43A0)
    )
)