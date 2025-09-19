package me.rhunk.snapenhance.common.config

import androidx.compose.ui.graphics.vector.ImageVector
import me.rhunk.snapenhance.common.bridge.wrapper.LocaleWrapper
import kotlin.reflect.KProperty

data class PropertyPair<T>(
    val key: PropertyKey<T>,
    val value: PropertyValue<*>
) {
    val name get() = key.name
}

enum class FeatureNotice(
    val key: String
) {
    UNSTABLE("unstable"),
    BAN_RISK("ban_risk"),
    INTERNAL_BEHAVIOR("internal_behavior");

    val id get() = 1 shl ordinal
}

enum class ConfigFlag {
    NO_TRANSLATE,
    HIDDEN,
    FOLDER,
    USER_IMPORT,
    NO_DISABLE_KEY,
    REQUIRE_RESTART,
    REQUIRE_CLEAN_CACHE,
    SENSITIVE;

    val id = 1 shl ordinal
}

data class VersionCheck(
    // Pair<versionString, versionCode>
    val minVersion: Pair<String, Long>? = null,
    val maxVersion: Pair<String, Long>? = null,
    val isDisabled: Boolean = false,
)  {
    fun checkVersion(versionCode: Long): Pair<Pair<String, Long>, VersionRequirement>? {
        minVersion?.let {
            if (versionCode <= it.second) {
                return minVersion to VersionRequirement.NEWER_REQUIRED
            }
        }

        maxVersion?.let {
            if (versionCode >= it.second) {
                return maxVersion to VersionRequirement.OLDER_REQUIRED
            }
        }

        return null
    }
}

enum class VersionRequirement(
    val key: String
) {
    OLDER_REQUIRED("older_required"),
    NEWER_REQUIRED("newer_required");

    val id = 1 shl ordinal
}

class ConfigParams(
    private var _flags: Int? = null,
    private var _notices: Int? = null,

    var icon: ImageVector? = null,
    var disabledKey: String? = null,
    var customTranslationPath: String? = null,
    var customOptionTranslationPath: String? = null,
    var inputCheck: ((String) -> Boolean)? = { true },
    var filenameFilter: ((String) -> Boolean)? = null,
    var versionCheck: VersionCheck? = null,
) {
    val notices get() = _notices?.let { FeatureNotice.entries.filter { flag -> it and flag.id != 0 } } ?: emptyList()
    val flags get() = _flags?.let { ConfigFlag.entries.filter { flag -> it and flag.id != 0 } } ?: emptyList()

    fun addNotices(vararg values: FeatureNotice) {
        this._notices = (this._notices ?: 0) or values.fold(0) { acc, featureNotice -> acc or featureNotice.id }
    }

    fun addFlags(vararg values: ConfigFlag) {
        this._flags = (this._flags ?: 0) or values.fold(0) { acc, flag -> acc or flag.id }
    }

    fun requireRestart() {
        addFlags(ConfigFlag.REQUIRE_RESTART)
    }
    fun requireCleanCache() {
        addFlags(ConfigFlag.REQUIRE_CLEAN_CACHE)
    }
}

class PropertyValue<T>(
    private var value: T? = null,
    val defaultValues: List<*>? = null
) {
    inner class PropertyValueNullable {
        fun get() = value
        operator fun getValue(t: Any?, property: KProperty<*>): T? = getNullable()
        operator fun setValue(t: Any?, property: KProperty<*>, t1: T?) = set(t1)
    }

    fun nullable() = PropertyValueNullable()

    fun isSet() = value != null
    fun getNullable() = value?.takeIf { it != "null" }
    fun isEmpty() = value == null || value == "null" || value.toString().isEmpty()
    fun get() = getNullable() ?: throw IllegalStateException("Property is not set")
    fun set(value: T?) { setAny(value) }
    @Suppress("UNCHECKED_CAST")
    fun setAny(value: Any?) { this.value = value as T? }

    operator fun getValue(t: Any?, property: KProperty<*>): T = get()
    operator fun setValue(t: Any?, property: KProperty<*>, t1: T?) = set(t1)
}

data class PropertyKey<T>(
    private val _parent: () -> PropertyKey<*>?,
    val name: String,
    val dataType: DataProcessors.PropertyDataProcessor<T>,
    val params: ConfigParams = ConfigParams(),
) {
    private val parentKey by lazy { _parent() }

    fun propertyOption(translation: LocaleWrapper, key: String): String {
        if (key == "null") {
            return translation[params.disabledKey?.let { disabledKey ->
                params.customOptionTranslationPath?.let {
                    "$it.$disabledKey"
                } ?: key
            } ?: "manager.sections.features.disabled"]
        }

        return if (!params.flags.contains(ConfigFlag.NO_TRANSLATE))
            translation[params.customOptionTranslationPath?.let {
                "$it.$key"
            } ?: "features.options.${name}.$key"]
        else key
    }

    fun propertyName() = propertyTranslationPath() + ".name"
    fun propertyDescription() = propertyTranslationPath() + ".description"

    fun propertyTranslationPath(): String {
        params.customTranslationPath?.let {
            return it
        }
        return parentKey?.let {
            "${it.propertyTranslationPath()}.properties.$name"
        } ?: "features.properties.$name"
    }
}

