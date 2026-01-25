package me.rhunk.snapenhance.ui.manager.pages.home

import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.navigation.NavBackStackEntry
import me.rhunk.snapenhance.common.ui.cardShapeGroupedBottom
import me.rhunk.snapenhance.common.ui.cardShapeGroupedTop
import me.rhunk.snapenhance.common.ui.lazyColumnContentPadding
import me.rhunk.snapenhance.common.ui.rememberMutableBooleanPreferenceState
import me.rhunk.snapenhance.ui.manager.Routes

class HomeAdvancedDebugSettings : Routes.Route() {
    @Composable
    private fun PreferenceToggle(sharedPreferences: SharedPreferences, key: String, text: String) {
        val realKey = "debug_$key"
        var value by remember { mutableStateOf(sharedPreferences.getBoolean(realKey, false)) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 55.dp)
                .clickable {
                    value = !value
                    sharedPreferences
                        .edit() {
                            putBoolean(realKey, value)
                        }
                }
                .padding(start = 15.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = text, fontSize = 14.sp)
            Switch(checked = value, onCheckedChange = {
                value = it
                sharedPreferences.edit().putBoolean(realKey, it).apply()
            })
        }
    }
    override val content: @Composable (NavBackStackEntry) -> Unit = {
        var promoMode = context.sharedPreferences.rememberMutableBooleanPreferenceState(
            key = "promo_mode",
            defaultValue = false
        )
        val getPromoMode by promoMode

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = lazyColumnContentPadding(staticVertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.Start
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShapeGroupedTop
                    ) {
                        PreferenceToggle(context.sharedPreferences, "test_mode", "Test Mode")
                    }

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShapeGroupedBottom
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { }
                                .heightIn(min = 55.dp)
                                .padding(start = 15.dp, end = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = "Enable Promo Mode",
                                    fontSize = 14.sp
                                )
                            }

                            Switch(
                                checked = getPromoMode,
                                onCheckedChange = { newState ->
                                    promoMode.value = newState
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}