package me.rhunk.snapenhance.core.action.impl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rhunk.snapenhance.common.data.ContentType
import me.rhunk.snapenhance.common.data.FriendLinkType
import me.rhunk.snapenhance.common.database.impl.ConversationMessage
import me.rhunk.snapenhance.common.database.impl.FriendInfo
import me.rhunk.snapenhance.common.messaging.MessagingConstraints
import me.rhunk.snapenhance.common.messaging.MessagingTask
import me.rhunk.snapenhance.common.messaging.MessagingTaskType
import me.rhunk.snapenhance.common.ui.createComposeAlertDialog
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableState
import me.rhunk.snapenhance.common.util.ktx.copyToClipboard
import me.rhunk.snapenhance.common.util.snap.BitmojiSelfie
import me.rhunk.snapenhance.common.util.snap.RemoteMediaResolver
import me.rhunk.snapenhance.core.action.AbstractAction
import me.rhunk.snapenhance.core.features.impl.experiments.AddFriendSourceSpoof
import me.rhunk.snapenhance.core.features.impl.experiments.BetterLocation
import me.rhunk.snapenhance.core.features.impl.messaging.Messaging
import me.rhunk.snapenhance.core.ui.ViewAppearanceHelper
import me.rhunk.snapenhance.core.util.EvictingMap
import me.rhunk.snapenhance.core.util.dataBuilder
import me.rhunk.snapenhance.mapper.impl.FriendRelationshipChangerMapper
import java.net.URL
import java.text.DateFormat
import java.util.Date
import kotlin.random.Random

class BulkMessagingAction : AbstractAction() {
    enum class SortBy {
        NONE,
        USERNAME,
        ADDED_TIMESTAMP,
        SNAP_SCORE,
        STREAK_LENGTH,
        MOST_MESSAGES_SENT,
        MOST_RECENT_MESSAGE,
        NEAREST_LOCATION
    }

    enum class Filter {
        ALL,
        MY_FRIENDS,
        BLOCKED,
        REMOVED_ME,
        DELETED,
        SUGGESTED,
        BUSINESS_ACCOUNTS,
        STREAKS,
        NON_STREAKS,
        LOCATION_ON_MAP
    }

    private val translation by lazy { context.translation.getCategory("bulk_messaging_action") }
    private val betterLocation by lazy { context.feature(BetterLocation::class) }

    private fun removeAction(
        ctx: Context,
        ids: List<String>,
        delay: Pair<Long, Long>,
        action: suspend (id: String, setDialogMessage: (String) -> Unit) -> Unit = { _, _ -> }
    ) = context.coroutineScope.launch {
        val statusTextView = TextView(ctx)
        val dialog = withContext(Dispatchers.Main) {
            ViewAppearanceHelper.newAlertDialogBuilder(ctx)
                .setTitle("...")
                .setView(LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    addView(statusTextView.apply {
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        textAlignment = View.TEXT_ALIGNMENT_CENTER
                    })
                    addView(ProgressBar(ctx))
                })
                .setCancelable(false)
                .show()
        }

