package me.rhunk.snapenhance.common.config.impl

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Memory
import me.rhunk.snapenhance.common.config.ConfigContainer
import me.rhunk.snapenhance.common.config.ConfigFlag
import me.rhunk.snapenhance.common.config.FeatureNotice

class Experimental : ConfigContainer() {
    companion object {
        val cofExperimentList = listOf(
            "android_action_menu_v2",
            "android_action_menu_adjust_message_position",
            "chat_emoji_reactions_sending_enabled",
            "chat_text_message_plugin",
        )
    }

    class ComposerHooksConfig: ConfigContainer(hasGlobalState = true) {
        val showFirstCreatedUsername = boolean("show_first_created_username")
        val bypassCameraRollLimit = boolean("bypass_camera_roll_limit")
        val composerConsole = boolean("composer_console")
        val composerLogs = boolean("composer_logs")
    }

    class NativeHooks : ConfigContainer(hasGlobalState = true) {
        val composerHooks = container("composer_hooks", ComposerHooksConfig()) { requireRestart() }
        val disableBitmoji = boolean("disable_bitmoji")
        val customEmojiFont = string("custom_emoji_font") {
            requireRestart()
            addNotices(FeatureNotice.UNSTABLE)
            addFlags(ConfigFlag.USER_IMPORT)
            filenameFilter = { it.endsWith(".ttf") }
        }
    }

    class E2EEConfig : ConfigContainer(hasGlobalState = true) {
        val encryptedMessageIndicator = boolean("encrypted_message_indicator")
        val forceMessageEncryption = boolean("force_message_encryption")
    }

    class AccountSwitcherConfig : ConfigContainer(hasGlobalState = true) {
        val autoBackupCurrentAccount = boolean("auto_backup_current_account", defaultValue = true)
    }

    class AppLockConfig: ConfigContainer(hasGlobalState = true) {
        val lockOnResume = boolean("lock_on_resume", defaultValue = true)
    }

    val nativeHooks = container("native_hooks", NativeHooks()) { icon = Icons.Default.Memory; requireRestart() }
    val spoof = container("spoof", Spoof()) { icon = Icons.Default.Fingerprint ; addNotices(FeatureNotice.BAN_RISK); requireRestart() }
    val convertMessageLocally = boolean("convert_message_locally") { requireRestart() }
    val mediaFilePicker = boolean("media_file_picker") { requireRestart(); addNotices(FeatureNotice.UNSTABLE) }
    val storyLogger = boolean("story_logger") { requireRestart(); addNotices(FeatureNotice.UNSTABLE); }
    val callRecorder = boolean("call_recorder") { requireRestart(); addNotices(FeatureNotice.UNSTABLE); }
    val accountSwitcher = container("account_switcher", AccountSwitcherConfig()) { requireRestart(); addNotices(FeatureNotice.UNSTABLE) }
    val editMessage = boolean("edit_message") { requireRestart() }
    val contextMenuFix = boolean("context_menu_fix") { requireRestart() }
    val cofExperiments = multiple("cof_experiments", *cofExperimentList.toTypedArray()) { requireRestart(); addFlags(ConfigFlag.NO_TRANSLATE); addNotices(FeatureNotice.UNSTABLE) }
    val appLock = container("app_lock", AppLockConfig()) { requireRestart(); addNotices(FeatureNotice.UNSTABLE) }
    val infiniteStoryBoost = boolean("infinite_story_boost")
    val meoPasscodeBypass = boolean("meo_passcode_bypass")
    val noFriendScoreDelay = boolean("no_friend_score_delay") { requireRestart()}
    val bestFriendPinning = boolean("best_friend_pinning") { requireRestart(); addNotices(FeatureNotice.UNSTABLE) }
    val e2eEncryption = container("e2ee", E2EEConfig()) { requireRestart(); nativeHooks() }
    val hiddenSnapchatPlusFeatures = boolean("hidden_snapchat_plus_features") {
        addNotices(FeatureNotice.BAN_RISK, FeatureNotice.UNSTABLE)
        requireRestart()
    }
    val customStreaksExpirationFormat = string("custom_streaks_expiration_format") { requireRestart() }
    val addFriendSourceSpoof = unique("add_friend_source_spoof",
        "added_by_username",
        "added_by_mention",
        "added_by_group_chat",
        "added_by_qr_code",
        "added_by_community",
        "added_by_quick_add",
    ) { addNotices(FeatureNotice.BAN_RISK) }
    val preventForcedLogout = boolean("prevent_forced_logout") { requireRestart(); addNotices(FeatureNotice.BAN_RISK, FeatureNotice.INTERNAL_BEHAVIOR); }
}