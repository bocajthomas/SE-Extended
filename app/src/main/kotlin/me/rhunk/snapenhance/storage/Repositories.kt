package me.rhunk.snapenhance.storage

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.rhunk.snapenhance.common.data.RepositoryIndex
import me.rhunk.snapenhance.common.util.ktx.getStringOrNull
import me.rhunk.snapenhance.ui.manager.data.BUILT_IN_REPOSITORIES
import okhttp3.OkHttpClient
import okhttp3.Request

fun AppDatabase.getRepositories(): List<String> {
    return runBlocking(executor.asCoroutineDispatcher()) {
        database.rawQuery("SELECT url FROM repositories", null).use { cursor ->
            val repos = mutableListOf<String>()
            while (cursor.moveToNext()) {
                repos.add(cursor.getStringOrNull("url") ?: continue)
            }
            repos
        }
    }
}

fun AppDatabase.removeRepo(url: String) {
    runBlocking(executor.asCoroutineDispatcher()) {
        database.delete("repositories", "url = ?", arrayOf(url))
    }
}

fun AppDatabase.addRepo(url: String) {
    val values = ContentValues().apply { put("url", url) }
    database.insertWithOnConflict( "repositories", null, values, SQLiteDatabase.CONFLICT_IGNORE )
}

fun AppDatabase.initBuiltInRepos() {
    val okHttpClient = OkHttpClient()
    val gson = context.gson

    CoroutineScope(executor.asCoroutineDispatcher()).launch {
        BUILT_IN_REPOSITORIES.forEach { url ->
            var modifiedUrl = url
            if (url.startsWith("https://github.com/")) {
                val splitUrl = modifiedUrl.removePrefix("https://github.com/").split("/")
                val repoName = splitUrl[0] + "/" + splitUrl[1]

                runCatching {
                    okHttpClient.newCall(
                        Request.Builder().url("https://api.github.com/repos/$repoName").build()
                    ).execute().use { response ->
                        if (!response.isSuccessful) {
                            context.log.warn("Failed to fetch default branch for built-in repo $url: ${response.code}")
                            return@forEach
                        }
                        val json = response.body.string()
                        val defaultBranch = gson.fromJson(json, Map::class.java)["default_branch"] as String
                        context.log.info("Resolved default branch for $repoName: $defaultBranch")
                        modifiedUrl = "https://raw.githubusercontent.com/$repoName/$defaultBranch/"
                    }
                }.onFailure {
                    context.log.error("Failed to resolve built-in GitHub URL: $url", it)
                    return@forEach
                }
            }

            val indexUri = modifiedUrl.toUri().buildUpon().appendPath("index.json").build()

            runCatching {
                okHttpClient.newCall(
                    Request.Builder().url(indexUri.toString()).build()
                ).execute().use { response ->
                    if (!response.isSuccessful) {
                        context.log.warn("Failed to fetch index for built-in repo $indexUri: ${response.code}")
                        return@forEach
                    }
                    val repoIndex = gson.fromJson(response.body.charStream(), RepositoryIndex::class.java).also {
                        context.log.info("Validated built-in repository index: $it")
                    }
                    addRepo(modifiedUrl)
                }
            }.onFailure {
                context.log.error("Failed to validate or parse built-in repository index: $indexUri", it)
            }
        }
    }
}