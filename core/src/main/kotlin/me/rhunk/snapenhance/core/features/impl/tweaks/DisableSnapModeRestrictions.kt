package me.rhunk.snapenhance.core.features.impl.tweaks

import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.util.dataBuilder
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hookConstructor

class DisableSnapModeRestrictions: Feature("Disable Snap Mode Restrictions") {
    override fun init() {
        if (!context.config.messaging.disableSnapModeRestrictions.get()) return

        findClass("com.snapchat.client.messaging.SnapModeInfo").hookConstructor(HookStage.AFTER) { param ->
            param.thisObject<Any>().dataBuilder {
                set("mSelfDestructSnapDurationMs", null)
            }
        }
    }
}