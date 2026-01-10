package me.rhunk.snapenhance.nativelib

import android.annotation.SuppressLint
import android.util.Log
import kotlin.math.absoluteValue
import kotlin.random.Random

class NativeLib {
    var nativeUnaryCallCallback: (NativeRequestData) -> Unit = {}
    var signatureCache: String? = null

    companion object {
        var initialized = false
            private set
    }

    fun initOnce(callback: NativeLib.() -> Unit): () -> Unit {
        if (initialized) throw IllegalStateException("NativeLib already initialized")
        return runCatching {
            System.loadLibrary(BuildConfig.NATIVE_NAME)
            initialized = true
            callback(this)
            preInit()
            return@runCatching {
                signatureCache = init(signatureCache) ?: throw IllegalStateException("NativeLib init failed. Check logcat for more info")
            }
        }.onFailure {
            initialized = false
            Log.e("SnapEnhance", "NativeLib init failed", it)
        }.getOrThrow()
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

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    fun loadSharedLibrary(content: ByteArray) {
        if (!initialized) throw IllegalStateException("NativeLib not initialized")
        val generatedPath = "/data/app/${Random.nextLong().absoluteValue.toString(16)}.so"
        addLinkerSharedLibrary(generatedPath, content)
        System.load(generatedPath)
    }

    private external fun preInit()
    private external fun init(signatureCache: String?): String?
    private external fun loadConfig(config: NativeConfig)
    private external fun lockDatabase(name: String, callback: Runnable)
    external fun setValdiLoader(code: String)
    private external fun addLinkerSharedLibrary(path: String, content: ByteArray)
}