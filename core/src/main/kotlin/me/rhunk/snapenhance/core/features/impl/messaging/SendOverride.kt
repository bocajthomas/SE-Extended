package me.rhunk.snapenhance.core.features.impl.messaging

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.rhunk.snapenhance.common.data.ContentType
import me.rhunk.snapenhance.common.ui.createComposeAlertDialog
import me.rhunk.snapenhance.common.util.protobuf.ProtoEditor
import me.rhunk.snapenhance.common.util.protobuf.ProtoReader
import me.rhunk.snapenhance.common.util.protobuf.ProtoWriter
import me.rhunk.snapenhance.core.event.events.impl.MediaUploadEvent
import me.rhunk.snapenhance.core.event.events.impl.NativeUnaryCallEvent
import me.rhunk.snapenhance.core.event.events.impl.SendMessageWithContentEvent
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.features.impl.experiments.MediaFilePicker
import me.rhunk.snapenhance.core.messaging.MessageSender
import me.rhunk.snapenhance.core.util.ktx.getObjectFieldOrNull
import java.util.Locale
import kotlin.time.DurationUnit
import kotlin.time.toDuration


class SendOverride : Feature("Send Override") {
    private var selectedType by mutableStateOf("SNAP")
    private var customDuration by mutableFloatStateOf(10f)

