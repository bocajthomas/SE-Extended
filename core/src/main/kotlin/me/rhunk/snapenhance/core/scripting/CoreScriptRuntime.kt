package me.rhunk.snapenhance.core.scripting

import me.rhunk.snapenhance.bridge.scripting.AutoReloadListener
import me.rhunk.snapenhance.common.logger.AbstractLogger
import me.rhunk.snapenhance.common.scripting.ScriptRuntime
import me.rhunk.snapenhance.common.scripting.bindings.BindingSide
import me.rhunk.snapenhance.core.ModContext
import me.rhunk.snapenhance.core.scripting.impl.*

class CoreScriptRuntime(
    private val modContext: ModContext,
    logger: AbstractLogger,
): ScriptRuntime(
    config = { modContext.config },
    androidContext = modContext.androidContext,
    logger = logger
) {
    // we assume that the bridge is reloaded the next time we connect to it
    private var isBridgeReloaded = false

    fun init() {
        buildModuleObject = { module ->
            putConst("currentSide", this, BindingSide.CORE.key)
            module.registerBindings(
                CoreScriptConfig(),
                CoreIPC(),
                CoreScriptHooker(),
                CoreMessaging(modContext),
                CoreEvents(modContext),
            )
        }

        modContext.bridgeClient.addOnConnectedCallback(initNow = true) {
            scripting = modContext.bridgeClient.getScriptingInterface()

            if (!isBridgeReloaded) {
                scripting.enabledScripts.forEach { path ->
                    runCatching {
                        load(path, scripting.getScriptContent(path))
                    }.onFailure {
                        logger.error("Failed to load script $path", it)
                    }
                }
            }

            scripting.registerAutoReloadListener(object : AutoReloadListener.Stub() {
                override fun restartApp() {
                    modContext.softRestartApp()
                }
            })

            eachModule {
                onBridgeConnected(reloaded = isBridgeReloaded)
            }

            if (!isBridgeReloaded) {
                isBridgeReloaded = true
            }
        }
    }
}