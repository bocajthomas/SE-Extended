package me.rhunk.snapenhance.ui.manager.pages.theming

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rhunk.snapenhance.common.data.DatabaseTheme
import me.rhunk.snapenhance.common.data.DatabaseThemeContent
import me.rhunk.snapenhance.common.data.ExportedTheme
import me.rhunk.snapenhance.common.ui.AsyncUpdateDispatcher
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableStateList
import me.rhunk.snapenhance.common.ui.transparentTextFieldColors
import me.rhunk.snapenhance.common.util.ktx.getUrlFromClipboard
import me.rhunk.snapenhance.storage.*
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.util.ActivityLauncherHelper
import me.rhunk.snapenhance.ui.util.openFile
import me.rhunk.snapenhance.ui.util.pagerTabIndicatorOffset
import me.rhunk.snapenhance.ui.util.saveFile
import okhttp3.OkHttpClient

class ThemingRoot: Routes.Route() {
    val localReloadDispatcher = AsyncUpdateDispatcher()
    private lateinit var activityLauncherHelper: ActivityLauncherHelper

    private var currentPage by mutableIntStateOf(0)
    val okHttpClient by lazy { OkHttpClient() }
    val searchFilter = mutableStateOf("")


    private fun exportTheme(theme: DatabaseTheme) {
        context.coroutineScope.launch {
            val exportedTheme = theme.toExportedTheme(context.database.getThemeContent(theme.id) ?: DatabaseThemeContent())

            activityLauncherHelper.saveFile(theme.name.replace(" ", "_").lowercase() + ".json") { uri ->
                runCatching {
                    context.androidContext.contentResolver.openOutputStream(uri.toUri())?.use { outputStream ->
                        outputStream.write(context.gson.toJson(exportedTheme).toByteArray())
                        outputStream.flush()
                    }
                    context.shortToast("Theme exported successfully")
                }.onFailure {
                    context.log.error("Failed to save theme", it)
                    context.longToast("Failed to export theme! Check logs for more details")
                }
            }
        }
    }

    private fun duplicateTheme(theme: DatabaseTheme) {
        context.coroutineScope.launch {
            val themeId = context.database.addOrUpdateTheme(theme.copy(
                updateUrl = null
            ))
            context.database.setThemeContent(themeId, context.database.getThemeContent(theme.id) ?: DatabaseThemeContent())
            context.shortToast("Theme duplicated successfully")
            withContext(Dispatchers.Main) {
                localReloadDispatcher.dispatch()
            }
        }
    }

    suspend fun importTheme(content: String, updateUrl: String? = null) {
        val theme = context.gson.fromJson(content, ExportedTheme::class.java)
        val existingTheme = updateUrl?.let {
            context.database.getThemeIdByUpdateUrl(it)
        }?.let {
            context.database.getThemeInfo(it)
        }
        val databaseTheme = theme.toDatabaseTheme(
            updateUrl = updateUrl,
            enabled = existingTheme?.enabled ?: false
        )

        val themeId = context.database.addOrUpdateTheme(
            themeId = existingTheme?.id,
            theme = databaseTheme
        )

        context.database.setThemeContent(themeId, theme.content)
        context.shortToast("Theme imported successfully")
        withContext(Dispatchers.Main) {
            localReloadDispatcher.dispatch()
        }
    }

    private fun importTheme() {
        activityLauncherHelper.openFile { uri ->
            context.coroutineScope.launch {
                runCatching {
                    val themeJson = context.androidContext.contentResolver.openInputStream(uri.toUri())?.bufferedReader().use {
                        it?.readText()
                    } ?: throw Exception("Failed to read file")

                    importTheme(themeJson)
                }.onFailure {
                    context.log.error("Failed to import theme", it)
                    context.longToast("Failed to import theme! Check logs for more details")
                }
            }
        }
    }

    private suspend fun importFromURL(url: String) {
        val result = okHttpClient.newCall(
            okhttp3.Request.Builder()
                .url(url)
                .build()
        ).execute()

        if (!result.isSuccessful) {
            throw Exception("Failed to fetch theme from URL ${result.message}")
        }

        importTheme(result.body.string(), url)
    }

    override val init: () -> Unit = {
        activityLauncherHelper = ActivityLauncherHelper(context.activity!!)
    }

