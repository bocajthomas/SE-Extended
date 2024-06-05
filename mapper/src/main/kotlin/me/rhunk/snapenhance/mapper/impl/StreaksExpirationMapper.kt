package me.rhunk.snapenhance.mapper.impl

import me.rhunk.snapenhance.mapper.AbstractClassMapper
import me.rhunk.snapenhance.mapper.ext.findConstString
import me.rhunk.snapenhance.mapper.ext.getClassName
import me.rhunk.snapenhance.mapper.ext.searchNextFieldReference
import java.lang.reflect.Modifier

class StreaksExpirationMapper: AbstractClassMapper("StreaksExpirationMapper") {
    val hourGlassTimeRemainingField = string("hourGlassTimeRemainingField")
    val expirationTimeField = string("expirationTimeField")

    val streaksFormatterClass = classReference("streaksFormatterClass")
    val formatStreaksTextMethod = string("formatStreaksTextMethod")

    val simpleStreaksFormatterClass = classReference("simpleStreaksFormatterClass")
    val formatSimpleStreaksTextMethod = string("formatSimpleStreaksTextMethod")

    init {
        mapper {
            var streaksResultClass: String? = null
            for (clazz in classes) {
                val toStringMethod = clazz.methods.firstOrNull { it.name == "toString" } ?: continue
                if (toStringMethod.implementation?.findConstString("StreaksResult(", startsWith = true) != true) continue
                streaksResultClass = clazz.type
                break
            }

            if (streaksResultClass == null) return@mapper

            for (clazz in classes) {
                val formatStreaksTextDexMethod = clazz.methods.firstOrNull { method ->
                    Modifier.isStatic(method.accessFlags) &&
                    method.returnType == "Ljava/lang/String;" &&
                    method.parameterTypes.let {
                        it.size >= 3 && it.first() == streaksResultClass && it[1] == "Ljava/lang/String;" && it[2] == "J"
                    }
                } ?: continue
                simpleStreaksFormatterClass.set(clazz.getClassName())
                formatSimpleStreaksTextMethod.set(formatStreaksTextDexMethod.name)
                return@mapper
            }
        }

        mapper {
            var streaksExpirationClassName: String? = null
            for (clazz in classes) {
                val toStringMethod = clazz.methods.firstOrNull { it.name == "toString" } ?: continue
                if (toStringMethod.implementation?.findConstString("StreaksExpiration(", contains = true) != true) continue

                streaksExpirationClassName = clazz.getClassName()
                toStringMethod.implementation?.apply {
                    hourGlassTimeRemainingField.set(searchNextFieldReference("hourGlassTimeRemaining", contains = true)?.name)
                    expirationTimeField.set(searchNextFieldReference("expirationTime", contains = true)?.name)
                }
                break
            }

            if (streaksExpirationClassName == null) return@mapper

            for (clazz in classes) {
                val formatStreaksTextDexMethod = clazz.methods.firstOrNull { method ->
                    Modifier.isStatic(method.accessFlags) &&
                    method.returnType == "Ljava/lang/String;" &&
                    method.parameterTypes.let {
                        it.size >= 4 && it[0] == "Ljava/util/Map;" && it[2] == "Ljava/lang/Integer;" && it[3].contains(streaksExpirationClassName)
                    }
                } ?: continue
                streaksFormatterClass.set(clazz.getClassName())
                formatStreaksTextMethod.set(formatStreaksTextDexMethod.name)
                break
            }
        }
    }
}