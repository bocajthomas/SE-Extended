package me.rhunk.snapenhance.common.util.snap

import me.rhunk.snapenhance.common.Constants
import me.rhunk.snapenhance.common.util.ktx.await
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.util.Base64

object RemoteMediaResolver {
    private const val BOLT_HTTP_RESOLVER_URL = "https://web.snapchat.com/bolt-http"
    const val CF_ST_CDN_D = "https://cf-st.sc-cdn.net/d/"

    private val urlCache = mutableMapOf<String, String>()

    val okHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .retryOnConnectionFailure(true)
        .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request()
            val requestUrl = request.url.toString()

            if (urlCache.containsKey(requestUrl)) {
                val cachedUrl = urlCache[requestUrl]!!
                return@addInterceptor chain.proceed(request.newBuilder().url(cachedUrl).build())
            }

            chain.proceed(request).apply {
                val responseUrl = this.request.url.toString()
                if (responseUrl.startsWith("https://cf-st.sc-cdn.net")) {
                    urlCache[requestUrl] = responseUrl
                }
            }
        }
        .build()

    fun newResolveRequest(protoKey: ByteArray) = Request.Builder()
        .url(BOLT_HTTP_RESOLVER_URL + "/resolve?co=" + Base64.getUrlEncoder().encodeToString(protoKey))
        .addHeader("User-Agent", Constants.USER_AGENT)
        .build()

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
