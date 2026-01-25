@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package me.rhunk.snapenhance.ui.manager.pages.theming

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import kotlinx.coroutines.*
import me.rhunk.snapenhance.common.data.RepositoryIndex
import me.rhunk.snapenhance.common.ui.*
import me.rhunk.snapenhance.storage.getThemeList
import me.rhunk.snapenhance.storage.getRepositories
import me.rhunk.snapenhance.storage.getThemeIdByUpdateUrl
import me.rhunk.snapenhance.ui.util.pullrefresh.PullRefreshIndicator
import me.rhunk.snapenhance.ui.util.pullrefresh.pullRefresh
import me.rhunk.snapenhance.ui.util.pullrefresh.rememberPullRefreshState
import okhttp3.Request

private val cachedRepoIndexes = mutableStateMapOf<String, RepositoryIndex>()
private val cacheReloadDispatcher = AsyncUpdateDispatcher()

@Composable
fun ThemeCatalog(root: ThemingRoot) {
    val context = remember { root.context }
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }

    fun fetchRepoIndexes(): Map<String, RepositoryIndex> {
        val indexes = mutableMapOf<String, RepositoryIndex>()

        context.database.getRepositories().forEach { rootUri ->
            val indexUri = rootUri.toUri().buildUpon().appendPath("index.json").build()

            runCatching {
                root.okHttpClient.newCall(
                    Request.Builder().url(indexUri.toString()).build()
                ).execute().use { response ->
                    if (!response.isSuccessful) {
                        context.log.error("Failed to fetch theme index from $indexUri: ${response.code}")
                        context.shortToast("Failed to fetch index of $indexUri")
                        return@forEach
                    }

                    runCatching {
                        indexes[rootUri] = context.gson.fromJson(response.body.charStream(), RepositoryIndex::class.java)
                    }.onFailure {
                        context.log.error("Failed to parse theme index from $indexUri", it)
                        context.shortToast("Failed to parse index of $indexUri")
                    }
                }
            }.onFailure {
                context.log.error("Failed to fetch theme index from $indexUri", it)
                context.shortToast("Failed to fetch index of $indexUri")
            }
        }
        return indexes
    }

    suspend fun installTheme(themeUri: Uri) {
        root.okHttpClient.newCall(
            Request.Builder().url(themeUri.toString()).build()
        ).execute().use { response ->
            if (!response.isSuccessful) {
                context.log.error("Failed to fetch theme from $themeUri: ${response.code}")
                context.shortToast("Failed to fetch theme from $themeUri")
                return
            }

            val themeContent = response.body.bytes().toString(Charsets.UTF_8)
            root.importTheme(themeContent, themeUri.toString())
        }
    }

    var isRefreshing by remember { mutableStateOf(false) }

    suspend fun refreshCachedIndexes() {
        isRefreshing = true
        coroutineScope {
            launch(Dispatchers.IO) {
                fetchRepoIndexes().let {
                    context.log.verbose("Fetched ${it.size} theme indexes")
                    it.forEach { (t, u) ->
                        context.log.verbose("Fetched theme index from $t with ${u.themes.size} themes")
                    }
                    synchronized(cachedRepoIndexes) {
                        cachedRepoIndexes.clear()
                        cachedRepoIndexes += it
                    }
                    cacheReloadDispatcher.dispatch()
                    delay(600)
                    isRefreshing = false
                }
            }
        }
    }

    val installedThemes = rememberAsyncMutableStateList(defaultValue = listOf(), updateDispatcher = root.localReloadDispatcher, keys = arrayOf(cachedRepoIndexes)) {
        context.database.getThemeList()
    }

    val remoteThemes by rememberAsyncMutableState(defaultValue = listOf(), updateDispatcher = cacheReloadDispatcher, keys = arrayOf(root.searchFilter.value)) {
        cachedRepoIndexes.entries.flatMap {
            it.value.themes.map { theme -> it.key to theme }
        }.let {
            val filter = root.searchFilter.value
            if (filter.isNotBlank()) {
                it.filter { (_, theme) ->
                    theme.name.contains(filter, ignoreCase = true) || theme.description?.contains(filter, ignoreCase = true) == true
                }
            } else it
        }
    }

    LaunchedEffect(Unit) {
        if (cachedRepoIndexes.isNotEmpty()) return@LaunchedEffect
        isRefreshing = true
        coroutineScope.launch {
            refreshCachedIndexes()
        }
    }

    val pullRefreshState = rememberPullRefreshState(isRefreshing, onRefresh = {
        coroutineScope.launch {
            refreshCachedIndexes()
        }
    })

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState),
            contentPadding = lazyColumnContentPadding(staticVertical = 10.dp, hasSubAction = true, hasFAB = true),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            item {
                if (remoteThemes.isEmpty()) {
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
                                shape = MaterialShapes.Cookie4Sided.toShape()
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Palette,
                                        contentDescription = null,
                                        modifier = Modifier.size(100.dp)
                                    )
                                }
                            }
                            Text(
                                text = context.translation["manager.sections.theming.themes_catalog_empty"],
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(8.dp)
                            )
                            Text(
                                text = context.translation["manager.sections.theming.themes_catalog_empty_desc"],
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
            itemsIndexed(remoteThemes, key = { _, item -> item.first + item.second.hashCode() }) { index, (_, themeManifest) ->
                val themeUri = remember {
                    cachedRepoIndexes.entries.find { it.value.themes.contains(themeManifest) }?.key?.toUri()?.buildUpon()?.appendPath(themeManifest.filepath)?.build()
                }

                val hasUpdate by rememberAsyncMutableState(defaultValue = false, keys = arrayOf(themeManifest)) {
                    installedThemes.takeIf { themeUri != null }?.find { it.updateUrl == themeUri.toString() }?.let { installedTheme ->
                        installedTheme.version != themeManifest.version
                    } ?: false
                }

                var isInstalling by rememberAsyncMutableState(defaultValue = false, keys = arrayOf(themeManifest)) {
                    false
                }

                var isInstalled by rememberAsyncMutableState(defaultValue = true, keys = arrayOf(themeManifest)) {
                    context.database.getThemeIdByUpdateUrl(themeUri.toString()) != null
                }

                ElevatedCard(
                    onClick = { /*TODO: Show theme details*/ },
                    shape = cardShape(remoteThemes.size, index)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, modifier = Modifier.padding(16.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = themeManifest.name,
                                maxLines = 1,
                                fontSize = 16.sp,
                                lineHeight = 10.sp,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold,
                            )
                            themeManifest.author?.let {
                                Text(
                                    text = "by $it",
                                    maxLines = 1,
                                    fontSize = 10.sp,
                                    lineHeight = 16.sp,
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = FontWeight.Light,
                                    overflow = TextOverflow.Visible,
                                )
                            }
                            themeManifest.description?.let {
                                Text(
                                    text = it,
                                    fontSize = 12.sp,
                                    maxLines = 3,
                                    lineHeight = 16.sp,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (hasUpdate) {
                                Text(
                                    text = "Version ${themeManifest.version} available",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isInstalling) {
                                CircularWavyProgressIndicator(
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Button(
                                    enabled = !isInstalled || hasUpdate,
                                    onClick = {
                                        isInstalling = true
                                        context.coroutineScope.launch {
                                            runCatching {
                                                installTheme(themeUri ?: throw IllegalStateException("Failed to get theme URI"))
                                                isInstalled = true
                                            }.onFailure {
                                                context.log.error("Failed to install theme ${themeManifest.name}", it)
                                                context.shortToast("Failed to install theme ${themeManifest.name}. ${it.message}")
                                            }
                                            isInstalling = false
                                        }
                                    },
                                    shapes = ButtonDefaults.shapes()
                                ) {
                                    if (hasUpdate) {
                                        Text(context.translation["manager.sections.theming.update_theme_button"])
                                    } else {
                                        Text(if (isInstalled) context.translation["manager.sections.theming.installed_theme_button"] else context.translation["manager.sections.theming.install_theme_button"])
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter).componentPadding(staticVertical = 0.dp, staticHorizontal = 0.dp, hasSubAction = true)
        )
    }
}