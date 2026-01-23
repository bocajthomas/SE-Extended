@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package me.rhunk.snapenhance.ui.manager.pages.features

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.common.data.MessagingRuleType
import me.rhunk.snapenhance.common.data.RuleState
import me.rhunk.snapenhance.common.ui.*
import me.rhunk.snapenhance.storage.clearRuleIds
import me.rhunk.snapenhance.storage.getRuleIds
import me.rhunk.snapenhance.storage.setRule
import me.rhunk.snapenhance.ui.components.LazyColumnBottomSheet
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.manager.pages.social.AddFriendBottomSheet
import me.rhunk.snapenhance.ui.manager.pages.social.AddFriendBottomSheet.Actions

class ManageRuleFeature : Routes.Route() {
    @Composable
    fun SelectRuleTypeCard(
        checked: Boolean,
        text: String,
        description: String,
        onStateChanged: () -> Unit,
        shape: RoundedCornerShape,
        selectedBlock: @Composable () -> Unit = {},
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .clickable { if (!checked) onStateChanged() },
            shape = shape,
            colors = CardDefaults.elevatedCardColors(
                containerColor = if (checked) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column (
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (checked) {
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,

                            )
                        }
                    }

                    RadioButton(
                        selected = checked,
                        onClick = onStateChanged
                    )
                }
                if (checked) {
                    selectedBlock()
                }
            }
        }
    }

    override val content: @Composable (NavBackStackEntry) -> Unit = content@{ navBackStackEntry ->
        val currentRuleType = navBackStackEntry.arguments?.getString("rule_type")?.let {
            MessagingRuleType.getByName(it)
        } ?: return@content

        var ruleState by remember {
            mutableStateOf(context.config.root.rules.getRuleState(currentRuleType))
        }

        val propertyKeyPair = remember {
            context.config.root.rules.getPropertyPair(currentRuleType.key)
        }

        val updateDispatcher = rememberAsyncUpdateDispatcher()
        val currentRuleIds by rememberAsyncMutableState(defaultValue = mutableListOf(), updateDispatcher = updateDispatcher) {
            context.database.getRuleIds(currentRuleType.key)
        }

        fun setRuleState(newState: RuleState?) {
            ruleState = newState
            propertyKeyPair.value.setAny(newState?.key)
            context.coroutineScope.launch {
                context.config.writeConfig(dispatchConfigListener = false)
            }
        }

        var addFriendBottomSheet by remember { mutableStateOf(null as AddFriendBottomSheet?) }

        LaunchedEffect(addFriendBottomSheet) {
            if (addFriendBottomSheet == null) {
                updateDispatcher.dispatch()
            }
        }

        fun showAddFriendDialog() {
            addFriendBottomSheet = AddFriendBottomSheet(
                context = context,
                pinnedIds = currentRuleIds,
                actionHandler = Actions(
                    onFriendState = { friend, state ->
                        context.database.setRule(friend.userId, currentRuleType.key, state)
                        if (state) {
                            currentRuleIds.add(friend.userId)
                        } else {
                            currentRuleIds.remove(friend.userId)
                        }
                    },
                    onGroupState = { group, state ->
                        context.database.setRule(group.conversationId, currentRuleType.key, state)
                        if (state) {
                            currentRuleIds.add(group.conversationId)
                        } else {
                            currentRuleIds.remove(group.conversationId)
                        }
                    },
                    getFriendState = { friend ->
                        currentRuleIds.contains(friend.userId)
                    },
                    getGroupState = { group ->
                        currentRuleIds.contains(group.conversationId)
                    }
                )
            )
        }

        if (addFriendBottomSheet != null) {
            addFriendBottomSheet?.Content {
                addFriendBottomSheet = null
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .screenContentPadding(staticVertical = 10.dp),
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = remember {
                        context.translation[propertyKeyPair.key.propertyName()]
                    },
                    fontSize = 20.sp,
                )
                Text(
                    text = remember {
                        context.translation[propertyKeyPair.key.propertyDescription()]
                    },
                    fontWeight = FontWeight.Light,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SelectRuleTypeCard(
                    checked = ruleState == null,
                    text = translation["disable_state_option"],
                    description = translation["disable_state_subtext"],
                    onStateChanged = { setRuleState(null) },
                    shape = cardShapeGroupedTop as RoundedCornerShape
                )

                SelectRuleTypeCard(
                    checked = ruleState == RuleState.WHITELIST,
                    text = translation["whitelist_state_option"],
                    description = translation.format("whitelist_state_subtext", "count" to currentRuleIds.size.toString()),
                    onStateChanged = { setRuleState(RuleState.WHITELIST) },
                    shape = cardShapeGroupedMiddle as RoundedCornerShape
                ) {
                    OutlinedButton(
                        onClick = { showAddFriendDialog() },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Text(text = translation["whitelist_state_button"])
                    }
                }

                SelectRuleTypeCard(
                    checked = ruleState == RuleState.BLACKLIST,
                    text = translation["blacklist_state_option"],
                    description = translation.format("blacklist_state_subtext", "count" to currentRuleIds.size.toString()),
                    onStateChanged = { setRuleState(RuleState.BLACKLIST) },
                    shape = cardShapeGroupedBottom as RoundedCornerShape
                ) {
                    OutlinedButton(
                        onClick = { showAddFriendDialog() },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Text(text = translation["blacklist_state_button"])
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                var confirmationDialog by remember { mutableStateOf(false) }
                if (confirmationDialog) {
                    LazyColumnBottomSheet(
                        onDismiss = { confirmationDialog = false },
                    ) {
                        Text(
                            text = translation["clear_confirmation_text"],
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        HorizontalDivider(
                            modifier = Modifier
                                .padding(top = 15.dp, bottom = 10.dp)
                                .fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Button(
                                onClick = { confirmationDialog = false },
                                shapes = ButtonDefaults.shapes()
                            ) {
                                Text(text = translation["cancel_confirmation_button"])
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = {
                                    context.database.clearRuleIds(currentRuleType.key)
                                    context.coroutineScope.launch(context.database.executor.asCoroutineDispatcher()) {
                                        updateDispatcher.dispatch()
                                    }
                                    confirmationDialog = false
                                },
                                shapes = ButtonDefaults.shapes()
                            ) {
                                Text(text = translation["confirm_confirmation_button"])
                            }
                        }
                    }
                }

                Button(
                    onClick = { confirmationDialog = true },
                    modifier = Modifier.padding(vertical = 10.dp),
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(text = translation["clear_list_button"])
                }
            }
        }
    }
}