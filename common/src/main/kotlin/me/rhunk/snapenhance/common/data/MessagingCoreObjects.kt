package me.rhunk.snapenhance.common.data

import android.database.Cursor
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import me.rhunk.snapenhance.common.config.FeatureNotice
import me.rhunk.snapenhance.common.data.download.toKeyPair
import me.rhunk.snapenhance.common.util.ktx.getIntOrNull
import me.rhunk.snapenhance.common.util.ktx.getInteger
import me.rhunk.snapenhance.common.util.ktx.getLongOrNull
import me.rhunk.snapenhance.common.util.ktx.getStringOrNull
import kotlin.time.Duration.Companion.hours


enum class RuleState(
    val key: String
) {
    BLACKLIST("blacklist"),
    WHITELIST("whitelist");

    companion object {
        fun getByName(name: String) = entries.first { it.key == name }
    }
}

enum class SocialScope(
    val key: String,
    val tabRoute: String,
) {
    FRIEND("friend", "friend_info/{id}"),
    GROUP("group", "group_info/{id}");

    companion object {
        fun getByName(name: String) = entries.first { it.key == name }
    }
}

enum class MessagingRuleType(
    val key: String,
    val listMode: Boolean,
    val showInFriendMenu: Boolean = true,
    val defaultValue: String? = "whitelist",
    val configNotices: Array<FeatureNotice> = emptyArray()
) {
    STEALTH("stealth", true),
    AUTO_DOWNLOAD("auto_download", true),
    AUTO_SAVE("auto_save", true, defaultValue = "blacklist"),
    AUTO_OPEN_SNAPS("auto_open_snaps", true, configNotices = arrayOf(FeatureNotice.BAN_RISK, FeatureNotice.UNSTABLE), defaultValue = null),
    UNSAVEABLE_MESSAGES("unsaveable_messages", true, configNotices = arrayOf(FeatureNotice.REQUIRE_NATIVE_HOOKS), defaultValue = null),
    HIDE_FRIEND_FEED("hide_friend_feed", false, showInFriendMenu = false),
    E2E_ENCRYPTION("e2e_encryption", false),
    PIN_CONVERSATION("pin_conversation", false, showInFriendMenu = false);

    fun translateOptionKey(optionKey: String): String {
        return if (listMode) "rules.properties.$key.options.$optionKey" else "rules.properties.$key.name"
    }

    companion object {
        fun getByName(name: String) = entries.firstOrNull { it.key == name }
    }
}

@Parcelize
data class FriendStreaks(
    val notify: Boolean = true,
    val expirationTimestamp: Long,
    val length: Int
): Parcelable {
    fun hoursLeft() = (expirationTimestamp - System.currentTimeMillis()) / 1000 / 60 / 60

    fun isAboutToExpire(expireHours: Int) = (expirationTimestamp - System.currentTimeMillis()).let {
        it > 0 && it < expireHours.hours.inWholeMilliseconds
    }
}

@Parcelize
data class MessagingGroupInfo(
    val conversationId: String,
    val name: String,
    val participantsCount: Int
): Parcelable {
    companion object {
        fun fromCursor(cursor: Cursor): MessagingGroupInfo {
            return MessagingGroupInfo(
                conversationId = cursor.getStringOrNull("conversationId")!!,
                name = cursor.getStringOrNull("name")!!,
                participantsCount = cursor.getInteger("participantsCount")
            )
        }
    }
}

@Parcelize
data class MessagingFriendInfo(
    val userId: String,
    val dmConversationId: String?,
    val displayName: String?,
    val mutableUsername: String,
    val bitmojiId: String?,
    val selfieId: String?,
    var streaks: FriendStreaks?,
): Parcelable {
    companion object {
        fun fromCursor(cursor: Cursor): MessagingFriendInfo {
            return MessagingFriendInfo(
                userId = cursor.getStringOrNull("userId")!!,
                dmConversationId = cursor.getStringOrNull("dmConversationId"),
                displayName = cursor.getStringOrNull("displayName"),
                mutableUsername = cursor.getStringOrNull("mutableUsername")!!,
                bitmojiId = cursor.getStringOrNull("bitmojiId"),
                selfieId = cursor.getStringOrNull("selfieId"),
                streaks = cursor.getLongOrNull("expirationTimestamp")?.let {
                    FriendStreaks(
                        notify = cursor.getIntOrNull("notify") == 1,
                        expirationTimestamp = it,
                        length = cursor.getIntOrNull("length") ?: 0
                    )
                }
            )
        }
    }
}

class StoryData(
    val url: String,
    val postedAt: Long,
    val createdAt: Long,
    val key: ByteArray?,
    val iv: ByteArray?
) {
    fun getEncryptionKeyPair() = key?.let { (it to (iv ?: return@let null)) }?.toKeyPair()
}