package me.rhunk.snapenhance.ui.manager.pages.location

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import me.rhunk.snapenhance.bridge.location.LocationCoordinates
import me.rhunk.snapenhance.common.bridge.wrapper.LocaleWrapper
import me.rhunk.snapenhance.ui.util.AlertDialogs


@Composable
fun AddCoordinatesDialog(
    alertDialogs: AlertDialogs,
    translation: LocaleWrapper,
    locationCoordinates: LocationCoordinates,
    confirm: (locationCoordinates: LocationCoordinates) -> Unit
) {
    var savedName by remember {
        mutableStateOf(
            (locationCoordinates.name ?: "").let {
                TextFieldValue(it, selection = TextRange(it.length))
            }
        )
    }
    var savedLatitude by remember { mutableStateOf(locationCoordinates.latitude.toFloat().toString()) }
    var savedLongitude by remember { mutableStateOf(locationCoordinates.longitude.toFloat().toString()) }

    alertDialogs.DefaultDialogCard {
        val focusRequester = remember { FocusRequester() }
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(translation["save_coordinates_dialog_title"], fontSize = 20.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                modifier = Modifier
                    .focusRequester(focusRequester),
                value = savedName,
                onValueChange = { savedName = it },
                label = { Text(translation["saved_name_dialog_hint"]) }
            )

            LaunchedEffect(Unit) {
                delay(200)
                focusRequester.requestFocus()
            }

            OutlinedTextField(
                value = savedLatitude,
                onValueChange = { savedLatitude = it },
                label = { Text(translation["latitude_dialog_hint"]) }
            )
            OutlinedTextField(
                value = savedLongitude,
                onValueChange = { savedLongitude = it },
                label = { Text(translation["longitude_dialog_hint"]) }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        confirm(LocationCoordinates().apply {
                            this.name = savedName.text
                            this.latitude = savedLatitude.toDoubleOrNull() ?: 0.0
                            this.longitude = savedLongitude.toDoubleOrNull() ?: 0.0
                        })
                    },
                    enabled = savedName.text.isNotBlank() && savedLatitude.isNotBlank() && savedLongitude.isNotBlank()
                ) {
                    Text(translation["save_dialog_button"])
                }
            }
        }
    }
}