package me.rhunk.snapenhance.nativelib

import android.util.Log

@Suppress("KotlinJniMissingFunction")
class NativeLib {
    var nativeUnaryCallCallback: (NativeRequestData) -> Unit = {}

    companion object {
        var initialized = false
            private set
    }

    fun initOnce(callback: NativeLib.() -> Unit) {
        if (initialized) throw IllegalStateException("NativeLib already initialized")
        runCatching {
            System.loadLibrary(BuildConfig.NATIVE_NAME)
            initialized = true
            callback(this)
            if (!init()) {
                throw IllegalStateException("NativeLib init failed. Check logcat for more info")
            }
        }.onFailure {
            initialized = false
            Log.e("SnapEnhance", "NativeLib init failed", it)
        }
    }

    @Suppress("unused")
    private fun onNativeUnaryCall(uri: String, buffer: ByteArray): NativeRequestData? {
        val nativeRequestData = NativeRequestData(uri, buffer)
        runCatching {
            nativeUnaryCallCallback(nativeRequestData)
        }.onFailure {
            Log.e("SnapEnhance", "nativeUnaryCallCallback failed", it)
        }
        if (nativeRequestData.canceled || !nativeRequestData.buffer.contentEquals(buffer)) return nativeRequestData
        return null
    }

    fun loadNativeConfig(config: NativeConfig) {
        if (!initialized) return
        loadConfig(config)
    }

    fun lockNativeDatabase(name: String, callback: () -> Unit) {
        if (!initialized) return
        lockDatabase(name) {
            runCatching {
                callback()
            }.onFailure {
                Log.e("SnapEnhance", "lockNativeDatabase callback failed", it)
            }
        }
    }

    private external fun init(): Boolean
    private external fun loadConfig(config: NativeConfig)
    private external fun lockDatabase(name: String, callback: Runnable)
    external fun setComposerLoader(code: String)
    external fun composerEval(code: String): String?
    external fun hideAnonymousDexFiles()
}