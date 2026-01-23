package me.rhunk.snapenhance

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import me.rhunk.snapenhance.ui.manager.data.updater.UpdateFetcher

class UpdateNotification : BroadcastReceiver() {
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "app_update"
        private const val NOTIFICATION_ID = 1001

        private fun getPendingIntent(context: Context, noCreate: Boolean = false): PendingIntent? {
            val intent = Intent(context, UpdateNotification::class.java)
            val flags = if (noCreate) {
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            }
            return PendingIntent.getBroadcast(context, 0, intent, flags)
        }

        fun updateNotiRepeater(context: Context, force: Boolean = false) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val remoteContext = RemoteSideContext(context)
            val existingIntent = getPendingIntent(context, noCreate = true)
            if (existingIntent != null && !force) return
            val isEnabled = remoteContext.sharedPreferences.getBoolean("app_update_checker", true)
            if (!isEnabled) {
                existingIntent?.let { alarmManager.cancel(it) }
                return
            }
            val fetchInterval = remoteContext.sharedPreferences.getString("app_update_check_interval", "daily")
            val intervalMillis = when (fetchInterval) {
                "daily" -> 86400000L
                "weekly" -> 604800000L
                "monthly" -> 2592000000L
                else -> 86400000L
            }
            val pendingIntent = getPendingIntent(context, noCreate = false)!!
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                5000L,
                intervalMillis,
                pendingIntent
            )
        }

        fun showNotification(context: Context, update: UpdateFetcher.LatestRelease) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val remoteContext = RemoteSideContext(context)
            remoteContext.translation.load()
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                    "App Updates",
                    NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("update_url", update.releaseUrl)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.launcher_icon_monochrome)
                .setContentTitle(remoteContext.translation["manager.update_notification.content_title"])
                .setContentText(remoteContext.translation.format("manager.update_notification.content_description", "versionName" to update.versionName))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        UpdateFetcher.checkForUpdates(context)
    }
}