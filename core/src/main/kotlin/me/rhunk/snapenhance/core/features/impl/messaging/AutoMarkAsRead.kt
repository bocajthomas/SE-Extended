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
import me.rhunk.snapenhance.core.event.events.impl.SendMessageWithContentEvent
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.features.FeatureLoadParams
import me.rhunk.snapenhance.core.features.impl.spying.StealthMode
import me.rhunk.snapenhance.core.ui.ViewAppearanceHelper
import me.rhunk.snapenhance.core.util.ktx.getObjectFieldOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random

class AutoMarkAsRead : Feature("Auto Mark As Read", loadParams = FeatureLoadParams.INIT_SYNC) {
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