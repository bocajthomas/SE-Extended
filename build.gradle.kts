// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.rust.android) apply false
}

var versionName = "2.2.0"
var versionCode = 220

rootProject.ext.set("appVersionName", versionName)
rootProject.ext.set("appVersionCode", versionCode)
rootProject.ext.set("applicationId", "me.rhunk.snapenhance")
rootProject.ext.set("buildHash", properties["debug_build_hash"] ?: java.security.SecureRandom().nextLong(Long.MAX_VALUE / 1000L, Long.MAX_VALUE).toString(16))

tasks.register("getVersion") {
    doLast {
        val versionFile = File("app/build/version.txt")
        versionFile.parentFile.mkdirs()
        if (!versionFile.exists()) {
            versionFile.createNewFile()
        }
        versionFile.writeText(versionName)
    }
}
