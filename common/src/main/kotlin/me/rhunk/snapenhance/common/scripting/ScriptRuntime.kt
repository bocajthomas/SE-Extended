package me.rhunk.snapenhance.common.scripting

import android.content.Context
import android.os.ParcelFileDescriptor
import me.rhunk.snapenhance.bridge.scripting.IScripting
import me.rhunk.snapenhance.common.BuildConfig
import me.rhunk.snapenhance.common.config.impl.RootConfig
import me.rhunk.snapenhance.common.logger.AbstractLogger
import me.rhunk.snapenhance.common.scripting.type.readModuleInfo
import org.mozilla.javascript.ScriptableObject
import java.io.InputStream

open class ScriptRuntime(
    val config: () -> RootConfig,
    val androidContext: Context,
    logger: AbstractLogger,
) {
    val logger = ScriptingLogger(logger)

    lateinit var scripting: IScripting
    var buildModuleObject: ScriptableObject.(JSModule) -> Unit = {}

    private val modules = mutableMapOf<String, JSModule>()

    fun eachModule(f: JSModule.() -> Unit) {
        modules.values.forEach { module ->
            runCatching {
                module.f()
            }.onFailure {
                logger.error("Failed to run module function in ${module.moduleInfo.name}", it)
            }
        }
    }

    fun getModuleByName(name: String): JSModule? {
        return modules.values.find { it.moduleInfo.name == name }
    }

    fun removeModule(scriptPath: String) {
        modules.remove(scriptPath)
    }

    fun unload(scriptPath: String) {
        val module = modules[scriptPath] ?: return
        logger.info("Unloading module $scriptPath")
        module.unload()
        modules.remove(scriptPath)
    }

    fun load(scriptPath: String, pfd: ParcelFileDescriptor): JSModule {
        return ParcelFileDescriptor.AutoCloseInputStream(pfd).use {
            load(scriptPath, it)
        }
    }

    fun load(scriptPath: String, content: InputStream): JSModule {
        logger.info("Loading module $scriptPath")
        val bufferedReader = content.bufferedReader()
        val moduleInfo = bufferedReader.readModuleInfo()

        if (moduleInfo.minSEVersion != null && moduleInfo.minSEVersion > BuildConfig.VERSION_CODE) {
            throw Exception("Module requires a newer version of SnapEnhance (min version: ${moduleInfo.minSEVersion})")
        }

        return JSModule(
            scriptRuntime = this,
            moduleInfo = moduleInfo,
            reader = bufferedReader,
        ).apply {
            load {
                buildModuleObject(this, this@apply)
            }
            modules[scriptPath] = this
        }
    }
}