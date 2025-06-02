package me.rhunk.snapenhance.ui.manager.pages.scripting

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.*
import me.rhunk.snapenhance.common.scripting.type.ModuleInfo
import me.rhunk.snapenhance.common.scripting.ui.EnumScriptInterface
import me.rhunk.snapenhance.common.scripting.ui.InterfaceManager
import me.rhunk.snapenhance.common.scripting.ui.ScriptInterface
import me.rhunk.snapenhance.common.ui.AsyncUpdateDispatcher
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableState
import me.rhunk.snapenhance.common.ui.rememberAsyncUpdateDispatcher
import me.rhunk.snapenhance.common.util.ktx.getUrlFromClipboard
import me.rhunk.snapenhance.storage.isScriptEnabled
import me.rhunk.snapenhance.storage.setScriptEnabled
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.util.ActivityLauncherHelper
import me.rhunk.snapenhance.ui.util.Dialog
import me.rhunk.snapenhance.ui.util.chooseFolder
import me.rhunk.snapenhance.ui.util.pullrefresh.PullRefreshIndicator
import me.rhunk.snapenhance.ui.util.pullrefresh.pullRefresh
import me.rhunk.snapenhance.ui.util.pullrefresh.rememberPullRefreshState

class ScriptingRootSection : Routes.Route() {
    private lateinit var activityLauncherHelper: ActivityLauncherHelper
    private val reloadDispatcher = AsyncUpdateDispatcher(updateOnFirstComposition = false)

    override val init: () -> Unit = {
        activityLauncherHelper = ActivityLauncherHelper(context.activity!!)
    }

