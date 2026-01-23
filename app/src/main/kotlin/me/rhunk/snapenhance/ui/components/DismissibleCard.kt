/*
 * Copyright (C) 2026 bocajthomas
 * SPDX-License-Identifier: GPL-3.0
*/

package me.rhunk.snapenhance.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DismissibleCardItem(
    onDelete: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.settledValue) {
        if (state.settledValue != SwipeToDismissBoxValue.Settled) {
            onDelete()
            state.reset()
        }
    }

    Box(modifier = modifier) {
        SwipeToDismissBox(
            state = state,
            backgroundContent = { ExpressiveBackground(state) },
            content = {
                Surface(
                    shape = shape,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    content()
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpressiveBackground(state: SwipeToDismissBoxState) {
    val direction = state.dismissDirection
    if (direction == SwipeToDismissBoxValue.Settled) return

    val density = LocalDensity.current
    val willDismiss = state.targetValue != SwipeToDismissBoxValue.Settled
    val offset = try { state.requireOffset().absoluteValue } catch (e: Exception) { 0f }
    val fullWidthDp = with(density) { offset.toDp() }

    val iconScale by animateFloatAsState(
        targetValue = if (willDismiss) 1.3f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd)
            Alignment.CenterStart else Alignment.CenterEnd
    ) {
        Box(
            modifier = Modifier
                .width(fullWidthDp)
                .fillMaxHeight()
                .padding(
                    end = if (direction == SwipeToDismissBoxValue.StartToEnd) 2.dp else 0.dp,
                    start = if (direction == SwipeToDismissBoxValue.EndToStart) 2.dp else 0.dp
                )
                .clip(CircleShape)
                .background(Color(0xFFF2B8B5)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = Color(0xFF601410),
                modifier = Modifier.scale(iconScale)
            )
        }
    }
}