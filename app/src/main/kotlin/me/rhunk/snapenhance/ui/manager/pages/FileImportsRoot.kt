@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package me.rhunk.snapenhance.ui.manager.pages

import android.text.format.Formatter
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.common.ui.AsyncUpdateDispatcher
import me.rhunk.snapenhance.common.ui.cardShape
import me.rhunk.snapenhance.common.ui.lazyColumnContentPadding
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableState
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableStateList
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.util.ActivityLauncherHelper
import me.rhunk.snapenhance.ui.util.openFile
import java.text.DateFormat

class FileImportsRoot: Routes.Route() {
    private lateinit var activityLauncherHelper: ActivityLauncherHelper
    private val reloadDispatcher = AsyncUpdateDispatcher()

    override val init: () -> Unit = {
        activityLauncherHelper = ActivityLauncherHelper(context.activity!!)
    }

    override val floatingActionButton: @Composable () -> Unit = {
        val coroutineScope = rememberCoroutineScope()

        ExtendedFloatingActionButton(
            onClick = {
                context.coroutineScope.launch {
                    activityLauncherHelper.openFile { filePath ->
                        val fileUri = filePath.toUri()
                        runCatching {
                            DocumentFile.fromSingleUri(context.activity!!, fileUri)?.let { file ->
                                if (!file.exists()) {
                                    context.shortToast(translation["file_not_found"])
                                    return@openFile
                                }
                                context.fileHandleManager.importFile(file.name!!) {
                                    context.androidContext.contentResolver.openInputStream(fileUri)
                                        ?.use { inputStream ->
                                            inputStream.copyTo(this)
                                        }
                                }
                            }
                        }.onFailure {
                            context.log.error("Failed to import file", it)
                            context.shortToast(
                                translation.format(
                                    "file_import_failed",
                                    "error" to it.message.toString()
                                )
                            )
                        }.onSuccess {
                            context.shortToast(translation["file_imported"])
                            coroutineScope.launch {
                                reloadDispatcher.dispatch()
                            }
                        }
                    }
                }
            },
            modifier = Modifier.offset(x = 2.dp, y = 2.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = { Icon(Icons.Rounded.Upload, contentDescription = null) },
            text = { Text(translation["import_file_button"]) },
        )
    }

    override val content: @Composable (NavBackStackEntry) -> Unit = {
        val files = rememberAsyncMutableStateList(defaultValue = listOf(), updateDispatcher = reloadDispatcher) {
            context.fileHandleManager.getStoredFiles()
        }

        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            contentPadding = lazyColumnContentPadding(staticVertical = 10.dp, hasFAB = true)
        ) {
            item {
                if (files.isEmpty()) {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            ElevatedCard(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .size(200.dp),
                                shape = MaterialShapes.Cookie12Sided.toShape()
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FolderOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(100.dp)
                                    )
                                }
                            }
                            Text(
                                text = translation["files_empty"],
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(8.dp)
                            )
                            Text(
                                text = translation["files_empty_desc"],
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
            itemsIndexed(files) { index, file ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = cardShape(files.size, index)
                ) {
                    val fileInfo by rememberAsyncMutableState(defaultValue = null) {
                        context.fileHandleManager.getFileInfo(file.name)
                    }
                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.AttachFile, contentDescription = null, modifier = Modifier.padding(5.dp))
                        Column(
                            modifier = Modifier.weight(1f).padding(8.dp),
                        ) {
                            Text(text = file.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 20.sp)
                            fileInfo?.let { (size, lastModified) ->
                                Text(text = "${Formatter.formatFileSize(context.androidContext, size)} - ${DateFormat.getDateTimeInstance().format(lastModified)}", lineHeight = 15.sp)
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    context.coroutineScope.launch {
                                        if (context.fileHandleManager.deleteFile(file.name)) {
                                            files.remove(file)
                                        } else {
                                            context.shortToast(translation["file_delete_failed"])
                                        }
                                    }
                                },
                                shapes = IconButtonDefaults.shapes()
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}