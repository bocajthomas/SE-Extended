@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package me.rhunk.snapenhance.ui.setup.screens.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rhunk.snapenhance.common.ui.columnPadding
import me.rhunk.snapenhance.ui.components.LazyColumnBottomSheet
import me.rhunk.snapenhance.ui.setup.screens.SetupScreen
import me.rhunk.snapenhance.ui.util.BottomSheets

class MappingsScreen : SetupScreen() {
    override val shouldDisableBottomBar = true

    @Composable
    override fun Content() {
        val coroutineScope = rememberCoroutineScope()
        var infoText by remember { mutableStateOf(null as String?) }
        var isGenerating by remember { mutableStateOf(false) }

        if (infoText != null) {
            fun dismiss() {
                infoText = null
                goNext()
            }

            LazyColumnBottomSheet(
                onDismiss = { dismiss() },
            ) {
                remember { BottomSheets(context.translation) }.InfoBottomSheet(title = infoText!!) {
                    dismiss()
                }
            }
        }

        LaunchedEffect(Unit) {
            coroutineScope.launch(Dispatchers.IO) {
                if (isGenerating) return@launch
                isGenerating = true
                runCatching {
                    if (context.installationSummary.snapchatInfo == null) {
                        throw Exception(context.translation["setup.mappings.generate_failure_no_snapchat"])
                    }
                    val warnings = context.mappings.refresh()

                    if (warnings.isNotEmpty()) {
                        isGenerating = false
                        infoText = "${warnings.size} warning(s) occurred while generating mappings:\n\n${warnings.joinToString("\n")}".also {
                            context.log.warn(it)
                        }
                        return@launch
                    }

                    withContext(Dispatchers.Main) {
                        goNext()
                    }
                }.onFailure {
                    isGenerating = false
                    infoText = context.translation["setup.mappings.generate_failure"] + "\n\n" + it.message
                    context.log.error("Failed to generate mappings", it)
                }
            }
        }

        if (isGenerating) {
            Column(
                modifier = Modifier.fillMaxSize().columnPadding(staticVertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                DialogText(text = context.translation["setup.mappings.info"])
                CircularWavyProgressIndicator(
                    modifier = Modifier.size(50.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}