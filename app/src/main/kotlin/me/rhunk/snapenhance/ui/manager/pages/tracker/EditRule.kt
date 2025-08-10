package me.rhunk.snapenhance.ui.manager.pages.tracker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import me.rhunk.snapenhance.common.data.*
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableState
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableStateList
import me.rhunk.snapenhance.storage.*
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.manager.pages.social.AddFriendDialog

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
        },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            modifier = Modifier.size(30.dp),
            checked = checked.value,
            onCheckedChange = {
                checked.value = it
                onChanged(it)
            }
        )
        Text(text, fontSize = 12.sp)
    }
}


@Composable
fun ConditionCheckboxes(
    params: TrackerRuleActionParams
) {
    ActionCheckbox(text = "Only when I'm inside conversation", checked = remember { mutableStateOf(params.onlyInsideConversation) }, onChanged = { params.onlyInsideConversation = it })
    ActionCheckbox(text = "Only when I'm outside conversation", checked = remember { mutableStateOf(params.onlyOutsideConversation) }, onChanged = { params.onlyOutsideConversation = it })
    ActionCheckbox(text = "Only when Snapchat is active", checked = remember { mutableStateOf(params.onlyWhenAppActive) }, onChanged = { params.onlyWhenAppActive = it })
    ActionCheckbox(text = "Only when Snapchat is inactive", checked = remember { mutableStateOf(params.onlyWhenAppInactive) }, onChanged = { params.onlyWhenAppInactive = it })
    ActionCheckbox(text = "No notification when Snapchat is active", checked = remember { mutableStateOf(params.noPushNotificationWhenAppActive) }, onChanged = { params.noPushNotificationWhenAppActive = it })
}

