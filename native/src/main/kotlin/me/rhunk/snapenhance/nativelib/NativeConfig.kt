package me.rhunk.snapenhance.nativelib

data class NativeConfig(
    @JvmField
    val disableBitmoji: Boolean = false,
    @JvmField
    val disableMetrics: Boolean = false,
    @JvmField
    val composerHooks: Boolean = false,
    @JvmField
    val remapExecutable: Boolean = false,
    @JvmField
    val customEmojiFontPath: String? = null,
)