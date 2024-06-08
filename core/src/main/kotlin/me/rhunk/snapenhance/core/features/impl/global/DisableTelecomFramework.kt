package me.rhunk.snapenhance.core.features.impl.global

import android.content.ContextWrapper
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.features.FeatureLoadParams
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook

class DisableTelecomFramework: Feature("Disable Telecom Framework", loadParams = FeatureLoadParams.INIT_SYNC) {
    override fun init() {
        if (!context.config.global.disableTelecomFramework.get()) return

        ContextWrapper::class.java.hook("getSystemService", HookStage.BEFORE) { param ->
            if (param.arg<Any>(0).toString() == "telecom") param.setResult(null)
        }
    }
}