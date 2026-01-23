package me.rhunk.snapenhance.ui.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import me.rhunk.snapenhance.ui.manager.data.AppIcons

fun switchAppIcon(context: Context, iconKey: String) {
    val packageManager = context.packageManager
    val packageName = context.packageName

    AppIcons.IconList.forEach { appIcon ->
        val componentName = ComponentName(packageName, appIcon.aliasClassName)
        val newState = if (appIcon.key == iconKey) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }

        packageManager.setComponentEnabledSetting(
            componentName,
            newState,
            PackageManager.DONT_KILL_APP
        )
    }
}