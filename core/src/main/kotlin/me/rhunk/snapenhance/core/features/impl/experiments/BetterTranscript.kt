package me.rhunk.snapenhance.core.features.impl.experiments

import me.rhunk.snapenhance.common.data.ContentType
import me.rhunk.snapenhance.common.util.protobuf.ProtoEditor
import me.rhunk.snapenhance.core.event.events.impl.BuildMessageEvent
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.ktx.getObjectFieldOrNull
import me.rhunk.snapenhance.core.util.ktx.setObjectField

class BetterTranscript: Feature("Better Transcript") {
    override fun init() {
        if (context.config.experimental.betterTranscript.globalState != true) return

        onNextActivityCreate {
            val config = context.config.experimental.betterTranscript
            val preferredTranscriptionLang = config.preferredTranscriptionLang.getNullable()?.takeIf {
                it.isNotBlank()
            }

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
}