package me.rhunk.snapenhance.common.database.impl

import android.annotation.SuppressLint
import android.database.Cursor
import me.rhunk.snapenhance.common.database.DatabaseObject
import me.rhunk.snapenhance.common.util.ktx.getBlobOrNull
import me.rhunk.snapenhance.common.util.ktx.getIntOrNull
import me.rhunk.snapenhance.common.util.ktx.getLongOrNull
import me.rhunk.snapenhance.common.util.ktx.getStringOrNull
import java.nio.ByteBuffer
import java.util.UUID

data class FriendFeedEntry(
    var feedDisplayName: String? = null,
    var participantsSize: Int = 0,
    var lastInteractionTimestamp: Long = 0,
    var displayTimestamp: Long = 0,
    var displayInteractionType: String? = null,
    var lastInteractionUserId: Int? = null,
    var key: String? = null,
    var friendUserId: String? = null,
    var participants: List<String>? = null,
    var conversationType: Int? = null,
    var friendDisplayName: String? = null,
    var friendDisplayUsername: String? = null,
    var friendLinkType: Int? = null,
    var bitmojiAvatarId: String? = null,
    var bitmojiSelfieId: String? = null,
) : DatabaseObject {
    @SuppressLint("Range")
    override fun write(cursor: Cursor) {
        with(cursor) {
            key = getStringOrNull("client_conversation_id") ?: getStringOrNull("key")
            feedDisplayName = (getStringOrNull("conversation_title") ?: getStringOrNull("feedDisplayName"))?.takeIf { it.isNotBlank() }
            lastInteractionTimestamp = getLongOrNull("last_updated_timestamp") ?: getLongOrNull("lastInteractionTimestamp") ?:  0L

            participants = getBlobOrNull("participants")?.toList()?.chunked(16)?.map { ByteBuffer.wrap(it.toByteArray()).run { UUID(long, long) }.toString() } ?: emptyList()
            participantsSize = getIntOrNull("participantsSize") ?: participants?.size ?: 0
            conversationType = getIntOrNull("conversation_type") ?: getIntOrNull("kind")

            displayTimestamp = getLongOrNull("displayTimestamp") ?: 0L
            displayInteractionType = getStringOrNull("displayInteractionType")
            lastInteractionUserId = getIntOrNull("lastInteractionUserId")
            friendUserId = getStringOrNull("friendUserId")
            friendDisplayName = getStringOrNull("friendDisplayName")
            friendDisplayUsername = getStringOrNull("friendDisplayUsername")
            friendLinkType = getIntOrNull("friendLinkType")
            bitmojiAvatarId = getStringOrNull("bitmojiAvatarId")
            bitmojiSelfieId = getStringOrNull("bitmojiSelfieId")
        }
    }
}
