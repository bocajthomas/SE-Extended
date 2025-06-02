package me.rhunk.snapenhance.storage

import android.database.sqlite.SQLiteDatabase
import me.rhunk.snapenhance.RemoteSideContext
import me.rhunk.snapenhance.common.data.MessagingFriendInfo
import me.rhunk.snapenhance.common.data.MessagingGroupInfo
import me.rhunk.snapenhance.common.util.SQLiteDatabaseHelper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class AppDatabase(
    val context: RemoteSideContext,
) {
    val executor: ExecutorService = Executors.newSingleThreadExecutor()
    lateinit var database: SQLiteDatabase

    var receiveMessagingDataCallback: (friends: List<MessagingFriendInfo>, groups: List<MessagingGroupInfo>) -> Unit = { _, _ -> }

    fun executeAsync(block: () -> Unit) {
        executor.execute {
            runCatching {
                block()
            }.onFailure {
                context.log.error("Failed to execute async block", it)
            }
        }
    }

    fun init() {
        database = context.androidContext.openOrCreateDatabase("main.db", 0, null)
        SQLiteDatabaseHelper.createTablesFromSchema(database, mapOf(
            "friends" to listOf(
                "id INTEGER PRIMARY KEY AUTOINCREMENT",
                "userId CHAR(36) UNIQUE",
                "dmConversationId VARCHAR(36)",
                "displayName VARCHAR",
                "mutableUsername VARCHAR",
                "bitmojiId VARCHAR",
                "selfieId VARCHAR"
            ),
            "groups" to listOf(
                "id INTEGER PRIMARY KEY AUTOINCREMENT",
                "conversationId CHAR(36) UNIQUE",
                "name VARCHAR",
                "participantsCount INTEGER"
            ),
            "rules" to listOf(
                "id INTEGER PRIMARY KEY AUTOINCREMENT",
                "type VARCHAR",
                "targetUuid VARCHAR"
            ),
            "streaks" to listOf(
                "id VARCHAR PRIMARY KEY",
                "notify BOOLEAN",
                "expirationTimestamp BIGINT",
                "length INTEGER"
            ),
            "enabled_scripts" to listOf(
                "name VARCHAR PRIMARY KEY",
            ),
            "tracker_rules" to listOf(
                "id INTEGER PRIMARY KEY AUTOINCREMENT",
                "enabled BOOLEAN DEFAULT 1",
                "name VARCHAR",
            ),
            "tracker_scopes" to listOf(
                "id INTEGER PRIMARY KEY AUTOINCREMENT",
                "rule_id INTEGER",
                "scope_type VARCHAR",
                "scope_id CHAR(36)"
            ),
            "tracker_rules_events" to listOf(
                "id INTEGER PRIMARY KEY AUTOINCREMENT",
                "rule_id INTEGER",
                "flags INTEGER DEFAULT 1",
                "event_type VARCHAR",
                "params TEXT",
                "actions TEXT"
            ),
            "friend_scores" to listOf(
                "userId CHAR(36) PRIMARY KEY",
                "score BIGINT"
            ),
            "quick_tiles" to listOf(
                "key VARCHAR PRIMARY KEY",
                "position INTEGER",
            ),
            "location_coordinates" to listOf(
                "id INTEGER PRIMARY KEY AUTOINCREMENT",
                "name VARCHAR",
                "latitude DOUBLE",
                "longitude DOUBLE",
                "radius DOUBLE",
            ),
            "themes" to listOf(
                "id INTEGER PRIMARY KEY AUTOINCREMENT",
                "enabled BOOLEAN DEFAULT 0",
                "name VARCHAR",
                "description TEXT",
                "version VARCHAR",
                "author VARCHAR",
                "updateUrl VARCHAR",
                "content TEXT",
            ),
            "repositories" to listOf(
                "url VARCHAR PRIMARY KEY",
            ),
            "notes" to listOf(
                "id CHAR(36) PRIMARY KEY",
                "content TEXT",
            ),
        ))
    }
}