package me.rhunk.snapenhance.common.scripting.type

import java.io.BufferedReader

data class ModuleInfo(
    val name: String,
    val version: String,
    val displayName: String? = null,
    val description: String? = null,
    val updateUrl: String? = null,
    val author: String? = null,
    val minSnapchatVersion: Long? = null,
    val minSEVersion: Long? = null,
    val grantedPermissions: List<String>,
    val executionSides: List<String>? = null,
    val note: String? = null,
    val notice: List<String>,
    val changelogUrl: String? = null,
    val howToUseUrl: String? = null,

) {
    fun ensurePermissionGranted(permission: Permissions) {
        if (!grantedPermissions.contains(permission.key)) {
            throw AssertionError("Permission $permission is not granted")
        }
    }
}

fun BufferedReader.readModuleInfo(): ModuleInfo {
    val header = readLine()
    if (!header.startsWith("// ==SE_module==")) {
        throw Exception("Invalid module header")
    }

    val properties = mutableMapOf<String, String>()
    while (true) {
        val line = readLine()
        if (line.startsWith("// ==/SE_module==")) {
            break
        }
        val split = line.replaceFirst("//", "").split(":", limit = 2)
        if (split.size != 2) {
            throw Exception("Invalid module property")
        }
        properties[split[0].trim()] = split[1].trim()
    }

    return ModuleInfo(
        name = properties["name"]?.also {
            if (!it.matches(Regex("[a-z_]+"))) {
                throw Exception("Invalid module name : Only lowercase letters and underscores are allowed")
            }
        } ?: throw Exception("Missing module name"),
        version = properties["version"] ?: throw Exception("Missing module version"),
        displayName = properties["displayName"],
        description = properties["description"],
        updateUrl = properties["updateUrl"],
        author = properties["author"] ?: throw Exception("Missing module author"),
        minSnapchatVersion = properties["minSnapchatVersion"]?.toLongOrNull(),
        minSEVersion = properties["minSEVersion"]?.toLongOrNull(),
        grantedPermissions = properties["permissions"]?.split(",")?.map { it.trim() } ?: emptyList(),
        executionSides = properties["executionSides"]?.lowercase()?.split(",")?.map { it.trim() },
        note = properties["note"],
        notice = properties["notice"]?.split(",")?.map { it.trim() } ?: emptyList(),
        changelogUrl = properties["changelogUrl"],
        howToUseUrl = properties["howToUseUrl"],
    )
}
