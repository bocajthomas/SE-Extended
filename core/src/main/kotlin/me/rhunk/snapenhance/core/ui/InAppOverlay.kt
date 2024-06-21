package me.rhunk.snapenhance.core.ui

import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import me.rhunk.snapenhance.common.ui.AppMaterialTheme
import me.rhunk.snapenhance.common.ui.createComposeView
import me.rhunk.snapenhance.common.util.ktx.copyToClipboard
import me.rhunk.snapenhance.core.ModContext
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.Hooker
import me.rhunk.snapenhance.core.util.ktx.isDarkTheme
import kotlin.math.roundToInt
import kotlin.random.Random

class InAppOverlay(
    private val context: ModContext
) {
    companion object {
        fun showCrashOverlay(content: String, throwable: Throwable? = null) {
            Hooker.ephemeralHook(Activity::class.java, "onPostCreate", HookStage.AFTER) { param ->
                val contentView = param.thisObject<Activity>().findViewById<FrameLayout>(android.R.id.content)
                contentView.children().forEach { it.visibility = View.GONE }
                lateinit var screenView: View
                screenView = createComposeView(param.thisObject()) {
                    AppMaterialTheme(isDarkTheme = true) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "SnapEnhance",
                                        fontSize = 28.sp
                                    )
                                    Spacer(modifier = Modifier.height(40.dp))
                                    Text(
                                        text = content,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(40.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        throwable?.let {
                                            Button(onClick = {
                                                contentView.context.copyToClipboard(it.stackTraceToString())
                                            }) {
                                                Text("Copy error to clipboard")
                                            }
                                        }
                                        Button(onClick = {
                                            contentView.children().forEach { it.visibility = View.VISIBLE }
                                            contentView.removeView(screenView)
                                        }) {
                                            Text("Ignore")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }.apply {
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                }
                contentView.addView(screenView)
            }
        }
    }

    inner class Toast(
        val composable: @Composable Toast.() -> Unit,
        val durationMs: Int
    ) {
        var shown by mutableStateOf(false)
        var visible by mutableStateOf(false)
    }

    private val toasts = mutableStateListOf<Toast>()

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun OverlayContent() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            toasts.forEach { toast ->
                val animation by animateFloatAsState(
                    targetValue = if (toast.visible) 1f else 0f,
                    animationSpec = if (toast.visible) tween(durationMillis = 150) else tween(durationMillis = 300),
                    label = "toast"
                )

                LaunchedEffect(toast) {
                    toast.visible = true
                    if (toast.durationMs < 0) return@LaunchedEffect
                    delay(toast.durationMs.toLong())
                    toast.visible = false
                    delay(1000)
                    toast.shown = true
                    synchronized(toasts) {
                        if (toasts.isNotEmpty() && toasts.all { it.shown }) toasts.clear()
                    }
                }

                val deviceWidth = LocalContext.current.resources.displayMetrics.widthPixels
                val draggableState = remember {
                    AnchoredDraggableState(
                        initialValue = 0,
                        positionalThreshold = { distance: Float -> distance * 0.5f },
                        velocityThreshold = { deviceWidth / 2f },
                        animationSpec = tween(),
                        confirmValueChange = {
                            if (it == 0) return@AnchoredDraggableState true
                            toast.visible = false
                            true
                        }
                    ).apply {
                        updateAnchors(
                            DraggableAnchors {
                                -1 at -deviceWidth.toFloat()
                                0 at 0f
                                1 at deviceWidth.toFloat()
                            }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .anchoredDraggable(draggableState, Orientation.Horizontal)
                        .offset { IntOffset(draggableState.offset.roundToInt(), 0) }
                        .graphicsLayer {
                            alpha = animation
                            translationY = -100.dp.toPx() * (1 - animation)
                        }
                ) {
                    if (animation > 0.01f) {
                        toast.composable(toast)
                    }
                }
            }
        }
    }

    private val overlayTag = Random.nextLong()

    private fun injectOverlay(activity: Activity) {
        val root = activity.findViewById<FrameLayout>(android.R.id.content)
        activity.runOnUiThread {
            if (root.findViewWithTag<View>(overlayTag) != null) return@runOnUiThread
            root.addView(createComposeView(activity) {
                AppMaterialTheme(isDarkTheme = remember { activity.isDarkTheme() }) {
                    OverlayContent()
                }
            }.apply {
                tag = overlayTag
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            })
        }
    }

    fun onActivityCreate(activity: Activity) {
        injectOverlay(activity)
    }

    @Composable
    private fun DurationProgress(
        duration: Int,
        modifier: Modifier = Modifier
    ) {
        val progress = remember { Animatable(1f) }

        LaunchedEffect(Unit) {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = duration, easing = LinearEasing)
            )
        }

        LinearProgressIndicator(
            progress = { progress.value },
            modifier = modifier
        )
    }

    fun showStatusToast(
        icon: ImageVector,
        text: String,
        durationMs: Int = 2000,
        showDuration: Boolean = true,
    ) {
        showToast(
            icon = { Icon(icon, contentDescription = "icon", modifier = Modifier.size(32.dp)) },
            text = {
                Text(text, modifier = Modifier.fillMaxWidth(), maxLines = 2, overflow = TextOverflow.Ellipsis)
            },
            durationMs = durationMs,
            showDuration = showDuration
        )
    }

    private fun showToast(
        icon: @Composable () -> Unit = {
            Icon(Icons.Outlined.Warning, contentDescription = "icon", modifier = Modifier.size(32.dp))
        },
        text: @Composable () -> Unit = {},
        durationMs: Int = 3000,
        showDuration: Boolean = true,
    ) {
        injectOverlay(context.mainActivity!!)
        toasts.add(Toast(
            composable = {
                ElevatedCard(
                    modifier = Modifier
                        .padding(16.dp)
                        .shadow(8.dp, RoundedCornerShape(8.dp))
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        icon()
                        text()
                    }
                    if (showDuration && durationMs > 0) {
                        DurationProgress(duration = durationMs, modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            durationMs = durationMs
        ))
    }
}