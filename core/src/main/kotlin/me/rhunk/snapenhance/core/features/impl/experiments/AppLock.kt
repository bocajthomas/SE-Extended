package me.rhunk.snapenhance.core.features.impl.experiments

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.Shape
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import me.rhunk.snapenhance.common.Constants
import me.rhunk.snapenhance.common.ui.theme.AppMaterialTheme
import me.rhunk.snapenhance.common.ui.createComposeView
import me.rhunk.snapenhance.core.event.events.impl.ActivityResultEvent
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.ui.addForegroundDrawable
import me.rhunk.snapenhance.core.ui.children
import me.rhunk.snapenhance.core.ui.removeForegroundDrawable
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import kotlin.random.Random

class AppLock : Feature("AppLock") {
    private var isUnlockRequested = false

    private val rootContentView get() = context.mainActivity!!.findViewById<FrameLayout>(android.R.id.content)
    private val requestCode = Random.nextInt(100, 65535)

    private fun hideRootView() {
        rootContentView.addForegroundDrawable("locked_overlay", ShapeDrawable(object: Shape() {
            override fun draw(canvas: Canvas, paint: Paint) {
                paint.color = 0xFF000000.toInt()
                canvas.drawRect(0F, 0F, canvas.width.toFloat(), canvas.height.toFloat(), paint)
            }
        }))
    }

    private fun requestUnlock() {
        isUnlockRequested = true
        context.mainActivity!!.startActivityForResult(Intent().apply {
            component = ComponentName(Constants.SE_PACKAGE_NAME, "me.rhunk.snapenhance.bridge.BiometricPromptActivity")
        }, requestCode)
    }

    private fun lock(prompt: Boolean = true) {
        isUnlockRequested = true
        hideRootView()

        val lockedView = rootContentView.findViewWithTag<View>("locked_view") ?: createComposeView(rootContentView.context) {
            AppMaterialTheme(preferences = context.androidContext.getSharedPreferences("selected_theme", 0)) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Image(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "Lock",
                                modifier = Modifier.size(100.dp),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                            )
                            Button(onClick = {
                                requestUnlock()
                            }) {
                                Text(remember { context.translation["biometric_auth.unlock_button"] })
                            }
                        }
                    }
                }
            }
        }.apply {
            tag = "locked_view"
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            rootContentView.addView(this)
        }

        rootContentView.postDelayed({
            rootContentView.children().forEach { it.visibility = View.GONE }
            lockedView.visibility = View.VISIBLE
            rootContentView.removeForegroundDrawable("locked_overlay")
        }, 500)

        if (prompt) {
            requestUnlock()
        }
    }

    private fun unlock() {
        rootContentView.apply {
            removeForegroundDrawable("locked_overlay")
            children().forEach { it.visibility = View.VISIBLE }
            visibility = View.VISIBLE
            findViewWithTag<View>("locked_view")?.visibility = View.GONE
            postDelayed({
                isUnlockRequested = false
            }, 1000)
        }
    }

    override fun init() {
        if (context.config.experimental.appLock.globalState != true) return

        onNextActivityCreate {
            Activity::class.java.apply {
                if (context.config.experimental.appLock.lockOnResume.get()) {
                    hook("onResume", HookStage.BEFORE) { param ->
                        if (param.thisObject<Activity>().packageName != Constants.SNAPCHAT_PACKAGE_NAME) return@hook
                        if (isUnlockRequested) return@hook
                        lock(prompt = true)
                    }
                    hook("onPause", HookStage.BEFORE) { param ->
                        if (param.thisObject<Activity>().packageName != Constants.SNAPCHAT_PACKAGE_NAME) return@hook
                        if (isUnlockRequested) return@hook
                        hideRootView()
                    }
                }
            }

            context.event.subscribe(ActivityResultEvent::class) { event ->
                if (event.requestCode != requestCode) return@subscribe
                if (event.resultCode == Activity.RESULT_OK) {
                    unlock()
                    return@subscribe
                }
                lock(prompt = false)
            }
            lock()
        }
    }
}