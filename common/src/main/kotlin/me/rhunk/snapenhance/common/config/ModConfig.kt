package me.rhunk.snapenhance.common.config

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import me.rhunk.snapenhance.bridge.ConfigStateListener
import me.rhunk.snapenhance.bridge.storage.FileHandleManager
import me.rhunk.snapenhance.common.bridge.InternalFileHandleType
import me.rhunk.snapenhance.common.bridge.InternalFileWrapper
import me.rhunk.snapenhance.common.bridge.wrapper.LocaleWrapper
import me.rhunk.snapenhance.common.config.impl.RootConfig
import me.rhunk.snapenhance.common.logger.AbstractLogger
import me.rhunk.snapenhance.common.util.LazyBridgeValue
import kotlin.properties.Delegates

class ModConfig(
    private val context: Context,
    fileHandleManager: LazyBridgeValue<FileHandleManager>
) {
    private val fileWrapper = InternalFileWrapper(fileHandleManager, InternalFileHandleType.CONFIG, "{}")
    var locale: String = LocaleWrapper.DEFAULT_LOCALE

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    var wasPresent by Delegates.notNull<Boolean>()

    /* Used to notify the bridge client about config changes */
    var configStateListener: ConfigStateListener? = null
    lateinit var root: RootConfig
        private set

    fun isInitialized() = ::root.isInitialized

    private fun createRootConfig() = RootConfig().apply { lateInit(context) }

    fun load() {
        root = createRootConfig()
        wasPresent = fileWrapper.exists()
        if (!wasPresent) {
            writeConfig()
            return
        }

        runCatching {
            loadConfig()
        }.onFailure {
            writeConfig()
        }
    }

    private fun loadConfig() {
        val configFileContent = fileWrapper.readBytes()
        val configObject = gson.fromJson(configFileContent.toString(Charsets.UTF_8), JsonObject::class.java)
        locale = configObject.get("_locale")?.asString ?: LocaleWrapper.DEFAULT_LOCALE
        root.fromJson(configObject)
    }

    fun exportToString(
        exportSensitiveData: Boolean = true
    ): String {
        val configObject = root.toJson(exportSensitiveData)
        configObject.addProperty("_locale", locale)
        return gson.toJson(configObject)
    }

    fun reset() {
        root = RootConfig()
        writeConfig()
    }

    fun writeConfig() {
        var shouldRestart = false
        var shouldCleanCache = false
        var configChanged = false

        fun compareDiff(originalContainer: ConfigContainer, modifiedContainer: ConfigContainer) {
            val parentContainerFlags = modifiedContainer.parentContainerKey?.params?.flags ?: emptySet()

            parentContainerFlags.takeIf { originalContainer.hasGlobalState }?.apply {
                if (modifiedContainer.globalState != originalContainer.globalState) {
                    configChanged = true
                    if (contains(ConfigFlag.REQUIRE_RESTART)) shouldRestart = true
                    if (contains(ConfigFlag.REQUIRE_CLEAN_CACHE)) shouldCleanCache = true
                }
            }

            for (property in modifiedContainer.properties) {
                val modifiedValue = property.value.getNullable()
                val originalValue = originalContainer.properties.entries.firstOrNull {
                    it.key.name == property.key.name
                }?.value?.getNullable()

                if (originalValue is ConfigContainer && modifiedValue is ConfigContainer) {
                    compareDiff(originalValue, modifiedValue)
                    continue
                }

                if (modifiedValue != originalValue) {
                    val flags = property.key.params.flags + parentContainerFlags
                    configChanged = true
                    if (flags.contains(ConfigFlag.REQUIRE_RESTART)) shouldRestart = true
                    if (flags.contains(ConfigFlag.REQUIRE_CLEAN_CACHE)) shouldCleanCache = true
                }
            }
        }

        val oldConfig = runCatching { fileWrapper.readBytes().toString(Charsets.UTF_8) }.getOrNull()
        fileWrapper.writeBytes(exportToString().toByteArray(Charsets.UTF_8))

        configStateListener?.also {
            runCatching {
                compareDiff(createRootConfig().apply {
                    fromJson(gson.fromJson(oldConfig ?: return@runCatching, JsonObject::class.java))
                }, root)

                if (configChanged) {
                    it.onConfigChanged()
                    if (shouldCleanCache) it.onCleanCacheRequired()
                    else if (shouldRestart) it.onRestartRequired()
                }
            }.onFailure {
                AbstractLogger.directError("Error while calling config state listener", it, "ConfigStateListener")
            }
        }
    }

    fun loadFromString(string: String) {
        val configObject = gson.fromJson(string, JsonObject::class.java)
        locale = configObject.get("_locale")?.asString ?: LocaleWrapper.DEFAULT_LOCALE
        root.fromJson(configObject)
        writeConfig()
    }
}
