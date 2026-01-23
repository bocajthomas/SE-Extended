package me.rhunk.snapenhance.core.wrapper.impl.valdi

import me.rhunk.snapenhance.core.SnapEnhance
import me.rhunk.snapenhance.core.wrapper.AbstractWrapper

class ValdiFunction(obj: Any): AbstractWrapper(obj) {
    private val performMethod by lazy { instanceNonNull().javaClass.getMethod("perform",
        SnapEnhance.classCache.valdiMarshaller) }

    fun perform(valdiMarshaller: ValdiMarshaller): Boolean = performMethod.invoke(instanceNonNull(), valdiMarshaller.instanceNonNull()) as Boolean
}