package me.rhunk.snapenhance.common.ui

import android.content.SharedPreferences
import androidx.compose.runtime.*

@Composable
fun SharedPreferences.rememberMutableBooleanPreferenceState(
    key: String,
    defaultValue: Boolean
): MutableState<Boolean> {
    val internalState = remember { mutableStateOf(getBoolean(key, defaultValue)) }
    val state = remember {
        object : MutableState<Boolean> {
            override var value: Boolean
                get() = internalState.value
                set(newValue) {
                    internalState.value = newValue
                    edit().putBoolean(key, newValue).apply()
                }
            override fun component1(): Boolean = value
            override fun component2(): (Boolean) -> Unit = { value = it }
        }
    }
    val preferenceChangeListener = remember {
        SharedPreferences.OnSharedPreferenceChangeListener { sp, changedKey ->
            if (changedKey == key) {
                internalState.value = sp.getBoolean(key, defaultValue)
            }
        }
    }
    DisposableEffect(this, key) {
        registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        onDispose {
            unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        }
    }
    return state
}

@Composable
fun SharedPreferences.rememberMutableStringPreferenceState(
    key: String,
    defaultValue: String
): MutableState<String> {
    val internalState = remember { mutableStateOf(getString(key, defaultValue) ?: defaultValue) }

    val state = remember {
        object : MutableState<String> {
            override var value: String
                get() = internalState.value
                set(newValue) {
                    internalState.value = newValue
                    edit().putString(key, newValue).apply()
                }
            override fun component1(): String = value
            override fun component2(): (String) -> Unit = { value = it }
        }
    }

    val preferenceChangeListener = remember {
        SharedPreferences.OnSharedPreferenceChangeListener { sp, changedKey ->
            if (changedKey == key) {
                internalState.value = sp.getString(key, defaultValue) ?: defaultValue
            }
        }
    }

    DisposableEffect(this, key) {
        registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        onDispose {
            unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        }
    }
    return state
}