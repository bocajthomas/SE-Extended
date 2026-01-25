package me.rhunk.snapenhance.core.wrapper.impl.valdi

import me.rhunk.snapenhance.core.SnapEnhance
import me.rhunk.snapenhance.core.wrapper.AbstractWrapper
import java.io.Closeable

class ValdiMarshaller(obj: Any): AbstractWrapper(obj), Closeable {
    companion object {
        fun create(): ValdiMarshaller? {
            return ValdiMarshaller(SnapEnhance.classCache.valdiMarshaller.getMethod("create").invoke(null) ?: return null)
        }
    }

    private val getUntypedMethod by lazy { instanceNonNull().javaClass.methods.first { it.name == "getUntyped" } }
    private val getSizeMethod by lazy { instanceNonNull().javaClass.methods.first { it.name == "getSize" } }
    private val pushUntypedMethod by lazy { instanceNonNull().javaClass.methods.first { it.name == "pushUntyped" } }
    private val destroy by lazy { instanceNonNull().javaClass.methods.first { it.name == "destroy" } }

    fun getUntyped(index: Int): Any? = getUntypedMethod.invoke(instanceNonNull(), index)
    fun getSize() = getSizeMethod.invoke(instanceNonNull()) as Int
    fun pushUntyped(value: Any?): Any? = pushUntypedMethod.invoke(instanceNonNull(), value)

    fun destroy() {
        destroy.invoke(instanceNonNull())
    }

    override fun close() {
        runCatching { destroy() }
    }
}