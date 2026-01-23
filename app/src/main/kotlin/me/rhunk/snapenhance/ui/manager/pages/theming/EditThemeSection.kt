@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package me.rhunk.snapenhance.ui.manager.pages.theming

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rhunk.snapenhance.common.data.*
import me.rhunk.snapenhance.common.ui.cardShape
import me.rhunk.snapenhance.common.ui.lazyColumnContentPadding
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableStateList
import me.rhunk.snapenhance.common.ui.transparentTextFieldColors
import me.rhunk.snapenhance.storage.*
import me.rhunk.snapenhance.ui.components.DefaultBottomSheet
import me.rhunk.snapenhance.ui.components.LazyColumnBottomSheet
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.util.BottomSheets
import me.rhunk.snapenhance.ui.util.CircularAlphaTile
import me.rhunk.snapenhance.ui.util.Dialog

class EditThemeSection: Routes.Route() {
    private var saveCallback by mutableStateOf<(() -> Unit)?>(null)
    private var addEntryCallback by mutableStateOf<(key: String, initialColor: Int) -> Unit>({ _, _ -> })
    private var deleteCallback by mutableStateOf<(() -> Unit)?>(null)
    private var themeColors = mutableStateListOf<ThemeColorEntry>()
    private var themeName by mutableStateOf("")
    private var themeDescription by mutableStateOf("")
    private var themeVersion by mutableStateOf("1.0.0")
    private var themeAuthor by mutableStateOf("")
    private var themeUpdateUrl by mutableStateOf("")

    private val bottomSheets by lazy {
        BottomSheets(context.translation)
    }

