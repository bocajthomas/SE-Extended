package me.rhunk.snapenhance.core.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.Shape
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import me.rhunk.snapenhance.core.SnapEnhance
import me.rhunk.snapenhance.core.wrapper.impl.valdi.ValdiContext
import me.rhunk.snapenhance.core.wrapper.impl.valdi.ValdiViewNode

private val foregroundDrawableListTag = randomTag()

@Suppress("UNCHECKED_CAST")
private fun View.getForegroundDrawables(): MutableMap<String, Drawable> {
    return getTag(foregroundDrawableListTag) as? MutableMap<String, Drawable>
        ?: mutableMapOf<String, Drawable>().also {
        setTag(foregroundDrawableListTag, it)
    }
}

private fun View.updateForegroundDrawable() {
    foreground = ShapeDrawable(object: Shape() {
        override fun draw(canvas: Canvas, paint: Paint) {
            getForegroundDrawables().forEach { (_, drawable) ->
                drawable.draw(canvas)
            }
        }
    })
}

fun View.removeForegroundDrawable(tag: String) {
    getForegroundDrawables().remove(tag)?.let {
        updateForegroundDrawable()
    }
}

fun View.addForegroundDrawable(tag: String, drawable: Drawable) {
    getForegroundDrawables()[tag] = drawable
    updateForegroundDrawable()
}

fun View.triggerCloseTouchEvent() {
    arrayOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP).forEach {
        this.dispatchTouchEvent(
            MotionEvent.obtain(
                SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(),
                it, 0f, 0f, 0
            )
        )
    }
}

fun Activity.triggerRootCloseTouchEvent() {
    findViewById<View>(android.R.id.content).triggerCloseTouchEvent()
}

fun ViewGroup.children(): List<View> {
    val children = mutableListOf<View>()
    for (i in 0 until childCount) {
        children.add(getChildAt(i))
    }
    return children
}

fun View.iterateParent(predicate: (View) -> Boolean) {
    var parent = this.parent as? View ?: return
    while (true) {
        if (predicate(parent)) return
        parent = parent.parent as? View ?: return
    }
}

fun View.findParent(maxIteration: Int = Int.MAX_VALUE, predicate: (View) -> Boolean): View? {
    var parent = this.parent as? View ?: return null
    var iteration = 0
    while (iteration < maxIteration) {
        if (predicate(parent)) return parent
        parent = parent.parent as? View ?: return null
        iteration++
    }
    return null
}


data class LayoutChangeParams(
    val view: View,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val oldLeft: Int,
    val oldTop: Int,
    val oldRight: Int,
    val oldBottom: Int
)

fun View.onLayoutChange(block: (LayoutChangeParams) -> Unit): View.OnLayoutChangeListener {
    return View.OnLayoutChangeListener  { view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
        block(LayoutChangeParams(view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom))
    }.also { addOnLayoutChangeListener (it) }
}

fun View.onAttachChange(onAttach: (View.OnAttachStateChangeListener) -> Unit = {}, onDetach: (View.OnAttachStateChangeListener) -> Unit = {}): View.OnAttachStateChangeListener {
    return object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            onAttach(this)
        }
        override fun onViewDetachedFromWindow(v: View) {
            onDetach(this)
        }
    }.also { addOnAttachStateChangeListener(it) }
}

fun View.hideViewCompletely() {
    fun hide() {
        isEnabled = false
        visibility = View.GONE
        setWillNotDraw(true)

        layoutParams = layoutParams?.apply {
            width = 0
            height = 0
        } ?: return
    }
    hide()
    post { hide() }
    onLayoutChange { hide() }
}

fun View.getValdiViewNode(): ValdiViewNode? {
    if (!SnapEnhance.classCache.valdiView.isInstance(this)) return null

    val viewNode = this::class.java.methods.firstOrNull {
        it.name == "getComposerViewNode" || it.name == "getValdiViewNode"
    }?.invoke(this) ?: return null

    return ValdiViewNode.fromNode(viewNode)
}

fun View.getValdiContext(): ValdiContext? {
    if (!SnapEnhance.classCache.valdiView.isInstance(this)) return null

    return ValdiContext(this::class.java.methods.firstOrNull {
        it.name == "getComposerContext" || it.name == "getValdiContext"
    }?.invoke(this) ?: return null)
}

object ViewAppearanceHelper {
    fun newAlertDialogBuilder(context: Context?) = AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
}
