import org.apache.tools.ant.taskdefs.condition.Os
plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
}

android {
    namespace = rootProject.ext["applicationId"].toString() + ".valdi"
    compileSdk = 36

    sourceSets {
        getByName("main") {
            assets.srcDirs("build/assets")
        }
    }
}

task("compileTypeScript") {
    doLast {
        if (Os.isFamily(Os.FAMILY_WINDOWS))  {
            project.exec {
                commandLine("npx.cmd", "--yes", "tsc", "--project", "tsconfig.json")
            }
            project.exec {
                commandLine("npx.cmd", "--yes", "rollup", "--config", "rollup.config.js", "--bundleConfigAsCjs")
            }
        } else {
            project.exec {
                commandLine("npx", "--yes", "tsc", "--project", "tsconfig.json")
            }
            project.exec {
                commandLine("npx", "--yes", "rollup", "--config", "rollup.config.js", "--bundleConfigAsCjs")
            }
        }

        project.copy {
            from("build/loader.js")
            into("build/assets/valdi")
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn("compileTypeScript")
}