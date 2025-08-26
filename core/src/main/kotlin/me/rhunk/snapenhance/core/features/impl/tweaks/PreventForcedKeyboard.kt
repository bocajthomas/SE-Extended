package me.rhunk.snapenhance.core.features.impl.tweaks

import android.view.View
import android.view.inputmethod.InputMethodManager
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook

class PreventForcedKeyboard : Feature("Prevent Forced Keyboard") {
    override fun init() {
        if (!context.config.userInterface.preventForcedKeyboard.get()) return

        InputMethodManager::class.java.hook("showSoftInput", HookStage.BEFORE) { param ->
            if (param.argNullable<View>(0)?.javaClass?.name?.endsWith("InputBarEditText") == true && param.args().getOrNull(1) is Int) {
                param.setResult(false)
            }
        }
    }
}