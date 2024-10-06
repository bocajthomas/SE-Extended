package me.rhunk.snapenhance.core.ui.menu.impl

import android.view.View
import android.widget.FrameLayout
import me.rhunk.snapenhance.common.ui.OverlayType
import me.rhunk.snapenhance.core.ui.menu.AbstractMenu
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.ktx.getId
import me.rhunk.snapenhance.core.util.ktx.getIdentifier

class SettingsMenu : AbstractMenu() {
    private val hovaHeaderSearchIconId by lazy {
        context.resources.getId("hova_header_search_icon")
    }

    private val ngsChatLabel by lazy {
        context.resources.run {
            getString(getIdentifier("ngs_chat_label", "string"))
        }
    }
    private val customFriendFeedlabel by context.config.userInterface.customLabels.customFriendFeedLabel

    override fun init() {
        context.androidContext.classLoader.loadClass("com.snap.ui.view.SnapFontTextView").hook("setText", HookStage.BEFORE) { param ->
            val view = param.thisObject<View>()
            if ((view.parent as? FrameLayout)?.findViewById<View>(hovaHeaderSearchIconId) != null) {
                view.post {
                    view.setOnClickListener {
                        context.bridgeClient.openOverlay(OverlayType.SETTINGS)
                    }
                }

                if (param.argNullable<String>(0) == ngsChatLabel) {
                    if (customFriendFeedlabel.isNotEmpty()) {
                        param.setArg(0, customFriendFeedlabel)
                    } else {
                        param.setArg(0, "SE Extended")
                    }
                }
            }
        }
    }
}