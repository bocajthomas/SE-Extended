@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package me.rhunk.snapenhance.ui.manager.pages.theming

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Launch
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontWeight
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
import me.rhunk.snapenhance.common.ui.cardShape
import me.rhunk.snapenhance.common.ui.lazyColumnContentPadding
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableStateList
import me.rhunk.snapenhance.common.ui.transparentTextFieldColors
import me.rhunk.snapenhance.common.util.ktx.getUrlFromClipboard
import me.rhunk.snapenhance.storage.*
import me.rhunk.snapenhance.ui.components.LazyColumnBottomSheet
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.util.ActivityLauncherHelper
import me.rhunk.snapenhance.ui.util.openFile
import me.rhunk.snapenhance.ui.util.saveFile
import okhttp3.OkHttpClient

class ThemingRoot: Routes.Route() {
    val localReloadDispatcher = AsyncUpdateDispatcher()
    private lateinit var activityLauncherHelper: ActivityLauncherHelper
    val okHttpClient by lazy { OkHttpClient() }
    val searchFilter = mutableStateOf("")
    private val themeOptions by lazy {
        listOf(translation["installed_themes_tab"], translation["theme_catalog_tab"])
    }
    private var selectedIndex by mutableIntStateOf(0)
    private var isSearchBarVisibleState = mutableStateOf(false)

    private fun exportTheme(theme: DatabaseTheme) {
        context.coroutineScope.launch {
            val exportedTheme = theme.toExportedTheme(
                context.database.getThemeContent(theme.id) ?: DatabaseThemeContent()
            )

            activityLauncherHelper.saveFile(
                theme.name.replace(" ", "_").lowercase() + ".json"
            ) { uri ->
                runCatching {
                    context.androidContext.contentResolver.openOutputStream(uri.toUri())
                        ?.use { outputStream ->
                            outputStream.write(context.gson.toJson(exportedTheme).toByteArray())
                            outputStream.flush()
                        }
                    context.shortToast(context.translation["manager.dialogs.theme_settings.export_success_toast"])
                }.onFailure {
                    context.log.error("Failed to save theme", it)
                    context.longToast(context.translation["manager.dialogs.theme_settings.export_failed_toast"])
                }
            }
        }
    }

