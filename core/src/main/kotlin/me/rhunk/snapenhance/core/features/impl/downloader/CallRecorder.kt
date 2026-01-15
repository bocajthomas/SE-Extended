package me.rhunk.snapenhance.core.features.impl.downloader

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.os.ParcelFileDescriptor
import me.rhunk.snapenhance.bridge.call.CallDownloadSession
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.hook.hookConstructor
import me.rhunk.snapenhance.core.util.ktx.getObjectField
import me.rhunk.snapenhance.core.util.ktx.getObjectFieldOrNull
import me.rhunk.snapenhance.core.wrapper.impl.SnapUUID
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

class CallRecorder : Feature("Call Recorder") {
    private var wasInCall = false
    private var callDownloadSession: CallDownloadSession? = null

    inner class LazyStream(
        private val audioFormat: AudioFormat,
        private val startTimestamp: Long = System.currentTimeMillis(),
    ) {
        private var stream: ParcelFileDescriptor.AutoCloseOutputStream? = null

        fun get(): OutputStream? {
            if (stream != null) return stream
            if (callDownloadSession == null) return null

            stream = ParcelFileDescriptor.AutoCloseOutputStream(
                callDownloadSession?.createStream(
                    startTimestamp,
                    audioFormat.channelCount,
                    audioFormat.sampleRate,
                    audioFormat.encoding
                ) ?: return null
            )

            return stream
        }
    }

    private fun initCallDownloadSession(conversationId: String) {
        val author = (if (context.database.getConversationType(conversationId) == 1) {
            context.database.getFeedEntryByConversationId(conversationId)?.feedDisplayName
        } else {
            context.database.getDMOtherParticipant(conversationId)?.let { context.database.getFriendInfo(it)?.mutableUsername }
        }) ?: "unknown"
        callDownloadSession = context.bridgeClient.startCallDownload(System.currentTimeMillis(), author)
    }

    private fun onCallStarted(conversationId: String) {
        initCallDownloadSession(conversationId)
    }

    private fun onCallEnded(conversationId: String) {
        callDownloadSession?.end()
    }

    override fun init() {
        val callRecorderConfig = context.config.downloader.callRecorder.getNullable()
        if (callRecorderConfig == null) return

        val streams = ConcurrentHashMap<Int, LazyStream>() // audioTrack -> stream

        runCatching {
            findClass("com.snapchat.talkcorev3.CallingSessionState")
        }.getOrNull()?.hookConstructor(HookStage.AFTER) { param ->
            val instance = param.thisObject<Any>()
            val callingState = instance.getObjectFieldOrNull("mLocalUser")?.getObjectField("mCallingState")

            if (callingState.toString() == "IN_CALL") {
                // TODO: implement for older Snapchat versions
            }
        } ?: findClass("com.snapchat.talkcorev3.TSCallingStateUpdateParams").hookConstructor(
            HookStage.AFTER) { param ->
            val instance = param.thisObject<Any>()
            val conversationId = SnapUUID(instance.getObjectField("mConversationId")).toString()

            if (instance.getObjectFieldOrNull("mInCall") == true) {
                if (!wasInCall) {
                    wasInCall = true
                    onCallStarted(conversationId)
                }
            } else {
                if (wasInCall) {
                    wasInCall = false
                    onCallEnded(conversationId)
                }
            }
        }


        AudioRecord::class.java.apply {
            if (callRecorderConfig == "only_record_others") return@apply
            declaredConstructors.first { it.parameterCount > 5 }.hook(HookStage.AFTER) { param ->
                val audioAttributes = param.arg<AudioAttributes>(0)
                context.log.verbose(audioAttributes.usage)
                if (audioAttributes.usage != AudioAttributes.USAGE_UNKNOWN) return@hook
                val audioFormat = param.arg<AudioFormat>(1)
                val hashCode = param.thisObject<Any>().hashCode()

                streams.put(hashCode, LazyStream(audioFormat))
                context.log.verbose("AudioRecord called usage=${audioAttributes.usage}, format=$audioFormat")
            }

            getMethod("read", ByteBuffer::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType).hook(
                HookStage.AFTER) { param ->
                val readBytes = param.getResult() as Int
                if (readBytes <= 0) return@hook

                streams[param.thisObject<Any>().hashCode()]?.let { handlers ->
                    val byteBuffer = param.arg<ByteBuffer>(0)
                    val position = byteBuffer.position()
                    val buffer = ByteArray(readBytes)
                    byteBuffer.get(buffer)
                    byteBuffer.position(position)

                    runCatching {
                        handlers.get()?.write(buffer, 0, buffer.size)
                    }.onFailure {
                        context.log.error("Failed to record call audio data", it)
                    }
                }
            }

            hook("release", HookStage.BEFORE) {
                runCatching {
                    streams.remove(it.thisObject<Any>().hashCode())?.get()?.close()
                }
            }
        }

        AudioTrack::class.java.apply {
            if (callRecorderConfig == "only_record_self") return@apply
            getConstructor(
                AudioAttributes::class.java,
                AudioFormat::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
            ).hook(HookStage.AFTER) { param ->
                val audioAttributes = param.arg<AudioAttributes>(0)
                if (audioAttributes.usage != AudioAttributes.USAGE_VOICE_COMMUNICATION) return@hook
                val audioFormat = param.arg<AudioFormat>(1)
                val hashCode = param.thisObject<Any>().hashCode()

                streams.put(hashCode, LazyStream(audioFormat))
                context.log.verbose("AudioTrack called usage=${audioAttributes.usage}, format=$audioFormat")
            }

            getMethod("write", ByteBuffer::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType).hook(
                HookStage.BEFORE) { param ->
                streams[param.thisObject<Any>().hashCode()]?.let { handlers ->
                    val byteBuffer = param.arg<ByteBuffer>(0)
                    val position = byteBuffer.position()
                    val buffer = ByteArray(param.arg(1))
                    byteBuffer.get(buffer)
                    byteBuffer.position(position)

                    runCatching {
                        handlers.get()?.write(buffer, 0, buffer.size)
                    }.onFailure {
                        context.log.error("Failed to record call audio data", it)
                    }
                }
            }

            hook("release", HookStage.BEFORE) {
                runCatching { streams.remove(it.thisObject<Any>().hashCode())?.get()?.close() }
            }
        }
    }
}