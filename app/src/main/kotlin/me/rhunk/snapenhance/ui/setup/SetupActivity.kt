@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package me.rhunk.snapenhance.ui.setup

import android.content.Context.MODE_PRIVATE
import android.content.Context
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import me.rhunk.snapenhance.SharedContextHolder
import me.rhunk.snapenhance.common.ui.theme.AppMaterialTheme
import me.rhunk.snapenhance.ui.setup.screens.SetupScreen
import me.rhunk.snapenhance.ui.setup.screens.impl.MappingsScreen
import me.rhunk.snapenhance.ui.setup.screens.impl.PermissionsScreen
import me.rhunk.snapenhance.ui.setup.screens.impl.PickLanguageScreen
import me.rhunk.snapenhance.ui.setup.screens.impl.SaveFolderScreen
import me.rhunk.snapenhance.ui.setup.screens.impl.DisclaimerScreen

class SetupActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                Color.Transparent.toArgb(),
                Color.Transparent.toArgb(),
            ),
            navigationBarStyle = SystemBarStyle.auto(
                Color.Transparent.toArgb(),
                Color.Transparent.toArgb(),
            )
        )
        super.onCreate(savedInstanceState)
        val setupContext = SharedContextHolder.remote(this).apply {
            activity = this@SetupActivity
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            window.isNavigationBarContrastEnforced = false
        }
        fun endActivity() {
            setupContext.reload()
            finish()
        }

        val requirements = intent.getIntExtra("requirements", Requirements.FIRST_RUN)

        fun hasRequirement(requirement: Int) = requirements and requirement == requirement

        val requiredScreens = mutableListOf<SetupScreen>()
        val isFirstRun = hasRequirement(Requirements.FIRST_RUN)

        with(requiredScreens) {
            if (isFirstRun || hasRequirement(Requirements.LANGUAGE)) {
                add(PickLanguageScreen().apply { route = "language" })
            }
            if (isFirstRun || hasRequirement(Requirements.GRANT_PERMISSIONS)) {
                add(PermissionsScreen().apply { route = "permissions" })
            }
            if (isFirstRun || hasRequirement(Requirements.SAVE_FOLDER)) {
                add(SaveFolderScreen().apply { route = "saveFolder" })
            }
            if (isFirstRun || hasRequirement(Requirements.MAPPINGS)) {
                add(MappingsScreen().apply { route = "mappings" })
            }
            if (isFirstRun || hasRequirement(Requirements.DISCLAIMER)) {
                add(DisclaimerScreen().apply { route = "disclaimer" })
            }
        }

        // If there are no required screens, we can just finish the activity
        if (requiredScreens.isEmpty()) {
            endActivity()
            return
        }

        requiredScreens.forEach { screen ->
            screen.context = setupContext
            screen.init()
        }

        // TODO: fix some screens next button being disabled.
        setContent {
            val navController = rememberNavController()
            var canGoNext by remember { mutableStateOf(false) }
            val currentRoute = navController.currentBackStackEntry?.destination?.route ?: requiredScreens.first().route
            val currentScreen = remember(currentRoute) { requiredScreens.first { it.route == currentRoute } }
            val isLastScreen = remember(currentRoute) { requiredScreens.indexOfFirst { it.route == currentRoute } == requiredScreens.lastIndex }

            fun nextScreen() {
                if (!canGoNext) return
                val currentRouteToPop = requiredScreens.firstOrNull()?.route ?: run {
                    endActivity()
                    return
                }
                requiredScreens.firstOrNull()?.onLeave()

                if (!isLastScreen) {
                    canGoNext = false
                    requiredScreens.removeAt(0)
                    navController.navigate(requiredScreens.first().route) {
                        popUpTo(currentRouteToPop) {
                            inclusive = true
                        }
                    }
                } else {
                    endActivity()
                }
            }

            currentScreen.isLastScreen = isLastScreen
            currentScreen.allowNext = { canGoNext = it }
            currentScreen.goNext = { nextScreen() }

            val defaultBottomBar: @Composable RowScope.() -> Unit = {
                Button(
                    onClick = { nextScreen() },
                    enabled = canGoNext,
                    shapes = ButtonDefaults.shapes()
                ) {
                    val buttonText = if (isLastScreen) "Done" else "Next"
                    Text(buttonText)
                }
            }

            AppMaterialTheme(
                preferences = setupContext.androidContext.getSharedPreferences("selected_theme", 0)
            ) {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        if (currentScreen.shouldDisableBottomBar) return@Scaffold
                        val isTransparent = currentScreen.makeBottomBarTransparent
                        BottomAppBar(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                            containerColor = if (isTransparent) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(end = 10.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                currentScreen.bottomBarButtons?.invoke(this) ?: defaultBottomBar(this)
                            }
                        }
                    },
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = requiredScreens.firstOrNull()?.route ?: "exit"
                    ) {
                        composable("exit") {
                            LaunchedEffect(Unit) {
                                finish()
                            }
                        }
                        requiredScreens.forEach { screen ->
                            screen.allowNext = { canGoNext = it }
                            screen.goNext = {
                                    canGoNext = true
                                    nextScreen()
                            }
                            composable(screen.route) {
                                BackHandler(true) {}
                                screen.Content()
                            }
                        }
                    }
                }
            }
        }
    }
}