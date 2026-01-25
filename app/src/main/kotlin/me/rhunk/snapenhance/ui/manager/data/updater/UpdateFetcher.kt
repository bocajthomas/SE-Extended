package me.rhunk.snapenhance.ui.manager.data.updater

import android.content.Context
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rhunk.snapenhance.UpdateNotification
import me.rhunk.snapenhance.common.BuildConfig
import me.rhunk.snapenhance.common.logger.AbstractLogger
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object UpdateFetcher {
    data class LatestRelease(
        val versionName: String,
        val releaseUrl: String
    )
    var cachedRelease: LatestRelease? = null
        private set
    private var lastCheckTime: Long = 0
    private const val CACHE_DURATION_MS = 60 * 60 * 1000L
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    fun getLatestUpdate(): LatestRelease? {
        val currentTime = System.currentTimeMillis()
        if (cachedRelease != null && (currentTime - lastCheckTime) < CACHE_DURATION_MS) {
            return cachedRelease
        }
        val result = if (BuildConfig.DEBUG) fetchLatestDebugCI() else fetchLatestRelease()
        lastCheckTime = currentTime
        cachedRelease = result
        return result
    }

    private fun fetchLatestRelease() = runCatching {
        val endpoint = Request.Builder()
            .url("https://api.github.com/repos/bocajthomas/SE-Extended/releases")
            .build()

        val response = client.newCall(endpoint).execute()

        if (!response.isSuccessful) throw Throwable("Failed to fetch releases: ${response.code}")

        val bodyString = response.body?.string() ?: throw Throwable("Empty response body")
        val releases = JsonParser.parseString(bodyString).asJsonArray.also {
            if (it.size() == 0) throw Throwable("No releases found")
        }

        val latestRelease = releases.get(0).asJsonObject
        val latestVersion = latestRelease.getAsJsonPrimitive("tag_name").asString

        if (latestVersion.removePrefix("v") == BuildConfig.VERSION_NAME) return@runCatching null

        LatestRelease(
            versionName = latestVersion,
            releaseUrl = "https://github.com/bocajthomas/SE-Extended/releases/latest"
        )
    }.onFailure {
        AbstractLogger.directError("Failed to fetch latest release", it)
    }.getOrNull()

    private fun fetchLatestDebugCI() = runCatching {
        val actionRuns = client.newCall(
            Request.Builder()
                .url("https://api.github.com/repos/bocajthomas/SE-Extended/actions/runs?event=workflow_dispatch&branch=dev")
                .build()
        ).execute().use {
            if (!it.isSuccessful) throw Throwable("Failed to fetch CI runs: ${it.code}")
            val bodyString = it.body?.string() ?: throw Throwable("Empty CI body")
            JsonParser.parseString(bodyString).asJsonObject
        }

        val debugRuns = actionRuns.getAsJsonArray("workflow_runs")?.mapNotNull { it.asJsonObject }?.filter { run ->
            run.get("conclusion")?.takeIf { it.isJsonPrimitive }?.asString == "success" &&
                    run.getAsJsonPrimitive("path")?.asString == ".github/workflows/debug.yml"
        } ?: throw Throwable("No debug CI runs found")

        val latestRun = debugRuns.firstOrNull() ?: throw Throwable("No debug CI runs found")
        val headSha = latestRun.getAsJsonPrimitive("head_sha")?.asString ?: throw Throwable("No head sha found")

        if (headSha == BuildConfig.GIT_HASH) return@runCatching null

        val arch = System.getProperty("os.arch") ?: "arm64-v8a"
        val apkName = if (arch.contains("64", ignoreCase = true)) {
            "SE-Extended-ARMV8-Debug.zip"
        } else {
            "SE-Extended-ARMV7-Debug.zip"
        }

        LatestRelease(
            versionName = headSha.take(7) + "-debug",
            releaseUrl = (latestRun.getAsJsonPrimitive("html_url")?.asString?.replace("github.com", "nightly.link") ?: return@runCatching null) + "/$apkName"
        )
    }.onFailure {
        AbstractLogger.directError("Failed to fetch latest debug CI", it)
    }.getOrNull()

    val latestRelease: LatestRelease? get() = cachedRelease

    fun checkForUpdates(context: Context, isManual: Boolean = false) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val update = getLatestUpdate()
                if (update != null) {
                    UpdateNotification.showNotification(context, update)
                    UpdateNotification.updateNotiRepeater(context, force = false)
                }
            } catch (e: Exception) {
                if (isManual) {
                    AbstractLogger.directError("Manual Update Check Failed", e)
                }
            }
        }
    }

    fun fetchUpdateInBackground(onResult: (LatestRelease?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val update = getLatestUpdate()
                withContext(Dispatchers.Main) {
                    onResult(update)
                }
            } catch (e: Exception) {
                //stops blocking ui thread
                withContext(Dispatchers.Main) {
                    onResult(cachedRelease)
                }
            }
        }
    }
}