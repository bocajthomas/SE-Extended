package me.rhunk.snapenhance.core.features.impl.ui

import me.rhunk.snapenhance.common.config.impl.UserInterfaceTweaks
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.features.FeatureLoadParams
import java.io.File


class ClientBootstrapOverride: Feature("ClientBootstrapOverride", loadParams = FeatureLoadParams.INIT_SYNC) {

    private val clientBootstrapFolder by lazy { File(context.androidContext.filesDir, "client-bootstrap") }

    private val appearanceStartupConfigFile by lazy { File(clientBootstrapFolder, "appearancestartupconfig") }
    private val plusFile by lazy { File(clientBootstrapFolder, "plus") }

    override fun init() {
        val bootstrapOverrideConfig = context.config.userInterface.bootstrapOverride

        if (!clientBootstrapFolder.exists() && (bootstrapOverrideConfig.appAppearance.getNullable() != null || bootstrapOverrideConfig.homeTab.getNullable() != null)) {
            clientBootstrapFolder.mkdirs()
        }

        bootstrapOverrideConfig.appAppearance.getNullable()?.also { appearance ->
            val state = when (appearance) {
                "always_light" -> 0
                "always_dark" -> 1
                else -> return@also
            }.toByte()
            appearanceStartupConfigFile.writeBytes(byteArrayOf(0, 0, 0, state))
        }

        bootstrapOverrideConfig.homeTab.getNullable()?.also { currentTab ->
            plusFile.writeBytes(byteArrayOf(8, (UserInterfaceTweaks.BootstrapOverride.tabs.indexOf(currentTab) + 1).toByte()))
        }
    }
}