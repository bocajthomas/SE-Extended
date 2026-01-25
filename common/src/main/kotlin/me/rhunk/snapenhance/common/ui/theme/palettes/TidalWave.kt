package me.rhunk.snapenhance.common.ui.theme.palettes

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import me.rhunk.snapenhance.common.data.theme.AppPalette
/**
 * Colors for Tidal Wave theme
 * Original color scheme by NahutabDevelop
 *
 * Key colors:
 * Primary #004152
 * Secondary #5ed4fc
 * Tertiary #92f7bc
 * Neutral #16151D
 */
private val primaryLight = Color(0xFF006780)
private val onPrimaryLight = Color(0xFFffffff)
private val primaryContainerLight = Color(0xFFB4D4DF)
private val onPrimaryContainerLight = Color(0xFF001f28)
private val secondaryLight = Color(0xFF006780)
private val onSecondaryLight = Color(0xFFffffff)
private val secondaryContainerLight = Color(0xFF9AE1FF)
private val onSecondaryContainerLight = Color(0xFF001f28)
private val tertiaryLight = Color(0xFF92f7bc)
private val onTertiaryLight = Color(0xFF001c3b)
private val tertiaryContainerLight = Color(0xFFc3fada)
private val onTertiaryContainerLight = Color(0xFF78ffd6)
private val errorLight = Color(0xFFBA1A1A) // Default Material Error
private val onErrorLight = Color(0xFFFFFFFF)
private val errorContainerLight = Color(0xFFFFDAD6)
private val onErrorContainerLight = Color(0xFF410002)
private val backgroundLight = Color(0xFFfdfbff)
private val onBackgroundLight = Color(0xFF001c3b)
private val surfaceLight = Color(0xFFfdfbff)
private val onSurfaceLight = Color(0xFF001c3b)
private val surfaceVariantLight = Color(0xFFe8eff5)
private val onSurfaceVariantLight = Color(0xFF40484c)
private val outlineLight = Color(0xFF70787c)
private val outlineVariantLight = Color(0xFFbfc8cc)
private val scrimLight = Color(0xFF000000)
private val inverseSurfaceLight = Color(0xFF020400)
private val inverseOnSurfaceLight = Color(0xFFffe3c4)
private val inversePrimaryLight = Color(0xFFff987f)
private val surfaceDimLight = Color(0xFFd5dbdf)
private val surfaceBrightLight = Color(0xFFfdfbff)
private val surfaceContainerLowestLight = Color(0xFFe2e8ec)
private val surfaceContainerLowLight = Color(0xFFe5ecf1)
private val surfaceContainerLight = Color(0xFFe8eff5)
private val surfaceContainerHighLight = Color(0xFFedf4fA)
private val surfaceContainerHighestLight = Color(0xFFf5faff)

private val primaryDark = Color(0xFF5ed4fc)
private val onPrimaryDark = Color(0xFF003544)
private val primaryContainerDark = Color(0xFF004d61)
private val onPrimaryContainerDark = Color(0xFFb8eaff)
private val secondaryDark = Color(0xFF5ed4fc)
private val onSecondaryDark = Color(0xFF003544)
private val secondaryContainerDark = Color(0xFF004d61)
private val onSecondaryContainerDark = Color(0xFFb8eaff)
private val tertiaryDark = Color(0xFF92f7bc)
private val onTertiaryDark = Color(0xFF001c3b)
private val tertiaryContainerDark = Color(0xFFc3fada)
private val onTertiaryContainerDark = Color(0xFF78ffd6)
private val errorDark = Color(0xFFFFB4AB) // Default Material Error
private val onErrorDark = Color(0xFF690005)
private val errorContainerDark = Color(0xFF93000A)
private val onErrorContainerDark = Color(0xFFFFDAD6)
private val backgroundDark = Color(0xFF001c3b)
private val onBackgroundDark = Color(0xFFd5e3ff)
private val surfaceDark = Color(0xFF001c3b)
private val onSurfaceDark = Color(0xFFd5e3ff)
private val surfaceVariantDark = Color(0xFF082b4b)
private val onSurfaceVariantDark = Color(0xFFbfc8cc)
private val outlineDark = Color(0xFF8a9296)
private val outlineVariantDark = Color(0xFF40484c)
private val scrimDark = Color(0xFF000000)
private val inverseSurfaceDark = Color(0xFFffe3c4)
private val inverseOnSurfaceDark = Color(0xFF001c3b)
private val inversePrimaryDark = Color(0xFFa12b03)
private val surfaceDimDark = Color(0xFF001c3b)
private val surfaceBrightDark = Color(0xFF082b4b)
private val surfaceContainerLowestDark = Color(0xFF072642)
private val surfaceContainerLowDark = Color(0xFF072947)
private val surfaceContainerDark = Color(0xFF082b4b)
private val surfaceContainerHighDark = Color(0xFF093257)
private val surfaceContainerHighestDark = Color(0xFF0A3861)

