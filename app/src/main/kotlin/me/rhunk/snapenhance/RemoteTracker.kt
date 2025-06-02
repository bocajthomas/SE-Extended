package me.rhunk.snapenhance

import me.rhunk.snapenhance.bridge.logger.TrackerInterface
import me.rhunk.snapenhance.common.data.ScopedTrackerRule
import me.rhunk.snapenhance.common.data.TrackerEventsResult
import me.rhunk.snapenhance.common.data.TrackerRule
import me.rhunk.snapenhance.common.data.TrackerRuleEvent
import me.rhunk.snapenhance.common.util.toSerialized
import me.rhunk.snapenhance.storage.getRuleTrackerScopes
import me.rhunk.snapenhance.storage.getTrackerEvents
import me.rhunk.snapenhance.storage.updateFriendScore


class RemoteTracker(
    private val context: RemoteSideContext
): TrackerInterface.Stub() {
    fun init() {}

    override fun getTrackedEvents(eventType: String): String? {
        val events = mutableMapOf<TrackerRule, MutableList<TrackerRuleEvent>>()

        context.database.getTrackerEvents(eventType).forEach { (event, rule) ->
            events.getOrPut(rule) { mutableListOf() }.add(event)
        }

        return TrackerEventsResult(events.mapKeys {
            ScopedTrackerRule(it.key, context.database.getRuleTrackerScopes(it.key.id))
        }).toSerialized()
    }

    override fun updateFriendScore(userId: String, score: Long): Long {
        return context.database.updateFriendScore(userId, score)
    }
}