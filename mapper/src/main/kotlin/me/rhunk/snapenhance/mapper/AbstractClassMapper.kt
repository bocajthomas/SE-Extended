package me.rhunk.snapenhance.mapper

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject

abstract class AbstractClassMapper(
    val mapperName: String
) {
    lateinit var classLoader: ClassLoader

    private val gson = Gson()
    private val values = mutableMapOf<String, Any?>()
    private val mappers = mutableListOf<MapperContext.() -> Unit>()

    private fun findClassSafe(className: String?) = runCatching {
        classLoader.loadClass(className)
    }.getOrNull()

    @Suppress("UNCHECKED_CAST")
    inner class PropertyDelegate<T>(
        private val key: String,
        defaultValue: Any? = null,
        private val setter: (Any?) -> Unit = { values[key] = it },
        private val getter: (Any?) -> T? = { it as? T }
    ) {
        init {
            values[key] = defaultValue
        }

        operator fun getValue(thisRef: Any?, property: Any?): T? {
            return getter(values[key])
        }

        operator fun setValue(thisRef: Any?, property: Any?, value: Any?) {
            setter(value)
        }

        fun set(value: String?) {
            values[key] = value
        }

        fun get(): T? {
            return getter(values[key])
        }

        fun getAsClass(): Class<*>? {
            return getter(values[key]) as? Class<*>
        }

        fun getAsString(): String? {
            return getter(values[key])?.toString()
        }

        fun getAsMap(): Map<String, String?>? {
            return getter(values[key]) as? Map<String, String?>
        }

        fun getClass(key: String): Class<*>? {
            return (get() as? Map<String, String?>)?.let {
                findClassSafe(it[key].toString())
            }
        }

        override fun toString() = getter(values[key]).toString()
    }

    fun string(key: String): PropertyDelegate<String> = PropertyDelegate(key, null)

    fun classReference(key: String): PropertyDelegate<Class<*>> = PropertyDelegate(key, getter = { findClassSafe(it as? String) })

    fun map(key: String, value: MutableMap<String, String?> = mutableMapOf()): PropertyDelegate<MutableMap<String, String?>> = PropertyDelegate(key, value)

    fun readFromJson(json: JsonObject) {
        values.forEach { (key, _) ->
            runCatching {
                val jsonElement = json.get(key) ?: return@forEach
                when (jsonElement) {
                    is JsonObject -> values[key] = gson.fromJson(jsonElement, HashMap::class.java)
                    else -> values[key] = jsonElement.asString
                }
            }.onFailure {
                Log.e("Mapper","Failed to deserialize property $key")
            }
        }
    }

    fun writeFromJson(json: JsonObject): List<String> {
        val warns = mutableListOf<String>()
        values.forEach { (key, value) ->
            runCatching {
                when (value) {
                    is String -> json.addProperty(key, value)
                    is Class<*> -> json.addProperty(key, value.name)
                    is Map<*, *> -> json.add(key, gson.toJsonTree(value))
                    else -> json.addProperty(key, value.toString())
                }
            }.onFailure {
                Log.e("Mapper","Failed to serialize property $key")
            }
            if (json.get(key).let { it.isJsonNull || (it.isJsonPrimitive && it.asString == "null") }) {
                warns.add("Failed to serialize property $key in $mapperName")
            }
        }
        return warns
    }

    fun mapper(task: MapperContext.() -> Unit) {
        mappers.add(task)
    }

    fun run(context: MapperContext) {
        mappers.forEach { it(context) }
    }
}