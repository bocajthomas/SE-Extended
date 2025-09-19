package me.rhunk.snapenhance.core.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.OpenParams
import android.database.sqlite.SQLiteDatabaseCorruptException
import me.rhunk.snapenhance.common.database.DatabaseObject
import me.rhunk.snapenhance.common.database.impl.*
import me.rhunk.snapenhance.common.util.ktx.getBlobOrNull
import me.rhunk.snapenhance.common.util.ktx.getIntOrNull
import me.rhunk.snapenhance.common.util.ktx.getInteger
import me.rhunk.snapenhance.common.util.ktx.getStringOrNull
import me.rhunk.snapenhance.common.util.protobuf.ProtoReader
import me.rhunk.snapenhance.core.ModContext
import me.rhunk.snapenhance.core.wrapper.impl.toSnapUUID
import me.rhunk.snapenhance.nativelib.NativeLib


enum class DatabaseType(
    val fileName: String
) {
    MAIN("main.db"),
    CORE("core.db"),
    ARROYO("arroyo.db"),
    SIMPLE_DB_HELPER("simple_db_helper.db")
}

class DatabaseAccess(
    private val context: ModContext
) {
    private val openedDatabases = mutableMapOf<DatabaseType, SQLiteDatabase>()

    private val hasArroyoConversationTable by lazy {
        useDatabase(DatabaseType.ARROYO)?.performOperation {
            safeRawQuery("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'conversation'")?.use { query ->
                query.moveToFirst() && query.getStringOrNull("name") == "conversation"
            }
        } == true
    }

    private fun useDatabase(database: DatabaseType, writeMode: Boolean = false): SQLiteDatabase? {
        // only cache read-only databases
        if (!writeMode && openedDatabases.containsKey(database) && openedDatabases[database]?.isOpen == true) {
            return openedDatabases[database]
        }

        val dbPath = context.androidContext.getDatabasePath(database.fileName)
        if (!dbPath.exists()) return null
        return runCatching {
            SQLiteDatabase.openDatabase(
                dbPath,
                OpenParams.Builder()
                    .setOpenFlags(
                        if (writeMode) SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING
                        else SQLiteDatabase.OPEN_READONLY
                    )
                    .setErrorHandler {
                        context.androidContext.deleteDatabase(dbPath.absolutePath)
                        context.softRestartApp()
                    }.build()
            )
        }.onFailure {
            context.log.error("Failed to open database ${database.fileName}!", it)
        }.getOrNull()?.also {
            if (!writeMode) openedDatabases[database] = it
        }
    }

    private fun <T> SQLiteDatabase.performOperation(query: SQLiteDatabase.() -> T?): T? {
        return runCatching {
            if (NativeLib.initialized && openedDatabases[DatabaseType.ARROYO] == this) {
                var result: T? = null
                context.native.lockNativeDatabase(DatabaseType.ARROYO.fileName) {
                    result = query()
                }
                result
            } else synchronized(this) {
                query()
            }
        }.onFailure {
            context.log.error("Database operation failed", it)
        }.getOrNull()
    }

    private fun SQLiteDatabase.safeRawQuery(query: String, args: Array<String>? = null): Cursor? {
        return runCatching {
            rawQuery(query, args)
        }.onFailure {
            if (it !is SQLiteDatabaseCorruptException) {
                context.log.error("Failed to execute query $query", it)
                return@onFailure
            }
            context.longToast("Database ${this.path} is corrupted! Restarting ...")
            context.androidContext.deleteDatabase(this.path)
            context.crash("Database ${this.path} is corrupted!", it)
        }.getOrNull()
    }

    private val friendDMsCache by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        getFeedEntries(Int.MAX_VALUE)
            .filter { it.conversationType == 0 && it.participantsSize == 2 }
            .associate { it.participants?.firstOrNull { it != myUserId } to it.key }
            .toMutableMap()
    }

    private val dmOtherParticipantCache by lazy {
        if (hasArroyoConversationTable) {
            return@lazy useDatabase(DatabaseType.ARROYO)?.performOperation {
                safeRawQuery(
                    "SELECT client_conversation_id, conversation_metadata FROM conversation",
                )?.use { query ->
                    val result = mutableMapOf<String, String?>()
                    if (!query.moveToFirst()) {
                        return@performOperation null
                    }
                    do {
                        val conversationId = query.getStringOrNull("client_conversation_id") ?: continue
                        val conversationMetadata = ProtoReader(query.getBlobOrNull("conversation_metadata") ?: continue)

                        val participants = mutableListOf<String>()
                        conversationMetadata.eachBuffer(3) {
                            participants.add(getByteArray(1, 1)?.toSnapUUID()?.toString() ?: return@eachBuffer)
                        }

                        result[conversationId] = if (participants.size == 2) {
                            participants.firstOrNull { it != myUserId }?.also {
                                result[it] = null
                            }
                        } else null
                    } while (query.moveToNext())

                    result
                }
            }?.toMutableMap() ?: mutableMapOf()
        }

        (useDatabase(DatabaseType.ARROYO)?.performOperation {
            safeRawQuery(
                "SELECT client_conversation_id, conversation_type, user_id FROM user_conversation WHERE user_id != ?",
                arrayOf(myUserId)
            )?.use { query ->
                val participants = mutableMapOf<String, String?>()
                if (!query.moveToFirst()) {
                    return@performOperation null
                }
                do {
                    val conversationId = query.getStringOrNull("client_conversation_id") ?: continue
                    val userId = query.getStringOrNull("user_id") ?: continue
                    participants[conversationId] = when (query.getIntOrNull("conversation_type")) {
                        0 -> userId
                        else -> null
                    }
                    participants[userId] = null
                } while (query.moveToNext())
                participants
            }
        } ?: emptyMap()).toMutableMap()
    }

    fun hasMain(): Boolean = useDatabase(DatabaseType.MAIN)?.isOpen == true
    fun hasArroyo(): Boolean = useDatabase(DatabaseType.ARROYO)?.isOpen == true

    fun init() {
        // perform integrity check on databases
        DatabaseType.entries.forEach { type ->
            useDatabase(type, writeMode = true)?.apply {
                rawQuery("PRAGMA integrity_check", null).use { query ->
                    if (!query.moveToFirst() || query.getString(0).lowercase() != "ok") {
                        context.log.error("Failed to perform integrity check on ${type.fileName}")
                        context.androidContext.deleteDatabase(type.fileName)
                    }
                }
            }?.close()
        }
    }

    fun finalize() {
        openedDatabases.values.forEach { it.close() }
    }

    private fun <T : DatabaseObject> SQLiteDatabase.readDatabaseObject(
        obj: T,
        table: String,
        where: String,
        args: Array<String>
    ): T? = this.safeRawQuery("SELECT * FROM $table WHERE $where", args)?.use {
        if (!it.moveToFirst()) {
            return null
        }
        try {
            obj.write(it)
        } catch (e: Throwable) {
            context.log.error("Failed to read database object", e)
        }
        obj
    }

    val myUserId by lazy {
        context.androidContext.getSharedPreferences("user_session_shared_pref", 0).getString("key_user_id", null) ?:
        useDatabase(DatabaseType.ARROYO)?.performOperation {
            safeRawQuery(buildString {
                append("SELECT value FROM required_values WHERE key = 'USERID'")
            }, null)?.use { query ->
                if (!query.moveToFirst()) {
                    return@performOperation null
                }
                query.getStringOrNull("value")!!
            }
        }!!
    }

    fun getFeedEntryByConversationId(conversationId: String): FriendFeedEntry? {
        return useDatabase(DatabaseType.ARROYO)?.performOperation {
            readDatabaseObject(
                FriendFeedEntry(),
                "feed_entry",
                "client_conversation_id = ?",
                arrayOf(conversationId)
            )
        } ?: useDatabase(DatabaseType.MAIN)?.performOperation {
            readDatabaseObject(
                FriendFeedEntry(),
                "FriendsFeedView",
                "key = ?",
                arrayOf(conversationId)
            )
        }
    }

    fun getFriendInfo(userId: String): FriendInfo? {
        return useDatabase(DatabaseType.MAIN)?.performOperation {
            readDatabaseObject(
                FriendInfo(),
                "FriendWithUsername",
                "userId = ?",
                arrayOf(userId)
            )
        }
    }

    fun getFriendOriginalUsername(mutableUsername: String): String? {
        return useDatabase(DatabaseType.MAIN)?.performOperation {
            safeRawQuery(
                "SELECT originalUsername FROM CombinedUsername WHERE mutableUsername = ?",
                arrayOf(mutableUsername)
            )?.use { query ->
                if (!query.moveToFirst()) {
                    return@performOperation null
                }
                query.getStringOrNull("originalUsername")
            }
        }
    }

    fun getFriendInfoByUsername(username: String): FriendInfo? {
        return useDatabase(DatabaseType.MAIN)?.performOperation {
            readDatabaseObject(
                FriendInfo(),
                "FriendWithUsername",
                "usernameForSorting = ?",
                arrayOf(username)
            )
        }
    }

    fun getAllFriends(): List<FriendInfo> {
        return useDatabase(DatabaseType.MAIN)?.performOperation {
            safeRawQuery(
                "SELECT * FROM FriendWithUsername",
                null
            )?.use { query ->
                val list = mutableListOf<FriendInfo>()
                while (query.moveToNext()) {
                    val friendInfo = FriendInfo()
                    try {
                        friendInfo.write(query)
                    } catch (_: Throwable) {}
                    list.add(friendInfo)
                }
                list
            }
        } ?: emptyList()
    }

    fun getFeedEntries(limit: Int, whereClause: String? = null): List<FriendFeedEntry> {
        val entries = mutableListOf<FriendFeedEntry>()
        return useDatabase(DatabaseType.ARROYO)?.performOperation {
            safeRawQuery(
                "SELECT * FROM feed_entry ${whereClause?.let { "WHERE $it" }.orEmpty()} ORDER BY last_updated_timestamp DESC LIMIT ?",
                arrayOf(limit.toString())
            )?.use { query ->
                while (query.moveToNext()) {
                    val friendFeedEntry = FriendFeedEntry()
                    try {
                        friendFeedEntry.write(query)
                    } catch (_: Throwable) {}
                    entries.add(friendFeedEntry)
                }
                entries
            }
        } ?: useDatabase(DatabaseType.MAIN)?.performOperation {
            safeRawQuery(
                "SELECT * FROM FriendsFeedView ORDER BY _id LIMIT ?",
                arrayOf(limit.toString())
            )?.use { query ->
                while (query.moveToNext()) {
                    val friendFeedEntry = FriendFeedEntry()
                    try {
                        friendFeedEntry.write(query)
                    } catch (_: Throwable) {}
                    entries.add(friendFeedEntry)
                }
                entries
            }
        } ?: emptyList()
    }

    fun getConversationMessageFromId(clientMessageId: Long): ConversationMessage? {
        return useDatabase(DatabaseType.ARROYO)?.performOperation {
            readDatabaseObject(
                ConversationMessage(),
                "conversation_message",
                "client_message_id = ?",
                arrayOf(clientMessageId.toString())
            )
        }
    }

    fun getConversationServerMessage(conversationId: String, serverId: Long): ConversationMessage? {
        return useDatabase(DatabaseType.ARROYO)?.performOperation {
            readDatabaseObject(
                ConversationMessage(),
                "conversation_message",
                "client_conversation_id = ? AND server_message_id = ?",
                arrayOf(conversationId, serverId.toString())
            )
        }
    }

    fun getConversationType(conversationId: String): Int? {
        if (hasArroyoConversationTable) {
            return getFeedEntryByConversationId(conversationId)?.conversationType
        }

        return useDatabase(DatabaseType.ARROYO)?.performOperation {
            safeRawQuery(
                "SELECT conversation_type FROM user_conversation WHERE client_conversation_id = ?",
                arrayOf(conversationId)
            )?.use { query ->
                if (!query.moveToFirst()) {
                    return@performOperation null
                }
                query.getInteger("conversation_type")
            }
        }
    }

    fun getDMConversationId(userId: String): String? {
        if (hasArroyoConversationTable) {
            return friendDMsCache[userId]
        }

        return useDatabase(DatabaseType.ARROYO)?.performOperation {
            readDatabaseObject(
                UserConversationLink(),
                "user_conversation",
                "user_id = ? AND conversation_type = 0",
                arrayOf(userId)
            )?.clientConversationId
        }
    }

    private fun getConversationParticipantsRaw(conversationId: String): List<String>? {
        if (hasArroyoConversationTable) {
            return useDatabase(DatabaseType.ARROYO)?.performOperation {
                safeRawQuery(
                    "SELECT conversation_metadata FROM conversation WHERE client_conversation_id = ?",
                    arrayOf(conversationId)
                )?.use { query ->
                    val participants = mutableListOf<String>()
                    if (!query.moveToFirst()) {
                        return@performOperation null
                    }
                    val conversationMetadata = ProtoReader(query.getBlobOrNull("conversation_metadata") ?: return@performOperation null)

                    conversationMetadata.eachBuffer(3) {
                        participants.add(getByteArray(1, 1)?.toSnapUUID()?.toString() ?: return@eachBuffer)
                    }

                    participants
                }
            }
        }

        return useDatabase(DatabaseType.ARROYO)?.performOperation {
            safeRawQuery(
                "SELECT user_id FROM user_conversation WHERE client_conversation_id = ?",
                arrayOf(conversationId)
            )?.use { query ->
                if (!query.moveToFirst()) {
                    return@performOperation emptyList()
                }
                val participants = mutableListOf<String>()
                do {
                    query.getStringOrNull("user_id")?.let { participants.add(it) }
                } while (query.moveToNext())
                participants
            }
        }
    }

    fun getDMOtherParticipant(conversationId: String): String? {
        if (dmOtherParticipantCache.containsKey(conversationId)) return dmOtherParticipantCache[conversationId]

        return getConversationParticipantsRaw(conversationId)?.takeIf { it.size == 2 }?.firstOrNull { it != myUserId }.also {
            dmOtherParticipantCache[conversationId] = it
        }
    }

    fun getStoryEntryFromId(storyId: String): StoryEntry? {
        return useDatabase(DatabaseType.MAIN)?.performOperation  {
            readDatabaseObject(StoryEntry(), "Story", "storyId = ?", arrayOf(storyId))
        }
    }

    fun getConversationParticipants(conversationId: String, useCache: Boolean = true): List<String>? {
        if (dmOtherParticipantCache[conversationId] != null && useCache) return dmOtherParticipantCache[conversationId]?.let { listOf(myUserId, it) }

        return getConversationParticipantsRaw(conversationId)?.also {
            if (!dmOtherParticipantCache.containsKey(conversationId)) {
                dmOtherParticipantCache[conversationId] = it.firstOrNull { it != myUserId }
            }
        }
    }

    fun getMessagesFromConversationId(
        conversationId: String,
        limit: Int,
        page: Int = 0,
    ): List<ConversationMessage>? {
        return useDatabase(DatabaseType.ARROYO)?.performOperation {
            safeRawQuery(
                "SELECT * FROM conversation_message WHERE client_conversation_id = ? ORDER BY creation_timestamp DESC LIMIT ? OFFSET ?",
                arrayOf(conversationId, limit.toString(), (limit * page).toString())
            )?.use { query ->
                if (!query.moveToFirst()) {
                    return@performOperation null
                }
                val messages = mutableListOf<ConversationMessage>()
                do {
                    val message = ConversationMessage()
                    message.write(query)
                    messages.add(message)
                } while (query.moveToNext())
                messages
            }
        }
    }

    fun getAddSource(userId: String): String? {
        return useDatabase(DatabaseType.MAIN)?.performOperation  {
            rawQuery(
                "SELECT addSource FROM FriendWhoAddedMe WHERE userId = ?",
                arrayOf(userId)
            ).use {
                if (!it.moveToFirst()) {
                    return@performOperation null
                }
                it.getStringOrNull("addSource")
            }
        }
    }

    fun setStoriesViewedState(userId: String, viewed: Boolean): Boolean {
        var success = false
        useDatabase(DatabaseType.MAIN, writeMode = true)?.apply {
            performOperation {
                success = update(
                    "StorySnap",
                    ContentValues().apply {
                        put("viewed", if (viewed) 1 else 0)
                    },
                    "userId = ? AND viewed != ?",
                    arrayOf(userId, if (viewed) "1" else "0")
                ) > 0
            }
            close()
        }
        return success
    }

    fun getAccessTokens(userId: String): Map<String, String>? {
        return useDatabase(DatabaseType.MAIN)?.performOperation {
            rawQuery(
                "SELECT accessTokensPb FROM SnapToken WHERE userId = ?",
                arrayOf(userId)
            ).use {
                if (!it.moveToFirst()) {
                    return@performOperation null
                }
                val reader = ProtoReader(it.getBlobOrNull("accessTokensPb") ?: return@performOperation null)
                val services = mutableMapOf<String, String>()

                reader.eachBuffer(1) {
                    val token = getString(1) ?: return@eachBuffer
                    val service = getString(2)?.substringAfterLast("/") ?: return@eachBuffer
                    services[service] = token
                }

                services
            }
        }
    }

    private fun getBestFriends(): List<FriendInfo> {
        return useDatabase(DatabaseType.MAIN)?.performOperation {
            safeRawQuery(
                "SELECT * FROM Friend WHERE friendmojiCategories != ''",
                null
            )?.use { query ->
                val list = mutableListOf<FriendInfo>()
                while (query.moveToNext()) {
                    val friendInfo = FriendInfo()
                    try {
                        friendInfo.write(query)
                    } catch (_: Throwable) {}
                    list.add(friendInfo)
                }
                list
            }
        } ?: emptyList()
    }

    fun updatePinnedBestFriendStatus(userId: String, friendmoji: String) {
        useDatabase(DatabaseType.MAIN, writeMode = true)?.apply {
            val numberOneBestFriends = getBestFriends().filter { friend ->
                friend.friendmojiCategories?.split(",")?.any { it.startsWith("number_one") } == true
            }

            numberOneBestFriends.forEach { friendInfo ->
                performOperation {
                    update(
                        "Friend",
                        ContentValues().apply {
                            put("friendmojiCategories", friendInfo.friendmojiCategories?.split(",")?.filter {
                                it == "on_fire" || it == "birthday"
                            }?.joinToString(",") ?: "")
                            put("isPinnedBestFriend", 0)
                        },
                        "userId = ?",
                        arrayOf(friendInfo.userId)
                    )
                }
            }

            val friend = getFriendInfo(userId) ?: return@apply
            performOperation {
                update(
                    "Friend",
                    ContentValues().apply {
                        put("friendmojiCategories", (friend.friendmojiCategories?.split(",") ?: listOf()).toMutableList().apply {
                            add(friendmoji)
                        }.joinToString(","))
                        put("isPinnedBestFriend", 1)
                    },
                    "userId = ?",
                    arrayOf(userId)
                )
            }
        }?.close()
    }

    fun getStorySnapEntry(rawSnapId: String): StorySnapEntry? {
        return useDatabase(DatabaseType.SIMPLE_DB_HELPER)?.performOperation {
            readDatabaseObject(
                StorySnapEntry(),
                "DiscoverStorySnap",
                "rawSnapId = ?",
                arrayOf(rawSnapId)
            )
        }
    }

    fun setCameraType(cameraType: String) {
        useDatabase(DatabaseType.CORE, writeMode = true)?.use { database ->
            database.performOperation {
                if (rawQuery("SELECT * FROM Preferences WHERE 'key' = 'CAMERA~CAMERA_TYPE'", null).use { !it.moveToFirst() }) {
                    insert(
                        "Preferences",
                        null,
                        ContentValues().apply {
                            put("key", "CAMERA~CAMERA_TYPE")
                            put("type", 0)
                            put("stringValue", cameraType)
                        }
                    )
                } else update(
                    "Preferences",
                    ContentValues().apply {
                        put("stringValue", cameraType)
                    },
                    "key = ?",
                    arrayOf("CAMERA~CAMERA_TYPE")
                )
            }
        }
    }
}