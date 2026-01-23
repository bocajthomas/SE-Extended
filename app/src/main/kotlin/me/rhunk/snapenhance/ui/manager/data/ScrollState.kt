package me.rhunk.snapenhance.ui.manager.data

import androidx.lifecycle.ViewModel

// TODO: Make each screen save its scroll state, this for blur settings
data class ScrollState(
    val index: Int = 0,
    val offset: Int = 0
)

class AppearanceRootViewModel : ViewModel() {
    var savedScrollState: ScrollState = ScrollState(0, 0)
}

class AppThemeRootViewModel : ViewModel() {
    var savedScrollState: ScrollState = ScrollState(0, 0)
}

class FeaturesRootViewModel : ViewModel() {
    var savedScrollState: ScrollState = ScrollState(0, 0)
}

class ManageRuleRootViewModel : ViewModel() {
    var savedScrollState: ScrollState = ScrollState(0, 0)
}

class HomeLogsRootViewModel : ViewModel() {
    var savedScrollState: ScrollState = ScrollState(0, 0)
}

class HomeRootViewModel : ViewModel() {
    var savedScrollState: ScrollState = ScrollState(0, 0)
}

class HomeSettingsRootViewModel : ViewModel() {
    var savedScrollState: ScrollState = ScrollState(0, 0)
}

class BetterLocationRootViewModel : ViewModel() {
    var savedScrollState: ScrollState = ScrollState(0, 0)
}

class ScriptingRootViewModel : ViewModel() {
    var savedScrollState: ScrollState = ScrollState(0, 0)
}

class LoggedStoriesRootViewModel : ViewModel() {
    var savedScrollState: ScrollState = ScrollState(0, 0)
}

class ManageScopeRootViewModel : ViewModel() {
    var savedScrollState: ScrollState = ScrollState(0, 0)
}

class MessagePreviewRootViewModel : ViewModel() {
    var savedScrollState: ScrollState = ScrollState(0, 0)
}

class SocialRootViewModel : ViewModel() {
    var savedScrollState: ScrollState = ScrollState(0, 0)
}

class EditThemeRootViewModel : ViewModel() {
    var savedScrollState: ScrollState = ScrollState(0, 0)
}

class ThemingRootViewModel : ViewModel() {
    var savedScrollState: ScrollState = ScrollState(0, 0)
}

class EditRuleRootViewModel : ViewModel() {
    var savedScrollState: ScrollState = ScrollState(0, 0)
}

class FriendTrackerRootViewModel : ViewModel() {
    var savedScrollState: ScrollState = ScrollState(0, 0)
}

class FriendTrackerLogsRootViewModel : ViewModel() {
    var savedScrollState: ScrollState = ScrollState(0, 0)
}

class FileImportRootViewModel : ViewModel() {
    var savedScrollState: ScrollState = ScrollState(0, 0)
}

class LoggerHistoryRootViewModel : ViewModel() {
    var savedScrollState: ScrollState = ScrollState(0, 0)
}

class ManageReposRootViewModel : ViewModel() {
    var savedScrollState: ScrollState = ScrollState(0, 0)
}

class TasksRootViewModel : ViewModel() {
    var savedScrollState: ScrollState = ScrollState(0, 0)
}