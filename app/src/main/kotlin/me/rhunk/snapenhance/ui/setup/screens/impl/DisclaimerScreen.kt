@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package me.rhunk.snapenhance.ui.setup.screens.impl

import android.annotation.SuppressLint
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.rhunk.snapenhance.ui.setup.screens.SetupScreen
import me.rhunk.snapenhance.common.ui.*

class DisclaimerScreen : SetupScreen() {
    @SuppressLint("ApplySharedPref")
    @Composable
    override fun Content() {
        allowNext(true)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(ScrollState(0))
                .columnPadding(staticVertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.PriorityHigh,
                contentDescription = null,
                modifier = Modifier.padding(16.dp).size(40.dp),
            )

            Text(
                text = context.translation["setup.disclaimer.title"],
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = context.translation["setup.disclaimer.subtitle"],
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal,
                )

                Text(
                    text = context.translation["setup.disclaimer.statement"],
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
        }
    }
    override val makeBottomBarTransparent = true
    override val bottomBarButtons: @Composable (RowScope.() -> Unit) ? = {
        Button(
            onClick = { goNext() },
            shapes = ButtonDefaults.shapes()
        ) {
            Text(context.translation["setup.disclaimer.agree_button"])
        }
    }
}
