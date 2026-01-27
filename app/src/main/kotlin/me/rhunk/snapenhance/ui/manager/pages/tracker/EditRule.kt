@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package me.rhunk.snapenhance.ui.manager.pages.tracker

import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Launch
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import me.rhunk.snapenhance.common.data.*
import me.rhunk.snapenhance.common.ui.*
import me.rhunk.snapenhance.storage.*
import me.rhunk.snapenhance.ui.components.LazyColumnBottomSheet
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.manager.pages.social.AddFriendBottomSheet

@Composable
fun ActionCheckbox(
    text: String,
    checked: MutableState<Boolean>,
    onChanged: (Boolean) -> Unit = {}
) {
    Row(
        modifier = Modifier.clickable {
            checked.value = !checked.value
            onChanged(checked.value)
        }.padding(5.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Checkbox(
            modifier = Modifier.size(30.dp),
            checked = checked.value,
            onCheckedChange = {
                checked.value = it
                onChanged(it)
            }
        )
    }
}

class EditRule : Routes.Route() {
    private val fab = mutableStateOf<@Composable (() -> Unit)?>(null)

    @Composable
    fun ConditionCheckboxes(
        params: TrackerRuleActionParams
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(topStart = 15.dp, topEnd = 5.dp, bottomEnd = 5.dp, bottomStart = 5.dp)
                ) {
                    ActionCheckbox(
                        text = translation["inside_conversation"],
                        checked = remember { mutableStateOf(params.onlyInsideConversation) },
                        onChanged = { params.onlyInsideConversation = it }
                    )
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(topStart = 5.dp, topEnd = 15.dp, bottomEnd = 5.dp, bottomStart = 5.dp)
                ) {
                    ActionCheckbox(
                        text = translation["outside_conversation"],
                        checked = remember { mutableStateOf(params.onlyOutsideConversation) },
                        onChanged = { params.onlyOutsideConversation = it }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomEnd = 5.dp, bottomStart = 5.dp)
                ) {
                    ActionCheckbox(
                        text = translation["snapchat_active"],
                        checked = remember { mutableStateOf(params.onlyWhenAppActive) },
                        onChanged = { params.onlyWhenAppActive = it }
                    )
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomEnd = 5.dp, bottomStart = 5.dp)
                ) {
                    ActionCheckbox(
                        text = translation["snapchat_inactive"],
                        checked = remember { mutableStateOf(params.onlyWhenAppInactive) },
                        onChanged = { params.onlyWhenAppInactive = it }
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = cardShapeGroupedBottom
            ) {
                ActionCheckbox(
                    text = translation["no_notification_snapchat_active"],
                    checked = remember { mutableStateOf(params.noPushNotificationWhenAppActive) },
                    onChanged = { params.noPushNotificationWhenAppActive = it }
                )
            }
        }
    }

    // persistent add event state
    private var currentEventType by mutableStateOf(TrackerEventType.CONVERSATION_ENTER.key)
    private var addEventActions by mutableStateOf(emptySet<TrackerRuleAction>())
    private val addEventActionParams by mutableStateOf(TrackerRuleActionParams())

    override val floatingActionButton: @Composable () -> Unit = {
        fab.value?.invoke()
    }

    @OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
    override val content: @Composable (NavBackStackEntry) -> Unit = { navBackStackEntry ->
        val currentRuleId = navBackStackEntry.arguments?.getString("rule_id")?.toIntOrNull()
        val events = rememberAsyncMutableStateList(defaultValue = emptyList()) {
            currentRuleId?.let { ruleId ->
                context.database.getTrackerEvents(ruleId)
            } ?: emptyList()
        }
        var currentScopeType by remember { mutableStateOf(TrackerScopeType.BLACKLIST) }
        val scopes = rememberAsyncMutableStateList(defaultValue = emptyList()) {
            currentRuleId?.let { ruleId ->
                context.database.getRuleTrackerScopes(ruleId).also {
                    currentScopeType = if (it.isEmpty()) {
                        TrackerScopeType.WHITELIST
                    } else {
                        it.values.first()
                    }
                }.map { it.key }
            } ?: emptyList()
        }
        val ruleName = rememberAsyncMutableState(defaultValue = "", keys = arrayOf(currentRuleId)) {
            currentRuleId?.let { ruleId ->
                context.database.getTrackerRule(ruleId)?.name ?: translation["default_rule_name"]
            } ?: translation["default_rule_name"]
        }
        var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }
        val primaryColor = MaterialTheme.colorScheme.primary
        val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

        LaunchedEffect(Unit) {
            fab.value = {
                var deleteConfirmation by remember { mutableStateOf(false) }
                if (deleteConfirmation) {
                    LazyColumnBottomSheet(
                        onDismiss = { deleteConfirmation = false },
                    ) {
                        Text(
                            text = context.translation["manager.dialogs.delete_rule.title"],
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 5.dp, bottom = 10.dp)
                        )

                        Text(
                            text = context.translation["manager.dialogs.delete_rule.content"],
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 15.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Button(
                                onClick = { deleteConfirmation = false },
                                shapes = ButtonDefaults.shapes()
                            ) {
                                Text(context.translation["manager.dialogs.delete_rule.cancel_button"])
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = {
                                    if (currentRuleId != null) {
                                        context.database.deleteTrackerRule(currentRuleId)
                                    }
                                    routes.navController.popBackStack()
                                },
                                shapes = ButtonDefaults.shapes()
                            ) {
                                Text(context.translation["manager.dialogs.delete_rule.delete_button"])
                            }
                        }
                    }
                }

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
                            val ruleId = currentRuleId ?: context.database.newTrackerRule()
                            events.forEach { event ->
                                context.database.addOrUpdateTrackerRuleEvent(
                                    event.id.takeIf { it > -1 },
                                    ruleId,
                                    event.eventType,
                                    event.params,
                                    event.actions
                                )
                            }
                            context.database.setTrackerRuleName(ruleId, ruleName.value.trim())
                            context.database.setRuleTrackerScopes(ruleId, currentScopeType, scopes)
                            routes.navController.popBackStack()
                        },
                        icon = { Icon(Icons.Rounded.Save, contentDescription = null) },
                        text = { Text(translation["save_rule_button"]) },
                        containerColor = primaryColor,
                        contentColor = onPrimaryColor
                    )
                    if (currentRuleId != null) {
                        FloatingActionButtonMenuItem(
                            onClick = { deleteConfirmation = true },
                            icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                            text = { Text(translation["delete_rule_button"]) },
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }
        }

        DisposableEffect(Unit) {
            onDispose { fab.value = null }
        }

        LazyColumn(
            contentPadding = lazyColumnContentPadding(staticVertical = 10.dp, hasFAB = true),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            item {
                TextField(
                    value = ruleName.value,
                    onValueChange = {
                        ruleName.value = it
                    },
                    singleLine = true,
                    placeholder = {
                        Text(
                            translation["rule_name_placeholder"],
                            fontSize = 18.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    textStyle = TextStyle(fontSize = 20.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                )
            }


            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ){
                    Text(translation["scope_title"], fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))

                    var addFriendBottomSheet by remember { mutableStateOf(null as AddFriendBottomSheet?) }

                    val friendDialogActions = remember {
                        AddFriendBottomSheet.Actions(
                            onFriendState = { friend, state ->
                                if (state) {
                                    scopes.add(friend.userId)
                                } else {
                                    scopes.remove(friend.userId)
                                }
                            },
                            onGroupState = { group, state ->
                                if (state) {
                                    scopes.add(group.conversationId)
                                } else {
                                    scopes.remove(group.conversationId)
                                }
                            },
                            getFriendState = { friend ->
                                friend.userId in scopes
                            },
                            getGroupState = { group ->
                                group.conversationId in scopes
                            }
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(cardShapeGroupedTop)
                                .clickable { scopes.clear() },
                            shape = cardShapeGroupedTop
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(end = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    translation["scope_all"],
                                    modifier = Modifier.padding(10.dp).weight(1f)
                                )
                                RadioButton(selected = scopes.isEmpty(), onClick = null)
                            }
                        }

                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(cardShapeGroupedMiddle)
                                .clickable {
                                    currentScopeType = TrackerScopeType.WHITELIST
                                    addFriendBottomSheet = AddFriendBottomSheet(
                                        context,
                                        friendDialogActions,
                                        pinnedIds = scopes,
                                    )
                                },
                            shape = cardShapeGroupedMiddle
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(end = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    translation["scope_no_one_except"] + " " + if (currentScopeType == TrackerScopeType.WHITELIST && scopes.isNotEmpty()) scopes.size.toString() + " " + translation["friends_groups"] else "...",
                                    modifier = Modifier.padding(10.dp).weight(1f)
                                )
                                RadioButton(
                                    selected = scopes.isNotEmpty() && currentScopeType == TrackerScopeType.WHITELIST,
                                    onClick = null
                                )
                            }
                        }

                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(cardShapeGroupedBottom)
                                .clickable {
                                    currentScopeType = TrackerScopeType.BLACKLIST
                                    addFriendBottomSheet = AddFriendBottomSheet(
                                        context,
                                        friendDialogActions,
                                        pinnedIds = scopes,
                                    )
                                },
                            shape = cardShapeGroupedBottom
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(end = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    translation["scope_everyone_except"] + " " + if (currentScopeType == TrackerScopeType.BLACKLIST && scopes.isNotEmpty()) scopes.size.toString() + " " + translation["friends_groups"] else "...",
                                    modifier = Modifier.padding(10.dp).weight(1f)
                                )
                                RadioButton(
                                    selected = scopes.isNotEmpty() && currentScopeType == TrackerScopeType.BLACKLIST,
                                    onClick = null
                                )
                            }
                        }
                    }

                    addFriendBottomSheet?.Content {
                        addFriendBottomSheet = null
                    }
                }

                var addEventBottomSheet by remember { mutableStateOf(false) }
                var showEventTypeBottomSheet by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(translation["events_title"], fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                    IconButton(
                        onClick = { addEventBottomSheet = true },
                        modifier = Modifier.padding(8.dp),
                        shapes = IconButtonDefaults.shapes()
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add Event", modifier = Modifier.size(32.dp))
                    }
                }

                if (showEventTypeBottomSheet) {
                    LazyColumnBottomSheet(
                        onDismiss = { showEventTypeBottomSheet = false },
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            TrackerEventType.entries.forEachIndexed { index, eventType ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = cardShape(TrackerEventType.entries.size, index)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable {
                                                currentEventType = eventType.key
                                                showEventTypeBottomSheet = false
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            context.translation["tracker_events.${eventType.key}"],
                                            modifier = Modifier.padding(10.dp).weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (addEventBottomSheet) {
                    LazyColumnBottomSheet(
                        onDismiss = { addEventBottomSheet = false },
                    ) {
                        Text(
                            modifier = Modifier.padding(bottom = 5.dp).fillMaxWidth(),
                            text = context.translation["manager.dialogs.add_event.title"],
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Card (
                                modifier = Modifier.fillMaxWidth(),
                                shape = cardShapeSingle
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable {
                                            showEventTypeBottomSheet = true
                                        },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(context.translation["manager.dialogs.add_event.type_title"], fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(10.dp))
                                    Text(context.translation["tracker_events.$currentEventType"], overflow = TextOverflow.Ellipsis, maxLines = 1, modifier = Modifier.padding(10.dp))
                                }
                            }

                            Text(context.translation["manager.dialogs.add_event.triggers_title"], fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(2.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Card (
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(topStart = 15.dp, topEnd = 5.dp, bottomEnd = 5.dp, bottomStart = 5.dp)
                                    ) {
                                        ActionCheckbox(
                                            context.translation["tracker_actions.log"],
                                            checked = remember {
                                                mutableStateOf(addEventActions.contains(TrackerRuleAction.LOG))
                                            }
                                        ) { isChecked ->
                                            if (isChecked) {
                                                addEventActions += TrackerRuleAction.LOG
                                            } else {
                                                addEventActions -= TrackerRuleAction.LOG
                                            }
                                        }
                                    }

                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(topStart = 5.dp, topEnd = 15.dp, bottomEnd = 5.dp, bottomStart = 5.dp)
                                    ) {
                                        ActionCheckbox(
                                            context.translation["tracker_actions.in_app_notification"],
                                            checked = remember {
                                                mutableStateOf(addEventActions.contains(TrackerRuleAction.IN_APP_NOTIFICATION))
                                            }
                                        ) { isChecked ->
                                            if (isChecked) {
                                                addEventActions += TrackerRuleAction.IN_APP_NOTIFICATION
                                            } else {
                                                addEventActions -= TrackerRuleAction.IN_APP_NOTIFICATION
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomEnd = 5.dp, bottomStart = 15.dp)
                                    ) {
                                        ActionCheckbox(
                                            context.translation["tracker_actions.push_notification"],
                                            checked = remember {
                                                mutableStateOf(addEventActions.contains(TrackerRuleAction.PUSH_NOTIFICATION))
                                            }
                                        ) { isChecked ->
                                            if (isChecked) {
                                                addEventActions += TrackerRuleAction.PUSH_NOTIFICATION
                                            } else {
                                                addEventActions -= TrackerRuleAction.PUSH_NOTIFICATION
                                            }
                                        }
                                    }

                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomEnd = 15.dp, bottomStart = 5.dp)
                                    ) {
                                        ActionCheckbox(
                                            context.translation["tracker_actions.custom"],
                                            checked = remember {
                                                mutableStateOf(addEventActions.contains(TrackerRuleAction.CUSTOM))
                                            }
                                        ) { isChecked ->
                                            if (isChecked) {
                                                addEventActions += TrackerRuleAction.CUSTOM
                                            } else {
                                                addEventActions -= TrackerRuleAction.CUSTOM
                                            }
                                        }
                                    }
                                }
                            }

                            Text(context.translation["manager.dialogs.add_event.conditions_title"], fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(2.dp))
                            ConditionCheckboxes(addEventActionParams)

                            HorizontalDivider(
                                modifier = Modifier
                                    .padding(top = 15.dp, bottom = 10.dp)
                                    .fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                Button(
                                    onClick = {
                                        events.add(
                                            0,
                                            TrackerRuleEvent(
                                                -1,
                                                true,
                                                currentEventType,
                                                addEventActionParams.copy(),
                                                addEventActions.toList()
                                            )
                                        )
                                        addEventBottomSheet = false
                                    },
                                    shapes = ButtonDefaults.shapes()
                                ) {
                                    Text(context.translation["manager.dialogs.add_event.add_button"])
                                }
                            }
                        }
                    }
                }
            }

            item {
                if (events.isEmpty()) {
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
                                shape = MaterialShapes.SoftBurst.toShape()
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Event,
                                        contentDescription = null,
                                        modifier = Modifier.size(100.dp)
                                    )
                                }
                            }
                            Text(
                                text = translation["events_empty"],
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(8.dp)
                            )
                            Text(
                                text = translation["events_empty_desc"],
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    events.forEachIndexed { index, event ->
                        var expanded by remember { mutableStateOf(false) }
                        ElevatedCard(
                            modifier = Modifier.fillMaxSize()
                                .clip(cardShape(events.size, index))
                                .clickable { expanded = !expanded },
                            shape = cardShape(events.size, index),
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f, fill = false),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Column {
                                            Text(
                                                context.translation["tracker_events.${event.eventType}"],
                                                lineHeight = 20.sp,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = event.actions.joinToString(", ") { context.translation["tracker_actions.${it.key}"] },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Light,
                                                overflow = TextOverflow.Ellipsis,
                                                maxLines = 1,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                    OutlinedIconButton(
                                        onClick = {
                                            if (event.id > -1) {
                                                context.database.deleteTrackerRuleEvent(event.id)
                                            }
                                            events.remove(event)
                                        },
                                        shapes = IconButtonDefaults.shapes()
                                    ) {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete"
                                        )
                                    }
                                }
                                if (expanded) {
                                    Column(
                                        modifier = Modifier.padding(10.dp)
                                    ) {
                                        ConditionCheckboxes(event.params)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}