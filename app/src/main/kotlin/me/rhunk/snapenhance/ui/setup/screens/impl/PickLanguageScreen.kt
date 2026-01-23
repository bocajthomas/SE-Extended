@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package me.rhunk.snapenhance.ui.setup.screens.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.rhunk.snapenhance.common.bridge.wrapper.LocaleWrapper
import me.rhunk.snapenhance.common.ui.cardShape
import me.rhunk.snapenhance.common.ui.lazyColumnContentPadding
import me.rhunk.snapenhance.ui.setup.screens.SetupScreen
import java.util.Locale


class PickLanguageScreen : SetupScreen(){
    private val availableLocales by lazy {
        LocaleWrapper.fetchAvailableLocales(context.androidContext)
    }
    private lateinit var selectedLocaleState: MutableState<String>
    private fun getLocaleDisplayName(locale: String): String {
        locale.split("_").let {
            if (it.size != 2) return Locale(locale).getDisplayName(Locale.getDefault())
            return Locale(it[0], it[1]).getDisplayName(Locale.getDefault())
        }
    }

    private fun reloadTranslation(selectedLocale: String) {
        context.translation.reload(selectedLocale)
    }

    private fun setLocale(locale: String) {
        with(context) {
            config.locale = locale
            config.writeConfig()
            reloadTranslation(locale)
        }
    }

    override fun onLeave() {
        context.config.locale = selectedLocaleState.value
        context.config.writeConfig()
    }

    override fun init() {
        val deviceLocale = Locale.getDefault().toString()
        val initialLocale = availableLocales.firstOrNull {
                locale -> locale == deviceLocale
        } ?: LocaleWrapper.DEFAULT_LOCALE
        context.config.locale = initialLocale
    }

    @Composable
    override fun Content() {
        allowNext(true)
        val listSize = availableLocales.size
        selectedLocaleState = remember { mutableStateOf(context.config.locale) }
        var selectedLocaleValue by selectedLocaleState

        fun handleLocaleSelection(locale: String) {
            selectedLocaleValue = locale
            setLocale(locale)
            reloadTranslation(locale)
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = lazyColumnContentPadding(staticVertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            itemsIndexed(availableLocales) { index, locale ->
                ElevatedCard (
                    modifier = Modifier.fillMaxWidth(),
                    shape = cardShape(groupSize = listSize, index = index),
                ){
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { handleLocaleSelection(locale) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = getLocaleDisplayName(locale),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                        )

                        RadioButton(
                            selected = locale == selectedLocaleValue,
                            onClick = { handleLocaleSelection(locale) },
                        )
                    }
                }
            }
        }
    }
}