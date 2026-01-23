package me.rhunk.snapenhance.nativelib

data class NativeConfig(
    @JvmField
    val disableBitmoji: Boolean = false,
    @JvmField
    val disableMetrics: Boolean = false,
    @JvmField
    val valdiHooks: Boolean = false,
    @JvmField
    val customEmojiFontPath: String? = null,
)