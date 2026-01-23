package me.rhunk.snapenhance.ui.util

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import me.rhunk.snapenhance.common.logger.AbstractLogger

typealias ActivityLauncherCallback = (resultCode: Int, intent: Intent?) -> Unit

class ActivityLauncherHelper(
    val activity: ComponentActivity,
) {
    private var callback: ActivityLauncherCallback? = null
    private var permissionResultLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            runCatching {
                callback?.let { it(if (result) Activity.RESULT_OK else Activity.RESULT_CANCELED, null) }
            }.onFailure {
                AbstractLogger.directError("Failed to process activity result", it)
            }
            callback = null
        }

    private var activityResultLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            runCatching {
                callback?.let { it(result.resultCode, result.data) }
            }.onFailure {
                AbstractLogger.directError("Failed to process activity result", it)
            }
            callback = null
        }

    fun launch(intent: Intent, callback: ActivityLauncherCallback) {
        if (this.callback != null) {
            throw IllegalStateException("Already launching an activity")
        }
        this.callback = callback
        activityResultLauncher.launch(intent)
    }

    fun requestPermission(permission: String, callback: ActivityLauncherCallback) {
        if (this.callback != null) {
            throw IllegalStateException("Already launching an activity")
        }
        this.callback = callback
        permissionResultLauncher.launch(permission)
    }
}

fun ActivityLauncherHelper.chooseFolder(callback: (uri: String) -> Unit) {
    launch(
        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    ) { resultCode, intent ->
        if (resultCode != Activity.RESULT_OK) {
            return@launch
        }
        val uri = intent?.data ?: return@launch
        val value = uri.toString()
        this.activity.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        callback(value)
    }
}

fun ActivityLauncherHelper.saveFile(name: String, type: String = "*/*", callback: (uri: String) -> Unit) {
    launch(
        Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(type)
            .putExtra(Intent.EXTRA_TITLE, name)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    ) { resultCode, intent ->
        if (resultCode != Activity.RESULT_OK) {
            return@launch
        }
        val uri = intent?.data ?: return@launch
        val value = uri.toString()
        this.activity.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        callback(value)
    }
}
fun ActivityLauncherHelper.openFile(type: String = "*/*", callback: (uri: String) -> Unit) {
    launch(
        Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(type)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    ) { resultCode, intent ->
        if (resultCode != Activity.RESULT_OK) {
            return@launch
        }
        val uri = intent?.data ?: return@launch
        val value = uri.toString()
        this.activity.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        callback(value)
    }
}
fun ActivityLauncherHelper.createFile(
    fileName: String,
    mimeType: String = "*/*",
    callback: (uri: String) -> Unit
) {
    launch(
        Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(mimeType)
            .putExtra(Intent.EXTRA_TITLE, fileName)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    ) { resultCode, intent ->
        if (resultCode != Activity.RESULT_OK) {
            return@launch
        }
        val uri = intent?.data ?: return@launch
        val value = uri.toString()

        this.activity.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        callback(value)
    }
}
fun ActivityLauncherHelper.openMultipleFiles(type: String = "*/*", callback: (uris: List<String>) -> Unit) {
    launch(
        Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(type)
            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    ) { resultCode, intent ->
        if (resultCode != Activity.RESULT_OK) {
            return@launch
        }
        val uris = mutableListOf<String>()
        intent?.let {
            val clipData = it.clipData
            if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    uris.add(uri.toString())
                    this.activity.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
            } else {
                val uri = it.data ?: return@let
                uris.add(uri.toString())
                this.activity.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        }
        callback(uris)
    }
}

