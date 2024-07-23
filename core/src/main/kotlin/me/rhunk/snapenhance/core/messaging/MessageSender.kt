package me.rhunk.snapenhance.core.messaging

import me.rhunk.snapenhance.common.data.ContentType
import me.rhunk.snapenhance.common.util.protobuf.ProtoWriter
import me.rhunk.snapenhance.core.ModContext
import me.rhunk.snapenhance.core.features.impl.messaging.Messaging
import me.rhunk.snapenhance.core.util.CallbackBuilder
import me.rhunk.snapenhance.core.wrapper.AbstractWrapper
import me.rhunk.snapenhance.core.wrapper.impl.MessageDestinations
import me.rhunk.snapenhance.core.wrapper.impl.SnapUUID
import me.rhunk.snapenhance.mapper.impl.CallbackMapper

class MessageSender(
    private val context: ModContext,
) {
    companion object {
        val audioNoteProto: (Long, String?) -> ByteArray = { duration, userLocale ->
            ProtoWriter().apply {
                from(6, 1) {
                    from(1) {
                        addVarInt(2, 4)
                        from(5) {
                            addVarInt(1, 0)
                            addVarInt(2, 0)
                        }
                        addVarInt(7, 0)
                        addVarInt(13, duration)
                    }
                    if (userLocale != null) {
                        addString(3, userLocale)
                    }
                }
            }.toByteArray()
        }

    }

    private val sendMessageCallback by lazy {
        lateinit var result: Class<*>
        context.mappings.useMapper(CallbackMapper::class) {
            result = callbacks.getClass("SendMessageCallback") ?: return@useMapper
        }
        result
    }

    private fun createLocalMessageContentTemplate(
        contentType: ContentType,
        messageContent: ByteArray,
        localMediaReference: ByteArray? = null,
        savePolicy: String = "PROHIBITED",
    ): String {
        return """
        {
            "mAllowsTranscription": false,
            "mBotMention": false,
            "mContent": [${messageContent.joinToString(",")}],
            "mContentType": "${contentType.name}",
            "mIncidentalAttachments": [],
            "mLocalMediaReferences": [${
                if (localMediaReference != null) {
                    "{\"mId\": [${localMediaReference.joinToString(",")}]}"
                } else {
                    ""
                }
            }],
            "mPlatformAnalytics": {
                "mAttemptId": null,
                "mContent": null,
                "mMetricsMessageMediaType": "NO_MEDIA",
                "mMetricsMessageType": "TEXT",
                "mReactionSource": "NONE"
            },
            "mSavePolicy": "$savePolicy"
        }
        """.trimIndent()
    }

    private fun internalSendMessage(conversations: List<SnapUUID>, localMessageContentTemplate: String, callback: Any) {
        val sendMessageWithContentMethod = context.classCache.conversationManager.declaredMethods.first { it.name == "sendMessageWithContent" }

        val localMessageContent = context.gson.fromJson(localMessageContentTemplate, context.classCache.localMessageContent)
        val messageDestinations = MessageDestinations(AbstractWrapper.newEmptyInstance(context.classCache.messageDestinations)).also {
            it.conversations = conversations.toCollection(ArrayList())
            it.mPhoneNumbers = arrayListOf<Any>()
            it.stories = arrayListOf<Any>()
        }

        sendMessageWithContentMethod.invoke(context.feature(Messaging::class).conversationManager?.instanceNonNull(), messageDestinations.instanceNonNull(), localMessageContent, callback)
    }

    fun sendChatMessage(conversations: List<SnapUUID>, message: String, onError: (Any) -> Unit = {}, onSuccess: () -> Unit = {}) {
        internalSendMessage(conversations, createLocalMessageContentTemplate(ContentType.CHAT, ProtoWriter().apply {
            from(2) {
                addString(1, message)
            }
        }.toByteArray(), savePolicy = "LIFETIME"), CallbackBuilder(sendMessageCallback)
            .override("onSuccess", callback = { onSuccess() })
            .override("onError", callback = { onError(it.arg(0)) })
            .build())
    }

    fun sendCustomChatMessage(conversations: List<SnapUUID>, contentType: ContentType, message: ProtoWriter.() -> Unit, onError: (Any) -> Unit = {}, onSuccess: () -> Unit = {}) {
        internalSendMessage(conversations, createLocalMessageContentTemplate(contentType, ProtoWriter().apply {
            message()
        }.toByteArray(), savePolicy = "LIFETIME"), CallbackBuilder(sendMessageCallback)
            .override("onSuccess", callback = { onSuccess() })
            .override("onError", callback = { onError(it.arg(0)) })
            .build())
    }
}