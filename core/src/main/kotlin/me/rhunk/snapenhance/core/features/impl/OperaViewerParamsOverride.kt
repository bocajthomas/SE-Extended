package me.rhunk.snapenhance.core.features.impl

import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hookConstructor
import me.rhunk.snapenhance.core.wrapper.impl.media.opera.ParamMap
import me.rhunk.snapenhance.mapper.impl.OperaViewerParamsMapper
import java.util.concurrent.ConcurrentHashMap

class OperaViewerParamsOverride : Feature("OperaViewerParamsOverride") {
    var currentPlaybackRate = 1.0F

    data class OverrideKey(
        val name: String,
        val defaultValue: Any?
    )

    data class Override(
        val filter: (value: Any?) -> Boolean,
        val value: (key: OverrideKey, value: Any?) -> Any?
    )

    override fun init() {
        val overrideMap = mutableMapOf<String, Override>()

        fun overrideParam(key: String, filter: (value: Any?) -> Boolean, value: (overrideKey: OverrideKey, value: Any?) -> Any?) {
            overrideMap[key] = Override(filter, value)
        }

        currentPlaybackRate = context.config.global.defaultVideoPlaybackRate.getNullable()?.takeIf { it > 0 } ?: 1.0F

        if (context.config.global.videoPlaybackRateSlider.get() || currentPlaybackRate != 1.0F) {
            overrideParam("video_playback_rate", { currentPlaybackRate != 1.0F }, { _, _ -> currentPlaybackRate.toDouble() })
        }

        if (context.config.messaging.loopMediaPlayback.get()) {
            //https://github.com/rodit/SnapMod/blob/master/app/src/main/java/xyz/rodit/snapmod/features/opera/SnapDurationModifier.kt
            overrideParam("auto_advance_mode", { true }, { key, _ -> key.defaultValue })
            overrideParam("auto_advance_max_loop_number", { true }, { _, _ -> Int.MAX_VALUE })
            overrideParam("media_playback_mode", { true }, { _, value ->
                val playbackMode = value ?: return@overrideParam null
                playbackMode::class.java.enumConstants?.firstOrNull {
                    it.toString() == "LOOPING"
                } ?: return@overrideParam value
            })
        }

        onNextActivityCreate {
            context.mappings.useMapper(OperaViewerParamsMapper::class) {
                fun overrideParamResult(paramKey: Any, value: Any?): Any? {
                    val fields = paramKey::class.java.fields
                    val key = OverrideKey(
                        name = fields.firstOrNull {
                            it.type == String::class.java
                        }?.get(paramKey)?.toString() ?: return value,
                        defaultValue = fields.firstOrNull {
                            it.type == Object::class.java
                        }?.get(paramKey)
                    )

                    overrideMap[key.name]?.let { override ->
                        if (override.filter(value)) {
                            runCatching {
                                return override.value(key, value)
                            }.onFailure {
                                context.log.error("Failed to override param $key", it)
                            }
                        }
                    }

                    return value
                }

                classReference.get()?.hookConstructor(HookStage.AFTER) { param ->
                    ParamMap(param.thisObject()).paramMapField.set(param.thisObject(), object: ConcurrentHashMap<Any, Any>() {
                        override fun put(key: Any, value: Any): Any? {
                            return super.put(key, overrideParamResult(key, value) ?: return value)
                        }

                        override fun get(key: Any): Any? {
                            return overrideParamResult(key, super.get(key))
                        }
                    })
                }
            }
        }
    }
}