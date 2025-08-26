package me.rhunk.snapenhance.core.features.impl.messaging

import android.content.ComponentName
import android.content.Intent
import me.rhunk.snapenhance.common.Constants
import me.rhunk.snapenhance.common.ReceiversConfig
import me.rhunk.snapenhance.core.event.events.impl.ConversationUpdateEvent
import me.rhunk.snapenhance.core.event.events.impl.OnSnapInteractionEvent
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.features.impl.spying.StealthMode
import me.rhunk.snapenhance.core.util.EvictingMap
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.hook.hookConstructor
import me.rhunk.snapenhance.core.util.ktx.getObjectField
import me.rhunk.snapenhance.core.util.ktx.getObjectFieldOrNull
import me.rhunk.snapenhance.core.wrapper.impl.*
import me.rhunk.snapenhance.mapper.impl.CallbackMapper
import me.rhunk.snapenhance.mapper.impl.FriendsFeedEventDispatcherMapper
import java.util.UUID
import java.util.concurrent.Future

class Messaging : Feature("Messaging") {
    var conversationManager: ConversationManager? = null
        private set
    var snapManager: Any? = null
        private set

    private var conversationManagerDelegate: Any? = null
    private var identityDelegate: Any? = null

    var openedConversationUUID: SnapUUID? = null
        private set
    var lastFocusedConversationId: String? = null
        private set
    var lastFocusedConversationType: Int = -1
        private set
    var lastFocusedMessageId: Long = -1
        private set

    private val feedCachedSnapMessages = EvictingMap<String, List<Long>>(100)
    private val conversationManagerReadyListeners = mutableListOf<() -> Unit>()

    fun onConversationManagerReady(listener: () -> Unit) {
        synchronized(conversationManagerReadyListeners) {
            conversationManager?.let { listener() } ?: conversationManagerReadyListeners.add(listener)
        }
    }

    fun resetLastFocusedConversation() {
        lastFocusedConversationId = null
        lastFocusedConversationType = -1
    }

