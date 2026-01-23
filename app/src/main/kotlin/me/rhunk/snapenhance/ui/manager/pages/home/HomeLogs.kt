@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package me.rhunk.snapenhance.ui.manager.pages.home

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowDown
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.rhunk.snapenhance.LogReader
import me.rhunk.snapenhance.common.logger.LogChannel
import me.rhunk.snapenhance.common.logger.LogLevel
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.util.ActivityLauncherHelper
import me.rhunk.snapenhance.ui.util.pullrefresh.PullRefreshIndicator
import me.rhunk.snapenhance.ui.util.pullrefresh.rememberPullRefreshState
import me.rhunk.snapenhance.ui.util.saveFile
import me.rhunk.snapenhance.common.ui.lazyColumnContentPadding
import androidx.core.net.toUri

class HomeLogs : Routes.Route() {
    private val logListState by lazy { LazyListState(0) }
    private lateinit var activityLauncherHelper: ActivityLauncherHelper

    override val init: () -> Unit = {
        activityLauncherHelper = ActivityLauncherHelper(context.activity!!)
    }

    override val topBarActions: @Composable (RowScope.() -> Unit) = {
        var showDropDown by remember { mutableStateOf(false) }

        IconButton(
            onClick = { showDropDown = true },
            shapes = IconButtonDefaults.shapes()
        ) {
            Icon(Icons.Rounded.MoreVert, contentDescription = null)
        }

        DropdownMenu(
            expanded = showDropDown,
            onDismissRequest = { showDropDown = false },
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            DropdownMenuItem(onClick = {
                context.coroutineScope.launch {
                    context.log.clearLogs()
                }
                navigateReload()
                showDropDown = false
            }, text = {
                Text(translation["clear_logs_button"])
            })

            DropdownMenuItem(onClick = {
                activityLauncherHelper.saveFile("SE Extended-logs-${System.currentTimeMillis()}.zip", "application/zip") { uri ->
                    context.coroutineScope.launch {
                        context.shortToast(translation["saving_logs_toast"])
                        context.androidContext.contentResolver.openOutputStream(uri.toUri())?.use {
                            runCatching {
                                context.log.exportLogsToZip(it)
                                context.longToast(translation["saved_logs_success_toast"])
                            }.onFailure {
                                context.longToast(translation["saved_logs_failure_toast"])
                                context.log.error("Failed to save logs to $uri!", it)
                            }
                        }
                    }
                }
                showDropDown = false
            }, text = {
                Text(translation["export_logs_button"])
            })
        }
    }

    override val content: @Composable (NavBackStackEntry) -> Unit = {
        val coroutineScope = rememberCoroutineScope()
        val clipboardManager = LocalClipboardManager.current
        var lineCount by remember { mutableIntStateOf(0) }
        var logReader by remember { mutableStateOf<LogReader?>(null) }
        var isRefreshing by remember { mutableStateOf(false) }

        fun refreshLogs() {
            coroutineScope.launch(Dispatchers.IO) {
                runCatching {
                    logReader = context.log.newReader {
                        lineCount++
                    }
                    lineCount = logReader!!.lineCount
                }.onFailure {
                    context.longToast("Failed to read logs!")
                }
                delay(300)
                isRefreshing = false
                withContext(Dispatchers.Main) {
                    logListState.scrollToItem((logListState.layoutInfo.totalItemsCount - 1).takeIf { it >= 0 } ?: return@withContext)
                }
            }
        }

        val pullRefreshState = rememberPullRefreshState(isRefreshing, onRefresh = {
            refreshLogs()
        })

        LaunchedEffect(Unit) {
            isRefreshing = true
            refreshLogs()
        }
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.horizontalScroll(ScrollState(0)),
                contentPadding = lazyColumnContentPadding(staticVertical = 10.dp),
                state = logListState
            ) {
                item {
                    if (lineCount == 0 && logReader != null) {
                        Text(
                            text = translation["no_logs_hint"],
                            modifier = Modifier.padding(16.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                }
                items(lineCount) { index ->
                    val logLine by remember(index) {
                        mutableStateOf(runBlocking(Dispatchers.IO) {
                            logReader?.getLogLine(index)
                        })
                    }
                    logLine?.let { line ->
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        coroutineScope.launch {
                                            clipboardManager.setText(
                                                AnnotatedString(
                                                    line.message
                                                )
                                            )
                                        }
                                    }
                                )
                            }) {
                            Column(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 30.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = when (line.logLevel) {
                                            LogLevel.DEBUG -> Icons.Rounded.BugReport
                                            LogLevel.ERROR, LogLevel.ASSERT -> Icons.Rounded.Report
                                            LogLevel.INFO, LogLevel.VERBOSE -> Icons.Rounded.Info
                                            LogLevel.WARN -> Icons.Rounded.Warning
                                            else -> Icons.Rounded.Info
                                        },
                                        modifier = Modifier.size(16.dp),
                                        contentDescription = null,
                                    )

                                    Text(
                                        text = LogChannel.fromChannel(line.tag)?.shortName ?: line.tag,
                                        modifier = Modifier.padding(start = 4.dp),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                    )

                                    Text(
                                        text = line.dateTime,
                                        modifier = Modifier.padding(start = 4.dp, end = 4.dp),
                                        fontSize = 10.sp
                                    )
                                }

                                Text(
                                    text = line.message.trimIndent(),
                                    lineHeight = 10.sp,
                                    fontSize = 9.sp,
                                    maxLines = Int.MAX_VALUE,
                                )
                            }
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    override val floatingActionButton: @Composable () -> Unit = {
        val coroutineScope = rememberCoroutineScope()
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            val firstVisibleItem by remember { derivedStateOf { logListState.firstVisibleItemIndex } }
            val layoutInfo by remember { derivedStateOf { logListState.layoutInfo } }
            FilledIconButton(
                onClick = {
                    coroutineScope.launch {
                        logListState.scrollToItem(0)
                    }
                },
                shapes = IconButtonDefaults.shapes(),
                enabled = firstVisibleItem != 0
            ) {
                Icon(Icons.Rounded.KeyboardDoubleArrowUp, contentDescription = null)
            }

            FilledIconButton(
                onClick = {
                    coroutineScope.launch {
                        logListState.scrollToItem((logListState.layoutInfo.totalItemsCount - 1).takeIf { it >= 0 } ?: return@launch)
                    }
                },
                shapes = IconButtonDefaults.shapes(),
                enabled = layoutInfo.visibleItemsInfo.lastOrNull()?.index != layoutInfo.totalItemsCount - 1
            ) {
                Icon(Icons.Rounded.KeyboardDoubleArrowDown, contentDescription = null)
            }
        }
    }
}