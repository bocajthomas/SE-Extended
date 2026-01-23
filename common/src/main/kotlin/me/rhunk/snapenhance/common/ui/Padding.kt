package me.rhunk.snapenhance.common.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val topAppBarHeight = TopAppBarDefaults.MediumAppBarCollapsedHeight
private val navBarHeight = 80.dp
private val topAppBarSubActionsHeight = 49.dp
private val fabHeight = 70.dp

@Composable
private fun calculateBasePadding(
    staticHorizontal: Dp,
    staticVertical: Dp,
    start: Dp?,
    end: Dp?,
    top: Dp?,
    bottom: Dp?,
    hasSubAction: Boolean = false,
    hasFAB: Boolean = false
): PaddingValues {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomNavHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val systemTopPadding = statusBarHeight + topAppBarHeight + if (hasSubAction) topAppBarSubActionsHeight else 0.dp
    val systemBottomPadding = bottomNavHeight + navBarHeight + if (hasFAB) fabHeight else 0.dp
    val finalTop = top ?: (systemTopPadding + staticVertical)
    val finalBottom = bottom ?: (systemBottomPadding + staticVertical)
    val finalStart = start ?: staticHorizontal
    val finalEnd = end ?: staticHorizontal

    return PaddingValues(
        start = finalStart,
        end = finalEnd,
        top = finalTop,
        bottom = finalBottom
    )
}

@Composable
fun lazyColumnContentPadding(
    staticHorizontal: Dp = 10.dp,
    staticVertical: Dp = 0.dp,
    start: Dp? = null,
    end: Dp? = null,
    top: Dp? = null,
    bottom: Dp? = null,
    hasSubAction: Boolean = false,
    hasFAB: Boolean = false
): PaddingValues {
    return calculateBasePadding(
        staticHorizontal = staticHorizontal,
        staticVertical = staticVertical,
        start = start,
        end = end,
        top = top,
        bottom = bottom,
        hasSubAction = hasSubAction,
        hasFAB = hasFAB
    )
}

@Composable
fun Modifier.screenContentPadding(
    staticHorizontal: Dp = 10.dp,
    staticVertical: Dp = 0.dp,
    start: Dp? = null,
    end: Dp? = null,
    top: Dp? = null,
    bottom: Dp? = null,
    hasSubAction: Boolean = false,
    hasFAB: Boolean = false
): Modifier {
    val finalPaddingValues = calculateBasePadding(
        staticHorizontal = staticHorizontal,
        staticVertical = staticVertical,
        start = start,
        end = end,
        top = top,
        bottom = bottom,
        hasSubAction = hasSubAction,
        hasFAB = hasFAB
    )
    return this.padding(finalPaddingValues)
}

@Composable
fun Modifier.columnPadding(
    staticHorizontal: Dp = 10.dp,
    staticVertical: Dp = 0.dp,
    start: Dp? = null,
    end: Dp? = null,
    top: Dp? = null,
    bottom: Dp? = null,
    hasSubAction: Boolean = false,
    hasFAB: Boolean = false
): Modifier {
    val finalPaddingValues = calculateBasePadding(
        staticHorizontal = staticHorizontal,
        staticVertical = staticVertical,
        start = start,
        end = end,
        top = top,
        bottom = bottom,
        hasSubAction = hasSubAction,
        hasFAB = hasFAB
    )
    return this.padding(finalPaddingValues)
}


// TODO: Refactor just for component padding
@Composable
fun Modifier.componentPadding(
    staticHorizontal: Dp = 10.dp,
    staticVertical: Dp = 0.dp,
    start: Dp? = null,
    end: Dp? = null,
    top: Dp? = null,
    bottom: Dp? = null,
    hasSubAction: Boolean = false,
    hasFAB: Boolean = false
): Modifier {
    val finalPaddingValues = calculateBasePadding(
        staticHorizontal = staticHorizontal,
        staticVertical = staticVertical,
        start = start,
        end = end,
        top = top,
        bottom = bottom,
        hasSubAction = hasSubAction,
        hasFAB = hasFAB
    )
    return this.padding(finalPaddingValues)
}