    override fun init() {
        val stealthMode = context.feature(StealthMode::class)
        context.classCache.conversationManager.hookConstructor(HookStage.BEFORE) { param ->
            synchronized(conversationManagerReadyListeners) {
                conversationManager = ConversationManager(context, param.thisObject())
                context.messagingBridge.triggerSessionStart()
                context.mainActivity?.takeIf { it.intent.getBooleanExtra(ReceiversConfig.MESSAGING_PREVIEW_EXTRA, false) }?.run {
                    startActivity(Intent().apply {
                        setComponent(ComponentName(Constants.SE_PACKAGE_NAME, "me.rhunk.snapenhance.ui.manager.MainActivity"))
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
                conversationManagerReadyListeners.removeIf { it(); true }
            }
        }

        context.classCache.snapManager.hookConstructor(HookStage.BEFORE) { param ->
            snapManager = param.thisObject()
        }

        context.mappings.useMapper(CallbackMapper::class) {
            callbacks.getClass("ConversationManagerDelegate")?.apply {
                hookConstructor(HookStage.AFTER) { param ->
                    conversationManagerDelegate = param.thisObject()
                }
                hook("onConversationUpdated", HookStage.BEFORE) { param ->
                    context.event.post(ConversationUpdateEvent(
                        conversationId = SnapUUID(param.arg(0)).toString(),
                        conversation = param.argNullable(1),
                        messages = param.arg<ArrayList<*>>(2).map { Message(it) },
                    ).apply { adapter = param }) {
                        param.setArg(
                            2,
                            messages.map { it.instanceNonNull() }.toCollection(ArrayList())
                        )
                    }
                }
            }
            callbacks.getClass("IdentityDelegate")?.apply {
                hookConstructor(HookStage.AFTER) {
                    identityDelegate = it.thisObject()
                }
            }
        }

        defer {
            arrayOf("activate", "deactivate", "processTypingActivity").forEach { hook ->
                context.classCache.presenceSession.hook(hook, HookStage.BEFORE, {
                    context.config.messaging.hideBitmojiPresence.get() || stealthMode.canUseRule(openedConversationUUID.toString())
                }) {
                    it.setResult(null)
                }
            }

            context.classCache.presenceSession.hook("startPeeking", HookStage.BEFORE, {
                context.config.messaging.hidePeekAPeek.get() || stealthMode.canUseRule(openedConversationUUID.toString())
            }) { it.setResult(null) }

            //get last opened snap for media downloader
            context.event.subscribe(OnSnapInteractionEvent::class) { event ->
                openedConversationUUID = event.conversationId
                lastFocusedMessageId = event.messageId
            }

            context.classCache.conversationManager.hook("fetchMessage", HookStage.BEFORE) { param ->
                val conversationId = SnapUUID(param.arg(0)).toString()
                if (openedConversationUUID?.toString() == conversationId) {
                    lastFocusedMessageId = param.arg(1)
                }
            }

            context.classCache.conversationManager.hook("sendTypingNotification", HookStage.BEFORE, {
                context.config.messaging.hideTypingNotifications.get() || stealthMode.canUseRule(openedConversationUUID.toString())
            }) {
                it.setResult(null)
            }
        }

        onNextActivityCreate {
            context.mappings.useMapper(FriendsFeedEventDispatcherMapper::class) {
                classReference.getAsClass()?.hook("onItemLongPress", HookStage.BEFORE) { param ->
                    val viewItemContainer = param.arg<Any>(0)
                    val viewItem = viewItemContainer.getObjectField(viewModelField.get()!!).toString()
                    val conversationId = viewItem.substringAfter("conversationId: ").substring(0, 36).also {
                        if (it.startsWith("null")) return@hook
                    }
                    lastFocusedConversationId = conversationId
                    lastFocusedConversationType = context.database.getConversationType(conversationId) ?: 0
                }
            }

            context.classCache.feedEntry.hookConstructor(HookStage.AFTER) { param ->
                val instance = param.thisObject<Any>()
                val interactionInfo = instance.getObjectFieldOrNull("mInteractionInfo") ?: return@hookConstructor
                val messages = (interactionInfo.getObjectFieldOrNull("mMessages") as? List<*>)?.map { Message(it) } ?: return@hookConstructor
                val conversationId = SnapUUID(instance.getObjectFieldOrNull("mConversationId") ?: return@hookConstructor).toString()
                val myUserId = context.database.myUserId

                feedCachedSnapMessages[conversationId] = messages.filter { msg ->
                    msg.messageMetadata?.openedBy?.none { it.toString() == myUserId } == true
                }.sortedBy { it.orderKey }.mapNotNull { it.messageDescriptor?.messageId }
            }

            context.classCache.conversationManager.apply {
                hook("enterConversation", HookStage.BEFORE) { param ->
                    openedConversationUUID = SnapUUID(param.arg(0))
                    if (context.config.messaging.bypassMessageRetentionPolicy.get()) {
                        val callback = param.argNullable<Any>(2) ?: return@hook
                        callback::class.java.methods.firstOrNull { it.name == "onSuccess" }?.invoke(callback)
                        param.setResult(null)
                    }
                }

                hook("exitConversation", HookStage.BEFORE) {
                    openedConversationUUID = null
                }
            }
        }
    }

    fun getFeedCachedMessageIds(conversationId: String) = feedCachedSnapMessages[conversationId]

    fun clearConversationFromFeed(conversationId: String, onError : (String) -> Unit = {}, onSuccess : () -> Unit = {}) {
        conversationManager?.clearConversation(conversationId, onError = { onError(it) }, onSuccess = {
            runCatching {
                conversationManagerDelegate!!.let {
                    it::class.java.methods.first { method ->
                        method.name == "onConversationRemoved"
                    }.invoke(conversationManagerDelegate, conversationId.toSnapUUID().instanceNonNull())
                }
                onSuccess()
            }.onFailure {
                context.log.error("Failed to invoke onConversationRemoved: $it")
                onError(it.message ?: "Unknown error")
            }
        })
    }

    fun localUpdateMessage(conversationId: String, message: Message, forceUpdate: Boolean = false) {
        if (forceUpdate) {
            message.messageMetadata?.screenRecordedBy = ArrayList<SnapUUID>(message.messageMetadata?.screenRecordedBy ?: emptyList()).apply {
                add(SnapUUID(UUID.randomUUID().toString()))
            }
        }
        conversationManagerDelegate?.let {
            it::class.java.methods.first { method ->
                method.name == "onConversationUpdated"
            }.invoke(conversationManagerDelegate, conversationId.toSnapUUID().instanceNonNull(), null, mutableListOf(message.instanceNonNull()), mutableListOf<Any>())
        }
    }

    fun fetchSnapchatterInfos(userIds: List<String>): List<Snapchatter> {
        val identity = identityDelegate ?: return emptyList()
        val snapUUIDs = userIds.map {
            it.toSnapUUID().instanceNonNull()
        }

        val future = identity::class.java.methods.first {
            it.name == "fetchSnapchatterInfos"
        }.let { method ->
            if (method.parameterCount == 2) method.invoke(identity, snapUUIDs, false)
            else method.invoke(identity, snapUUIDs)
        } as Future<*>

        return (future.get() as? List<*>)?.map { Snapchatter(it) } ?: return emptyList()
    }
}