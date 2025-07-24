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

        DialogText("Welcome to the SE Extended Module."+"\nDisclaimer:"+"\n•Users must take full Responsibility for any risks associated with using this module."+"\n•The SE Extended Module can modify the behaviour of the Snapchat app. Users should carefully review the module's operations and make their own decisions regarding its usage."+"\n•The author nor the forker accepts no responsibility for any warnings or bans that may occur on your snapchat account that may arise from the use of the SE Extended module, and all consequences are to be borne by the users themselves.")

        Column (
            modifier = Modifier.padding(15.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = {
                    goNext()
                }
            ) {
                Text("I Agree", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