    private fun duplicateTheme(theme: DatabaseTheme) {
        context.coroutineScope.launch {
            val themeId = context.database.addOrUpdateTheme(
                theme.copy(
                    updateUrl = null
                )
            )
            context.database.setThemeContent(
                themeId,
                context.database.getThemeContent(theme.id) ?: DatabaseThemeContent()
            )
            context.shortToast(context.translation["manager.dialogs.theme_settings.duplicate_success_toast"])
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
        withContext(Dispatchers.Main) {
            localReloadDispatcher.dispatch()
        }
    }

    private fun importTheme() {
        activityLauncherHelper.openFile { uri ->
            context.coroutineScope.launch {
                runCatching {
                    val themeJson =
                        context.androidContext.contentResolver.openInputStream(uri.toUri())
                            ?.bufferedReader().use {
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

    override val isSearchBarVisible: @Composable () -> Boolean = {
        isSearchBarVisibleState.value
    }

    override val topBarActions: @Composable (RowScope.() -> Unit) = {
        var showSearchBar by isSearchBarVisibleState
        val focusRequester = remember { FocusRequester() }

        if (showSearchBar) {
            OutlinedTextField(
                value = searchFilter.value,
                onValueChange = { searchFilter.value = it },
                placeholder = { Text(text = translation["search_theme_placeholder"]) },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onGloballyPositioned {
                        focusRequester.requestFocus()
                    },
                singleLine = true,
                colors = transparentTextFieldColors()
            )
            DisposableEffect(Unit) {
                onDispose {
                    searchFilter.value = ""
                }
            }
        }
        IconButton(
            onClick = { showSearchBar = !showSearchBar },
            shapes = IconButtonDefaults.shapes()
        ) {
            Icon(
                if (showSearchBar) Icons.Rounded.Close else Icons.Rounded.Search,
                contentDescription = null
            )
        }
    }

    override val topBarSubActions: @Composable (RowScope.() -> Unit) = {
        val getSelectedIndex = selectedIndex

        FlowRow(
            Modifier.padding(horizontal = 8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            themeOptions.forEachIndexed { index, label ->
                ToggleButton(
                    checked = getSelectedIndex == index,
                    onCheckedChange = { selectedIndex = index },
                    shapes =
                        when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            themeOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                    modifier = Modifier.semantics { role = Role.RadioButton }
                        .weight(1f),
                ) {
                    Text(label)
                }
            }
        }
    }

    override val floatingActionButton: @Composable () -> Unit = {
        var showImportFromUrlDialog by remember { mutableStateOf(false) }
        val fabContainerColor = MaterialTheme.colorScheme.primary
        var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

        if (showImportFromUrlDialog) {
            var url by remember { mutableStateOf("") }
            var loading by remember { mutableStateOf(false) }

            LazyColumnBottomSheet(
                onDismiss = { showImportFromUrlDialog = false },
            ) {
                val focusRequester = remember { FocusRequester() }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = context.translation["manager.dialogs.import_theme_url.title"],
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp)
                    )
                    TextField(
                        value = url,
                        onValueChange = {
                            url = it
                        },
                        label = {
                            Text(text = context.translation["manager.dialogs.import_theme_url.url_hint"])
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
                        modifier = Modifier.fillMaxWidth(),
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Text(context.translation["manager.dialogs.import_theme_url.import_button"])
                    }
                }
            }
        }

        when (selectedIndex) {
            0 -> {
                FloatingActionButtonMenu(
                    modifier = Modifier.offset(x = 18.dp, y = 18.dp),
                    expanded = fabMenuExpanded,
                    button = {
                        ToggleFloatingActionButton(
                            modifier = Modifier.semantics {
                                traversalIndex = -1f
                                stateDescription = if (fabMenuExpanded) "Expanded" else "Collapsed"
                                contentDescription = "null"
                            },
                            checked = fabMenuExpanded,
                            onCheckedChange = { fabMenuExpanded = !fabMenuExpanded },
                            containerColor = { fabContainerColor },
                        ) {
                            val imageVector by remember {
                                derivedStateOf {
                                    if (checkedProgress > 0.5f) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.Launch
                                }
                            }
                            Icon(
                                painter = rememberVectorPainter(imageVector),
                                contentDescription = null,
                                modifier = Modifier.animateIcon({ checkedProgress }),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                ) {
                    FloatingActionButtonMenuItem(
                        onClick = { importTheme() },
                        icon = { Icon(Icons.Rounded.Upload, contentDescription = null) },
                        text = { Text(translation["import_from_file_button"]) },
                        containerColor = fabContainerColor,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )

                    FloatingActionButtonMenuItem(
                        onClick = { showImportFromUrlDialog = true },
                        icon = { Icon(Icons.Rounded.Link, contentDescription = null) },
                        text = { Text(translation["import_from_url_button"]) },
                        containerColor = fabContainerColor,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )

                    FloatingActionButtonMenuItem(
                        onClick = { routes.editTheme.navigate() },
                        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                        text = { Text(translation["new_theme_button"]) },
                        containerColor = fabContainerColor,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            1 -> {
                ExtendedFloatingActionButton(
                    modifier = Modifier.offset(x = 2.dp, y = 2.dp),
                    onClick = { routes.manageRepos.navigate() },
                    icon = { Icon(Icons.Rounded.Public, contentDescription = null) },
                    text = { Text(translation["manage_repositories_button"]) },
                    containerColor = fabContainerColor,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
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
            modifier = Modifier.fillMaxSize(),
            contentPadding = lazyColumnContentPadding(staticVertical = 10.dp, hasSubAction = true, hasFAB = true),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            item {
                if (themes.isEmpty()) {
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
                                text = translation["themes_empty"],
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(8.dp)
                            )
                            Text(
                                text = translation["themes_empty_desc"],
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
            itemsIndexed(themes) { index, theme ->
                var showSettings by remember(theme) { mutableStateOf(false) }
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        routes.editTheme.navigate {
                            this["theme_id"] = theme.id.toString()
                        }
                    },
                    shape = cardShape(themes.size, index)
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

                            IconButton(
                                onClick = { showSettings = true },
                                shapes = IconButtonDefaults.shapes()
                            ) {
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

                    LazyColumnBottomSheet(
                        onDismiss = { showSettings = false },
                    ) {
                        Text(
                            text = context.translation["manager.dialogs.theme_settings.title"],
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            actionsRow.onEachIndexed  { index, entry ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = cardShape(actionsRow.size, index)
                                ) {
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
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    override val content: @Composable (NavBackStackEntry) -> Unit = {
        val getCurrentSelectedIndex = selectedIndex
        val pagerState = rememberPagerState(
            initialPage = getCurrentSelectedIndex,
            pageCount = { themeOptions.size }
        )

        DisposableEffect(key1 = Unit) {
            onDispose {
                isSearchBarVisibleState.value = false
                searchFilter.value = ""
            }
        }

        LaunchedEffect(getCurrentSelectedIndex) {
            if (getCurrentSelectedIndex != pagerState.currentPage) {
                pagerState.animateScrollToPage(getCurrentSelectedIndex)
            }
        }

        LaunchedEffect(pagerState.currentPage) {
            if (selectedIndex != pagerState.currentPage) {
                selectedIndex = pagerState.currentPage
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> InstalledThemes()
                1 -> ThemeCatalog(this@ThemingRoot)
            }
        }
    }
}