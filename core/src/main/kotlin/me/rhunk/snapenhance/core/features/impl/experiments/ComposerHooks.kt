package me.rhunk.snapenhance.core.features.impl.experiments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.common.bridge.FileHandleScope
import me.rhunk.snapenhance.common.bridge.toWrapper
import me.rhunk.snapenhance.common.ui.createComposeAlertDialog
import me.rhunk.snapenhance.core.SnapEnhance
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.features.impl.downloader.MediaDownloader
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.Hooker
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.wrapper.impl.composer.ComposerMarshaller
import me.rhunk.snapenhance.nativelib.NativeLib
import java.lang.reflect.Proxy
import kotlin.math.absoluteValue
import kotlin.random.Random

class ComposerHooks: Feature("ComposerHooks") {
    private val config by lazy { context.config.experimental.nativeHooks.composerHooks }
    private val getImportsFunctionName = Random.nextLong().absoluteValue.toString(16)

    private val composerConsole by lazy {
        createComposeAlertDialog(context.mainActivity!!) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                var result by remember { mutableStateOf("") }
                var codeContent by remember { mutableStateOf("return 1 + 2") }

                Text("Composer Console", fontSize = 18.sp, fontWeight = FontWeight.Bold)

                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle.Default.copy(fontSize = 12.sp),
                    value = codeContent,
                    placeholder = { Text("Enter your JS code here:") },
                    onValueChange = {
                        codeContent = it
                    }
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        context.log.verbose("input: $codeContent", "ComposerConsole")
                        result = "Running..."
                        context.coroutineScope.launch {
                            result = (context.native.composerEval("""
                                (() => {
                                    try {
                                        $codeContent
                                    } catch (e) {
                                        return e.toString()
                                    }
                                })()
                            """.trimIndent()) ?: "(no result)").also {
                                context.log.verbose("result: $it", "ComposerConsole")
                            }
                        }
                    }
                ) {
                    Text("Run")
                }

                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(result)
                }
            }
        }
    }

    private fun newComposerFunction(block: ComposerMarshaller.() -> Boolean): Any? {
        val composerFunctionClass = findClass("com.snap.composer.callable.ComposerFunction")
        return Proxy.newProxyInstance(
            composerFunctionClass.classLoader,
            arrayOf(composerFunctionClass)
        ) { _, method, args ->
            if (method.name != "perform") return@newProxyInstance null
            block(ComposerMarshaller(args?.get(0) ?: return@newProxyInstance false))
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun init() {
        if (config.globalState != true) return

        val importedFunctions = mutableMapOf<String, Any?>()

        fun composerFunction(name: String, block: ComposerMarshaller.() -> Unit) {
            importedFunctions[name] = newComposerFunction {
                block(this)
                true
            }
        }

        composerFunction("getConfig") {
            pushUntyped(mapOf<String, Any>(
                "operaDownloadButton" to context.config.downloader.operaDownloadButton.get(),
                "bypassCameraRollLimit" to config.bypassCameraRollLimit.get(),
                "showFirstCreatedUsername" to config.showFirstCreatedUsername.get(),
                "composerLogs" to config.composerLogs.get(),
                "customSelfDestructSnapDelay" to config.customSelfDestructSnapDelay.get(),
            ))
        }

        composerFunction("showToast") {
            if (getSize() < 1) return@composerFunction
            context.shortToast(getUntyped(0) as? String ?: return@composerFunction)
        }

        composerFunction("downloadLastOperaMedia") {
            context.feature(MediaDownloader::class).downloadLastOperaMediaAsync(getUntyped(0) == true)
        }

        composerFunction("getFriendOriginalUsername") {
            if (getSize() < 1) return@composerFunction
            val username = getUntyped(0) as? String ?: return@composerFunction

            runCatching {
                pushUntyped(context.database.getFriendOriginalUsername(username))
            }.onFailure {
                pushUntyped(null)
            }
        }

        composerFunction("log") {
            if (getSize() < 2) return@composerFunction
            val logLevel = getUntyped(0) as? String ?: return@composerFunction
            val message = getUntyped(1) as? String ?: return@composerFunction

            val tag = "ComposerLogs"

            when (logLevel) {
                "log" -> context.log.verbose(message, tag)
                "debug" -> context.log.debug(message, tag)
                "info" -> context.log.info(message, tag)
                "warn" -> context.log.warn(message, tag)
                "error" -> context.log.error(message, tag)
            }
        }

        fun loadHooks() {
            if (!NativeLib.initialized) {
                context.log.error("ComposerHooks cannot be loaded without NativeLib")
                return
            }
            val loaderScript = runCatching {
                context.fileHandlerManager.getFileHandle(FileHandleScope.COMPOSER.key, "loader.js").toWrapper().readBytes().toString(Charsets.UTF_8)
            }.onFailure {
                context.log.error("Failed to load composer loader script", it)
            }.getOrNull() ?: return
            context.native.setComposerLoader("""
                const i = setInterval(() => {
                    try {
                        const _runtimeName = "${if (SnapEnhance.classCache.nativeBridge.name == "com.snapchat.client.valdi.NativeBridge") "valdi" else "composer"}";
                        require(_runtimeName + '_core/src/DeviceBridge').getDisplayWidth();
                        clearInterval(i);
                        (() => { const _getImportsFunctionName = "$getImportsFunctionName"; $loaderScript })();
                    } catch (e) {}
                }, 200)
            """.trimIndent().trim())
        }

        loadHooks()

        if (config.composerConsole.get()) {
            context.inAppOverlay.addCustomComposable {
                FilledIconButton(
                    onClick = {
                        composerConsole.show()
                    },
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 100.dp, end = 16.dp)
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = "Debug Console")
                }
            }
        }

        SnapEnhance.classCache.nativeBridge.hook("registerNativeModuleFactory", HookStage.BEFORE) { param ->
            val moduleFactory = param.argNullable<Any>(1) ?: return@hook
            if (moduleFactory.javaClass.getMethod("getModulePath").invoke(moduleFactory)?.toString()?.contains("DeviceBridge") != true) return@hook
            Hooker.ephemeralHookObjectMethod(moduleFactory.javaClass, moduleFactory, "loadModule", HookStage.AFTER) { methodParam ->
                val result = methodParam.getResult() as? MutableMap<String, Any?> ?: return@ephemeralHookObjectMethod
                result[getImportsFunctionName] = newComposerFunction {
                    pushUntyped(importedFunctions)
                    true
                }
            }
        }
    }
}