@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package me.rhunk.snapenhance.ui.setup.screens.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.rhunk.snapenhance.common.ui.columnPadding
import me.rhunk.snapenhance.ui.setup.screens.SetupScreen
import me.rhunk.snapenhance.ui.util.ActivityLauncherHelper
import me.rhunk.snapenhance.ui.util.chooseFolder

class SaveFolderScreen : SetupScreen() {
    private lateinit var activityLauncherHelper: ActivityLauncherHelper

    override fun init() {
        activityLauncherHelper = ActivityLauncherHelper(context.activity!!)
    }

    @Composable
    override fun Content() {
        Column(
            modifier = Modifier.fillMaxSize().columnPadding(staticVertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            DialogText(text = context.translation["setup.save_folder.description"])
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    activityLauncherHelper.chooseFolder {
                        if (it.isBlank()) return@chooseFolder
                        context.config.root.downloader.saveFolder.set(it)
                        context.config.writeConfig()
                        goNext()
                    }
                },
                shapes = ButtonDefaults.shapes()
            ) {
                Text(text = context.translation["setup.save_folder.select_folder_button"])
            }
        }
    }
}