package me.rhunk.snapenhance.core

import android.system.Os
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.NotInterested
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import me.rhunk.snapenhance.common.bridge.FileHandleScope
import me.rhunk.snapenhance.common.bridge.toWrapper
import me.rhunk.snapenhance.common.config.MOD_DETECTION_VERSION_CHECK
import me.rhunk.snapenhance.common.config.VersionRequirement
import me.rhunk.snapenhance.common.ui.createComposeView
import me.rhunk.snapenhance.core.event.events.impl.UnaryCallEvent
import me.rhunk.snapenhance.core.ui.CustomComposable
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.hook.hookConstructor
import me.rhunk.snapenhance.core.util.ktx.getObjectField

class SecurityFeatures(
    private val context: ModContext
) {
    private fun transact(option: Int, option2: Long) = runCatching { Os.prctl(option, option2, 0, 0, 0) }.getOrNull()

    private val token by lazy {
        transact(0, 0)
    }

    private fun getStatus() = token?.run {
        transact(this, 0)?.toString(2)?.padStart(32, '0')?.count { it == '1' }
    }

    fun init() {
        val snapchatVersionCode = context.androidContext.packageManager?.getPackageInfo(context.androidContext.packageName, 0)?.longVersionCode ?: throw IllegalStateException("Failed to get version code")
        var shouldDisablePlugin = MOD_DETECTION_VERSION_CHECK.checkVersion(snapchatVersionCode)?.second == VersionRequirement.OLDER_REQUIRED

        // load user shared library
        context.config.experimental.nativeHooks.customSharedLibrary.get().takeIf { it.isNotEmpty() }?.let {
            runCatching {
                context.native.loadSharedLibrary(
                    context.fileHandlerManager.getFileHandle(FileHandleScope.USER_IMPORT.key, it).toWrapper().readBytes()
                )
                context.log.verbose("loaded custom shared library")
                shouldDisablePlugin = false

                lateinit var composable: CustomComposable
                composable = {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopCenter),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF85A947))
                    }

                    LaunchedEffect(Unit) {
                        delay(2500)
                        context.inAppOverlay.removeCustomComposable(composable)
                    }
                }

                context.inAppOverlay.addCustomComposable(composable)
            }.onFailure {
                context.log.error("Failed to load custom shared library", it)
            }
        }

        if (context.bridgeClient.getDebugProp("test_mode", "false") == "true") {
            shouldDisablePlugin = false
        }

        context.disablePlugin = shouldDisablePlugin
        context.log.verbose("disablePlugin=${context.disablePlugin}")
        if (!context.disablePlugin) return

        context.event.subscribe(UnaryCallEvent::class) { event ->
            val callOptions = event.adapter.arg<Any>(2).let { it.javaClass.getMethod("build").invoke(it) } ?: return@subscribe
            if (callOptions.getObjectField("mAttestation") != null) {
                event.canceled = true
            }
        }

        context.androidContext.classLoader.loadClass("com.snapchat.client.client_attestation.ArgosClient\$CppProxy").apply {
            hook("getArgosTokenAsync", HookStage.BEFORE) { it.setResult(null) }
            hook("getAttestationHeaders", HookStage.BEFORE) { it.setResult(null) }
        }

        context.androidContext.classLoader.loadClass("com.snap.security.attestation.impl.SCClientAttestationDurableJob")
            .hookConstructor(HookStage.BEFORE) { param ->
                param.setArg(0, null)
        }

        context.features.addActivityCreateListener { activity ->
            if (!activity.javaClass.name.endsWith("LoginSignupActivity")) return@addActivityCreateListener

            activity.findViewById<ViewGroup>(android.R.id.content).apply {
                visibility = ViewGroup.INVISIBLE

                post {
                    addView(createComposeView(activity) {
                        Surface(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Column(
                                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Icon(Icons.Rounded.NotInterested, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(110.dp))
                                    Spacer(Modifier.height(50.dp))
                                    Text(
                                        "SnapEnhance can't be used to login or signup because your Snapchat version isn't the recommended one. Please downgrade to Snapchat v${MOD_DETECTION_VERSION_CHECK.maxVersion?.first ?: "0.0.0"} or disable SnapEnhance in LSPosed to continue.\n\nFor more details, join t.me/snapenhance_chat",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }

                        LaunchedEffect(Unit) {
                            visibility = ViewGroup.VISIBLE
                        }
                    })
                }
            }
        }
    }
}