package me.rhunk.snapenhance.ui.manager.pages.scripting

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rhunk.snapenhance.common.scripting.type.ModuleInfo
import me.rhunk.snapenhance.common.scripting.ui.EnumScriptInterface
import me.rhunk.snapenhance.common.scripting.ui.InterfaceManager
import me.rhunk.snapenhance.common.scripting.ui.ScriptInterface
import me.rhunk.snapenhance.common.ui.AsyncUpdateDispatcher
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableState
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.util.ActivityLauncherHelper
import me.rhunk.snapenhance.ui.util.chooseFolder
import me.rhunk.snapenhance.ui.util.pullrefresh.PullRefreshIndicator
import me.rhunk.snapenhance.ui.util.pullrefresh.pullRefresh
import me.rhunk.snapenhance.ui.util.pullrefresh.rememberPullRefreshState

class ScriptingRoot : Routes.Route() {
    private lateinit var activityLauncherHelper: ActivityLauncherHelper

    override val init: () -> Unit = {
        activityLauncherHelper = ActivityLauncherHelper(context.activity!!)
    }

    @Composable
    fun ModuleItem(script: ModuleInfo) {
        var enabled by rememberAsyncMutableState(defaultValue = false) {
            context.modDatabase.isScriptEnabled(script.name)
        }
        var openSettings by remember {
            mutableStateOf(false)
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
                        openSettings = !openSettings
                    }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(text = script.displayName ?: script.name, fontSize = 20.sp,)
                    Text(text = script.description ?: "No description", fontSize = 14.sp,)
                }
                IconButton(onClick = { openSettings = !openSettings }) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings",)
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { isChecked ->
                        context.coroutineScope.launch(Dispatchers.IO) {
                            runCatching {
                                context.modDatabase.setScriptEnabled(script.name, isChecked)
                                withContext(Dispatchers.Main) {
                                    enabled = isChecked
                                }
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
                            }.onFailure { throwable ->
                                withContext(Dispatchers.Main) {
                                    enabled = !isChecked
                                }
                                ("Failed to ${if (isChecked) "enable" else "disable"} script").let {
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
    }

    override val floatingActionButton: @Composable () -> Unit = {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            ExtendedFloatingActionButton(
                onClick = {

                },
                icon= { Icon(imageVector = Icons.Default.Link, contentDescription = "Link") },
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
                icon= { Icon(imageVector = Icons.Default.FolderOpen, contentDescription = "Folder") },
                text = {
                    Text(text = "Open Scripts Folder")
                },
            )
        }
    }


    @Composable
    fun ScriptSettings(script: ModuleInfo) {
       val settingsInterface = remember {
            val module = context.scriptManager.runtime.getModuleByName(script.name) ?: return@remember null
            (module.getBinding(InterfaceManager::class))?.buildInterface(EnumScriptInterface.SETTINGS)
        }

        if (settingsInterface == null) {
            Text(
                text = "This module does not have any settings",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp)
            )
        } else  {
            ScriptInterface(interfaceBuilder = settingsInterface)
        }
    }

    override val content: @Composable (NavBackStackEntry) -> Unit = {
        val reloadDispatcher = remember { AsyncUpdateDispatcher(updateOnFirstComposition = false) }
        val scriptingFolder by rememberAsyncMutableState(defaultValue = null, updateDispatcher = reloadDispatcher) {
            context.scriptManager.getScriptsFolder()
        }
        val scriptModules by rememberAsyncMutableState(defaultValue = emptyList(), updateDispatcher = reloadDispatcher) {
            context.scriptManager.sync()
            context.modDatabase.getScripts()
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
                items(scriptModules.size) { index ->
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
                data = "https://github.com/SnapEnhance/docs".toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }) {
            Icon(imageVector = Icons.AutoMirrored.Default.LibraryBooks, contentDescription = "Documentation")
        }
    }
}