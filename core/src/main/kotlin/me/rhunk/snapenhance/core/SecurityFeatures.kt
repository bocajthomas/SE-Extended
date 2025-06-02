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
import me.rhunk.snapenhance.common.bridge.InternalFileHandleType
import me.rhunk.snapenhance.common.bridge.toWrapper
import me.rhunk.snapenhance.common.config.MOD_DETECTION_VERSION_CHECK
import me.rhunk.snapenhance.common.config.VersionRequirement
import me.rhunk.snapenhance.common.ui.createComposeView
import me.rhunk.snapenhance.core.ui.CustomComposable

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

    private fun isSafeMode(): Boolean {
        val snapchatVersionCode = context.androidContext.packageManager?.getPackageInfo(context.androidContext.packageName, 0)?.longVersionCode ?: throw IllegalStateException("Failed to get version code")
        val shouldUseSafeMode = MOD_DETECTION_VERSION_CHECK.checkVersion(snapchatVersionCode)?.second == VersionRequirement.OLDER_REQUIRED // true if version is >12.81.0.44

        context.config.experimental.nativeHooks.customSharedLibrary.get().takeIf { it.isNotEmpty() }?.let {
            runCatching {
                context.native.loadSharedLibrary(
                    context.fileHandlerManager.getFileHandle(FileHandleScope.USER_IMPORT.key, it).toWrapper().readBytes()
                )
                context.log.verbose("loaded custom shared library")
            }.onFailure {
                context.log.error("Failed to load custom shared library", it)
                return true
            }
        } ?: context.bridgeClient.getDebugProp("enable_security_features", "false").takeIf { it == "true" }?.runCatching {
            context.native.loadSharedLibrary(
                context.fileHandlerManager.getFileHandle(FileHandleScope.INTERNAL.key, InternalFileHandleType.SIF.key)
                    .toWrapper()
                    .readBytes()
                    .takeIf {
                        it.isNotEmpty()
                    } ?: throw IllegalStateException("Binary is empty")
            )
            context.log.verbose("loaded sif")
        }?.onFailure {
            context.log.error("Failed to load sif: " + it.message)
            return shouldUseSafeMode
        } ?: context.log.warn("Security features are disabled")

        token // pre init token

        val status = getStatus()
        val safeMode = shouldUseSafeMode && (status == null || status < 2)

        if (status != null && status >= 2) {
            context.log.verbose("status=$status")
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
        }

        return safeMode
    }

    fun init() {
        context.isSafeMode = isSafeMode()
        context.log.verbose("isSafeMode=${context.isSafeMode}")
        if (!context.isSafeMode) return

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
                                        "SnapEnhance can't be used to login or signup because your Snapchat version isn't the recommended one (v${MOD_DETECTION_VERSION_CHECK.maxVersion?.first ?: "0.0.0"}). Please downgrade to continue using it.\n\nFor more details, join t.me/snapenhance_chat",
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