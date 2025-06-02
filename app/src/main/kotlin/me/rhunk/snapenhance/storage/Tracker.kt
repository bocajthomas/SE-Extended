package me.rhunk.snapenhance.storage

import android.content.ContentValues
import com.google.gson.JsonArray
import kotlinx.coroutines.runBlocking
import me.rhunk.snapenhance.common.data.TrackerRule
import me.rhunk.snapenhance.common.data.TrackerRuleAction
import me.rhunk.snapenhance.common.data.TrackerRuleActionParams
import me.rhunk.snapenhance.common.data.TrackerRuleEvent
import me.rhunk.snapenhance.common.data.TrackerScopeType
import me.rhunk.snapenhance.common.util.ktx.getInteger
import me.rhunk.snapenhance.common.util.ktx.getLongOrNull
import me.rhunk.snapenhance.common.util.ktx.getStringOrNull
import kotlin.coroutines.suspendCoroutine


fun AppDatabase.clearTrackerRules() {
    runBlocking {
        suspendCoroutine { continuation ->
            executeAsync {
                database.execSQL("DELETE FROM tracker_rules")
                database.execSQL("DELETE FROM tracker_rules_events")
                continuation.resumeWith(Result.success(Unit))
            }
        }
    }
}

fun AppDatabase.deleteTrackerRule(ruleId: Int) {
    executeAsync {
        database.execSQL("DELETE FROM tracker_rules WHERE id = ?", arrayOf(ruleId))
        database.execSQL("DELETE FROM tracker_rules_events WHERE rule_id = ?", arrayOf(ruleId))
    }
}

fun AppDatabase.newTrackerRule(name: String = "Custom Rule"): Int {
    return runBlocking {
        suspendCoroutine { continuation ->
            executeAsync {
                val id = database.insert("tracker_rules", null, ContentValues().apply {
                    put("name", name)
                })
                continuation.resumeWith(Result.success(id.toInt()))
            }
        }
    }
}

fun AppDatabase.addOrUpdateTrackerRuleEvent(
    ruleEventId: Int? = null,
    ruleId: Int? = null,
    eventType: String? = null,
    params: TrackerRuleActionParams,
    actions: List<TrackerRuleAction>
): Int? {
    return runBlocking {
        suspendCoroutine { continuation ->
            executeAsync {
                val id = if (ruleEventId != null) {
                    database.execSQL("UPDATE tracker_rules_events SET params = ?, actions = ? WHERE id = ?", arrayOf(
                        context.gson.toJson(params),
                        context.gson.toJson(actions.map { it.key }),
                        ruleEventId
                    ))
                    ruleEventId
                } else {
                    database.insert("tracker_rules_events", null, ContentValues().apply {
                        put("rule_id", ruleId)
                        put("event_type", eventType)
                        put("params", context.gson.toJson(params))
                        put("actions", context.gson.toJson(actions.map { it.key }))
                    }).toInt()
                }
                continuation.resumeWith(Result.success(id))
            }
        }
    }
}

fun AppDatabase.deleteTrackerRuleEvent(eventId: Int) {
    executeAsync {
        database.execSQL("DELETE FROM tracker_rules_events WHERE id = ?", arrayOf(eventId))
    }
}

fun AppDatabase.getTrackerRulesDesc(): List<TrackerRule> {
    val rules = mutableListOf<TrackerRule>()

    database.rawQuery("SELECT * FROM tracker_rules ORDER BY id DESC", null).use { cursor ->
        while (cursor.moveToNext()) {
            rules.add(
                TrackerRule(
                    id = cursor.getInteger("id"),
                    enabled = cursor.getInteger("enabled") == 1,
                    name = cursor.getStringOrNull("name") ?: "",
                )
            )
        }
    }

    return rules
}

fun AppDatabase.getTrackerRule(ruleId: Int): TrackerRule? {
    return database.rawQuery("SELECT * FROM tracker_rules WHERE id = ?", arrayOf(ruleId.toString())).use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        TrackerRule(
            id = cursor.getInteger("id"),
            enabled = cursor.getInteger("enabled") == 1,
            name = cursor.getStringOrNull("name") ?: "",
        )
    }
}

fun AppDatabase.setTrackerRuleName(ruleId: Int, name: String) {
    executeAsync {
        database.execSQL("UPDATE tracker_rules SET name = ? WHERE id = ?", arrayOf(name, ruleId))
    }
}

fun AppDatabase.setTrackerRuleState(ruleId: Int, enabled: Boolean) {
    executeAsync {
        database.execSQL("UPDATE tracker_rules SET enabled = ? WHERE id = ?", arrayOf(if (enabled) 1 else 0, ruleId))
    }
}

