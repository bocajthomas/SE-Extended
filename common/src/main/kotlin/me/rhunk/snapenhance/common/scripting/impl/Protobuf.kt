package me.rhunk.snapenhance.common.scripting.impl

import me.rhunk.snapenhance.common.scripting.bindings.AbstractBinding
import me.rhunk.snapenhance.common.scripting.bindings.BindingSide
import me.rhunk.snapenhance.common.scripting.ktx.putFunction
import me.rhunk.snapenhance.common.scripting.ktx.scriptableObject
import me.rhunk.snapenhance.common.util.protobuf.*
import org.mozilla.javascript.NativeArray
import java.io.InputStream


class Protobuf : AbstractBinding("protobuf", BindingSide.COMMON) {
    private fun parseInput(input: Any?): ByteArray? {
        return when (input) {
            is ByteArray -> input
            is InputStream -> input.readBytes()
            is NativeArray -> input.toArray().map { it as Byte }.toByteArray()
            else -> {
                context.runtime.logger.error("Invalid input type for buffer: $input")
                null
            }
        }
    }

    override fun getObject(): Any {
        return scriptableObject {
            putFunction("reader") { args ->
                val input = args?.get(0) ?: return@putFunction null

                val buffer = parseInput(input) ?: run {
                    return@putFunction null
                }

                ProtoReader(buffer)
            }
            putFunction("writer") {
                ProtoWriter()
            }
            putFunction("editor") { args ->
                val input = args?.get(0) ?: return@putFunction null

                val buffer = parseInput(input) ?: run {
                    return@putFunction null
                }
                ProtoEditor(buffer)
            }

            putFunction("grpcWriter") { args ->
                val messages = args?.mapNotNull {
                    parseInput(it)
                }?.toTypedArray() ?: run {
                    return@putFunction null
                }

                GrpcWriter(*messages)
            }

            putFunction("grpcReader") { args ->
                val input = args?.get(0) ?: return@putFunction null

                val buffer = parseInput(input) ?: run {
                    return@putFunction null
                }

                GrpcReader(buffer)
            }
        }
    }
}