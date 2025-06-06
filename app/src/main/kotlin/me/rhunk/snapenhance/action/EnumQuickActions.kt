package me.rhunk.snapenhance.action

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.ui.graphics.vector.ImageVector
import me.rhunk.snapenhance.ui.manager.Routes

enum class EnumQuickActions(
    val key: String,
    val icon: ImageVector,
    val action: Routes.() -> Unit
) {
    FILE_IMPORTS("file_imports", Icons.Default.FolderOpen, {
        fileImports.navigateReset()
    }),
    FRIEND_TRACKER("friend_tracker", Icons.Default.PersonSearch, {
        friendTracker.navigateReset()
    }),
    LOGGER_HISTORY("logger_history", Icons.Default.History, {
        loggerHistory.navigateReset()
    }),
}