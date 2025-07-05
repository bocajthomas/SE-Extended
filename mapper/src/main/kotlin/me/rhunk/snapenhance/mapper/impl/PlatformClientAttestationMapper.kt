package me.rhunk.snapenhance.mapper.impl

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import me.rhunk.snapenhance.mapper.AbstractClassMapper
import me.rhunk.snapenhance.mapper.ext.getSuperClassName

class PlatformClientAttestationMapper: AbstractClassMapper("PlatformClientAttestationMapper") {
    val pluginNativeClass = classReference("pluginNativeClass")

    init {
        mapper {
            for (clazz in classes) {
                if (clazz.getSuperClassName()?.endsWith("PlatformClientAttestation") != true) continue
                val getSignatureMethod = clazz.methods.firstOrNull { it.name == "getSignature" } ?: continue

                getSignatureMethod.implementation?.instructions?.firstOrNull { instruction ->
                    instruction.opcode == Opcode.INVOKE_STATIC && instruction is Instruction35c && (instruction.reference as? MethodReference)?.takeIf { it.definingClass.count { it == '/' } == 1 }?.returnType == "[B"
                }?.let { instruction ->
                    val method = (instruction as Instruction35c).reference as MethodReference
                    pluginNativeClass.set(method.definingClass.replaceFirst("L", "").replaceFirst(";", "").replace("/", "."))
                }
                return@mapper
            }
        }
    }
}