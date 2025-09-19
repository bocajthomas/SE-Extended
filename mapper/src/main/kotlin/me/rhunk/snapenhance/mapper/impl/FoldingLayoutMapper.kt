package me.rhunk.snapenhance.mapper.impl

import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22c
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import me.rhunk.snapenhance.mapper.AbstractClassMapper

class FoldingLayoutMapper: AbstractClassMapper("FoldingLayoutMapper") {
    val onLayoutCompletedMethod = string("onLayoutCompletedMethod")
    val shouldScrollToBottomField = string("shouldScrollToBottomField")
    val sizeSparseArrayField = string("sizeSparseArrayField")
    val recyclerViewField = string("recyclerViewField")

    init {
        mapper {
            val foldingLayoutManagerClass = getClass("Lcom/snap/messaging/chat/features/messagelist/FoldingLayoutManager;") ?: return@mapper

            sizeSparseArrayField.set(foldingLayoutManagerClass.fields.firstOrNull { it.type == "Landroid/util/SparseIntArray;" }?.name ?: return@mapper)
            recyclerViewField.set(foldingLayoutManagerClass.fields.firstOrNull { it.type == "Landroidx/recyclerview/widget/RecyclerView;" }?.name ?: return@mapper)

            foldingLayoutManagerClass.methods.firstOrNull {
                it.parameterTypes.size == 1 && it.returnType == "V" && it.implementation?.instructions?.any {
                    ((it as? Instruction35c)?.reference as? MethodReference)?.name == "invoke"
                } == true
            }?.let { method ->
                onLayoutCompletedMethod.set(method.name)
                for (instruction in method.implementation?.instructions ?: return@mapper) {
                    shouldScrollToBottomField.set(((instruction as? Instruction22c)?.reference as? FieldReference)?.takeIf { it.type == "Z" }?.name ?: continue)
                    break
                }
            }
        }
    }
}