@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package me.rhunk.snapenhance.ui.manager.pages.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import me.rhunk.snapenhance.common.ui.cardShape
import me.rhunk.snapenhance.common.ui.cardShapeGroupedBottom
import me.rhunk.snapenhance.common.ui.cardShapeGroupedMiddle
import me.rhunk.snapenhance.common.ui.cardShapeGroupedTop
import me.rhunk.snapenhance.common.ui.cardShapeSingle
import me.rhunk.snapenhance.common.ui.lazyColumnContentPadding
import me.rhunk.snapenhance.common.ui.rememberMutableBooleanPreferenceState
import me.rhunk.snapenhance.common.ui.rememberMutableStringPreferenceState
import me.rhunk.snapenhance.ui.components.DefaultBottomSheet
import me.rhunk.snapenhance.ui.components.IconSelector
import me.rhunk.snapenhance.ui.components.LazyColumnBottomSheet
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.manager.data.AppIcons
import me.rhunk.snapenhance.ui.util.ActivityLauncherHelper
import me.rhunk.snapenhance.ui.util.switchAppIcon

class HomeAppearance: Routes.Route() {
    private lateinit var activityLauncherHelper: ActivityLauncherHelper

    override val init: () -> Unit = {
        activityLauncherHelper = ActivityLauncherHelper(context.activity!!)
    }

