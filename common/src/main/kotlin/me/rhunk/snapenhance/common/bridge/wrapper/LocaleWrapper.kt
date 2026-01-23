package me.rhunk.snapenhance.common.bridge.wrapper

import android.content.Context
import android.os.ParcelFileDescriptor
import android.os.ParcelFileDescriptor.AutoCloseInputStream
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import me.rhunk.snapenhance.bridge.storage.FileHandleManager
import me.rhunk.snapenhance.common.bridge.FileHandleScope
import me.rhunk.snapenhance.common.logger.AbstractLogger
import me.rhunk.snapenhance.common.util.LazyBridgeValue
import java.util.Locale

class LocaleWrapper(
    private val fileHandleManager: LazyBridgeValue<FileHandleManager>
) {
    companion object {
        const val DEFAULT_LOCALE = "en_US"

        fun fetchAvailableLocales(context: Context): List<String> {
            return context.resources.assets.list("lang")?.map { it.substringBefore(".") }?.sorted() ?: listOf(DEFAULT_LOCALE)
        }
    }

    var userLocale = DEFAULT_LOCALE

    private val translationMap = linkedMapOf<String, String>()

    lateinit var loadedLocale: Locale

    private fun load(locale: String, pfd: ParcelFileDescriptor) {
        loadedLocale = if (locale.contains("_")) {
            val split = locale.split("_")
            Locale(split[0], split[1])
        } else {
            Locale(locale)
        }

        val translations = AutoCloseInputStream(pfd).use {
            runCatching {
                JsonParser.parseReader(it.reader()).asJsonObject
            }.onFailure {
                AbstractLogger.directError("Failed to parse locale file: ${it.message}", it)
            }.getOrNull()
        }
        if (translations == null || translations.isJsonNull) {
            throw IllegalStateException("Failed to parse $locale.json")
        }

        fun scanObject(jsonObject: JsonObject, prefix: String = "") {
            jsonObject.entrySet().forEach {
                if (it.value.isJsonPrimitive) {
                    val key = "$prefix${it.key}"
                    translationMap[key] = it.value.asString
                }
                if (!it.value.isJsonObject) return@forEach
                scanObject(it.value.asJsonObject, "$prefix${it.key}.")
            }
        }

        scanObject(translations)
    }

    fun load() {
        fileHandleManager.value.getFileHandle(FileHandleScope.LOCALE.key, "$DEFAULT_LOCALE.json")?.open(ParcelFileDescriptor.MODE_READ_ONLY)?.use {
            load(DEFAULT_LOCALE, it)
        } ?: run {
            throw IllegalStateException("Failed to load default locale")
        }

        if (userLocale != DEFAULT_LOCALE) {
            fileHandleManager.value.getFileHandle(FileHandleScope.LOCALE.key, "$userLocale.json")?.open(ParcelFileDescriptor.MODE_READ_ONLY)?.use {
                load(userLocale, it)
            }
        }
    }

    fun reload(locale: String) {
        userLocale = locale
        translationMap.clear()
        load()
    }

    operator fun get(key: String) = translationMap[key] ?: key.also { AbstractLogger.directDebug("Missing translation for $key") }
    fun getOrNull(key: String) = translationMap[key]

    fun format(key: String, vararg args: Pair<String, String>): String {
        return args.fold(get(key)) { acc, pair ->
            acc.replace("{${pair.first}}", pair.second)
        }
    }

    fun getCategory(key: String): LocaleWrapper {
        return LocaleWrapper(fileHandleManager).apply {
            translationMap.putAll(
                this@LocaleWrapper.translationMap
                    .filterKeys { it.startsWith("$key.") }
                    .mapKeys { it.key.substring(key.length + 1) }
            )
        }
    }
}