    @OptIn(ExperimentalLayoutApi::class)
    override fun init() {
        val stripMediaMetadata = context.config.messaging.stripMediaMetadata.get()
        var postSavePolicy: Int? = null

        val configOverrideType = context.config.messaging.galleryMediaSendOverride.getNullable()
        if (configOverrideType == null && stripMediaMetadata.isEmpty()) return

        context.event.subscribe(MediaUploadEvent::class) { event ->
            ProtoReader(event.localMessageContent.content!!).followPath(11, 5)?.let { snapDocPlayback ->
                event.onMediaUploaded { result ->
                    result.messageContent.content = ProtoEditor(result.messageContent.content!!).apply {
                        edit(11, 5) {
                            edit(1) {
                                edit(1) {
                                    snapDocPlayback.getVarInt(2, 99)?.let { customDuration ->
                                        remove(15)
                                        addVarInt(15, customDuration)
                                    }
                                    remove(27)
                                    remove(26)
                                    addBuffer(26, byteArrayOf())
                                }
                            }

                            // set back the original snap duration
                            snapDocPlayback.getByteArray(2)?.let {
                                val originalHasSound = firstOrNull(2)?.toReader()?.getVarInt(5)
                                remove(2)
                                addBuffer(2, it)

                                originalHasSound?.let { hasSound ->
                                    edit(2) {
                                        remove(5)
                                        addVarInt(5, hasSound)
                                    }
                                }
                            }
                        }

                        if (stripMediaMetadata.isNotEmpty()) {
                            when (result.messageContent.contentType) {
                                ContentType.SNAP, ContentType.EXTERNAL_MEDIA -> {
                                    edit(*(if (result.messageContent.contentType == ContentType.SNAP) intArrayOf(11) else intArrayOf(3, 3))) {
                                        if (stripMediaMetadata.contains("hide_caption_text")) {
                                            edit(5) {
                                                editEach(1) {
                                                    remove(2)
                                                }
                                            }
                                        }
                                        if (stripMediaMetadata.contains("hide_snap_filters")) {
                                            remove(9)
                                            remove(11)
                                        }
                                        if (stripMediaMetadata.contains("hide_extras")) {
                                            remove(13)
                                            edit(5, 1) {
                                                remove(2)
                                            }
                                        }
                                    }
                                }
                                ContentType.NOTE -> {
                                    if (stripMediaMetadata.contains("remove_audio_note_duration")) {
                                        edit(6, 1, 1) {
                                            remove(13)
                                        }
                                    }
                                    if (stripMediaMetadata.contains("remove_audio_note_transcript_capability")) {
                                        edit(6, 1) {
                                            remove(3)
                                        }
                                    }
                                }
                                else -> {}
                            }
                        }

                        edit(11, 5, 2) {
                            remove(99)
                        }
                    }.toByteArray()
                }
            }
        }

        if (configOverrideType == null) return

        context.event.subscribe(NativeUnaryCallEvent::class) { event ->
            if (event.uri != "/messagingcoreservice.MessagingCoreService/CreateContentMessage") return@subscribe
            postSavePolicy?.let { savePolicy ->
                context.log.verbose("postSavePolicy=$savePolicy")
                event.buffer = ProtoEditor(event.buffer).apply {
                    edit(4) {
                        remove(7)
                        addVarInt(7, savePolicy)
                    }

                    // remove Keep Snaps in Chat ability
                    if (savePolicy == 1/* PROHIBITED */) {
                        edit(6, 9) {
                            remove(1)
                        }
                    }
                }.toByteArray()
            }
        }

        context.event.subscribe(SendMessageWithContentEvent::class) { event ->
            postSavePolicy = null
            if (event.destinations.stories?.isNotEmpty() == true && event.destinations.conversations?.isEmpty() == true) return@subscribe
            val localMessageContent = event.messageContent
            if (localMessageContent.contentType != ContentType.EXTERNAL_MEDIA && localMessageContent.instanceNonNull().getObjectFieldOrNull("mExternalContentMetadata") == null) return@subscribe

            //prevent story replies
            val messageProtoReader = ProtoReader(localMessageContent.content ?: return@subscribe)
            if (messageProtoReader.contains(7)) return@subscribe

            event.canceled = true

            fun sendMedia(overrideType: String, snapDurationMs: Int?): Boolean {
                if (overrideType != "ORIGINAL" && (messageProtoReader.followPath(3)?.getCount(3) ?: 0) > 1) {
                    context.inAppOverlay.showStatusToast(
                        icon = Icons.Default.WarningAmber,
                        context.translation["gallery_media_send_override.multiple_media_toast"]
                    )
                    return false
                }

                when (overrideType) {
                    "SNAP", "SAVEABLE_SNAP" -> {
                        postSavePolicy = if (overrideType == "SAVEABLE_SNAP") 3 /* VIEW_SESSION */ else 1 /* PROHIBITED */

                        val extras = messageProtoReader.followPath(3, 3, 13)?.getBuffer()

                        if (localMessageContent.contentType != ContentType.SNAP) {
                            localMessageContent.content = ProtoWriter().apply {
                                from(11) {
                                    from(5) {
                                        from(1) {
                                            from(1) {
                                                addVarInt(2, 0)
                                                addVarInt(12, 0)
                                                addVarInt(15, 0)
                                            }
                                            addVarInt(6, 1)
                                        }
                                        from(2) {}
                                    }
                                    extras?.let {
                                        addBuffer(13, it)
                                    }
                                    from(22) {}
                                }
                            }.toByteArray()
                        }

                        localMessageContent.contentType = ContentType.SNAP
                        localMessageContent.content = ProtoEditor(localMessageContent.content!!).apply {
                            edit(11, 5, 2) {
                                arrayOf(6, 7, 8).forEach { remove(it) }
                                addVarInt(5, messageProtoReader.getVarInt(3, 3, 5, 2, 5) ?: messageProtoReader.getVarInt(11, 5, 2, 5) ?: 1)
                                // set snap duration
                                if (snapDurationMs != null) {
                                    addVarInt(8, snapDurationMs / 1000)
                                    if (snapDurationMs / 1000 <= 0) {
                                        addVarInt(99, snapDurationMs)
                                    }
                                } else {
                                    addBuffer(6, byteArrayOf())
                                }
                            }

                            // set app source
                            edit(11, 22) {
                                remove(4)
                                addVarInt(4, 5) // APP_SOURCE_CAMERA
                            }
                        }.toByteArray()
                    }
                    "NOTE" -> {
                        localMessageContent.contentType = ContentType.NOTE
                        localMessageContent.content =
                            MessageSender.audioNoteProto(
                                messageProtoReader.getVarInt(3, 3, 5, 1, 1, 15) ?: context.feature(MediaFilePicker::class).lastMediaDuration ?: 0,
                                Locale.getDefault().toLanguageTag()
                            )
                    }
                }

                return true
            }

            if (configOverrideType != "always_ask") {
                if (sendMedia(configOverrideType, 10)) {
                    event.invokeOriginal()
                }
                return@subscribe
            }

            context.runOnUiThread {
                createComposeAlertDialog(context.mainActivity!!) { alertDialog ->
                    val mainTranslation = remember {
                        context.translation.getCategory("send_override_dialog")
                    }

                    @Composable
                    fun ActionTile(
                        modifier: Modifier = Modifier,
                        selected: Boolean = false,
                        icon: ImageVector,
                        title: String,
                        onClick: () -> Unit
                    ) {
                        Card(
                            modifier = modifier,
                            onClick = onClick,
                            elevation = if (selected) CardDefaults.elevatedCardElevation(disabledElevation = 3.dp) else CardDefaults.cardElevation(),
                            colors = if (selected) CardDefaults.elevatedCardColors() else CardDefaults.cardColors()
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .size(75.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(icon, contentDescription = title, modifier = Modifier
                                    .size(32.dp)
                                    .padding(4.dp))
                                Text(title, modifier = Modifier.fillMaxWidth(), fontSize = 12.sp, fontWeight = FontWeight.Light, softWrap = true, lineHeight = 14.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val translation = remember {
                            context.translation.getCategory("features.options.gallery_media_send_override")
                        }

                        Text(fontSize = 20.sp, fontWeight = FontWeight.Medium, text = "Send as ${
                            translation[selectedType]}", modifier = Modifier.padding(5.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ActionTile(selected = selectedType == "ORIGINAL", icon = Icons.Filled.Photo, title =
                            translation["ORIGINAL"]) {
                                selectedType = "ORIGINAL"
                            }
                            ActionTile(selected = selectedType == "SNAP" || selectedType == "SAVEABLE_SNAP", icon = Icons.Filled.PhotoCamera, title = translation["SNAP"]) {
                                selectedType = "SNAP"
                            }
                            ActionTile(selected = selectedType == "NOTE", icon = Icons.Filled.MusicNote, title = translation["NOTE"]) {
                                selectedType = "NOTE"
                            }
                        }

                        fun convertDuration(duration: Float): Int? {
                            return when  {
                                duration in -2f..-1f -> 100
                                duration in -1f..-0f -> 250
                                duration in -0f..1f -> 500
                                duration >= 11f -> null
                                else -> ((duration * 1000).toInt() / 1000) * 1000
                            }
                        }

                        when (selectedType) {
                            "SNAP", "SAVEABLE_SNAP" -> {
                                fun toggleSaveable() {
                                    selectedType = if (selectedType == "SAVEABLE_SNAP") "SNAP" else "SAVEABLE_SNAP"
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        toggleSaveable()
                                    },
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ){
                                    Checkbox(
                                        checked = selectedType == "SAVEABLE_SNAP",
                                        onCheckedChange = {
                                            toggleSaveable()
                                        }
                                    )
                                    Text(text = mainTranslation["saveable_snap_hint"], lineHeight = 15.sp)
                                }
                                Column(
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text(
                                        text = mainTranslation.format("duration",
                                            "duration" to (convertDuration(customDuration)?.toDuration(DurationUnit.MILLISECONDS)?.toString(DurationUnit.SECONDS, 2) ?: mainTranslation["unlimited_duration"])
                                        )
                                    )
                                    Slider(
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = selectedType != "SAVEABLE_SNAP",
                                        value = customDuration,
                                        onValueChange = {
                                            customDuration = it
                                        },
                                        valueRange = -2f..11f,
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(onClick = {
                                alertDialog.dismiss()
                            }) {
                                Text(context.translation["button.cancel"])
                            }
                            Button(onClick = {
                                alertDialog.dismiss()
                                if (sendMedia(selectedType, if (selectedType != "SAVEABLE_SNAP" ) convertDuration(customDuration) else null)) {
                                    event.invokeOriginal()
                                }
                            }) {
                                Text(context.translation["button.send"])
                            }
                        }
                    }
                }.show()
            }
        }
    }
}