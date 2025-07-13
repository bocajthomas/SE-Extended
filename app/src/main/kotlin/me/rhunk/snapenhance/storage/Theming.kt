package me.rhunk.snapenhance.storage

import android.content.ContentValues
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import me.rhunk.snapenhance.common.data.DatabaseTheme
import me.rhunk.snapenhance.common.data.DatabaseThemeContent
import me.rhunk.snapenhance.common.util.ktx.getIntOrNull
import me.rhunk.snapenhance.common.util.ktx.getStringOrNull


fun AppDatabase.getThemeList(): List<DatabaseTheme> {
    return runBlocking(executor.asCoroutineDispatcher()) {
        database.rawQuery("SELECT * FROM themes ORDER BY id DESC", null).use { cursor ->
            val themes = mutableListOf<DatabaseTheme>()
            while (cursor.moveToNext()) {
                themes.add(
                    DatabaseTheme(
                        id = cursor.getIntOrNull("id") ?: continue,
                        enabled = cursor.getIntOrNull("enabled") == 1,
                        name = cursor.getStringOrNull("name") ?: continue,
                        description = cursor.getStringOrNull("description"),
                        version = cursor.getStringOrNull("version"),
                        author = cursor.getStringOrNull("author"),
                        updateUrl = cursor.getStringOrNull("updateUrl")
                    )
                )
            }
            themes
        }
    }
}

fun AppDatabase.getThemeInfo(id: Int): DatabaseTheme? {
    return runBlocking(executor.asCoroutineDispatcher()) {
        database.rawQuery("SELECT * FROM themes WHERE id = ?", arrayOf(id.toString())).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            DatabaseTheme(
                id = cursor.getIntOrNull("id") ?: return@use null,
                enabled = cursor.getIntOrNull("enabled") == 1,
                name = cursor.getStringOrNull("name") ?: return@use null,
                description = cursor.getStringOrNull("description"),
                version = cursor.getStringOrNull("version"),
                author = cursor.getStringOrNull("author"),
                updateUrl = cursor.getStringOrNull("updateUrl")
            )
        }
    }
}

fun AppDatabase.getThemeIdByUpdateUrl(updateUrl: String): Int? {
    return runBlocking(executor.asCoroutineDispatcher()) {
        database.rawQuery("SELECT id FROM themes WHERE updateUrl = ?", arrayOf(updateUrl)).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            cursor.getIntOrNull("id")
        }
    }
}

fun AppDatabase.addOrUpdateTheme(theme: DatabaseTheme, themeId: Int? = null): Int {
    return runBlocking(executor.asCoroutineDispatcher()) {
        val contentValues = ContentValues().apply {
            put("enabled", if (theme.enabled) 1 else 0)
            put("name", theme.name)
            put("description", theme.description)
            put("version", theme.version)
            put("author", theme.author)
            put("updateUrl", theme.updateUrl)
        }
        if (themeId != null) {
            database.update("themes", contentValues, "id = ?", arrayOf(themeId.toString()))
            return@runBlocking themeId
        }
        database.insert("themes", null, contentValues).toInt()
    }
}

fun AppDatabase.setThemeState(id: Int, enabled: Boolean) {
    runBlocking(executor.asCoroutineDispatcher()) {
        database.update("themes", ContentValues().apply {
            put("enabled", if (enabled) 1 else 0)
        }, "id = ?", arrayOf(id.toString()))
    }
}

fun AppDatabase.deleteTheme(id: Int) {
    runBlocking(executor.asCoroutineDispatcher()) {
        database.delete("themes", "id = ?", arrayOf(id.toString()))
    }
}


fun AppDatabase.getThemeContent(id: Int): DatabaseThemeContent? {
    return runBlocking(executor.asCoroutineDispatcher()) {
        database.rawQuery("SELECT content FROM themes WHERE id = ?", arrayOf(id.toString())).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            runCatching {
                context.gson.fromJson(cursor.getStringOrNull("content"), DatabaseThemeContent::class.java)
            }.getOrNull()
        }
    }
}


fun AppDatabase.getEnabledThemesContent(): List<DatabaseThemeContent> {
    return runBlocking(executor.asCoroutineDispatcher()) {
        database.rawQuery("SELECT content FROM themes WHERE enabled = 1", null).use { cursor ->
            val themes = mutableListOf<DatabaseThemeContent>()
            while (cursor.moveToNext()) {
                runCatching {
                    themes.add(context.gson.fromJson(cursor.getStringOrNull("content"), DatabaseThemeContent::class.java))
                }
            }
            themes
        }
    }
}


fun AppDatabase.setThemeContent(id: Int, content: DatabaseThemeContent) {
    runBlocking(executor.asCoroutineDispatcher()) {
        database.update("themes", ContentValues().apply {
            put("content", context.gson.toJson(content))
        }, "id = ?", arrayOf(id.toString()))
    }
}