package me.rhunk.snapenhance.core.features.impl.tweaks

import android.util.SparseIntArray
import androidx.core.util.size
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.ktx.getObjectField
import me.rhunk.snapenhance.core.util.ktx.setObjectField
import me.rhunk.snapenhance.mapper.impl.FoldingLayoutMapper


class PreventMessageListAutoScroll : Feature("PreventMessageListAutoScroll") {
    companion object {
        private const val MIN_SCROLL_ITEMS = 4
    }

    override fun init() {
        if (!context.config.userInterface.preventMessageListAutoScroll.get()) return

        val foldingLayoutManager = findClass("com.snap.messaging.chat.features.messagelist.FoldingLayoutManager")
        val recyclerViewClass = findClass("androidx.recyclerview.widget.RecyclerView")

        val computeVerticalScrollOffsetMethod = recyclerViewClass.getMethod("computeVerticalScrollOffset")
        val computeVerticalScrollExtentMethod = recyclerViewClass.getMethod("computeVerticalScrollExtent")
        val computeVerticalScrollRangeMethod = recyclerViewClass.getMethod("computeVerticalScrollRange")

        context.mappings.useMapper(FoldingLayoutMapper::class) {
            foldingLayoutManager.hook(onLayoutCompletedMethod.getAsString() ?: throw NoSuchMethodError("onLayoutCompleted"), HookStage.BEFORE) { param ->
                val instance = param.thisObject<Any>()
                val shouldScrollToBottom = instance.getObjectField(shouldScrollToBottomField.getAsString()!!) as Boolean

                if (shouldScrollToBottom) {
                    val sparseIntArray = param.thisObject<Any>().getObjectField(sizeSparseArrayField.getAsString()!!) as SparseIntArray
                    val recyclerView = param.thisObject<Any>().getObjectField(recyclerViewField.getAsString()!!)

                    val scrollOffset = computeVerticalScrollRangeMethod.invoke(recyclerView) as Int - (computeVerticalScrollOffsetMethod.invoke(recyclerView) as Int + computeVerticalScrollExtentMethod.invoke(recyclerView) as Int)

                    var layoutSizeSum = 0

                    for (i in 0 until sparseIntArray.size) {
                        if (sparseIntArray.keyAt(i) < MIN_SCROLL_ITEMS) {
                            layoutSizeSum += sparseIntArray.valueAt(i)
                        }
                    }

                    if (scrollOffset <= 0 || scrollOffset > layoutSizeSum) {
                        instance.setObjectField(shouldScrollToBottomField.getAsString()!!, false)
                    }
                }
            }
        }
    }
}