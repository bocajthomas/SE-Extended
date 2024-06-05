package me.rhunk.snapenhance.ui.manager.pages

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.bridge.DownloadCallback
import me.rhunk.snapenhance.common.data.download.DownloadMetadata
import me.rhunk.snapenhance.common.data.download.MediaDownloadSource
import me.rhunk.snapenhance.common.data.download.createNewFilePath
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableState
import me.rhunk.snapenhance.common.util.ktx.longHashCode
import me.rhunk.snapenhance.download.DownloadProcessor
import me.rhunk.snapenhance.download.FFMpegProcessor
import me.rhunk.snapenhance.task.*
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.util.OnLifecycleEvent
import me.rhunk.snapenhance.ui.util.coil.cacheKey
import java.io.File
import java.util.UUID
import kotlin.math.absoluteValue

class TasksRootSection : Routes.Route() {
    private var activeTasks by mutableStateOf(listOf<PendingTask>())
    private lateinit var recentTasks: MutableList<Task>
    private val taskSelection = mutableStateListOf<Pair<Task, DocumentFile?>>()

    private fun fetchActiveTasks(scope: CoroutineScope = context.coroutineScope) {
        scope.launch(Dispatchers.IO) {
            activeTasks = context.taskManager.getActiveTasks().values.sortedByDescending { it.taskId }.toMutableList()
        }
    }

    private fun mergeSelection(selection: List<Pair<Task, DocumentFile>>) {
        val firstTask = selection.first().first

        val taskHash = UUID.randomUUID().toString().longHashCode().absoluteValue.toString(16)
        val pendingTask = context.taskManager.createPendingTask(
            Task(TaskType.DOWNLOAD, "Merge ${selection.size} files", firstTask.author, taskHash)
        )
        pendingTask.status = TaskStatus.RUNNING
        fetchActiveTasks()

        context.coroutineScope.launch {
            val filesToMerge = mutableListOf<File>()

            selection.forEach { (task, documentFile) ->
                val tempFile = File.createTempFile(task.hash, "." + documentFile.name?.substringAfterLast("."), context.androidContext.cacheDir).also {
                    it.deleteOnExit()
                }

                runCatching {
                    pendingTask.updateProgress("Copying ${documentFile.name}")
                    context.androidContext.contentResolver.openInputStream(documentFile.uri)?.use { inputStream ->
                        //copy with progress
                        val length = documentFile.length().toFloat()
                        tempFile.outputStream().use { outputStream ->
                            val buffer = ByteArray(16 * 1024)
                            var read: Int
                            while (inputStream.read(buffer).also { read = it } != -1) {
                                outputStream.write(buffer, 0, read)
                                pendingTask.updateProgress("Copying ${documentFile.name}", (outputStream.channel.position().toFloat() / length * 100f).toInt())
                            }
                            outputStream.flush()
                            filesToMerge.add(tempFile)
                        }
                    }
                }.onFailure {
                    pendingTask.fail("Failed to copy file $documentFile to $tempFile")
                    filesToMerge.forEach { it.delete() }
                    return@launch
                }
            }

            val mergedFile = File.createTempFile("merged", ".mp4", context.androidContext.cacheDir).also {
                it.deleteOnExit()
            }

            runCatching {
                context.shortToast(translation.format("merge_files_toast", "count" to filesToMerge.size.toString()))
                FFMpegProcessor.newFFMpegProcessor(context, pendingTask).execute(
                    FFMpegProcessor.Request(FFMpegProcessor.Action.MERGE_MEDIA, filesToMerge.map { it.absolutePath }, mergedFile)
                )
                DownloadProcessor(context, object: DownloadCallback.Default() {
                    override fun onSuccess(outputPath: String) {
                        context.log.verbose("Merged files to $outputPath")
                    }
                }).saveMediaToGallery(pendingTask, mergedFile, DownloadMetadata(
                    mediaIdentifier = taskHash,
                    outputPath = createNewFilePath(
                        context.config.root,
                        taskHash,
                        downloadSource = MediaDownloadSource.MERGED,
                        mediaAuthor = firstTask.author,
                        creationTimestamp = System.currentTimeMillis()
                    ),
                    mediaAuthor = firstTask.author,
                    downloadSource = MediaDownloadSource.MERGED.translate(context.translation),
                    iconUrl = null
                ))
            }.onFailure {
                context.log.error("Failed to merge files", it)
                pendingTask.fail(it.message ?: "Failed to merge files")
            }.onSuccess {
                pendingTask.success()
            }
            filesToMerge.forEach { it.delete() }
            mergedFile.delete()
        }.also {
            pendingTask.addListener(PendingTaskListener(onCancel = { it.cancel() }))
        }
    }

