package me.rhunk.snapenhance.core.features.impl.experiments

import me.rhunk.snapenhance.common.data.ContentType
import me.rhunk.snapenhance.common.util.protobuf.ProtoEditor
import me.rhunk.snapenhance.core.event.events.impl.BuildMessageEvent
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.util.dataBuilder
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.ktx.getObjectFieldOrNull
import me.rhunk.snapenhance.core.util.ktx.setObjectField
import java.nio.ByteBuffer

class BetterTranscript: Feature("Better Transcript") {
    private val voiceML: Any by lazy {
        findClass("com.snapchat.client.voiceml.IVoiceMLSDK").getMethod("create").invoke(null) ?: error("Failed to create IVoiceMLSDK instance")
    }

    private fun createAsrConfig(): Any? {
        findClass("com.snapchat.client.voiceml.IConfigFactory").methods.first { it.name == "simpleAsrConfig" }.let { method ->
            return method.invoke(null, method.parameterTypes[0].dataBuilder {
                set("mSampleRate", 44100)
                set("mLanguageModel", "en")
                set("mUseCase", "VOICENOTESTRANSCRIPTION")
                set("mAppVersion", "voice note transcript")
                set("mUiLanguage", "en")
                set("mAuthType", "SNAPTOKEN")
                set("mEncoding", "AAC")
            })
        }
    }

    fun transcribe(audio: ByteBuffer): String? {
        val transcribeMethod = voiceML.javaClass.methods.first { it.name == "asrTranscribe" }
        val snapToken = context.database.getAccessTokens(context.database.myUserId)?.get("api-gateway") ?: error("Failed to get api-gateway token")

        return transcribeMethod.invoke(voiceML, snapToken, createAsrConfig(), audio)
            ?.let { asrResult ->
                asrResult.getObjectFieldOrNull("mTranscription")?.toString()
            }
    }

    override fun init() {
        if (context.config.experimental.betterTranscript.globalState != true) return

        onNextActivityCreate {
            val config = context.config.experimental.betterTranscript

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
                config.preferredTranscriptionLang.getNullable()?.takeIf {
                    it.isNotBlank()
                }?.trim()?.lowercase()?.let {
                    val asrConfig = param.arg<Any>(1)
                    asrConfig.getObjectFieldOrNull("mBaseConfig")?.apply {
                        setObjectField("mLanguageModel", it)
                        setObjectField("mUiLanguage", it)
                    }
                }
            }
        }
    }
}