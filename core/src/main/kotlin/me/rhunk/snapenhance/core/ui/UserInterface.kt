package me.rhunk.snapenhance.core.ui

import android.content.res.Resources
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import me.rhunk.snapenhance.core.ModContext
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.ktx.isDarkTheme

class UserInterface(
    private val context: ModContext
) {
    private val fontMap = mutableMapOf<Int, Typeface>()

    val colorPrimary get() = if (context.androidContext.isDarkTheme()) 0xfff5f5f5.toInt() else 0xff212121.toInt()
    val actionSheetBackground get() = if (context.androidContext.isDarkTheme()) 0xff1e1e1e.toInt() else 0xffffffff.toInt()

    val avenirNextFontId = 500
    val avenirNextTypeface get() = fontMap[avenirNextFontId] ?: fontMap.entries.minByOrNull { it.key }?.value ?: Typeface.DEFAULT

    fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    @Suppress("unused")
    fun pxToDp(px: Int): Int {
        return (px / context.resources.displayMetrics.density).toInt()
    }

    fun applyActionButtonTheme(view: TextView) {
        view.apply {
            setTextColor(colorPrimary)
            typeface = avenirNextTypeface
            setShadowLayer(0F, 0F, 0F, 0)
            gravity = Gravity.CENTER_VERTICAL
            isAllCaps = false
            textSize = 16f
            outlineProvider = null
            setPadding(dpToPx(12),  dpToPx(15), 0, dpToPx(15))
            setBackgroundColor(0)
        }
    }

    fun init() {
        ResourcesCompat::class.java.hook("getFont", HookStage.BEFORE) { param ->
            val id = param.arg<Int>(1)
            if (id == avenirNextFontId) {
                param.setResult(avenirNextTypeface)
            } else if (fontMap.containsKey(id)) {
                param.setResult(fontMap[id])
            }
        }

        lateinit var unhook: () -> Unit

        unhook = Resources::class.java.hook("getValue", HookStage.AFTER) { param ->
            val typedValue = param.argNullable<TypedValue>(1)?.takeIf {
                it.resourceId != 0 &&
                it.type == TypedValue.TYPE_STRING && it.string?.endsWith(".ttf") == true
            } ?: return@hook

            unhook()

            var offset = typedValue.resourceId.shr(8).shl(8)

            while (true) {
                try {
                    if (context.resources.getResourceTypeName(++offset) != "font") break
                    val font = runCatching { context.resources.getFont(offset) }.getOrNull() ?: break
                    fontMap[font.weight] = font
                } catch (_: Throwable) {
                    break
                }
            }
        }.let { unhooks ->
            { unhooks.forEach { it.unhook() } }
        }
    }
}