    @Composable
    private fun ImportRemoteScript(
        dismiss: () -> Unit
    ) {
        Dialog(onDismissRequest = dismiss) {
            var url by remember { mutableStateOf("") }
            val focusRequester = remember { FocusRequester() }
            var isLoading by remember {
                mutableStateOf(false)
            }
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Import Script from URL",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp),
                    )
                    Text(
                        text = "Warning: Imported scripts can be harmful to your device. Only import scripts from trusted sources.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Light,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(8.dp),
                        textAlign = TextAlign.Center,
                    )
                    TextField(
                        value = url,
                        onValueChange = {
                            url = it
                        },
                        label = {
                            Text(text = "Enter URL here:")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onGloballyPositioned {
                                focusRequester.requestFocus()
                            }
                    )
                    LaunchedEffect(Unit) {
                        context.androidContext.getUrlFromClipboard()?.let {
                            url = it
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        enabled = url.isNotBlank(),
                        onClick = {
                            isLoading = true
                            context.coroutineScope.launch {
                                runCatching {
                                    val moduleInfo = context.scriptManager.importFromUrl(url)
                                    context.shortToast("Script ${moduleInfo.name} imported!")
                                    reloadDispatcher.dispatch()
                                    withContext(Dispatchers.Main) {
                                        dismiss()
                                    }
                                    return@launch
                                }.onFailure {
                                    context.log.error("Failed to import script", it)
                                    context.shortToast("Failed to import script. ${it.message}. Check logs for more details")
                                }
                                isLoading = false
                            }
                        },
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(30.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(text = "Import")
                        }
                    }
                }
            }
        }
    }


    @Composable
    private fun ModuleActions(
        script: ModuleInfo,
        canUpdate: Boolean,
        dismiss: () -> Unit
    ) {
        Dialog(
            onDismissRequest = dismiss,
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(2.dp),
            ) {
                val actions = remember {
                    mutableMapOf<Pair<String, ImageVector>, suspend () -> Unit>().apply {
                        if (canUpdate) {
                            put("Update Module" to Icons.Rounded.Download) {
                                dismiss()
                                context.shortToast("Updating script ${script.name}...")
                                runCatching {
                                    val modulePath = context.scriptManager.getModulePath(script.name) ?: throw Exception("Module not found")
                                    context.scriptManager.unloadScript(modulePath)
                                    val moduleInfo = context.scriptManager.importFromUrl(script.updateUrl!!, filepath = modulePath)
                                    context.shortToast("Updated ${script.name} to version ${moduleInfo.version}")
                                    context.database.setScriptEnabled(script.name, false)
                                    withContext(context.database.executor.asCoroutineDispatcher()) {
                                        reloadDispatcher.dispatch()
                                    }
                                }.onFailure {
                                    context.log.error("Failed to update module", it)
                                    context.shortToast("Failed to update module. Check logs for more details")
                                }
                            }
                        }

                        put("Edit Module" to Icons.Rounded.Edit) {
                            runCatching {
                                val modulePath = context.scriptManager.getModulePath(script.name)!!
                                context.androidContext.startActivity(
                                    Intent(Intent.ACTION_VIEW).apply {
                                        data = context.scriptManager.getScriptsFolder()!!
                                            .findFile(modulePath)!!.uri
                                        flags =
                                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    }
                                )
                                dismiss()
                            }.onFailure {
                                context.log.error("Failed to open module file", it)
                                context.shortToast("Failed to open module file. Check logs for more details")
                            }
                        }
                        put("Clear Module Data" to Icons.Rounded.Save) {
                            runCatching {
                                context.scriptManager.getModuleDataFolder(script.name)
                                    .deleteRecursively()
                                context.shortToast("Module data cleared!")
                                dismiss()
                            }.onFailure {
                                context.log.error("Failed to clear module data", it)
                                context.shortToast("Failed to clear module data. Check logs for more details")
                            }
                        }
                        put("Delete Module" to Icons.Rounded.DeleteOutline) {
                            context.scriptManager.apply {
                                runCatching {
                                    val modulePath = getModulePath(script.name)!!
                                    unloadScript(modulePath)
                                    getScriptsFolder()?.findFile(modulePath)?.delete()
                                    reloadDispatcher.dispatch()
                                    context.shortToast("Deleted script ${script.name}!")
                                    dismiss()
                                }.onFailure {
                                    context.log.error("Failed to delete module", it)
                                    context.shortToast("Failed to delete module. Check logs for more details")
                                }
                            }
                        }
                    }.toMap()
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Text(
                            text = "Actions",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }
                    items(actions.size) { index ->
                        val action = actions.entries.elementAt(index)
                        ListItem(
                            modifier = Modifier
                                .clickable {
                                    context.coroutineScope.launch {
                                        action.value()
                                        dismiss()
                                    }
                                }
                                .fillMaxWidth(),
                            leadingContent = {
                                Icon(
                                    imageVector = action.key.second,
                                    contentDescription = action.key.first
                                )
                            },
                            headlineContent = {
                                Text(text = action.key.first)
                            },
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun ModuleItem(script: ModuleInfo) {
        var enabled by rememberAsyncMutableState(defaultValue = false, keys = arrayOf(script)) {
            context.database.isScriptEnabled(script.name)
        }
        var openSettings by remember(script) { mutableStateOf(false) }
        var openActions by remember { mutableStateOf(false) }

        val dispatcher = rememberAsyncUpdateDispatcher()
        val reloadCallback = remember { suspend { dispatcher.dispatch() } }
        val latestUpdate by rememberAsyncMutableState(defaultValue = null, updateDispatcher = dispatcher, keys = arrayOf(script)) {
            context.scriptManager.checkForUpdate(script)
        }

        LaunchedEffect(Unit) {
            reloadDispatcher.addCallback(reloadCallback)
        }

        DisposableEffect(Unit) {
            onDispose {
                reloadDispatcher.removeCallback(reloadCallback)
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (!enabled) return@clickable
                        openSettings = !openSettings
                    }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (enabled) {
                    Icon(
                        imageVector = if (openSettings) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(32.dp),
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(text = script.displayName ?: script.name, fontSize = 20.sp)
                    if (!script.description.isNullOrEmpty()) {
                        Text(text = script.description.let { "$it" }, fontSize = 13.sp)
                    }
                    Text(text = script.author.let { "By: $it" }, fontSize = 13.sp)
                    Text(text = script.version.let { "Version: $it" }, fontSize = 13.sp)
                    if (!script.note.isNullOrEmpty()) {
                        Text(text = script.note.let { "Note: $it" }, fontSize = 13.sp)
                    }
                    if (script.notice.isNotEmpty()) {
                        for (notice in script.notice) {
                            when (notice) {
                                "unstable" -> Text(text = "\u26A0 Unstable", fontSize = 12.sp, color = Color(0xFFFFFB87))
                                "ban_risk" -> Text(text = "\u26A0 This script may cause bans", fontSize = 12.sp, color = Color(0xFFFF8585))
                                "internal_behavior" -> Text(text = "\u26A0 This script may break Snapchat's internal behavior", fontSize = 12.sp, color = Color(0xFFFFFB87))
                                else -> {
                                    context.log.error("notice: $notice is not supported")
                                }
                            }
                        }
                    }
                    latestUpdate?.let {
                        Text(text = "Update available: ${it.version}", fontSize = 14.sp, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = {
                    openActions = !openActions
                }) {
                    Icon(imageVector = Icons.Rounded.Build, contentDescription = "Actions")
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { isChecked ->
                        openSettings = false
                        context.coroutineScope.launch(Dispatchers.IO) {
                            runCatching {
                                val modulePath = context.scriptManager.getModulePath(script.name)!!
                                context.scriptManager.unloadScript(modulePath)
                                if (isChecked) {
                                    context.scriptManager.loadScript(modulePath)
                                    context.scriptManager.runtime.getModuleByName(script.name)
                                        ?.callFunction("module.onSnapEnhanceLoad")
                                    context.shortToast("Loaded script ${script.name}")
                                } else {
                                    context.shortToast("Unloaded script ${script.name}")
                                }

                                context.database.setScriptEnabled(script.name, isChecked)
                                withContext(Dispatchers.Main) {
                                    enabled = isChecked
                                }
                            }.onFailure { throwable ->
                                withContext(Dispatchers.Main) {
                                    enabled = !isChecked
                                }
                                ("Failed to ${if (isChecked) "enable" else "disable"} script. Check logs for more details").also {
                                    context.log.error(it, throwable)
                                    context.shortToast(it)
                                }
                            }
                        }
                    }
                )
            }

            if (openSettings) {
                ScriptSettings(script)
            }
        }

        if (openActions) {
            ModuleActions(
                script = script,
                canUpdate = latestUpdate != null,
            ) { openActions = false }
        }
    }

    override val floatingActionButton: @Composable () -> Unit = {
        var showImportDialog by remember {
            mutableStateOf(false)
        }
        if (showImportDialog) {
            ImportRemoteScript {
                showImportDialog = false
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            ExtendedFloatingActionButton(
                onClick = {
                    showImportDialog = true
                },
                icon = { Icon(imageVector = Icons.Rounded.Link, contentDescription = "Link") },
                text = {
                    Text(text = "Import from URL")
                },
            )
            ExtendedFloatingActionButton(
                onClick = {
                    context.scriptManager.getScriptsFolder()?.let {
                        context.androidContext.startActivity(
                            Intent(Intent.ACTION_VIEW).apply {
                                data = it.uri
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        )
                    }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = "Folder"
                    )
                },
                text = {
                    Text(text = "Open Scripts Folder")
                },
            )
        }
    }


    @Composable
    fun ScriptSettings(script: ModuleInfo) {
        val settingsInterface = remember {
            val module =
                context.scriptManager.runtime.getModuleByName(script.name) ?: return@remember null
            (module.getBinding(InterfaceManager::class))?.buildInterface(EnumScriptInterface.SETTINGS)
        }

        if (settingsInterface == null) {
            Text(
                text = "This module does not have any settings",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp)
            )
        } else {
            ScriptInterface(interfaceBuilder = settingsInterface)
        }
    }

    override val content: @Composable (NavBackStackEntry) -> Unit = {
        val scriptingFolder by rememberAsyncMutableState(
            defaultValue = null,
            updateDispatcher = reloadDispatcher
        ) {
            context.scriptManager.getScriptsFolder()
        }
        val scriptModules by rememberAsyncMutableState(
            defaultValue = emptyList(),
            updateDispatcher = reloadDispatcher
        ) {
            context.scriptManager.sync()
            context.scriptManager.getSyncedModules()
        }

        val coroutineScope = rememberCoroutineScope()

        var refreshing by remember {
            mutableStateOf(false)
        }

        LaunchedEffect(Unit) {
            refreshing = true
            withContext(Dispatchers.IO) {
                reloadDispatcher.dispatch()
                refreshing = false
            }
        }

        val pullRefreshState = rememberPullRefreshState(refreshing, onRefresh = {
            refreshing = true
            coroutineScope.launch(Dispatchers.IO) {
                reloadDispatcher.dispatch()
                refreshing = false
            }
        })

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    if (scriptingFolder == null && !refreshing) {
                        Text(
                            text = "No scripts folder selected",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            activityLauncherHelper.chooseFolder {
                                context.config.root.scripting.moduleFolder.set(it)
                                context.config.writeConfig()
                                coroutineScope.launch {
                                    reloadDispatcher.dispatch()
                                }
                            }
                        }) {
                            Text(text = "Select folder")
                        }
                    } else if (scriptModules.isEmpty()) {
                        Text(
                            text = "No scripts found",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                items(scriptModules.size, key = { scriptModules[it].hashCode() }) { index ->
                    ModuleItem(scriptModules[index])
                }
                item {
                    Spacer(modifier = Modifier.height(200.dp))
                }
            }

            PullRefreshIndicator(
                refreshing = refreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        var scriptingWarning by remember {
            mutableStateOf(context.sharedPreferences.run {
                getBoolean("scripting_warning", true).also {
                    edit().putBoolean("scripting_warning", false).apply()
                }
            })
        }

        if (scriptingWarning) {
            var timeout by remember {
                mutableIntStateOf(10)
            }

            LaunchedEffect(Unit) {
                while (timeout > 0) {
                    delay(1000)
                    timeout--
                }
            }

            AlertDialog(onDismissRequest = {
                if (timeout == 0) {
                    scriptingWarning = false
                }
            }, title = {
                Text(text = context.translation["manager.dialogs.scripting_warning.title"])
            }, text = {
                Text(text = context.translation["manager.dialogs.scripting_warning.content"])
            }, confirmButton = {
                TextButton(
                    onClick = {
                        scriptingWarning = false
                    },
                    enabled = timeout == 0
                ) {
                    Text(text = "OK " + if (timeout > 0) "($timeout)" else "")
                }
            })
        }
    }

    override val topBarActions: @Composable() (RowScope.() -> Unit) = {
        IconButton(onClick = {
            context.androidContext.startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = "https://github.com/SE-Extended/Scripts".toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.LibraryBooks,
                contentDescription = "Documentation"
            )
        }
    }
}