package me.rhunk.snapenhance.core.wrapper.impl.composer

import me.rhunk.snapenhance.core.wrapper.AbstractWrapper
import java.lang.ref.WeakReference
import java.lang.reflect.Proxy

class ComposerContext(obj: Any): AbstractWrapper(obj) {
    val componentPath by field<String>("componentPath")
    val viewModel by field<Any?>("innerViewModel")
    val moduleName by field<String>("moduleName")
    val componentContext by field<WeakReference<Any?>>("componentContext")

    fun enqueueNextRenderCallback(callback: () -> Unit) {
        val method = instanceNonNull()::class.java.methods.firstOrNull {
            it.name == "onNextLayout"
        }
        method?.invoke(instanceNonNull(), Proxy.newProxyInstance(
            instanceNonNull()::class.java.classLoader,
            arrayOf(method.parameterTypes[0])
        ) { _, _, _ ->
            callback()
        })
    }
}