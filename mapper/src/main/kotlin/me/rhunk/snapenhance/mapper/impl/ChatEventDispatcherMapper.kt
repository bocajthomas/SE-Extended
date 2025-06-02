package me.rhunk.snapenhance.mapper.impl

import me.rhunk.snapenhance.mapper.AbstractClassMapper
import me.rhunk.snapenhance.mapper.ext.getClassName

class ChatEventDispatcherMapper : AbstractClassMapper("ChatEventDispatcher")  {
    val classReference = classReference("class")

    init {
        mapper {
            for (clazz in classes) {
                if (clazz.methods.firstOrNull { it.name == "onChatItemDoubleClickEvent" } == null) continue
                classReference.set(clazz.getClassName())
                return@mapper
            }
        }
    }
}