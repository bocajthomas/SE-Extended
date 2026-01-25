@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package me.rhunk.snapenhance.ui.manager.pages.features

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.common.config.*
import me.rhunk.snapenhance.common.ui.*
import me.rhunk.snapenhance.ui.components.*
import me.rhunk.snapenhance.ui.manager.MainActivity
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.util.*

class FeaturesRootSection : Routes.Route() {
    private val bottomSheets by lazy { BottomSheets(context.translation) }

    companion object {
        const val FEATURE_CONTAINER_ROUTE = "feature_container/{name}"
        const val SEARCH_FEATURE_ROUTE = "search_feature/{keyword}"
    }
    private var activityLauncherHelper: ActivityLauncherHelper? = null
    private var isSearchBarVisibleState = mutableStateOf(false)
    private val searchFilterState = mutableStateOf("")

    private val allContainers by lazy {
        val containers = mutableMapOf<String, PropertyPair<*>>()
        fun queryContainerRecursive(container: ConfigContainer) {
            container.properties.forEach {
                if (it.key.dataType.type == DataProcessors.Type.CONTAINER) {
                    containers[it.key.name] = PropertyPair(it.key, it.value)
                    queryContainerRecursive(it.value.get() as ConfigContainer)
                }
            }
        }
        queryContainerRecursive(context.config.root)
        containers
    }

    private val allProperties by lazy {
        val properties = mutableMapOf<PropertyKey<*>, PropertyValue<*>>()
        allContainers.values.forEach {
            val container = it.value.get() as ConfigContainer
            container.properties.forEach { property ->
                properties[property.key] = property.value
            }
        }
        properties
    }

    private fun navigateToMainRoot() {
        routes.navController.navigate(routeInfo.id, NavOptions.Builder()
            .setPopUpTo(routes.navController.graph.findStartDestination().id, false)
            .setLaunchSingleTop(true)
            .build()
        )
    }

    override val init: () -> Unit = {
        activityLauncherHelper = ActivityLauncherHelper(context.activity!!)
    }

