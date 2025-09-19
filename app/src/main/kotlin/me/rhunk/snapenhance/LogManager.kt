package me.rhunk.snapenhance

import android.util.Log
import com.google.gson.GsonBuilder
import me.rhunk.snapenhance.common.data.FileType
import me.rhunk.snapenhance.common.logger.AbstractLogger
import me.rhunk.snapenhance.common.logger.LogChannel
import me.rhunk.snapenhance.common.logger.LogLevel
import java.io.File
import java.io.OutputStream
import java.io.RandomAccessFile
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.time.Duration.Companion.hours

class LogLine(
    val logLevel: LogLevel,
    val dateTime: String,
    val tag: String,
    val message: String
) {
    companion object {
        fun fromString(line: String) = runCatching {
            val parts = line.trimEnd().split("/")
            if (parts.size != 4) return@runCatching null
            LogLine(
                LogLevel.fromLetter(parts[0]) ?: return@runCatching null,
                parts[1],
                parts[2],
                parts[3]
            )
        }.getOrNull()
    }

    override fun toString(): String {
        return "${logLevel.letter}/$dateTime/$tag/$message"
    }
}


class LogReader(
    logFile: File
) {
    private val randomAccessFile = RandomAccessFile(logFile, "r")
    private var startLineIndexes = mutableListOf<Long>()
    var lineCount = queryLineCount()

    private fun readLogLine(): LogLine? {
        val lines = StringBuilder()
        val lastPointer = randomAccessFile.filePointer
        var lastChar: Int = -1
        var bufferLength = 0
        while (true) {
            val char = randomAccessFile.read()
            if (char == -1) {
                randomAccessFile.seek(lastPointer)
                return null
            }
            if ((char == '|'.code && lastChar == '\n'.code) || bufferLength > 4096) {
                break
            }
            lines.append(char.toChar())
            bufferLength++
            lastChar = char
        }

        return LogLine.fromString(lines.trimEnd().toString())
            ?: LogLine(LogLevel.ERROR, "1970-01-01 00:00:00", "LogReader", "Failed to parse log line: $lines")
    }

    fun incrementLineCount() {
        synchronized(randomAccessFile) {
            randomAccessFile.seek(randomAccessFile.length())
            startLineIndexes.add(randomAccessFile.filePointer + 1)
            lineCount++
        }
    }

    private fun queryLineCount(): Int {
        val buffer = ByteArray(1024 * 1024)

        synchronized(randomAccessFile) {
            randomAccessFile.seek(0)
            var lineCount = 0
            var read: Int
            var lastPointer: Long = 0
            var line: StringBuilder? = null

            while (randomAccessFile.read(buffer).also { read = it } != -1) {
                for (i in 0 until read) {
                    val char = buffer[i].toInt().toChar()
                    if (line == null) {
                        line = StringBuilder()
                        lastPointer = randomAccessFile.filePointer - read + i
                    }
                    line.append(char)
                    if (char == '\n') {
                        if (line.startsWith('|')) {
                            lineCount++
                            startLineIndexes.add(lastPointer + 1)
                        }
                        line = null
                    }
                }
            }

            return lineCount
        }
    }

    private fun getLine(index: Int): String? {
        if (index <= 0 || index > lineCount) return null
        synchronized(randomAccessFile) {
            randomAccessFile.seek(startLineIndexes.getOrNull(index) ?: return null)
            return readLogLine()?.toString()
        }
    }

    fun getLogLine(index: Int): LogLine? {
        return getLine(index)?.let { LogLine.fromString(it) }
    }
}