    override val topBarActions: @Composable (RowScope.() -> Unit) = {
        var showSearchBar by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showSearchBar) {
                OutlinedTextField(
                    value = searchFilter.value,
                    onValueChange = { searchFilter.value = it },
                    placeholder = { Text(context.translation["manager.sections.theming.search_theme_placeholder"]) },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onGloballyPositioned {
                            focusRequester.requestFocus()
                        },
                    colors = transparentTextFieldColors()
                )
                DisposableEffect(Unit) {
                    onDispose {
                        searchFilter.value = ""
                    }
                }
            }
            IconButton(onClick = {
                showSearchBar = !showSearchBar
            }) {
                Icon(if (showSearchBar) Icons.Rounded.Close else Icons.Rounded.Search, contentDescription = null)
            }
        }
    }

    override val floatingActionButton: @Composable () -> Unit = {
        var showImportFromUrlDialog by remember { mutableStateOf(false) }

        if (showImportFromUrlDialog) {
            var url by remember { mutableStateOf("") }
            var loading by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showImportFromUrlDialog = false },
                title = { Text(context.translation["manager.dialogs.import_theme_url.title"]) },
                text = {
                    val focusRequester = remember { FocusRequester() }
                    TextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text(context.translation["manager.dialogs.import_theme_url.url_hint"]) },
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
                },
                confirmButton = {
                    Button(
                        enabled = url.isNotBlank() && !loading,
                        onClick = {
                            loading = true
                            context.coroutineScope.launch {
                                runCatching {
                                    importFromURL(url)
                                    withContext(Dispatchers.Main) {
                                        showImportFromUrlDialog = false
                                    }
                                }.onFailure {
                                    context.log.error("Failed to import theme", it)
                                    context.longToast("Failed to import theme! ${it.message}")
                                }
                                withContext(Dispatchers.Main) {
                                    loading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(context.translation["manager.dialogs.import_theme_url.import_button"])
                    }
                }
            )
        }
        Column(
            horizontalAlignment = Alignment.End
        ) {
            when (currentPage) {
                0 -> {
                    ExtendedFloatingActionButton(
                        onClick = {
                            routes.editTheme.navigate()
                        },
                        icon = {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                        },
                        text = {
                            Text(translation["new_theme_button"])
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ExtendedFloatingActionButton(
                        onClick = {
                            importTheme()
                        },
                        icon = {
                            Icon(Icons.Rounded.Upload, contentDescription = null)
                        },
                        text = {
                            Text(translation["import_from_file_button"])
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ExtendedFloatingActionButton(
                        onClick = { showImportFromUrlDialog = true },
                        icon = {
                            Icon(Icons.Rounded.Link, contentDescription = null)
                        },
                        text = {
                            Text(translation["import_from_url_button"])
                        }
                    )
                }
                1 -> {
                    ExtendedFloatingActionButton(
                        onClick = {
                            routes.manageRepos.navigate()
                        },
                        icon = {
                            Icon(Icons.Rounded.Public, contentDescription = null)
                        },
                        text = {
                            Text(translation["manage_repositories_button"])
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun InstalledThemes() {
        val themes = rememberAsyncMutableStateList(defaultValue = listOf(), updateDispatcher = localReloadDispatcher, keys = arrayOf(searchFilter.value)) {
            context.database.getThemeList().let {
                val filter = searchFilter.value
                if (filter.isNotBlank()) {
                    it.filter { theme ->
                        theme.name.contains(filter, ignoreCase = true) ||
                        theme.author?.contains(filter, ignoreCase = true) == true ||
                        theme.description?.contains(filter, ignoreCase = true) == true
                    }
                } else it
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                if (themes.isEmpty()) {
                    Text(
                        text = translation["no_themes_hint"],
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            }
            items(themes, key = { it.id }) { theme ->
                var showSettings by remember(theme) { mutableStateOf(false) }

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = {
                        routes.editTheme.navigate {
                            this["theme_id"] = theme.id.toString()
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Palette, contentDescription = null, modifier = Modifier.padding(5.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(8.dp),
                        ) {
                            Text(text = theme.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 20.sp)
                            theme.author?.takeIf { it.isNotBlank() }?.let {
                                Text(text = "by $it", lineHeight = 15.sp, fontWeight = FontWeight.Light, fontSize = 12.sp)
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            var state by remember { mutableStateOf(theme.enabled) }

                            IconButton(onClick = {
                                showSettings = true
                            }) {
                                Icon(Icons.Rounded.Settings, contentDescription = null)
                            }

                            Switch(checked = state, onCheckedChange = {
                                state = it
                                context.database.setThemeState(theme.id, it)
                            })
                        }
                    }
                }

                if (showSettings) {
                    val actionsRow = remember {
                        mapOf(
                            (context.translation["manager.dialogs.theme_settings.duplicate_button"] to Icons.Rounded.ContentCopy) to { duplicateTheme(theme) },
                            (context.translation["manager.dialogs.theme_settings.export_button"] to Icons.Rounded.Download) to { exportTheme(theme) }
                        )
                    }
                    AlertDialog(
                        onDismissRequest = { showSettings = false },
                        title = { context.translation["manager.dialogs.theme_settings.title"] },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                actionsRow.forEach { entry ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                showSettings = false
                                                entry.value()
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(entry.key.second, contentDescription = null, modifier = Modifier.padding(16.dp))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(entry.key.first)
                                    }
                                }
                            }
                        },
                        confirmButton = {}
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(200.dp))
            }
        }
    }


    @OptIn(ExperimentalFoundationApi::class)
    override val content: @Composable (NavBackStackEntry) -> Unit = {
        val coroutineScope = rememberCoroutineScope()
        val titles = remember { listOf(translation["installed_themes_title"], translation["theme_catalog_title"]) }
        val pagerState = rememberPagerState { titles.size }
        currentPage = pagerState.currentPage

        Column {
            TabRow(selectedTabIndex = pagerState.currentPage, indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.pagerTabIndicatorOffset(
                        pagerState = pagerState,
                        tabPositions = tabPositions
                    )
                )
            }) {
                titles.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            Text(
                                text = title,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            HorizontalPager(
                modifier = Modifier.weight(1f),
                state = pagerState
            ) { page ->
                when (page) {
                    0 -> InstalledThemes()
                    1 -> ThemeCatalog(this@ThemingRoot)
                }
            }
        }
    }
}