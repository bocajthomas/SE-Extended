package me.rhunk.snapenhance.core.features.impl.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.Shape
import android.view.ViewGroup
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.common.data.ContentType
import me.rhunk.snapenhance.common.util.protobuf.ProtoReader
import me.rhunk.snapenhance.core.event.events.impl.BindViewEvent
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.ui.addForegroundDrawable
import me.rhunk.snapenhance.core.ui.randomTag
import me.rhunk.snapenhance.core.ui.removeForegroundDrawable
import me.rhunk.snapenhance.core.util.EvictingMap
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.ktx.getObjectField
import me.rhunk.snapenhance.core.util.media.PreviewUtils
import me.rhunk.snapenhance.mapper.impl.CallbackMapper
import java.io.File

class SnapPreview : Feature("SnapPreview") {
    private val mediaFileCache = EvictingMap<String, File>(500) // mMediaId => mediaFile
    private val bitmapCache = EvictingMap<String, Bitmap>(50) // filePath => bitmap

    private val fetchJobTab = randomTag()

    override fun init() {
        if (!context.config.userInterface.snapPreview.get()) return
        context.mappings.useMapper(CallbackMapper::class) {
            callbacks.getClass("ContentCallback")?.hook("handleContentResult", HookStage.BEFORE) { param ->
                val contentResult = param.arg<Any>(0)
                val classMethods = contentResult::class.java.methods

                val contentKey = classMethods.find { it.name == "getContentKey" }?.invoke(contentResult) ?: return@hook
                if (contentKey.getObjectField("mMediaContextType").toString() != "CHAT") return@hook

                val filePath = classMethods.find { it.name == "getFilePath" }?.invoke(contentResult) ?: return@hook
                val mediaId = contentKey.getObjectField("mMediaId").toString()

                mediaFileCache[mediaId.substringAfter("-")] = File(filePath.toString())
            }
        }

        onNextActivityCreate {
            val (chatMediaCardHeight, chatMediaCardSnapMargin, chatMediaCardSnapMarginStartSdl) = context.userInterface.run {
                Triple(dpToPx(60), dpToPx(10), dpToPx(15))
            }

            fun decodeMedia(file: File) = runCatching {
                bitmapCache.getOrPut(file.absolutePath) {
                    PreviewUtils.resizeBitmap(
                        PreviewUtils.createPreviewFromFile(file) ?: return@runCatching null,
                        chatMediaCardHeight - chatMediaCardSnapMargin,
                        chatMediaCardHeight - chatMediaCardSnapMargin
                    )
                }
            }.getOrNull()

            context.event.subscribe(BindViewEvent::class) { event ->
                event.chatMessage { _, _ ->
                    val messageLinearLayout = (event.view as ViewGroup).getChildAt(0) as? ViewGroup ?: return@subscribe
                    messageLinearLayout.removeForegroundDrawable("snapPreview")

                    val message = event.databaseMessage ?: return@chatMessage
                    val messageReader = ProtoReader(message.messageContent ?: return@chatMessage)
                    val contentType = ContentType.fromMessageContainer(messageReader.followPath(4, 4))

                    if (contentType != ContentType.SNAP || message.isSaved == 1) return@chatMessage

                    val mediaIdKey = messageReader.getString(4, 5, 1, 3, 2, 2) ?: return@chatMessage

                    var mediaFile = mediaFileCache[mediaIdKey] ?: return@chatMessage
                    val mediaFilePath = mediaFile.absolutePath

                    (messageLinearLayout.getTag(fetchJobTab) as? Job)?.cancel()

                    if (bitmapCache[mediaFilePath] == null) {
                        messageLinearLayout.setTag(fetchJobTab, context.coroutineScope.launch {
                            bitmapCache[mediaFilePath] = decodeMedia(mediaFile) ?: return@launch
                            messageLinearLayout.postInvalidate()
                        })
                    }

                    messageLinearLayout.addForegroundDrawable("snapPreview", ShapeDrawable(object: Shape() {
                        override fun draw(canvas: Canvas, paint: Paint) {
                            val bitmap = bitmapCache[mediaFilePath] ?: return

                            canvas.drawBitmap(bitmap,
                                canvas.width.toFloat() - bitmap.width - chatMediaCardSnapMarginStartSdl.toFloat() - chatMediaCardSnapMargin.toFloat(),
                                (canvas.height - bitmap.height) / 2f,
                                null
                            )
                        }
                    }))
                }
            }
        }
    }
}