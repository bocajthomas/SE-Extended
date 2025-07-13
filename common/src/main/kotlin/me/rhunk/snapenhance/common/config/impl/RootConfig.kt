package me.rhunk.snapenhance.common.config.impl

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Rule
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import me.rhunk.snapenhance.common.config.ConfigContainer
import me.rhunk.snapenhance.common.config.FeatureNotice

class RootConfig : ConfigContainer() {
    val downloader = container("downloader", DownloaderConfig()) { icon = Icons.Rounded.Download }
    val userInterface = container("user_interface", UserInterfaceTweaks()) { icon = Icons.Rounded.RemoveRedEye }
    val messaging = container("messaging", MessagingTweaks()) { icon = Icons.AutoMirrored.Rounded.Send }
    val global = container("global", Global()) { icon = Icons.Rounded.MiscellaneousServices }
    val rules = container("rules", Rules()) { icon = Icons.AutoMirrored.Rounded.Rule }
    val camera = container("camera", Camera()) { icon = Icons.Rounded.Camera; requireRestart() }
    val streaksReminder = container("streaks_reminder", StreaksReminderConfig()) { icon = Icons.Rounded.Alarm }
    val experimental = container("experimental", Experimental()) { icon = Icons.Rounded.Science; addNotices(FeatureNotice.UNSTABLE) }
    val scripting = container("scripting", Scripting()) { icon = Icons.Rounded.DataObject }
    val friendTracker = container("friend_tracker", FriendTrackerConfig()) { icon = Icons.Rounded.PersonSearch }
}