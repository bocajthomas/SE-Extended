package me.rhunk.snapenhance.core.ui.menu.impl

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rhunk.snapenhance.core.features.impl.downloader.MediaDownloader
import me.rhunk.snapenhance.core.features.impl.messaging.AutoMarkAsRead
import me.rhunk.snapenhance.core.ui.children
import me.rhunk.snapenhance.core.ui.iterateParent
import me.rhunk.snapenhance.core.ui.menu.AbstractMenu
import me.rhunk.snapenhance.core.ui.triggerCloseTouchEvent
import me.rhunk.snapenhance.core.util.ktx.getDimens
import me.rhunk.snapenhance.core.util.ktx.getDrawable
import me.rhunk.snapenhance.core.util.ktx.vibrateLongPress

class OperaViewerIcons : AbstractMenu() {
    private val downloadSvgDrawable by lazy { context.resources.getDrawable("svg_download", context.androidContext.theme) }
    private val eyeSvgDrawable by lazy { context.resources.getDrawable("svg_eye_24x24", context.androidContext.theme) }
    private val actionMenuIconSize by lazy { context.resources.getDimens("action_menu_icon_size") }
    private val actionMenuIconMargin by lazy { context.resources.getDimens("action_menu_icon_margin") }
    private val actionMenuIconMarginTop by lazy { context.resources.getDimens("action_menu_icon_margin_top") }

    override fun inject(parent: ViewGroup, view: View, viewConsumer: (View) -> Unit) {
        val mediaDownloader = context.feature(MediaDownloader::class)

        if (context.config.downloader.operaDownloadButton.get()) {
            parent.addView(LinearLayout(view.context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, actionMenuIconMarginTop * 2 + actionMenuIconSize, 0, 0)
                    marginEnd = actionMenuIconMargin
                    gravity = Gravity.TOP or Gravity.END
                }
                addOnAttachStateChangeListener(object: View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        v.visibility = View.VISIBLE
                        (parent.parent as? ViewGroup)?.children()?.forEach { child ->
                            if (child !is ViewGroup) return@forEach
                            child.children().forEach {
                                if (it::class.java.name.endsWith("PreviewToolbar")) v.visibility = View.GONE
                            }
                        }
                    }

                    override fun onViewDetachedFromWindow(v: View) {}
                })

                addView(ImageView(view.context).apply {
                    setImageDrawable(downloadSvgDrawable)
                    setColorFilter(Color.WHITE)
                    layoutParams = LinearLayout.LayoutParams(
                        actionMenuIconSize,
                        actionMenuIconSize
                    ).apply {
                        setMargins(0, 0, 0, actionMenuIconMargin * 2)
                    }
                    setOnClickListener {
                        mediaDownloader.downloadLastOperaMediaAsync(allowDuplicate = false)
                    }
                    setOnLongClickListener {
                        context.vibrateLongPress()
                        mediaDownloader.downloadLastOperaMediaAsync(allowDuplicate = true)
                        true
                    }
                })
            }, 0)

        }

        if (context.config.messaging.markSnapAsSeenButton.get()) {
            fun getMessageId(): Pair<String, String>? {
                return mediaDownloader.lastSeenMapParams?.get("MESSAGE_ID")
                    ?.toString()
                    ?.split(":")
                    ?.takeIf { it.size == 3 }
                    ?.let { return it[0] to it[2] }
            }

            parent.addView(ImageView(view.context).apply {
                setImageDrawable(eyeSvgDrawable)
                setColorFilter(Color.WHITE)
                addOnAttachStateChangeListener(object: View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        v.visibility = View.GONE
                        this@OperaViewerIcons.context.coroutineScope.launch(Dispatchers.Main) {
                            delay(300)
                            v.visibility = if (getMessageId() != null) View.VISIBLE else View.GONE
                        }
                    }
                    override fun onViewDetachedFromWindow(v: View) {}
                })
                layoutParams = FrameLayout.LayoutParams(
                    actionMenuIconSize,
                    actionMenuIconSize
                ).apply {
                    setMargins(0, 0, 0, actionMenuIconMarginTop * 2 + (80 * context.resources.displayMetrics.density).toInt())
                    marginEnd = actionMenuIconMarginTop * 2
                    marginStart = actionMenuIconMarginTop * 2
                    gravity = Gravity.BOTTOM or Gravity.END
                }
                setOnClickListener {
                    this@OperaViewerIcons.context.apply {
                        coroutineScope.launch {
                            val (conversationId, clientMessageId) = getMessageId() ?: return@launch
                            val result = feature(AutoMarkAsRead::class).markSnapAsSeen(conversationId, clientMessageId.toLong())
                            if (result == "DUPLICATEREQUEST") return@launch
                            if (result == null) {
                                if (config.messaging.skipWhenMarkingAsSeen.get()) {
                                    withContext(Dispatchers.Main) {
                                        parent.iterateParent {
                                            it.triggerCloseTouchEvent()
                                            false
                                        }
                                    }
                                }
                                inAppOverlay.showStatusToast(
                                    Icons.Default.Info,
                                    translation["mark_as_seen.seen_toast"],
                                    durationMs = 800
                                )
                            } else {
                                inAppOverlay.showStatusToast(
                                    Icons.Default.Info,
                                    "Failed to mark as seen: $result",
                                )
                            }
                        }
                    }
                }
            }, 0)
        }
    }
}