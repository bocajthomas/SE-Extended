import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.ByteArrayOutputStream

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.compose.compiler)
    id("kotlin-parcelize")
}

android {
    namespace = rootProject.ext["applicationId"].toString() + ".common"
    compileSdk = 36

    buildFeatures {
        aidl = true
        buildConfig = true
        compose = true
    }

    defaultConfig {
        minSdk = 28
        buildConfigField("String", "VERSION_NAME", "\"${rootProject.ext["appVersionName"]}\"")
        buildConfigField("int", "VERSION_CODE", "${rootProject.ext["appVersionCode"]}")
        buildConfigField("String", "APPLICATION_ID", "\"${rootProject.ext["applicationId"]}\"")
        buildConfigField("long", "BUILD_TIMESTAMP", "${System.currentTimeMillis()}L")
        buildConfigField("String", "BUILD_HASH", "\"${rootProject.ext["buildHash"]}\".toString()")
        val gitHash = ByteArrayOutputStream()
        exec {
            commandLine("git", "rev-parse", "HEAD")
            standardOutput = gitHash
        }
        buildConfigField("String", "GIT_HASH", "\"${gitHash.toString(Charsets.UTF_8).trim()}\"")
        buildConfigField("String", "SIF_ENDPOINT", "\"${properties["debug_sif_endpoint"]?.toString() ?: "https://github.com/SnapEnhance/resources/raw/refs/heads/main/sif"}\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.addAll(
                "-XXLanguage:+PropertyParamAnnotationDefaultTargetMode",
                "-Xjvm-default=all"
            )
        }
    }
}

dependencies {
    implementation(libs.coroutines)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.androidx.documentfile)
    implementation(libs.rhino)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.androidx.activity.compose)
    implementation(libs.rhino.android) {
        exclude(group = "org.mozilla", module = "rhino-runtime")
    }
    implementation(libs.androidx.compose.foundation)

    compileOnly(libs.androidx.activity.ktx)
    compileOnly(platform(libs.androidx.compose.bom))
    compileOnly(libs.androidx.navigation.compose)
    compileOnly(libs.androidx.material.icons.core)
    compileOnly(libs.androidx.material.ripple)
    compileOnly(libs.androidx.material.icons.extended)
    compileOnly(libs.androidx.material3)

    implementation(project(":mapper"))
}