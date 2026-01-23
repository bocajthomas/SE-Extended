package me.rhunk.snapenhance

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log

class Installer : BroadcastReceiver() {
    private val TAG = "Installer"

    fun deleteExtractedUpdate(context: Context) {
        val dir = context.getExternalFilesDir(null) ?: return
        val files = dir.listFiles() ?: return
        for (i in files.indices) {
            val file = files[i]
            if (file.name.contains("update_extract_")) {
                file.deleteRecursively()
            }
        }
    }

    @SuppressLint("UnsafeIntentLaunch")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "INSTALL_PACKAGE_RESULT") {
            val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
            val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)

            when (status) {
                PackageInstaller.STATUS_SUCCESS -> {
                    Log.i(TAG, "Installation successful")
                    deleteExtractedUpdate(context)
                }
                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                    val activityIntent = if (android.os.Build.VERSION.SDK_INT >= 33) {
                        intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                    }

                    activityIntent?.let {
                        context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
                else -> {
                    Log.e(TAG, "Installation failed with status $status: $message")
                    deleteExtractedUpdate(context)
                }
            }
        }
    }
}