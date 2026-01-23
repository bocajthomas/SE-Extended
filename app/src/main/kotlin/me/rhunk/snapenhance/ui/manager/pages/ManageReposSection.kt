@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package me.rhunk.snapenhance.ui.manager.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.common.data.RepositoryIndex
import me.rhunk.snapenhance.common.ui.AsyncUpdateDispatcher
import me.rhunk.snapenhance.common.ui.cardShape
import me.rhunk.snapenhance.common.ui.lazyColumnContentPadding
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableStateList
import me.rhunk.snapenhance.common.util.ktx.copyToClipboard
import me.rhunk.snapenhance.common.util.ktx.getUrlFromClipboard
import me.rhunk.snapenhance.storage.addRepo
import me.rhunk.snapenhance.storage.getRepositories
import me.rhunk.snapenhance.storage.removeRepo
import me.rhunk.snapenhance.ui.components.LazyColumnBottomSheet
import me.rhunk.snapenhance.ui.manager.Routes
import okhttp3.OkHttpClient

class ManageReposSection: Routes.Route() {
    private val updateDispatcher = AsyncUpdateDispatcher()
    private val okHttpClient by lazy { OkHttpClient() }

    override val floatingActionButton: @Composable () -> Unit = {
        var showAddBottomSheet by remember { mutableStateOf(false) }

        ExtendedFloatingActionButton(
            modifier = Modifier.offset(x = 2.dp, y = 2.dp),
            onClick = { showAddBottomSheet = true },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text(translation["add_repository_button"]) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )

        if (showAddBottomSheet) {
            val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
            var url by remember { mutableStateOf("") }
            var loading by remember { mutableStateOf(false) }
            val focusRequester = remember { FocusRequester() }

            suspend fun addRepo(url: String) {
                var modifiedUrl = url;

                if (url.startsWith("https://github.com/")) {
                    val splitUrl = modifiedUrl.removePrefix("https://github.com/").split("/")
                    val repoName = splitUrl[0] + "/" + splitUrl[1]
                    okHttpClient.newCall(
                        okhttp3.Request.Builder().url("https://api.github.com/repos/$repoName").build()
                    ).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw Exception("Failed to fetch default branch: ${response.code}")
                        }
                        val json = response.body.string()
                        val defaultBranch = context.gson.fromJson(json, Map::class.java)["default_branch"] as String
                        context.log.info("Default branch for $repoName is $defaultBranch")
                        modifiedUrl = "https://raw.githubusercontent.com/$repoName/$defaultBranch/"
                    }
                }

                val indexUri = modifiedUrl.toUri().buildUpon().appendPath("index.json").build()
                okHttpClient.newCall(
                    okhttp3.Request.Builder().url(indexUri.toString()).build()
                ).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw Exception("Failed to fetch index from $indexUri: ${response.code}")
                    }
                    runCatching {
                        val repoIndex = context.gson.fromJson(response.body.charStream(), RepositoryIndex::class.java).also {
                            context.log.info("repository index: $it")
                        }

                        context.database.addRepo(modifiedUrl)
                        context.shortToast("Repository added successfully! $repoIndex")
                        showAddBottomSheet = false
                        updateDispatcher.dispatch()
                    }.onFailure {
                        throw Exception("Failed to parse index from $indexUri")
                    }
                }
            }

            LazyColumnBottomSheet(
                onDismiss = { showAddBottomSheet = false }
            ) {
                Text (
                    text = context.translation["manager.dialogs.add_repository_url.title"],
                    fontSize = 20.sp,
                    modifier = Modifier.padding(start = 5.dp, bottom = 10.dp)
                )
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onGloballyPositioned { focusRequester.requestFocus() },
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(context.translation["manager.dialogs.add_repository_url.url_hint"]) }
                )
                LaunchedEffect(Unit) {
                    context.androidContext.getUrlFromClipboard()?.let {
                        url = it
                    }
                }
                HorizontalDivider(
                    modifier = Modifier
                        .padding(top = 15.dp, bottom = 10.dp)
                        .fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        enabled = !loading,
                        onClick = {
                            loading = true;
                            coroutineScope.launch {
                                runCatching {
                                    addRepo(url)
                                }.onFailure {
                                    context.log.error("Failed to add repository", it)
                                    context.shortToast("Failed to add repository: ${it.message}")
                                }
                                loading = false
                            }
                        },
                        shapes = ButtonDefaults.shapes()
                    ) {
                        if (loading) {
                            CircularWavyProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Text(context.translation["manager.dialogs.add_repository_url.add_button"])
                        }
                    }
                }
            }
        }
    }

    override val content: @Composable (NavBackStackEntry) -> Unit = {
        val coroutineScope = rememberCoroutineScope()
        val repositories = rememberAsyncMutableStateList(defaultValue = listOf(), updateDispatcher = updateDispatcher) {
            context.database.getRepositories()
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = lazyColumnContentPadding(staticVertical = 10.dp, hasFAB = true),
        ) {
            if (repositories.isEmpty()) {
                item {
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
                                shape = MaterialShapes.Cookie6Sided.toShape()
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Public,
                                        contentDescription = null,
                                        modifier = Modifier.size(100.dp)
                                    )
                                }
                            }
                            Text(
                                text = translation["repositories_empty"],
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(8.dp)
                            )
                            Text(
                                text = translation["repositories_empty_desc"],
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
            itemsIndexed(repositories) { index, url ->
                ElevatedCard(
                    onClick = {
                        context.androidContext.copyToClipboard(url)
                    },
                    shape = cardShape(repositories.size, index)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Public, contentDescription = null)
                        Text(text = url, modifier = Modifier.weight(1f), overflow = TextOverflow.Ellipsis, maxLines = 4, fontSize = 15.sp, lineHeight = 15.sp)
                        Button(
                            onClick = {
                                context.database.removeRepo(url)
                                coroutineScope.launch {
                                    updateDispatcher.dispatch()
                                }
                            },
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Text(translation["remove_repository_button"])
                        }
                    }
                }
            }
        }
    }
}