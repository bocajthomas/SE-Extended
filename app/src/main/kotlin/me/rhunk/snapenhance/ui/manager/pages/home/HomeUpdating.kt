@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package me.rhunk.snapenhance.ui.manager.pages.home

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import me.rhunk.snapenhance.UpdateNotification
import me.rhunk.snapenhance.common.ui.*
import me.rhunk.snapenhance.ui.components.LazyColumnBottomSheet
import me.rhunk.snapenhance.ui.manager.Routes

class HomeUpdating: Routes.Route() {
    private fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }


    override val content: @Composable (NavBackStackEntry) -> Unit = {
        val localContext = LocalContext.current
        var showUpdateCheckerIntervalMenu by remember { mutableStateOf(false) }

        var appUpdateChecker = context.sharedPreferences.rememberMutableBooleanPreferenceState(
            key = "app_update_checker",
            defaultValue = true
        )
        val getAppUpdateChecker by appUpdateChecker

        var updateCheckerInterval = context.sharedPreferences.rememberMutableStringPreferenceState(
            key = "app_update_check_interval",
            defaultValue = "daily"
        )
        val getUpdateCheckerInterval by updateCheckerInterval

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = lazyColumnContentPadding(staticVertical = 10.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            item {
                Text(text = translation["notification_title"], modifier = Modifier.padding(16.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShapeSingle
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { openNotificationSettings(localContext) }
                                .padding(15.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(translation["notification_settings_title"])
                            IconButton(
                                onClick = { openNotificationSettings(localContext) },
                                shapes = IconButtonDefaults.shapes()
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.OpenInNew,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
            }
            item {
                Text(text = translation["app_title"], modifier = Modifier.padding(16.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShapeGroupedTop,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    appUpdateChecker.value = !appUpdateChecker.value
                                }
                                .padding(15.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = translation["check_for_app_updates_title"],
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 20.sp
                                )
                                Text(
                                    text = translation["check_for_app_updates_description"],
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Light,
                                    lineHeight = 15.sp
                                )
                            }

                            Switch(
                                checked = getAppUpdateChecker,
                                onCheckedChange = { newState ->
                                    appUpdateChecker.value = newState
                                }
                            )
                        }
                    }

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShapeGroupedBottom,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showUpdateCheckerIntervalMenu = true }
                                .padding(15.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = translation["update_checker_interval_title"],
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 20.sp
                                )
                                Text(
                                    text = translation["update_checker_interval_description"],
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Light,
                                    lineHeight = 15.sp
                                )
                            }

                            Text(
                                text = getUpdateCheckerInterval,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 17.sp
                            )
                        }
                    }

                    if (showUpdateCheckerIntervalMenu) {
                        LazyColumnBottomSheet(
                            onDismiss = { showUpdateCheckerIntervalMenu = false }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = cardShapeGroupedTop
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                updateCheckerInterval.value = "daily"
                                                UpdateNotification.updateNotiRepeater(context.androidContext, force = true)
                                            },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(context.translation["manager.dialogs.update_checker_interval.daily"], modifier = Modifier.weight(1f).padding(start = 10.dp, top = 15.dp, bottom = 15.dp), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        RadioButton(
                                            selected = getUpdateCheckerInterval == "daily",
                                            onClick = {
                                                updateCheckerInterval.value = "daily"
                                                UpdateNotification.updateNotiRepeater(context.androidContext, force = true)
                                            }
                                        )
                                    }
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = cardShapeGroupedMiddle
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                updateCheckerInterval.value = "weekly"
                                                UpdateNotification.updateNotiRepeater(context.androidContext, force = true)
                                            },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(context.translation["manager.dialogs.update_checker_interval.weekly"], modifier = Modifier.weight(1f).padding(start = 10.dp, top = 15.dp, bottom = 15.dp), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        RadioButton(
                                            selected = getUpdateCheckerInterval == "weekly",
                                            onClick = {
                                                updateCheckerInterval.value = "weekly"
                                                UpdateNotification.updateNotiRepeater(context.androidContext, force = true)
                                            }
                                        )
                                    }
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = cardShapeGroupedBottom
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                updateCheckerInterval.value = "monthly"
                                            },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(context.translation["manager.dialogs.update_checker_interval.monthly"], modifier = Modifier.weight(1f).padding(start = 10.dp, top = 15.dp, bottom = 15.dp), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        RadioButton(
                                            selected = getUpdateCheckerInterval == "monthly",
                                            onClick = {
                                                updateCheckerInterval.value = "monthly"
                                                UpdateNotification.updateNotiRepeater(context.androidContext, force = true)
                                            }
                                        )
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