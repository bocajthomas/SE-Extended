package me.rhunk.snapenhance.common.util.ktx

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ApplicationInfoFlags
import android.os.Build
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream

fun PackageManager.getApplicationInfoCompat(packageName: String, flags: Int) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getApplicationInfo(packageName, ApplicationInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION")
        getApplicationInfo(packageName, flags)
    }

fun Context.copyToClipboard(data: String, label: String = "Copied Text") {
    runCatching {
        getSystemService(android.content.ClipboardManager::class.java).setPrimaryClip(
            ClipData.newPlainText(label, data))
    }
}

fun Context.getTextFromClipboard(): String? {
    return runCatching {
        getSystemService(android.content.ClipboardManager::class.java).primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.text?.toString()
    }.getOrNull()
}

fun Context.getUrlFromClipboard(): String? {
    return getTextFromClipboard()?.takeIf { it.startsWith("http") }
}

fun Context.openLink(url: String, shouldThrow: Boolean = false) {
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW).apply {
            data = url.toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }.onFailure {
        if (shouldThrow) throw it
        Toast.makeText(this, "Failed to open link", Toast.LENGTH_SHORT).show()
    }
}

fun InputStream.toParcelFileDescriptor(coroutineScope: CoroutineScope): ParcelFileDescriptor {
    val pfd = ParcelFileDescriptor.createPipe()
    val fos = ParcelFileDescriptor.AutoCloseOutputStream(pfd[1])

    coroutineScope.launch(Dispatchers.IO) {
        try {
            copyTo(fos)
        } finally {
            close()
            fos.flush()
            fos.close()
        }
    }

    return pfd[0]
}
