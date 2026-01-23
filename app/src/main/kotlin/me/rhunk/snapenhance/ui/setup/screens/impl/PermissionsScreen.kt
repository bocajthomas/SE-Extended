@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package me.rhunk.snapenhance.ui.setup.screens.impl

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.common.ui.cardShape
import me.rhunk.snapenhance.common.ui.columnPadding
import me.rhunk.snapenhance.ui.setup.screens.SetupScreen
import me.rhunk.snapenhance.ui.util.ActivityLauncherHelper
import me.rhunk.snapenhance.ui.util.OnLifecycleEvent

data class PermissionData(
    val translationKey: String,
    val isPermissionGranted: () -> Boolean,
    val requestPermission: (PermissionData) -> Unit,
)

class PermissionsScreen : SetupScreen() {
    private lateinit var activityLauncherHelper: ActivityLauncherHelper

    override fun init() {
        activityLauncherHelper = ActivityLauncherHelper(context.activity!!)
    }

    @Composable
    private fun RequestButton(onClick: () -> Unit) {
        Button(onClick = onClick, shapes = ButtonDefaults.shapes()) {
            Text(text = context.translation["setup.permissions.request_button"])
        }
    }

    @Composable
    private fun GrantedIcon() {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .padding(5.dp)
        )
    }

    @SuppressLint("BatteryLife")
    @Composable
    override fun Content() {
        val coroutineScope = rememberCoroutineScope()
        val grantedPermissions = remember {
            mutableStateMapOf<String, Boolean>()
        }
        val permissions = remember {
            listOf(
                PermissionData(
                    translationKey = "notification_access",
                    isPermissionGranted = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                            context.androidContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                        } else {
                            true
                        }
                    },
                    requestPermission = { perm ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                            activityLauncherHelper.requestPermission(Manifest.permission.POST_NOTIFICATIONS) { resultCode, _ ->
                                coroutineScope.launch {
                                    grantedPermissions[perm.translationKey] = resultCode == Activity.RESULT_OK
                                }
                            }
                        }
                    }
                ),

                PermissionData(
                    translationKey = "install_unknown_apps",
                    isPermissionGranted = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                            context.androidContext.packageManager.canRequestPackageInstalls()
                        } else {
                            true
                        }
                    },
                    requestPermission = { perm ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                            activityLauncherHelper.launch(Intent().apply {
                                action = Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
                                data = "package:${context.androidContext.packageName}".toUri()
                            }) { resultCode, _ ->
                                coroutineScope.launch {
                                    grantedPermissions[perm.translationKey] = resultCode == 0
                                }
                            }
                        }
                    }
                ),
                PermissionData(
                    translationKey = "battery_optimization",
                    isPermissionGranted = {
                        val powerManager =
                            context.androidContext.getSystemService(Context.POWER_SERVICE) as PowerManager
                        powerManager.isIgnoringBatteryOptimizations(context.androidContext.packageName)
                    },
                    requestPermission = { perm ->
                        activityLauncherHelper.launch(Intent().apply {
                            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                            data = "package:${context.androidContext.packageName}".toUri()
                        }) { resultCode, _ ->
                            coroutineScope.launch {
                                grantedPermissions[perm.translationKey] = resultCode == 0
                            }
                        }
                    }
                ),
                PermissionData(
                    translationKey = "display_over_other_apps",
                    isPermissionGranted = {
                        Settings.canDrawOverlays(context.androidContext)
                    },
                    requestPermission = { perm ->
                        activityLauncherHelper.launch(Intent().apply {
                            action = Settings.ACTION_MANAGE_OVERLAY_PERMISSION
                            data = "package:${context.androidContext.packageName}".toUri()
                        }) { resultCode, _ ->
                            coroutineScope.launch {
                                grantedPermissions[perm.translationKey] = resultCode == 0
                            }
                        }
                    }
                )
            )
        }

        fun updateState() {
            permissions.forEach { perm ->
                grantedPermissions[perm.translationKey] = perm.isPermissionGranted()
            }
            if (permissions.all { perm -> grantedPermissions[perm.translationKey] == true }) {
                goNext()
            }
        }

        OnLifecycleEvent { _, event ->
            if (event != Lifecycle.Event.ON_RESUME) return@OnLifecycleEvent
            coroutineScope.launch {
                updateState()
                delay(1000)
                updateState()
            }
        }

        LaunchedEffect(Unit) {
            updateState()
        }

        Column(
            modifier = Modifier.fillMaxSize().columnPadding(staticVertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DialogText(text = context.translation["setup.permissions.description"])

            permissions.forEachIndexed { index, perm ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = cardShape(groupSize = permissions.size, index = index),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (perm.isPermissionGranted()) {
                                    grantedPermissions[perm.translationKey] = true
                                } else {
                                    perm.requestPermission(perm)
                                }
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = context.translation["setup.permissions.${perm.translationKey}"],
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                        )

                        if (grantedPermissions[perm.translationKey] == true) {
                            GrantedIcon()
                        } else {
                            RequestButton {
                                if (perm.isPermissionGranted()) {
                                    grantedPermissions[perm.translationKey] = true
                                } else {
                                    perm.requestPermission(perm)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}