@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package me.rhunk.snapenhance.ui.manager.pages.home

import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Merge
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rhunk.snapenhance.common.action.EnumAction
import me.rhunk.snapenhance.common.bridge.InternalFileHandleType
import me.rhunk.snapenhance.common.ui.*
import me.rhunk.snapenhance.ui.components.DefaultBottomSheet
import me.rhunk.snapenhance.ui.components.LazyColumnBottomSheet
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.setup.Requirements
import me.rhunk.snapenhance.ui.util.*
import kotlin.collections.forEachIndexed

class HomeSettings : Routes.Route() {
    private lateinit var activityLauncherHelper: ActivityLauncherHelper
    private val bottomSheets by lazy { BottomSheets(context.translation) }

    override val init: () -> Unit = {
        activityLauncherHelper = ActivityLauncherHelper(context.activity!!)
    }

    @Composable
    private fun RowTitle(title: String) {
        Text(text = title, modifier = Modifier.padding(16.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }

    @Composable
    private fun PreferenceToggle(sharedPreferences: SharedPreferences, key: String, text: String) {
        val realKey = "debug_$key"
        var value by remember { mutableStateOf(sharedPreferences.getBoolean(realKey, false)) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 55.dp)
                .clickable {
                    value = !value
                    sharedPreferences
                        .edit() {
                            putBoolean(realKey, value)
                        }
                }
                .padding(start = 15.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = text, fontSize = 14.sp)
            Switch(checked = value, onCheckedChange = {
                value = it
                sharedPreferences.edit().putBoolean(realKey, it).apply()
            })
        }

    }

    @Composable
    private fun RowAction(
        key: String,
        requireConfirmation: Boolean = false,
        action: () -> Unit
    ) {
        var confirmationDialog by remember {
            mutableStateOf(false)
        }

        fun takeAction() {
            if (requireConfirmation) {
                confirmationDialog = true
            } else {
                action()
            }
        }

        if (requireConfirmation && confirmationDialog) {
            Dialog(onDismissRequest = { confirmationDialog = false }) {
                bottomSheets.ConfirmBottomSheet(title = context.translation["manager.dialogs.action_confirm.title"], onConfirm = {
                    action()
                    confirmationDialog = false
                }, onDismiss = {
                    confirmationDialog = false
                })
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { takeAction() }
                .padding(15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(text = context.translation["actions.$key.name"], fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
                context.translation.getOrNull("actions.$key.description")?.let { Text(text = it, fontSize = 12.sp, fontWeight = FontWeight.Light, lineHeight = 15.sp) }
            }
            IconButton(
                onClick = { takeAction() },
                modifier = Modifier.padding(end = 2.dp),
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    @Composable
    fun DebugCardList() {
        var showPasswordBottomSheet by remember { mutableStateOf(false) }
        val advancedDebugPassword = "notevenhidden"
        var imputedPassword by remember { mutableStateOf("") }

        fun checkPassword() {
            if (imputedPassword == advancedDebugPassword) {
                showPasswordBottomSheet = false
                imputedPassword = ""
                routes.homeAdvancedDebugSettings.navigate()
            } else {
                context.shortToast("Wrong password")
                imputedPassword = ""
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = cardShapeGroupedTop
            ) {
                PreferenceToggle(context.sharedPreferences, "log_resources", "Log Resources")
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = cardShapeGroupedMiddle
            ) {
                PreferenceToggle(context.sharedPreferences, "disable_feature_loading", "Disable Feature Loading")
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = cardShapeGroupedMiddle
            ) {
                PreferenceToggle(context.sharedPreferences, "disable_mapper", "Disable Auto Mapper")
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = cardShapeGroupedBottom
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPasswordBottomSheet = true }
                        .heightIn(min = 55.dp)
                        .padding(start = 15.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = "Advanced Debug Settings",
                            fontSize = 14.sp
                        )
                    }

                    IconButton(
                        onClick = { showPasswordBottomSheet = true },
                        modifier = Modifier.padding(end = 2.dp),
                        shapes = IconButtonDefaults.shapes()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            if (showPasswordBottomSheet) {
                LazyColumnBottomSheet(
                    onDismiss = { showPasswordBottomSheet = false }
                ) {
                    Column {
                        Text(
                            text = "To protect you as a user some debug settings requires a password to use",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    OutlinedTextField(
                        value = imputedPassword,
                        onValueChange = { imputedPassword = it },
                        label = { Text("Enter Password") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        singleLine = true,
                    )

                    HorizontalDivider(
                        modifier = Modifier
                            .padding(top = 15.dp, bottom = 10.dp)
                            .fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .padding(top = 10.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                showPasswordBottomSheet = false
                                imputedPassword = ""
                            },
                            shapes = ButtonDefaults.shapes(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text(text = "Cancel")
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(onClick = { checkPassword() }, shapes = ButtonDefaults.shapes()) {
                            Text(text = "Done")
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun GeneralCardList() {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = cardShapeGroupedTop
            ) {
                RowAction(key = "regen_mappings") {
                    context.checkForRequirements(Requirements.MAPPINGS)
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = cardShapeGroupedMiddle
            ) {
                RowAction(key = "change_language") {
                    context.checkForRequirements(Requirements.LANGUAGE)
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = cardShapeGroupedMiddle
            ) {
                RowAction(key = "disclaimer_statement") {
                    context.checkForRequirements(Requirements.DISCLAIMER)
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = cardShapeGroupedMiddle
            ) {
                RowAction(key = "appearance") {
                    routes.homeAppearance.navigate()
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = cardShapeGroupedMiddle
            ) {
                RowAction(key = "updating") {
                    routes.homeUpdating.navigate()
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = cardShapeGroupedMiddle
            ) {
                RowAction(key = "credits") {
                    routes.homeCredits.navigate()
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = cardShapeGroupedBottom
            ) {
                RowAction(key = "about") {
                    routes.homeAbout.navigate()
                }
            }
        }
    }

    override val content: @Composable (NavBackStackEntry) -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .screenContentPadding(staticVertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RowTitle(title = translation["general_title"])
            GeneralCardList()

            RowTitle(title = translation["actions_title"])
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                EnumAction.entries.forEachIndexed { index, enumAction ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = cardShape(EnumAction.entries.size, index)
                    ) {
                        RowAction(key = enumAction.key) {
                            context.launchActionIntent(enumAction)
                        }
                    }
                }
            }
            RowTitle(title = translation["message_logger_title"])
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                var storedMessagesCount by rememberAsyncMutableState(defaultValue = 0) {
                    context.messageLogger.getStoredMessageCount()
                }
                var storedStoriesCount by rememberAsyncMutableState(defaultValue = 0) {
                    context.messageLogger.getStoredStoriesCount()
                }
                var showMessageLoggerSettings by remember { mutableStateOf(false) }
                var showImportDatabaseWarning by remember { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()

                if (showMessageLoggerSettings) {
                    LazyColumnBottomSheet(
                        onDismiss = { showMessageLoggerSettings = false }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = cardShapeGroupedTop
                            ) {
                                ListItem(
                                    colors = ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showImportDatabaseWarning = true },
                                    leadingContent = {
                                        Icon(
                                            imageVector = Icons.Rounded.FileUpload,
                                            contentDescription = context.translation["manager.dialogs.message_logger_settings.import_database_action"]
                                        )
                                    },
                                    headlineContent = {
                                        Text(text = context.translation["manager.dialogs.message_logger_settings.import_database_action"])
                                    },
                                )
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = cardShapeGroupedMiddle
                            ) {
                                ListItem(
                                    colors = ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            coroutineScope.launch { showMessageLoggerSettings = false }.invokeOnCompletion { showMessageLoggerSettings = false }
                                            runCatching {
                                                activityLauncherHelper.saveFile(
                                                    "message_logger.db",
                                                    "application/octet-stream"
                                                ) { uri ->
                                                    context.androidContext.contentResolver.openOutputStream(
                                                        uri.toUri()
                                                    )?.use { outputStream ->
                                                        context.messageLogger.databaseFile.inputStream()
                                                            .use { inputStream ->
                                                                inputStream.copyTo(
                                                                    outputStream
                                                                )
                                                            }
                                                    }
                                                }
                                                context.shortToast(translation["export_success_toast"])
                                            }.onFailure {
                                                context.log.error("Failed to export database", it)
                                                context.longToast(translation["export_failed_toast"])
                                            }
                                        },
                                    leadingContent = {
                                        Icon(
                                            imageVector = Icons.Rounded.FileDownload,
                                            contentDescription = context.translation["manager.dialogs.message_logger_settings.export_database_action"]
                                        )
                                    },
                                    headlineContent = {
                                        Text(text = context.translation["manager.dialogs.message_logger_settings.export_database_action"])
                                    },
                                )
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = cardShapeGroupedMiddle
                            ) {
                                ListItem(
                                    colors = ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            coroutineScope.launch { showMessageLoggerSettings = false }
                                            runCatching {
                                                activityLauncherHelper.openMultipleFiles(type = "*/*") { inputUriStrings ->
                                                    if (inputUriStrings.isEmpty()) return@openMultipleFiles
                                                    val inputUris = inputUriStrings.map { it.toUri() }
                                                    coroutineScope.launch(Dispatchers.Main) {
                                                        delay(150)
                                                        activityLauncherHelper.createFile(
                                                            fileName = "message_logger_merged_${System.currentTimeMillis() / 1000}.db",
                                                            mimeType = "application/octet-stream"
                                                        ) { outputUriString ->
                                                           // if (outputUriString == null) return@createFile

                                                            coroutineScope.launch(Dispatchers.IO) {
                                                                val isSuccessful = context.messageLogger.mergeDatabases(
                                                                    context.androidContext,
                                                                    inputUris,
                                                                    outputUriString.toUri()
                                                                ) { percent, status ->
                                                                    context.log.info("Merge database progress [$percent%]: $status")
                                                                }

                                                                withContext(Dispatchers.Main) {
                                                                    if (isSuccessful) {
                                                                        context.shortToast(translation["merge_success_toast"])
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            } .onFailure { error ->
                                                context.log.error("Failed to merge databases", error)
                                                context.longToast(translation["merge_failed_toast"])
                                            }
                                        },
                                    leadingContent = {
                                        Icon(
                                            imageVector = Icons.Rounded.Merge,
                                            contentDescription = context.translation["manager.dialogs.message_logger_settings.merge_database_action"]
                                        )
                                    },
                                    headlineContent = {
                                        Text(text = context.translation["manager.dialogs.message_logger_settings.merge_database_action"])
                                    },
                                )
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = cardShapeGroupedBottom
                            ) {
                                ListItem(
                                    colors = ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            coroutineScope.launch { showMessageLoggerSettings = false }.invokeOnCompletion { showMessageLoggerSettings = false }
                                            runCatching {
                                                context.messageLogger.purgeAll()
                                                storedMessagesCount = 0
                                                storedStoriesCount = 0
                                            }.onFailure {
                                                context.log.error("Failed to clear database", it)
                                                context.longToast(translation["clear_failed_toast"])
                                            }.onSuccess {
                                                context.shortToast(translation["clear_success_toast"])
                                            }
                                        },
                                    leadingContent = {
                                        Icon(
                                            imageVector = Icons.Rounded.Clear,
                                            contentDescription = context.translation["manager.dialogs.message_logger_settings.clear_database_action"]
                                        )
                                    },
                                    headlineContent = {
                                        Text(text = context.translation["manager.dialogs.message_logger_settings.clear_database_action"])
                                    },
                                )
                            }
                        }
                    }
                }

                if (showImportDatabaseWarning) {
                    DefaultBottomSheet(
                        onDismiss = { showImportDatabaseWarning = false }
                    ) {
                        bottomSheets.ConfirmBottomSheet(
                            title = context.translation["manager.dialogs.message_logger_import_warning.title"],
                            message = context.translation["manager.dialogs.message_logger_import_warning.content"],
                            onConfirm = {
                                showImportDatabaseWarning = false
                                showMessageLoggerSettings = false

                                context.coroutineScope.launch(Dispatchers.IO) {
                                    runCatching {
                                        activityLauncherHelper.openFile("application/octet-stream") { uri ->
                                            val contentResolver = context.androidContext.contentResolver
                                            contentResolver.openInputStream(uri.toUri())?.use { inputStream ->
                                                contentResolver.openFileDescriptor(context.messageLogger.databaseFile.toUri(), "rwt")?.use { pfd ->
                                                    java.io.FileOutputStream(pfd.fileDescriptor).use { outputStream ->
                                                        inputStream.copyTo(outputStream)
                                                        outputStream.flush()
                                                        pfd.fileDescriptor.sync()
                                                    }
                                                }
                                            }

                                            context.coroutineScope.launch(Dispatchers.Main) {
                                                context.shortToast(translation["import_success_toast"])
                                                delay(1000)
                                                val packageManager = context.androidContext.packageManager
                                                val intent = packageManager.getLaunchIntentForPackage(context.androidContext.packageName)
                                                val componentName = intent?.component
                                                val restartIntent = android.content.Intent.makeRestartActivityTask(componentName)
                                                context.androidContext.startActivity(restartIntent)
                                                Runtime.getRuntime().exit(0)
                                            }
                                        }
                                    }.onFailure {
                                        context.log.error("Failed to import database", it)
                                        context.coroutineScope.launch(Dispatchers.Main) {
                                            context.longToast(translation["import_failed_toast"])
                                        }
                                    }
                                }
                            },
                            onDismiss = {
                                showImportDatabaseWarning = false
                            }
                        )

                    }
                }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = cardShapeGroupedTop
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            translation.format("message_logger_message_count",
                                "messageCount" to if (storedMessagesCount > 999999) "999999+" else storedMessagesCount.toString(),
                            )
                        )

                        Text(
                            translation.format("message_logger_story_count",
                                "storyCount" to if (storedStoriesCount > 999999) "999999+" else storedStoriesCount.toString(),
                            )
                        )
                    }
                }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = cardShapeGroupedBottom
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp, 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showMessageLoggerSettings = true },
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Icon(imageVector = Icons.Rounded.Settings, contentDescription = null)
                        }

                        OutlinedButton(
                            onClick = { routes.loggerHistory.navigate() },
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Text(translation["view_logger_history_button"])
                        }
                    }
                }
            }

            RowTitle(title = translation["debug_title"])

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = cardShapeSingle,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { routes.liquidGlassSettingsRoot.navigate() }
                        .padding(15.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = "Debug Liquid Glass Settings",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 20.sp
                        )
                        Text(
                            text = "Used for debugging Liquid Glass UI components",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Light,
                            lineHeight = 15.sp
                        )
                    }

                    IconButton(
                        onClick = { routes.liquidGlassSettingsRoot.navigate() },
                        modifier = Modifier.padding(end = 2.dp),
                        shapes = IconButtonDefaults.shapes()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = cardShapeSingle
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(start = 26.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
                ) {
                    var selectedFileType by remember { mutableStateOf(InternalFileHandleType.entries.first()) }
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        var expanded by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            modifier = Modifier.fillMaxWidth(0.7f)
                        ) {
                            OutlinedTextField(
                                value = selectedFileType.fileName,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                }
                            )

                            DropdownMenuPopup(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                val fileTypes = InternalFileHandleType.entries.toList()

                                DropdownMenuGroup(
                                    shapes = MenuDefaults.groupShape(0, 1)
                                ) {
                                    fileTypes.forEachIndexed { index, fileType ->
                                        val isSelected = selectedFileType == fileType

                                        DropdownMenuItem(
                                            selected = isSelected,
                                            onClick = {
                                                selectedFileType = fileType
                                                expanded = false
                                            },
                                            text = { Text(text = fileType.fileName) },
                                            shapes = MenuDefaults.itemShape(index, fileTypes.size),
                                            leadingIcon = {
                                                val icon = if (fileType.fileName.contains("json", true))
                                                    Icons.Rounded.Code else Icons.Rounded.InsertDriveFile
                                                Icon(icon, contentDescription = null)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Button(onClick = {
                        runCatching {
                            context.coroutineScope.launch {
                                selectedFileType.resolve(context.androidContext).delete()
                            }
                        }.onFailure {
                            context.log.error("Failed to clear file", it)
                            context.longToast("Failed to clear file! ${it.localizedMessage}")
                        }.onSuccess {
                            context.shortToast(translation["success_toast"])
                        }
                    }, shapes = ButtonDefaults.shapes()) {
                        Text(translation["clear_button"])
                    }
                }
            }
            DebugCardList()
        }
    }
}