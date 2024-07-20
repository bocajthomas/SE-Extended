package me.rhunk.snapenhance.core.features.impl.global

import android.app.AlertDialog
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import java.lang.reflect.Modifier

class GooglePlayServicesDialogs : Feature("Disable GMS Dialogs") {
    override fun init() {
        if (!context.config.global.disableGooglePlayDialogs.get()) return

        onNextActivityCreate(defer = true) {
            findClass("com.google.android.gms.common.GoogleApiAvailability").methods
                .first { Modifier.isStatic(it.modifiers) && it.returnType == AlertDialog::class.java }.let { method ->
                    method.hook(HookStage.BEFORE) { param ->
                        context.log.verbose("GoogleApiAvailability.showErrorDialogFragment() called, returning null")
                        param.setResult(null)
                    }
                }
        }
    }
}