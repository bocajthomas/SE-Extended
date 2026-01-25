package me.rhunk.snapenhance.core.features.impl.messaging

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import me.rhunk.snapenhance.core.event.events.impl.AddViewEvent
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.ui.ViewAppearanceHelper
import me.rhunk.snapenhance.core.ui.children
import me.rhunk.snapenhance.core.ui.hideViewCompletely
import me.rhunk.snapenhance.core.util.hook.HookAdapter
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook

class CallButtonsOverride : Feature("CallButtonsOverride") {
    private fun hookTouchEvent(param: HookAdapter, motionEvent: MotionEvent, onConfirm: () -> Unit) {
        if (motionEvent.action != MotionEvent.ACTION_UP) return
        param.setResult(true)
        ViewAppearanceHelper.newAlertDialogBuilder(context.mainActivity)
            .setTitle(context.translation["call_start_confirmation.dialog_title"])
            .setMessage(context.translation["call_start_confirmation.dialog_message"])
            .setPositiveButton(context.translation["button.positive"]) { _, _ -> onConfirm() }
            .setNeutralButton(context.translation["button.negative"]) { _, _ -> }
            .show()
    }

    override fun init() {
        val hideUiComponents by context.config.userInterface.hideUiComponents

        val hideProfileCallButtons = hideUiComponents.contains("hide_profile_call_buttons")
        val hideChatCallButtons = hideUiComponents.contains("hide_chat_call_buttons")
        val callStartConfirmation = context.config.messaging.callStartConfirmation.get()

        if (!hideProfileCallButtons && !hideChatCallButtons && !callStartConfirmation) return

        var actionSheetVideoCallButtonId = -1
        var actionSheetAudioCallButtonId = -1

        context.event.subscribe(AddViewEvent::class) { event ->
            if (event.viewClassName.endsWith("ConstraintLayout")) {
                val layout = event.view as? ViewGroup ?: return@subscribe
                val children = layout.children()
                if (children.any { !it.javaClass.name.endsWith("FriendActionButton") } || children.size != 4) return@subscribe

                actionSheetVideoCallButtonId = children.getOrNull(2)?.id ?: throw IllegalStateException("Video call button not found")
                actionSheetAudioCallButtonId = children.getOrNull(3)?.id ?: throw IllegalStateException("Audio call button not found")

                if (hideProfileCallButtons) {
                    children.getOrNull(2)?.hideViewCompletely()
                    children.getOrNull(3)?.hideViewCompletely()
                }
            }

            if (event.viewClassName.endsWith("CallButtonsView") && hideChatCallButtons) {
                event.view.hideViewCompletely()
            }
        }

        onNextActivityCreate {
            if (callStartConfirmation) {
                (runCatching { findClass("com.snap.valdi.views.ValdiRootView") }.getOrNull() ?: findClass("com.snap.composer.views.ComposerRootView"))
                    .hook("dispatchTouchEvent", HookStage.BEFORE) { param ->
                    val view = param.thisObject() as? ViewGroup ?: return@hook
                    if (!view.javaClass.name.endsWith("CallButtonsView")) return@hook
                    val childComposerView = view.getChildAt(0) as? ViewGroup ?: return@hook
                    // check if the child composer view contains 2 call buttons
                    if (childComposerView.children().count {
                            it::class.java == childComposerView::class.java
                        } != 2) return@hook
                    hookTouchEvent(param, param.arg(0)) {
                        param.invokeOriginal()
                    }
                }

                findClass("com.snap.ui.view.stackdraw.StackDrawLayout").hook("onTouchEvent", HookStage.BEFORE) { param ->
                    val view = param.thisObject<View>().takeIf { it.id != -1 } ?: return@hook
                    if (view.id != actionSheetAudioCallButtonId && view.id != actionSheetVideoCallButtonId) return@hook

                    hookTouchEvent(param, param.arg(0)) {
                        arrayOf(
                            MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0f, 0f, 0),
                            MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 0f, 0f, 0)
                        ).forEach {
                            param.invokeOriginal(arrayOf(it))
                        }
                    }
                }
            }
        }
    }
}