fun AppDatabase.getTrackerEvents(ruleId: Int): List<TrackerRuleEvent> {
    val events = mutableListOf<TrackerRuleEvent>()
    database.rawQuery("SELECT * FROM tracker_rules_events WHERE rule_id = ?", arrayOf(ruleId.toString())).use { cursor ->
        while (cursor.moveToNext()) {
            events.add(
                TrackerRuleEvent(
                    id = cursor.getInteger("id"),
                    eventType = cursor.getStringOrNull("event_type") ?: continue,
                    enabled = cursor.getInteger("flags") == 1,
                    params = context.gson.fromJson(cursor.getStringOrNull("params") ?: "{}", TrackerRuleActionParams::class.java),
                    actions = context.gson.fromJson(cursor.getStringOrNull("actions") ?: "[]", JsonArray::class.java).mapNotNull {
                        TrackerRuleAction.fromString(it.asString)
                    }
                )
            )
        }
    }
    return events
}

fun AppDatabase.getTrackerEvents(eventType: String): Map<TrackerRuleEvent, TrackerRule> {
    val events = mutableMapOf<TrackerRuleEvent, TrackerRule>()
    database.rawQuery("SELECT tracker_rules_events.id as event_id, tracker_rules_events.params as event_params," +
            "tracker_rules_events.actions, tracker_rules_events.flags, tracker_rules_events.event_type, tracker_rules.name, tracker_rules.id as rule_id " +
            "FROM tracker_rules_events " +
            "INNER JOIN tracker_rules " +
            "ON tracker_rules_events.rule_id = tracker_rules.id " +
            "WHERE event_type = ? AND tracker_rules.enabled = 1", arrayOf(eventType)
    ).use { cursor ->
        while (cursor.moveToNext()) {
            val trackerRule = TrackerRule(
                id = cursor.getInteger("rule_id"),
                enabled = true,
                name = cursor.getStringOrNull("name") ?: "",
            )
            val trackerRuleEvent = TrackerRuleEvent(
                id = cursor.getInteger("event_id"),
                eventType = cursor.getStringOrNull("event_type") ?: continue,
                enabled = cursor.getInteger("flags") == 1,
                params = context.gson.fromJson(cursor.getStringOrNull("event_params") ?: "{}", TrackerRuleActionParams::class.java),
                actions = context.gson.fromJson(cursor.getStringOrNull("actions") ?: "[]", JsonArray::class.java).mapNotNull {
                    TrackerRuleAction.fromString(it.asString)
                }
            )
            events[trackerRuleEvent] = trackerRule
        }
    }
    return events
}

fun AppDatabase.setRuleTrackerScopes(ruleId: Int, type: TrackerScopeType, scopes: List<String>) {
    executeAsync {
        database.execSQL("DELETE FROM tracker_scopes WHERE rule_id = ?", arrayOf(ruleId))
        scopes.forEach { scopeId ->
            database.execSQL("INSERT INTO tracker_scopes (rule_id, scope_type, scope_id) VALUES (?, ?, ?)", arrayOf(
                ruleId,
                type.key,
                scopeId
            ))
        }
    }
}

fun AppDatabase.getRuleTrackerScopes(ruleId: Int, limit: Int = Int.MAX_VALUE): Map<String, TrackerScopeType> {
    val scopes = mutableMapOf<String, TrackerScopeType>()
    database.rawQuery("SELECT * FROM tracker_scopes WHERE rule_id = ? LIMIT ?", arrayOf(ruleId.toString(), limit.toString())).use { cursor ->
        while (cursor.moveToNext()) {
            scopes[cursor.getStringOrNull("scope_id") ?: continue] = TrackerScopeType.entries.find { it.key == cursor.getStringOrNull("scope_type") } ?: continue
        }
    }
    return scopes
}

fun AppDatabase.updateFriendScore(userId: String, score: Long): Long {
    return runBlocking {
        suspendCoroutine { continuation ->
            executeAsync {
                val currentScore = database.rawQuery("SELECT score FROM friend_scores WHERE userId = ?", arrayOf(userId)).use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    cursor.getLongOrNull("score")
                }

                if (currentScore != null) {
                    database.execSQL("UPDATE friend_scores SET score = ? WHERE userId = ?", arrayOf(score, userId))
                } else {
                    database.execSQL("INSERT INTO friend_scores (userId, score) VALUES (?, ?)", arrayOf(userId, score))
                }

                continuation.resumeWith(Result.success(currentScore ?: -1))
            }
        }
    }
}