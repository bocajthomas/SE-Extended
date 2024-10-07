package me.rhunk.snapenhance.common.config.impl

import me.rhunk.snapenhance.common.config.ConfigContainer
import me.rhunk.snapenhance.common.config.FeatureNotice
import me.rhunk.snapenhance.common.config.RES_OBF_VERSION_CHECK
import me.rhunk.snapenhance.common.data.MessagingRuleType

class UserInterfaceTweaks : ConfigContainer() {
    class BootstrapOverride : ConfigContainer() {
        companion object {
            val tabs = arrayOf("map", "chat", "camera", "discover", "spotlight")
        }

        val appAppearance = unique("app_appearance", "always_light", "always_dark")
        val homeTab = unique("home_tab", *tabs) { addNotices(FeatureNotice.UNSTABLE) }
    }

    inner class FriendFeedMessagePreview : ConfigContainer(hasGlobalState = true) {
        val amount = integer("amount", defaultValue = 1)
    }

    class CustomLabels : ConfigContainer() {
        val customFriendFeedLabel = string("custom_friend_feed_label")
    }

    val friendFeedMenuButtons = multiple(
        "friend_feed_menu_buttons","conversation_info", "mark_snaps_as_seen", "mark_stories_as_seen_locally", *MessagingRuleType.entries.filter { it.showInFriendMenu }.map { it.key }.toTypedArray()
    ).apply {
        set(mutableListOf("conversation_info", MessagingRuleType.STEALTH.key))
    }
    val autoCloseFriendFeedMenu = boolean("auto_close_friend_feed_menu")
    val customTheme = unique("custom_theme",
        "custom",
        "amoled_dark_mode",
        "material_you_light",
        "material_you_dark",
        "light_blue",
        "dark_blue",
        "midnight_slate",
        "earthy_autumn",
        "mint_chocolate",
        "ginger_snap",
        "lemon_meringue",
        "lava_flow",
        "ocean_fog",
        "alien_landscape",
        "watercolor_wash",
        "zesty_lemon",
        "tropical_paradise",
        "industrial_chic",
        "cherry_bomb",
        "woodland_mystery",
        "galaxy_glitter",
        "creamy_vanilla",
        "spicy_chili",
        "spring_meadow",
        "midnight_library",
        "lemon_sorbet",
        "cosmic_night",
        "spicy_mustard",
        "peppermint_candy",
        "gingerbread_house",
        "art_deco_glam",
        "ocean_depths",
        "bubblegum_pink",
        "firefly_night",
        "apple_orchard",
        "lavender_field",
        "lemon_drop",
        "modern_farmhouse",
    ) { addNotices(FeatureNotice.UNSTABLE); requireRestart(); versionCheck = RES_OBF_VERSION_CHECK.copy(isDisabled = true)  }
    val customLabels = container("custom_labels", CustomLabels()) { addNotices(FeatureNotice.UNSTABLE); requireRestart()}
    val friendFeedMessagePreview = container("friend_feed_message_preview", FriendFeedMessagePreview()) { requireRestart() }
    val snapPreview = boolean("snap_preview") { addNotices(FeatureNotice.UNSTABLE); requireRestart() }
    val bootstrapOverride = container("bootstrap_override", BootstrapOverride()) { requireRestart() }
    val mapFriendNameTags = boolean("map_friend_nametags") { requireRestart() }
    val preventMessageListAutoScroll = boolean("prevent_message_list_auto_scroll") { requireRestart(); addNotices(FeatureNotice.UNSTABLE) }
    val streakExpirationInfo = boolean("streak_expiration_info") { requireRestart() }
    val hideFriendFeedEntry = boolean("hide_friend_feed_entry") { requireRestart() }
    val hideStreakRestore = boolean("hide_streak_restore") { requireRestart() }
    val hideQuickAddFriendFeed = boolean("hide_quick_add_friend_feed") { requireRestart() }
    val hideStorySuggestions = multiple("hide_story_suggestions", "hide_friend_suggestions", "hide_suggested_friend_stories", "hide_my_stories") { requireRestart() }
    val hideUiComponents = multiple("hide_ui_components",
        "hide_voice_record_button",
        "hide_stickers_button",
        "hide_live_location_share_button",
        "hide_chat_call_buttons",
        "hide_chat_camera_button",
        "hide_chat_gallery_button",
        "hide_profile_call_buttons",
        "hide_unread_chat_hint",
        "hide_post_to_story_buttons",
        "hide_snap_create_group_buttons",
        "hide_explorer_token_button",
        "hide_gift_snapchat_plus_reminders",
        "hide_map_reactions"
    ) { requireRestart(); versionCheck = RES_OBF_VERSION_CHECK }
    val operaMediaQuickInfo = boolean("opera_media_quick_info") { requireRestart() }
    val oldBitmojiSelfie = unique("old_bitmoji_selfie", "2d", "3d") { requireCleanCache() }
    val disableSpotlight = boolean("disable_spotlight") { requireRestart() }
    val verticalStoryViewer = boolean("vertical_story_viewer") { requireRestart() }
    val messageIndicators = multiple("message_indicators", "encryption_indicator", "platform_indicator", "location_indicator", "ovf_editor_indicator", "director_mode_indicator") { requireRestart() }
    val stealthModeIndicator = boolean("stealth_mode_indicator") { requireRestart() }
    val editTextOverride = multiple("edit_text_override", "multi_line_chat_input", "bypass_text_input_limit") {
        requireRestart(); addNotices(FeatureNotice.BAN_RISK, FeatureNotice.INTERNAL_BEHAVIOR)
    }
}
