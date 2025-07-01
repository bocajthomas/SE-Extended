package me.rhunk.snapenhance.common.util.snap

import me.rhunk.snapenhance.common.Constants
import me.rhunk.snapenhance.common.util.ktx.await
import me.rhunk.snapenhance.common.util.protobuf.ProtoReader
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.util.Base64

object RemoteMediaResolver {
    const val CF_ST_CDN_D = "https://cf-st.sc-cdn.net/d/"

    val okHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .retryOnConnectionFailure(true)
        .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    fun newResolveRequest(protoKey: ByteArray): Request {
        val protoReader = ProtoReader(protoKey)
        val url = if (!protoReader.containsPath(2, 3)) {
            "https://bolt-gcdn.sc-cdn.net/br/" + protoReader.getString(2, 2)
        }
        else {
            "https://gcp.api.snapchat.com/bolt-http/resolve?co=" + Base64.getUrlEncoder().encodeToString(protoKey)
        }

        return Request.Builder()
            .url(url)
            .addHeader("User-Agent", Constants.USER_AGENT)
            .build()
    }

    suspend inline fun downloadMedia(url: String, decryptionCallback: (InputStream) -> InputStream = { it }, result: (InputStream, Long) -> Unit) {
        okHttpClient.newCall(Request.Builder().url(url).build()).await().use { response ->
            if (!response.isSuccessful) {
                throw Throwable("invalid response ${response.code}")
            }
            result(decryptionCallback(response.body.byteStream()), response.body.contentLength())
        }
    }

    suspend inline fun downloadBoltMedia(
        protoKey: ByteArray,
        decryptionCallback: (InputStream) -> InputStream = { it },
        resultCallback: (stream: InputStream, length: Long) -> Unit
    ) {
        okHttpClient.newCall(newResolveRequest(protoKey)).await().use { response ->
            if (!response.isSuccessful) {
                throw Throwable("invalid response ${response.code}")
            }
            resultCallback(
                decryptionCallback(
                    response.body.byteStream()
                ),
                response.body.contentLength()
            )
        }
    }

    fun getMediaHeaders(protoKey: ByteArray): Headers {
        val request = newResolveRequest(protoKey)
        return okHttpClient.newCall(request.newBuilder().method("HEAD", null).build()).execute().headers
    }
}
