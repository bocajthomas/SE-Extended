package me.rhunk.snapenhance.core.features.impl.tweaks

import me.rhunk.snapenhance.common.data.ContentType
import me.rhunk.snapenhance.common.data.MessagingRuleType
import me.rhunk.snapenhance.common.util.protobuf.ProtoEditor
import me.rhunk.snapenhance.common.util.protobuf.ProtoReader
import me.rhunk.snapenhance.core.event.events.impl.NativeUnaryCallEvent
import me.rhunk.snapenhance.core.features.MessagingRuleFeature
import me.rhunk.snapenhance.core.wrapper.impl.SnapUUID

class UnsaveableMessages : MessagingRuleFeature(
    "Unsaveable Messages",
    MessagingRuleType.UNSAVEABLE_MESSAGES
) {
    override fun init() {
        if (context.config.rules.getRuleState(MessagingRuleType.UNSAVEABLE_MESSAGES) == null) return

        context.event.subscribe(NativeUnaryCallEvent::class) { event ->
            if (event.uri != "/messagingcoreservice.MessagingCoreService/CreateContentMessage") return@subscribe

            val protoReader = ProtoReader(event.buffer)
            val conversationIds = mutableListOf<String>()

            protoReader.eachBuffer(3) {
                if (contains(2)) {
                    return@eachBuffer
                }
                conversationIds.add(SnapUUID(getByteArray(1, 1, 1) ?: return@eachBuffer).toString())
            }

            if (conversationIds.all { canUseRule(it) }) {
                event.buffer = ProtoEditor(event.buffer).apply {
                    edit(4) {
                        val contentType = firstOrNull(2)?.value
                        if (contentType != ContentType.STATUS.id.toLong() && firstOrNull(4)?.toReader()?.contains(11) != true && contentType != null) {
                            remove(7)
                            addVarInt(7, 3) // set savePolicy to VIEW_SESSION except for status and snaps
                        }
                    }
                }.toByteArray()
            }
        }
    }
}