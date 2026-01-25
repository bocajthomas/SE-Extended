package me.rhunk.snapenhance.core.ui.menu.impl

import android.content.DialogInterface
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.sharp.*
import androidx.compose.material.icons.automirrored.twotone.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.sharp.*
import androidx.compose.material.icons.twotone.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toDrawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rhunk.snapenhance.common.data.ContentType
import me.rhunk.snapenhance.common.data.FriendLinkType
import me.rhunk.snapenhance.common.database.impl.ConversationMessage
import me.rhunk.snapenhance.common.database.impl.FriendInfo
import me.rhunk.snapenhance.common.scripting.ui.EnumScriptInterface
import me.rhunk.snapenhance.common.scripting.ui.InterfaceManager
import me.rhunk.snapenhance.common.scripting.ui.ScriptInterface
import me.rhunk.snapenhance.common.ui.createComposeAlertDialog
import me.rhunk.snapenhance.common.ui.createComposeView
import me.rhunk.snapenhance.common.util.protobuf.ProtoReader
import me.rhunk.snapenhance.common.util.snap.BitmojiSelfie
import me.rhunk.snapenhance.core.event.events.impl.AddViewEvent
import me.rhunk.snapenhance.core.features.impl.experiments.EndToEndEncryption
import me.rhunk.snapenhance.core.features.impl.messaging.AutoMarkAsRead
import me.rhunk.snapenhance.core.features.impl.messaging.Messaging
import me.rhunk.snapenhance.core.features.impl.spying.MessageLogger
import me.rhunk.snapenhance.core.ui.ViewAppearanceHelper
import me.rhunk.snapenhance.core.ui.children
import me.rhunk.snapenhance.core.ui.menu.AbstractMenu
import me.rhunk.snapenhance.core.ui.triggerRootCloseTouchEvent
import me.rhunk.snapenhance.core.util.ktx.isDarkTheme
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
        return BitmapFactory.decodeStream(input).toDrawable(Resources.getSystem())
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
                        BitmojiSelfie.BitmojiSelfieType.NEW_THREE_D
                    )!!
                )
            }
        } catch (e: Throwable) {
            context.log.error("Error loading bitmoji selfie", e)
        }
        val finalIcon = icon
        val translation = context.translation.getCategory("profile_info")
        val firstCreatedUsername = context.database.getFriendOriginalUsername(profile.mutableUsername.toString()) ?: profile.firstCreatedUsername

        context.runOnUiThread {
            val addedTimestamp: Long = profile.addedTimestamp.coerceAtLeast(profile.reverseAddedTimestamp)
            val builder = ViewAppearanceHelper.newAlertDialogBuilder(context.mainActivity)
            builder.setIcon(finalIcon)
            builder.setTitle(profile.displayName ?: profile.username)

            val birthday = Calendar.getInstance()
            birthday[Calendar.MONTH] = (profile.birthday shr 32).toInt() - 1

            builder.setMessage(mapOf(
                translation["first_created_username"] to firstCreatedUsername,
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

    private suspend fun showConversationPreview(
        targetUser: String?,
        conversationId: String
    ) {
        val getIconStyle = context.config.userInterface.iconStyle.getNullable()
        val friendInfo = targetUser?.let { context.database.getFriendInfo(it) }
        val conversationInfo = conversationId.takeIf { targetUser == null }?.let { context.database.getFeedEntryByConversationId(it) }
        val participants by lazy {
            context.database.getConversationParticipants(conversationId)!!
                .map { context.database.getFriendInfo(it)!! }
                .associateBy { it.userId!! }
        }

        val personIconStyle = if (getIconStyle != null) {
            when (getIconStyle) {
                "outlined" -> Icons.Outlined.Person
                "filled" -> Icons.Filled.Person
                "sharp" -> Icons.Sharp.Person
                "two-tone" -> Icons.TwoTone.Person
                else -> {
                    context.log.warn("Error setting icon style $getIconStyle")
                    Icons.Rounded.Person
                }
            }
        } else {
            Icons.Rounded.Person
        }

        val localFireDepartmentIconStyle = if (getIconStyle != null) {
            when (getIconStyle) {
                "outlined" -> Icons.Outlined.LocalFireDepartment
                "filled" -> Icons.Filled.LocalFireDepartment
                "sharp" -> Icons.Sharp.LocalFireDepartment
                "two-tone" -> Icons.TwoTone.LocalFireDepartment
                else -> {
                    context.log.warn("Error setting icon style $getIconStyle")
                    Icons.Rounded.LocalFireDepartment
                }
            }
        } else {
            Icons.Rounded.LocalFireDepartment
        }

        val groupIconStyle = if (getIconStyle != null) {
            when (getIconStyle) {
                "outlined" -> Icons.Outlined.Group
                "filled" -> Icons.Filled.Group
                "sharp" -> Icons.Sharp.Group
                "two-tone" -> Icons.TwoTone.Group
                else -> {
                    context.log.warn("Error setting icon style $getIconStyle")
                    Icons.Rounded.Group
                }
            }
        } else {
            Icons.Rounded.Group
        }

        val messageIconStyle = if (getIconStyle != null) {
            when (getIconStyle) {
                "outlined" -> Icons.AutoMirrored.Outlined.Message
                "filled" -> Icons.AutoMirrored.Filled.Message

                "sharp" -> Icons.AutoMirrored.Sharp.Message
                "two-tone" -> Icons.AutoMirrored.TwoTone.Message
                else -> {
                    context.log.warn("Error setting icon style $getIconStyle")
                    Icons.AutoMirrored.Rounded.Message
                }
            }
        } else {
            Icons.AutoMirrored.Rounded.Message
        }

        val moreVertIconStyle = if (getIconStyle != null) {
            when (getIconStyle) {
                "outlined" -> Icons.Outlined.MoreVert
                "filled" -> Icons.Filled.MoreVert
                "sharp" -> Icons.Sharp.MoreVert
                "two-tone" -> Icons.TwoTone.MoreVert
                else -> {
                    context.log.warn("Error setting icon style $getIconStyle")
                    Icons.Rounded.MoreVert
                }
            }
        } else {
            Icons.Rounded.MoreVert
        }

        withContext(Dispatchers.Main) {
            createComposeAlertDialog(
                context.mainActivity!!,
            ) {
                var pageIndex by remember { mutableIntStateOf(0) }
                val messages = remember { mutableStateListOf<@Composable () -> Unit>() }
                var totalMessages by remember { mutableIntStateOf(-1) }
                val coroutineScope = rememberCoroutineScope()

                suspend fun loadMore() {
                    val conversationMessages = context.database.getMessagesFromConversationId(
                        conversationId,
                        50,
                        page = pageIndex++
                    ) ?: emptyList()

                    if (totalMessages == -1) {
                        totalMessages = conversationMessages.firstOrNull()?.serverMessageId ?: 0
                    }

                    val messageLogger = context.feature(MessageLogger::class)
                    val endToEndEncryption = context.feature(EndToEndEncryption::class)

                    val parsedMessages = conversationMessages.mapNotNull<ConversationMessage, @Composable () -> Unit> { message ->
                        val sender = participants[message.senderId]
                        val messageProtoReader =
                            (messageLogger.takeIf { it.isEnabled && message.contentType == ContentType.STATUS.id }?.getMessageProto(conversationId, message.clientMessageId.toLong()) // process deleted messages if message logger is enabled
                                ?: ProtoReader(message.messageContent!!).followPath(4, 4) // database message
                            )?.let {
                            if (endToEndEncryption.isEnabled) endToEndEncryption.decryptDatabaseMessage(message) else it // try to decrypt message if e2ee is enabled
                        } ?: return@mapNotNull null

                        val contentType = ContentType.fromMessageContainer(messageProtoReader) ?: ContentType.fromId(message.contentType)
                        var messageString = if (contentType == ContentType.CHAT) {
                            messageProtoReader.getString(2, 1) ?: return@mapNotNull null
                        } else "[${context.translation.getOrNull("content_type.${contentType.name}") ?: contentType.name}]"

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

                        {
                            Text(
                                text = "$displayUsername: $messageString",
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }

                    withContext(Dispatchers.Main) {
                        messages.addAll(parsedMessages)
                    }
                }

                Column(
                    modifier = Modifier.fillMaxHeight(fraction = 0.85f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        @Composable
                        fun Entry(icon: ImageVector, text: String?, title: Boolean) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(icon, contentDescription = null)
                                Text(
                                    text = text ?: "",
                                    fontWeight = if (title) FontWeight.Bold else FontWeight.Light,
                                    fontSize = if (title) 16.sp else 14.sp
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                        ) {
                            friendInfo?.let { friendInfo ->
                                Entry(personIconStyle, friendInfo.displayName?.let { "$it (${friendInfo.usernameForSorting})" } ?: friendInfo.usernameForSorting, true)
                                friendInfo.streakExpirationTimestamp.takeIf { it > 0L && friendInfo.streakLength > 0 && System.currentTimeMillis() < it }?.let { timestamp ->
                                    Entry(localFireDepartmentIconStyle, context.translation.format("conversation_preview.streak_expiration",
                                        "day" to ((timestamp - System.currentTimeMillis()) / 1000 / 60 / 60 / 24).toString(),
                                        "hour" to ((timestamp - System.currentTimeMillis()) / 1000 / 60 / 60 % 24).toString(),
                                        "minute" to ((timestamp - System.currentTimeMillis()) / 1000 / 60 % 60).toString()
                                    ), false)
                                }
                            }
                            conversationInfo?.let {
                                Entry(groupIconStyle, (it.feedDisplayName ?: it.key).toString(), true)
                            }
                            Entry(messageIconStyle, context.translation.format("conversation_preview.total_messages", "count" to totalMessages.toString()), false)
                        }
                        friendInfo?.let {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch(Dispatchers.IO) { showProfileInfo(it) }
                                }
                            ) {
                                Icon(moreVertIconStyle, contentDescription = null)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
                    LazyColumn(
                        contentPadding = PaddingValues(8.dp),
                        reverseLayout = true
                    ) {
                        items(messages) { message ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                message()
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            LaunchedEffect(Unit) {
                                withContext(Dispatchers.IO) {
                                    loadMore()
                                }
                            }
                            if (messages.isEmpty()) {
                                Text(
                                    text = context.translation["conversation_preview.no_messages"],
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }.show()
        }
    }

    @Composable
    private fun MenuElement(
        index: Int,
        icon: ImageVector,
        text: String,
        onClick: () -> Unit,
        onLongClick: (() -> Unit)? = null,
        content: @Composable RowScope.() -> Unit = {}
    ) {
        if (index > 0) {
            Spacer(modifier = Modifier
                .height(1.dp)
                .background(remember {
                    if (context.androidContext.isDarkTheme()) Color(0x1affffff) else Color(
                        0xffeeeeee
                    )
                })
                .fillMaxWidth())
        }
        Surface(
            color = Color(context.userInterface.actionSheetBackground),
            contentColor = Color(context.userInterface.colorPrimary),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                onLongClick?.invoke()
                            },
                            onTap = {
                                onClick()
                            }
                        )
                    }
                    .heightIn(min = 55.dp)
                    .padding(start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier
                    .size(32.dp)
                    .padding(end = 8.dp))
                Text(
                    text = text,
                    modifier = Modifier.weight(1f),
                    lineHeight = 18.sp,
                    fontSize = 16.sp,
                )
                content()
            }
        }
    }

    private val messaging by lazy { context.feature(Messaging::class)}

    override fun onViewAdded(event: AddViewEvent) {
        fun hasAvatarHeader(viewGroup: ViewGroup): Boolean {
            val constraintLayout = viewGroup.getChildAt(0)?.takeIf { it.javaClass.name.endsWith("ConstraintLayout") } as? ViewGroup ?: return false
            return constraintLayout.children().firstOrNull { it.javaClass.name.endsWith("AvatarView") } != null
        }

        if (messaging.lastFocusedConversationType == 1 && event.view.javaClass.name.endsWith("ConstraintLayout") && event.parent.javaClass.name.endsWith("RecyclerView")) {
            val linearLayout = LinearLayout(event.view.context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            injectIntoActionSheetItems(linearLayout, isGroupMenu = true) {
                linearLayout.addView(it, 0)
            }

            (event.view as ViewGroup).addView(linearLayout, 0)
        }

        if (event.parent is LinearLayout && event.viewClassName.endsWith("SnapCardView") && hasAvatarHeader(event.parent)) {
            val actionSheetItemsContainerLayout = (event.view as ViewGroup).getChildAt(0) as? ViewGroup ?: throw IllegalStateException("ActionSheetItemsContainerLayout not found")
            injectIntoActionSheetItems(actionSheetItemsContainerLayout, isGroupMenu = false) {
                actionSheetItemsContainerLayout.addView(it, 0)
            }
        }
    }

    private fun injectIntoActionSheetItems(contextView: View, isGroupMenu: Boolean = false, viewConsumer: ((View) -> Unit)) {
        val friendFeedMenuOptions by context.config.userInterface.friendFeedMenuButtons
        val getIconStyle = context.config.userInterface.iconStyle.getNullable()
        if (friendFeedMenuOptions.isEmpty()) return

        val removeRedEyeIconStyle = if (getIconStyle != null) {
            when (getIconStyle) {
                "outlined" -> Icons.Outlined.RemoveRedEye
                "filled" -> Icons.Filled.RemoveRedEye
                "sharp" -> Icons.Sharp.RemoveRedEye
                "two-tone" -> Icons.TwoTone.RemoveRedEye
                else -> {
                    context.log.warn("Error setting icon style $getIconStyle")
                    Icons.Rounded.RemoveRedEye
                }
            }
        } else {
            Icons.Rounded.RemoveRedEye
        }

        val checkCircleOutlineIconStyle = if (getIconStyle != null) {
            when (getIconStyle) {
                "outlined" -> Icons.Outlined.CheckCircleOutline
                "filled" -> Icons.Filled.CheckCircleOutline
                "sharp" -> Icons.Sharp.CheckCircleOutline
                "two-tone" -> Icons.TwoTone.CheckCircleOutline
                else -> {
                    context.log.warn("Error setting icon style $getIconStyle")
                    Icons.Rounded.CheckCircleOutline
                }
            }
        } else {
            Icons.Rounded.CheckCircleOutline
        }

        val notInterestedIconStyle = if (getIconStyle != null) {
            when (getIconStyle) {
                "outlined" -> Icons.Outlined.NotInterested
                "filled" -> Icons.Filled.NotInterested
                "sharp" -> Icons.Sharp.NotInterested
                "two-tone" -> Icons.TwoTone.NotInterested
                else -> {
                    context.log.warn("Error setting icon style $getIconStyle")
                    Icons.Rounded.NotInterested
                }
            }
        } else {
            Icons.Rounded.NotInterested
        }

        val editNoteIconStyle = if (getIconStyle != null) {
            when (getIconStyle) {
                "outlined" -> Icons.Outlined.EditNote
                "filled" -> Icons.Filled.EditNote
                "sharp" -> Icons.Sharp.EditNote
                "two-tone" -> Icons.TwoTone.EditNote
                else -> {
                    context.log.warn("Error setting icon style $getIconStyle")
                    Icons.Rounded.EditNote
                }
            }
        } else {
            Icons.Rounded.EditNote
        }

        val infoIconStyle = if (getIconStyle != null) {
            when (getIconStyle) {
                "outlined" -> Icons.Outlined.Info
                "filled" -> Icons.Filled.Info
                "sharp" -> Icons.Sharp.Info
                "two-tone" -> Icons.TwoTone.Info
                else -> {
                    context.log.warn("Error setting icon style $getIconStyle")
                    Icons.Rounded.Info
                }
            }
        } else {
            Icons.Rounded.Info
        }

        val messaging = context.feature(Messaging::class)
        val conversationId = messaging.lastFocusedConversationId ?: return
        val targetUser by lazy { context.database.getDMOtherParticipant(conversationId) }
        messaging.resetLastFocusedConversation()

        val translation = context.translation.getCategory("friend_menu_option")

        fun closeMenu() {
            if (!context.config.userInterface.autoCloseFriendFeedMenu.get()) return
            context.mainActivity?.triggerRootCloseTouchEvent()
        }

        @Composable
        fun ComposeFriendFeedMenu() {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                var elementIndex by remember { mutableIntStateOf(if (isGroupMenu) 1 else 0) }

                if (friendFeedMenuOptions.contains("conversation_info")) {
                    MenuElement(
                        remember { elementIndex++ },
                        removeRedEyeIconStyle,
                        translation["preview"],
                        onClick = {
                            context.coroutineScope.launch {
                                showConversationPreview(targetUser, conversationId)
                            }
                        }
                    )
                }

                context.features.getRuleFeatures().forEach { ruleFeature ->
                    if (!friendFeedMenuOptions.contains(ruleFeature.ruleType.key)) return@forEach

                    val ruleState = ruleFeature.getRuleState() ?: return@forEach
                    var state by remember { mutableStateOf(ruleFeature.getState(conversationId)) }

                    fun toggle() {
                        state = !ruleFeature.getState(conversationId)
                        ruleFeature.setState(conversationId, state)
                        context.inAppOverlay.showStatusToast(
                            if (state) checkCircleOutlineIconStyle else notInterestedIconStyle,
                            context.translation.format("rules.toasts.${if (state) "enabled" else "disabled"}", "ruleName" to context.translation[ruleFeature.ruleType.translateOptionKey(ruleState.key)]),
                            durationMs = 1500
                        )
                        closeMenu()
                    }

                    MenuElement(
                        remember { elementIndex++ },
                        icon = ruleFeature.ruleType.icon,
                        text = context.translation[ruleFeature.ruleType.translateOptionKey(ruleState.key)],
                        onClick = {
                            toggle()
                        }
                    ) {
                        Switch(
                            checked = state,
                            onCheckedChange = {
                                state = it
                                toggle()
                            }
                        )
                    }
                }

                if (friendFeedMenuOptions.contains("mark_snaps_as_seen")) {
                    MenuElement(
                        remember { elementIndex++ },
                        editNoteIconStyle,
                        translation["mark_snaps_as_seen"],
                        onClick = {
                            context.apply {
                                closeMenu()
                                feature(AutoMarkAsRead::class).markSnapsAsSeen(conversationId)
                            }
                        }
                    )
                }

                if (targetUser != null && friendFeedMenuOptions.contains("mark_stories_as_seen_locally")) {
                    val markAsSeenTranslation = remember { context.translation.getCategory("mark_as_seen") }

                    MenuElement(
                        remember { elementIndex++ },
                        removeRedEyeIconStyle,
                        translation["mark_stories_as_seen_locally"],
                        onClick = {
                            context.apply {
                                closeMenu()
                                inAppOverlay.showStatusToast(
                                    infoIconStyle,
                                    if (database.setStoriesViewedState(targetUser!!, true)) markAsSeenTranslation["seen_toast"]
                                    else markAsSeenTranslation["already_seen_toast"],
                                    durationMs = 2500
                                )
                            }
                        },
                        onLongClick = {
                            contextView.post {
                                context.apply {
                                    closeMenu()
                                    inAppOverlay.showStatusToast(
                                        infoIconStyle,
                                        if (database.setStoriesViewedState(targetUser!!, false)) markAsSeenTranslation["unseen_toast"]
                                        else markAsSeenTranslation["already_unseen_toast"],
                                        durationMs = 2500
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }

        viewConsumer(
            createComposeView(contextView.context) {
                CompositionLocalProvider(
                    LocalTextStyle provides LocalTextStyle.current.merge(TextStyle(fontFamily = FontFamily(
                        Font(context.userInterface.avenirNextFontId, FontWeight.Medium)
                    )))
                ) {
                    ComposeFriendFeedMenu()
                }
            }.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        )

        if (context.config.scripting.integratedUI.get()) {
            context.scriptRuntime.eachModule {
                val interfaceManager = getBinding(InterfaceManager::class)
                    ?.takeIf {
                        it.hasInterface(EnumScriptInterface.FRIEND_FEED_CONTEXT_MENU)
                    } ?: return@eachModule

                viewConsumer(LinearLayout(contextView.context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )

                    orientation = LinearLayout.VERTICAL
                    addView(createComposeView(contextView.context) {
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

        if (isGroupMenu) {
            viewConsumer(
                createComposeView(contextView.context) {
                    Spacer(modifier = Modifier.height(64.dp))
                }
            )
        }
    }
}