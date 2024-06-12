package me.rhunk.snapenhance.common.util

import com.google.gson.JsonParser
import me.rhunk.snapenhance.common.Constants
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody


class TranscriptApi(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder().addInterceptor {
        it.proceed(it.request().newBuilder().header("User-Agent", Constants.USER_AGENT).build())
    }.build()
) {
    private fun genDlClearance() = okHttpClient.newCall(
        Request("https://clearance.deepl.com/token".toHttpUrl())
    ).execute().use { response ->
        val cookie = response.headers.firstOrNull { it.first.lowercase() == "set-cookie" && it.second.contains("dl_clearance", ignoreCase = true) }
        cookie?.second?.substringBefore(";")?.substringAfter("dl_clearance=")
    }

    fun transcribe(
        body: RequestBody,
        lang: String? = null,
    ): String? {
        val clearance = genDlClearance() ?: return null
        val url = "https://voice-pro.www.deepl.com/sync/transcribe".toHttpUrl().newBuilder()
            .apply {
                lang?.let { addQueryParameter("lang", it) }
            }
            .build()
        val request = Request(url, headers = Headers.headersOf(
            "Cookie", "dl_clearance=$clearance",
            "Content-Type", "audio/webm"
        ), method = "POST", body = body)
        return okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use null
            val jsonObject = JsonParser.parseString(response.body.string()).asJsonObject
            jsonObject.getAsJsonArray("segments").fold("") { text, segment ->
                text + segment.asJsonObject.getAsJsonPrimitive("text").asString
            }.trim()
        }
    }
}
