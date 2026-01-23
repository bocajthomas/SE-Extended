@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalHazeMaterialsApi::class)
package me.rhunk.snapenhance.ui.manager

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navigation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp.Companion.Hairline
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import me.rhunk.snapenhance.RemoteSideContext
import me.rhunk.snapenhance.ui.components.LiquidBottomTab
import me.rhunk.snapenhance.ui.components.LiquidBottomTabs
import me.rhunk.snapenhance.common.ui.rememberMutableBooleanPreferenceState
import me.rhunk.snapenhance.common.ui.rememberMutableStringPreferenceState

class Navigation(
    private val context: RemoteSideContext,
    private val navController: NavHostController,
    val routes: Routes = Routes(context).also {
        it.navController = navController
    },
){
    @OptIn(ExperimentalHazeMaterialsApi::class, ExperimentalMaterial3Api::class)
    @Composable
    fun TopBar(
        hazeState: HazeState,
        scrollBehavior: TopAppBarScrollBehavior,
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = remember(navBackStackEntry) { routes.getCurrentRoute(navBackStackEntry) }
        val isSearchBarVisible = currentRoute?.isSearchBarVisible?.invoke() ?: false
        val canGoBack = remember(navBackStackEntry) {
            currentRoute?.let {
                !it.routeInfo.primary || it.routeInfo.childIds.contains(routes.currentDestination)
            } == true
        }

        val useBlurEffect by context.sharedPreferences.rememberMutableBooleanPreferenceState(
            key = "appearance_use_blur",
            defaultValue = false
        )

        val fadeDistance = 200f
        val state = scrollBehavior.state
        val targetAlpha by remember {
            derivedStateOf {
                if (state.contentOffset == 0f) 0f
                else ((-state.contentOffset) / fadeDistance).coerceIn(0f, 1f)
            }
        }

        val animatedAlpha by animateFloatAsState(
            targetValue = targetAlpha,
            animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
            label = "blur_alpha",
        )
        val defaultBackgroundColor = MaterialTheme.colorScheme.surface
        val defaultSurfaceColor = MaterialTheme.colorScheme.surface
        val borderColor = MaterialTheme.colorScheme.outlineVariant
        val topBarColors = TopAppBarDefaults.topAppBarColors(
            containerColor = if (useBlurEffect) Color.Transparent else defaultSurfaceColor,
            scrolledContainerColor = if (useBlurEffect) Color.Transparent else defaultSurfaceColor,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )

        val borderModifier = Modifier.drawBehind {
            if (useBlurEffect && animatedAlpha > 0.01f) {
                val borderSize = Hairline.value
                val y = size.height - borderSize
                drawLine(
                    color = borderColor.copy(alpha = animatedAlpha),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = borderSize
                )
            }
        }
        val hazeModifier = if (useBlurEffect && animatedAlpha > 0.01f) {
            Modifier.hazeEffect(
                state = hazeState,
                style = HazeMaterials.ultraThin(),
            ) {
                this.backgroundColor = defaultBackgroundColor
                this.alpha = animatedAlpha
            }
        } else Modifier

        LaunchedEffect(navBackStackEntry) {
            scrollBehavior.state.heightOffset = 0f
            scrollBehavior.state.contentOffset = 0f
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(hazeModifier)
                .then(borderModifier),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopAppBar(
                title = {
                    if (!isSearchBarVisible) {
                        currentRoute?.apply {
                            title?.invoke() ?: routeInfo.translatedKey?.value?.let {
                                Text(text = it)
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (!isSearchBarVisible) {
                        val backButtonAlpha by animateFloatAsState(
                            targetValue = if (canGoBack) 1f else 0f,
                            label = "back_btn_alpha"
                        )

                        if (backButtonAlpha > 0f) {
                            Box(
                                modifier = Modifier
                                    .graphicsLayer { alpha = backButtonAlpha }
                                    .width(lerp(0.dp, 48.dp, backButtonAlpha))
                                    .height(48.dp)
                            ) {
                                IconButton(
                                    onClick = { if (canGoBack) navController.popBackStack() },
                                    enabled = canGoBack,
                                    shapes = IconButtonDefaults.shapes()
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    currentRoute?.topBarActions?.invoke(this)
                },
                colors = topBarColors,
                scrollBehavior = scrollBehavior,
                windowInsets = TopAppBarDefaults.windowInsets
            )

            Row(
                modifier = Modifier.fillMaxWidth().background(if (useBlurEffect) Color.Transparent else defaultBackgroundColor),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                currentRoute?.topBarSubActions?.invoke(this)
            }
        }
    }

    @OptIn(ExperimentalHazeMaterialsApi::class)
    @Composable
    fun BottomBar(hazeState: HazeState) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = remember(navBackStackEntry) { routes.getCurrentRoute(navBackStackEntry) }
        val primaryRoutes = remember { routes.getRoutes().filter { it.routeInfo.showInNavBar } }
        val useBlurEffect by context.sharedPreferences.rememberMutableBooleanPreferenceState(
            "appearance_use_blur",
            false
        )
        val liquidBackdrop = rememberLayerBackdrop()
        val hideBottomBarLabels by context.sharedPreferences.rememberMutableBooleanPreferenceState(
            "appearance_hide_bottom_bar_labels",
            false
        )
        val useLiquidGlass by context.sharedPreferences.rememberMutableBooleanPreferenceState(
            "appearance_use_liquid_glass",
            false
        )

        val containerColor = NavigationBarDefaults.containerColor
        val borderColor = MaterialTheme.colorScheme.outlineVariant

        val blurModifier = if (useBlurEffect) {
            Modifier
                .hazeEffect(
                    state = hazeState,
                    style = HazeMaterials.ultraThin()
                ) {
                    backgroundColor = containerColor
                }
                .drawBehind {
                    val borderSize = Hairline.value
                    drawLine(
                        color = borderColor,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = borderSize
                    )
                }
        } else Modifier

        val barContainerColor = if (useBlurEffect) Color.Transparent else containerColor
        val navBarShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)

        val selectedIndex by remember(currentRoute) {
            derivedStateOf {
                primaryRoutes.indexOfFirst { it.routeInfo.id == currentRoute?.routeInfo?.id }
            }
        }

        if (!useLiquidGlass) {
            NavigationBar(
                blurModifier.clip(navBarShape),
                containerColor = barContainerColor,
                contentColor = MaterialTheme.colorScheme.contentColorFor(barContainerColor),
                tonalElevation = 0.dp,
            ) {
                primaryRoutes.forEachIndexed { index, route ->
                    val isSelected = selectedIndex == index
                    NavigationBarItem(
                        alwaysShowLabel = !hideBottomBarLabels || currentRoute == route,
                        icon = {
                            Icon(imageVector = route.routeInfo.icon, contentDescription = null)
                        },
                        label = {
                            Text(
                                textAlign = TextAlign.Center,
                                softWrap = false,
                                fontSize = 12.sp,
                                modifier = Modifier.wrapContentWidth(unbounded = true),
                                text = remember(context.translation.loadedLocale) { context.translation["manager.routes.${route.routeInfo.key.substringBefore("/")}"] },
                            )
                        },
                        selected = currentRoute == route,
                        onClick = {
                            val currentEntry = navController.currentBackStackEntry
                            val currentRouteId = currentEntry?.destination?.route
                            val isExactlyAtRoot = currentRouteId == route.routeInfo.id || currentRouteId == "main_${route.routeInfo.id}"

                            if (!isSelected || !isExactlyAtRoot) {
                                route.navigateReset()
                            }
                        }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                LiquidBottomTabs(
                    selectedTabIndex = { selectedIndex },
                    onTabSelected = { index ->
                        if (index >= 0) primaryRoutes[index].navigateReset()
                    },
                    backdrop = liquidBackdrop,
                    tabsCount = primaryRoutes.size,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(start = 10.dp, end = 10.dp, bottom = 55.dp)
                ) {
                    primaryRoutes.forEachIndexed { index, route ->
                        val isSelected = selectedIndex == index
                        LiquidBottomTab(
                            onClick = {
                                val currentEntry = navController.currentBackStackEntry
                                val currentRouteId = currentEntry?.destination?.route
                                val isExactlyAtRoot = currentRouteId == route.routeInfo.id || currentRouteId == "main_${route.routeInfo.id}"

                                if (!isSelected || !isExactlyAtRoot) {
                                    route.navigateReset()
                                }
                            }
                        ) {
                            Icon(imageVector = route.routeInfo.icon, contentDescription = null)

                            if (!hideBottomBarLabels || isSelected) {
                                Text(
                                    textAlign = TextAlign.Center,
                                    softWrap = false,
                                    fontSize = 12.sp,
                                    modifier = Modifier.wrapContentWidth(unbounded = true),
                                    text = remember(context.translation.loadedLocale) { context.translation["manager.routes.${route.routeInfo.key.substringBefore("/")}"] },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun FloatingActionButton() {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        remember(navBackStackEntry) { routes.getCurrentRoute(navBackStackEntry) }?.floatingActionButton?.invoke()
    }

    @Composable
    fun Content(paddingValues: PaddingValues, startDestination: String, hazeState: HazeState) {
        val contentPadding = PaddingValues(top = 0.dp, bottom = 0.dp)
        val animationSpeed by context.sharedPreferences.rememberMutableStringPreferenceState(
            key = "animation_speed",
            defaultValue = "300"
        )

        val animationSpeedInt = animationSpeed.toIntOrNull() ?: 300

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize()
                .hazeSource(state = hazeState),
            enterTransition = { fadeIn(tween(animationSpeedInt)) },
            exitTransition = { fadeOut(tween(animationSpeedInt)) }
        ) {
            routes.getRoutes().filter { it.parentRoute == null }.forEach { route ->
                val children = routes.getRoutes().filter { it.parentRoute == route }
                if (children.isEmpty()) {
                    composable(route.routeInfo.id) {
                        route.content.invoke(it)
                    }
                    route.customComposables.invoke(this)
                } else {
                    navigation("main_" + route.routeInfo.id, route.routeInfo.id) {
                        composable("main_" + route.routeInfo.id) {
                            route.content.invoke(it)
                        }
                        children.forEach { child ->
                            composable(child.routeInfo.id) {
                                child.content.invoke(it)
                            }
                        }
                        route.customComposables.invoke(this)
                    }
                }
            }
        }
    }
}