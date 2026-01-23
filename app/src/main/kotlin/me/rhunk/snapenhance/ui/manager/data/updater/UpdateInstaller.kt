package me.rhunk.snapenhance.ui.manager.data.updater

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import me.rhunk.snapenhance.common.logger.AbstractLogger
import me.rhunk.snapenhance.Installer
import java.io.File
import java.io.InputStream
import java.io.OutputStream

@SuppressLint("RequestInstallPackagesPolicy")
fun installApk(
    context: Context,
    apkFile: File,
    fileProviderAuthority: String,
    logger: AbstractLogger
) {
    if (!apkFile.exists()) return

    val packageInstaller = context.packageManager.packageInstaller
    val params = PackageInstaller.SessionParams(
        PackageInstaller.SessionParams.MODE_FULL_INSTALL
    )
    try {
        val sessionId = packageInstaller.createSession(params)
        val session = packageInstaller.openSession(sessionId)
        transferFileToSession(session, apkFile, logger)
        val intent = Intent(context, Installer::class.java).apply {
            action = "INSTALL_PACKAGE_RESULT"
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            sessionId,
            intent,
            pendingIntentFlags
        )
        session.commit(pendingIntent.intentSender)
        session.close()
    } catch (e: Exception) {
        logger.error("PackageInstaller failed to start session.", e)
    }
}

private fun transferFileToSession(
    session: PackageInstaller.Session,
    apkFile: File,
    logger: AbstractLogger
) {
    var sessionStream: OutputStream? = null
    var fileStream: InputStream? = null
    try {
        fileStream = apkFile.inputStream()
        sessionStream = session.openWrite("package_file", 0, apkFile.length())
        fileStream.copyTo(sessionStream)
        session.fsync(sessionStream)
    } catch (e: Exception) {
        session.abandon()
        throw e
    } finally {
        sessionStream?.close()
        fileStream?.close()
    }
}