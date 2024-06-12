package me.rhunk.snapenhance.core.ui.menu

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import me.rhunk.snapenhance.core.event.events.impl.AddViewEvent
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.features.FeatureLoadParams
import me.rhunk.snapenhance.core.features.impl.COFOverride
import me.rhunk.snapenhance.core.features.impl.messaging.Messaging
import me.rhunk.snapenhance.core.ui.findParent
import me.rhunk.snapenhance.core.ui.menu.impl.*
import me.rhunk.snapenhance.core.util.ktx.getIdentifier
import kotlin.reflect.KClass

@SuppressLint("DiscouragedApi")
class MenuViewInjector : Feature("MenuViewInjector", loadParams = FeatureLoadParams.ACTIVITY_CREATE_ASYNC) {
    private val menuMap by lazy {
        arrayOf(
            NewChatActionMenu(),
            OperaContextActionMenu(),
            OperaViewerIcons(),
            SettingsGearInjector(),
            FriendFeedInfoMenu(),
            ChatActionMenu(),
            SettingsMenu()
        ).associateBy {
            it.context = context
            it.menuViewInjector = this
            it::class
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: AbstractMenu> menu(menuClass: KClass<T>): T? {
        return menuMap[menuClass] as? T
    }

    override fun asyncOnActivityCreate() {
        menuMap.forEach { it.value.init() }

        val messaging = context.feature(Messaging::class)

        val actionSheetItemsContainerLayoutId = context.resources.getIdentifier("action_sheet_items_container", "id")
        val actionMenuTitle = context.resources.getIdentifier("action_menu_title", "id")
        val actionMenu = context.resources.getIdentifier("action_menu", "id")
        val componentsHolder = context.resources.getIdentifier("components_holder", "id")
        val feedNewChat = context.resources.getIdentifier("feed_new_chat", "id")
        val contextMenuButtonIconView = context.resources.getIdentifier("context_menu_button_icon_view", "id")
        val chatActionMenu = context.resources.getIdentifier("chat_action_menu", "id")

        val hasV2ActionMenu = { context.feature(COFOverride::class).hasActionMenuV2 }

        context.event.subscribe(AddViewEvent::class) { event ->
            val originalAddView: (View) -> Unit = {
                event.adapter.invokeOriginal(arrayOf(it, -1,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    ))
                )
            }

            val viewGroup: ViewGroup = event.parent
            val childView: View = event.view
            menuMap[OperaContextActionMenu::class]!!.inject(viewGroup, childView, originalAddView)

            if (event.view.id == actionSheetItemsContainerLayoutId) {
                event.view.post {
                    if (event.parent.findParent(4) {
                        it.findViewById<View>(actionMenuTitle) != null
                    } == null) return@post

                    val views = mutableListOf<View>()
                    menuMap[FriendFeedInfoMenu::class]?.inject(event.parent, event.view) {
                        views.add(it)
                    }
                    views.reversed().forEach { (event.view as ViewGroup).addView(it, 0) }
                }
            }

            if (childView.id == contextMenuButtonIconView) {
                menuMap[OperaViewerIcons::class]!!.inject(viewGroup, childView, originalAddView)
            }

            if (event.parent.id == componentsHolder && childView.id == feedNewChat) {
                menuMap[SettingsGearInjector::class]!!.inject(viewGroup, childView, originalAddView)
                return@subscribe
            }

            if (viewGroup !is LinearLayout && childView.id == chatActionMenu && context.isDeveloper) {
                event.view = LinearLayout(childView.context).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(
                        (menuMap[NewChatActionMenu::class]!! as NewChatActionMenu).createDebugInfoView(childView.context)
                    )
                    addView(event.view)
                }
            }

            if (childView.javaClass.name.endsWith("ChatActionMenuComponent") && hasV2ActionMenu()) {
                (menuMap[NewChatActionMenu::class]!! as NewChatActionMenu).handle(event)
                return@subscribe
            }

            if (viewGroup.javaClass.name.endsWith("ActionMenuChatItemContainer") && !hasV2ActionMenu()) {
                if (viewGroup.parent == null || viewGroup.parent.parent == null) return@subscribe
                menuMap[ChatActionMenu::class]!!.inject(viewGroup, childView, originalAddView)
                return@subscribe
            }

            if (viewGroup !is LinearLayout && childView.id == actionMenu && messaging.lastFocusedConversationType == 1) {
                val injectedLayout = LinearLayout(childView.context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.BOTTOM
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    addView(childView)
                }

                event.parent.post {
                    injectedLayout.addView(ScrollView(injectedLayout.context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            weight = 1f;
                            setMargins(0, 100, 0, 0)
                        }

                        addView(LinearLayout(context).apply {
                            orientation = LinearLayout.VERTICAL
                            menuMap[FriendFeedInfoMenu::class]?.inject(event.parent, injectedLayout) { view ->
                                view.layoutParams = LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                ).apply {
                                    setMargins(0, 5, 0, 5)
                                }
                                addView(view)
                            }
                        })
                    }, 0)
                }

                event.view = injectedLayout
            }
        }
    }
}