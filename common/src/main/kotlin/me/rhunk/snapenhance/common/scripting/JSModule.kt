package me.rhunk.snapenhance.common.scripting

import android.os.Handler
import android.widget.Toast
import me.rhunk.snapenhance.common.scripting.bindings.AbstractBinding
import me.rhunk.snapenhance.common.scripting.bindings.BindingsContext
import me.rhunk.snapenhance.common.scripting.impl.JavaInterfaces
import me.rhunk.snapenhance.common.scripting.impl.Networking
import me.rhunk.snapenhance.common.scripting.impl.Protobuf
import me.rhunk.snapenhance.common.scripting.ktx.contextScope
import me.rhunk.snapenhance.common.scripting.ktx.putFunction
import me.rhunk.snapenhance.common.scripting.ktx.scriptable
import me.rhunk.snapenhance.common.scripting.ktx.scriptableObject
import me.rhunk.snapenhance.common.scripting.type.ModuleInfo
import me.rhunk.snapenhance.common.scripting.type.Permissions
import me.rhunk.snapenhance.common.scripting.ui.InterfaceManager
import org.mozilla.javascript.Function
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined
import org.mozilla.javascript.Wrapper
import java.io.Reader
import java.lang.reflect.Modifier
import kotlin.reflect.KClass

