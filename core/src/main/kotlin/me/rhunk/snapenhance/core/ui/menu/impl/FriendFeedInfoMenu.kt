package me.rhunk.snapenhance.core.ui.menu.impl

import android.content.DialogInterface
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotInterested
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import me.rhunk.snapenhance.common.data.ContentType
import me.rhunk.snapenhance.common.data.FriendLinkType
import me.rhunk.snapenhance.common.database.impl.ConversationMessage
import me.rhunk.snapenhance.common.database.impl.FriendInfo
import me.rhunk.snapenhance.common.scripting.ui.EnumScriptInterface
import me.rhunk.snapenhance.common.scripting.ui.InterfaceManager
import me.rhunk.snapenhance.common.scripting.ui.ScriptInterface
import me.rhunk.snapenhance.common.ui.createComposeView
import me.rhunk.snapenhance.common.util.protobuf.ProtoReader
import me.rhunk.snapenhance.common.util.snap.BitmojiSelfie
import me.rhunk.snapenhance.core.features.impl.experiments.EndToEndEncryption
import me.rhunk.snapenhance.core.features.impl.messaging.AutoMarkAsRead
import me.rhunk.snapenhance.core.features.impl.messaging.Messaging
import me.rhunk.snapenhance.core.features.impl.spying.MessageLogger
import me.rhunk.snapenhance.core.ui.ViewAppearanceHelper
import me.rhunk.snapenhance.core.ui.applyTheme
import me.rhunk.snapenhance.core.ui.menu.AbstractMenu
import me.rhunk.snapenhance.core.ui.triggerRootCloseTouchEvent
import me.rhunk.snapenhance.core.util.ktx.vibrateLongPress
import java.net.HttpURLConnection
import java.net.URL
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FriendFeedInfoMenu : AbstractMenu() {
    private fun getImageDrawable(url: String): Drawable {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connect()
        val input = connection.inputStream
        return BitmapDrawable(Resources.getSystem(), BitmapFactory.decodeStream(input))
    }

    private fun formatDate(timestamp: Long): String? {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(Date(timestamp))
    }

    private fun showProfileInfo(profile: FriendInfo) {
        var icon: Drawable? = null
        try {
            if (profile.bitmojiSelfieId != null && profile.bitmojiAvatarId != null) {
                icon = getImageDrawable(
                    BitmojiSelfie.getBitmojiSelfie(
                        profile.bitmojiSelfieId.toString(),
                        profile.bitmojiAvatarId.toString(),
                        BitmojiSelfie.BitmojiSelfieType.THREE_D
                    )!!
                )
            }
        } catch (e: Throwable) {
            context.log.error("Error loading bitmoji selfie", e)
        }
        val finalIcon = icon
        val translation = context.translation.getCategory("profile_info")

        context.runOnUiThread {
            val addedTimestamp: Long = profile.addedTimestamp.coerceAtLeast(profile.reverseAddedTimestamp)
            val builder = ViewAppearanceHelper.newAlertDialogBuilder(context.mainActivity)
            builder.setIcon(finalIcon)
            builder.setTitle(profile.displayName ?: profile.username)

            val birthday = Calendar.getInstance()
            birthday[Calendar.MONTH] = (profile.birthday shr 32).toInt() - 1

            builder.setMessage(mapOf(
                translation["first_created_username"] to profile.firstCreatedUsername,
                translation["mutable_username"] to profile.mutableUsername,
                translation["display_name"] to profile.displayName,
                translation["added_date"] to formatDate(addedTimestamp).takeIf { addedTimestamp > 0 },
                null to birthday.getDisplayName(
                    Calendar.MONTH,
                    Calendar.LONG,
                    context.translation.loadedLocale
                )?.let {
                    if (profile.birthday == 0L) context.translation["profile_info.hidden_birthday"]
                    else context.translation.format("profile_info.birthday",
                        "month" to it,
                        "day" to profile.birthday.toInt().toString())
                },
                translation["friendship"] to run {
                    context.translation["friendship_link_type.${FriendLinkType.fromValue(profile.friendLinkType).shortName}"]
                }.takeIf {
                    if (profile.friendLinkType == FriendLinkType.MUTUAL.value) addedTimestamp.toInt() > 0 else true
                },
                translation["add_source"] to context.database.getAddSource(profile.userId!!)?.takeIf { it.isNotEmpty() },
                translation["snapchat_plus"] to run {
                    translation.getCategory("snapchat_plus_state")[if (profile.postViewEmoji != null) "subscribed" else "not_subscribed"]
                }
            ).filterValues { it != null }.map {
                line -> "${line.key?.let { "$it: " } ?: ""}${line.value}"
            }.joinToString("\n"))

            builder.setPositiveButton(
                "OK"
            ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
            builder.show()
        }
    }

    private fun showPreview(userId: String?, conversationId: String) {
        //query message
        val messageLogger = context.feature(MessageLogger::class)
        val endToEndEncryption = context.feature(EndToEndEncryption::class)
        val messages: List<ConversationMessage> = context.database.getMessagesFromConversationId(
            conversationId,
            context.config.messaging.messagePreviewLength.get()
        )?.reversed() ?: emptyList()

        val participants: Map<String, FriendInfo> = context.database.getConversationParticipants(conversationId)!!
            .map { context.database.getFriendInfo(it)!! }
            .associateBy { it.userId!! }
        
        val messageBuilder = StringBuilder()
        val translation = context.translation.getCategory("content_type")

        messages.forEach { message ->
            val sender = participants[message.senderId]
            val messageProtoReader =
                (
                    messageLogger.takeIf { it.isEnabled && message.contentType == ContentType.STATUS.id }?.getMessageProto(conversationId, message.clientMessageId.toLong()) // process deleted messages if message logger is enabled
                    ?: ProtoReader(message.messageContent!!).followPath(4, 4) // database message
                )?.let {
                    if (endToEndEncryption.isEnabled) endToEndEncryption.decryptDatabaseMessage(message) else it // try to decrypt message if e2ee is enabled
                } ?: return@forEach

            val contentType = ContentType.fromMessageContainer(messageProtoReader) ?: ContentType.fromId(message.contentType)
            var messageString = if (contentType == ContentType.CHAT) {
                messageProtoReader.getString(2, 1) ?: return@forEach
            } else translation.getOrNull(contentType.name) ?: contentType.name

            if (contentType == ContentType.SNAP) {
                messageString = "\uD83D\uDFE5" //red square
                if (message.readTimestamp > 0) {
                    messageString += " \uD83D\uDC40 " //eyes
                    messageString += DateFormat.getDateTimeInstance(
                        DateFormat.SHORT,
                        DateFormat.SHORT
                    ).format(Date(message.readTimestamp))
                }
            }

            var displayUsername = sender?.displayName ?: sender?.usernameForSorting?: context.translation["conversation_preview.unknown_user"]

            if (displayUsername.length > 12) {
                displayUsername = displayUsername.substring(0, 13) + "... "
            }

            messageBuilder.append(displayUsername).append(": ").append(messageString).append("\n")
        }

        val targetPerson = if (userId == null) null else participants[userId]

        targetPerson?.streakExpirationTimestamp?.takeIf { it > 0 }?.let {
            val timeSecondDiff = ((it - System.currentTimeMillis()) / 1000 / 60).toInt()
            if (timeSecondDiff <= 0) return@let
            messageBuilder.append("\n")
                .append("\uD83D\uDD25 ") //fire emoji
                .append(
                    context.translation.format("conversation_preview.streak_expiration",
                    "day" to (timeSecondDiff / 60 / 24).toString(),
                    "hour" to (timeSecondDiff / 60 % 24).toString(),
                    "minute" to (timeSecondDiff % 60).toString()
                ))
        }

        messages.lastOrNull()?.let {
            messageBuilder
                .append("\n\n")
                .append(context.translation.format("conversation_preview.total_messages", "count" to it.serverMessageId.toString()))
                .append("\n")
        }

        //alert dialog
        val builder = ViewAppearanceHelper.newAlertDialogBuilder(context.mainActivity)
        builder.setTitle(context.translation["conversation_preview.title"])
        builder.setMessage(messageBuilder.toString())
        builder.setPositiveButton(
            "OK"
        ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
        targetPerson?.let {
            builder.setNegativeButton(context.translation["modal_option.profile_info"]) { _, _ ->
                context.executeAsync { showProfileInfo(it) }
            }
        }
        builder.show()
    }

    private fun createToggleFeature(viewConsumer: ((View) -> Unit), value: String, checked: () -> Boolean, toggle: (Boolean) -> Unit) {
        viewConsumer(Switch(context.androidContext).apply {
            text = this@FriendFeedInfoMenu.context.translation[value]
            isChecked = checked()
            applyTheme(hasRadius = true)
            isSoundEffectsEnabled = false
            setOnCheckedChangeListener { _, checked ->
                toggle(checked)
            }
        })
    }

    override fun inject(parent: ViewGroup, view: View, viewConsumer: ((View) -> Unit)) {
        val modContext = context

        val friendFeedMenuOptions by context.config.userInterface.friendFeedMenuButtons
        if (friendFeedMenuOptions.isEmpty()) return

        val messaging = context.feature(Messaging::class)
        val conversationId = messaging.lastFocusedConversationId ?: run {
            context.shortToast("No conversation focused!")
            return
        }
        val targetUser = context.database.getDMOtherParticipant(conversationId)
        messaging.resetLastFocusedConversation()

        val translation = context.translation.getCategory("friend_menu_option")
        if (friendFeedMenuOptions.contains("conversation_info")) {
            viewConsumer(Button(view.context).apply {
                text = translation["preview"]
                applyTheme(view.width, hasRadius = true)
                setOnClickListener {
                    showPreview(
                        targetUser,
                        conversationId
                    )
                }
            })
        }

        modContext.features.getRuleFeatures().forEach { ruleFeature ->
            if (!friendFeedMenuOptions.contains(ruleFeature.ruleType.key)) return@forEach

            val ruleState = ruleFeature.getRuleState() ?: return@forEach
            createToggleFeature(viewConsumer,
                ruleFeature.ruleType.translateOptionKey(ruleState.key),
                { ruleFeature.getState(conversationId) },
                {
                    ruleFeature.setState(conversationId, it)
                    context.inAppOverlay.showStatusToast(
                        if (it) Icons.Default.CheckCircleOutline else Icons.Default.NotInterested,
                        context.translation.format("rules.toasts.${if (it) "enabled" else "disabled"}", "ruleName" to context.translation[ruleFeature.ruleType.translateOptionKey(ruleState.key)]),
                        durationMs = 1500
                    )
                    context.mainActivity?.triggerRootCloseTouchEvent()
                }
            )
        }

        if (friendFeedMenuOptions.contains("mark_snaps_as_seen")) {
            viewConsumer(Button(view.context).apply {
                text = translation["mark_snaps_as_seen"]
                isSoundEffectsEnabled = false
                applyTheme(view.width, hasRadius = true)
                setOnClickListener {
                    this@FriendFeedInfoMenu.context.apply {
                        mainActivity?.triggerRootCloseTouchEvent()
                        feature(AutoMarkAsRead::class).markSnapsAsSeen(conversationId)
                    }
                }
            })
        }

        if (targetUser != null && friendFeedMenuOptions.contains("mark_stories_as_seen_locally")) {
            viewConsumer(Button(view.context).apply {
                text = translation["mark_stories_as_seen_locally"]
                applyTheme(view.width, hasRadius = true)
                isSoundEffectsEnabled = false

                val translations = this@FriendFeedInfoMenu.context.translation.getCategory("mark_as_seen")

                this@FriendFeedInfoMenu.context.apply {
                    setOnClickListener {
                        mainActivity?.triggerRootCloseTouchEvent()
                        this@FriendFeedInfoMenu.context.inAppOverlay.showStatusToast(
                            Icons.Default.Info,
                            if (database.setStoriesViewedState(targetUser, true)) translations["seen_toast"]
                            else translations["already_seen_toast"],
                            durationMs = 2500
                        )
                    }
                    setOnLongClickListener {
                        context.vibrateLongPress()
                        mainActivity?.triggerRootCloseTouchEvent()
                        this@FriendFeedInfoMenu.context.inAppOverlay.showStatusToast(
                            Icons.Default.Info,
                            if (database.setStoriesViewedState(targetUser, false)) translations["unseen_toast"]
                            else translations["already_unseen_toast"],
                            durationMs = 2500
                        )
                        true
                    }
                }
            })
        }

        if (context.config.scripting.integratedUI.get()) {
            context.scriptRuntime.eachModule {
                val interfaceManager = getBinding(InterfaceManager::class)
                    ?.takeIf {
                        it.hasInterface(EnumScriptInterface.FRIEND_FEED_CONTEXT_MENU)
                    } ?: return@eachModule

                viewConsumer(LinearLayout(view.context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )

                    applyTheme(view.width, hasRadius = true)

                    orientation = LinearLayout.VERTICAL
                    addView(createComposeView(view.context) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            ScriptInterface(interfaceBuilder = remember {
                                interfaceManager.buildInterface(EnumScriptInterface.FRIEND_FEED_CONTEXT_MENU, mapOf(
                                    "conversationId" to conversationId,
                                    "userId" to targetUser
                                ))
                            } ?: return@Surface)
                        }
                    })
                })
            }
        }
    }
}