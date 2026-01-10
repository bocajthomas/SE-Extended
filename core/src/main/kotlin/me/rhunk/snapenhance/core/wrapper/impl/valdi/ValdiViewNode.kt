package me.rhunk.snapenhance.core.wrapper.impl.valdi

import me.rhunk.snapenhance.core.SnapEnhance
import me.rhunk.snapenhance.core.wrapper.AbstractWrapper

class ValdiViewNode(obj: Long) : AbstractWrapper(obj) {
    companion object {
        fun fromNode(viewNode: Any?): ValdiViewNode? {
            return (viewNode?.javaClass?.methods?.firstOrNull {
                it.name == "getNativeHandle"
            }?.invoke(viewNode) as? Long)?.let { ValdiViewNode(it) } ?: return null
        }
    }

    fun getAttribute(name: String): Any? {
        return SnapEnhance.classCache.nativeBridge.methods.firstOrNull {
            it.name == "getValueForAttribute"
        }?.invoke(null, instanceNonNull(), name)
    }

    fun setAttribute(name: String, value: Any) {
        SnapEnhance.classCache.nativeBridge.methods.firstOrNull {
            it.name == "setValueForAttribute"
        }?.invoke(null, instanceNonNull(), name, value, false)
    }

    fun getChildren(): List<ValdiViewNode> {
        return ((SnapEnhance.classCache.nativeBridge.methods.firstOrNull {
            it.name == "getRetainedViewNodeChildren"
        }?.invoke(null, instanceNonNull(), 1))!! as? LongArray)?.map {
            ValdiViewNode(it)
        } ?: emptyList()
    }

    fun getClassName(): String {
        return SnapEnhance.classCache.nativeBridge.methods.firstOrNull {
            it.name == "getViewClassName"
        }?.invoke(null, instanceNonNull()).toString()
    }

    override fun toString(): String {
        return SnapEnhance.classCache.nativeBridge.methods.firstOrNull {
            it.name == "getViewNodeDebugDescription"
        }?.invoke(null, instanceNonNull()).toString()
    }
}