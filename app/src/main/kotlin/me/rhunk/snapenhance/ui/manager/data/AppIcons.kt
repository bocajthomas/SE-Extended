/*
 * Copyright (C) 2026 bocajthomas
 * SPDX-License-Identifier: GPL-3.0
*/

package me.rhunk.snapenhance.ui.manager.data

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import me.rhunk.snapenhance.R

data class AppIcon(
    val key: String,
    val aliasClassName: String,
    @DrawableRes val resourceId: Int,
    @ColorRes val colorId: Int
)

object AppIcons {
    const val packageName = "me.rhunk.snapenhance"
    val IconList: List<AppIcon> = listOf(
        AppIcon(
            key = "default",
            aliasClassName = "$packageName.MainActivityAliasDefault",
            resourceId = R.mipmap.default_icon_foreground,
            colorId = R.color.default_icon_background
        ),
        AppIcon (
            key = "king",
            aliasClassName = "$packageName.MainActivityAliasKing",
            resourceId = R.mipmap.king_icon_foreground,
            colorId = R.color.king_icon_background
        ),
        AppIcon(
            key = "inverse",
            aliasClassName = "$packageName.MainActivityAliasInverse",
            resourceId = R.mipmap.inverse_icon_foreground,
            colorId = R.color.inverse_icon_background
        ),
        AppIcon(
            key = "light_blue",
            aliasClassName = "$packageName.MainActivityAliasLightBlue",
            resourceId = R.mipmap.light_blue_icon_foreground,
            colorId = R.color.light_blue_icon_background
        ),
        AppIcon(
            key = "light_green",
            aliasClassName = "$packageName.MainActivityAliasLightGreen",
            resourceId = R.mipmap.light_green_icon_foreground,
            colorId = R.color.light_green_icon_background
        ),
        AppIcon(
            key = "light_pink",
            aliasClassName = "$packageName.MainActivityAliasLightPink",
            resourceId = R.mipmap.light_pink_icon_foreground,
            colorId = R.color.light_pink_icon_background
        ),
        AppIcon(
            key = "dark_purple",
            aliasClassName = "$packageName.MainActivityAliasDarkPurple",
            resourceId = R.mipmap.dark_purple_icon_foreground,
            colorId = R.color.dark_purple_icon_background
        ),
        AppIcon(
            key = "golden_earth",
            aliasClassName = "$packageName.MainActivityAliasGoldenEarth",
            resourceId = R.mipmap.golden_earth_icon_foreground,
            colorId = R.color.golden_earth_icon_background
        ),
        AppIcon(
            key = "honeycomb",
            aliasClassName = "$packageName.MainActivityAliasHoneycomb",
            resourceId = R.mipmap.honeycomb_icon_foreground,
            colorId = R.color.honeycomb_icon_background
        ),
        AppIcon(
            key = "regal_dusk",
            aliasClassName = "$packageName.MainActivityAliasRegalDusk",
            resourceId = R.mipmap.regal_dusk_icon_foreground,
            colorId = R.color.regal_dusk_icon_background
        ),
        AppIcon(
            key = "cloud_nine",
            aliasClassName = "$packageName.MainActivityAliasCloudNine",
            resourceId = R.mipmap.cloud_nine_icon_foreground,
            colorId = R.color.cloud_nine_icon_background
        ),
        AppIcon(
            key = "zen_garden",
            aliasClassName = "$packageName.MainActivityAliasZenGarden",
            resourceId = R.mipmap.zen_garden_icon_foreground,
            colorId = R.color.zen_garden_icon_background
        ),
        AppIcon(
            key = "ocean_flare",
            aliasClassName = "$packageName.MainActivityAliasOceanFlare",
            resourceId = R.mipmap.ocean_flare_icon_foreground,
            colorId = R.color.ocean_flare_icon_background
        ),
        AppIcon(
            key = "canyon_dusk",
            aliasClassName = "$packageName.MainActivityAliasCanyonDusk",
            resourceId = R.mipmap.canyon_dusk_icon_foreground,
            colorId = R.color.canyon_dusk_icon_background
        ),
        AppIcon(
            key = "og_snapenhance",
            aliasClassName = "$packageName.MainActivityAliasOgSnapEnhance",
            resourceId = R.mipmap.og_snapenhance_icon_foreground,
            colorId = R.color.og_snapenhance_icon_background
        )
    )
    fun getIconByKey(key: String): AppIcon {
        return IconList.find { it.key == key } ?: IconList.first()
    }
}