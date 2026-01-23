@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package me.rhunk.snapenhance.ui.manager

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.core.view.WindowCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dev.chrisbanes.haze.rememberHazeState
import me.rhunk.snapenhance.RemoteSideContext
import me.rhunk.snapenhance.SharedContextHolder
import me.rhunk.snapenhance.UpdateNotification.Companion.updateNotiRepeater
import me.rhunk.snapenhance.common.ui.theme.AppMaterialTheme
import me.rhunk.snapenhance.common.ui.rememberMutableStringPreferenceState

class MainActivity : ComponentActivity() {
    private lateinit var navController: NavHostController
    private lateinit var managerContext: RemoteSideContext

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (::navController.isInitialized.not()) return
        intent.getStringExtra("route")?.let { route ->
            navController.popBackStack()
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id){
                    inclusive = true
                }
            }
        }
    }

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
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        managerContext = SharedContextHolder.remote(this).apply {
            activity = this@MainActivity
            checkForRequirements()
        }

        val shouldCheckForUpdates = managerContext.sharedPreferences.getBoolean("app_update_checker", true)
        if (shouldCheckForUpdates) {
            updateNotiRepeater(this, force = false)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        val routes = Routes(managerContext)
        routes.getRoutes().forEach { it.init() }

        setContent {
            navController = rememberNavController()
            val navigation = remember {
                Navigation(managerContext, navController,  routes.also {
                    it.navController = navController
                })
            }

            val defaultPage by managerContext.sharedPreferences.rememberMutableStringPreferenceState(
                key = "appearance_default_page",
                defaultValue = "home"
            )
            val theme = getSharedPreferences("selected_theme", MODE_PRIVATE)
            val startDestination = remember { intent.getStringExtra("route") ?: defaultPage }
            val hazeState = rememberHazeState()

            AppMaterialTheme(preferences = theme) {
                val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = { navigation.TopBar(hazeState, scrollBehavior) },
                    bottomBar = { navigation.BottomBar(hazeState) },
                    floatingActionButton = { navigation.FloatingActionButton() }
                ) { innerPadding -> navigation.Content(innerPadding, startDestination, hazeState) }
            }
        }
    }
}
