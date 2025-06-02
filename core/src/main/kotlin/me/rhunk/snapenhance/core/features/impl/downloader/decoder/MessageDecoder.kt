package me.rhunk.snapenhance.core.features.impl.downloader.decoder

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import me.rhunk.snapenhance.common.data.download.DownloadMediaType
import me.rhunk.snapenhance.common.data.download.InputMedia
import me.rhunk.snapenhance.common.data.download.MediaEncryptionKeyPair
import me.rhunk.snapenhance.common.data.download.toKeyPair
import me.rhunk.snapenhance.common.util.protobuf.ProtoReader
import me.rhunk.snapenhance.common.util.snap.RemoteMediaResolver
import me.rhunk.snapenhance.core.wrapper.impl.MessageContent
import java.io.InputStream
import java.net.URL
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class DecodedAttachment(
    val boltKey: String?,
    val directUrl: String? = null,
    val type: AttachmentType,
    val attachmentInfo: AttachmentInfo?
) {
    @OptIn(ExperimentalEncodingApi::class)
    val mediaUniqueId: String? by lazy {
        runCatching {
            Base64.UrlSafe.decode(boltKey.toString())
        }.getOrNull()?.let {
            ProtoReader(it).getString(2, 2)?.substringBefore(".")
        } ?: directUrl?.substringAfterLast("/")?.substringBeforeLast("?")?.substringBeforeLast(".")?.let { Base64.UrlSafe.encode(it.toByteArray()) }
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend inline fun openStream(callback: (mediaStream: InputStream?, length: Long) -> Unit) {
        boltKey?.let { mediaUrlKey ->
            RemoteMediaResolver.downloadBoltMedia(Base64.UrlSafe.decode(mediaUrlKey), decryptionCallback = {
                attachmentInfo?.encryption?.decryptInputStream(it) ?: it
            }, resultCallback = { inputStream, length ->
                callback(inputStream, length)
            })
        } ?: directUrl?.let { rawMediaUrl ->
            RemoteMediaResolver.downloadMedia(rawMediaUrl, decryptionCallback = {
                attachmentInfo?.encryption?.decryptInputStream(it) ?: it
            }) { inputStream, length ->
                callback(inputStream, length)
            }
        } ?: callback(null, 0)
    }

    fun createInputMedia(
        isOverlay: Boolean = false
    ): InputMedia? {
        return InputMedia(
            content = boltKey ?: directUrl ?: return null,
            type = if (boltKey != null) DownloadMediaType.PROTO_MEDIA else DownloadMediaType.REMOTE_MEDIA,
            encryption = attachmentInfo?.encryption,
            attachmentType = type.key,
            isOverlay = isOverlay
        )
    }
}

@OptIn(ExperimentalEncodingApi::class)
object MessageDecoder {
    private val gson = GsonBuilder().create()

    private fun ProtoReader.decodeClearTextEncryption(encoded: Boolean = true): MediaEncryptionKeyPair? {
        val key = if (encoded) Base64.decode(getString(1)?.trim() ?: return null) else getByteArray(1) ?: return null
        val iv = if (encoded) Base64.decode(getString(2)?.trim() ?: return null) else getByteArray(2) ?: return null

        return Pair(key, iv).toKeyPair()
    }

    private fun ProtoReader.decodeMediaMetadata(): AttachmentInfo {
        return AttachmentInfo(
            encryption = run {
                followPath(4)?.apply {
                    decodeClearTextEncryption(encoded = true)?.let {
                        return@run it
                    }
                }

                followPath(19)?.apply {
                    decodeClearTextEncryption( encoded = false)?.let { encryption ->
                        return@run encryption
                    }
                }
                null
            },
            resolution = followPath(5)?.let {
                (it.getVarInt(1)?.toInt() ?: 0) to (it.getVarInt(2)?.toInt() ?: 0)
            },
            duration = getVarInt(15)   // external medias
                ?: getVarInt(13) // audio notes
        )
    }

    private fun ProtoReader.decodeAttachment(): AttachmentInfo? {
        return followPath(1, 1)?.decodeMediaMetadata()
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun getEncodedMediaReferences(messageContent: JsonElement): List<String> {
        return getMediaReferences(messageContent).map { reference ->
                Base64.UrlSafe.encode(
                    reference.asJsonObject.getAsJsonArray("mContentObject").map { it.asByte }.toByteArray()
                )
            }
            .toList()
    }

    fun getEncodedMediaReferences(messageContent: MessageContent): List<String> {
        return getEncodedMediaReferences(gson.toJsonTree(messageContent.instanceNonNull()))
    }

    fun getMediaReferences(messageContent: JsonElement): List<JsonElement> {
        return messageContent.asJsonObject.getAsJsonArray("mRemoteMediaReferences")
            .asSequence()
            .map { it.asJsonObject.getAsJsonArray("mMediaReferences") }
            .flatten()
            .sortedBy {
                it.asJsonObject["mMediaListId"].asLong
            }.toList()
    }


    fun decode(messageContent: MessageContent): List<DecodedAttachment> {
        return decode(
            ProtoReader(messageContent.content!!),
            customMediaReferences = getEncodedMediaReferences(gson.toJsonTree(messageContent.instanceNonNull()))
        ).toMutableList().apply {
            if (messageContent.quotedMessage?.takeIf { it.isPresent() } != null && messageContent.quotedMessage!!.content?.takeIf { it.isPresent() } != null) {
                addAll(0, decode(
                    MessageContent(messageContent.quotedMessage!!.content!!.instanceNonNull())
                ))
            }
        }
    }

    fun decode(messageContent: JsonObject): List<DecodedAttachment> {
        return decode(
            ProtoReader(messageContent.getAsJsonArray("mContent")
                .map { it.asByte }
                .toByteArray()),
            customMediaReferences = getEncodedMediaReferences(messageContent)
        ).toMutableList().apply {
            if (messageContent.has("mQuotedMessage") && messageContent.getAsJsonObject("mQuotedMessage").has("mContent")) {
                addAll(0, decode(messageContent.getAsJsonObject("mQuotedMessage").getAsJsonObject("mContent")))
            }
        }
    }

    fun decode(
        protoReader: ProtoReader,
        customMediaReferences: List<String>? = null // when customReferences is null it means that the message is from arroyo database
    ): List<DecodedAttachment> {
        val decodedAttachment = mutableListOf<DecodedAttachment>()
        val mediaReferences = mutableListOf<String>()
        customMediaReferences?.let { mediaReferences.addAll(it) }
        var mediaKeyIndex = 0

        fun ProtoReader.decodeSnapDocMediaPlayback(type: AttachmentType) {
            decodedAttachment.add(
                DecodedAttachment(
                    boltKey = mediaReferences.getOrNull(mediaKeyIndex++),
                    type = type,
                    attachmentInfo = decodeAttachment() ?: return
                )
            )
        }

        fun ProtoReader.decodeSnapDocMedia(type: AttachmentType) {
            followPath(5) { decodeSnapDocMediaPlayback(type) }
        }

        fun ProtoReader.decodeStickers() {
            followPath(1) {
                val packId = getString(1)
                val reference = getString(2) ?: return@followPath
                val stickerUrl = when (packId) {
                    "snap" -> "https://gcs.sc-cdn.net/sticker-packs-sc/stickers/$reference"
                    "bitmoji" -> reference.split(":").let {
                        "https://cf-st.sc-cdn.net/3d/render/${
                            it.getOrNull(0) ?: return@followPath
                        }-${it.drop(2).joinToString("-")}-v${it.getOrNull(1) ?: return@followPath}.webp?ua=2"
                    }
                    else -> return@followPath
                }
                decodedAttachment.add(
                    DecodedAttachment(
                        boltKey = null,
                        directUrl = stickerUrl,
                        type = AttachmentType.STICKER,
                        attachmentInfo = BitmojiSticker(
                            reference = reference
                        )
                    )
                )
            }
            followPath(2, 1) {
                decodedAttachment.add(
                    DecodedAttachment(
                        boltKey = mediaReferences.getOrNull(mediaKeyIndex++),
                        type = AttachmentType.STICKER,
                        attachmentInfo = decodeMediaMetadata()
                    )
                )
            }
        }

        fun ProtoReader.decodeShares() {
            // saved story
            followPath(24, 2) {
                decodeSnapDocMedia(AttachmentType.EXTERNAL_MEDIA)
            }
            // memories story
            followPath(11) {
                eachBuffer(3) {
                    decodeSnapDocMedia(AttachmentType.EXTERNAL_MEDIA)
                }
            }
        }

        // media keys
        protoReader.eachBuffer(4, 5) {
            getByteArray(1, 3)?.also { mediaKey ->
                mediaReferences.add(Base64.UrlSafe.encode(mediaKey))
            }
        }

        val mediaReader = customMediaReferences?.let { protoReader } ?: protoReader.followPath(4, 4) ?: return emptyList()

        mediaReader.apply {
            // external media
            eachBuffer(3, 3) {
                decodeSnapDocMedia(AttachmentType.EXTERNAL_MEDIA)
            }

            // stickers
            followPath(4) { decodeStickers() }

            // shares
            followPath(5) {
                decodeShares()
            }

            // audio notes
            followPath(6) note@{
                val audioNote = decodeAttachment() ?: return@note

                decodedAttachment.add(
                    DecodedAttachment(
                        boltKey = mediaReferences.getOrNull(mediaKeyIndex++),
                        type = AttachmentType.NOTE,
                        attachmentInfo = audioNote
                    )
                )
            }

            // story replies
            followPath(7) {
                // original story reply
                followPath(3) {
                    decodeSnapDocMedia(AttachmentType.ORIGINAL_STORY)
                }

                // external medias
                followPath(12) {
                    eachBuffer(3) { decodeSnapDocMedia(AttachmentType.EXTERNAL_MEDIA) }
                }

                // attached sticker
                followPath(13) { decodeStickers()  }

                // reply shares
                followPath(14) { decodeShares() }

                // attached audio note
                followPath(15) { decodeSnapDocMediaPlayback(AttachmentType.NOTE) }

                // reply snap
                followPath(17) {
                    decodeSnapDocMedia(AttachmentType.SNAP)
                }
            }

            // snaps
            followPath(11) {
                decodeSnapDocMedia(AttachmentType.SNAP)
            }

            // creative tools items
            followPath(14, 2, 2) {
                // custom sticker
                followPath(3) sticker@{
                    decodedAttachment.add(
                        DecodedAttachment(
                            boltKey = if (contains(4)) {
                                Base64.UrlSafe.encode(getByteArray(4, 4) ?: return@sticker)
                            } else mediaReferences.getOrNull(mediaKeyIndex++),
                            type = AttachmentType.STICKER,
                            attachmentInfo = AttachmentInfo(
                                encryption = decodeClearTextEncryption(encoded = true) ?: followPath(5)
                                    ?.decodeClearTextEncryption(encoded = false)
                            )
                        )
                    )
                }

                // gifs
                followPath(13) {
                    eachBuffer(4) {
                        followPath(2) {
                            decodedAttachment.add(
                                DecodedAttachment(
                                    boltKey = getByteArray(4)?.let { Base64.UrlSafe.encode(it) },
                                    type = AttachmentType.GIF,
                                    attachmentInfo = null
                                )
                            )
                        }
                    }
                }
            }

            // map reaction
            followPath(20, 2) {
                decodeSnapDocMedia(AttachmentType.EXTERNAL_MEDIA)
            }
        }

        return decodedAttachment
    }
}