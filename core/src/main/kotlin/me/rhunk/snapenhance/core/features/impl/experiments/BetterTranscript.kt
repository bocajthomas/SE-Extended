package me.rhunk.snapenhance.core.features.impl.experiments

import me.rhunk.snapenhance.common.data.ContentType
import me.rhunk.snapenhance.common.util.TranscriptApi
import me.rhunk.snapenhance.common.util.protobuf.ProtoEditor
import me.rhunk.snapenhance.core.event.events.impl.BuildMessageEvent
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.features.FeatureLoadParams
import me.rhunk.snapenhance.core.util.dataBuilder
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.ktx.getObjectFieldOrNull
import me.rhunk.snapenhance.core.util.ktx.setObjectField
import okhttp3.RequestBody.Companion.toRequestBody
import java.lang.reflect.Method
import java.nio.ByteBuffer

class BetterTranscript: Feature("Better Transcript", loadParams = FeatureLoadParams.ACTIVITY_CREATE_SYNC) {
    override fun onActivityCreate() {
        if (context.config.experimental.betterTranscript.globalState != true) return
        val config = context.config.experimental.betterTranscript
        val preferredTranscriptionLang = config.preferredTranscriptionLang.getNullable()?.takeIf {
            it.isNotBlank()
        }
        val transcriptApi by lazy { TranscriptApi() }

        if (config.forceTranscription.get()) {
            context.event.subscribe(BuildMessageEvent::class, priority = 104) { event ->
                if (event.message.messageContent?.contentType != ContentType.NOTE) return@subscribe
                event.message.messageContent!!.content = ProtoEditor(event.message.messageContent!!.content!!).apply {
                    edit(6, 1) {
                        if (firstOrNull(3) == null) {
                            addString(3, context.getConfigLocale())
                        }
                    }
                }.toByteArray()
            }
        }

        findClass("com.snapchat.client.voiceml.IVoiceMLSDK\$CppProxy").hook("asrTranscribe", HookStage.BEFORE) { param ->
            if (config.enhancedTranscript.get()) {
                val buffer = param.arg<ByteBuffer>(2).let {
                    it.rewind()
                    ByteArray(it.remaining()).also { it1 -> it.get(it1); it.rewind() }
                }
                val result = runCatching {
                    transcriptApi.transcribe(
                        buffer.toRequestBody(),
                        lang = config.preferredTranscriptionLang.getNullable()?.takeIf {
                            it.isNotBlank()
                        }?.uppercase()
                    )
                }.onFailure {
                    context.log.error("Failed to transcribe audio", it)
                    context.shortToast("Failed to transcribe audio! Check logcat for more details.")
                }.getOrNull()

                param.setResult(
                    (param.method() as Method).returnType.dataBuilder {
                        set("mError", result == null)
                        set("mNlpResponses", ArrayList<Any>())
                        set("mWordInfo", ArrayList<Any>())
                        set("mTranscription", result)
                    }
                )
                return@hook
            }
            preferredTranscriptionLang?.lowercase()?.let {
                val asrConfig = param.arg<Any>(1)
                asrConfig.getObjectFieldOrNull("mBaseConfig")?.apply {
                    setObjectField("mLanguageModel", it)
                    setObjectField("mUiLanguage", it)
                }
            }
        }
    }
}