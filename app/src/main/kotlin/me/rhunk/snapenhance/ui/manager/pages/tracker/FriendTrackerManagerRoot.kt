@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package me.rhunk.snapenhance.ui.manager.pages.tracker

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Launch
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.common.ui.cardShape
import me.rhunk.snapenhance.common.ui.lazyColumnContentPadding
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableState
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableStateList
import me.rhunk.snapenhance.common.ui.rememberAsyncUpdateDispatcher
import me.rhunk.snapenhance.common.util.snap.BitmojiSelfie
import me.rhunk.snapenhance.storage.*
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.util.ActivityLauncherHelper
import me.rhunk.snapenhance.ui.util.coil.BitmojiImage

@OptIn(ExperimentalFoundationApi::class)
class FriendTrackerManagerRoot : Routes.Route() {
    enum class FilterType {
        CONVERSATION, USERNAME, EVENT
    }

    private val titles by lazy { listOf(translation["logs_tab"], translation["rules_tab"]) }
    private var selectedIndex by mutableIntStateOf(0)
    private lateinit var logDeleteAction : () -> Unit
    private lateinit var exportAction : () -> Unit

    private lateinit var activityLauncherHelper: ActivityLauncherHelper

    override val init: () -> Unit = {
        activityLauncherHelper = ActivityLauncherHelper(context.activity!!)
    }

