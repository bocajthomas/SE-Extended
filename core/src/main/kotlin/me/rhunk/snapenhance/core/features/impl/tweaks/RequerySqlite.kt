package me.rhunk.snapenhance.core.features.impl.tweaks

import me.rhunk.snapenhance.common.data.MessagingRuleType
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook

class RequerySqlite : Feature("Requery Sqlite") {
    override fun init() {
        val hideQuickAddSuggestions = context.config.userInterface.hideQuickAddSuggestions.get()
        val hideFriendFeedEntry = context.config.userInterface.hideFriendFeedEntry.get()
        val hideSuggestedStories = context.config.userInterface.hideStorySuggestions.get().contains("hide_suggested_friend_stories")

        if (!hideQuickAddSuggestions && !hideFriendFeedEntry && !hideSuggestedStories) return

        findClass("io.requery.android.database.sqlite.SQLiteDatabase").hook("rawQueryWithFactory", HookStage.BEFORE) { param ->
            var sqlRequest = param.argNullable<String>(1) ?: return@hook

            fun patchRequest(condition: String) {
                sqlRequest.lastIndexOf("WHERE").takeIf { it != -1 }?.let {
                    sqlRequest = sqlRequest.substring(0, it + 5) + " $condition AND " + sqlRequest.substring(it + 5)
                    param.setArg(1, sqlRequest)
                }
            }

            if (hideQuickAddSuggestions && sqlRequest.contains("SuggestedFriendPlacement")) {
                patchRequest("0 = 1")
            }

            if (hideSuggestedStories && sqlRequest.contains("DiscoverFeedFriendStoriesViewV2 AS DFStories")) {
                patchRequest("DFStories.isFriendOfFriend = 0")
            }

            if (hideFriendFeedEntry && sqlRequest.startsWith("SELECT") && (sqlRequest.contains("FriendWithUsername")) && sqlRequest.contains("userId")) {
                val ids = context.bridgeClient.getRuleIds(MessagingRuleType.HIDE_FRIEND_FEED).takeIf { it.isNotEmpty() } ?: return@hook
                patchRequest(ids.joinToString(" AND ") { "${if (sqlRequest.contains("Friend.userId")) "Friend.userId" else "userId "} != '$it'" })
            }
        }
    }
}