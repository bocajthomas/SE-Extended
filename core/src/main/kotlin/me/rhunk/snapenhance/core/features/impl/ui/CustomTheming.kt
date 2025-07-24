package me.rhunk.snapenhance.core.features.impl.ui

import android.content.res.TypedArray
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.ParcelFileDescriptor.AutoCloseInputStream
import android.util.TypedValue
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.ui.graphics.toArgb
import com.google.gson.reflect.TypeToken
import me.rhunk.snapenhance.common.bridge.FileHandleScope
import me.rhunk.snapenhance.common.data.DatabaseThemeContent
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.ktx.getIdentifier
import me.rhunk.snapenhance.core.util.ktx.getObjectField

class CustomTheming: Feature("Custom Theming") {
    private fun getAttribute(name: String): Int {
        return context.resources.getIdentifier(name, "attr")
    }

    private fun parseAttributeList(vararg attributes: Pair<String, Number>): Map<Int, Int> {
        return attributes.toMap().mapKeys {
            getAttribute(it.key)
        }.filterKeys { it != 0 }.mapValues {
            it.value.toInt()
        }
    }

    override fun init() {
        val customThemeName = context.config.userInterface.customTheme.getNullable() ?: return
        var currentTheme = mapOf<Int, Int>() // resource id -> color

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val colorScheme = dynamicDarkColorScheme(context.androidContext)
            val light = customThemeName == "material_you_light"
            val surfaceVariant = (if (light) colorScheme.surfaceVariant else colorScheme.onSurfaceVariant).toArgb()
            val background = (if (light) colorScheme.onBackground else colorScheme.background).toArgb()

            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to surfaceVariant,
                "sigColorChatChat" to surfaceVariant,
                "sigColorChatPendingSending" to surfaceVariant,
                "sigColorChatSnapWithSound" to surfaceVariant,
                "sigColorChatSnapWithoutSound" to surfaceVariant,
                "sigColorBackgroundMain" to background,
                "sigColorBackgroundSurface" to background,
                "listDivider" to colorScheme.onPrimary.copy(alpha = 0.12f).toArgb(),
                "actionSheetBackgroundDrawable" to background,
                "actionSheetRoundedBackgroundDrawable" to background,
                "sigExceptionColorCameraGridLines" to background,
            )
        }

        if (customThemeName == "amoled_dark_mode") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFFFFFFFF,
                "sigColorChatChat" to 0xFFFFFFFF,
                "sigColorChatPendingSending" to 0xFFFFFFFF,
                "sigColorChatSnapWithSound" to 0xFFFFFFFF,
                "sigColorChatSnapWithoutSound" to 0xFFFFFFFF,
                "sigColorBackgroundMain" to 0xFF000000,
                "sigColorBackgroundSurface" to 0xFF000000,
                "listDivider" to 0xFF000000,
                "actionSheetBackgroundDrawable" to 0xFF000000,
                "actionSheetRoundedBackgroundDrawable" to 0xFF000000,
                "sigExceptionColorCameraGridLines" to 0xFF000000,
            )
        }
        if (customThemeName == "light_blue") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFF03BAFC,
                "sigColorBackgroundMain" to 0xFFBDE6FF,
                "sigColorBackgroundSurface" to 0xFF78DBFF,
                "listDivider" to 0xFFBDE6FF,
                "actionSheetBackgroundDrawable" to 0xFF78DBFF,
                "sigColorChatChat" to 0xFF08D6FF,
                "sigColorChatPendingSending" to 0xFF08D6FF,
                "sigColorChatSnapWithSound" to 0xFF08D6FF,
                "sigColorChatSnapWithoutSound" to 0xFF08D6FF,
                "sigExceptionColorCameraGridLines" to 0xFF08D6FF,
            )
        }
        if (customThemeName == "dark_blue") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFF98C2FD,
                "sigColorBackgroundMain" to 0xFF192744,
                "sigColorBackgroundSurface" to 0xFF192744,
                "actionSheetBackgroundDrawable" to 0xFF192744,
                "sigColorChatChat" to 0xFF98C2FD,
                "sigColorChatPendingSending" to 0xFF98C2FD,
                "sigColorChatSnapWithSound" to 0xFF98C2FD,
                "sigColorChatSnapWithoutSound" to 0xFF98C2FD,
                "sigExceptionColorCameraGridLines" to 0xFF192744,
            )
        }
        if (customThemeName == "midnight_slate") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFFFFFFFF,
                "sigColorBackgroundMain" to 0xFF1F2833,
                "sigColorBackgroundSurface" to 0xFF1F2833,
                "actionSheetBackgroundDrawable" to 0xFF2C3E50,
                "actionSheetRoundedBackgroundDrawable" to 0xFF34495E,
                "listDivider" to 0xFF1F2833,
                "sigColorChatChat" to 0xFFFFFF00,
                "sigColorChatPendingSending" to 0xFF00FFFF,
                "sigColorChatSnapWithSound" to 0xFFFF9800,
                "sigColorChatSnapWithoutSound" to 0xFF00E676,
                "sigExceptionColorCameraGridLines" to 0xFFFFFFFF,
                "sigColorIconPrimary" to 0xFFFFFF00,
            )
        }
        if (customThemeName == "earthy_autumn") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFFF7CAC9,
                "sigColorBackgroundMain" to 0xFF800000,
                "sigColorBackgroundSurface" to 0xFF800000,
                "actionSheetBackgroundDrawable" to 0xFF800000,
                "sigColorChatChat" to 0xFFF7CAC9,
                "sigColorChatPendingSending" to 0xFFF7CAC9,
                "sigColorChatSnapWithSound" to 0xFFF7CAC9,
                "sigColorChatSnapWithoutSound" to 0xFFF7CAC9,
                "sigExceptionColorCameraGridLines" to 0xFF800000,
            )
        }
        if (customThemeName == "mint_chocolate") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFFFFFFFF,
                "sigColorBackgroundMain" to 0xFF98FF98,
                "sigColorBackgroundSurface" to 0xFF98FF98,
                "actionSheetBackgroundDrawable" to 0xFF98FF98,
                "sigColorChatChat" to 0xFFFFFFFF,
                "sigColorChatPendingSending" to 0xFFFFFFFF,
                "sigColorChatSnapWithSound" to 0xFFFFFFFF,
                "sigColorChatSnapWithoutSound" to 0xFFFFFFFF,
                "sigExceptionColorCameraGridLines" to 0xFF98FF98,
            )
        }
        if (customThemeName == "ginger_snap") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFFFFFFFF,
                "sigColorBackgroundMain" to 0xFFC6893A,
                "sigColorBackgroundSurface" to 0xFFC6893A,
                "actionSheetBackgroundDrawable" to 0xFFC6893A,
                "sigColorChatChat" to 0xFFFFFFFF,
                "sigColorChatPendingSending" to 0xFFFFFFFF,
                "sigColorChatSnapWithSound" to 0xFFFFFFFF,
                "sigColorChatSnapWithoutSound" to 0xFFFFFFFF,
                "sigExceptionColorCameraGridLines" to 0xFFC6893A,
            )
        }
        if (customThemeName == "lemon_meringue") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFF000000,
                "sigColorBackgroundMain" to 0xFFFCFFE7,
                "sigColorBackgroundSurface" to 0xFFFCFFE7,
                "actionSheetBackgroundDrawable" to 0xFFFCFFE7,
                "sigColorChatChat" to 0xFF000000,
                "sigColorChatPendingSending" to 0xFF000000,
                "sigColorChatSnapWithSound" to 0xFF000000,
                "sigColorChatSnapWithoutSound" to 0xFF000000,
                "sigExceptionColorCameraGridLines" to 0xFFFCFFE7,
            )
        }
        if (customThemeName == "lava_flow") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFFFFCC00,
                "sigColorBackgroundMain" to 0xFFC70039,
                "sigColorBackgroundSurface" to 0xFFC70039,
                "actionSheetBackgroundDrawable" to 0xFFC70039,
                "sigColorChatChat" to 0xFFFFCC00,
                "sigColorChatPendingSending" to 0xFFFFCC00,
                "sigColorChatSnapWithSound" to 0xFFFFCC00,
                "sigColorChatSnapWithoutSound" to 0xFFFFCC00,
                "sigExceptionColorCameraGridLines" to 0xFFC70039,
            )
        }
        if (customThemeName == "ocean_fog") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFF333333,
                "sigColorBackgroundMain" to 0xFFB0C4DE,
                "sigColorBackgroundSurface" to 0xFFB0C4DE,
                "actionSheetBackgroundDrawable" to 0xFFB0C4DE,
                "sigColorChatChat" to 0xFF333333,
                "sigColorChatPendingSending" to 0xFF333333,
                "sigColorChatSnapWithSound" to 0xFF333333,
                "sigColorChatSnapWithoutSound" to 0xFF333333,
                "sigExceptionColorCameraGridLines" to 0xFFB0C4DE,
            )
        }
        if (customThemeName == "alien_landscape") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFFFFFFFF,
                "sigColorBackgroundMain" to 0xFF9B59B6,
                "sigColorBackgroundSurface" to 0xFF9B59B6,
                "actionSheetBackgroundDrawable" to 0xFF9B59B6,
                "sigColorChatChat" to 0xFFFFFFFF,
                "sigColorChatPendingSending" to 0xFFFFFFFF,
                "sigColorChatSnapWithSound" to 0xFFFFFFFF,
                "sigColorChatSnapWithoutSound" to 0xFFFFFFFF,
                "sigExceptionColorCameraGridLines" to 0xFF9B59B6,
            )
        }
        if (customThemeName == "watercolor_wash") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFF3F51B5,
                "sigColorBackgroundMain" to 0xFFFFF5F3,
                "sigColorBackgroundSurface" to 0xFFFFF5F3,
                "actionSheetBackgroundDrawable" to 0xFFFFF5F3,
                "sigColorChatChat" to 0xFF3F51B5,
                "sigColorChatPendingSending" to 0xFF3F51B5,
                "sigColorChatSnapWithSound" to 0xFF3F51B5,
                "sigColorChatSnapWithoutSound" to 0xFF3F51B5,
                "sigExceptionColorCameraGridLines" to 0xFFFFF5F3,
            )
        }
        if (customThemeName == "zesty_lemon") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFF222222,
                "sigColorBackgroundMain" to 0xFFFFFFE0,
                "sigColorBackgroundSurface" to 0xFFFFFFE0,
                "actionSheetBackgroundDrawable" to 0xFFFFFFE0,
                "sigColorChatChat" to 0xFF222222,
                "sigColorChatPendingSending" to 0xFF222222,
                "sigColorChatSnapWithSound" to 0xFF222222,
                "sigColorChatSnapWithoutSound" to 0xFF222222,
                "sigExceptionColorCameraGridLines" to 0xFFFFFFE0,
            )
        }
        if (customThemeName == "tropical_paradise") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFF000000,
                "sigColorBackgroundMain" to 0xFFD3FFCE,
                "sigColorBackgroundSurface" to 0xFFD3FFCE,
                "actionSheetBackgroundDrawable" to 0xFFD3FFCE,
                "sigColorChatChat" to 0xFF000000,
                "sigColorChatPendingSending" to 0xFF000000,
                "sigColorChatSnapWithSound" to 0xFF000000,
                "sigColorChatSnapWithoutSound" to 0xFF000000,
                "sigExceptionColorCameraGridLines" to 0xFFD3FFCE,
            )
        }
        if (customThemeName == "industrial_chic") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFF424242,
                "sigColorBackgroundMain" to 0xFFEEEEEE,
                "sigColorBackgroundSurface" to 0xFFEEEEEE,
                "actionSheetBackgroundDrawable" to 0xFFEEEEEE,
                "sigColorChatChat" to 0xFF424242,
                "sigColorChatPendingSending" to 0xFF424242,
                "sigColorChatSnapWithSound" to 0xFF424242,
                "sigColorChatSnapWithoutSound" to 0xFF424242,
                "sigExceptionColorCameraGridLines" to 0xFFEEEEEE,
            )
        }
        if (customThemeName == "cherry_bomb") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFFFFFFFF,
                "sigColorBackgroundMain" to 0xFFC24641,
                "sigColorBackgroundSurface" to 0xFFC24641,
                "actionSheetBackgroundDrawable" to 0xFFC24641,
                "sigColorChatChat" to 0xFFFFFFFF,
                "sigColorChatPendingSending" to 0xFFFFFFFF,
                "sigColorChatSnapWithSound" to 0xFFFFFFFF,
                "sigColorChatSnapWithoutSound" to 0xFFFFFFFF,
                "sigExceptionColorCameraGridLines" to 0xFFC24641,
            )
        }
        if (customThemeName == "woodland_mystery") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFFC2C2F0,
                "sigColorBackgroundMain" to 0xFF333333,
                "sigColorBackgroundSurface" to 0xFF333333,
                "actionSheetBackgroundDrawable" to 0xFF333333,
                "sigColorChatChat" to 0xFFC2C2F0,
                "sigColorChatPendingSending" to 0xFFC2C2F0,
                "sigColorChatSnapWithSound" to 0xFFC2C2F0,
                "sigColorChatSnapWithoutSound" to 0xFFC2C2F0,
                "sigExceptionColorCameraGridLines" to 0xFF333333,
            )
        }
        if (customThemeName == "galaxy_glitter") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFFFFFFFF,
                "sigColorBackgroundMain" to 0xFF2F4F4F,
                "sigColorBackgroundSurface" to 0xFF2F4F4F,
                "actionSheetBackgroundDrawable" to 0xFF2F4F4F,
                "sigColorChatChat" to 0xFFFFFFFF,
                "sigColorChatPendingSending" to 0xFFFFFFFF,
                "sigColorChatSnapWithSound" to 0xFFFFFFFF,
                "sigColorChatSnapWithoutSound" to 0xFFFFFFFF,
                "sigExceptionColorCameraGridLines" to 0xFF2F4F4F,
            )
        }
        if (customThemeName == "creamy_vanilla") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFF333333,
                "sigColorBackgroundMain" to 0xFFF1F1F1,
                "sigColorBackgroundSurface" to 0xFFF1F1F1,
                "actionSheetBackgroundDrawable" to 0xFFF1F1F1,
                "sigColorChatChat" to 0xFF333333,
                "sigColorChatPendingSending" to 0xFF333333,
                "sigColorChatSnapWithSound" to 0xFF333333,
                "sigColorChatSnapWithoutSound" to 0xFF333333,
                "sigExceptionColorCameraGridLines" to 0xFFF1F1F1,
            )
        }
        if (customThemeName == "spicy_chili") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFFF5F5F5,
                "sigColorBackgroundMain" to 0xFFC70039,
                "sigColorBackgroundSurface" to 0xFFC70039,
                "actionSheetBackgroundDrawable" to 0xFFC70039,
                "sigColorChatChat" to 0xFFF5F5F5,
                "sigColorChatPendingSending" to 0xFFF5F5F5,
                "sigColorChatSnapWithSound" to 0xFFF5F5F5,
                "sigColorChatSnapWithoutSound" to 0xFFF5F5F5,
                "sigExceptionColorCameraGridLines" to 0xFFC70039,
            )
        }
        if (customThemeName == "spring_meadow") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFF388E3C,
                "sigColorBackgroundMain" to 0xFFF5FBE0,
                "sigColorBackgroundSurface" to 0xFFF5FBE0,
                "actionSheetBackgroundDrawable" to 0xFFF5FBE0,
                "sigColorChatChat" to 0xFF388E3C,
                "sigColorChatPendingSending" to 0xFF388E3C,
                "sigColorChatSnapWithSound" to 0xFF388E3C,
                "sigColorChatSnapWithoutSound" to 0xFF388E3C,
                "sigExceptionColorCameraGridLines" to 0xFFF5FBE0,
            )
        }
        if (customThemeName == "midnight_library") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFFEAEAEA,
                "sigColorBackgroundMain" to 0xFF424242,
                "sigColorBackgroundSurface" to 0xFF424242,
                "actionSheetBackgroundDrawable" to 0xFF424242,
                "sigColorChatChat" to 0xFFEAEAEA,
                "sigColorChatPendingSending" to 0xFFEAEAEA,
                "sigColorChatSnapWithSound" to 0xFFEAEAEA,
                "sigColorChatSnapWithoutSound" to 0xFFEAEAEA,
                "sigExceptionColorCameraGridLines" to 0xFF424242,
            )
        }
        if (customThemeName == "lemon_sorbet") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFF000000,
                "sigColorBackgroundMain" to 0xFFFCFFE7,
                "sigColorBackgroundSurface" to 0xFFFCFFE7,
                "actionSheetBackgroundDrawable" to 0xFFFCFFE7,
                "sigColorChatChat" to 0xFF000000,
                "sigColorChatPendingSending" to 0xFF000000,
                "sigColorChatSnapWithSound" to 0xFF000000,
                "sigColorChatSnapWithoutSound" to 0xFF000000,
                "sigExceptionColorCameraGridLines" to 0xFFFCFFE7,
            )
        }
        if (customThemeName == "cosmic_night") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFFFFFFFF,
                "sigColorBackgroundMain" to 0xFF2F4F4F,
                "sigColorBackgroundSurface" to 0xFF2F4F4F,
                "actionSheetBackgroundDrawable" to 0xFF2F4F4F,
                "sigColorChatChat" to 0xFFFFFFFF,
                "sigColorChatPendingSending" to 0xFFFFFFFF,
                "sigColorChatSnapWithSound" to 0xFFFFFFFF,
                "sigColorChatSnapWithoutSound" to 0xFFFFFFFF,
                "sigExceptionColorCameraGridLines" to 0xFF2F4F4F,
            )
        }
        if (customThemeName == "spicy_mustard") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFFFFFFFF,
                "sigColorBackgroundMain" to 0xFFFCC01E,
                "sigColorBackgroundSurface" to 0xFFFCC01E,
                "actionSheetBackgroundDrawable" to 0xFFFCC01E,
                "sigColorChatChat" to 0xFFFFFFFF,
                "sigColorChatPendingSending" to 0xFFFFFFFF,
                "sigColorChatSnapWithSound" to 0xFFFFFFFF,
                "sigColorChatSnapWithoutSound" to 0xFFFFFFFF,
                "sigExceptionColorCameraGridLines" to 0xFFFCC01E,
            )
        }
        if (customThemeName == "gingerbread_house") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFF333333,
                "sigColorBackgroundMain" to 0xFFCDB391,
                "sigColorBackgroundSurface" to 0xFFCDB391,
                "actionSheetBackgroundDrawable" to 0xFFCDB391,
                "sigColorChatChat" to 0xFF333333,
                "sigColorChatPendingSending" to 0xFF333333,
                "sigColorChatSnapWithSound" to 0xFF333333,
                "sigColorChatSnapWithoutSound" to 0xFF333333,
                "sigExceptionColorCameraGridLines" to 0xFFCDB391,
            )
        }
        if (customThemeName == "peppermint_candy") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFF29ABCA,
                "sigColorBackgroundMain" to 0xFFFFDDCF,
                "sigColorBackgroundSurface" to 0xFFFFDDCF,
                "actionSheetBackgroundDrawable" to 0xFFFFDDCF,
                "sigColorChatChat" to 0xFF29ABCA,
                "sigColorChatPendingSending" to 0xFF29ABCA,
                "sigColorChatSnapWithSound" to 0xFF29ABCA,
                "sigColorChatSnapWithoutSound" to 0xFF29ABCA,
                "sigExceptionColorCameraGridLines" to 0xFFFFDDCF,
            )
        }
        if (customThemeName == "art_deco_glam") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFF000000,
                "sigColorBackgroundMain" to 0xFFF8F8F8,
                "sigColorBackgroundSurface" to 0xFFF8F8F8,
                "actionSheetBackgroundDrawable" to 0xFFF8F8F8,
                "sigColorChatChat" to 0xFF000000,
                "sigColorChatPendingSending" to 0xFF000000,
                "sigColorChatSnapWithSound" to 0xFF000000,
                "sigColorChatSnapWithoutSound" to 0xFF000000,
                "sigExceptionColorCameraGridLines" to 0xFFF8F8F8,
            )
        }
        if (customThemeName == "ocean_depths") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFFFFFFFF,
                "sigColorBackgroundMain" to 0xFF000080,
                "sigColorBackgroundSurface" to 0xFF000080,
                "actionSheetBackgroundDrawable" to 0xFF000080,
                "sigColorChatChat" to 0xFFFFFFFF,
                "sigColorChatPendingSending" to 0xFFFFFFFF,
                "sigColorChatSnapWithSound" to 0xFFFFFFFF,
                "sigColorChatSnapWithoutSound" to 0xFFFFFFFF,
                "sigExceptionColorCameraGridLines" to 0xFF000080,
            )
        }
        if (customThemeName == "bubblegum_pink") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFFFFFFFF,
                "sigColorBackgroundMain" to 0xFFFFC0CB,
                "sigColorBackgroundSurface" to 0xFFFFC0CB,
                "actionSheetBackgroundDrawable" to 0xFFFFC0CB,
                "sigColorChatChat" to 0xFFFFFFFF,
                "sigColorChatPendingSending" to 0xFFFFFFFF,
                "sigColorChatSnapWithSound" to 0xFFFFFFFF,
                "sigColorChatSnapWithoutSound" to 0xFFFFFFFF,
                "sigExceptionColorCameraGridLines" to 0xFFFFC0CB,
            )
        }
        if (customThemeName == "firefly_night") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFFFFFFF0,
                "sigColorBackgroundMain" to 0xFF222222,
                "sigColorBackgroundSurface" to 0xFF222222,
                "actionSheetBackgroundDrawable" to 0xFF222222,
                "sigColorChatChat" to 0xFFFFFFF0,
                "sigColorChatPendingSending" to 0xFFFFFFF0,
                "sigColorChatSnapWithSound" to 0xFFFFFFF0,
                "sigColorChatSnapWithoutSound" to 0xFFFFFFF0,
                "sigExceptionColorCameraGridLines" to 0xFF222222,
            )
        }
        if (customThemeName == "apple_orchard") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFF333333,
                "sigColorBackgroundMain" to 0xFFF4D35E,
                "sigColorBackgroundSurface" to 0xFFF4D35E,
                "actionSheetBackgroundDrawable" to 0xFFF4D35E,
                "sigColorChatChat" to 0xFF333333,
                "sigColorChatPendingSending" to 0xFF333333,
                "sigColorChatSnapWithSound" to 0xFF333333,
                "sigColorChatSnapWithoutSound" to 0xFF333333,
                "sigExceptionColorCameraGridLines" to 0xFFF4D35E,
            )
        }
        if (customThemeName == "lavender_field") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFF293145,
                "sigColorBackgroundMain" to 0xFFBDBDBD,
                "sigColorBackgroundSurface" to 0xFFBDBDBD,
                "actionSheetBackgroundDrawable" to 0xFFBDBDBD,
                "sigColorChatChat" to 0xFF293145,
                "sigColorChatPendingSending" to 0xFF293145,
                "sigColorChatSnapWithSound" to 0xFF293145,
                "sigColorChatSnapWithoutSound" to 0xFF293145,
                "sigExceptionColorCameraGridLines" to 0xFFBDBDBD,
            )
        }
        if (customThemeName == "lemon_drop") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFF000000,
                "sigColorBackgroundMain" to 0xFFFCE5C7,
                "sigColorBackgroundSurface" to 0xFFFCE5C7,
                "actionSheetBackgroundDrawable" to 0xFFFCE5C7,
                "sigColorChatChat" to 0xFF000000,
                "sigColorChatPendingSending" to 0xFF000000,
                "sigColorChatSnapWithSound" to 0xFF000000,
                "sigColorChatSnapWithoutSound" to 0xFF000000,
                "sigExceptionColorCameraGridLines" to 0xFFFCE5C7,
            )
        }
        if (customThemeName == "modern_farmhouse") {
            currentTheme = parseAttributeList(
                "sigColorTextPrimary" to 0xFF333333,
                "sigColorBackgroundMain" to 0xFFF2F2F2,
                "sigColorBackgroundSurface" to 0xFFF2F2F2,
                "actionSheetBackgroundDrawable" to 0xFFF2F2F2,
                "sigColorChatChat" to 0xFF333333,
                "sigColorChatPendingSending" to 0xFF333333,
                "sigColorChatSnapWithSound" to 0xFF333333,
                "sigColorChatSnapWithoutSound" to 0xFF333333,
                "sigExceptionColorCameraGridLines" to 0xFFF2F2F2,
            )
        }

        if (customThemeName == "custom") {
            val availableThemes = context.fileHandlerManager.getFileHandle(FileHandleScope.THEME.key, "")?.open(ParcelFileDescriptor.MODE_READ_ONLY)?.use { pfd ->
                AutoCloseInputStream(pfd).use { it.readBytes() }
            }?.let {
                context.gson.fromJson(it.toString(Charsets.UTF_8), object: TypeToken<List<DatabaseThemeContent>>() {})
            } ?: run {
                context.log.verbose("No custom themes found")
                return
            }

            val customThemeColors = mutableMapOf<Int, Int>()

            context.log.verbose("Loading ${availableThemes.size} custom themes")

            availableThemes.forEach { themeContent ->
                themeContent.colors.forEach colors@{ colorEntry ->
                    customThemeColors[getAttribute(colorEntry.key).takeIf { it != 0 }.also {
                        if (it == null) {
                            context.log.warn("unknown color attribute: ${colorEntry.key}")
                        }
                    } ?: return@colors] = colorEntry.value
                }
            }

            currentTheme = customThemeColors

            context.log.verbose("loaded ${customThemeColors.size} custom theme colors")
        }

        onNextActivityCreate {
            if (currentTheme.isEmpty()) return@onNextActivityCreate

            context.androidContext.theme.javaClass.getMethod("obtainStyledAttributes", IntArray::class.java).hook(
                HookStage.AFTER) { param ->
                val array = param.arg<IntArray>(0)
                val customColor = (currentTheme[array[0]] as? Number)?.toInt() ?: return@hook

                val result = param.getResult() as TypedArray
                val typedArrayData = result.getObjectField("mData") as IntArray

                if (context.bridgeClient.getDebugProp("log_resources") == "true") {
                    context.log.verbose(context.resources.getResourceName(array[0]))
                }

                when (val attributeType = result.getType(0)) {
                    TypedValue.TYPE_INT_COLOR_ARGB8, TypedValue.TYPE_INT_COLOR_RGB8, TypedValue.TYPE_INT_COLOR_ARGB4, TypedValue.TYPE_INT_COLOR_RGB4 -> {
                        typedArrayData[1] = customColor // index + STYLE_DATA
                    }
                    TypedValue.TYPE_STRING -> {
                        val stringValue = result.getString(0)
                        if (stringValue?.endsWith(".xml") == true) {
                            typedArrayData[0] = TypedValue.TYPE_INT_COLOR_ARGB4 // STYLE_TYPE
                            typedArrayData[1] = customColor // STYLE_DATA
                            typedArrayData[5] = 0; // STYLE_DENSITY
                        }
                    }
                    else -> context.log.warn("unknown attribute type: ${attributeType.toString(16)}")
                }
            }
        }
    }
}