class JSModule(
    private val scriptRuntime: ScriptRuntime,
    val moduleInfo: ModuleInfo,
    private val reader: Reader,
) {
    private val moduleBindings = mutableMapOf<String, AbstractBinding>()
    private lateinit var moduleObject: ScriptableObject

    private val moduleBindingContext by lazy {
        BindingsContext(
            moduleInfo = moduleInfo,
            runtime = scriptRuntime
        )
    }

    fun load(block: ScriptableObject.() -> Unit) {
        contextScope {
            val classLoader = scriptRuntime.androidContext.classLoader
            moduleObject = initSafeStandardObjects()
            moduleObject.putConst("module", moduleObject, scriptableObject {
                putConst("info", this, scriptableObject {
                    putConst("name", this, moduleInfo.name)
                    putConst("version", this, moduleInfo.version)
                    putConst("displayName", this, moduleInfo.displayName)
                    putConst("description", this, moduleInfo.description)
                    putConst("author", this, moduleInfo.author)
                    putConst("minSnapchatVersion", this, moduleInfo.minSnapchatVersion)
                    putConst("minSEVersion", this, moduleInfo.minSEVersion)
                    putConst("grantedPermissions", this, moduleInfo.grantedPermissions)
                })
            })

            scriptRuntime.logger.apply {
                moduleObject.putConst("console", moduleObject, scriptableObject {
                    putFunction("log") { info(argsToString(it)) }
                    putFunction("warn") { warn(argsToString(it)) }
                    putFunction("error") { error(argsToString(it)) }
                    putFunction("debug") { debug(argsToString(it)) }
                    putFunction("info") { info(argsToString(it)) }
                    putFunction("trace") { verbose(argsToString(it)) }
                    putFunction("verbose") { verbose(argsToString(it)) }
                })
            }

            registerBindings(
                JavaInterfaces(),
                InterfaceManager(),
                Networking(),
                Protobuf()
            )

            moduleObject.putFunction("setField") { args ->
                val obj = args?.get(0) ?: return@putFunction Undefined.instance
                val name = args[1].toString()
                val value = args[2]
                val field = obj.javaClass.declaredFields.find { it.name == name } ?: return@putFunction Undefined.instance
                field.isAccessible = true
                field.set(obj, value.toPrimitiveValue(lazy { field.type.name }))
                Undefined.instance
            }

            moduleObject.putFunction("getField") { args ->
                val obj = args?.get(0) ?: return@putFunction Undefined.instance
                val name = args[1].toString()
                val field = obj.javaClass.declaredFields.find { it.name == name } ?: return@putFunction Undefined.instance
                field.isAccessible = true
                field.get(obj)
            }

            moduleObject.putFunction("sleep") { args ->
                val time = args?.get(0) as? Number ?: return@putFunction Undefined.instance
                Thread.sleep(time.toLong())
                Undefined.instance
            }

            moduleObject.putFunction("findClass") {
                val className = it?.get(0).toString()
                val useModClassLoader = it?.getOrNull(1) as? Boolean ?: false
                if (useModClassLoader) moduleInfo.ensurePermissionGranted(Permissions.UNSAFE_CLASSLOADER)

                runCatching {
                    if (useModClassLoader) this::class.java.classLoader?.loadClass(className)
                    else classLoader.loadClass(className)
                }.onFailure { throwable ->
                    scriptRuntime.logger.error("Failed to load class $className", throwable)
                }.getOrNull()
            }

            moduleObject.putFunction("type") { args ->
                val className = args?.get(0).toString()
                val useModClassLoader = args?.getOrNull(1) as? Boolean ?: false
                if (useModClassLoader) moduleInfo.ensurePermissionGranted(Permissions.UNSAFE_CLASSLOADER)

                val clazz = runCatching {
                    if (useModClassLoader) this::class.java.classLoader?.loadClass(className) else classLoader.loadClass(className)
                }.getOrNull() ?: return@putFunction Undefined.instance

                scriptableObject("JavaClassWrapper") {
                    val newInstance: (Array<out Any?>?) -> Any? = { args ->
                        val constructor = clazz.declaredConstructors.find {
                            (args ?: emptyArray()).isSameParameters(it.parameterTypes)
                        }?.also { it.isAccessible = true } ?: throw IllegalArgumentException("Constructor not found with args ${argsToString(args)}")
                        constructor.newInstance(*args ?: emptyArray())
                    }
                    putFunction("__new__") { newInstance(it) }

                    clazz.declaredMethods.filter { Modifier.isStatic(it.modifiers) }.forEach { method ->
                        putFunction(method.name) { args ->
                            val declaredMethod = clazz.declaredMethods.find {
                                it.name == method.name && (args ?: emptyArray()).isSameParameters(it.parameterTypes)
                            }?.also { it.isAccessible = true } ?: throw IllegalArgumentException("Method ${method.name} not found with args ${argsToString(args)}")
                            declaredMethod.invoke(null, *args ?: emptyArray())
                        }
                    }

                    clazz.declaredFields.filter { Modifier.isStatic(it.modifiers) }.forEach { field ->
                        field.isAccessible = true
                        defineProperty(field.name, { field.get(null) }, { value -> field.set(null, value) }, 0)
                    }

                    if (get("newInstance") == null) {
                        putFunction("newInstance") { newInstance(it) }
                    }
                }
            }

            moduleObject.putFunction("logInfo") { args ->
                scriptRuntime.logger.info(argsToString(args))
                Undefined.instance
            }

            moduleObject.putFunction("logError") { args ->
                scriptRuntime.logger.error(argsToString(arrayOf(args?.get(0))), args?.getOrNull(1) as? Throwable ?: Throwable())
                Undefined.instance
            }

            for (toastFunc in listOf("longToast", "shortToast")) {
                moduleObject.putFunction(toastFunc) { args ->
                    Handler(scriptRuntime.androidContext.mainLooper).post {
                        Toast.makeText(
                            scriptRuntime.androidContext,
                            args?.joinToString(" ") ?: "",
                            if (toastFunc == "longToast") Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                        ).show()
                    }
                    Undefined.instance
                }
            }

            block(moduleObject)

            moduleBindings.forEach { (_, instance) ->
                instance.context = moduleBindingContext

                runCatching {
                    instance.onInit()
                }.onFailure {
                    scriptRuntime.logger.error("Failed to init binding ${instance.name}", it)
                }
            }

            moduleObject.putFunction("require") { args ->
                val bindingName = args?.get(0).toString()
                val (namespace, path) = bindingName.takeIf {
                    it.startsWith("@") && it.contains("/")
                }?.let {
                    it.substring(1).substringBefore("/") to it.substringAfter("/")
                } ?: (null to "")

                when (namespace) {
                    "modules" -> scriptRuntime.getModuleByName(path)?.moduleObject?.scriptable("module")?.scriptable("exports")
                    else -> moduleBindings[bindingName]?.getObject()
                }
            }
        }

        contextScope(shouldOptimize = true) {
            evaluateReader(moduleObject, reader, moduleInfo.name, 1, null)
        }
    }

    fun unload() {
        callFunction("module.onUnload")
        moduleBindings.entries.removeIf { (name, binding) ->
            runCatching {
                binding.onDispose()
            }.onFailure {
                scriptRuntime.logger.error("Failed to dispose binding $name", it)
            }
            true
        }
    }

    fun callFunction(name: String, vararg args: Any?) {
        contextScope {
            name.split(".").also { split ->
                val function = split.dropLast(1).fold(moduleObject) { obj, key ->
                    obj.get(key, obj) as? ScriptableObject ?: return@contextScope Unit
                }.get(split.last(), moduleObject) as? Function ?: return@contextScope Unit

                runCatching {
                    function.call(this, moduleObject, moduleObject, args)
                }.onFailure {
                    scriptRuntime.logger.error("Error while calling function $name", it)
                }
            }
        }
    }
    fun registerBindings(vararg bindings: AbstractBinding) {
        bindings.forEach {
            moduleBindings[it.name] = it.apply {
                context = moduleBindingContext
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getBinding(clazz: KClass<T>): T? {
        return moduleBindings.values.find { clazz.isInstance(it) } as? T
    }

    private fun argsToString(args: Array<out Any?>?): String {
        return args?.joinToString(" ") {
            when (it) {
                is Wrapper -> it.unwrap().let { value ->
                    if (value is Throwable) value.message + "\n" + value.stackTraceToString()
                    else value.toString()
                }
                else -> it.toString()
            }
        } ?: "null"
    }
}