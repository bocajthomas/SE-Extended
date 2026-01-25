package me.rhunk.snapenhance.common.data.theme

import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.Color

enum class ThemeMode(val label: String) {
    SYSTEM("auto_button"),
    LIGHT("light_button"),
    DARK("dark_button")
}

fun updateSystemBars(activity: ComponentActivity, themeMode: ThemeMode) {
    val window = activity.window
    val transparentStyle = SystemBarStyle.dark(Color.Transparent.toArgb())

    activity.enableEdgeToEdge(
        statusBarStyle = when (themeMode) {
            ThemeMode.LIGHT -> SystemBarStyle.light(Color.Transparent.toArgb(), Color.Transparent.toArgb())
            ThemeMode.DARK -> SystemBarStyle.dark(Color.Transparent.toArgb())
            ThemeMode.SYSTEM -> SystemBarStyle.auto(Color.Transparent.toArgb(), Color.Transparent.toArgb())
        },
        navigationBarStyle = transparentStyle
    )

    if (themeMode != ThemeMode.SYSTEM) {
        val isLight = themeMode == ThemeMode.LIGHT
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = isLight
            isAppearanceLightNavigationBars = isLight
        }
    }
}