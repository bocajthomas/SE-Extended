@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package me.rhunk.snapenhance.ui.setup.screens

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.rhunk.snapenhance.RemoteSideContext

abstract class SetupScreen {
    lateinit var context: RemoteSideContext
    lateinit var allowNext: (canGoNext: Boolean) -> Unit
    lateinit var goNext: () -> Unit
    lateinit var route: String
    var isLastScreen: Boolean = false

    open val bottomBarButtons: @Composable (RowScope.() -> Unit)? = null
    open val shouldDisableBottomBar: Boolean = false
    open val makeBottomBarTransparent: Boolean = false

    @Composable
    fun DialogText(text: String, modifier: Modifier = Modifier) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(16.dp).then(modifier)
        )
    }
    open fun init() {}
    open fun onLeave() {}

    @Composable
    abstract fun Content()
}