package me.rhunk.snapenhance.common.config.impl

import me.rhunk.snapenhance.common.config.ConfigContainer
import me.rhunk.snapenhance.common.config.ConfigFlag
import me.rhunk.snapenhance.common.config.FeatureNotice

class Global : ConfigContainer() {
    companion object {
        val permissionMap = mapOf(
            "android.permission.POST_NOTIFICATIONS" to "notifications",
            "android.permission.READ_MEDIA_IMAGES" to "read_media_images",
            "android.permission.READ_MEDIA_VIDEO" to "read_media_video",
            "android.permission.CAMERA" to "camera",
            "android.permission.ACCESS_FINE_LOCATION" to "location",
            "android.permission.RECORD_AUDIO" to "microphone",
            "android.permission.READ_CONTACTS" to "read_contacts",
            "android.permission.BLUETOOTH_CONNECT" to "nearby_devices",
            "android.permission.READ_PHONE_STATE" to "phone_calls",
        )
    }

    inner class BetterLocationConfig : ConfigContainer(hasGlobalState = true) {
        val spoofLocation = boolean("spoof_location")
        val coordinates = mapCoordinates("coordinates", 0.0 to 0.0) { addFlags(ConfigFlag.SENSITIVE) } // lat, long
        val walkRadius = string("walk_radius") { requireRestart(); inputCheck = { it.toDoubleOrNull()?.isFinite() == true && it.toDouble() >= 0.0 } }
        val alwaysUpdateLocation = boolean("always_update_location") { requireRestart() }
        val suspendLocationUpdates = boolean("suspend_location_updates")
        val spoofBatteryLevel = string("spoof_battery_level") { requireRestart(); inputCheck = { it.isEmpty() || it.toIntOrNull() in 0..100 } }
        val spoofHeadphones = boolean("spoof_headphones") { requireRestart() }
    }

    inner class MediaUploadQualityConfig : ConfigContainer() {
        val forceVideoUploadSourceQuality = boolean("force_video_upload_source_quality") { requireRestart() }
        val disableImageCompression = boolean("disable_image_compression") { requireRestart() }
        val customUploadImageFormat = unique("custom_image_upload_format", "jpeg", "png", "webp") { requireRestart(); addFlags(ConfigFlag.NO_TRANSLATE) }
    }

    val betterLocation = container("better_location", BetterLocationConfig())
    val snapchatPlus = boolean("snapchat_plus") { requireRestart() }
    val mediaUploadQualityConfig = container("media_upload_quality", MediaUploadQualityConfig())
    val disableConfirmationDialogs = multiple("disable_confirmation_dialogs", "erase_message", "remove_friend", "block_friend", "ignore_friend", "hide_friend", "hide_conversation", "clear_conversation") { requireRestart() }
    val disableMetrics = boolean("disable_metrics") { requireRestart() }
    val disableStorySections = multiple("disable_story_sections", "friends", "following", "discover") { requireRestart(); requireCleanCache() }
    val blockAds = boolean("block_ads")
    val disableCustomTabs = boolean("disable_custom_tabs") { requireRestart() }
    val disablePermissionRequests = multiple("disable_permission_requests", *permissionMap.values.toTypedArray()) { requireRestart(); addNotices(FeatureNotice.UNSTABLE) }
    val disableMemoriesSnapFeed = boolean("disable_memories_snap_feed")
    val spotlightCommentsUsername = boolean("spotlight_comments_username") { requireRestart() }
    val bypassVideoLengthRestriction = unique("bypass_video_length_restriction", "split", "single") { addNotices(
        FeatureNotice.BAN_RISK); requireRestart(); nativeHooks() }
    val defaultVideoPlaybackRate = float("default_video_playback_rate", 1.0F) { requireRestart(); inputCheck = { (it.toFloatOrNull() ?: 1.0F) in 0.1F..4.0F} }
    val videoPlaybackRateSlider = boolean("video_playback_rate_slider") { requireRestart() }
    val disableGooglePlayDialogs = boolean("disable_google_play_dialogs") { requireRestart() }
    val defaultVolumeControls = boolean("default_volume_controls") { requireRestart() }
    val hideActiveMusic = boolean("hide_active_music") { requireRestart() }
    val disableSnapSplitting = boolean("disable_snap_splitting") { addNotices(FeatureNotice.INTERNAL_BEHAVIOR) }
}