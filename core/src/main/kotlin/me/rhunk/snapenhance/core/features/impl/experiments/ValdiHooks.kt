package me.rhunk.snapenhance.core.features.impl.experiments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
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
import me.rhunk.snapenhance.core.wrapper.impl.valdi.ValdiFunction
import me.rhunk.snapenhance.core.wrapper.impl.valdi.ValdiMarshaller
import me.rhunk.snapenhance.nativelib.NativeLib
import java.lang.reflect.Proxy
import kotlin.math.absoluteValue
import kotlin.random.Random

class ValdiHooks: Feature("ValdiHooks") {
    private val config by lazy { context.config.experimental.nativeHooks.valdiHooks }
    private val getImportsFunctionName = Random.nextLong().absoluteValue.toString(16)

    private var evalFunction: ValdiFunction? = null
    private val valdiConsole by lazy {
        createComposeAlertDialog(context.mainActivity!!) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                var result by remember { mutableStateOf("") }
                var codeContent by remember { mutableStateOf("1 + 2") }

                Text("Valdi Console", fontSize = 18.sp, fontWeight = FontWeight.Bold)

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
                        context.log.verbose("input: $codeContent", "ValdiConsole")
                        result = "Running..."

                        context.coroutineScope.launch {
                            ValdiMarshaller.create()?.use { valdiMarshaller ->
                                valdiMarshaller.pushUntyped(codeContent)
                                valdiMarshaller.pushUntyped(newValdiFunction {
                                    if (getSize() < 1) return@newValdiFunction false
                                    val output = getUntyped(0)
                                    context.log.verbose("eval: $output", "ValdiConsole")

                                    result = if (output is Exception) {
                                        "${output.javaClass.simpleName}: ${output.message}"
                                    } else {
                                        output?.toString() ?: "undefined"
                                    }
                                    true
                                })

                                evalFunction?.perform(valdiMarshaller)
                            } ?: run {
                                result = "Failed to create ValdiMarshaller"
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

    private fun newValdiFunction(block: ValdiMarshaller.() -> Boolean): Any? {
        val functionClass = SnapEnhance.classCache.valdiFunction
        return Proxy.newProxyInstance(
            functionClass.classLoader,
            arrayOf(functionClass)
        ) { _, method, args ->
            if (method.name != "perform") return@newProxyInstance null
            block(ValdiMarshaller(args?.get(0) ?: return@newProxyInstance false))
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun init() {
        if (config.globalState != true) return

        val nativeBridgeClass = runCatching { findClass("com.snapchat.client.valdi.NativeBridge") }.getOrNull() ?: findClass("com.snapchat.client.composer.NativeBridge")

        val importedFunctions = mutableMapOf<String, Any?>()

        fun valdiFunction(name: String, block: ValdiMarshaller.() -> Unit) {
            importedFunctions[name] = newValdiFunction {
                block(this)
                true
            }
        }

        valdiFunction("getConfig") {
            pushUntyped(mapOf<String, Any>(
                "operaDownloadButton" to context.config.downloader.operaDownloadButton.get(),
                "bypassCameraRollLimit" to config.bypassCameraRollLimit.get(),
                "showFirstCreatedUsername" to config.showFirstCreatedUsername.get(),
                "valdiLogs" to config.valdiLogs.get(),
                "customSelfDestructSnapDelay" to config.customSelfDestructSnapDelay.get(),
            ))
        }

        valdiFunction("showToast") {
            if (getSize() < 1) return@valdiFunction
            context.shortToast(getUntyped(0) as? String ?: return@valdiFunction)
        }

        valdiFunction("downloadLastOperaMedia") {
            context.feature(MediaDownloader::class).downloadLastOperaMediaAsync(getUntyped(0) == true)
        }

        valdiFunction("getFriendOriginalUsername") {
            if (getSize() < 1) return@valdiFunction
            val username = getUntyped(0) as? String ?: return@valdiFunction

            runCatching {
                pushUntyped(context.database.getFriendOriginalUsername(username))
            }.onFailure {
                pushUntyped(null)
            }
        }

        valdiFunction("log") {
            if (getSize() < 2) return@valdiFunction
            val logLevel = getUntyped(0) as? String ?: return@valdiFunction
            val message = getUntyped(1) as? String ?: return@valdiFunction

            val tag = "ValdiLogs"

            when (logLevel) {
                "log" -> context.log.verbose(message, tag)
                "debug" -> context.log.debug(message, tag)
                "info" -> context.log.info(message, tag)
                "warn" -> context.log.warn(message, tag)
                "error" -> context.log.error(message, tag)
            }
        }

        valdiFunction("setEvalFunction") {
            if (getSize() < 1) return@valdiFunction
            evalFunction = ValdiFunction(getUntyped(0) ?: return@valdiFunction)
            context.log.verbose("Set eval function: $evalFunction", "ValdiHooks")
        }

        fun loadHooks() {
            if (!NativeLib.initialized) {
                context.log.error("ValdiHooks cannot be loaded without NativeLib")
                return
            }
            val loaderScript = runCatching {
                context.fileHandlerManager.getFileHandle(FileHandleScope.VALDI.key, "loader.js").toWrapper().readBytes().toString(Charsets.UTF_8)
            }.onFailure {
                context.log.error("Failed to load valdi loader script", it)
            }.getOrNull() ?: return
            context.native.setValdiLoader("""
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

        if (config.valdiConsole.get()) {
            context.inAppOverlay.addCustomComposable {
                FilledIconButton(
                    onClick = {
                        valdiConsole.show()
                    },
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 100.dp, end = 16.dp)
                ) {
                    Icon(Icons.Rounded.BugReport, contentDescription = "Debug Console")
                }
            }
        }

        SnapEnhance.classCache.nativeBridge.hook("registerNativeModuleFactory", HookStage.BEFORE) { param ->
            val moduleFactory = param.argNullable<Any>(1) ?: return@hook
            if (moduleFactory.javaClass.getMethod("getModulePath").invoke(moduleFactory)?.toString()?.contains("DeviceBridge") != true) return@hook
            Hooker.ephemeralHookObjectMethod(moduleFactory.javaClass, moduleFactory, "loadModule", HookStage.AFTER) { methodParam ->
                val result = methodParam.getResult() as? MutableMap<String, Any?> ?: return@ephemeralHookObjectMethod
                result[getImportsFunctionName] = newValdiFunction {
                    pushUntyped(importedFunctions)
                    true
                }
            }
        }
    }
}