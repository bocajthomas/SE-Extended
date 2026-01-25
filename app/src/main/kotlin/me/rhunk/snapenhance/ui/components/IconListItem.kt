/*
 * Copyright (C) 2026 bocajthomas
 * SPDX-License-Identifier: GPL-3.0
*/
package me.rhunk.snapenhance.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import me.rhunk.snapenhance.ui.manager.data.AppIcon
import me.rhunk.snapenhance.ui.manager.data.AppIcons
import me.rhunk.snapenhance.RemoteSideContext

@Composable
fun IconSelector(context: RemoteSideContext, onIconSelected: (AppIcon) -> Unit) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    LazyColumn(
        contentPadding = PaddingValues(10.dp),
        modifier = Modifier
            .heightIn(max = screenHeight / 1.5f)
            .fillMaxWidth()
    ) {
        items(AppIcons.IconList) { icon ->
            IconListItem(context = context, iconKey = icon) {
                onIconSelected(icon)
            }
        }
    }
}

@Composable
fun IconListItem(context: RemoteSideContext, iconKey: AppIcon, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconResourceID  = iconKey.resourceId
        val iconColorID = iconKey.colorId

        Box(
            modifier = Modifier
                .size(45.dp)
                .clip(CircleShape)
                .background(colorResource(iconColorID)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = iconResourceID),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = context.translation["manager.app_icons.${iconKey.key}"], style = MaterialTheme.typography.bodyLarge)
    }
}