@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package me.rhunk.snapenhance.ui.manager.pages.home

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.R
import me.rhunk.snapenhance.action.EnumQuickActions
import me.rhunk.snapenhance.common.BuildConfig
import me.rhunk.snapenhance.common.action.EnumAction
import me.rhunk.snapenhance.common.ui.*
import me.rhunk.snapenhance.storage.getQuickTiles
import me.rhunk.snapenhance.storage.setQuickTiles
import me.rhunk.snapenhance.ui.components.LazyColumnBottomSheet
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.util.ActivityLauncherHelper
import me.rhunk.snapenhance.ui.manager.data.updater.*
import java.text.DateFormat

class HomeRootSection : Routes.Route() {
    companion object {
        val cardMargin = 10.dp
    }
    private lateinit var activityLauncherHelper: ActivityLauncherHelper
    private val cards by lazy {
        EnumQuickActions.entries.map {
            (context.translation["actions.${it.key}.name"] to it.icon) to it.action
        }.associate {
            it.first to it.second
        }.toMutableMap().apply {
            EnumAction.entries.forEach { action ->
                this[context.translation["actions.${action.key}.name"] to action.icon] = {
                    context.launchActionIntent(action)
                }
            }
        }
    }

    // TODO: Refactor into a helper class
    private fun openLink(link: String) {
        kotlin.runCatching {
            context.activity?.startActivity(Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                data = link.toUri()
            })
        }.onFailure {
            context.log.error("Couldn't open link", it)
            context.shortToast("Couldn't open link. Check SE Extended logs for more details.")
        }
    }

    // TODO: Refactor into components/
    @Composable
    fun ExternalLinkIcon(
        modifier: Modifier = Modifier,
        size: Dp = 45.dp,
        imageVector: ImageVector,
        link: String,
        iconScale: Float = 1.0f
    ) {
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = { openLink(link) },
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(size * 0.75f)
                        .scale(iconScale)
                )
            }
        }
    }

    override val init: () -> Unit = {
        activityLauncherHelper = ActivityLauncherHelper(context.activity!!)
    }

    override val topBarActions: @Composable (RowScope.() -> Unit) = {
        IconButton(
            onClick = { routes.homeLogs.navigate() },
            shapes = IconButtonDefaults.shapes()
        ) {
            Icon(Icons.Rounded.BugReport, contentDescription = null)
        }

        IconButton(
            onClick = { routes.homeSettings.navigate() },
            shapes = IconButtonDefaults.shapes()
        ) {
            Icon(Icons.Rounded.Settings, contentDescription = null)
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    override val content: @Composable (NavBackStackEntry) -> Unit = {
        val avenirNextFontFamily = remember {
            FontFamily(
                Font(R.font.avenir_next_medium, FontWeight.Medium)
            )
        }
        val scrollState = remember { ScrollState(0) }
        val clicks = remember { mutableIntStateOf(0) }
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .screenContentPadding(staticVertical = 10.dp)
        ) {
            Text(
                text = remember {
                    intArrayOf(
                        100, 101, 100, 110, 101, 116, 120, 69, 32, 69, 83
                    ).map { it.toChar() }.joinToString("").reversed()
                },
                fontSize = 30.sp,
                fontFamily = avenirNextFontFamily,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Text(
                text = translation.format(
                    "version_title",
                    "versionName" to BuildConfig.VERSION_NAME
                ),
                fontSize = 12.sp,
                fontFamily = remember {
                    FontFamily(
                        Font(R.font.avenir_next_medium, FontWeight.Medium)
                    )
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable {
                        clicks.intValue += 1

                        if (clicks.intValue >= 5) {
                            routes.easterEgg.navigate()
                            clicks.intValue = 0
                        }
                    },
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    13.dp,
                    Alignment.CenterHorizontally
                ),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 5.dp)
            ) {

                ExternalLinkIcon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_telegram_round),
                    link = "https://t.me/SE_Extended",
                    iconScale = 1.0f
                )

                ExternalLinkIcon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_github),
                    link = "https://github.com/bocajthomas/SE-Extended",
                    iconScale = 1.0f
                )

                ExternalLinkIcon(
                    imageVector = Icons.AutoMirrored.Rounded.Help,
                    link = "https://github.com/bocajthomas/SE-Extended/wiki",
                    iconScale = 1.2f
                )

                ExternalLinkIcon(
                    imageVector = Icons.Rounded.Paid,
                    link = "https://ko-fi.com/seextended",
                    iconScale = 1.2f
                )
            }
            val selectedTiles = rememberAsyncMutableStateList(defaultValue = listOf()) {
                context.database.getQuickTiles()
            }
            var shouldShowUpdates by context.sharedPreferences.rememberMutableBooleanPreferenceState(
                key = "app_update_checker",
                defaultValue = true
            )
            var promoMode by context.sharedPreferences.rememberMutableBooleanPreferenceState(
                key = "promo_mode",
                defaultValue = false
            )
            var latestUpdate by remember { mutableStateOf(UpdateFetcher.cachedRelease) }
            val progress by downloadProgress.collectAsState()
            val isDownloading by isDownloading.collectAsState()
            val androidContext = LocalContext.current
            val isUpdateCardVisible = if (shouldShowUpdates) latestUpdate != null else false
            val isDebugCardVisible = BuildConfig.DEBUG
            val updateCardShape = if (isDebugCardVisible) cardShapeGroupedTop else cardShapeSingle
            val debugInfoCardShape = when {
                isUpdateCardVisible -> cardShapeGroupedBottom
                else -> cardShapeSingle
            }

            LaunchedEffect(Unit) {
                if (!shouldShowUpdates) return@LaunchedEffect
                UpdateFetcher.fetchUpdateInBackground { result ->
                    latestUpdate = result
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {

                if (promoMode) return@Column

                if (isUpdateCardVisible || isDebugCardVisible) {
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (isUpdateCardVisible) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = updateCardShape
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(all = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = translation["update_title"],
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        fontSize = 12.sp,
                                        text = translation.format(
                                            "update_content",
                                            "version" to (latestUpdate?.versionName ?: "unknown")
                                        ),
                                        lineHeight = 20.sp,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Button(
                                    modifier = Modifier.height(40.dp),
                                    onClick = {
                                        downloadUpdate(
                                            context = androidContext,
                                            logger = context.log,
                                            fileProviderAuthority = androidContext.packageName + ".fileprovider"
                                        )
                                    },
                                    enabled = !isDownloading,
                                    shapes = ButtonDefaults.shapes()
                                ) {
                                    if (isDownloading) {
                                        Text(text = translation["downloading_button"])
                                    } else {
                                        Text(text = translation["download_button"])
                                    }
                                }
                            }

                            if (isDownloading) {
                                Spacer(modifier = Modifier.height(5.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LinearWavyProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(8.dp)
                                            .padding(end = 10.dp)
                                    )

                                    Text(
                                        text = "${(progress * 100).toInt()}%",
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }

                if (isDebugCardVisible) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = debugInfoCardShape
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(all = 10.dp)
                        ) {
                            Text(
                                text = translation["debug_build_summary_title"],
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            val buildSummary = buildAnnotatedString {
                                withStyle(
                                    style = SpanStyle(
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Light
                                    )
                                ) {
                                    append(
                                        remember {
                                            translation.format(
                                                "debug_build_summary_content",
                                                "versionName" to BuildConfig.VERSION_NAME,
                                                "versionCode" to BuildConfig.VERSION_CODE.toString(),
                                            )
                                        }
                                    )
                                    append(" - ")
                                }
                                pushStringAnnotation(
                                    tag = "git_hash",
                                    annotation = BuildConfig.GIT_HASH
                                )
                                withStyle(
                                    style = SpanStyle(
                                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    append(BuildConfig.GIT_HASH.substring(0, 7))
                                }
                                pop()
                            }
                            ClickableText(
                                text = buildSummary,
                                onClick = { offset ->
                                    buildSummary.getStringAnnotations(
                                        tag = "git_hash", start = offset, end = offset
                                    ).firstOrNull()?.let {
                                        openLink("https://github.com/bocajthomas/SE-Extended/commit/${it.item}")
                                    }
                                }
                            )
                            Text(
                                fontSize = 12.sp,
                                text = remember {
                                    translation.format(
                                        "debug_build_summary_date",
                                        "date" to DateFormat.getDateTimeInstance()
                                            .format(BuildConfig.BUILD_TIMESTAMP),
                                        "days" to ((System.currentTimeMillis() - BuildConfig.BUILD_TIMESTAMP) / 86400000).toInt()
                                            .toString()
                                    )
                                },
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Light
                            )
                        }
                    }
                }
            }

            var showQuickActionsMenu by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 5.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    translation["quick_actions_title"], fontSize = 20.sp,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { showQuickActionsMenu = !showQuickActionsMenu },
                    shapes = IconButtonDefaults.shapes()
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null)
                }

                if (showQuickActionsMenu) {
                    LazyColumnBottomSheet(
                        onDismiss = { showQuickActionsMenu = false },
                    ) {
                        val cardList = cards.toList()

                        Column(
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            cardList.forEachIndexed { index, (cardInfo, action) ->
                                val (title, icon) = cardInfo

                                fun toggle(state: Boolean? = null) {
                                    val isCurrentlySelected = selectedTiles.contains(title)
                                    val targetState = state ?: !isCurrentlySelected

                                    if (targetState) {
                                        selectedTiles.add(0, title)
                                    } else {
                                        selectedTiles.remove(title)
                                    }

                                    context.coroutineScope.launch {
                                        context.database.setQuickTiles(selectedTiles)
                                    }
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = cardShape(cardList.size, index)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.clickable { toggle() }
                                            .fillMaxWidth()
                                    ) {
                                        Text(
                                            text = title,
                                            fontSize = 16.sp,
                                            modifier = Modifier.weight(1f)
                                                .padding(start = 10.dp)
                                        )

                                        Checkbox(
                                            checked = selectedTiles.contains(title),
                                            onCheckedChange = {
                                                toggle(it)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                maxItemsInEachRow = 3,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                val tileHeight = LocalDensity.current.run {
                    remember { (context.androidContext.resources.displayMetrics.widthPixels / 3).toDp() - cardMargin / 2 }
                }

                remember(selectedTiles.size, context.translation.loadedLocale) {
                    selectedTiles.mapNotNull {
                        cards.entries.find { entry -> entry.key.first == it }
                    }
                }.forEach { (card, action) ->
                    ElevatedCard(
                        modifier = Modifier
                            .height(tileHeight)
                            .weight(1f),
                        onClick = { action(routes) },
                        shape = cardShapeSingle
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(all = 5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Icon(
                                imageVector = card.second, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(42.dp)
                            )
                            Text(
                                text = card.first,
                                lineHeight = 16.sp,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}