val TidalWave = AppPalette(
    key = "tidal_wave",
    lightColors = lightColorScheme(
        primary = primaryLight,
        onPrimary = onPrimaryLight,
        primaryContainer = primaryContainerLight,
        onPrimaryContainer = onPrimaryContainerLight,
        secondary = secondaryLight,
        onSecondary = onSecondaryLight,
        secondaryContainer = secondaryContainerLight,
        onSecondaryContainer = onSecondaryContainerLight,
        tertiary = tertiaryLight,
        onTertiary = onTertiaryLight,
        tertiaryContainer = tertiaryContainerLight,
        onTertiaryContainer = onTertiaryContainerLight,
        error = errorLight,
        onError = onErrorLight,
        errorContainer = errorContainerLight,
        onErrorContainer = onErrorContainerLight,
        background = backgroundLight,
        onBackground = onBackgroundLight,
        surface = surfaceLight,
        onSurface = onSurfaceLight,
        surfaceVariant = surfaceVariantLight,
        onSurfaceVariant = onSurfaceVariantLight,
        outline = outlineLight,
        outlineVariant = outlineVariantLight,
        scrim = scrimLight,
        inverseSurface = inverseSurfaceLight,
        inverseOnSurface = inverseOnSurfaceLight,
        inversePrimary = inversePrimaryLight,
        surfaceDim = surfaceDimLight,
        surfaceBright = surfaceBrightLight,
        surfaceContainerLowest = surfaceContainerLowestLight,
        surfaceContainerLow = surfaceContainerLowLight,
        surfaceContainer = surfaceContainerLight,
        surfaceContainerHigh = surfaceContainerHighLight,
        surfaceContainerHighest = surfaceContainerHighestLight,
    ),

    darkColors = darkColorScheme(
        primary = primaryDark,
        onPrimary = onPrimaryDark,
        primaryContainer = primaryContainerDark,
        onPrimaryContainer = onPrimaryContainerDark,
        secondary = secondaryDark,
        onSecondary = onSecondaryDark,
        secondaryContainer = secondaryContainerDark,
        onSecondaryContainer = onSecondaryContainerDark,
        tertiary = tertiaryDark,
        onTertiary = onTertiaryDark,
        tertiaryContainer = tertiaryContainerDark,
        onTertiaryContainer = onTertiaryContainerDark,
        error = errorDark,
        onError = onErrorDark,
        errorContainer = errorContainerDark,
        onErrorContainer = onErrorContainerDark,
        background = backgroundDark,
        onBackground = onBackgroundDark,
        surface = surfaceDark,
        onSurface = onSurfaceDark,
        surfaceVariant = surfaceVariantDark,
        onSurfaceVariant = onSurfaceVariantDark,
        outline = outlineDark,
        outlineVariant = outlineVariantDark,
        scrim = scrimDark,
        inverseSurface = inverseSurfaceDark,
        inverseOnSurface = inverseOnSurfaceDark,
        inversePrimary = inversePrimaryDark,
        surfaceDim = surfaceDimDark,
        surfaceBright = surfaceBrightDark,
        surfaceContainerLowest = surfaceContainerLowestDark,
        surfaceContainerLow = surfaceContainerLowDark,
        surfaceContainer = surfaceContainerDark,
        surfaceContainerHigh = surfaceContainerHighDark,
        surfaceContainerHighest = surfaceContainerHighestDark,
    )
)