    override val content: @Composable (NavBackStackEntry) -> Unit = {
        val useBlurEffectState = context.sharedPreferences.rememberMutableBooleanPreferenceState(
            key = "appearance_use_blur",
            defaultValue = false
        )
        val useBlurEffect by useBlurEffectState

        var showDefaultPageMenu by remember { mutableStateOf(false) }
        var showAppIconMenu by remember { mutableStateOf(false) }
        var showAnimationSpeedMenu by remember { mutableStateOf(false) }

        var hideBottomBarLabels = context.sharedPreferences.rememberMutableBooleanPreferenceState(
            key = "appearance_hide_bottom_bar_labels",
            defaultValue = false
        )
        val useHideBottomBarLabels by hideBottomBarLabels

        val useLiquidGlassEffectState = context.sharedPreferences.rememberMutableBooleanPreferenceState(
            key = "appearance_use_liquid_glass",
            defaultValue = false
        )
        val useLiquidGlass by useLiquidGlassEffectState

        val defaultPageOptions = listOf(
            "home" to context.translation["manager.routes.home"],
            "features" to context.translation["manager.routes.features"],
            "tasks" to context.translation["manager.routes.tasks"],
            "social" to context.translation["manager.routes.social"],
            "scripts" to context.translation["manager.routes.scripts"]
        )
        var defaultPage = context.sharedPreferences.rememberMutableStringPreferenceState(
            key = "appearance_default_page",
            defaultValue = "home"
        )

        val getDefaultPage by defaultPage
        val defaultPageLabel = defaultPageOptions.find { it.first == defaultPage.value }?.second
            ?: context.translation["manager.routes.home"]

        var appIcon = context.sharedPreferences.rememberMutableStringPreferenceState(
            key = "appearance_app_icon",
            defaultValue = "default"
        )
        val getAppIcon by appIcon

        val animationSpeedsOptions = listOf(
            "500" to context.translation["manager.animation_speeds.slow"],
            "300" to context.translation["manager.animation_speeds.medium"],
            "100" to context.translation["manager.animation_speeds.fast"]
        )

        var animationSpeed = context.sharedPreferences.rememberMutableStringPreferenceState(
            key = "animation_speed",
            defaultValue = "300"
        )
        val getAnimationSpeed by animationSpeed

        val animationSpeedLabel = animationSpeedsOptions.find { it.first == animationSpeed.value }?.second
            ?: translation["manager.animation_speeds.medium"]

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = lazyColumnContentPadding(staticVertical = 10.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            item {
                Text(
                    text = translation["theming_title"],
                    modifier = Modifier.padding(16.dp),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShapeGroupedTop,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { routes.homeAppTheme.navigate() }
                                .padding(15.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = translation["app_theme_title"],
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 20.sp
                                )
                                Text(
                                    text = translation["app_theme_description"],
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Light,
                                    lineHeight = 15.sp
                                )
                            }

                            IconButton(
                                onClick = { routes.homeAppTheme.navigate() },
                                modifier = Modifier.padding(end = 2.dp),
                                shapes = IconButtonDefaults.shapes()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShapeGroupedMiddle,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    useBlurEffectState.value = !useBlurEffectState.value
                                }
                                .padding(15.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = translation["blur_effect_title"],
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 20.sp
                                )
                                Text(
                                    text = translation["blur_effect_description"],
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Light,
                                    lineHeight = 15.sp
                                )
                            }

                            Switch(
                                checked = useBlurEffect,
                                onCheckedChange = { newState ->
                                    useBlurEffectState.value = newState
                                }
                            )
                        }
                    }

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShapeGroupedBottom,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showAppIconMenu = true
                                }
                                .padding(15.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = translation["app_icon_title"],
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 20.sp
                                )
                                Text(
                                    text = translation["app_icon_description"],
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Light,
                                    lineHeight = 15.sp
                                )
                            }
                            val iconKey = AppIcons.getIconByKey(getAppIcon)
                            val iconResourceID  = iconKey.resourceId
                            val iconColorID = iconKey.colorId

                            Box(
                                modifier = Modifier
                                    .size(45.dp)
                                    .clip(CircleShape)
                                    .background(colorResource(iconColorID)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = iconResourceID),
                                    contentDescription = null,
                                    contentScale = ContentScale.FillBounds,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                    if (showAppIconMenu) {
                        val localContext = LocalContext.current
                        DefaultBottomSheet(
                            onDismiss = { showAppIconMenu = false }
                        ) {
                            IconSelector(context = this@HomeAppearance.context) { selectedIcon ->
                                appIcon.value = selectedIcon.key
                                switchAppIcon(localContext, selectedIcon.key)
                                showAppIconMenu = false
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = translation["navigation_bar_title"],
                    modifier = Modifier.padding(16.dp),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShapeGroupedTop,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDefaultPageMenu = true }
                                .padding(15.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = translation["default_page_title"],
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 20.sp
                                )
                                Text(
                                    text = translation["default_page_description"],
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Light,
                                    lineHeight = 15.sp
                                )
                            }
                            Text(
                                text = defaultPageLabel,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 17.sp
                            )
                        }
                    }

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShapeGroupedMiddle,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    hideBottomBarLabels.value = !hideBottomBarLabels.value
                                }
                                .padding(15.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = translation["hide_bottom_bar_labels_title"],
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 20.sp
                                )
                                Text(
                                    text = translation["hide_bottom_bar_labels_description"],
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Light,
                                    lineHeight = 15.sp
                                )
                            }

                            Switch(
                                checked = useHideBottomBarLabels,
                                onCheckedChange = { newState ->
                                    hideBottomBarLabels.value = newState
                                }
                            )
                        }
                    }

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShapeGroupedBottom,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    useLiquidGlassEffectState.value =
                                        !useLiquidGlassEffectState.value
                                }
                                .padding(15.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = translation["liquid_glass_title"],
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 20.sp
                                )
                                Text(
                                    text = translation["liquid_glass_description"],
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Light,
                                    lineHeight = 15.sp
                                )
                            }

                            Switch(
                                checked = useLiquidGlass,
                                onCheckedChange = { newState ->
                                    useLiquidGlassEffectState.value = newState
                                }
                            )
                        }
                    }
                }

                if (showDefaultPageMenu) {
                    LazyColumnBottomSheet(
                        onDismiss = { showDefaultPageMenu = false }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            defaultPageOptions.forEachIndexed { index, option ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = cardShape(defaultPageOptions.size, index)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { defaultPage.value = option.first },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            option.second,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(
                                                    start = 10.dp,
                                                    top = 15.dp,
                                                    bottom = 15.dp
                                            ),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        RadioButton(
                                            selected = getDefaultPage == option.first,
                                            onClick = { defaultPage.value = option.first }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = translation["animation_title"],
                    modifier = Modifier.padding(16.dp),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = cardShapeSingle,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAnimationSpeedMenu = true }
                            .padding(15.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = translation["animation_speed_title"],
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 20.sp
                            )
                            Text(
                                text = translation["animation_speed_description"],
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Light,
                                lineHeight = 15.sp
                            )
                        }
                        Text(
                            text =  animationSpeedLabel,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 17.sp
                        )
                    }
                }
                if (showAnimationSpeedMenu) {
                    LazyColumnBottomSheet(
                        onDismiss = { showAnimationSpeedMenu = false }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            animationSpeedsOptions.forEachIndexed { index, option ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = cardShape(animationSpeedsOptions.size, index)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                animationSpeed.value = option.first
                                                showAnimationSpeedMenu = false
                                            },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            option.second,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(
                                                    start = 10.dp,
                                                    top = 15.dp,
                                                    bottom = 15.dp
                                                ),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        RadioButton(
                                            selected = getAnimationSpeed == option.first,
                                            onClick = { animationSpeed.value = option.first }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}