package me.rhunk.snapenhance.core.data

class SnapClassCache (
    private val classLoader: ClassLoader
) {
    val snapUUID by lazy { findClass("com.snapchat.client.messaging.UUID") }
    val snapManager by lazy { findClass("com.snapchat.client.messaging.SnapManager\$CppProxy") }
    val conversationManager by lazy { findClass("com.snapchat.client.messaging.ConversationManager\$CppProxy") }
    val presenceSession by lazy { findClass("com.snapchat.talkcorev3.PresenceSession\$CppProxy") }
    val message by lazy { findClass("com.snapchat.client.messaging.Message") }
    val messageUpdateEnum by lazy { findClass("com.snapchat.client.messaging.MessageUpdate") }
    val serverMessageIdentifier by lazy { findClass("com.snapchat.client.messaging.ServerMessageIdentifier") }
    val unifiedGrpcService by lazy { findClass("com.snapchat.client.grpc.UnifiedGrpcService\$CppProxy") }
    val networkApi by lazy { findClass("com.snapchat.client.network_api.NetworkApi\$CppProxy") }
    val messageDestinations by lazy { findClass("com.snapchat.client.messaging.MessageDestinations") }
    val localMessageContent by lazy { findClass("com.snapchat.client.messaging.LocalMessageContent") }
    val feedEntry by lazy { findClass("com.snapchat.client.messaging.FeedEntry") }
    val conversation by lazy { findClass("com.snapchat.client.messaging.Conversation") }
    val feedManager by lazy { findClass("com.snapchat.client.messaging.FeedManager\$CppProxy") }
    val nativeBridge by lazy { runCatching { findClass("com.snapchat.client.valdi.NativeBridge") }.getOrNull() ?: findClass("com.snapchat.client.composer.NativeBridge") }
    val valdiView by lazy { runCatching { findClass("com.snap.valdi.views.ValdiView") }.getOrNull() ?: findClass("com.snap.composer.views.ComposerView") }
    val valdiFunction by lazy { runCatching { findClass("com.snap.valdi.callable.ValdiFunction") }.getOrNull() ?: findClass("com.snap.composer.callable.ComposerFunction") }
    val valdiMarshaller by lazy { runCatching { findClass("com.snap.valdi.utils.ValdiMarshaller") }.getOrNull() ?: findClass("com.snap.composer.utils.ComposerMarshaller") }

    private fun findClass(className: String): Class<*> {
        return try {
            classLoader.loadClass(className)
        } catch (e: ClassNotFoundException) {
            throw RuntimeException("Failed to find class $className", e)
        }
    }
}