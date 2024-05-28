package me.rhunk.snapenhance.common.bridge.wrapper

import android.content.Context
import android.os.ParcelFileDescriptor.AutoCloseInputStream
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import me.rhunk.snapenhance.common.bridge.types.LocalePair
import me.rhunk.snapenhance.common.logger.AbstractLogger
import me.rhunk.snapenhance.common.util.ktx.toParcelFileDescriptor
import java.util.Locale


class LocaleWrapper {
    companion object {
        const val DEFAULT_LOCALE = "en_US"

        fun fetchLocales(context: Context, locale: String = DEFAULT_LOCALE): List<LocalePair> {
            val coroutineScope = CoroutineScope(Dispatchers.IO)
            val locales = mutableListOf<LocalePair>().apply {
                add(LocalePair(DEFAULT_LOCALE, context.resources.assets.open("lang/$DEFAULT_LOCALE.json").toParcelFileDescriptor(coroutineScope)))
            }

            if (locale == DEFAULT_LOCALE) return locales

            val compatibleLocale = context.resources.assets.list("lang")?.firstOrNull { it.startsWith(locale) }?.substringBefore(".") ?: return locales

            locales.add(
                LocalePair(
                    compatibleLocale,
                    context.resources.assets.open("lang/$compatibleLocale.json").toParcelFileDescriptor(coroutineScope)
                )
            )

            return locales
        }

        fun fetchAvailableLocales(context: Context): List<String> {
            return context.resources.assets.list("lang")?.map { it.substringBefore(".") }?.sorted() ?: listOf(DEFAULT_LOCALE)
        }
    }

    var userLocale = DEFAULT_LOCALE

    private val translationMap = linkedMapOf<String, String>()

    lateinit var loadedLocale: Locale

    private fun load(localePair: LocalePair) {
        loadedLocale = localePair.getLocale()

        val translations = AutoCloseInputStream(localePair.content).use {
            JsonParser.parseReader(it.reader()).asJsonObject
        }
        if (translations == null || translations.isJsonNull) {
            return
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

    fun loadFromCallback(callback: (String) -> List<LocalePair>) {
        callback(userLocale).forEach {
            load(it)
        }
    }

    fun loadFromContext(context: Context) {
        fetchLocales(context, userLocale).forEach {
            load(it)
        }
    }

    fun reloadFromContext(context: Context, locale: String) {
        userLocale = locale
        translationMap.clear()
        loadFromContext(context)
    }

    operator fun get(key: String) = translationMap[key] ?: key.also { AbstractLogger.directDebug("Missing translation for $key") }
    fun getOrNull(key: String) = translationMap[key]

    fun format(key: String, vararg args: Pair<String, String>): String {
        return args.fold(get(key)) { acc, pair ->
            acc.replace("{${pair.first}}", pair.second)
        }
    }

    fun getCategory(key: String): LocaleWrapper {
        return LocaleWrapper().apply {
            translationMap.putAll(
                this@LocaleWrapper.translationMap
                    .filterKeys { it.startsWith("$key.") }
                    .mapKeys { it.key.substring(key.length + 1) }
            )
        }
    }
}