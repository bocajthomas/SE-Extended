package me.rhunk.snapenhance.common.config.impl

import me.rhunk.snapenhance.common.config.ConfigContainer
import me.rhunk.snapenhance.common.config.FeatureNotice
import me.rhunk.snapenhance.common.config.PropertyValue
import me.rhunk.snapenhance.common.data.NotificationType
import me.rhunk.snapenhance.common.util.PURGE_DISABLED_KEY
import me.rhunk.snapenhance.common.util.PURGE_TRANSLATION_KEY
import me.rhunk.snapenhance.common.util.PURGE_VALUES

class MessagingTweaks : ConfigContainer() {
    inner class HalfSwipeNotifierConfig : ConfigContainer(hasGlobalState = true) {
        val minDuration: PropertyValue<Int> = integer("min_duration", defaultValue = 0) {
            inputCheck = { it.toIntOrNull()?.coerceAtLeast(0) != null && maxDuration.get() >= it.toInt() }
        }
        val maxDuration: PropertyValue<Int> = integer("max_duration", defaultValue = 20) {
            inputCheck = { it.toIntOrNull()?.coerceAtLeast(0) != null && minDuration.get() <= it.toInt() }
        }
    }

    inner class MessageLoggerConfig : ConfigContainer(hasGlobalState = true) {
        val keepMyOwnMessages = boolean("keep_my_own_messages")
        val autoPurge = unique("auto_purge", *PURGE_VALUES) {
            customOptionTranslationPath = PURGE_TRANSLATION_KEY
            disabledKey = PURGE_DISABLED_KEY
        }.apply { set("3_days") }

        val messageFilter = multiple("message_filter", "CHAT",
            "SNAP",
            "NOTE",
            "EXTERNAL_MEDIA",
            "STICKER"
        ) {
            customOptionTranslationPath = "content_type"
        }
    }

    class BetterNotifications: ConfigContainer() {
        val groupNotifications = boolean("group_notifications")
        val chatPreview = boolean("chat_preview")
        val mediaPreview = multiple("media_preview", "SNAP", "EXTERNAL_MEDIA", "STICKER", "SHARE", "TINY_SNAP", "MAP_REACTION") {
            customOptionTranslationPath = "content_type"
        }
        val mediaCaption = boolean("media_caption")
        val stackedMediaMessages = boolean("stacked_media_messages")
        val friendAddSource = boolean("friend_add_source")
        val replyButton = boolean("reply_button") { addNotices(FeatureNotice.UNSTABLE) }
        val smartReplies = boolean("smart_replies")
        val downloadButton = boolean("download_button")
        val markAsReadButton = boolean("mark_as_read_button") { addNotices(FeatureNotice.UNSTABLE) }
        val markAsReadAndSaveInChat = boolean("mark_as_read_and_save_in_chat") { addNotices(FeatureNotice.UNSTABLE) }
    }

    val bypassScreenshotDetection = boolean("bypass_screenshot_detection") { requireRestart() }
    val anonymousStoryViewing = boolean("anonymous_story_viewing")
    val preventStoryRewatchIndicator = boolean("prevent_story_rewatch_indicator") { requireRestart() }
    val hidePeekAPeek = boolean("hide_peek_a_peek")
    val hideBitmojiPresence = boolean("hide_bitmoji_presence")
    val hideTypingNotifications = boolean("hide_typing_notifications")
    val unlimitedSnapViewTime = boolean("unlimited_snap_view_time")
    val autoMarkAsRead = multiple("auto_mark_as_read", "snap_reply", "conversation_read") { requireRestart() }
    val loopMediaPlayback = boolean("loop_media_playback") { requireRestart() }
    val disableReplayInFF = boolean("disable_replay_in_ff")
    val halfSwipeNotifier = container("half_swipe_notifier", HalfSwipeNotifierConfig()) { requireRestart()}
    val messagePreviewLength = integer("message_preview_length", defaultValue = 20)
    val callStartConfirmation = boolean("call_start_confirmation") { requireRestart() }
    val unlimitedConversationPinning = boolean("unlimited_conversation_pinning") { requireRestart() }
    val autoSaveMessagesInConversations = multiple("auto_save_messages_in_conversations",
        "CHAT",
        "SNAP",
        "NOTE",
        "EXTERNAL_MEDIA",
        "STICKER"
    ) { requireRestart(); customOptionTranslationPath = "content_type" }
    val preventMessageSending = multiple("prevent_message_sending", *NotificationType.getOutgoingValues().map { it.key }.toTypedArray()) {
        customOptionTranslationPath = "features.options.notifications"
        nativeHooks()
    }
    val friendMutationNotifier = multiple("friend_mutation_notifier",
        "remove_friend",
        "birthday_changes",
        "bitmoji_selfie_changes",
        "bitmoji_avatar_changes",
        "bitmoji_background_changes",
        "bitmoji_scene_changes",
    ) { requireRestart() }
    val betterNotifications = container("better_notifications", BetterNotifications()) { requireRestart() }
    val notificationBlacklist = multiple("notification_blacklist", *NotificationType.getIncomingValues().map { it.key }.toTypedArray()) {
        customOptionTranslationPath = "features.options.notifications"
    }
    val messageLogger = container("message_logger", MessageLoggerConfig()) { addNotices(FeatureNotice.UNSTABLE); requireRestart() }
    val galleryMediaSendOverride = unique("gallery_media_send_override", "always_ask", "SNAP", "NOTE", "SAVABLE_SNAP") { requireRestart(); nativeHooks() }
    val stripMediaMetadata = multiple("strip_media_metadata", "hide_caption_text", "hide_snap_filters", "hide_extras", "remove_audio_note_duration", "remove_audio_note_transcript_capability") { requireRestart() }
    val bypassMessageRetentionPolicy = boolean("bypass_message_retention_policy") { addNotices(FeatureNotice.UNSTABLE); requireRestart() }
    val bypassMessageActionRestrictions = boolean("bypass_message_action_restrictions") { requireRestart() }
    val removeGroupsLockedStatus = boolean("remove_groups_locked_status") { requireRestart() }
}