    override val floatingActionButton: @Composable () -> Unit = {
        val primaryColor = MaterialTheme.colorScheme.primary
        val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
        var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

        when (selectedIndex) {
            0 -> {
                FloatingActionButtonMenu(
                    modifier = Modifier.offset(x = 18.dp, y = 18.dp),
                    expanded = fabMenuExpanded,
                    button = {
                        ToggleFloatingActionButton(
                            modifier = Modifier.semantics {
                                traversalIndex = -1f
                                stateDescription = if (fabMenuExpanded) "Expanded" else "Collapsed"
                                contentDescription = "null"
                            },
                            checked = fabMenuExpanded,
                            onCheckedChange = { fabMenuExpanded = !fabMenuExpanded },
                            containerColor = { primaryColor },
                        ) {
                            val imageVector by remember {
                                derivedStateOf {
                                    if (checkedProgress > 0.5f) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.Launch
                                }
                            }
                            Icon(
                                painter = rememberVectorPainter(imageVector),
                                contentDescription = null,
                                modifier = Modifier.animateIcon(
                                    checkedProgress = { checkedProgress },
                                    color = { onPrimaryColor }
                                )
                            )
                        }
                    },
                ) {
                    FloatingActionButtonMenuItem(
                        onClick = {
                            context.coroutineScope.launch { exportAction() }
                            fabMenuExpanded = false
                        },
                        icon = { Icon(Icons.Rounded.SaveAlt, contentDescription = "Export") },
                        text = { Text(translation["export_button"]) },
                        containerColor = primaryColor,
                        contentColor = onPrimaryColor
                    )

                    FloatingActionButtonMenuItem(
                        onClick = {
                            context.coroutineScope.launch { logDeleteAction() }
                            fabMenuExpanded = false
                        },
                        icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete") },
                        text = { Text(translation["delete_button"]) },
                        containerColor = primaryColor,
                        contentColor = onPrimaryColor
                    )
                }
            }
            1 -> {
                ExtendedFloatingActionButton(
                    modifier = Modifier.offset(x = 2.dp, y = 2.dp),
                    onClick = { routes.editRule.navigate() },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = "Add Rule") },
                    text = { Text(translation["add_rule_button"]) },
                    containerColor = primaryColor,
                    contentColor = onPrimaryColor
                )
            }
        }
    }

    @Composable
    private fun ConfigRulesTab() {
        val updateRules = rememberAsyncUpdateDispatcher()
        val rules = rememberAsyncMutableStateList(defaultValue = listOf(), updateDispatcher = updateRules) {
            context.database.getTrackerRulesDesc()
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = lazyColumnContentPadding(staticVertical = 10.dp, hasSubAction = true, hasFAB = true),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            item {
                if (rules.isEmpty()) {
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
                                shape = MaterialShapes.VerySunny.toShape()
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PersonSearch,
                                        contentDescription = null,
                                        modifier = Modifier.size(100.dp)
                                    )
                                }
                            }
                            Text(
                                text = translation["rules_empty"],
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(8.dp)
                            )
                            Text(
                                text = translation["rules_empty_desc"],
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }

            itemsIndexed(rules) { index, rule ->
                val ruleName by rememberAsyncMutableState(defaultValue = rule.name) {
                    context.database.getTrackerRule(rule.id)?.name ?: "(empty)"
                }
                val eventCount by rememberAsyncMutableState(defaultValue = 0) {
                    context.database.getTrackerEvents(rule.id).size
                }
                val scopeCount by rememberAsyncMutableState(defaultValue = 0) {
                    context.database.getRuleTrackerScopes(rule.id).size
                }
                var enabled by rememberAsyncMutableState(defaultValue = rule.enabled) {
                    context.database.getTrackerRule(rule.id)?.enabled ?: false
                }

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape(rules.size, index))
                        .clickable {
                            routes.editRule.navigate {
                                this["rule_id"] = rule.id.toString()
                            }
                        },
                    shape = cardShape(rules.size, index)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(ruleName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text(buildString {
                                append(eventCount)
                                append(" events")
                                if (scopeCount > 0) {
                                    append(", ")
                                    append(scopeCount)
                                    append(" scopes")
                                }

                            }, fontSize = 13.sp, fontWeight = FontWeight.Light)
                        }

                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val scopesBitmoji = rememberAsyncMutableStateList(defaultValue = emptyList()) {
                                context.database.getRuleTrackerScopes(rule.id, limit = 10).mapNotNull {
                                    context.database.getFriendInfo(it.key)?.let { friend ->
                                        friend.selfieId to friend.bitmojiId
                                    }
                                }.take(3)
                            }

                            Row {
                                scopesBitmoji.forEachIndexed { index, friend ->
                                    Box(
                                        modifier = Modifier
                                            .offset(x = (-index * 20).dp + (scopesBitmoji.size * 20).dp - 20.dp)
                                    ) {
                                        BitmojiImage(
                                            size = 50,
                                            modifier = Modifier
                                                .border(
                                                    BorderStroke(1.dp, Color.White),
                                                    CircleShape
                                                )
                                                .background(Color.White, CircleShape)
                                                .clip(CircleShape),
                                            context = context,
                                            url = BitmojiSelfie.getBitmojiSelfie(friend.first, friend.second, BitmojiSelfie.BitmojiSelfieType.NEW_THREE_D)
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier
                                .padding(start = 5.dp, end = 5.dp)
                                .height(50.dp)
                                .width(1.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(5.dp)
                                )
                            )

                            Switch(
                                checked = enabled,
                                onCheckedChange = {
                                    enabled = it
                                    context.database.setTrackerRuleState(rule.id, it)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override val topBarSubActions: @Composable (RowScope.() -> Unit) = {
        val getSelectedIndex = selectedIndex

        FlowRow(
            Modifier.padding(horizontal = 8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            titles.forEachIndexed { index, label ->
                ToggleButton(
                    checked = getSelectedIndex == index,
                    onCheckedChange = { selectedIndex = index },
                    shapes =
                        when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            titles.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
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
            pageCount = { titles.size }
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


        HorizontalPager(
            modifier = Modifier.fillMaxSize(),
            state = pagerState
        ) { page ->
            when (page) {
                0 -> LogsTab(
                    context = context,
                    activityLauncherHelper = activityLauncherHelper,
                    deleteAction = { logDeleteAction = it },
                    exportAction = { exportAction = it }
                )
                1 -> ConfigRulesTab()
            }
        }
    }
}