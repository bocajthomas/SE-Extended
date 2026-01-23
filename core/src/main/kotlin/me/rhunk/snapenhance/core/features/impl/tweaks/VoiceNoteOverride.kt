package me.rhunk.snapenhance.core.features.impl.tweaks

import android.view.ViewGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.core.SnapEnhance
import me.rhunk.snapenhance.core.event.events.impl.BindViewEvent
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.features.impl.downloader.MediaDownloader
import me.rhunk.snapenhance.core.features.impl.messaging.Messaging
import me.rhunk.snapenhance.core.ui.getValdiContext
import me.rhunk.snapenhance.core.util.dataBuilder
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.ktx.getId
import me.rhunk.snapenhance.core.util.ktx.getObjectField
import me.rhunk.snapenhance.core.util.ktx.getObjectFieldOrNull
import me.rhunk.snapenhance.core.util.makeFunctionProxy

class VoiceNoteOverride: Feature("Voice Note Override") {
    override fun init() {
        val voiceNoteAutoPlay = context.config.experimental.voiceNoteAutoPlay.get()
        val autoDownloadVoiceNotes = context.config.downloader.autoDownloadVoiceNotes.get()

        if (!autoDownloadVoiceNotes && !voiceNoteAutoPlay) return

        val playbackMap = sortedMapOf<Long, Any>()

        fun setPlaybackState(componentContext: Any, state: String): Boolean {
            val seek = componentContext.getObjectField("_seek") ?: return false
            seek.javaClass.getMethod("invoke", Any::class.java).invoke(seek, 0)

            val onPlayButtonTapped = componentContext.getObjectField("_onPlayButtonTapped") ?: return false
            onPlayButtonTapped.javaClass.getMethod("invoke", Any::class.java).invoke(
                onPlayButtonTapped,
                findClass("com.snap.voicenotes.PlaybackState").enumConstants?.first {
                    it.toString() == state
                }
            )
            return true
        }

        fun getCurrentContextMessageId(currentContext: Any): Long? {
            return synchronized(playbackMap) {
                playbackMap.entries.lastOrNull { entry -> entry.value.hashCode() == currentContext.hashCode() }?.key
            }
        }

        fun playNextVoiceNote(currentContext: Any) {
            val currentContextMessageId = getCurrentContextMessageId(currentContext) ?: return

            context.log.verbose("messageId=$currentContextMessageId")

            val nextPlayback = synchronized(playbackMap) {
                playbackMap.entries.firstOrNull { it.key > currentContextMessageId }
            }

            if (nextPlayback == null) {
                context.log.verbose("No more voice notes to play")
                return
            }

            setPlaybackState(nextPlayback.value, "PLAYING")
        }

        context.classCache.conversationManager.apply {
            arrayOf("enterConversation", "exitConversation").forEach {
                hook(it, HookStage.BEFORE) {
                    synchronized(playbackMap) {
                        playbackMap.clear()
                    }
                }
            }
        }

        SnapEnhance.classCache.nativeBridge.hook("createContext", HookStage.BEFORE) { param ->
            val componentPath = param.arg<String>(1)
            val componentContext = param.argNullable<Any>(3)

            if (componentPath != "PlaybackView@voice_notes/src/PlaybackView") return@hook

            var lastPlayerState: String? = null

            componentContext.dataBuilder {
                interceptFieldInterface("_onPlayButtonTapped") { args, originalCall ->
                    lastPlayerState = null
                    context.log.verbose("onPlayButtonTapped ${args.contentToString()}")
                    originalCall(args)
                }

                from("_playbackStateObservable") {
                    interceptFieldInterface("_subscribe") { subscribeArgs, originalSubscribe ->
                        originalSubscribe(
                            arrayOf(
                                makeFunctionProxy(
                                    subscribeArgs[0]!!
                                ) { args, originalCall ->
                                    val state = args[2]?.toString()

                                    if (autoDownloadVoiceNotes && state != lastPlayerState && state == "PLAYING") {
                                        val currentConversationId = context.feature(Messaging::class).openedConversationUUID.toString()
                                        val currentMessageId = getCurrentContextMessageId(componentContext!!)
                                        val mediaDownloader = context.feature(MediaDownloader::class)

                                        context.coroutineScope.launch {
                                            val databaseMessage = context.database.getConversationServerMessage(currentConversationId, currentMessageId ?: return@launch) ?: throw IllegalStateException("Failed to get database message")

                                            if (mediaDownloader.canAutoDownloadMessage(databaseMessage)) {
                                                mediaDownloader.downloadMessageId(databaseMessage.clientMessageId.toLong(), forceDownloadFirst = true)
                                            }
                                        }
                                    }

                                    if (voiceNoteAutoPlay && state == "PAUSED" && lastPlayerState == "PLAYING") {
                                        lastPlayerState = null
                                        context.log.verbose("playback finished. playing next voice note")
                                        runCatching {
                                            context.coroutineScope.launch(Dispatchers.Main) {
                                                playNextVoiceNote(componentContext!!)
                                            }
                                        }.onFailure {
                                            context.log.error("Failed to play next voice note", it)
                                        }
                                    }

                                    lastPlayerState = state
                                    originalCall(args)
                                }
                            )
                        )
                    }
                }
            }
        }

        onNextActivityCreate {
            context.event.subscribe(BindViewEvent::class) { event ->
                event.chatMessage { _, _ ->
                    val messagePluginContentHolder = event.view.findViewById<ViewGroup>(context.resources.getId("plugin_content_holder")) ?: return@subscribe
                    val composerRootView = messagePluginContentHolder.getChildAt(0) ?: return@subscribe

                    composerRootView.post {
                        val composerContext = composerRootView.getValdiContext() ?: return@post
                        val playbackViewComponentContext = composerContext.componentContext?.get() ?: return@post

                        if (event.databaseMessage?.serverMessageId == 0 || playbackViewComponentContext.getObjectFieldOrNull("_getSamples") == null) return@post

                        val serverMessageId = event.databaseMessage?.serverMessageId?.toLong() ?: return@post

                        synchronized(playbackMap) {
                            playbackMap[serverMessageId] = playbackViewComponentContext
                        }
                    }
                }
            }
        }
    }
}