    override val topBarActions: @Composable (RowScope.() -> Unit) = {
        var deleteConfirmationBottomSheet by remember { mutableStateOf(false) }
        var themeInfoBottomSheet by remember { mutableStateOf(false) }

        if (themeInfoBottomSheet) {
            val focusRequester = remember { FocusRequester() }

            LazyColumnBottomSheet(
                onDismiss = { themeInfoBottomSheet = false },
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    TextField(
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        value = themeName,
                        onValueChange = { themeName = it },
                        label = { Text(translation["name_option"]) },
                        colors = transparentTextFieldColors(),
                        singleLine = true,
                    )

                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        value = themeDescription,
                        onValueChange = { themeDescription = it },
                        label = { Text(translation["description_option"]) },
                        colors = transparentTextFieldColors()
                    )

                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        value = themeVersion,
                        onValueChange = { themeVersion = it },
                        label = { Text(translation["version_option"]) },
                        colors = transparentTextFieldColors()
                    )

                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        value = themeAuthor,
                        onValueChange = { themeAuthor = it },
                        label = { Text(translation["author_option"]) },
                        colors = transparentTextFieldColors()
                    )

                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        value = themeUpdateUrl,
                        onValueChange = { themeUpdateUrl = it },
                        label = { Text(translation["update_url_option"]) },
                        colors = transparentTextFieldColors()
                    )
                }
            }
        }

        if (deleteConfirmationBottomSheet) {
            LazyColumnBottomSheet(
                onDismiss = { deleteConfirmationBottomSheet = false },
            ) {
                Text(
                    text = context.translation["manager.dialogs.delete_theme.title"],
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 5.dp, bottom = 10.dp)
                )

                Text(
                    text = context.translation["manager.dialogs.delete_theme.content"],
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 15.dp)
                )

                HorizontalDivider(
                    modifier = Modifier
                        .padding(top = 15.dp, bottom = 10.dp)
                        .fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(
                        onClick = { deleteConfirmationBottomSheet = false },
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Text(text = context.translation["manager.dialogs.delete_theme.cancel_button"])
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            deleteCallback?.invoke()
                            deleteConfirmationBottomSheet = false
                        },
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Text(text = context.translation["manager.dialogs.delete_theme.confirm_button"])
                    }
                }
            }
        }

        saveCallback?.let {
            IconButton(
                onClick = { it() },
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null)
            }
        }

        deleteCallback?.let {
            IconButton(
                onClick = { deleteConfirmationBottomSheet = true },
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null)
            }
        }

        IconButton(
            onClick = { themeInfoBottomSheet = true },
            shapes = IconButtonDefaults.shapes()
        ) {
            Icon(Icons.Rounded.Edit, contentDescription = null)
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    override val floatingActionButton: @Composable () -> Unit = {
        var addAttributeBottomSheet by remember { mutableStateOf(false) }
        val attributesTranslation =
            remember { context.translation.getCategory("theming_attributes") }

        if (addAttributeBottomSheet) {
            var filter by remember { mutableStateOf("") }
            val attributes =
                rememberAsyncMutableStateList(defaultValue = listOf(), keys = arrayOf(filter)) {
                    AvailableThemingAttributes[ThemingAttributeType.COLOR]?.filter { key ->
                        themeColors.none { it.key == key } && (key.contains(
                            filter,
                            ignoreCase = true
                        ) || attributesTranslation.getOrNull(key)
                            ?.contains(filter, ignoreCase = true) == true)
                    } ?: emptyList()
                }

            DefaultBottomSheet(
                onDismiss = { addAttributeBottomSheet = false },
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                        .heightIn(max = LocalConfiguration.current.screenHeightDp.dp / 1.5f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    stickyHeader {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Text(context.translation["manager.dialogs.add_theme_attribute.title"])
                            TextField(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp),
                                value = filter,
                                onValueChange = { filter = it },
                                label = { Text(context.translation["manager.dialogs.add_theme_attribute.search_attribute"]) },
                                colors = transparentTextFieldColors().copy(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceBright
                                )
                            )
                        }
                    }
                    item {
                        if (attributes.isEmpty()) {
                            Text(context.translation["manager.dialogs.add_theme_attribute.attribute_empty"])
                        }
                    }
                    itemsIndexed(attributes) { index, attribute ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                addEntryCallback(attribute, Color.White.toArgb())
                                addAttributeBottomSheet = false
                            },
                            shape = cardShape(attributes.size, index)
                        ) {
                            val attributeTranslation = remember(attribute) {
                                attributesTranslation.getOrNull(attribute)
                            }

                            Column(
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text(attributeTranslation ?: attribute, lineHeight = 15.sp)
                                attributeTranslation?.let {
                                    Text(
                                        attribute,
                                        fontWeight = FontWeight.Light,
                                        fontSize = 10.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            modifier = Modifier.offset(x = 2.dp, y = 2.dp),
            onClick = { addAttributeBottomSheet = true },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text(translation["add_attribute_button"]) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override val content: @Composable (NavBackStackEntry) -> Unit = {
        val coroutineScope = rememberCoroutineScope()
        val currentThemeId = remember { it.arguments?.getString("theme_id")?.toIntOrNull() }
        val lazyListState = rememberLazyListState()
        var themeInfo by remember { mutableStateOf<DatabaseTheme?>(null) }

        LaunchedEffect(currentThemeId) {
            themeName = ""
            themeDescription = ""
            themeVersion = "1.0.0"
            themeAuthor = ""
            themeUpdateUrl = ""
            themeColors.clear()
            deleteCallback = null
            themeInfo = null

            currentThemeId?.let { themeId ->
                context.database.getThemeInfo(themeId)?.also { theme ->
                    themeInfo = theme
                    themeName = theme.name
                    themeDescription = theme.description ?: ""
                    theme.version?.let { themeVersion = it }
                    themeAuthor = theme.author ?: ""
                    themeUpdateUrl = theme.updateUrl ?: ""
                }

                context.database.getThemeContent(themeId)?.also { content ->
                    themeColors.addAll(content.colors)
                    if (themeColors.isNotEmpty()) {
                        delay(50)
                        lazyListState.scrollToItem(themeColors.size - 1)
                    }
                }

                deleteCallback = {
                    coroutineScope.launch(Dispatchers.IO) {
                        context.database.deleteTheme(currentThemeId)
                        withContext(Dispatchers.Main) {
                            routes.theming.navigateReload()
                        }
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            addEntryCallback = { key, initialColor ->
                coroutineScope.launch(Dispatchers.Main) {
                    themeColors.add(ThemeColorEntry(key, initialColor))
                    delay(100)
                    lazyListState.scrollToItem(themeColors.size - 1)
                }
            }
        }

        LaunchedEffect(themeName, themeColors.size, currentThemeId) {
            if (themeName.isNotBlank() || themeColors.isNotEmpty()) {
                saveCallback = {
                    coroutineScope.launch(Dispatchers.IO) {
                        val currentThemeInfo = themeInfo ?: currentThemeId?.let { context.database.getThemeInfo(it) }

                        val theme = DatabaseTheme(
                            id = currentThemeId ?: -1,
                            enabled = currentThemeInfo?.enabled ?: false,
                            name = themeName,
                            description = themeDescription,
                            version = themeVersion,
                            author = themeAuthor,
                            updateUrl = themeUpdateUrl
                        )
                        val themeId = context.database.addOrUpdateTheme(theme, currentThemeId)
                        context.database.setThemeContent(
                            themeId, DatabaseThemeContent(
                                colors = themeColors
                            )
                        )
                        withContext(Dispatchers.Main) {
                            routes.theming.navigateReload()
                        }
                    }
                }
            } else {
                saveCallback = null
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = lazyListState,
            contentPadding = lazyColumnContentPadding(staticVertical = 10.dp, hasFAB = true),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            reverseLayout = true
        ) {
            itemsIndexed(themeColors) { index, colorEntry ->
                var showEditColorDialog by remember { mutableStateOf(false) }
                var currentColor by remember { mutableIntStateOf(colorEntry.value) }
                val revIndex = themeColors.size - 1 - index

                LaunchedEffect(colorEntry.value) {
                    currentColor = colorEntry.value
                }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showEditColorDialog = true },
                    shape = cardShape(themeColors.size, revIndex)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Colorize,
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            val translation =
                                remember(colorEntry.key) { context.translation.getOrNull("theming_attributes.${colorEntry.key}") }
                            Text(
                                text = translation ?: colorEntry.key,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                lineHeight = 15.sp
                            )
                            translation?.let {
                                Text(
                                    text = colorEntry.key,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Light,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                        CircularAlphaTile(selectedColor = Color(currentColor))
                    }
                }

                if (showEditColorDialog) {
                    Dialog(onDismissRequest = { showEditColorDialog = false }) {
                        bottomSheets.ColorPickerBottomSheet(
                            initialColor = Color(currentColor),
                            setProperty = {
                                if (it == null) {
                                    themeColors.remove(colorEntry)
                                    return@ColorPickerBottomSheet
                                }
                                currentColor = it.toArgb()
                                colorEntry.value = currentColor
                            },
                            dismiss = { showEditColorDialog = false }
                        )
                    }
                }
            }
            item {
                if (themeColors.isEmpty()) {
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
                                text = translation["colors_empty"],
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(8.dp)
                            )
                            Text(
                                text = translation["colors_empty_desc"],
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}