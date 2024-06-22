package me.rhunk.snapenhance.common.util.protobuf

import org.mozilla.javascript.annotations.JSFunction
import java.io.ByteArrayOutputStream

fun ProtoWriter.toGrpcWriter() = GrpcWriter(toByteArray())

class GrpcWriter(
    vararg val messages: ByteArray
) {
    private val headers = mutableMapOf<String, String>()

    @JSFunction
    fun addHeader(key: String, value: String) {
        headers[key] = value
    }

    @JSFunction
    fun toByteArray(): ByteArray {
        val stream = ByteArrayOutputStream()

        fun writeByte(value: Int) = stream.write(value)
        fun writeUInt(value: Int) {
            writeByte(value ushr 24)
            writeByte(value ushr 16)
            writeByte(value ushr 8)
            writeByte(value)
        }

        messages.forEach { message ->
            writeByte(0)
            writeUInt(message.size)
            stream.write(message)
        }

        if (headers.isNotEmpty()){
            val rawHeaders = headers.map { (key, value) -> "$key:$value" }.joinToString("\n")
            val rawHeadersBytes = rawHeaders.toByteArray(Charsets.UTF_8)
            writeByte(-128)
            writeUInt(rawHeadersBytes.size)
            stream.write(rawHeadersBytes)
        }

        return stream.toByteArray()
    }
}