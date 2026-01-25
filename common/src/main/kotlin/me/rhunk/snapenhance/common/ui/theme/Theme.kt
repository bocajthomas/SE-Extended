package me.rhunk.snapenhance.common.ui.theme

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat.getInsetsController
import me.rhunk.snapenhance.common.data.theme.AppPalette
import me.rhunk.snapenhance.common.data.theme.ThemeMode
import me.rhunk.snapenhance.common.ui.rememberMutableStringPreferenceState
import me.rhunk.snapenhance.common.ui.theme.palettes.*

object Themes {
    @Composable
    fun getPalettes(context: Context): List<AppPalette> {
        val palettes = mutableListOf<AppPalette>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            palettes.add(AppPalette(
                key = "dynamic",
                lightColors = dynamicLightColorScheme(context),
                darkColors = dynamicDarkColorScheme(context)
            ))
        }

        palettes.addAll(
            listOf(
                Monochrome,
                PurrAurora,
                StrawberryDaiquiri,
                TidalWave,
                GreenApple
            )
        )

        return palettes
    }

    fun getPalettesByName(key: String, context: Context): AppPalette {
        return when (key) {
            "monochrome" -> Monochrome
            "purraurora" -> PurrAurora
            "strawberry_daiquiri" -> StrawberryDaiquiri
            "tidal_wave" -> TidalWave
            "green_apple" -> GreenApple
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    AppPalette(
                        key = "dynamic",
                        lightColors = dynamicLightColorScheme(context),
                        darkColors = dynamicDarkColorScheme(context)
                    )
                } else {
                    Monochrome
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppMaterialTheme(
    preferences: SharedPreferences,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val defaultPalettesName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "Dynamic" else Monochrome.key
    val selectedPaletteName by preferences.rememberMutableStringPreferenceState(
        key = "theme_name",
        defaultValue = defaultPalettesName
    )
    val selectedTheme = Themes.getPalettesByName(selectedPaletteName, context)
    val themeMode by preferences.rememberMutableStringPreferenceState(
        key = "theme_mode",
        defaultValue = ThemeMode.SYSTEM.name
    )
    val systemDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        ThemeMode.LIGHT.name -> false
        ThemeMode.DARK.name -> true
        ThemeMode.SYSTEM.name -> systemDarkTheme
        else -> systemDarkTheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (context as android.app.Activity).window
            val insetsController = getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !useDarkTheme
            insetsController.isAppearanceLightNavigationBars = !useDarkTheme
        }
    }

    val colorScheme: ColorScheme = if (useDarkTheme) {
        selectedTheme.darkColors
    } else {
        selectedTheme.lightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
        motionScheme = MotionScheme.expressive()
    )
}