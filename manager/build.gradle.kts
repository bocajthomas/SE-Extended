import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.compose.compiler)
    id("kotlin-parcelize")
}

android {
    namespace = rootProject.ext["applicationId"].toString() + ".manager"
    compileSdk = 36

    androidResources {
        noCompress += ".so"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("String", "APPLICATION_ID", "\"${rootProject.ext["applicationId"]}\"")
        applicationId = rootProject.ext["applicationId"].toString() + ".manager"
        versionCode = 1
        versionName = "1.0.0"
        minSdk = 28
        targetSdk = 36
        multiDexEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles += file("proguard-rules.pro")
        }
        debug {
            (properties["debug_flavor"] == null).also {
                isDebuggable = !it
                isMinifyEnabled = it
                isShrinkResources = it
            }
            proguardFiles += file("proguard-rules.pro")
        }
    }

    applicationVariants.all {
        outputs.map { it as BaseVariantOutputImpl }.forEach { outputVariant ->
            outputVariant.outputFileName = "Manager.apk"
        }
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

configurations {
    all {
        resolutionStrategy {
            exclude(group = "com.google.guava", module = "listenablefuture")
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(libs.libsu)
    implementation(libs.guava)
    implementation(libs.apksig)
    implementation(libs.dexlib2)
    implementation(libs.gson)
    implementation(libs.jsoup)
    implementation(libs.okhttp)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material3)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.ripple)
    implementation(libs.androidx.material.icons.extended)
}
