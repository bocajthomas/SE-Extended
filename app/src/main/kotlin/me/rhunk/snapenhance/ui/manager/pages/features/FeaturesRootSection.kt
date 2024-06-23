package me.rhunk.snapenhance.ui.manager.pages.features

import android.content.Intent
import android.net.Uri
import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.github.skydoves.colorpicker.compose.AlphaTile
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.common.config.*
import me.rhunk.snapenhance.common.config.impl.UserInterfaceTweaks.ColorsConfig
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableStateList
import me.rhunk.snapenhance.ui.manager.MainActivity
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.util.*

class FeaturesRootSection : Routes.Route() {
    private val alertDialogs by lazy { AlertDialogs(context.translation) }

    companion object {
        const val FEATURE_CONTAINER_ROUTE = "feature_container/{name}"
        const val SEARCH_FEATURE_ROUTE = "search_feature/{keyword}"
    }

    private var activityLauncherHelper: ActivityLauncherHelper? = null

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
            //open manager if activity launcher is null
            val intent = Intent(context.androidContext, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("route", routeInfo.id)
            context.androidContext.startActivity(intent)
        }
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

    @Composable
    private fun PropertyAction(property: PropertyPair<*>, registerClickCallback: RegisterClickCallback) {
        var showDialog by remember { mutableStateOf(false) }
        var dialogComposable by remember { mutableStateOf<@Composable () -> Unit>({}) }

        fun registerDialogOnClickCallback() = registerClickCallback { showDialog = true }

        if (showDialog) {
            Dialog(
                properties = DialogProperties(
                    usePlatformDefaultWidth = false
                ),
                onDismissRequest = { showDialog = false },
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
                var selectedFile by remember(files.size) { mutableStateOf(files.firstOrNull { it.name == propertyValue.getNullable() }.also {
                    if (files.isNotEmpty() && it == null) propertyValue.setAny(null)
                }?.name) }

                Card(
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().padding(4.dp),
                    ) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = context.translation["manager.dialogs.file_imports.settings_select_file_hint"],
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                if (isEmpty) {
                                    Text(
                                        text = context.translation["manager.dialogs.file_imports.no_files_settings_hint"],
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(top = 10.dp),
                                    )
                                }
                            }
                        }
                        items(files, key = { it.name }) { file ->
                            Row(
                                modifier = Modifier.clickable {
                                    selectedFile = if (selectedFile == file.name) null else file.name
                                    propertyValue.setAny(selectedFile)
                                }.padding(5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.padding(5.dp))
                                Text(
                                    text = file.name,
                                    modifier = Modifier
                                        .padding(3.dp)
                                        .weight(1f),
                                    fontSize = 14.sp,
                                    lineHeight = 16.sp
                                )
                                if (selectedFile == file.name) {
                                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.padding(5.dp))
                                }
                            }
                        }
                    }
                }
            }

            Icon(Icons.Filled.AttachFile, contentDescription = null)
            return
        }

        if (property.key.params.flags.contains(ConfigFlag.FOLDER)) {
            IconButton(onClick = registerClickCallback {
                activityLauncher {
                    chooseFolder { uri ->
                        propertyValue.setAny(uri)
                    }
                }
            }.let { { it.invoke(true) } }) {
                Icon(Icons.Filled.FolderOpen, contentDescription = null)
            }
            return
        }

        when (val dataType = remember { property.key.dataType.type }) {
            DataProcessors.Type.BOOLEAN -> {
                var state by remember { mutableStateOf(propertyValue.get() as Boolean) }
                Switch(
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
                    alertDialogs.ChooseLocationDialog(property) {
                        showDialog = false
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
                    alertDialogs.UniqueSelectionDialog(property)
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
                            alertDialogs.MultipleSelectionDialog(property)
                        }
                        DataProcessors.Type.STRING, DataProcessors.Type.INTEGER, DataProcessors.Type.FLOAT -> {
                            alertDialogs.KeyboardInputDialog(property) { showDialog = false }
                        }
                        else -> {}
                    }
                }

                registerDialogOnClickCallback().let { { it.invoke(true) } }.also {
                    if (dataType == DataProcessors.Type.INTEGER ||
                        dataType == DataProcessors.Type.FLOAT) {
                        FilledIconButton(onClick = it) {
                            Text(
                                text = propertyValue.get().toString(),
                                modifier = Modifier.wrapContentWidth(),
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        IconButton(onClick = it) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                        }
                    }
                }
            }

            DataProcessors.Type.INT_COLOR -> {
                dialogComposable = {
                    alertDialogs.ColorPickerDialog(property) {
                        showDialog = false
                    }
                }

                registerDialogOnClickCallback().let { { it.invoke(true) } }.also {
                    val selectedColor = (propertyValue.getNullable() as? Int)?.let { Color(it) }
                    AlphaTile(
                        modifier = Modifier
                            .size(30.dp)
                            .border(2.dp, Color.White, shape = RoundedCornerShape(15.dp))
                            .clip(RoundedCornerShape(15.dp)),
                        selectedColor = selectedColor ?: Color.Transparent,
                        tileEvenColor = selectedColor?.let { Color(0xFFCBCBCB) } ?: Color.Transparent,
                        tileOddColor = selectedColor?.let { Color.White } ?: Color.Transparent,
                        tileSize = 8.dp,
                    )
                }
            }

            DataProcessors.Type.CONTAINER -> {
                val container = propertyValue.get() as ConfigContainer

                registerClickCallback {
                    routes.navController.navigate(FEATURE_CONTAINER_ROUTE.replace("{name}", property.name))
                }

                if (!container.hasGlobalState) return

                var state by remember { mutableStateOf(container.globalState ?: false) }

                Box(
                    modifier = Modifier
                        .padding(end = 15.dp),
                ) {

                    Box(modifier = Modifier
                        .height(50.dp)
                        .width(1.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(5.dp)
                        ))
                }

                Switch(
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
    private fun PropertyCard(property: PropertyPair<*>) {
        var clickCallback by remember { mutableStateOf<ClickCallback?>(null) }
        val noticeColorMap = mapOf(
            FeatureNotice.UNSTABLE.key to Color(0xFFFFFB87),
            FeatureNotice.BAN_RISK.key to Color(0xFFFF8585),
            FeatureNotice.INTERNAL_BEHAVIOR.key to Color(0xFFFFFB87),
            FeatureNotice.REQUIRE_NATIVE_HOOKS.key to Color(0xFFFF5722),
        )

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, top = 5.dp, bottom = 5.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        clickCallback?.invoke(true)
                    }
                    .padding(all = 4.dp),
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
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(all = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PropertyAction(property, registerClickCallback = { callback ->
                        clickCallback = callback
                        callback
                    })
                }
            }
        }
    }

    @Composable
    private fun FeatureSearchBar(rowScope: RowScope, focusRequester: FocusRequester) {
        var searchValue by remember { mutableStateOf("") }
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

                keyboardActions = KeyboardActions(onDone = {
                    focusRequester.freeFocus()
                }),
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .weight(1f, fill = true)
                    .padding(end = 10.dp)
                    .height(70.dp),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }

    override val topBarActions: @Composable (RowScope.() -> Unit) = topBarActions@{
        var showSearchBar by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }

        if (showSearchBar) {
            FeatureSearchBar(this, focusRequester)
            LaunchedEffect(true) {
                focusRequester.requestFocus()
            }
        }

        IconButton(onClick = {
            showSearchBar = showSearchBar.not()
            if (!showSearchBar && routes.currentDestination == SEARCH_FEATURE_ROUTE) {
                navigateToMainRoot()
            }
        }) {
            Icon(
                imageVector = if (showSearchBar) Icons.Filled.Close
                else Icons.Filled.Search,
                contentDescription = null
            )
        }

        if (showSearchBar) return@topBarActions

        var showExportDropdownMenu by remember { mutableStateOf(false) }
        var showResetConfirmationDialog by remember { mutableStateOf(false) }
        var showExportDialog by remember { mutableStateOf(false) }
        var showExportColorThemeDialog by remember { mutableStateOf(false) }

        if (showResetConfirmationDialog) {
            AlertDialog(
                title = { Text(text = context.translation["manager.dialogs.reset_config.title"]) },
                text = { Text(text = context.translation["manager.dialogs.reset_config.content"]) },
                onDismissRequest = { showResetConfirmationDialog = false },
                confirmButton = {
                    Button(
                        onClick = {
                            context.config.reset()
                            context.shortToast(context.translation["manager.dialogs.reset_config.success_toast"])
                            showResetConfirmationDialog = false
                        }
                    ) {
                        Text(text = context.translation["button.positive"])
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            showResetConfirmationDialog = false
                        }
                    ) {
                        Text(text = context.translation["button.negative"])
                    }
                }
            )
        }

        if (showExportDialog) {
            fun exportConfig(
                exportSensitiveData: Boolean
            ) {
                showExportDialog = false
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

            AlertDialog(
                title = { Text(text = context.translation["manager.dialogs.export_config.title"]) },
                text = { Text(text = context.translation["manager.dialogs.export_config.content"]) },
                onDismissRequest = { showExportDialog = false },
                confirmButton = {
                    Button(
                        onClick = { exportConfig(true) }
                    ) {
                        Text(text = context.translation["button.positive"])
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { exportConfig(false) }
                    ) {
                        Text(text = context.translation["button.negative"])
                    }
                }
            )
        }
        
        if (showExportColorThemeDialog) {
            fun exportColorTheme() {
                showExportColorThemeDialog = false
                activityLauncher {
                    fun getColorValue(propertyValue: Any?): Int? {
                        return when (propertyValue) {
                            is Int -> propertyValue
                            is String -> try {
                                AndroidColor.parseColor(propertyValue.trim())
                            } catch (e: IllegalArgumentException) {
                                null
                            }
                            else -> {
                                if (propertyValue is Color) {
                                    propertyValue.toArgb()
                                } else {
                                    null
                                }
                            }
                        }
                    }
                    saveFile("theme_colors.json", "application/json") { uri ->
                        runCatching {
                            context.androidContext.contentResolver.openOutputStream(Uri.parse(uri))
                                ?.use { outputStream ->
                                    val colorConfig = ColorsConfig()
                                    val colorData = hashMapOf<String, Int>()

                                    for (colorProperty in ColorsConfig::class.java.declaredFields) {
                                        val colorName = colorProperty.name
                                        if (colorName.endsWith("Color")) {
                                            val getterMethod = ColorsConfig::class.java.getDeclaredMethod(
                                                "get${colorName.replaceFirstChar { it.uppercase() }}"
                                            )
                                            val colorValue = getColorValue(getterMethod.invoke(colorConfig))
                                            if (colorValue != null) {
                                                colorData[colorName] = colorValue
                                            } else {
                                                context.log.warn("ThemeExport", "Color not found for: $colorName")
                                            }
                                        }
                                    }

                                    val jsonData = hashMapOf<String, Any>(
                                        "customize_ui" to hashMapOf<String, Any>(
                                            "properties" to hashMapOf<String, Any>(
                                                "theme_picker" to "custom",
                                                "colors" to hashMapOf<String, Any>(
                                                    "properties" to colorData
                                                )
                                            )
                                        )
                                    )
                                    val jsonData = Gson().toJson(jsonData)
                                    outputStream.write(jsonData.toByteArray())
                                    context.shortToast(translation["theme_export_success_toast"])
                                }
                        }.onFailure {
                            context.longToast(translation.format("theme_export_failure_toast","error" to it.message.toString()))
                        }
                    }
                }
            }


            AlertDialog(
                title = { Text(text = context.translation["manager.dialogs.export_theme.title"]) },
                text = { Text(text = context.translation["manager.dialogs.export_theme.content"]) },
                onDismissRequest = { showExportColorThemeDialog = false },
                confirmButton = {
                    Button(
                        onClick = { exportColorTheme() }
                    ) {
                        Text(text = context.translation["button.positive"])
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { exportColorTheme() }
                    ) {
                        Text(text = context.translation["button.negative"])
                    }
                }
            )
        }
        
        val actions = remember {
            mapOf(
                translation["export_option"] to { showExportDialog = true },
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
                translation["export_color_theme"] to { showExportColorThemeDialog = true },
                translation["import_color_theme"] to {
                    activityLauncher {
                        openFile("application/json") { uri ->
                            context.androidContext.contentResolver.openInputStream(Uri.parse(uri))?.use { inputStream ->
                                runCatching {
                                    val jsonString = inputStream.readBytes().toString(Charsets.UTF_8)
                                    context.config.loadFromString(jsonString)
                                }.onFailure {
                                    context.longToast(translation.format("theme_import_failure_toast", "error" to it.message.toString()))
                                    return@use
                                }
                                context.shortToast(translation["theme_import_success_toast"])
                                context.coroutineScope.launch(Dispatchers.Main) {
                                    navigateReload()
                                }
                            }
                        }
                    }
                },
                translation["reset_option"] to { showResetConfirmationDialog = true }
            )
        }

        if (context.activity != null) {
            IconButton(onClick = { showExportDropdownMenu = !showExportDropdownMenu}) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = null
                )
            }
        }

        if (showExportDropdownMenu) {
            DropdownMenu(expanded = true, onDismissRequest = { showExportDropdownMenu = false }) {
                actions.forEach { (name, action) ->
                    DropdownMenuItem(
                        text = {
                            Text(text = name)
                        },
                        onClick = {
                            action()
                            showExportDropdownMenu = false
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun PropertiesView(
        properties: List<PropertyPair<*>>
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            content = { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(innerPadding),
                    //save button space
                    contentPadding = PaddingValues(top = 10.dp, bottom = 110.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    items(properties) {
                        PropertyCard(it)
                    }
                }
            }
        )
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

