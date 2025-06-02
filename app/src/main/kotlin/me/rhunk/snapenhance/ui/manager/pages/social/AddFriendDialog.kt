package me.rhunk.snapenhance.ui.manager.pages.social

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import me.rhunk.snapenhance.RemoteSideContext
import me.rhunk.snapenhance.common.ReceiversConfig
import me.rhunk.snapenhance.common.data.MessagingFriendInfo
import me.rhunk.snapenhance.common.data.MessagingGroupInfo
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableState
import me.rhunk.snapenhance.common.util.snap.BitmojiSelfie
import me.rhunk.snapenhance.common.util.snap.SnapWidgetBroadcastReceiverHelper
import me.rhunk.snapenhance.ui.util.coil.BitmojiImage

class AddFriendDialog(
    private val context: RemoteSideContext,
    private val actionHandler: Actions,
    private val pinnedIds: List<String>? = null
) {
    class Actions(
        val onFriendState: (friend: MessagingFriendInfo, state: Boolean) -> Unit,
        val onGroupState: (group: MessagingGroupInfo, state: Boolean) -> Unit,
        val getFriendState: (friend: MessagingFriendInfo) -> Boolean,
        val getGroupState: (group: MessagingGroupInfo) -> Boolean,
    )

    private val stateCache = mutableMapOf<String, Boolean>()
    private val translation by lazy { context.translation.getCategory("manager.dialogs.add_friend")}

    @Composable
    private fun ListCardEntry(
        id: String,
        bitmoji: String? = null,
        name: String,
        participantsCount: Int? = null,
        getCurrentState: () -> Boolean,
        onState: (Boolean) -> Unit = {},
    ) {
        var currentState by rememberAsyncMutableState(defaultValue = stateCache[id] ?: false) {
            getCurrentState().also { stateCache[id] = it }
        }
        val coroutineScope = rememberCoroutineScope()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    currentState = !currentState
                    stateCache[id] = currentState
                    coroutineScope.launch(Dispatchers.IO) {
                        onState(currentState)
                    }
                }
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BitmojiImage(
                context = this@AddFriendDialog.context,
                url = bitmoji,
                modifier = Modifier.padding(end = 2.dp),
                size = 32,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(
                    text = name,
                    fontSize = 15.sp,
                )

                participantsCount?.let {
                    Text(
                        text = translation.format("participants_text", "count" to it.toString()),
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Checkbox(
                checked = currentState,
                onCheckedChange = {
                    currentState = it
                    stateCache[id] = currentState
                    coroutineScope.launch(Dispatchers.IO) {
                        onState(currentState)
                    }
                }
            )
        }
    }

    @Composable
    private fun DialogHeader(searchKeyword: MutableState<String>) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
        ) {
            Text(
                text = translation["title"],
                fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .align(alignment = Alignment.CenterHorizontally)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchKeyword.value,
                onValueChange = { searchKeyword.value = it },
                label = {
                    Text(text = translation["search_hint"])
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = "Search")
                }
            )
        }
    }


    @Composable
    fun Content(dismiss: () -> Unit = { }) {
        var cachedFriends by remember { mutableStateOf(null as List<MessagingFriendInfo>?) }
        var cachedGroups by remember { mutableStateOf(null as List<MessagingGroupInfo>?) }

        val coroutineScope = rememberCoroutineScope()

        var timeoutJob: Job? = null
        var hasFetchError by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            context.database.receiveMessagingDataCallback = { friends, groups ->
                cachedFriends = friends.run {
                    if (pinnedIds != null) {
                        sortedBy { -pinnedIds.indexOf(it.userId) }
                    } else friends
                }
                cachedGroups = groups.run {
                    if (pinnedIds != null) {
                        sortedBy { -pinnedIds.indexOf(it.conversationId) }
                    } else groups
                }
                timeoutJob?.cancel()
                hasFetchError = false
            }
            SnapWidgetBroadcastReceiverHelper.create(ReceiversConfig.BRIDGE_SYNC_ACTION) {}.also {
                runCatching {
                    context.androidContext.sendBroadcast(it)
                }.onFailure {
                    context.log.error("Failed to send broadcast", it)
                    hasFetchError = true
                }
            }
            timeoutJob = coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    delay(20000)
                    hasFetchError = true
                }
            }
        }

        me.rhunk.snapenhance.ui.util.Dialog(
            onDismissRequest = {
                timeoutJob?.cancel()
                dismiss()
            },
            properties = me.rhunk.snapenhance.ui.util.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                colors = CardDefaults.elevatedCardColors(),
                modifier = Modifier
                    .fillMaxSize()
                    .fillMaxWidth()
                    .padding(all = 20.dp)
            ) {
                if (cachedGroups == null || cachedFriends == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (hasFetchError) {
                            Text(
                                text = translation["fetch_error"],
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 10.dp, top = 10.dp)
                            )
                            return@Card
                        }
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding()
                                .size(30.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    return@Card
                }

                val searchKeyword = remember { mutableStateOf("") }

                val filteredGroups = cachedGroups!!.takeIf { searchKeyword.value.isNotBlank() }?.filter {
                    it.name.contains(searchKeyword.value, ignoreCase = true)
                } ?: cachedGroups!!

                val filteredFriends = cachedFriends!!.takeIf { searchKeyword.value.isNotBlank() }?.filter {
                    it.mutableUsername.contains(searchKeyword.value, ignoreCase = true) ||
                    it.displayName?.contains(searchKeyword.value, ignoreCase = true) == true
                } ?: cachedFriends!!

                DialogHeader(searchKeyword)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                ) {
                    item {
                        if (filteredGroups.isEmpty()) return@item
                        Text(text = translation["category_groups"],
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 10.dp, top = 10.dp)
                        )
                    }

                    items(filteredGroups.size) {
                        val group = filteredGroups[it]
                        ListCardEntry(
                            id = group.conversationId,
                            name = group.name,
                            participantsCount = group.participantsCount,
                            getCurrentState = { actionHandler.getGroupState(group) }
                        ) { state ->
                            actionHandler.onGroupState(group, state)
                        }
                    }

                    item {
                        if (filteredFriends.isEmpty()) return@item
                        Text(text = translation["category_friends"],
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 10.dp, top = 10.dp)
                        )
                    }

                    items(filteredFriends.size) { index ->
                        val friend = filteredFriends[index]

                        ListCardEntry(
                            id = friend.userId,
                            bitmoji = friend.takeIf { it.bitmojiId != null }?.let {
                                BitmojiSelfie.getBitmojiSelfie(it.selfieId, it.bitmojiId, BitmojiSelfie.BitmojiSelfieType.NEW_THREE_D)
                            },
                            name = friend.displayName?.takeIf { name -> name.isNotBlank() } ?: friend.mutableUsername,
                            getCurrentState = { actionHandler.getFriendState(friend) }
                        ) { state ->
                            actionHandler.onFriendState(friend, state)
                        }
                    }
                }
            }
        }
    }
}