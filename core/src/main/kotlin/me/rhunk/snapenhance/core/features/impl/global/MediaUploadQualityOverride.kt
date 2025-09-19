package me.rhunk.snapenhance.core.features.impl.global

import android.graphics.Bitmap
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.mapper.impl.MediaQualityLevelProviderMapper
import java.lang.reflect.Method

class MediaUploadQualityOverride : Feature("Media Upload Quality Override") {
    override fun init() {
        if (context.config.global.mediaUploadQualityConfig.forceVideoUploadSourceQuality.get()) {
            context.mappings.useMapper(MediaQualityLevelProviderMapper::class) {
                mediaQualityLevelProvider.getAsClass()?.hook(
                    mediaQualityLevelProviderMethod.getAsString()!!,
                    HookStage.BEFORE
                ) { param ->
                    param.setResult((param.method() as Method).returnType.enumConstants!!.firstOrNull { it.toString() == "LEVEL_MAX" } )
                }
            }
        }

        val disableImageCompression by context.config.global.mediaUploadQualityConfig.disableImageCompression
        val imageUploadFormat = context.config.global.mediaUploadQualityConfig.customUploadImageFormat.getNullable()

        if (imageUploadFormat != null || disableImageCompression) {
            Bitmap::class.java.hook("compress", HookStage.BEFORE) { param ->
                if (param.arg<Int>(1) == 0) return@hook
                if (param.arg<Any>(0) == Bitmap.CompressFormat.JPEG) {
                    @Suppress("DEPRECATION")
                    param.setArg(0, when (imageUploadFormat) {
                        "png" -> Bitmap.CompressFormat.PNG
                        "webp" -> Bitmap.CompressFormat.WEBP
                        "jpeg" -> Bitmap.CompressFormat.JPEG
                        else -> Bitmap.CompressFormat.JPEG
                    })
                    if (disableImageCompression) {
                        param.setArg(1, 100)
                    }
                }
            }

            findClass("com.snap.camera.jni.SnapImageTranscoder").hook("nativeEncodeBitmapToJpeg", HookStage.BEFORE) {
                it.setResult(ByteArray(0))
            }
        }
    }
}