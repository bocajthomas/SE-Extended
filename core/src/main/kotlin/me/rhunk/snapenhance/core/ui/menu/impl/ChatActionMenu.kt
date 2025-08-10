package me.rhunk.snapenhance.core.ui.menu.impl

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Button
import android.widget.LinearLayout
import me.rhunk.snapenhance.bridge.logger.LoggedChatEdit
import me.rhunk.snapenhance.core.features.impl.downloader.MediaDownloader
import me.rhunk.snapenhance.core.features.impl.experiments.ConvertMessageLocally
import me.rhunk.snapenhance.core.features.impl.messaging.Messaging
import me.rhunk.snapenhance.core.features.impl.spying.MessageLogger
import me.rhunk.snapenhance.core.ui.ViewTagState
import me.rhunk.snapenhance.core.ui.menu.AbstractMenu
import me.rhunk.snapenhance.core.ui.triggerCloseTouchEvent
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.ktx.vibrateLongPress


class ChatActionMenu : AbstractMenu() {
    private val viewTagState = ViewTagState()
    private val defaultGap by lazy { context.userInterface.dpToPx(8) }
    private val chatActionMenuItemMargin by lazy { context.userInterface.dpToPx(15) }
    private val actionMenuItemHeight by lazy { context.userInterface.dpToPx(45) }

    private fun createRoundedBackground(color: Int, radius: Float, hasRadius: Boolean): Drawable {
        if (!hasRadius) return ColorDrawable(color)
        return ShapeDrawable().apply {
            paint.color = color
            shape = android.graphics.drawable.shapes.RoundRectShape(
                floatArrayOf(radius, radius, radius, radius, radius, radius, radius, radius),
                null,
                null
            )
        }
    }

    private fun createContainer(viewGroup: ViewGroup): LinearLayout {
        return LinearLayout(viewGroup.context).apply layout@{
            orientation = LinearLayout.VERTICAL
            layoutParams = MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                this@ChatActionMenu.context.userInterface.apply {
                    background = createRoundedBackground(actionSheetBackground, 16F, true)
                }
                setMargins(chatActionMenuItemMargin, 0, chatActionMenuItemMargin, defaultGap)
            }
        }
    }

    override fun init() {
        runCatching {
            if (!context.config.downloader.downloadContextMenu.get() && context.config.messaging.messageLogger.globalState != true && !context.isDeveloper) return
            context.androidContext.classLoader.loadClass("com.snap.messaging.chat.features.actionmenu.ActionMenuChatItemContainer")
                .hook("onMeasure", HookStage.BEFORE) { param ->
                    param.setArg(1,
                        View.MeasureSpec.makeMeasureSpec((context.resources.displayMetrics.heightPixels * 0.25).toInt(), View.MeasureSpec.AT_MOST)
                    )
                }
        }.onFailure {
            context.log.error("Failed to hook ActionMenuChatItemContainer: $it")
        }
    }

    override fun inject(parent: ViewGroup, view: View, viewConsumer: (View) -> Unit) {
        val viewGroup = parent.parent.parent as? ViewGroup ?: return
        if (viewTagState[viewGroup]) return
        //close the action menu using a touch event
        val closeActionMenu = {
            context.runOnUiThread {
                parent.triggerCloseTouchEvent()
            }
        }

        val messaging = context.feature(Messaging::class)
        val messageLogger = context.feature(MessageLogger::class)

        val buttonContainer = createContainer(viewGroup)

        val injectButton = { button: Button ->
            if (buttonContainer.childCount > 0) {
                buttonContainer.addView(View(viewGroup.context).apply {
                    layoutParams = MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        height = 1
                    }
                    setBackgroundColor(0x1A000000)
                })
            }

            with(button) {
                this@ChatActionMenu.context.userInterface.apply {
                    background = createRoundedBackground(actionSheetBackground, 16F, true)
                    setTextColor(colorPrimary)
                    typeface = this@ChatActionMenu.context.userInterface.avenirNextTypeface
                }
                isAllCaps = false
                setShadowLayer(0F, 0F, 0F, 0)
                setPadding(chatActionMenuItemMargin, 0, 0, 0)

                gravity = Gravity.CENTER_VERTICAL

                layoutParams = MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    post {
                        width = viewGroup.width
                    }
                    height = actionMenuItemHeight + defaultGap
                }
                buttonContainer.addView(this)
            }
        }

        if (context.config.downloader.downloadContextMenu.get()) {
            val mediaDownloader = context.feature(MediaDownloader::class)

            injectButton(Button(viewGroup.context).apply {
                text = this@ChatActionMenu.context.translation["chat_action_menu.preview_button"]
                setOnClickListener {
                    closeActionMenu()
                    mediaDownloader.onMessageActionMenu(true)
                }
            })

            injectButton(Button(viewGroup.context).apply {
                text = this@ChatActionMenu.context.translation["chat_action_menu.download_button"]
                setOnClickListener {
                    closeActionMenu()
                    mediaDownloader.onMessageActionMenu(false)
                }
                setOnLongClickListener {
                    closeActionMenu()
                    context.vibrateLongPress()
                    mediaDownloader.onMessageActionMenu(isPreviewMode = false, forceAllowDuplicate = true)
                    true
                }
            })
        }

        //delete logged message button
        if (context.config.messaging.messageLogger.globalState == true) {
            injectButton(Button(viewGroup.context).apply {
                text = this@ChatActionMenu.context.translation["chat_action_menu.delete_logged_message_button"]
                setOnClickListener {
                    closeActionMenu()
                    this@ChatActionMenu.context.executeAsync {
                        messageLogger.deleteMessage(messaging.openedConversationUUID.toString(), messaging.lastFocusedMessageId)
                    }
                }
            })

            injectButton(Button(viewGroup.context).apply {
                var chatEdits = emptyList<LoggedChatEdit>()
                text = this@ChatActionMenu.context.translation["chat_action_menu.show_chat_edit_history"]
                setOnClickListener {
                    menuViewInjector.menu(NewChatActionMenu::class)?.showChatEditHistory(chatEdits)
                }
                addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        visibility = View.GONE
                        chatEdits = this@ChatActionMenu.context.feature(MessageLogger::class).getChatEdits(
                            messaging.openedConversationUUID.toString(),
                            messaging.lastFocusedMessageId,
                        )
                        if (chatEdits.isEmpty()) return
                        visibility = View.VISIBLE
                    }

                    override fun onViewDetachedFromWindow(v: View) {
                        visibility = View.GONE
                        chatEdits = emptyList()
                    }
                })
            })
        }

        if (context.config.experimental.convertMessageLocally.get()) {
            injectButton(Button(viewGroup.context).apply {
                text = this@ChatActionMenu.context.translation["chat_action_menu.convert_message"]
                setOnClickListener {
                    closeActionMenu()
                    messaging.conversationManager?.fetchMessage(
                        messaging.openedConversationUUID.toString(),
                        messaging.lastFocusedMessageId,
                        onSuccess = {
                            this@ChatActionMenu.context.runOnUiThread {
                                runCatching {
                                    this@ChatActionMenu.context.feature(ConvertMessageLocally::class)
                                        .convertMessageInterface(it)
                                }.onFailure {
                                    this@ChatActionMenu.context.log.verbose("Failed to convert message: $it")
                                    this@ChatActionMenu.context.shortToast("Failed to edit message: $it")
                                }
                            }
                        },
                        onError = {
                            this@ChatActionMenu.context.shortToast("Failed to fetch message: $it")
                        }
                    )
                }
            })
        }


        viewGroup.addView(buttonContainer)
    }
}