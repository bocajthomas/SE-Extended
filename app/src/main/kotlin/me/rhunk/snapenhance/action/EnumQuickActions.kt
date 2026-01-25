package me.rhunk.snapenhance.action

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.ui.graphics.vector.ImageVector
import me.rhunk.snapenhance.ui.manager.Routes

enum class EnumQuickActions(
    val key: String,
    val icon: ImageVector,
    val action: Routes.() -> Unit
) {
    FILE_IMPORTS("file_imports", Icons.Rounded.FolderOpen, {
        fileImports.navigate()
    }),
    FRIEND_TRACKER("friend_tracker", Icons.Rounded.PersonSearch, {
        friendTracker.navigate()
    }),
    LOGGER_HISTORY("logger_history", Icons.Rounded.History, {
        loggerHistory.navigate()
    }),
    THEMING("theming", Icons.Rounded.Palette, {
        theming.navigate()
    })
}