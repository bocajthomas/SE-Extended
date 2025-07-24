package me.rhunk.snapenhance.ui.setup.screens.impl

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
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

class DisclaimerScreen : SetupScreen() {
    @SuppressLint("ApplySharedPref")
    @Composable
    override fun Content() {
        Icon(
            imageVector = Icons.Rounded.PriorityHigh,
            contentDescription = null,
            modifier = Modifier
                .padding(16.dp)
                .size(40.dp),
        )

        DialogText(
            text = context.translation["setup.dialogs.disclaimer.statement"],
        )

        Column (
            modifier = Modifier.padding(15.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = {
                    goNext()
                }
            ) {
                Text(context.translation["setup.dialogs.disclaimer.agree_button"], fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
