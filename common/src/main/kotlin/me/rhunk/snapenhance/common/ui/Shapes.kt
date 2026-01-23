package me.rhunk.snapenhance.common.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Shape

val cardShapeSingle: Shape = RoundedCornerShape(15.dp)
val cardShapeGroupedTop: Shape = RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp, bottomEnd = 5.dp, bottomStart = 5.dp)
val cardShapeGroupedMiddle: Shape = RoundedCornerShape(5.dp)
val cardShapeGroupedBottom: Shape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomEnd = 15.dp, bottomStart = 15.dp)

val bottomSheetShape: Shape =  RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
val bottomSheetClipShape: Shape = RoundedCornerShape(15.dp)

fun cardShape(groupSize: Int, index: Int): Shape {
    return when (groupSize) {
        1 -> cardShapeSingle
        else -> {
            when (index) {
                0 -> cardShapeGroupedTop
                groupSize - 1 -> cardShapeGroupedBottom
                else -> cardShapeGroupedMiddle
            }
        }
    }
}