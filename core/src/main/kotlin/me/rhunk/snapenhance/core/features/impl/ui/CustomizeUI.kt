package me.rhunk.snapenhance.core.features.impl.ui

import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.features.FeatureLoadParams
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.Hooker
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.ktx.getIdentifier

class CustomizeUI: Feature("Customize UI", loadParams = FeatureLoadParams.ACTIVITY_CREATE_SYNC) {
    private fun parseColor(color: String): Int? {
        return color.takeIf { it.isNotEmpty() }?.let {
            runCatching { Color.parseColor(color) }.getOrNull()
        }
    }

    override fun onActivityCreate() {
        
        //TODO: color picker
        val customizeUIConfig = context.config.userInterface.customizeUi
        val colorsConfig = context.config.userInterface.customizeUi.colors
        val themePicker = customizeUIConfig.themePicker.getNullable() ?: return
        val effectiveTextColor by lazy { parseColor(colorsConfig.textColor.get()) }
        val effectiveChatChatTextColor by lazy { parseColor(colorsConfig.chatChatTextColor.get()) }
        val effectivePendingSendingTextColor by lazy { parseColor(colorsConfig.pendingSendingTextColor.get()) }
        val effectiveSnapWithSoundTextColor by lazy { parseColor(colorsConfig.snapWithSoundTextColor.get()) }
        val effectiveSnapWithoutSoundTextColor by lazy { parseColor(colorsConfig.snapWithoutSoundTextColor.get()) }
        val effectiveBackgroundColor by lazy { parseColor(colorsConfig.backgroundColor.get()) }
        val effectiveBackgroundColorSurface by lazy { parseColor(colorsConfig.backgroundColorSurface.get()) }
        val effectiveActionMenuBackgroundColor by lazy { parseColor(colorsConfig.actionMenuBackgroundColor.get()) }
        val effectiveActionMenuRoundBackgroundColor by lazy { parseColor(colorsConfig.actionMenuRoundBackgroundColor.get()) }
        val effectiveCameraGridLines by lazy { parseColor(colorsConfig.cameraGridLines.get()) }
        
        val attributeCache = mutableMapOf<String, Int>()

        fun getAttribute(name: String): Int {
            if (attributeCache.containsKey(name)) return attributeCache[name]!!
            return context.resources.getIdentifier(name, "attr").also { attributeCache[name] = it }
        }

        context.androidContext.theme.javaClass.getMethod("obtainStyledAttributes", IntArray::class.java).hook(
            HookStage.AFTER) { param ->
            val array = param.arg<IntArray>(0)
            val result = param.getResult() as TypedArray

            fun ephemeralHook(methodName: String, content: Any) {
                Hooker.ephemeralHookObjectMethod(result::class.java, result, methodName, HookStage.BEFORE) {
                    it.setResult(content)
                }
            }
            if (themePicker == "amoled_dark_mode") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFF000000.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFF000000.toInt()))
                    }
                }
            }
                
            if (themePicker == "modern_minimalism") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF333333.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFF5F5F5.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFF5F5F5.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF333333.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFF5F5F5.toInt())
                    }
                }
            }
            
            if (themePicker == "serene_nature") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF2E8B57.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFFFFDE7.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFFFFDE7.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF2E8B57.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFFFFDE7.toInt())
                    }
                }
            }

            if (themePicker == "energetic_pop") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                         ephemeralHook("getColor", 0xFFFFDD00.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFF212121.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFF212121.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFFFDD00.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFF212121.toInt())
                    }
                }
            }

            if (themePicker == "luxurious_night") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFFDFCCC.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFF303030.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFF303030.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFFDFCCC.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFF303030.toInt())
                    }
                }
            }

            if (themePicker == "playful_candy") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFF473B9.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFC2F0F0.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFC2F0F0.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFF473B9.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFC2F0F0.toInt())
                    }
                }
            }

            if (themePicker == "retro_arcade") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFFFF9C4.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFF00008B.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFF00008B.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFFFF9C4.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFF00008B.toInt())
                    }
                }
            }

            if (themePicker == "rustic_country") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF77332E.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFEAEAEA.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFEAEAEA.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF77332E.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFEAEAEA.toInt())
                    }
                }
            }

            if (themePicker == "ocean_breeze") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF38B3DE.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFE5E5E5.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFE5E5E5.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF38B3DE.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFE5E5E5.toInt())
                    }
                }
            }

            if (themePicker == "sunset_glow") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF293145.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFF0E68C.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFF0E68C.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF293145.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFF0E68C.toInt())
                    }
                }
            }

            if (themePicker == "space_adventure") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFA5BFF7.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFF222222.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFF222222.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFA5BFF7.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFF222222.toInt())
                    }
                }
            }
            // Credit To @Gabe_does_tech (Telegram) For This Theme 
            if (themePicker == "light_blue") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF03BAFC.toInt())
                    }
                    getAttribute("sigColorBackgroundMain") -> {
                        ephemeralHook("getColor", 0xFFBDE6FF.toInt())
                    }
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFF78DBFF.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFF78DBFF.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF08D6FF.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFF08D6FF.toInt())
                    }
                }

            }
            // Credit To @jwhc1 (Telegram) For This Theme 
            if (themePicker == "dark_blue") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF98C2FD.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFF192744.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFF192744.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF98C2FD.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFF192744.toInt())
                    }
                }
            }

            if (themePicker == "earthy_autumn") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFF7CAC9.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFF800000.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFF800000.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFF7CAC9.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFF800000.toInt())
                    }
                }
            }

            if (themePicker == "watercolor_wash") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF3F51B5.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFFFF5F3.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFFFF5F3.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF3F51B5.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFFFF5F3.toInt())
                    }
                }
            }

            if (themePicker == "lemon_zest") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF222222.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFFFFFE0.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFFFFFE0.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF222222.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFFFFFE0.toInt())
                    }
                }
            }

            if (themePicker == "tropical_paradise") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF000000.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFD3FFCE.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFD3FFCE.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF000000.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFD3FFCE.toInt())
                    }
                }
            }
            
            if (themePicker == "industrial_chic") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF424242.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFEEEEEE.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFEEEEEE.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF424242.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFEEEEEE.toInt())
                    }
                }
            }

            if (themePicker == "cherry_bomb") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFC24641.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFC24641.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFC24641.toInt())
                    }
                }
            }

            if (themePicker == "woodland_mystery") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFC2C2F0.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFF333333.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFF333333.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFC2C2F0.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFF333333.toInt())
                    }
                }
            }

            if (themePicker == "galaxy_glitter") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFF2F4F4F.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFF2F4F4F.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFF2F4F4F.toInt())
                    }
                }
            }

            if (themePicker == "creamy_vanilla") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF333333.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFF1F1F1.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFF1F1F1.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF333333.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFF1F1F1.toInt())
                    }
                }
            }

            if (themePicker == "spicy_chili") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFF5F5F5.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFC70039.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFC70039.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFF5F5F5.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFC70039.toInt())
                    }
                }
            }

            if (themePicker == "spring_meadow") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF388E3C.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFF5FBE0.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFF5FBE0.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF388E3C.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFF5FBE0.toInt())
                    }
                }
            }

            if (themePicker == "midnight_library") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFEAEAEA.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFF424242.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFF424242.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFEAEAEA.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFF424242.toInt())
                    }
                }
            }

            if (themePicker == "lemon_sorbet") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF000000.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFFCFFE7.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFFCFFE7.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF000000.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFFCFFE7.toInt())
                    }
                }
            }

            if (themePicker == "cosmic_night") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFF2F4F4F.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFF2F4F4F.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFF2F4F4F.toInt())
                    }
                }
            }

            if (themePicker == "spicy_mustard") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFFCC01E.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFFCC01E.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFFCC01E.toInt())
                    }
                }
            }

            if (themePicker == "peppermint_candy") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF29ABCA.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFFFDDCF.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFFFDDCF.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF29ABCA.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFFFDDCF.toInt())
                    }
                }
            }

            if (themePicker == "gingerbread_house") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF333333.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFCDB391.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFCDB391.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF333333.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFCDB391.toInt())
                    }
                }
            }

            if (themePicker == "art_deco_glam") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF000000.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFF8F8F8.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFF8F8F8.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF000000.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFF8F8F8.toInt())
                    }
                }
            }

            if (themePicker == "ocean_depths") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFF000080.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFF000080.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFF000080.toInt())
                    }
                }
            }

            if (themePicker == "bubblegum_pink") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFFFC0CB.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFFFC0CB.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFFFC0CB.toInt())
                    }
                }
            }

            if (themePicker == "firefly_night") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFFFFFF0.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFF222222.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFF222222.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFFFFFF0.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFF222222.toInt())
                    }
                }
            }

            if (themePicker == "apple_orchard") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF333333.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFF4D35E.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFF4D35E.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF333333.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFF4D35E.toInt())
                    }
                }
            }

            if (themePicker == "lavender_field") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF293145.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFBDBDBD.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFBDBDBD.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF293145.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFBDBDBD.toInt())
                    }
                }
            }

            if (themePicker == "lemon_drop") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF000000.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFFCE5C7.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFFCE5C7.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF000000.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFFCE5C7.toInt())
                    }
                }
            }

            if (themePicker == "modern_farmhouse") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF333333.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFF2F2F2.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFF2F2F2.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF333333.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFF2F2F2.toInt())
                    }
                }
            }

            if (themePicker == "black_cat") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFF000000.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFF000000.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFF000000.toInt())
                    }
                }
            }

            if (themePicker == "mint_chocolate") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFF98FF98.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFF98FF98.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFF98FF98.toInt())
                    }
                }
            }

            if (themePicker == "desert_sunset") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF293145.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFF7CA48.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFF7CA48.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF293145.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFF7CA48.toInt())
                    }
                }
            }

            if (themePicker == "pumpkin_spice") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFC7893A.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFC7893A.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFC7893A.toInt())
                    }
                }
            }

            if (themePicker == "sky_blue") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF333333.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFADD8E6.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFADD8E6.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF333333.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFADD8E6.toInt())
                    }
                }
            }

            if (themePicker == "tropical_jungle") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF2E8B57.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFA0C48E.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFA0C48E.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF2E8B57.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFA0C48E.toInt())
                    }
                }
            }

            if (themePicker == "black_marble") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFF222222.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFF222222.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFF222222.toInt())
                    }
                }
            }

            if (themePicker == "coffee_shop") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFC2B280.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFC2B280.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFC2B280.toInt())
                    }
                }
            }

            if (themePicker == "mermaid_lagoon") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF293145.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFC6E2FF.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFC6E2FF.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF293145.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFC6E2FF.toInt())
                    }
                }
            }

            if (themePicker == "creamsicle") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF293145.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFFCE5C7.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFFCE5C7.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF293145.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFFCE5C7.toInt())
                    }
                }
            }

            if (themePicker == "vintage_comic") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF000000.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFF5F5F5.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFF5F5F5.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF000000.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFF5F5F5.toInt())
                    }
                }
            }

            if (themePicker == "neon_arcade") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFF80000.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFF80000.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFF80000.toInt())
                    }
                }
            }

            if (themePicker == "space_cadet") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFF2F4F4F.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFF2F4F4F.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFF2F4F4F.toInt())
                    }
                }
            }

            if (themePicker == "cherry_blossom") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF293145.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFF0E68C.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFF0E68C.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF293145.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFF0E68C.toInt())
                    }
                }
            }

            if (themePicker == "cyber_glitch") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF00FF00.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFF000000.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFF000000.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF00FF00.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFF000000.toInt())
                    }
                }
            }

            if (themePicker == "ginger_snap") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFC6893A.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFC6893A.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFC6893A.toInt())
                    }
                }
            }

            if (themePicker == "vintage_travel") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF333333.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFECEAF3.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFECEAF3.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF333333.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFECEAF3.toInt())
                    }
                }
            }

            if (themePicker == "lava_flow") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFFFCC00.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFC70039.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFC70039.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFFFCC00.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFC70039.toInt())
                    }
                }
            }

            if (themePicker == "cotton_candy_clouds") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF333333.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFF0F8FF.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFF0F8FF.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF333333.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFF0F8FF.toInt())
                    }
                }
            }

            if (themePicker == "rusty_robot") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFF7CA48.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFF303030.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFF303030.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFF7CA48.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFF303030.toInt())
                    }
                }
            }

            if (themePicker == "ocean_fog") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF333333.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFB0C4DE.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFB0C4DE.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF333333.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFB0C4DE.toInt())
                    }
                }
            }

            if (themePicker == "stained_glass") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF000000.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFF5F5F5.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFF5F5F5.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF000000.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFF5F5F5.toInt())
                    }
                }
            }

            if (themePicker == "spicy_chili_pepper") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFC70039.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFC70039.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFC70039.toInt())
                    }
                }
            }

            if (themePicker == "minimalist_night") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFA5BFF7.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFF000000.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFF000000.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFA5BFF7.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFF000000.toInt())
                    }
                }
            }

            if (themePicker == "pirate_treasure") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFFCE5C7.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFF333333.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFF333333.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFFCE5C7.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFF333333.toInt())
                    }
                }
            }

            if (themePicker == "lemon_meringue") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFF000000.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFFFCFFE7.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFFFCFFE7.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFF000000.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFFFCFFE7.toInt())
                    }
                }
            }

            if (themePicker == "alien_landscape") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigColorBackgroundMain"),
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", 0xFF9B59B6.toInt())
                    }
                    getAttribute("actionSheetBackgroundDrawable"),
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(0xFF9B59B6.toInt()))
                    }
                    getAttribute("sigColorChatChat"),
                    getAttribute("sigColorChatPendingSending"),
                    getAttribute("sigColorChatSnapWithSound"),
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", 0xFFFFFFFF.toInt())
                    }
                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", 0xFF9B59B6.toInt())
                    }
                }
            }
            
            if (themePicker == "custom") {
                when (array[0]) {
                    getAttribute("sigColorTextPrimary") -> {
                        ephemeralHook("getColor", effectiveTextColor ?: return@hook)
                    }
                        
                    getAttribute("sigColorBackgroundMain") -> {
                        ephemeralHook("getColor", effectiveBackgroundColor ?: return@hook)
                    }
                        
                    getAttribute("sigColorBackgroundSurface") -> {
                        ephemeralHook("getColor", effectiveBackgroundColorSurface ?: return@hook)
                    }
                        
                    getAttribute("actionSheetBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(effectiveActionMenuBackgroundColor ?: return@hook))
                    }
                        
                    getAttribute("actionSheetRoundedBackgroundDrawable") -> {
                        ephemeralHook("getDrawable", ColorDrawable(effectiveActionMenuRoundBackgroundColor ?: return@hook))
                    }
                        
                    getAttribute("sigColorChatChat") -> {
                        ephemeralHook("getColor", effectiveChatChatTextColor ?: return@hook)
                        
                    }
                        
                    getAttribute("sigColorChatPendingSending") -> {
                        ephemeralHook("getColor", effectivePendingSendingTextColor ?: return@hook)
                        
                    }
                        
                    getAttribute("sigColorChatSnapWithSound") -> {
                            ephemeralHook("getColor", effectiveSnapWithSoundTextColor ?: return@hook)
                        
                    }
                        
                    getAttribute("sigColorChatSnapWithoutSound") -> {
                        ephemeralHook("getColor", effectiveSnapWithoutSoundTextColor ?: return@hook)
                    }

                    getAttribute("sigExceptionColorCameraGridLines") -> {
                        ephemeralHook("getColor", effectiveCameraGridLines ?: return@hook)
                    }
                }
            }
        }
    }
}