        ids.forEachIndexed { index, id ->
            launch(Dispatchers.Main) {
                dialog.setTitle(
                    translation.format("progress_status", "index" to (index + 1).toString(), "total" to ids.size.toString())
                )
            }
            runCatching {
                action(id) {
                    launch(Dispatchers.Main) {
                        statusTextView.text = it
                    }
                }
            }.onFailure {
                context.log.error("Failed to process $it", it)
                context.shortToast("Failed to process $id")
            }
            delay(Random.nextLong(delay.first, delay.second))
        }
        withContext(Dispatchers.Main) {
            dialog.dismiss()
        }
    }

    @Composable
    private fun ConfirmationDialog(
        onConfirm: () -> Unit,
        onCancel: () -> Unit,
    ) {
        AlertDialog(
            onDismissRequest = onCancel,
            title = { Text(text = translation["confirmation_dialog.title"]) },
            text = { Text(text = translation["confirmation_dialog.message"]) },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(text = context.translation["button.positive"])
                }
            },
            dismissButton = {
                TextButton(onClick = onCancel) {
                    Text(text = context.translation["button.negative"])
                }
            }
        )
    }

    private fun filterFriends(friends: List<FriendInfo>, filter: Filter, nameFilter: String): List<FriendInfo> {
        val userIdBlacklist = arrayOf(
            context.database.myUserId,
            "b42f1f70-5a8b-4c53-8c25-34e7ec9e6781", // myai
            "84ee8839-3911-492d-8b94-72dd80f3713a", // teamsnapchat
        )

        return friends.filter { friend ->
            friend.userId !in userIdBlacklist && when (filter) {
                Filter.ALL -> true
                Filter.MY_FRIENDS -> friend.friendLinkType == FriendLinkType.MUTUAL.value && friend.addedTimestamp > 0
                Filter.BLOCKED -> friend.friendLinkType == FriendLinkType.BLOCKED.value
                Filter.REMOVED_ME -> friend.friendLinkType == FriendLinkType.OUTGOING.value && friend.addedTimestamp > 0 && friend.businessCategory == 0 // ignore followed accounts
                Filter.SUGGESTED -> friend.friendLinkType == FriendLinkType.SUGGESTED.value
                Filter.DELETED -> friend.friendLinkType == FriendLinkType.DELETED.value
                Filter.BUSINESS_ACCOUNTS -> friend.businessCategory > 0
                Filter.STREAKS -> friend.friendLinkType == FriendLinkType.MUTUAL.value && friend.addedTimestamp > 0 && friend.streakLength != 0
                Filter.NON_STREAKS -> friend.friendLinkType == FriendLinkType.MUTUAL.value&& friend.addedTimestamp > 0 && friend.streakLength == 0
                Filter.LOCATION_ON_MAP -> betterLocation.locationHistory.contains(friend.userId)
            } && nameFilter.takeIf { it.isNotBlank() }?.let { name ->
                friend.mutableUsername?.contains(
                    name,
                    ignoreCase = true
                ) == true || friend.displayName?.contains(name, ignoreCase = true) == true
            } ?: true
        }
    }

    private fun getDMLastMessage(userId: String?): ConversationMessage? {
        return context.database.getConversationLinkFromUserId(userId ?: return null)?.clientConversationId?.let {
            context.database.getMessagesFromConversationId(it, 1)
        }?.firstOrNull()
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    @Composable
    private fun BulkMessagingDialog() {
        val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
        var sortBy by remember { mutableStateOf(SortBy.USERNAME) }
        var filter by remember { mutableStateOf(Filter.REMOVED_ME) }
        var sortReverseOrder by remember { mutableStateOf(false) }
        val selectedFriends = remember { mutableStateListOf<String>() }
        val friends = remember { mutableStateListOf<FriendInfo>() }
        val bitmojiCache = remember { EvictingMap<String, Bitmap>(50) }
        val noBitmojiBitmap = remember { BitmapFactory.decodeResource(context.resources, android.R.drawable.ic_menu_report_image).asImageBitmap() }

        val focusManager = LocalFocusManager.current
        var nameFilter by remember { mutableStateOf("") }

        suspend fun refreshList(clearSelected: Boolean = true) {
            val myLocation = betterLocation.locationHistory[context.database.myUserId]

            withContext(Dispatchers.IO) {
                val newFriends = context.database.getAllFriends().let { friends ->
                    filterFriends(friends, filter, nameFilter)
                }.toMutableList()
                when (sortBy) {
                    SortBy.NONE -> {}
                    SortBy.USERNAME -> newFriends.sortBy { it.mutableUsername }
                    SortBy.ADDED_TIMESTAMP -> newFriends.sortBy { it.addedTimestamp }
                    SortBy.SNAP_SCORE -> newFriends.sortBy { it.snapScore }
                    SortBy.STREAK_LENGTH -> newFriends.sortBy { it.streakLength }
                    SortBy.MOST_MESSAGES_SENT -> newFriends.sortByDescending {
                        getDMLastMessage(it.userId)?.serverMessageId ?: 0
                    }
                    SortBy.MOST_RECENT_MESSAGE -> newFriends.sortByDescending {
                        getDMLastMessage(it.userId)?.creationTimestamp
                    }
                    SortBy.NEAREST_LOCATION -> {
                        if (myLocation != null) {
                            newFriends.sortBy {
                                betterLocation.locationHistory[it.userId]?.distanceTo(myLocation)
                                    ?: Double.MAX_VALUE
                            }
                        }
                    }
                }
                if (sortReverseOrder) newFriends.reverse()
                withContext(Dispatchers.Main) {
                    if (clearSelected) selectedFriends.clear()
                    friends.clear()
                    friends.addAll(newFriends)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                var filterMenuExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = filterMenuExpanded,
                    onExpandedChange = { filterMenuExpanded = it },
                ) {
                    ElevatedCard(
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    ) {
                        Text(text = filter.name, modifier = Modifier.padding(5.dp))
                    }

                    DropdownMenu(
                        expanded = filterMenuExpanded,
                        onDismissRequest = { filterMenuExpanded = false }
                    ) {
                        Filter.entries.forEach { entry ->
                            DropdownMenuItem(onClick = {
                                filter = entry
                                filterMenuExpanded = false
                            }, text = {
                                Text(text = entry.name, fontWeight = if (entry == filter) FontWeight.Bold else FontWeight.Normal)
                            })
                        }
                    }
                }

                var sortMenuExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = sortMenuExpanded,
                    onExpandedChange = { sortMenuExpanded = it },
                ) {
                    ElevatedCard(
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    ) {
                        Text(text = "Sort by", modifier = Modifier.padding(5.dp))
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        SortBy.entries.forEach { entry ->
                            DropdownMenuItem(onClick = {
                                sortBy = entry
                                sortMenuExpanded = false
                            }, text = {
                                Text(text = entry.name, fontWeight = if (entry == sortBy) FontWeight.Bold else FontWeight.Normal)
                            })
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = sortReverseOrder,
                        onCheckedChange = { sortReverseOrder = it },
                    )
                    Text(text = "Reverse order", fontSize = 15.sp, fontWeight = FontWeight.Light, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                stickyHeader {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(bottom = 2.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextField(
                            value = nameFilter,
                            onValueChange = {
                                nameFilter = it
                                coroutineScope.launch { refreshList(clearSelected = false) }
                            },
                            placeholder = { Text(text = "Search by name") },
                            singleLine = true,
                            modifier = Modifier
                                .padding(end = 5.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                        )

                        Checkbox(
                            checked = if (friends.isEmpty() || selectedFriends.size < friends.size) false else friends.all { friend -> selectedFriends.contains(friend.userId) },
                            onCheckedChange = { state ->
                                if (state) {
                                    friends.mapNotNull { it.userId }.forEach { userId ->
                                        if (!selectedFriends.contains(userId)) {
                                            selectedFriends.add(userId)
                                        }
                                    }
                                } else {
                                    if (nameFilter.isNotBlank()) {
                                        filterFriends(friends, filter, nameFilter).mapNotNull { it.userId }.forEach { userId ->
                                            selectedFriends.remove(userId)
                                        }
                                    } else {
                                        selectedFriends.clear()
                                    }
                                }
                            }
                        )
                    }
                }
                item {
                    if (friends.isEmpty()) {
                        Text(text = "No friends found", fontSize = 12.sp, fontWeight = FontWeight.Light, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }
                }
                items(friends, key = { it.userId!! }) { friendInfo ->
                    var bitmojiBitmap by remember(friendInfo) { mutableStateOf(bitmojiCache[friendInfo.bitmojiAvatarId]) }

                    fun selectFriend(state: Boolean) {
                        friendInfo.userId?.let {
                            if (state) {
                                selectedFriends.add(it)
                            } else {
                                selectedFriends.remove(it)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectFriend(!selectedFriends.contains(friendInfo.userId))
                            }.pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = { context.androidContext.copyToClipboard(friendInfo.mutableUsername.toString()) }
                                )
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LaunchedEffect(friendInfo) {
                            withContext(Dispatchers.IO) {
                                if (bitmojiBitmap != null || friendInfo.bitmojiAvatarId == null || friendInfo.bitmojiSelfieId == null) return@withContext

                                val bitmojiUrl = BitmojiSelfie.getBitmojiSelfie(friendInfo.bitmojiSelfieId, friendInfo.bitmojiAvatarId, BitmojiSelfie.BitmojiSelfieType.NEW_THREE_D) ?: return@withContext

                                runCatching {
                                    RemoteMediaResolver.downloadMedia(bitmojiUrl) { inputStream, length ->
                                        bitmojiCache[friendInfo.bitmojiAvatarId ?: return@withContext] = BitmapFactory.decodeStream(inputStream).also {
                                            bitmojiBitmap = it
                                        }
                                    }
                                }
                            }
                        }

                        Image(
                            bitmap = remember (bitmojiBitmap) { bitmojiBitmap?.asImageBitmap() ?: noBitmojiBitmap },
                            contentDescription = null,
                            modifier = Modifier.size(35.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ){
                                Text(text = (friendInfo.displayName ?: friendInfo.mutableUsername).toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, overflow = TextOverflow.Ellipsis, maxLines = 1, lineHeight = 10.sp)
                                Text(text = friendInfo.mutableUsername.toString(), fontSize = 10.sp, fontWeight = FontWeight.Light, overflow = TextOverflow.Ellipsis, maxLines = 1, lineHeight = 10.sp)
                            }
                            val lastMessage by rememberAsyncMutableState(defaultValue = null) {
                                getDMLastMessage(friendInfo.userId)
                            }

                            val userInfo = remember(friendInfo, lastMessage) {
                                buildString {
                                    append("Relationship: ")
                                    append(context.translation["friendship_link_type.${FriendLinkType.fromValue(friendInfo.friendLinkType).shortName}"])
                                    friendInfo.addedTimestamp.takeIf { it > 0L }?.let {
                                        append("\nAdded ${DateFormat.getDateTimeInstance().format(Date(it))}")
                                    }
                                    friendInfo.snapScore.takeIf { it > 0 }?.let {
                                        append("\nSnap Score: $it")
                                    }
                                    friendInfo.streakLength.takeIf { it > 0 }?.let {
                                        append("\nStreaks length: $it")
                                    }
                                    lastMessage?.let {
                                        append("\nSent messages: ${it.serverMessageId}")
                                        append("\nLast message: ${DateFormat.getDateTimeInstance().format(Date(it.creationTimestamp))}")
                                    }
                                    betterLocation.locationHistory[context.database.myUserId]?.let { myLocation ->
                                        betterLocation.locationHistory[friendInfo.userId]?.let {
                                            append("\n${myLocation.distanceTo(it).let { distance ->
                                                if (distance < 1) "${(distance * 1000).toInt()} m" else "${distance.toInt()} km"
                                            } } away")
                                        }
                                    }
                                }
                            }
                            Text(text = userInfo, fontSize = 12.sp, fontWeight = FontWeight.Light, lineHeight = 12.sp, overflow = TextOverflow.Ellipsis)
                        }

                        Checkbox(
                            checked = selectedFriends.contains(friendInfo.userId),
                            onCheckedChange = { selectFriend(it) }
                        )
                    }
                }
            }

            var showConfirmationDialog by remember { mutableStateOf(false) }
            var action by remember { mutableStateOf({}) }

            if (showConfirmationDialog) {
                ConfirmationDialog(
                    onConfirm = {
                        action()
                        action = {}
                        showConfirmationDialog = false
                    },
                    onCancel = {
                        action = {}
                        showConfirmationDialog = false
                    }
                )
            }

            val ctx = LocalContext.current

            val actions = remember {
                mapOf<() -> String, () -> Unit>(
                    { "Clean " + selectedFriends.size + " conversations" } to {
                        context.feature(Messaging::class).conversationManager?.getOneOnOneConversationIds(selectedFriends.toList().also {
                            selectedFriends.clear()
                        }, onError = { error ->
                            context.shortToast("Failed to fetch conversations: $error")
                        }, onSuccess = { conversations ->
                            removeAction(ctx, conversations.map { it.second }.distinct(), delay = 10L to 40L) { conversationId, setDialogMessage ->
                                cleanConversation(
                                    conversationId, setDialogMessage
                                )
                            }.invokeOnCompletion {
                                coroutineScope.launch { refreshList() }
                            }
                        })
                    },
                    { "Remove " + selectedFriends.size + " friends" } to {
                        removeAction(ctx, selectedFriends.toList().also {
                            selectedFriends.clear()
                        }, delay = 500L to 1200L) { userId, _ -> removeFriend(userId) }.invokeOnCompletion {
                            coroutineScope.launch { refreshList() }
                        }
                    },
                    { "Clean " + selectedFriends.size + " conversations and remove " + selectedFriends.size + " friends" } to {
                        context.feature(Messaging::class).conversationManager?.getOneOnOneConversationIds(selectedFriends.toList().also {
                            selectedFriends.clear()
                        }, onError = { error ->
                            context.shortToast("Failed to fetch conversations: $error")
                        }, onSuccess = { conversations ->
                            removeAction(ctx, conversations.map { it.second }.distinct(), delay = 500L to 1200L) { conversationId, setDialogMessage ->
                                cleanConversation(
                                    conversationId, setDialogMessage
                                )
                                removeFriend(conversations.firstOrNull { it.second == conversationId }?.first ?: return@removeAction)
                            }.invokeOnCompletion {
                                coroutineScope.launch { refreshList() }
                            }
                        })
                    }
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                actions.forEach { (text, actionFunction) ->
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(2.dp),
                        onClick = {
                            showConfirmationDialog = true
                            action = actionFunction
                        },
                        enabled = selectedFriends.isNotEmpty()
                    ) {
                        Text(text = remember(selectedFriends.size) { text() })
                    }
                }
            }
        }

        LaunchedEffect(sortBy, sortReverseOrder) {
            coroutineScope.launch {
                refreshList(clearSelected = false)
            }
            focusManager.clearFocus()
        }

        LaunchedEffect(filter) {
            coroutineScope.launch {
                refreshList()
            }
            focusManager.clearFocus()
        }
    }

    override fun run() {
        context.coroutineScope.launch(Dispatchers.Main) {
            createComposeAlertDialog(context.mainActivity!!) {
                BulkMessagingDialog()
            }.apply {
                setCanceledOnTouchOutside(false)
                show()
            }
        }
    }

    private fun removeFriend(userId: String) {
        context.mappings.useMapper(FriendRelationshipChangerMapper::class) {
            val friendRelationshipChangerInstance = context.feature(AddFriendSourceSpoof::class).friendRelationshipChangerInstance!!
            val runFriendDurableJobMethod = classReference.getAsClass()?.methods?.first {
                it.name == runFriendDurableJob.getAsString()
            } ?: throw Exception("Failed to find runFriendDurableJobMethod method")

            val removeFriendDurableJob = context.androidContext.classLoader.loadClass("com.snap.identity.job.snapchatter.RemoveFriendDurableJob")
                .constructors.firstOrNull {
                it.parameterTypes.size == 1
            }?.run {
                newInstance(
                    parameterTypes[0].dataBuilder {
                        set("a", userId) // userId
                        set("b", "DELETED_BY_MY_FRIENDS") // deleteSourceType
                        set("f", "")
                    }
                )
            } ?: throw Exception("Failed to create RemoveFriendDurableJob instance")

            val completable = runFriendDurableJobMethod.invoke(null,
                friendRelationshipChangerInstance,
                userId, // userId
                removeFriendDurableJob, // friend durable job
                0x5, // action type
                "DELETED_BY_MY_FRIENDS", // deleteSourceType
            )!!
            completable::class.java.methods.first {
                it.name == "subscribe" && it.parameterTypes.isEmpty()
            }.invoke(completable)
        }
    }

    private suspend fun cleanConversation(
        conversationId: String,
        setDialogMessage: (String) -> Unit
    ) {
        val messageCount = mutableIntStateOf(0)
        MessagingTask(
            context.messagingBridge,
            conversationId,
            taskType = MessagingTaskType.DELETE,
            constraints = listOf(MessagingConstraints.MY_USER_ID(context.messagingBridge), {
                contentType != ContentType.STATUS.id
            }),
            processedMessageCount = messageCount,
            onSuccess = {
                setDialogMessage("${messageCount.intValue} deleted messages")
            },
        ).run()
    }
}