class LogManager(
    private val remoteSideContext: RemoteSideContext
): AbstractLogger(LogChannel.MANAGER) {
    companion object {
        private val LOG_LIFETIME = 24.hours
    }

    private val printLogLock = Any()
    private val anonymizeLogs by lazy { !remoteSideContext.config.root.scripting.disableLogAnonymization.get() }

    var lineAddListener = { _: LogLine -> }

    private val logFolder = File(remoteSideContext.androidContext.cacheDir, "logs")
    private var logFile: File? = null

    private val uuidRegex by lazy { Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", RegexOption.MULTILINE) }
    private val contentUriRegex by lazy { Regex("content://[a-zA-Z0-9_\\-./]+") }
    private val filePathRegex by lazy { Regex("([a-zA-Z0-9_\\-./]+)\\.(${FileType.entries.joinToString("|") { file -> file.fileExtension.toString() }})") }

    fun init() {
        if (!logFolder.exists()) {
            logFolder.mkdirs()
        }
        logFile = remoteSideContext.sharedPreferences.getString("log_file", null)?.let { File(it) }?.takeIf { it.exists() } ?: run {
            newLogFile()
            logFile
        }

        if (System.currentTimeMillis() - remoteSideContext.sharedPreferences.getLong("last_created", 0) > LOG_LIFETIME.inWholeMilliseconds) {
            newLogFile()
        }
    }

    fun internalLog(tag: String, logLevel: LogLevel, message: Any?) {
        synchronized(printLogLock) {
            runCatching {
                val anonymizedMessage = message.toString().let {
                    if (remoteSideContext.config.isInitialized() && anonymizeLogs)
                        it.replace(uuidRegex, "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
                            .replace(contentUriRegex, "content://xxx")
                            .replace(filePathRegex, "xxxxxxxx.$2")
                    else it
                }
                val line = LogLine(
                    logLevel = logLevel,
                    dateTime = getCurrentDateTime(),
                    tag = tag,
                    message = anonymizedMessage
                )
                logFile?.appendText("|$line\n", Charsets.UTF_8)
                lineAddListener(line)
                Log.println(logLevel.priority, tag, anonymizedMessage)
            }.onFailure {
                Log.println(Log.ERROR, tag, "Failed to log message: $message")
                Log.println(Log.ERROR, tag, it.stackTraceToString())
            }
        }
    }

    private fun getCurrentDateTime(pathSafe: Boolean = false): String {
        return DateTimeFormatter.ofPattern(if (pathSafe) "yyyy-MM-dd_HH-mm-ss" else "yyyy-MM-dd HH:mm:ss").format(
            java.time.LocalDateTime.now()
        )
    }

    private fun newLogFile() {
        val currentTime = System.currentTimeMillis()
        logFile = File(logFolder, "SE Extended-${getCurrentDateTime(pathSafe = true)}.txt").also {
            it.createNewFile()
            remoteSideContext.sharedPreferences.edit().putString("log_file", it.absolutePath).putLong("last_created", currentTime).apply()
        }
    }

    fun clearLogs() {
        logFolder.listFiles()?.forEach { it.delete() }
        newLogFile()
    }

    fun exportLogsToZip(outputStream: OutputStream) {
        val zipOutputStream = ZipOutputStream(outputStream).apply {
            setMethod(ZipOutputStream.DEFLATED)
        }

        // add device info to zip
        zipOutputStream.putNextEntry(ZipEntry("device-info.json"))
        val gson = GsonBuilder().setPrettyPrinting().create()
        zipOutputStream.write(gson.toJson(remoteSideContext.installationSummary).toByteArray())
        zipOutputStream.closeEntry()

        // add config
        zipOutputStream.putNextEntry(ZipEntry("config.json"))
        zipOutputStream.write(remoteSideContext.config.exportToString(exportSensitiveData = false).toByteArray())
        zipOutputStream.closeEntry()

        //add logFolder to zip
        logFolder.walk().forEach {
            if (it.isFile) {
                zipOutputStream.putNextEntry(ZipEntry(it.name))
                it.inputStream().copyTo(zipOutputStream)
                zipOutputStream.closeEntry()
            }
        }

        zipOutputStream.close()
    }

    fun newReader(onAddLine: (LogLine) -> Unit) = LogReader(logFile!!).also {
        lineAddListener = { line -> it.incrementLineCount(); onAddLine(line) }
    }

    override fun debug(message: Any?, tag: String) {
        internalLog(tag, LogLevel.DEBUG, message)
    }

    override fun error(message: Any?, tag: String) {
        internalLog(tag, LogLevel.ERROR, message)
    }

    override fun error(message: Any?, throwable: Throwable, tag: String) {
        internalLog(tag, LogLevel.ERROR, message)
        internalLog(tag, LogLevel.ERROR, throwable.stackTraceToString())
    }

    override fun info(message: Any?, tag: String) {
        internalLog(tag, LogLevel.INFO, message)
    }

    override fun verbose(message: Any?, tag: String) {
        internalLog(tag, LogLevel.VERBOSE, message)
    }

    override fun warn(message: Any?, tag: String) {
        internalLog(tag, LogLevel.WARN, message)
    }

    override fun assert(message: Any?, tag: String) {
        internalLog(tag, LogLevel.ASSERT, message)
    }
}