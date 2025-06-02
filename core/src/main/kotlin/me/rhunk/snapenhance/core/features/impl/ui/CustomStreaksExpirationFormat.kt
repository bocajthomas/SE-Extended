package me.rhunk.snapenhance.core.features.impl.ui

import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.hook.hookConstructor
import me.rhunk.snapenhance.core.util.ktx.getObjectField
import me.rhunk.snapenhance.core.wrapper.impl.SnapUUID
import me.rhunk.snapenhance.mapper.impl.StreaksExpirationMapper
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

class CustomStreaksExpirationFormat: Feature("CustomStreaksExpirationFormat") {
    private fun Long.padZero(): String {
        return this.toString().padStart(2, '0')
    }

    private val streakCache = ConcurrentHashMap<String, Pair<Int, Long>>()

    override fun init() {
        context.classCache.feedEntry.hookConstructor(HookStage.AFTER) { param ->
            val conversationId = SnapUUID(param.thisObject<Any>().getObjectField("mConversationId")).toString()

            val streakMetadata = param.thisObject<Any>().getObjectField("mStreakMetadata") ?: apply {
                if (streakCache.containsKey(conversationId)) streakCache.remove(conversationId)
                return@hookConstructor
            }

            streakCache.put(conversationId, streakMetadata.getObjectField("mCount") as Int to
                streakMetadata.getObjectField("mExpirationTimestampMs") as Long)
        }

        onNextActivityCreate {
            val expirationFormat by context.config.experimental.customStreaksExpirationFormat
            if (expirationFormat.isNotEmpty() || context.config.userInterface.streakExpirationInfo.get()) {
                context.mappings.useMapper(StreaksExpirationMapper::class) {
                    simpleStreaksFormatterClass.getAsClass()?.hook(
                        formatSimpleStreaksTextMethod.get() ?: return@useMapper,
                        HookStage.AFTER
                    ) { param ->
                        val result = param.getResult() as? String ?: return@hook

                        streakCache[param.arg<String>(1)]?.also { (streakCount, expirationTime) ->
                            if (expirationTime <= 0L) return@also

                            if (expirationFormat.isEmpty()) {
                                val remainingTime = (expirationTime - System.currentTimeMillis()).milliseconds.inWholeHours
                                var emojiIndex = result.indexOfFirst { it.code > 127 }.takeIf { it != -1 }
                                    ?.let { it + 2 }

                                if (emojiIndex == null) {
                                    emojiIndex = result.length
                                }

                                param.setResult(
                                    result.substring(0, emojiIndex) + remainingTime + result.substring(emojiIndex)
                                )
                                return@hook
                            }

                            val delta = (expirationTime - System.currentTimeMillis()).milliseconds

                            val hourGlassEmoji =
                                if (delta.inWholeMilliseconds in 1..(15.hours.inWholeMilliseconds)) if (expirationTime % 2 == 0L) "\u23F3" else "\u231B" else ""

                            param.setResult(
                                expirationFormat
                                    .replace("%c", streakCount.toString())
                                    .replace("%e", hourGlassEmoji)
                                    .replace("%d", delta.inWholeDays.toString())
                                    .replace("%h", (delta.inWholeHours % 24).padZero())
                                    .replace("%m", (delta.inWholeMinutes % 60).padZero())
                                    .replace("%s", (delta.inWholeSeconds % 60).padZero())
                                    .replace("%w", delta.toString())
                            )
                        }
                    }
                }
            }
        }
    }
}