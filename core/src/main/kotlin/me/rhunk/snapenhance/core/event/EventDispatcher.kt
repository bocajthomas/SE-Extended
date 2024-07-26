package me.rhunk.snapenhance.core.event

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import me.rhunk.snapenhance.common.util.snap.SnapWidgetBroadcastReceiverHelper
import me.rhunk.snapenhance.core.ModContext
import me.rhunk.snapenhance.core.event.events.impl.*
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.Hooker
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.hook.hookConstructor
import me.rhunk.snapenhance.core.util.ktx.getObjectField
import me.rhunk.snapenhance.core.util.ktx.setObjectField
import me.rhunk.snapenhance.core.wrapper.impl.Message
import me.rhunk.snapenhance.core.wrapper.impl.MessageContent
import me.rhunk.snapenhance.core.wrapper.impl.MessageDestinations
import me.rhunk.snapenhance.core.wrapper.impl.SnapUUID
import me.rhunk.snapenhance.mapper.impl.CallbackMapper
import me.rhunk.snapenhance.mapper.impl.ViewBinderMapper
import java.nio.ByteBuffer

class EventDispatcher(
    private val context: ModContext
) {
    private fun hookViewBinder() {
        context.mappings.useMapper(ViewBinderMapper::class) {
            val cachedHooks = mutableListOf<String>()
            fun cacheHook(clazz: Class<*>, block: Class<*>.() -> Unit) {
                synchronized(cachedHooks) {
                    if (!cachedHooks.contains(clazz.name)) {
                        clazz.block()
                        cachedHooks.add(clazz.name)
                    }
                }
            }

            classReference.get()?.hookConstructor(HookStage.AFTER) { methodParam ->
                cacheHook(
                    methodParam.thisObject<Any>()::class.java
                ) {
                    hook(bindMethod.get().toString(), HookStage.AFTER) bindViewMethod@{ param ->
                        val instance = param.thisObject<Any>()
                        val view = instance::class.java.methods.firstOrNull {
                            it.name == getViewMethod.get().toString()
                        }?.invoke(instance) as? View ?: return@bindViewMethod

                        context.event.post(
                            BindViewEvent(
                                prevModel = param.arg(0),
                                nextModel = param.argNullable(1),
                                view = view
                            )
                        )
                    }
                }
            }
        }

    }

    fun init() {
        context.classCache.conversationManager.hook("sendMessageWithContent", HookStage.BEFORE) { param ->
            context.event.post(SendMessageWithContentEvent(
                destinations = MessageDestinations(param.arg(0)),
                messageContent = MessageContent(param.arg(1)),
                callback = param.arg(2)
            ).apply { adapter = param }) {
                postHookEvent()
            }
        }

        context.classCache.snapManager.hook("onSnapInteraction", HookStage.BEFORE) { param ->
            val interactionType = param.arg<Any>(0).toString()
            val conversationId = SnapUUID(param.arg(1))
            val messageId = param.arg<Long>(2)
            context.event.post(
                OnSnapInteractionEvent(
                    interactionType = interactionType,
                    conversationId = conversationId,
                    messageId = messageId
                ).apply {
                    adapter = param
                }
            ) {
                postHookEvent()
            }
        }

        context.androidContext.classLoader.loadClass(SnapWidgetBroadcastReceiverHelper.CLASS_NAME)
            .hook("onReceive", HookStage.BEFORE) { param ->
            val intent = param.arg(1) as? Intent ?: return@hook
            if (!SnapWidgetBroadcastReceiverHelper.isIncomingIntentValid(intent)) return@hook
            val action = intent.getStringExtra("action") ?: return@hook

            context.event.post(
                SnapWidgetBroadcastReceiveEvent(
                    androidContext = context.androidContext,
                    intent = intent,
                    action = action
                ).apply {
                    adapter = param
                }
            ) {
                postHookEvent()
            }
        }

        ViewGroup::class.java.getMethod(
            "addView",
            View::class.java,
            Int::class.javaPrimitiveType,
            LayoutParams::class.java
        ).hook(HookStage.BEFORE) { param ->
            context.event.post(
                AddViewEvent(
                    parent = param.thisObject(),
                    view = param.arg(0),
                    index = param.arg(1),
                    layoutParams = param.arg(2)
                ).apply {
                    adapter = param
                }
            ) {
                with(param) {
                    setArg(0, view)
                    setArg(1, index)
                    setArg(2, layoutParams)
                }
                postHookEvent()
            }
        }

        LayoutInflater::class.java.getMethod(
            "inflate",
            Int::class.java,
            ViewGroup::class.java,
            Boolean::class.javaPrimitiveType
        ).hook(HookStage.AFTER) { param ->
            val layoutId = param.argNullable<Int>(0) ?: return@hook
            val parent = param.argNullable<ViewGroup>(1)
            val result = param.getResult() as? View

            context.event.post(
                LayoutInflateEvent(
                    layoutId = layoutId,
                    parent = parent,
                    view = result
                ).apply {
                    adapter = param
                }
            ) {
                if (canceled) param.setResult(null)
                postHookEvent()
            }
        }

        context.classCache.networkApi.hook("submit", HookStage.BEFORE) { param ->
            val request = param.arg<Any>(0)

            context.event.post(
                NetworkApiRequestEvent(
                    url = request.getObjectField("mUrl") as String,
                    callback = param.arg(4),
                    uploadDataProvider = param.argNullable(5),
                    request = request,
                ).apply {
                    adapter = param
                }
            ) {
                if (canceled) param.setResult(null)
                request.setObjectField("mUrl", url)
                postHookEvent()
            }
        }

        context.classCache.message.hookConstructor(HookStage.AFTER) { param ->
            context.event.post(
                BuildMessageEvent(
                    message = Message(param.thisObject())
                )
            )
        }

        context.classCache.unifiedGrpcService.hook("unaryCall", HookStage.BEFORE) { param ->
            val uri = param.arg<String>(0)
            val buffer = param.argNullable<ByteBuffer>(1)?.run {
                val array = ByteArray(limit())
                position(0)
                get(array)
                rewind()
                array
            } ?: return@hook
            val unaryEventHandler = param.argNullable<Any>(3) ?: return@hook

            val event = context.event.post(
                UnaryCallEvent(
                    uri = uri,
                    buffer = buffer
                ).apply {
                    adapter = param
                }
            ) ?: return@hook

            if (event.canceled) {
                param.setResult(null)
                return@hook
            }

            if (!event.buffer.contentEquals(buffer)) {
                param.setArg(1, ByteBuffer.allocateDirect(event.buffer.size).apply {
                    put(event.buffer)
                    rewind()
                })
            }

            if (event.callbacks.size == 0) {
                return@hook
            }

            Hooker.ephemeralHookObjectMethod(unaryEventHandler::class.java, unaryEventHandler, "onEvent", HookStage.BEFORE) { methodParam ->
                val byteBuffer = methodParam.argNullable<ByteBuffer>(0) ?: return@ephemeralHookObjectMethod
                val array = byteBuffer.run {
                    val array = ByteArray(limit())
                    position(0)
                    get(array)
                    rewind()
                    array
                }

                val responseUnaryCallEvent = UnaryCallEvent(
                    uri = uri,
                    buffer = array
                ).also { it.context = context}

                event.callbacks.forEach { callback ->
                    callback(responseUnaryCallEvent)
                }

                if (responseUnaryCallEvent.canceled) {
                    param.setResult(null)
                    return@ephemeralHookObjectMethod
                }

                methodParam.setArg(0, ByteBuffer.wrap(responseUnaryCallEvent.buffer))
            }
        }

        arrayOf(
            "com.snap.mushroom.MainActivity",
            "com.snap.identity.loginsignup.ui.LoginSignupActivity"
        ).forEach {
            context.androidContext.classLoader.loadClass(it).hook("onActivityResult", HookStage.BEFORE) { param ->
                val instance = param.thisObject<Activity>()
                val requestCode = param.arg<Int>(0)
                val resultCode = param.arg<Int>(1)
                val intent = param.argNullable<Intent>(2) ?: return@hook

                context.event.post(
                    ActivityResultEvent(
                        activity = instance,
                        requestCode = requestCode,
                        resultCode = resultCode,
                        intent = intent
                    ).apply {
                        adapter = param
                    }
                ) {
                    if (canceled) param.setResult(null)
                    postHookEvent()
                }
            }
        }

        context.mappings.useMapper(CallbackMapper::class) {
            callbacks.getClass("UploadDelegate")?.hook("uploadMedia", HookStage.BEFORE) { param ->
                val uploadCallback = param.arg<Any>(2)

                val event = context.event.post(MediaUploadEvent(
                    localMessageContent = MessageContent(param.arg(0)),
                    destinations = MessageDestinations(param.arg(1)),
                    callback = uploadCallback
                ).apply {
                    adapter = param
                })

                if (event?.canceled == true) {
                    param.setResult(null)
                    return@hook
                }

                event?.mediaUploadCallbacks?.takeIf { it.isNotEmpty() }?.let { callbacks ->
                    Hooker.ephemeralHookObjectMethod(uploadCallback::class.java, uploadCallback, "onUploadFinished", HookStage.BEFORE) { methodParam ->
                        val messageContent = MessageContent(methodParam.arg(1))
                        callbacks.forEach {
                            it(MediaUploadEvent.MediaUploadResult(messageContent))
                        }
                    }
                }
            }
        }

        hookViewBinder()
    }
}