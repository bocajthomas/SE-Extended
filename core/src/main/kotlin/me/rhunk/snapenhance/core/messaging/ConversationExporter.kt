package me.rhunk.snapenhance.core.messaging

import android.util.Base64InputStream
import android.util.Base64OutputStream
import com.google.gson.stream.JsonWriter
import kotlinx.coroutines.runBlocking
import me.rhunk.snapenhance.common.BuildConfig
import me.rhunk.snapenhance.common.data.ContentType
import me.rhunk.snapenhance.common.database.impl.FriendFeedEntry
import me.rhunk.snapenhance.common.database.impl.FriendInfo
import me.rhunk.snapenhance.common.util.snap.MediaDownloaderHelper
import me.rhunk.snapenhance.core.ModContext
import me.rhunk.snapenhance.core.features.impl.downloader.decoder.MessageDecoder
import me.rhunk.snapenhance.core.util.hook.findRestrictedConstructor
import me.rhunk.snapenhance.core.wrapper.impl.Message
import me.rhunk.snapenhance.core.wrapper.impl.SnapUUID
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors
import java.util.zip.Deflater
import java.util.zip.DeflaterInputStream
import java.util.zip.DeflaterOutputStream
import java.util.zip.ZipFile
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
class ConversationExporter(
    private val context: ModContext,
    private val friendFeedEntry: FriendFeedEntry,
    private val conversationParticipants: Map<String, FriendInfo>,
    private val exportParams: ExportParams,
    private val cacheFolder: File,
    private val outputFile: File
) {
    lateinit var printLog: (Any?) -> Unit

    private val downloadThreadExecutor = Executors.newFixedThreadPool(4)
    private val writeThreadExecutor = Executors.newSingleThreadExecutor()

    private val conversationJsonDataFile by lazy { cacheFolder.resolve("messages_${friendFeedEntry.key}.json") }
    private val jsonDataWriter by lazy { JsonWriter(conversationJsonDataFile.writer()) }
    private val outputFileStream by lazy { outputFile.outputStream() }
    private val participants = mutableMapOf<String, Int>()

    private val newBase64OutputStream by lazy {
        Base64OutputStream::class.java.findRestrictedConstructor {
            it.parameterTypes.size == 3 &&
            it.parameterTypes[0] == OutputStream::class.java &&
            it.parameterTypes[1] == Int::class.javaPrimitiveType &&
            it.parameterTypes[2] == Boolean::class.javaPrimitiveType
        } ?: throw Throwable("Failed to find Base64OutputStream constructor")
    }

    private val newBase64InputStream by lazy {
        Base64InputStream::class.java.findRestrictedConstructor {
            it.parameterTypes.size == 3 &&
            it.parameterTypes[0] == InputStream::class.java &&
            it.parameterTypes[1] == Int::class.javaPrimitiveType &&
            it.parameterTypes[2] == Boolean::class.javaPrimitiveType
        } ?: throw Throwable("Failed to find Base64InputStream constructor")
    }

    fun init() {
        when (exportParams.exportFormat) {
            ExportFormat.TEXT -> {
                outputFileStream.write("Conversation id: ${friendFeedEntry.key}\n".toByteArray())
                outputFileStream.write("Conversation name: ${friendFeedEntry.feedDisplayName}\n".toByteArray())
                outputFileStream.write("Participants:\n".toByteArray())
                conversationParticipants.forEach { (userId, friendInfo) ->
                    outputFileStream.write("  $userId: ${friendInfo.displayName}\n".toByteArray())
                }
                outputFileStream.write("\n\n".toByteArray())
            }
            else -> {
                jsonDataWriter.isHtmlSafe = true
                jsonDataWriter.serializeNulls = true

                jsonDataWriter.beginObject()
                jsonDataWriter.name("conversationId").value(friendFeedEntry.key)
                jsonDataWriter.name("conversationName").value(friendFeedEntry.feedDisplayName)

                var index = 0

                jsonDataWriter.name("participants").apply {
                    beginObject()
                    conversationParticipants.forEach { (userId, friendInfo) ->
                        jsonDataWriter.name(userId).beginObject()
                        jsonDataWriter.name("id").value(index)
                        jsonDataWriter.name("displayName").value(friendInfo.displayName)
                        jsonDataWriter.name("username").value(friendInfo.usernameForSorting)
                        jsonDataWriter.name("bitmojiSelfieId").value(friendInfo.bitmojiSelfieId)
                        jsonDataWriter.endObject()
                        participants[userId] = index++
                    }
                    endObject()
                }

                jsonDataWriter.name("messages").beginArray()

                if (exportParams.exportFormat != ExportFormat.HTML) return
                outputFileStream.write("""
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <meta http-equiv="X-UA-Compatible" content="IE=edge">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title></title>
                    </head>
                """.trimIndent().toByteArray())

                outputFileStream.write("<!-- This file was generated by SnapEnhance ${BuildConfig.VERSION_NAME} -->\n</head>".toByteArray())

                outputFileStream.flush()
            }
        }
    }

    private val downloadedMediaIdCache = CopyOnWriteArraySet<String>()
    private val pendingDownloadMediaIdCache = CopyOnWriteArraySet<String>()

    private fun downloadMedia(message: Message) {
        downloadThreadExecutor.execute {
            MessageDecoder.decode(message.messageContent!!).forEach decode@{ attachment ->
                if (attachment.mediaUniqueId in downloadedMediaIdCache || attachment.mediaUniqueId in pendingDownloadMediaIdCache) return@decode
                pendingDownloadMediaIdCache.add(attachment.mediaUniqueId!!)
                for (i in 0..5) {
                    printLog("downloading ${attachment.boltKey ?: attachment.directUrl}... (attempt ${i + 1}/5)")
                    runCatching {
                        runBlocking {
                            attachment.openStream { downloadedInputStream, _ ->
                                MediaDownloaderHelper.getSplitElements(downloadedInputStream!!) { type, splitInputStream ->
                                    val mediaKey = "${type}_${attachment.mediaUniqueId}"
                                    val bufferedInputStream = BufferedInputStream(splitInputStream)
                                    val fileType = MediaDownloaderHelper.getFileType(bufferedInputStream)
                                    val mediaFile = cacheFolder.resolve("$mediaKey.${fileType.fileExtension}")

                                    mediaFile.outputStream().use { fos ->
                                        bufferedInputStream.copyTo(fos)
                                    }

                                    writeThreadExecutor.execute {
                                        outputFileStream.write("<div class=\"media-$mediaKey\"><!-- ".toByteArray())
                                        mediaFile.inputStream().use {
                                            val deflateInputStream = DeflaterInputStream(it, Deflater(Deflater.BEST_SPEED, true))
                                            (newBase64InputStream.newInstance(
                                                deflateInputStream,
                                                android.util.Base64.DEFAULT or android.util.Base64.NO_WRAP,
                                                true
                                            ) as InputStream).copyTo(outputFileStream)
                                            outputFileStream.write(" --></div>\n".toByteArray())
                                            outputFileStream.flush()
                                        }
                                    }
                                }
                                writeThreadExecutor.execute {
                                    downloadedMediaIdCache.add(attachment.mediaUniqueId!!)
                                }
                            }
                        }
                        return@decode
                    }.onFailure {
                        downloadedMediaIdCache.remove(attachment.mediaUniqueId!!)
                        printLog("failed to download media ${attachment.boltKey}. retrying...")
                        it.printStackTrace()
                    }
                }
                pendingDownloadMediaIdCache.remove(attachment.mediaUniqueId!!)
            }
        }
    }

    fun readMessage(message: Message) {
        if (exportParams.exportFormat == ExportFormat.TEXT) {
            val (displayName, senderUsername) = conversationParticipants[message.senderId.toString()]?.let {
                it.displayName to it.mutableUsername
            } ?: ("" to message.senderId.toString())

            val date = DateFormat.getDateTimeInstance().format(Date(message.messageMetadata!!.createdAt ?: -1))
            outputFileStream.write("[$date] - $displayName ($senderUsername): ${message.serialize() ?: message.messageContent?.contentType?.name}\n".toByteArray(Charsets.UTF_8))
            return
        }
        val contentType = message.messageContent?.contentType ?: return

        if (exportParams.downloadMedias && (contentType == ContentType.NOTE ||
                    contentType == ContentType.SNAP ||
                    contentType == ContentType.EXTERNAL_MEDIA ||
                    contentType == ContentType.STICKER ||
                    contentType == ContentType.SHARE ||
                    contentType == ContentType.MAP_REACTION)
            ) {
            downloadMedia(message)
        }

        jsonDataWriter.apply {
            beginObject()
            name("orderKey").value(message.orderKey)
            name("senderId").value(participants.getOrDefault(message.senderId.toString(), -1))
            name("type").value(message.messageContent!!.contentType.toString())

            fun addUUIDList(name: String, list: List<SnapUUID>) {
                name(name).beginArray()
                list.map { participants.getOrDefault(it.toString(), -1) }.forEach { value(it) }
                endArray()
            }

            addUUIDList("savedBy", message.messageMetadata!!.savedBy!!)
            addUUIDList("seenBy", message.messageMetadata!!.seenBy!!)
            addUUIDList("openedBy", message.messageMetadata!!.openedBy!!)

            name("reactions").beginObject()
            message.messageMetadata!!.reactions!!.forEach { reaction ->
                name(participants.getOrDefault(reaction.userId.toString(), -1L).toString()).value(reaction.reactionId)
            }
            endObject()

            name("createdTimestamp").value(message.messageMetadata!!.createdAt)
            name("readTimestamp").value(message.messageMetadata!!.readAt)
            name("serializedContent").value(message.serialize())
            name("rawContent").value(Base64.UrlSafe.encode(message.messageContent!!.content!!))
            name("attachments").beginArray()
            MessageDecoder.decode(message.messageContent!!)
                .forEach attachments@{ attachments ->
                    beginObject()
                    name("url").value(attachments.boltKey ?: attachments.directUrl)
                    name("key").value(attachments.mediaUniqueId)
                    name("type").value(attachments.type.toString())
                    name("encryption").apply {
                        attachments.attachmentInfo?.encryption?.let { encryption ->
                            beginObject()
                            name("key").value(encryption.key)
                            name("iv").value(encryption.iv)
                            endObject()
                        } ?: nullValue()
                    }
                    endObject()
                }
            endArray()
            endObject()
            flush()
        }
    }

    fun awaitDownload() {
        downloadThreadExecutor.shutdown()
        downloadThreadExecutor.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.NANOSECONDS)
        writeThreadExecutor.shutdown()
        writeThreadExecutor.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.NANOSECONDS)
    }

    fun close() {
        if (exportParams.exportFormat != ExportFormat.TEXT) {
            jsonDataWriter.endArray()
            jsonDataWriter.endObject()
            jsonDataWriter.flush()
            jsonDataWriter.close()
        }

        if (exportParams.exportFormat == ExportFormat.JSON) {
            conversationJsonDataFile.inputStream().use {
                it.copyTo(outputFileStream)
            }
        }

        if (exportParams.exportFormat == ExportFormat.HTML) {
            //write the json file
            outputFileStream.write("<script type=\"application/json\" class=\"exported_content\">".toByteArray())

            (newBase64OutputStream.newInstance(
                outputFileStream,
                android.util.Base64.DEFAULT or android.util.Base64.NO_WRAP,
                true
            ) as OutputStream).let { outputStream ->
                val deflateOutputStream = DeflaterOutputStream(outputStream, Deflater(Deflater.BEST_SPEED, true), true)
                conversationJsonDataFile.inputStream().use { fileInputStream ->
                    val buffer = ByteArray(4096)
                    var length: Int
                    while (fileInputStream.read(buffer).also { length = it } > 0) {
                        deflateOutputStream.write(buffer, 0, length)
                        deflateOutputStream.flush()
                    }
                }
                deflateOutputStream.finish()
                outputStream.flush()
            }

            outputFileStream.write("</script>\n".toByteArray())
            printLog("writing template...")

            runCatching {
                ZipFile(context.bridgeClient.getApplicationApkPath()).use { apkFile ->
                    //export rawinflate.js
                    apkFile.getEntry("assets/web/rawinflate.js")?.let { entry ->
                        outputFileStream.write("<script>".toByteArray())
                        apkFile.getInputStream(entry).copyTo(outputFileStream)
                        outputFileStream.write("</script>\n".toByteArray())
                    }

                    //export avenir next font
                    apkFile.getEntry("assets/web/avenir_next_medium.ttf")?.let { entry ->
                        val encodedFontData = Base64.Default.encode(apkFile.getInputStream(entry).readBytes())
                        outputFileStream.write("""
                            <style>
                                @font-face {
                                    font-family: 'Avenir Next';
                                    src: url('data:font/truetype;charset=utf-8;base64, $encodedFontData');
                                    font-weight: normal;
                                    font-style: normal;
                                }
                            </style>
                        """.trimIndent().toByteArray())
                    }

                    apkFile.getEntry("assets/web/export_template.html")?.let { entry ->
                        apkFile.getInputStream(entry).copyTo(outputFileStream)
                    }

                    apkFile.close()
                }
            }.onFailure {
                throw Throwable("Failed to read template from apk", it)
            }

            outputFileStream.write("</html>".toByteArray())
        }

        outputFileStream.flush()
        outputFileStream.close()
    }
}