class EditRule : Routes.Route() {
    private val fab = mutableStateOf<@Composable (() -> Unit)?>(null)

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
                context.database.getTrackerRule(ruleId)?.name ?: "Custom Rule"
            } ?: "Custom Rule"
        }

        LaunchedEffect(Unit) {
            fab.value = {
                var deleteConfirmation by remember { mutableStateOf(false) }

                if (deleteConfirmation) {
                    AlertDialog(
                        onDismissRequest = { deleteConfirmation = false },
                        title = { Text("Delete Rule") },
                        text = { Text("Are you sure you want to delete this rule?") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (currentRuleId != null) {
                                        context.database.deleteTrackerRule(currentRuleId)
                                    }
                                    routes.navController.popBackStack()
                                }
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            Button(
                                onClick = { deleteConfirmation = false }
                            ) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    ExtendedFloatingActionButton(
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
                        text = { Text("Save Rule") },
                        icon = { Icon(Icons.Default.Save, contentDescription = "Save Rule") }
                    )

                    if (currentRuleId != null) {
                        ExtendedFloatingActionButton(
                            containerColor = MaterialTheme.colorScheme.error,
                            onClick = { deleteConfirmation = true },
                            text = { Text("Delete Rule") },
                            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Rule") }
                        )
                    }
                }
            }
        }

        DisposableEffect(Unit) {
            onDispose { fab.value = null }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(2.dp),
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
                            "Rule Name",
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
                    Text("Scope", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))

                    var addFriendDialog by remember { mutableStateOf(null as AddFriendDialog?) }

                    val friendDialogActions = remember {
                        AddFriendDialog.Actions(
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

                    Box(modifier = Modifier.clickable { scopes.clear() }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = scopes.isEmpty(), onClick = null)
                            Text("All Friends/Groups")
                        }
                    }

                    Box(modifier = Modifier.clickable {
                        currentScopeType = TrackerScopeType.WHITELIST
                        addFriendDialog = AddFriendDialog(
                            context,
                            friendDialogActions,
                            pinnedIds = scopes,
                        )
                    }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = scopes.isNotEmpty() && currentScopeType == TrackerScopeType.WHITELIST, onClick = null)
                            Text("No one except " + if (currentScopeType == TrackerScopeType.WHITELIST && scopes.isNotEmpty()) scopes.size.toString() + " friends/groups" else "...")
                        }
                    }

                    Box(modifier = Modifier.clickable {
                        currentScopeType = TrackerScopeType.BLACKLIST
                        addFriendDialog = AddFriendDialog(
                            context,
                            friendDialogActions,
                            pinnedIds = scopes,
                        )
                    }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = scopes.isNotEmpty() && currentScopeType == TrackerScopeType.BLACKLIST, onClick = null)
                            Text("Everyone except " + if (currentScopeType == TrackerScopeType.BLACKLIST && scopes.isNotEmpty()) scopes.size.toString() + " friends/groups" else "...")
                        }
                    }

                    addFriendDialog?.Content {
                        addFriendDialog = null
                    }
                }

                var addEventDialog by remember { mutableStateOf(false) }
                val showDropdown = remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Events", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                    IconButton(onClick = { addEventDialog = true }, modifier = Modifier.padding(8.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Add Event", modifier = Modifier.size(32.dp))
                    }
                }

                if (addEventDialog) {
                    AlertDialog(
                        onDismissRequest = { addEventDialog = false },
                        title = { Text("Add Event", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                        text = {
                            Column(
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Type", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    ExposedDropdownMenuBox(expanded = showDropdown.value, onExpandedChange = { showDropdown.value = it }) {
                                        ElevatedButton(
                                            onClick = { showDropdown.value = true },
                                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        ) {
                                            Text(context.translation["tracker_events.$currentEventType"], overflow = TextOverflow.Ellipsis, maxLines = 1)
                                        }
                                        DropdownMenu(expanded = showDropdown.value, onDismissRequest = { showDropdown.value = false }) {
                                            TrackerEventType.entries.forEach { eventType ->
                                                DropdownMenuItem(onClick = {
                                                    currentEventType = eventType.key
                                                    showDropdown.value = false
                                                }, text = {
                                                    Text(context.translation["tracker_events.${eventType.key}"])
                                                })
                                            }
                                        }
                                    }
                                }

                                Text("Triggers", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(2.dp))

                                FlowRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(2.dp),
                                ) {
                                    TrackerRuleAction.entries.forEach { action ->
                                        ActionCheckbox(context.translation["tracker_actions.${action.key}"], checked = remember { mutableStateOf(addEventActions.contains(action)) }) {
                                            if (it) {
                                                addEventActions += action
                                            } else {
                                                addEventActions -= action
                                            }
                                        }
                                    }
                                }

                                Text("Conditions", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(2.dp))
                                ConditionCheckboxes(addEventActionParams)
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    events.add(0, TrackerRuleEvent(-1, true, currentEventType, addEventActionParams.copy(), addEventActions.toList()))
                                    addEventDialog = false
                                }
                            ) {
                                Text("Add")
                            }
                        }
                    )
                }
            }

            item {
                if (events.isEmpty()) {
                    Text("No events", fontSize = 12.sp, fontWeight = FontWeight.Light, modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth(), textAlign = TextAlign.Center)
                }
            }

            items(events) { event ->
                var expanded by remember { mutableStateOf(false) }

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .padding(4.dp),
                    onClick = { expanded = !expanded }
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(context.translation["tracker_events.${event.eventType}"], lineHeight = 20.sp, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Text(text = event.actions.joinToString(", ") { context.translation["tracker_actions.${it.key}"] }, fontSize = 10.sp, fontWeight = FontWeight.Light, overflow = TextOverflow.Ellipsis, maxLines = 1, lineHeight = 14.sp)
                                }
                            }
                            OutlinedIconButton(
                                onClick = {
                                    if (event.id > -1) {
                                        context.database.deleteTrackerRuleEvent(event.id)
                                    }
                                    events.remove(event)
                                }
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete")
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

            item {
                Spacer(modifier = Modifier.height(140.dp))
            }
        }
    }
}