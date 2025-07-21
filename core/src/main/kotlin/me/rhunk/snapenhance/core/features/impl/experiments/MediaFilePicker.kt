package me.rhunk.snapenhance.core.features.impl.experiments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.database.CursorWrapper
import android.media.MediaPlayer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.common.ui.createComposeView
import me.rhunk.snapenhance.common.util.ktx.getLongOrNull
import me.rhunk.snapenhance.common.util.ktx.getTypeArguments
import me.rhunk.snapenhance.core.event.events.impl.ActivityResultEvent
import me.rhunk.snapenhance.core.event.events.impl.AddViewEvent
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.ui.ViewAppearanceHelper
import me.rhunk.snapenhance.core.util.dataBuilder
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import java.io.InputStream
import java.lang.reflect.Method
import kotlin.random.Random

class MediaFilePicker : Feature("Media File Picker") {
    var lastMediaDuration: Long? = null
        private set

    @SuppressLint("Recycle")
    override fun init() {
        if (!context.config.experimental.mediaFilePicker.get()) return

        onNextActivityCreate(defer = true) {
            lateinit var chatMediaDrawerActionHandler: Any
            lateinit var sendItemsMethod: Method

            findClass("com.snap.composer.memories.ChatMediaDrawer").genericSuperclass?.getTypeArguments()?.getOrNull(1)?.apply {
                methods.first {
                    it.parameterTypes.size == 1 && it.parameterTypes[0].name.endsWith("ChatMediaDrawerActionHandler")
                }.also { method ->
                    sendItemsMethod = method.parameterTypes[0].methods.first { it.name == "sendItems" }
                }.hook(HookStage.AFTER) {
                    chatMediaDrawerActionHandler = it.arg(0)
                }
            }

            var requestCode: Int? = null
            var firstVideoId: Long? = null
            var mediaInputStream: InputStream? = null

            ContentResolver::class.java.apply {
                hook("query", HookStage.AFTER) { param ->
                    val uri = param.arg<Uri>(0)
                    if (!uri.toString().endsWith(firstVideoId.toString())) return@hook

                    param.setResult(object: CursorWrapper(param.getResult() as Cursor) {
                        override fun getLong(columnIndex: Int): Long {
                            if (getColumnName(columnIndex) == "duration") {
                                return lastMediaDuration ?: -1
                            }
                            return super.getLong(columnIndex)
                        }
                    })
                }
                hook("openInputStream", HookStage.BEFORE) { param ->
                    val uri = param.arg<Uri>(0)
                    if (uri.toString().endsWith(firstVideoId.toString())) {
                        param.setResult(mediaInputStream)
                        mediaInputStream = null
                    }
                }
            }

            context.event.subscribe(ActivityResultEvent::class) { event ->
                if (event.requestCode != requestCode || event.resultCode != Activity.RESULT_OK) return@subscribe
                requestCode = null

                firstVideoId = context.androidContext.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Video.Media._ID),
                    null,
                    null,
                    "${MediaStore.Video.Media.DATE_TAKEN} DESC"
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        cursor.getLongOrNull("_id")
                    } else {
                        null
                    }
                }

                if (firstVideoId == null) {
                    context.inAppOverlay.showStatusToast(
                        Icons.Default.Upload,
                        "Must have a video in gallery to upload."
                    )
                    return@subscribe
                }

                fun sendMedia() {
                    sendItemsMethod.invoke(chatMediaDrawerActionHandler, listOf<Any>(), listOf(
                        sendItemsMethod.genericParameterTypes[1].getTypeArguments().first().dataBuilder {
                            from("_item") {
                                set("_cameraRollSource", "Snapchat")
                                set("_contentUri", "")
                                set("_durationMs", 0.0)
                                set("_disabled", false)
                                set("_imageRotation", 0.0)
                                set("_width", 1080.0)
                                set("_height", 1920.0)
                                set("_timestampMs", System.currentTimeMillis().toDouble())
                                from("_itemId") {
                                    set("_itemId", firstVideoId.toString())
                                    set("_type", "VIDEO")
                                }
                            }
                            set("_order", 0.0)
                        }
                    ))
                }

                fun startConversion(audioOnly: Boolean) {
                    context.coroutineScope.launch {
                        lastMediaDuration = MediaPlayer().run {
                            setDataSource(context.androidContext, event.intent.data!!)
                            prepare()
                            duration.toLong().also {
                                release()
                            }
                        }

                        context.inAppOverlay.showStatusToast(Icons.Default.Crop, "Converting media...", durationMs = 3000)
                        val pfd = context.bridgeClient.convertMedia(
                            context.androidContext.contentResolver.openFileDescriptor(event.intent.data!!, "r")!!,
                            "m4a",
                            "m4a",
                            "aac",
                            if (!audioOnly) "libx264" else null
                        )

                        if (pfd == null) {
                            context.inAppOverlay.showStatusToast(Icons.Default.Error, "Failed to convert media.")
                            return@launch
                        }

                        context.inAppOverlay.showStatusToast(Icons.Default.CheckCircleOutline, "Media converted successfully.")

                        runCatching {
                            mediaInputStream = ParcelFileDescriptor.AutoCloseInputStream(pfd)
                            sendMedia()
                        }.onFailure {
                            mediaInputStream = null
                            context.log.error(it)
                            context.inAppOverlay.showStatusToast(Icons.Default.Error, "Failed to send media.")
                        }
                    }
                }

                val isAudio = context.androidContext.contentResolver.getType(event.intent.data!!)!!.startsWith("audio/")

                if (isAudio || context.config.messaging.galleryMediaSendOverride.getNullable() == null) {
                    startConversion(isAudio)
                    return@subscribe
                }

                ViewAppearanceHelper.newAlertDialogBuilder(context.mainActivity!!)
                    .setTitle("Convert video file")
                    .setItems(arrayOf("Send as video/audio", "Send as audio only")) { _, which ->
                        startConversion(which == 1)
                    }
                    .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }.show()
            }

            val buttonTag = Random.nextInt(0, 65535)

            context.event.subscribe(AddViewEvent::class) { event ->
                if (event.parent !is FrameLayout || !event.view::class.java.name.endsWith("ChatMediaDrawer")) return@subscribe

                event.view.addOnAttachStateChangeListener(object: View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        if (event.parent.findViewWithTag<View>(buttonTag)?.run {
                                visibility = View.VISIBLE
                                bringToFront()
                            } != null) return
                        event.parent.addView(
                            createComposeView(context.mainActivity!!) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    FilledIconButton(onClick = {
                                        requestCode = Random.nextInt(0, 65535)
                                        this@MediaFilePicker.context.mainActivity!!.startActivityForResult(
                                            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                                addCategory(Intent.CATEGORY_OPENABLE)
                                                type = "video/*"
                                                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("video/*", "audio/*"))
                                            },
                                            requestCode!!
                                        )
                                    }) {
                                        Icon(Icons.Default.Upload, "Upload media")
                                    }
                                }
                            }.apply {
                                tag = buttonTag
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                            }
                        )
                    }
                    override fun onViewDetachedFromWindow(v: View) {
                        event.parent.findViewWithTag<View>(buttonTag)?.visibility = View.GONE
                    }
                })
            }
        }
    }
}