    private fun activityLauncher(block: ActivityLauncherHelper.() -> Unit) {
        activityLauncherHelper?.let(block) ?: run {
            val intent = Intent(context.androidContext, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("route", routeInfo.id)
            context.androidContext.startActivity(intent)
        }
    }

    override val isSearchBarVisible: @Composable () -> Boolean = {
        isSearchBarVisibleState.value
    }

    override val content: @Composable (NavBackStackEntry) -> Unit = {
        Container(context.config.root)
    }

    override val customComposables: NavGraphBuilder.() -> Unit = {
        routeInfo.childIds.addAll(listOf(FEATURE_CONTAINER_ROUTE, SEARCH_FEATURE_ROUTE))

        composable(FEATURE_CONTAINER_ROUTE, enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(100))
        }, exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300))
        }) { backStackEntry ->
            backStackEntry.arguments?.getString("name")?.let { containerName ->
                allContainers[containerName]?.let {
                    Container(it.value.get() as ConfigContainer)
                }
            }
        }

        composable(SEARCH_FEATURE_ROUTE) { backStackEntry ->
            backStackEntry.arguments?.getString("keyword")?.let { keyword ->
                val properties = allProperties.filter {
                    it.key.name.contains(keyword, ignoreCase = true) ||
                            context.translation[it.key.propertyName()].contains(keyword, ignoreCase = true) ||
                            context.translation[it.key.propertyDescription()].contains(keyword, ignoreCase = true)
                }.map { PropertyPair(it.key, it.value) }

                PropertiesView(properties)
            }
        }
    }


    @OptIn(ExperimentalFoundationApi::class)
    @SuppressLint("ConfigurationScreenWidthHeight")
    @Composable
    private fun PropertyAction(property: PropertyPair<*>, registerClickCallback: RegisterClickCallback) {
        var showBottomSheet by remember { mutableStateOf(false) }
        var dialogComposable by remember { mutableStateOf<@Composable () -> Unit>({}) }
        fun registerDialogOnClickCallback() = registerClickCallback { showBottomSheet = true }
        val versionCheck = remember { property.key.params.versionCheck }
        val versionCheckPair = remember(property) { versionCheck?.checkVersion(context.installationSummary.snapchatInfo?.versionCode ?: return@remember null)}
        val isComponentDisabled = remember { versionCheckPair != null && versionCheck?.isDisabled == true }

        if (showBottomSheet) {
            DefaultBottomSheet(
                onDismiss = { showBottomSheet = false },
            ) {
                dialogComposable()
            }
        }

        val propertyValue = property.value
        if (property.key.params.flags.contains(ConfigFlag.USER_IMPORT)) {
            registerDialogOnClickCallback()
            dialogComposable = {
                var isEmpty by remember { mutableStateOf(false) }
                val files = rememberAsyncMutableStateList(defaultValue = listOf()) {
                    context.fileHandleManager.getStoredFiles {
                        property.key.params.filenameFilter?.invoke(it.name) == true
                    }.also {
                        isEmpty = it.isEmpty()
                        if (isEmpty) {
                            propertyValue.setAny(null)
                        }
                    }
                }
                var selectedFile by remember(files.size) {
                    mutableStateOf(files.firstOrNull { it.name == propertyValue.getNullable() }
                        .also {
                            if (files.isNotEmpty() && it == null) propertyValue.setAny(null)
                        }?.name
                    )
                }
                Text(
                    text = context.translation["manager.dialogs.file_imports.title"],
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    item {
                        if (isEmpty) {
                            Box(
                                modifier = Modifier.fillParentMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .size(200.dp),
                                        shape = MaterialShapes.Gem.toShape()
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
                                        text = context.translation["manager.dialogs.file_imports.files_empty"],
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                    Text(
                                        text = context.translation["manager.dialogs.file_imports.files_empty_desc"],
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    itemsIndexed(files) { index, file ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = cardShape(files.size, index)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clickable {
                                        selectedFile =
                                            if (selectedFile == file.name) null else file.name
                                        propertyValue.setAny(selectedFile)
                                    }
                                    .padding(5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.AttachFile,
                                    contentDescription = null,
                                    modifier = Modifier.padding(5.dp)
                                )
                                Text(
                                    text = file.name,
                                    modifier = Modifier
                                        .padding(3.dp)
                                        .weight(1f),
                                    fontSize = 14.sp,
                                    lineHeight = 16.sp
                                )
                                if (selectedFile == file.name) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.padding(5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Icon(Icons.Rounded.AttachFile, contentDescription = null)
            return
        }

        if (property.key.params.flags.contains(ConfigFlag.FOLDER)) {
            IconButton(
                onClick = registerClickCallback {
                    activityLauncher {
                        chooseFolder { uri ->
                            propertyValue.setAny(uri)
                        }
                    }
                }.let {
                    { it.invoke(true) }
                },
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(Icons.Rounded.FolderOpen, contentDescription = null)
            }
            return
        }

        when (val dataType = remember { property.key.dataType.type }) {
            DataProcessors.Type.BOOLEAN -> {
                var state by remember { mutableStateOf(propertyValue.get() as Boolean) }
                Switch(
                    enabled = !isComponentDisabled,
                    checked = state,
                    onCheckedChange = registerClickCallback {
                        state = state.not()
                        propertyValue.setAny(state)
                    }
                )
            }

            DataProcessors.Type.MAP_COORDINATES -> {
                registerDialogOnClickCallback()
                dialogComposable = {
                    bottomSheets.ChooseLocationBottomSheet(property) {
                        showBottomSheet = false
                    }
                }

                Text(
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier.widthIn(0.dp, 120.dp),
                    text = (propertyValue.get() as Pair<*, *>).let {
                        "${it.first.toString().toFloatOrNull() ?: 0F}, ${it.second.toString().toFloatOrNull() ?: 0F}"
                    }
                )
            }

            DataProcessors.Type.STRING_UNIQUE_SELECTION -> {
                registerDialogOnClickCallback()

                dialogComposable = {
                    if (!isComponentDisabled) {
                        bottomSheets.UniqueSelectionBottomSheet(property)
                    }
                }

                Text(
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier.widthIn(0.dp, 120.dp),
                    text = (propertyValue.getNullable() as? String ?: "null").let {
                        property.key.propertyOption(context.translation, it)
                    }
                )
            }

            DataProcessors.Type.STRING_MULTIPLE_SELECTION, DataProcessors.Type.STRING, DataProcessors.Type.INTEGER, DataProcessors.Type.FLOAT -> {
                dialogComposable = {
                    when (dataType) {
                        DataProcessors.Type.STRING_MULTIPLE_SELECTION -> {
                            if (!isComponentDisabled) {
                                bottomSheets.MultipleSelectionBottomSheet(property)
                            }
                        }
                        DataProcessors.Type.STRING, DataProcessors.Type.INTEGER, DataProcessors.Type.FLOAT -> {
                            if (!isComponentDisabled) {
                                bottomSheets.KeyboardInputBottomSheet(property) { showBottomSheet = false }
                            }
                        }
                        else -> {}
                    }
                }

                registerDialogOnClickCallback().let { { it.invoke(true) } }.also {
                    if (dataType == DataProcessors.Type.INTEGER ||
                        dataType == DataProcessors.Type.FLOAT) {
                        FilledIconButton(
                            onClick = it,
                            shapes = IconButtonDefaults.shapes()
                        ) {
                            Text(
                                text = propertyValue.get().toString(),
                                modifier = Modifier.wrapContentWidth(),
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        IconButton(
                            onClick = it,
                            shapes = IconButtonDefaults.shapes()
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null)
                        }
                    }
                }
            }

            DataProcessors.Type.INT_COLOR -> {
                dialogComposable = {
                    bottomSheets.ColorPickerPropertyBottomSheet(property) {
                        showBottomSheet = false
                    }
                }

                registerDialogOnClickCallback().let { { it.invoke(true) } }.also {
                    CircularAlphaTile(selectedColor = (propertyValue.getNullable() as? Int)?.let { Color(it) })
                }
            }

            DataProcessors.Type.CONTAINER -> {
                val container = propertyValue.get() as ConfigContainer

                registerClickCallback {
                    routes.navController.navigate(
                        FEATURE_CONTAINER_ROUTE.replace(
                            "{name}",
                            property.name
                        )
                    )
                }
                if (!container.hasGlobalState) return
                var state by remember { mutableStateOf(container.globalState ?: false) }
                Box(
                    modifier = Modifier.padding(end = 15.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(50.dp)
                            .width(1.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(5.dp)
                            )
                    )
                }

                Switch(
                    enabled = !isComponentDisabled,
                    checked = state,
                    onCheckedChange = {
                        state = state.not()
                        container.globalState = state
                    }
                )
            }
        }
    }

    @Composable
    private fun PropertyCard(property: PropertyPair<*>, shape: RoundedCornerShape) {
        var clickCallback by remember { mutableStateOf<ClickCallback?>(null) }
        val noticeColorMap = mapOf(
            FeatureNotice.UNSTABLE.key to Color(0xFFFFFB87),
            FeatureNotice.BAN_RISK.key to Color(0xFFFF8585),
            FeatureNotice.INTERNAL_BEHAVIOR.key to Color(0xFFFFFB87),
        )

        val versionCheck = remember { property.key.params.versionCheck }
        val versionCheckPair = remember(property) { versionCheck?.checkVersion(context.installationSummary.snapchatInfo?.versionCode ?: return@remember null)}
        val isComponentDisabled = remember { versionCheckPair != null && versionCheck?.isDisabled == true }

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isComponentDisabled) Modifier.graphicsLayer(alpha = 0.5f)
                    else Modifier
                ),
            shape = shape
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        if (!isComponentDisabled) {
                            clickCallback?.invoke(true)
                        }
                    }
                    .padding(all = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                property.key.params.icon?.let { icon ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .padding(start = 10.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .weight(1f, fill = true)
                        .padding(all = 10.dp)
                ) {
                    Text(
                        text = context.translation[property.key.propertyName()],
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = context.translation[property.key.propertyDescription()],
                        fontSize = 12.sp,
                        lineHeight = 15.sp
                    )
                    property.key.params.notices.also {
                        if (it.isNotEmpty()) Spacer(modifier = Modifier.height(5.dp))
                    }.forEach {
                        Text(
                            text = context.translation["features.notices.${it.key}"],
                            color = noticeColorMap[it.key] ?: Color(0xFFFFFB87),
                            fontSize = 12.sp,
                            lineHeight = 15.sp
                        )
                    }

                    if (versionCheckPair != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = context.translation.format(
                                "manager.sections.features.${versionCheckPair.second.key}",
                                "version" to versionCheckPair.first.first
                            ),
                            color = Color(0xFFFF8585),
                            fontSize = 12.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(all = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PropertyAction(property, registerClickCallback = { callback ->
                        if (property.key.propertyTranslationPath().startsWith("rules.properties")) {
                            clickCallback = {
                                routes.manageRuleFeature.navigate {
                                    put("rule_type", property.key.name)
                                }
                            }
                            return@PropertyAction clickCallback!!
                        }
                        clickCallback = callback
                        callback
                    })
                }
            }
        }
    }

    // TODO: Redesign
    @Composable
    private fun FeatureSearchBar(
        rowScope: RowScope,
        focusRequester: FocusRequester,
        searchValueState: MutableState<String>
    ) {
        var searchValue by searchValueState
        val scope = rememberCoroutineScope()
        var currentSearchJob by remember { mutableStateOf<Job?>(null) }

        rowScope.apply {
            TextField(
                value = searchValue,
                onValueChange = { keyword ->
                    searchValue = keyword
                    if (keyword.isEmpty()) {
                        navigateToMainRoot()
                        return@TextField
                    }
                    currentSearchJob?.cancel()
                    scope.launch {
                        delay(150)
                        routes.navController.navigate(SEARCH_FEATURE_ROUTE.replace("{keyword}", keyword), NavOptions.Builder()
                            .setLaunchSingleTop(true)
                            .setPopUpTo(routeInfo.id, false)
                            .build()
                        )
                    }.also { currentSearchJob = it }
                },
                placeholder = { Text(text = translation["search_feature_placeholder"]) },
                keyboardActions = KeyboardActions(onDone = {
                    focusRequester.freeFocus()
                }),
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .weight(1f, fill = true)
                    .padding(end = 10.dp),
                singleLine = true,
                colors = transparentTextFieldColors()
            )
        }

        DisposableEffect(key1 = Unit) {
            onDispose {
                isSearchBarVisibleState.value = false
                searchValueState.value = ""
            }
        }
    }

    override val topBarActions: @Composable (RowScope.() -> Unit) = topBarActions@{
        var showSearchBar by isSearchBarVisibleState
        val focusRequester = remember { FocusRequester() }

        if (showSearchBar) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FeatureSearchBar(this, focusRequester, searchFilterState)
                IconButton(
                    onClick = {
                        showSearchBar = false
                        navigateToMainRoot()
                    },
                    shapes = IconButtonDefaults.shapes()
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = null)
                }
            }
            LaunchedEffect(true) {
                focusRequester.requestFocus()
            }
        } else {
            IconButton(
                onClick = { showSearchBar = true },
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null)
            }
        }

        if (showSearchBar) return@topBarActions

        var showExportDropdownMenu by remember { mutableStateOf(false) }
        var showResetConfirmationBottomSheet by remember { mutableStateOf(false) }
        var showExportBottomSheet by remember { mutableStateOf(false) }

        if (showResetConfirmationBottomSheet) {
            LazyColumnBottomSheet(
                onDismiss = { showResetConfirmationBottomSheet = false }
            ) {
                bottomSheets.ConfirmBottomSheet(
                    type = ConfirmBottomSheetType.CHOICE,
                    title = context.translation["manager.dialogs.reset_config.title"],
                    message = context.translation["manager.dialogs.reset_config.content"],
                    onConfirm = {
                        context.config.reset()
                        context.shortToast(context.translation["manager.dialogs.reset_config.success_toast"])
                        navigateToMainRoot()
                        showResetConfirmationBottomSheet = false
                    },
                    onDismiss = { showResetConfirmationBottomSheet = false }
                )
            }
        }

        if (showExportBottomSheet) {
            fun exportConfig(
                exportSensitiveData: Boolean
            ) {
                showExportBottomSheet = false
                activityLauncher {
                    saveFile("config.json", "application/json") { uri ->
                        runCatching {
                            context.androidContext.contentResolver.openOutputStream(Uri.parse(uri))?.use {
                                context.config.writeConfig()
                                context.config.exportToString(exportSensitiveData).byteInputStream().copyTo(it)
                                context.shortToast(translation["config_export_success_toast"])
                            }
                        }.onFailure {
                            context.longToast(translation.format("config_export_failure_toast", "error" to it.message.toString()))
                        }
                    }
                }
            }

            DefaultBottomSheet(
                onDismiss = { showExportBottomSheet = false }
            ) {
                bottomSheets.ConfirmBottomSheet(
                    type = ConfirmBottomSheetType.CHOICE,
                    title = context.translation["manager.dialogs.export_config.title"],
                    message = context.translation["manager.dialogs.export_config.content"],
                    onConfirm = { exportConfig(true) },
                    onDismiss = { showExportBottomSheet = false }
                )
            }
        }

        val actions = remember {
            mapOf(
                translation["import_option"] to {
                    activityLauncher {
                        openFile("application/json") { uri ->
                            context.androidContext.contentResolver.openInputStream(Uri.parse(uri))?.use {
                                runCatching {
                                    context.config.loadFromString(it.readBytes().toString(Charsets.UTF_8))
                                }.onFailure {
                                    context.longToast(translation.format("config_import_failure_toast", "error" to it.message.toString()))
                                    return@use
                                }
                                context.shortToast(translation["config_import_success_toast"])
                                context.coroutineScope.launch(Dispatchers.Main) {
                                    navigateReload()
                                }
                            }
                        }
                    }
                },
                translation["export_option"] to { showExportBottomSheet = true },
                translation["reset_option"] to { showResetConfirmationBottomSheet = true }
            )
        }

        if (context.activity != null) {
            IconButton(
                onClick = { showExportDropdownMenu = !showExportDropdownMenu},
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = null
                )
            }
        }

        if (showExportDropdownMenu) {
            DropdownMenuPopup(
                expanded = true,
                onDismissRequest = { showExportDropdownMenu = false }
            ) {
                val actionEntries = actions.toList()

                DropdownMenuGroup(
                    shapes = MenuDefaults.groupShape(0, 1)
                ) {
                    actionEntries.fastForEachIndexed { index, (name, action) ->
                        DropdownMenuItem(
                            selected = false,
                            onClick = {
                                action()
                                showExportDropdownMenu = false
                            },
                            text = { Text(text = name) },
                            shapes = MenuDefaults.itemShape(index, actionEntries.size),
                            leadingIcon = {
                                val icon = when {
                                    name.contains("export", true) -> Icons.Rounded.FileUpload
                                    name.contains("import", true) -> Icons.Rounded.FileDownload
                                    else -> Icons.Default.RestartAlt
                                }
                                Icon(icon, contentDescription = null)
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun PropertiesView(
        properties: List<PropertyPair<*>>,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            contentPadding = lazyColumnContentPadding(staticVertical = 10.dp)
        ) {
            itemsIndexed(properties, key = { _, item -> item.key.propertyName() }) { index, property ->
                val cardShape = cardShape(properties.size, index) as RoundedCornerShape
                PropertyCard(property, cardShape)
            }
        }
    }

    override val floatingActionButton: @Composable () -> Unit = {
        fun saveConfig() {
            context.coroutineScope.launch(Dispatchers.IO) {
                context.config.writeConfig()
                context.log.verbose("saved config!")
            }
        }

        OnLifecycleEvent { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                saveConfig()
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                saveConfig()
            }
        }
    }


    @Composable
    private fun Container(
        configContainer: ConfigContainer
    ) {
        PropertiesView(remember {
            configContainer.properties.map { PropertyPair(it.key, it.value) }
        })
    }
}