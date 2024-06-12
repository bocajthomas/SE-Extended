package me.rhunk.snapenhance.ui.setup.screens.impl

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        Button(onClick = onClick) {
            Text(text = context.translation["setup.permissions.request_button"])
        }
    }

    @Composable
    private fun GrantedIcon() {
        Icon(
            imageVector = Icons.Filled.Check,
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
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            context.androidContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                        } else {
                            true
                        }
                    },
                    requestPermission = { perm ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            activityLauncherHelper.requestPermission(Manifest.permission.POST_NOTIFICATIONS) { resultCode, _ ->
                                coroutineScope.launch {
                                    grantedPermissions[perm.translationKey] = resultCode == Activity.RESULT_OK
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
                            data = Uri.parse("package:${context.androidContext.packageName}")
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
                            data = Uri.parse("package:${context.androidContext.packageName}")
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

        DialogText(text = context.translation["setup.permissions.dialog"])

        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .padding(all = 16.dp),
            ) {
                permissions.forEach { perm ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DialogText(
                            text = context.translation["setup.permissions.${perm.translationKey}"],
                            modifier = Modifier.weight(1f)
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