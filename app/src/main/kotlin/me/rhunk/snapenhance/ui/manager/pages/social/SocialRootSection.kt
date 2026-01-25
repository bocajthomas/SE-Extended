@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package me.rhunk.snapenhance.ui.manager.pages.social

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RemoveRedEye
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.R
import me.rhunk.snapenhance.common.data.MessagingFriendInfo
import me.rhunk.snapenhance.common.data.MessagingGroupInfo
import me.rhunk.snapenhance.common.data.SocialScope
import me.rhunk.snapenhance.common.ui.*
import me.rhunk.snapenhance.common.util.snap.BitmojiSelfie
import me.rhunk.snapenhance.storage.*
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.util.coil.BitmojiImage

class SocialRootSection : Routes.Route() {
    private var friendList: List<MessagingFriendInfo> by mutableStateOf(emptyList())
    private var groupList: List<MessagingGroupInfo> by mutableStateOf(emptyList())
    private var selectedIndex by mutableIntStateOf(0)
    private val socialOptions by lazy {
        listOf(translation["friends_tab"], translation["groups_tab"])
    }

    private fun updateScopeLists() {
        context.coroutineScope.launch {
            friendList = context.database.getFriends(descOrder = true)
            groupList = context.database.getGroups()
        }
    }

    @Composable
    private fun ScopeList(scope: SocialScope) {
        val remainingHours = remember { context.config.root.streaksReminder.remainingHours.get() }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = lazyColumnContentPadding(staticVertical = 10.dp, hasSubAction = true, hasFAB = true),
            verticalArrangement = Arrangement.spacedBy(3.dp)

        ) {
            val listSize = when (scope) {
                SocialScope.GROUP -> groupList.size
                SocialScope.FRIEND -> friendList.size
            }

            if (listSize == 0) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            ElevatedCard(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .size(200.dp),
                                shape = MaterialShapes.Arch.toShape()
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Group,
                                        contentDescription = null,
                                        modifier = Modifier.size(100.dp)
                                    )
                                }
                            }
                            Text(
                                text = if(scope == SocialScope.FRIEND) translation["friends_empty"] else translation["groups_empty"],
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(8.dp)
                            )
                            Text(
                                text = if(scope == SocialScope.FRIEND) translation["friends_empty_desc"] else translation["groups_empty_desc"],
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }

            items(listSize) { index ->
                val id = when (scope) {
                    SocialScope.GROUP -> groupList[index].conversationId
                    SocialScope.FRIEND -> friendList[index].userId
                }

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp),
                    onClick = {
                        routes.manageScope.navigate {
                            put("id", id)
                            put("scope", scope.key)
                        }
                    },
                    shape = cardShape(listSize, index)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (scope) {
                            SocialScope.GROUP -> {
                                val group = groupList[index]
                                Column(
                                    modifier = Modifier
                                        .padding(start = 7.dp)
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    Text(
                                        text = group.name,
                                        maxLines = 1,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            SocialScope.FRIEND -> {
                                val friend = friendList[index]
                                val streaks by rememberAsyncMutableState(defaultValue = friend.streaks) {
                                    context.database.getFriendStreaks(friend.userId)
                                }

                                BitmojiImage(
                                    context = context,
                                    url = BitmojiSelfie.getBitmojiSelfie(
                                        friend.selfieId,
                                        friend.bitmojiId,
                                        BitmojiSelfie.BitmojiSelfieType.NEW_THREE_D
                                    )
                                )

                                Column(
                                    modifier = Modifier
                                        .padding(start = 7.dp)
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    Text(
                                        text = friend.displayName ?: friend.mutableUsername,
                                        maxLines = 1,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = friend.mutableUsername,
                                        maxLines = 1,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Light
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    streaks?.takeIf { it.notify }?.let { streaks ->
                                        Icon(
                                            imageVector = ImageVector.vectorResource(id = R.drawable.streak_icon),
                                            contentDescription = null,
                                            modifier = Modifier.height(40.dp),
                                            tint = if (streaks.isAboutToExpire(remainingHours))
                                                MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = translation.format(
                                                "streaks_expiration_short",
                                                "hours" to (((streaks.expirationTimestamp - System.currentTimeMillis()) / 3600000).toInt().takeIf { it > 0 } ?: 0)
                                                    .toString()
                                            ),
                                            maxLines = 1,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        FilledIconButton(
                            onClick = {
                                routes.messagingPreview.navigate {
                                    put("id", id)
                                    put("scope", scope.key)
                                }
                            },
                            shapes = IconButtonDefaults.shapes()
                        ) {
                            Icon(imageVector = Icons.Rounded.RemoveRedEye, contentDescription = null)
                        }
                    }
                }
            }
        }
    }

    override val floatingActionButton: @Composable () -> Unit = {
        var addFriendBottomSheet by remember { mutableStateOf(null as AddFriendBottomSheet?) }

        if (addFriendBottomSheet != null) {
            addFriendBottomSheet?.Content {
                addFriendBottomSheet = null
            }
            DisposableEffect(Unit) {
                onDispose {
                    updateScopeLists()
                }
            }
        }

        LaunchedEffect(Unit) {
            updateScopeLists()
        }

        ExtendedFloatingActionButton(
            onClick = {
                addFriendBottomSheet = AddFriendBottomSheet(
                    context,
                    AddFriendBottomSheet.Actions(
                        onFriendState = { friend, state ->
                            if (state) {
                                context.bridgeService?.triggerScopeSync(SocialScope.FRIEND, friend.userId)
                            } else {
                                context.database.deleteFriend(friend.userId)
                            }
                        },
                        onGroupState = { group, state ->
                            if (state) {
                                context.bridgeService?.triggerScopeSync(SocialScope.GROUP, group.conversationId)
                            } else {
                                context.database.deleteGroup(group.conversationId)
                            }
                        },
                        getFriendState = { friend -> context.database.getFriendInfo(friend.userId) != null },
                        getGroupState = { group -> context.database.getGroupInfo(group.conversationId) != null }
                    ),
                    pinnedIds = (friendList.map { it.userId } + groupList.map { it.conversationId }).reversed(),
                )
            },
            modifier = Modifier.offset(x = 2.dp, y = 2.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text(text = translation["add_friend_group_button"]) },
        )
    }

    override val topBarSubActions: @Composable (RowScope.() -> Unit) = {
        val getSelectedIndex = selectedIndex

        FlowRow(
            Modifier.padding(horizontal = 8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            socialOptions.forEachIndexed { index, label ->
                ToggleButton(
                    checked = getSelectedIndex == index,
                    onCheckedChange = { selectedIndex = index },
                    shapes =
                        when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            socialOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                    modifier = Modifier.semantics { role = Role.RadioButton }
                        .weight(1f),
                ) {
                    Text(label)
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    override val content: @Composable (NavBackStackEntry) -> Unit = {
        val getCurrentSelectedIndex = selectedIndex

        val pagerState = rememberPagerState(
            initialPage = getCurrentSelectedIndex,
            pageCount = { socialOptions.size }
        )

        LaunchedEffect(getCurrentSelectedIndex) {
            if (getCurrentSelectedIndex != pagerState.currentPage) {
                pagerState.animateScrollToPage(getCurrentSelectedIndex)
            }
        }

        LaunchedEffect(pagerState.currentPage) {
            if (selectedIndex != pagerState.currentPage) {
                selectedIndex = pagerState.currentPage
            }
        }

        LaunchedEffect(Unit) {
            updateScopeLists()
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> ScopeList(scope = SocialScope.FRIEND)
                1 -> ScopeList(scope = SocialScope.GROUP)
            }
        }
    }
}