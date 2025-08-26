package me.rhunk.snapenhance.core.features.impl.messaging

import android.widget.ProgressBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.common.data.ContentType
import me.rhunk.snapenhance.common.data.MessageUpdate
import me.rhunk.snapenhance.core.event.events.impl.OnSnapInteractionEvent
import me.rhunk.snapenhance.core.event.events.impl.SendMessageWithContentEvent
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.features.impl.spying.StealthMode
import me.rhunk.snapenhance.core.ui.ViewAppearanceHelper
import me.rhunk.snapenhance.core.util.CallbackBuilder
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.ktx.getObjectFieldOrNull
import me.rhunk.snapenhance.core.wrapper.impl.SnapUUID
import me.rhunk.snapenhance.mapper.impl.CallbackMapper
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random

class AutoMarkAsRead : Feature("Auto Mark As Read") {
    val canMarkConversationAsRead by lazy { context.config.messaging.autoMarkAsRead.get().contains("conversation_read") }

    fun markConversationsAsRead(conversationIds: List<String>) {
        conversationIds.forEach { conversationId ->
            val lastClientMessageId = context.database.getMessagesFromConversationId(conversationId, 1)?.firstOrNull()?.clientMessageId?.toLong() ?: Long.MAX_VALUE
            context.feature(StealthMode::class).addDisplayedMessageException(lastClientMessageId)
            context.feature(Messaging::class).conversationManager?.displayedMessages(conversationId, lastClientMessageId) {
                if (it != null) {
                    context.log.warn("Failed to mark message $lastClientMessageId as read in conversation $conversationId")
                }
            }
        }
    }

    suspend fun markSnapAsSeen(conversationId: String, clientMessageId: Long): String? {
        return suspendCoroutine { continuation ->
            context.feature(Messaging::class).conversationManager?.updateMessage(conversationId, clientMessageId, MessageUpdate.READ) {
                continuation.resume(it)
                if (it != null && it != "DUPLICATEREQUEST") {
                    context.log.error("Error marking message as read $it")
                }
            }
        }
    }

    fun markSnapsAsSeen(conversationId: String) {
        val messaging = context.feature(Messaging::class)
        val messageIds = messaging.getFeedCachedMessageIds(conversationId)?.takeIf { it.isNotEmpty() } ?: run {
            context.inAppOverlay.showStatusToast(
                Icons.Default.WarningAmber,
                context.translation["mark_as_seen.no_unseen_snaps_toast"]
            )
            return
        }

        var job: Job? = null
        val dialog = ViewAppearanceHelper.newAlertDialogBuilder(context.mainActivity)
            .setTitle("Processing...")
            .setView(ProgressBar(context.mainActivity).apply {
                setPadding(10, 10, 10, 10)
            })
            .setOnDismissListener { job?.cancel() }
            .show()

        context.coroutineScope.launch(Dispatchers.IO) {
            messageIds.forEach { messageId ->
                markSnapAsSeen(conversationId, messageId)
                delay(Random.nextLong(20, 60))
                context.runOnUiThread {
                    dialog.setTitle("Processing... (${messageIds.indexOf(messageId) + 1}/${messageIds.size})")
                }
            }
        }.also { job = it }.invokeOnCompletion {
            context.runOnUiThread {
                dialog.dismiss()
            }
        }
    }

    override fun init() {
        val config by context.config.messaging.autoMarkAsRead
        if (config.isEmpty()) return

        if (config.contains("save_snap_in_chat")) {
            var lastInteractedSnapClientMessageId = -1L

            context.event.subscribe(OnSnapInteractionEvent::class) { event ->
                lastInteractedSnapClientMessageId = event.messageId
            }

            context.classCache.conversationManager.hook("updateMessage", HookStage.BEFORE) { param ->
                if (param.arg<Any>(2).toString() != "SAVE") return@hook

                val clientMessageId = param.arg<Long>(1)
                if (lastInteractedSnapClientMessageId != clientMessageId) return@hook

                val conversationId = SnapUUID(param.arg(0))

                param.setResult(null)

                val snapManager = context.feature(Messaging::class).snapManager ?: return@hook
                val stealthMode = context.feature(StealthMode::class)

                // ignore non-stealth mode conversations
                if (!stealthMode.canUseRule(conversationId.toString())) return@hook

                stealthMode.addSnapInteractionException(clientMessageId)

                val onSnapInteraction = snapManager.javaClass.methods.firstOrNull { it.name == "onSnapInteraction" } ?: return@hook

                context.mappings.useMapper(CallbackMapper::class) {
                    onSnapInteraction.invoke(snapManager,
                        findClass("com.snapchat.client.messaging.SnapInteractionType").enumConstants!!.first { it.toString() == "VIEWING_INITIATED" },
                        conversationId.instanceNonNull(),
                        clientMessageId,
                        CallbackBuilder(callbacks.getClass("SnapInteractionCallback")!!)
                            .override("onSuccess") {
                                param.invokeOriginal()
                                stealthMode.addSnapInteractionException(clientMessageId)
                            }
                            .override("onError") {
                                context.log.verbose("error ${it.arg<Any>(0)}")
                            }
                            .build()
                    )
                }
            }
        }

        context.event.subscribe(SendMessageWithContentEvent::class) { event ->
            event.addCallbackResult("onSuccess") {
                if (canMarkConversationAsRead) {
                    markConversationsAsRead(event.destinations.conversations?.map { it.toString() } ?: return@addCallbackResult)
                }

                if (config.contains("snap_reply")) {
                    val quotedMessageId = event.messageContent.instanceNonNull().getObjectFieldOrNull("mQuotedMessageId") as? Long ?: return@addCallbackResult
                    val message = context.database.getConversationMessageFromId(quotedMessageId) ?: return@addCallbackResult

                    if (message.contentType == ContentType.SNAP.id) {
                        context.coroutineScope.launch {
                            markSnapAsSeen(event.destinations.conversations?.firstOrNull()?.toString() ?: return@launch, quotedMessageId)
                        }
                    }
                }
            }
        }
    }
}