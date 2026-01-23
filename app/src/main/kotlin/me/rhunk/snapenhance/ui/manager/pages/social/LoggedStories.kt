@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package me.rhunk.snapenhance.ui.manager.pages.social

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavBackStackEntry
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberAsyncImagePainter
import me.rhunk.snapenhance.bridge.DownloadCallback
import me.rhunk.snapenhance.common.data.FileType
import me.rhunk.snapenhance.common.data.StoryData
import me.rhunk.snapenhance.common.data.download.*
import me.rhunk.snapenhance.common.ui.bottomSheetClipShape
import me.rhunk.snapenhance.common.ui.bottomSheetShape
import me.rhunk.snapenhance.common.util.ktx.longHashCode
import me.rhunk.snapenhance.download.DownloadProcessor
import me.rhunk.snapenhance.storage.getFriendInfo
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.util.coil.ImageRequestHelper
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.UUID
import kotlin.math.absoluteValue

class LoggedStories : Routes.Route() {
    @OptIn(ExperimentalCoilApi::class, ExperimentalLayoutApi::class)
    override val content: @Composable (NavBackStackEntry) -> Unit = content@{ navBackStackEntry ->
        val userId = navBackStackEntry.arguments?.getString("id") ?: return@content

        val stories = remember { mutableStateListOf<StoryData>() }
        val friendInfo = remember { context.database.getFriendInfo(userId) }
        var lastStoryTimestamp by remember { mutableLongStateOf(Long.MAX_VALUE) }

        var selectedStory by remember { mutableStateOf<StoryData?>(null) }

        selectedStory?.let { story ->
            fun downloadSelectedStory(
                inputMedia: InputMedia,
            ) {
                val mediaAuthor = friendInfo?.mutableUsername ?: userId
                val uniqueHash = UUID.randomUUID().toString().longHashCode().absoluteValue.toString(16)

                DownloadProcessor(
                    remoteSideContext = context,
                    callback = object: DownloadCallback.Default() {
                        override fun onSuccess(outputPath: String?) {
                            context.shortToast("Downloaded to $outputPath")
                        }

                        override fun onFailure(message: String?, throwable: String?) {
                            context.shortToast("Failed to download $message")
                        }
                    }
                ).enqueue(DownloadRequest(
                    inputMedias = arrayOf(inputMedia)
                ), DownloadMetadata(
                    mediaIdentifier = uniqueHash,
                    outputPath = createNewFilePath(
                        context.config.root,
                        uniqueHash,
                        MediaDownloadSource.STORY_LOGGER,
                        mediaAuthor,
                        story.createdAt
                    ),
                    iconUrl = null,
                    mediaAuthor = friendInfo?.mutableUsername ?: userId,
                    downloadSource = MediaDownloadSource.STORY_LOGGER.translate(context.translation),
                ))
            }

            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { selectedStory = null  },
                sheetState = sheetState,
                sheetGesturesEnabled = false,
                shape = bottomSheetShape
            ) {
                CompositionLocalProvider(LocalOverscrollFactory provides null) {
                    LazyColumn(
                        modifier = Modifier
                            .padding(10.dp)
                            .clip(bottomSheetClipShape),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        item {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                remember {
                                    story.postedAt.takeIf { it >= 0L }?.let {
                                        DateFormat.getDateTimeInstance().format(Date(it))
                                    }
                                }?.let {
                                    Text(text = "Posted at $it")
                                }
                                remember {
                                    story.createdAt.takeIf { it >= 0L }?.let {
                                        DateFormat.getDateTimeInstance().format(Date(it))
                                    }
                                }?.let {
                                    Text(text = "Created at $it")
                                }

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Button(onClick = {
                                        context.androidContext.externalCacheDir?.let { cacheDir ->
                                            context.imageLoader.diskCache?.openSnapshot(story.url)?.use { diskCacheSnapshot ->
                                                val cacheFile = diskCacheSnapshot.data.toFile()
                                                val targetFile = File(cacheDir, cacheFile.name).also {
                                                    it.deleteOnExit()
                                                }

                                                runCatching {
                                                    cacheFile.inputStream().let {
                                                        story.getEncryptionKeyPair()?.decryptInputStream(it) ?: it
                                                    }.use { inputStream ->
                                                        targetFile.outputStream().use { outputStream ->
                                                            inputStream.copyTo(outputStream)
                                                        }
                                                    }

                                                    context.androidContext.startActivity(Intent().apply {
                                                        action = Intent.ACTION_VIEW
                                                        setDataAndType(
                                                            FileProvider.getUriForFile(
                                                                context.androidContext,
                                                                "me.rhunk.snapenhance.fileprovider",
                                                                targetFile
                                                            ),
                                                            FileType.fromFile(targetFile).mimeType
                                                        )
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    })
                                                }.onFailure {
                                                    context.shortToast("Failed to open file. Check logs for more info")
                                                    context.log.error("Failed to open file", it)
                                                }
                                            } ?: run {
                                                context.shortToast("Failed to get file")
                                                return@Button
                                            }
                                        }
                                    }) {
                                        Text(text = context.translation["button.open"])
                                    }

                                    Button(onClick = {
                                        downloadSelectedStory(
                                            InputMedia(
                                                content = story.url,
                                                type = DownloadMediaType.REMOTE_MEDIA,
                                                encryption = story.getEncryptionKeyPair()
                                            )
                                        )
                                    }) {
                                        Text(text = context.translation["button.download"])
                                    }

                                    if (remember {
                                        context.imageLoader.diskCache?.openSnapshot(story.url)?.also { it.close() } != null
                                    }) {
                                        Button(onClick = {
                                            downloadSelectedStory(
                                                InputMedia(
                                                    content = context.imageLoader.diskCache?.openSnapshot(story.url)?.use {
                                                        it.data.toFile().absolutePath
                                                    } ?: run {
                                                        context.shortToast("Failed to get file")
                                                        return@Button
                                                    },
                                                    type = DownloadMediaType.LOCAL_MEDIA,
                                                    encryption = story.getEncryptionKeyPair()
                                                )
                                            )
                                        }) {
                                            Text(text = translation["save_from_cache_button"])
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (stories.isEmpty()) {
            Text(text = translation["no_stories"], Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(100.dp),
            contentPadding = PaddingValues(8.dp),
        ) {
            items(stories, key = { it.url }) { story ->
                var hasFailed by remember(story.url) { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable {
                            selectedStory = story
                        }
                        .clip(MaterialTheme.shapes.medium)
                        .heightIn(min = 128.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (hasFailed) {
                        Text(text = translation["story_failed_to_load"], Modifier.padding(8.dp), fontSize = 10.sp)
                    } else {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = ImageRequestHelper.newPreviewImageRequest(
                                    context.androidContext,
                                    story.url,
                                    story.getEncryptionKeyPair(),
                                ),
                                imageLoader = context.imageLoader,
                                onError = {
                                    hasFailed = true
                                }
                            ),
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxSize()
                                .height(128.dp)
                        )
                    }
                }
            }
            item {
                LaunchedEffect(Unit) {
                    context.messageLogger.getStories(userId, lastStoryTimestamp, 20).also { result ->
                        stories.addAll(result.values.reversed())
                        result.keys.minOrNull()?.let {
                            lastStoryTimestamp = it
                        }
                    }
                }
            }
        }
    }
}