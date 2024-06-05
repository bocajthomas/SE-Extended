package me.rhunk.snapenhance.core.features.impl.experiments

import me.rhunk.snapenhance.core.event.events.impl.UnaryCallEvent
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.features.FeatureLoadParams
import java.nio.ByteBuffer

class ContextMenuFix: Feature("Context Menu Fix", loadParams = FeatureLoadParams.INIT_SYNC) {
    override fun init() {
        if (!context.config.experimental.contextMenuFix.get()) return
        context.event.subscribe(UnaryCallEvent::class) { event ->
            if (event.uri == "/snapchat.maps.device.MapDevice/IsPrimary") {
                 event.canceled = true
                 val unaryEventHandler = event.adapter.arg<Any>(3)
                 runCatching {
                     unaryEventHandler::class.java.methods.first { it.name == "onEvent" }.invoke(unaryEventHandler, ByteBuffer.wrap(
                         byteArrayOf(8, 1)
                     ), null)
                 }.onFailure {
                     context.log.error(null, it)
                 }
             }
        }
    }
}