    override val topBarActions: @Composable (RowScope.() -> Unit) = {
        var showConfirmDialog by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()

        if (taskSelection.size > 1) {
            val canMergeSelection by rememberAsyncMutableState(defaultValue = false, keys = arrayOf(taskSelection.size)) {
                taskSelection.all { it.second?.type?.contains("video") == true }
            }

            if (canMergeSelection) {
                IconButton(onClick = {
                    mergeSelection(taskSelection.toList().also {
                        taskSelection.clear()
                    }.map { it.first to it.second!! })
                }) {
                    Icon(Icons.Filled.Merge, contentDescription = "Merge")
                }
            }
        }

        IconButton(onClick = {
            showConfirmDialog = true
        }) {
            Icon(Icons.Filled.Delete, contentDescription = "Clear tasks")
        }

        if (showConfirmDialog) {
            var alsoDeleteFiles by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = {
                    if (taskSelection.isNotEmpty()) {
                        Text(translation.format("remove_selected_tasks_confirm", "count" to taskSelection.size.toString()))
                    } else {
                        Text(translation["remove_all_tasks_confirm"])
                    }
                },
                text = {
                    Column {
                        if (taskSelection.isNotEmpty()) {
                            Text(translation["remove_selected_tasks_title"])
                            Row (
                                modifier = Modifier
                                    .padding(top = 10.dp)
                                    .fillMaxWidth()
                                    .clickable {
                                        alsoDeleteFiles = !alsoDeleteFiles
                                    },
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = alsoDeleteFiles, onCheckedChange = {
                                    alsoDeleteFiles = it
                                })
                                Text(translation["delete_files_option"])
                            }
                        } else {
                            Text(translation["remove_all_tasks_title"])
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmDialog = false
                            if (taskSelection.isNotEmpty()) {
                                taskSelection.forEach { (task, documentFile) ->
                                    coroutineScope.launch(Dispatchers.IO) {
                                        context.taskManager.removeTask(task)
                                        if (alsoDeleteFiles) {
                                            documentFile?.delete()
                                        }
                                    }
                                    recentTasks.remove(task)
                                }
                                activeTasks = activeTasks.filter { task -> !taskSelection.map { it.first }.contains(task.task) }
                                taskSelection.clear()
                            } else {
                                coroutineScope.launch(Dispatchers.IO) {
                                    context.taskManager.clearAllTasks()
                                }
                                recentTasks.clear()
                                activeTasks.forEach {
                                    runCatching {
                                        it.cancel()
                                    }.onFailure { throwable ->
                                        context.log.error("Failed to cancel task $it", throwable)
                                    }
                                }
                                activeTasks = listOf()
                                context.taskManager.getActiveTasks().clear()
                            }
                        }
                    ) {
                        Text(context.translation["button.positive"])
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            showConfirmDialog = false
                        }
                    ) {
                        Text(context.translation["button.negative"])
                    }
                }
            )
        }
    }

    @Composable
    private fun TaskCard(modifier: Modifier, task: Task, pendingTask: PendingTask? = null) {
        var taskStatus by remember { mutableStateOf(task.status) }
        var taskProgressLabel by remember { mutableStateOf<String?>(null) }
        var taskProgress by remember { mutableIntStateOf(-1) }
        val isSelected by remember { derivedStateOf { taskSelection.any { it.first == task } } }

        var documentFileMimeType by remember { mutableStateOf("") }
        var isDocumentFileReadable by remember { mutableStateOf(true) }
        val documentFile by rememberAsyncMutableState(defaultValue = null, keys = arrayOf(taskStatus.key)) {
            DocumentFile.fromSingleUri(context.androidContext, task.extra?.toUri() ?: return@rememberAsyncMutableState null)?.apply {
                documentFileMimeType = type ?: ""
                isDocumentFileReadable = canRead()
            }
        }


        val listener = remember { PendingTaskListener(
            onStateChange = {
                taskStatus = it
            },
            onProgress = { label, progress ->
                taskProgressLabel = label
                taskProgress = progress
            }
        ) }

        LaunchedEffect(Unit) {
            pendingTask?.addListener(listener)
        }

        DisposableEffect(Unit) {
            onDispose {
                pendingTask?.removeListener(listener)
            }
        }

        fun toggleSelection() {
            if (isSelected) {
                taskSelection.removeIf { it.first == task }
                return
            }
            taskSelection.add(task to documentFile)
        }

        fun openFile() {
            if (!isDocumentFileReadable || documentFile == null) return
            runCatching {
                context.androidContext.startActivity(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(documentFile!!.uri, documentFile!!.type)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }.onFailure {
                context.log.error("Failed to open file ${documentFile?.uri}", it)
                context.shortToast(translation["failed_to_open_file"])
            }
        }

        OutlinedCard(modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (taskSelection.size > 0) {
                            toggleSelection()
                            return@detectTapGestures
                        }
                        openFile()
                    },
                    onLongPress = {
                        if (taskSelection.size > 0) {
                            openFile()
                            return@detectTapGestures
                        }
                        toggleSelection()
                    }
                )
            }
            .let {
                if (isSelected) {
                    it
                        .border(2.dp, MaterialTheme.colorScheme.primary)
                        .clip(MaterialTheme.shapes.medium)
                } else it
            }
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 15.dp)
                        .size(50.dp)
                        .clipToBounds(),
                    contentAlignment = Alignment.Center
                ) {
                    var loadFailed by remember { mutableStateOf(false) }
                    documentFile?.let {
                        if (taskStatus.isFinalStage() && isDocumentFileReadable && !loadFailed && (documentFileMimeType.contains("image") || documentFileMimeType.contains("video"))) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(context.androidContext)
                                        .data(it.uri)
                                        .cacheKey(it.uri.toString())
                                        .placeholder(ColorDrawable(MaterialTheme.colorScheme.surfaceVariant.toArgb()))
                                        .build(),
                                    imageLoader = context.imageLoader,
                                    onError = { loadFailed = true }
                                ),
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(MaterialTheme.shapes.medium)
                            )
                        } else {
                            when {
                                !isDocumentFileReadable -> Icon(Icons.Filled.DeleteOutline, contentDescription = "File not found")
                                documentFileMimeType.contains("image") -> Icon(Icons.Filled.Image, contentDescription = "Image")
                                documentFileMimeType.contains("video") -> Icon(Icons.Filled.Videocam, contentDescription = "Video")
                                documentFileMimeType.contains("audio") -> Icon(Icons.Filled.MusicNote, contentDescription = "Audio")
                                else -> Icon(Icons.Filled.FileCopy, contentDescription = "File")
                            }
                        }
                    } ?: run {
                        when (task.type) {
                            TaskType.DOWNLOAD -> Icon(Icons.Filled.Download, contentDescription = "Download")
                            TaskType.CHAT_ACTION -> Icon(Icons.Filled.ChatBubble, contentDescription = "Chat Action")
                        }
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(task.title, style = MaterialTheme.typography.bodyMedium)
                        task.author?.takeIf { it != "null" }?.let {
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(task.hash, style = MaterialTheme.typography.labelSmall)
                    Column(
                        modifier = Modifier.padding(top = 5.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (taskStatus.isFinalStage()) {
                            if (taskStatus != TaskStatus.SUCCESS) {
                                Text("$taskStatus", style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            taskProgressLabel?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                            if (taskProgress != -1) {
                                LinearProgressIndicator(
                                    progress = { taskProgress.toFloat() / 100f },
                                    strokeCap = StrokeCap.Round,
                                )
                            } else {
                                task.extra?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                Column {
                    if (pendingTask != null && !taskStatus.isFinalStage()) {
                        FilledIconButton(onClick = {
                            runCatching {
                                pendingTask.cancel()
                            }.onFailure { throwable ->
                                context.log.error("Failed to cancel task $pendingTask", throwable)
                            }
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel")
                        }
                    } else {
                        when (taskStatus) {
                            TaskStatus.SUCCESS -> Icon(Icons.Filled.Check, contentDescription = "Success", tint = MaterialTheme.colorScheme.primary)
                            TaskStatus.FAILURE -> Icon(Icons.Filled.Error, contentDescription = "Failure", tint = MaterialTheme.colorScheme.error)
                            TaskStatus.CANCELLED -> Icon(Icons.Filled.Cancel, contentDescription = "Cancelled", tint = MaterialTheme.colorScheme.error)
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    override val content: @Composable (NavBackStackEntry) -> Unit = {
        val scrollState = rememberLazyListState()
        val scope = rememberCoroutineScope()
        recentTasks = rememberSaveable { mutableStateListOf() }
        var lastFetchedTaskId: Long? by rememberSaveable { mutableStateOf(null) }

        fun fetchNewRecentTasks() {
            scope.launch(Dispatchers.IO) {
                val tasks = context.taskManager.fetchStoredTasks(lastFetchedTaskId ?: Long.MAX_VALUE, limit = 20)
                if (tasks.isNotEmpty()) {
                    lastFetchedTaskId = tasks.keys.last()
                    val activeTaskIds = activeTasks.map { it.taskId }
                    recentTasks.addAll(tasks.filter { it.key !in activeTaskIds }.values)
                }
            }
        }

        LaunchedEffect(Unit) {
            fetchActiveTasks(this)
        }

        DisposableEffect(Unit) {
            onDispose {
                taskSelection.clear()
            }
        }

        OnLifecycleEvent { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                fetchActiveTasks(scope)
            }
        }

        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                if (activeTasks.isEmpty() && recentTasks.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        translation["no_tasks"].let {
                            Icon(Icons.Filled.CheckCircle, contentDescription = it, tint = MaterialTheme.colorScheme.primary)
                            Text(it, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            items(activeTasks, key = { it.taskId }) {pendingTask ->
                TaskCard(modifier = Modifier.padding(8.dp), pendingTask.task, pendingTask = pendingTask)
            }
            items(recentTasks, key = { it.hash }) { task ->
                TaskCard(modifier = Modifier.padding(8.dp), task)
            }
            item {
                Spacer(modifier = Modifier.height(40.dp))
                LaunchedEffect(remember { derivedStateOf { scrollState.firstVisibleItemIndex } }) {
                    fetchNewRecentTasks()
                }
            }
        }
    }
}