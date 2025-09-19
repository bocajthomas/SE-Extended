package me.rhunk.snapenhance.ui.manager.pages.tracker

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.google.gson.stream.JsonWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rhunk.snapenhance.RemoteSideContext
import me.rhunk.snapenhance.common.bridge.wrapper.TrackerLog
import me.rhunk.snapenhance.common.data.MessagingFriendInfo
import me.rhunk.snapenhance.common.data.TrackerEventType
import me.rhunk.snapenhance.common.util.snap.BitmojiSelfie
import me.rhunk.snapenhance.storage.getFriendInfo
import me.rhunk.snapenhance.ui.util.ActivityLauncherHelper
import me.rhunk.snapenhance.ui.util.coil.BitmojiImage
import me.rhunk.snapenhance.ui.util.saveFile
import java.text.DateFormat


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsTab(
    context: RemoteSideContext,
    activityLauncherHelper: ActivityLauncherHelper,
    deleteAction: (() -> Unit) -> Unit,
    exportAction: (() -> Unit) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    val logs = remember { mutableStateListOf<TrackerLog>() }
    var isLoading by remember { mutableStateOf(false) }
    var pageIndex by remember { mutableIntStateOf(0) }
    var filterType by remember { mutableStateOf(FriendTrackerManagerRoot.FilterType.USERNAME) }
    var reverseSortOrder by remember { mutableStateOf(true) }
    val sinceDatePickerState = rememberDatePickerState(
        initialDisplayMode = DisplayMode.Picker
    )

    var filter by remember { mutableStateOf("") }
    var searchTimeoutJob by remember { mutableStateOf<Job?>(null) }

    fun getPaginatedLogs(pageIndex: Int) = context.messageLogger.getLogs(
        pageIndex = pageIndex,
        pageSize = 30,
        timestamp = sinceDatePickerState.selectedDateMillis,
        reverseOrder = reverseSortOrder,
        filter = {
        when (filterType) {
            FriendTrackerManagerRoot.FilterType.USERNAME -> it.username.contains(filter, ignoreCase = true)
            FriendTrackerManagerRoot.FilterType.CONVERSATION -> it.conversationTitle?.contains(filter, ignoreCase = true) == true || (it.username == filter && !it.isGroup)
            FriendTrackerManagerRoot.FilterType.EVENT -> it.eventType.contains(filter, ignoreCase = true)
        }
    })

    suspend fun loadNewLogs() {
        withContext(Dispatchers.IO) {
            getPaginatedLogs(pageIndex).let {
                withContext(Dispatchers.Main) {
                    logs.addAll(it)
                    pageIndex += 1
                }
            }
        }
    }

    suspend fun resetAndLoadLogs() {
        isLoading = true
        logs.clear()
        pageIndex = 0
        loadNewLogs()
        isLoading = false
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showExportSelectionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        deleteAction { showDeleteDialog = true }
        exportAction { showExportSelectionDialog = true }
    }

    if (showDeleteDialog) {
        val deleteCoroutineScope = rememberCoroutineScope { Dispatchers.IO }
        var deleteLogsTask by remember { mutableStateOf<Job?>(null) }
        var deletedLogsCount by remember { mutableIntStateOf(0) }

        fun deleteLogs() {
            deleteLogsTask = deleteCoroutineScope.launch {
                var index = 0
                while (true) {
                    val newLogs = getPaginatedLogs(index++)
                    if (newLogs.isEmpty()) {
                        break
                    }
                    newLogs.forEach {
                        context.messageLogger.deleteTrackerLog(it.id)
                        deletedLogsCount++
                    }
                }

                withContext(Dispatchers.Main) {
                    delay(500)
                    resetAndLoadLogs()
                    context.shortToast("Deleted $deletedLogsCount logs")
                    showDeleteDialog = false
                }
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                deleteLogsTask?.cancel()
            }
        }

        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete logs?") },
            text = {
                if (deleteLogsTask != null) {
                    Text("Deleting $deletedLogsCount logs...")
                } else {
                    Text("This will delete logs based on the current filter and the search query. This action cannot be undone.")
                }
            },
            confirmButton = {
                Button(
                    enabled = deleteLogsTask == null,
                    onClick = {
                        deleteLogs()
                    }
                ) {
                    if (deleteLogsTask != null) {
                        CircularProgressIndicator(modifier = Modifier
                            .size(30.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        Text("Delete")
                    }
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) {
                    Text(context.translation["button.cancel"])
                }
            }
        )
    }

    if (showExportSelectionDialog) {
        val exportCoroutineScope = rememberCoroutineScope { Dispatchers.IO }
        var exportTask by remember { mutableStateOf<Job?>(null) }
        var exportType by remember { mutableStateOf("json") }

        fun exportLogs() {
            activityLauncherHelper.saveFile("tracker_logs_${System.currentTimeMillis()}.$exportType") { uri ->
                exportTask = exportCoroutineScope.launch {
                    context.androidContext.contentResolver.openOutputStream(Uri.parse(uri))?.use {
                        val writer = it.writer()
                        val jsonWriter by lazy {
                            JsonWriter(writer).apply {
                                setIndent("  ")
                                beginArray()
                            }
                        }

                        var index = 0
                        while (true) {
                            val newLogs = getPaginatedLogs(index++)
                            if (newLogs.isEmpty()) {
                                break
                            }
                            newLogs.forEach { log ->
                                when (exportType) {
                                    "json" -> {
                                        jsonWriter.jsonValue(log.toJson().toString())
                                    }
                                    "csv" -> {
                                        writer.write(log.toCsv())
                                        writer.write("\n")
                                    }
                                }
                                writer.flush()
                            }
                        }
                        when (exportType) {
                            "json" -> {
                                jsonWriter.endArray()
                                jsonWriter.close()
                            }
                            "csv" -> writer.close()
                        }
                    }
                }.apply {
                    invokeOnCompletion {
                        exportTask = null
                        showExportSelectionDialog = false
                        if (it == null) {
                            context.shortToast("Exported logs!")
                        } else {
                            context.log.error("Failed to export logs", it)
                            context.shortToast("Failed to export logs. Check logcat for more details.")
                        }
                    }
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showExportSelectionDialog = false },
            title = { Text("Export logs?") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (exportTask != null) {
                        Text("Exporting logs...")
                    } else {
                        Text("This will export logs based on the current filter and the search query.")
                        Spacer(modifier = Modifier.height(10.dp))
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                        ) {
                            Card(
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .padding(2.dp)
                            ) {
                                Text("Export as $exportType", modifier = Modifier.padding(8.dp))
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = {
                                expanded = false
                            }) {
                                listOf("json", "csv").forEach { type ->
                                    DropdownMenuItem(onClick = {
                                        exportType = type
                                        expanded = false
                                    }, text = {
                                        Text(type)
                                    })
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = exportTask == null,
                    onClick = {
                        exportLogs()
                    }
                ) {
                    if (exportTask != null) {
                        CircularProgressIndicator(modifier = Modifier
                            .size(30.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        Text("Export")
                    }
                }
            },
            dismissButton = {
                Button(onClick = { showExportSelectionDialog = false }) {
                    Text(context.translation["button.cancel"])
                }
            }
        )
    }


    @Composable
    fun FilterSelection(
        selectionExpanded: MutableState<Boolean>
    ) {
        var dropDownExpanded by remember { mutableStateOf(false) }
        var showDatePicker by remember { mutableStateOf(false) }

        if (showDatePicker) {
            DatePickerDialog(onDismissRequest = {
                showDatePicker = false
            }, confirmButton = {}) {
                DatePicker(
                    state = sinceDatePickerState,
                    modifier = Modifier.weight(1f),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = {
                        showDatePicker = false
                        sinceDatePickerState.selectedDateMillis = null
                    }) {
                        Text(context.translation["button.cancel"])
                    }
                    Button(onClick = {
                        showDatePicker = false
                    }) {
                        Text(context.translation["button.ok"])
                    }
                }
            }
        }

        DropdownMenu(expanded = selectionExpanded.value, onDismissRequest = {
            selectionExpanded.value = false
        }) {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val rowHSpacing = 10.dp

                Text("Filters", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(rowHSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Search by")
                    ExposedDropdownMenuBox(
                        expanded = dropDownExpanded,
                        onExpandedChange = { dropDownExpanded = it },
                    ) {
                        Card(
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .padding(2.dp)
                        ) {
                            Text(filterType.name, modifier = Modifier.padding(8.dp))
                        }
                        DropdownMenu(expanded = dropDownExpanded, onDismissRequest = {
                            dropDownExpanded = false
                        }) {
                            FriendTrackerManagerRoot.FilterType.entries.forEach { type ->
                                DropdownMenuItem(onClick = {
                                    filter = ""
                                    filterType = type
                                    dropDownExpanded = false
                                    coroutineScope.launch {
                                        resetAndLoadLogs()
                                    }
                                }, text = {
                                    Text(type.name)
                                })
                            }
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(rowHSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Newest first")
                    Switch(
                        checked = reverseSortOrder,
                        onCheckedChange = {
                            reverseSortOrder = it
                            selectionExpanded.value = false
                        }
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(rowHSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (reverseSortOrder) "Since" else "Until")
                    Button(onClick = {
                        showDatePicker = true
                    }) {
                        Text(remember(showDatePicker) {
                            sinceDatePickerState.selectedDateMillis?.let {
                                DateFormat.getDateInstance().format(it)
                            } ?: "Pick a date"
                        })
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            var showAutoComplete by remember { mutableStateOf(false) }
            val showFilterSelection = remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = showAutoComplete,
                onExpandedChange = { showAutoComplete = it },
            ) {
                TextField(
                    value = filter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .padding(8.dp),
                    onValueChange = {
                        filter = it
                        coroutineScope.launch {
                            searchTimeoutJob?.cancel()
                            searchTimeoutJob = coroutineScope.launch {
                                delay(200)
                                showAutoComplete = true
                                resetAndLoadLogs()
                            }
                        }
                    },
                    placeholder = { Text("Search") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    maxLines = 1,
                    leadingIcon = {
                        IconButton(
                            onClick = {
                                showFilterSelection.value = !showFilterSelection.value
                            },
                            modifier = Modifier
                                .padding(2.dp)
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                        FilterSelection(showFilterSelection)
                        if (showFilterSelection.value) {
                            DisposableEffect(Unit) {
                                onDispose {
                                    coroutineScope.launch {
                                        resetAndLoadLogs()
                                    }
                                }
                            }
                        }
                    },
                    trailingIcon = {
                        if (filter != "") {
                            IconButton(onClick = {
                                filter = ""
                                coroutineScope.launch {
                                    resetAndLoadLogs()
                                }
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }

                        DropdownMenu(
                            expanded = showAutoComplete,
                            onDismissRequest = {
                                showAutoComplete = false
                            },
                            properties = PopupProperties(focusable = false),
                        ) {
                            val suggestedEntries = remember(filter) {
                                mutableStateListOf<String>()
                            }

                            LaunchedEffect(filter) {
                                launch(Dispatchers.IO) {
                                    suggestedEntries.addAll(when (filterType) {
                                        FriendTrackerManagerRoot.FilterType.USERNAME -> context.messageLogger.findUsername(filter)
                                        FriendTrackerManagerRoot.FilterType.CONVERSATION -> context.messageLogger.findConversation(filter) + context.messageLogger.findUsername(filter)
                                        FriendTrackerManagerRoot.FilterType.EVENT -> TrackerEventType.entries.filter { it.name.contains(filter, ignoreCase = true) }.map { it.key }
                                    }.take(5))
                                }
                            }

                            suggestedEntries.forEach { entry ->
                                DropdownMenuItem(onClick = {
                                    filter = entry
                                    coroutineScope.launch {
                                        resetAndLoadLogs()
                                    }
                                    showAutoComplete = false
                                }, text = {
                                    Text(entry)
                                })
                            }
                        }
                    },
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (logs.isEmpty() && !isLoading) {
                        Text("No logs found", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Light, textAlign = TextAlign.Center)
                    }
                }
            }
            items(logs, key = { it.userId + it.id }) { log ->
                var databaseFriend by remember { mutableStateOf<MessagingFriendInfo?>(null) }
                LaunchedEffect(Unit) {
                    launch(Dispatchers.IO) {
                        databaseFriend = context.database.getFriendInfo(log.userId)
                    }
                }
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        BitmojiImage(
                            modifier = Modifier.padding(5.dp),
                            size = 55,
                            context = context,
                            url = databaseFriend?.takeIf { it.bitmojiId != null }?.let {
                                BitmojiSelfie.getBitmojiSelfie(it.selfieId, it.bitmojiId, BitmojiSelfie.BitmojiSelfieType.NEW_THREE_D)
                            },
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f),
                        ) {
                            Text(databaseFriend?.displayName?.let {
                                "$it (${log.username})"
                            } ?: log.username, lineHeight = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                            Text("${log.eventType} in ${log.conversationTitle}", fontSize = 10.sp, fontWeight = FontWeight.Light, lineHeight = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                DateFormat.getDateTimeInstance().format(log.timestamp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Light,
                                lineHeight = 15.sp,
                            )
                        }

                        IconButton(
                            onClick = {
                                context.messageLogger.deleteTrackerLog(log.id)
                                logs.remove(log)
                            }
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete")
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))

                LaunchedEffect(pageIndex) {
                    loadNewLogs()
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}