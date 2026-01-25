package me.rhunk.snapenhance.ui.manager.data.updater

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rhunk.snapenhance.common.logger.AbstractLogger
import okhttp3.OkHttpClient
import java.io.File
import java.util.zip.ZipInputStream
import me.rhunk.snapenhance.ui.manager.data.updater.UpdateFetcher.latestRelease
import okhttp3.Request

private fun getBaseDownloadDir(context: Context): File {
    return context.getExternalFilesDir(null) ?: throw IllegalStateException("External storage directory is null/unavailable.")
}

private val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
private val client = OkHttpClient()

private val _downloadProgress = kotlinx.coroutines.flow.MutableStateFlow(0f)
val downloadProgress: kotlinx.coroutines.flow.StateFlow<Float> = _downloadProgress
private val _isDownloading = kotlinx.coroutines.flow.MutableStateFlow(false)
val isDownloading: kotlinx.coroutines.flow.StateFlow<Boolean> = _isDownloading

fun updateProgress(progress: Float) { _downloadProgress.value = progress }
fun setDownloading(isDownloading: Boolean) { _isDownloading.value = isDownloading }

private fun unzip(
    zip: File,
    dir: File,
): File? {
    val destFolder = File(dir, "update_extract_${System.currentTimeMillis()}")
    destFolder.mkdirs()

    ZipInputStream(zip.inputStream()).use { zipInputStream ->
        var entry = zipInputStream.nextEntry
        while (entry != null) {
            if (!entry.isDirectory && entry.name.lowercase().endsWith(".apk")) {
                val outputFile = File(destFolder, entry.name.substringAfterLast('/'))
                outputFile.outputStream().use { outputStream ->
                    zipInputStream.copyTo(outputStream)
                }
                zip.delete()
                return outputFile
            }
            zipInputStream.closeEntry()
            entry = zipInputStream.nextEntry
        }
    }
    destFolder.deleteRecursively()
    return null
}

fun downloadUpdate(
    context: Context,
    logger: AbstractLogger,
    fileProviderAuthority: String
) = scope.launch {
    if (_isDownloading.value) return@launch
    val downloadBaseDir = try {
        getBaseDownloadDir(context)
    } catch (e: IllegalStateException) {
        logger.error("External storage is unavailable for download.", e)
        return@launch
    }
    val release = latestRelease ?: run { return@launch }
    val url = release.releaseUrl
    val isZip = url.lowercase().endsWith(".zip")
    val downloadedUpdateName = if (isZip) "update-${release.versionName}.zip" else "update-${release.versionName}.apk"
    val downloadedFile = File(downloadBaseDir, downloadedUpdateName)
    _isDownloading.value = true
    _downloadProgress.value = 0f
    try {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Download failed | HTTPS: ${response.code}")
            }
            val body = response.body
            val totalBytes = body.contentLength()
            var bytesRead: Long = 0

            downloadedFile.outputStream().use { output ->
                val input = body.byteStream()
                val buffer = ByteArray(4096)
                var read: Int

                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    bytesRead += read
                    _downloadProgress.value = bytesRead.toFloat() / totalBytes.toFloat()
                }
            }
            val finalApkFile = if (isZip) {
                unzip(downloadedFile, downloadBaseDir)
            } else {
                downloadedFile
            }
            if (finalApkFile != null) {
                withContext(Dispatchers.Main) {
                    installApk(context, finalApkFile, fileProviderAuthority, logger)
                }
            }
        }
    } catch (e: Exception) {
        logger.error("Download, unzip, or install failed", e)
    } finally {
        downloadedFile.delete()
        _isDownloading.value = false
        _downloadProgress.value = 0f
    }
}