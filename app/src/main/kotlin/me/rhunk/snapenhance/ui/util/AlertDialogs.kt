package me.rhunk.snapenhance.ui.util

import android.content.Context
import android.view.MotionEvent
import android.widget.Toast
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.AlphaTile
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.ColorPickerController
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import me.rhunk.snapenhance.common.bridge.wrapper.LocaleWrapper
import me.rhunk.snapenhance.common.config.DataProcessors
import me.rhunk.snapenhance.common.config.PropertyPair
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import java.io.File


class AlertDialogs(
    private val translation: LocaleWrapper,
){
    @Composable
    fun DefaultDialogCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
        Card(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .padding(16.dp)
                .then(modifier),
        ) {
            Column(
                modifier = Modifier
                    .padding(10.dp, 10.dp, 10.dp, 10.dp)
                    .verticalScroll(ScrollState(0)),
            ) { content() }
        }
    }

    @Composable
    fun ConfirmDialog(
        title: String,
        message: String? = null,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit,
    ) {
        DefaultDialogCard {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 5.dp, bottom = 10.dp)
            )
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 15.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(onClick = { onDismiss() }) {
                    Text(text = translation["button.cancel"])
                }
                Button(onClick = { onConfirm() }) {
                    Text(text = translation["button.ok"])
                }
            }
        }
    }

    @Composable
    fun InfoDialog(
        title: String,
        message: String? = null,
        onDismiss: () -> Unit,
    ) {
        DefaultDialogCard {
            Text(
                text = title,
                fontSize = 20.sp,
                modifier = Modifier.padding(start = 5.dp, bottom = 10.dp)
            )
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 15.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(onClick = { onDismiss() }) {
                    Text(text = translation["button.ok"])
                }
            }
        }
    }

    @Composable
    fun TranslatedText(property: PropertyPair<*>, key: String, modifier: Modifier = Modifier) {
        Text(
            text = property.key.propertyOption(translation, key),
            modifier = Modifier
                .padding(10.dp, 10.dp, 10.dp, 10.dp)
                .then(modifier)
        )
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    fun UniqueSelectionDialog(property: PropertyPair<*>) {
        val keys = (property.value.defaultValues as List<String>).toMutableList().apply {
            add(0, "null")
        }

        var selectedValue by remember {
            mutableStateOf(property.value.getNullable()?.toString() ?: "null")
        }

        DefaultDialogCard {
            keys.forEachIndexed { index, item ->
                fun select() {
                    selectedValue = item
                    property.value.setAny(if (index == 0) {
                        null
                    } else {
                        item
                    })
                }

                Row(
                    modifier = Modifier.clickable { select() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TranslatedText(
                        property = property,
                        key = item,
                        modifier = Modifier.weight(1f)
                    )
                    RadioButton(
                        selected = selectedValue == item,
                        onClick = { select() }
                    )
                }
            }
        }
    }

    @Composable
    fun KeyboardInputDialog(property: PropertyPair<*>, dismiss: () -> Unit = {}) {
        val focusRequester = remember { FocusRequester() }
        val context = LocalContext.current

        DefaultDialogCard {
            var fieldValue by remember {
                mutableStateOf(property.value.get().toString().let {
                    TextFieldValue(
                        text = it,
                        selection = TextRange(it.length)
                    )
                })
            }

            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 10.dp)
                    .onGloballyPositioned {
                        focusRequester.requestFocus()
                    }
                    .focusRequester(focusRequester),
                value = fieldValue,
                onValueChange = { fieldValue = it },
                keyboardOptions = when (property.key.dataType.type) {
                    DataProcessors.Type.INTEGER -> KeyboardOptions(keyboardType = KeyboardType.Number)
                    DataProcessors.Type.FLOAT -> KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    else -> KeyboardOptions(keyboardType = KeyboardType.Text)
                },
                singleLine = true
            )

            Row(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(onClick = { dismiss() }) {
                    Text(text = translation["button.cancel"])
                }
                Button(onClick = {
                    if (fieldValue.text.isNotEmpty() && property.key.params.inputCheck?.invoke(fieldValue.text) == false) {
                        Toast.makeText(context, "Invalid input! Make sure you entered a valid value.", Toast.LENGTH_SHORT).show() //TODO: i18n
                        return@Button
                    }

                    when (property.key.dataType.type) {
                        DataProcessors.Type.INTEGER -> {
                            runCatching {
                                property.value.setAny(fieldValue.text.toInt())
                            }.onFailure {
                                property.value.setAny(0)
                            }
                        }
                        DataProcessors.Type.FLOAT -> {
                            runCatching {
                                property.value.setAny(fieldValue.text.toFloat())
                            }.onFailure {
                                property.value.setAny(0f)
                            }
                        }
                        else -> property.value.setAny(fieldValue.text)
                    }
                    dismiss()
                }) {
                    Text(text = translation["button.ok"])
                }
            }
        }
    }

    @Composable
    fun RawInputDialog(onDismiss: () -> Unit, onConfirm: (value: String) -> Unit) {
        val focusRequester = remember { FocusRequester() }

        DefaultDialogCard {
            val fieldValue = remember {
                mutableStateOf(TextFieldValue())
            }

            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 10.dp)
                    .onGloballyPositioned {
                        focusRequester.requestFocus()
                    }
                    .focusRequester(focusRequester),
                value = fieldValue.value,
                onValueChange = {
                    fieldValue.value = it
                },
                singleLine = true
            )

            Row(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(onClick = { onDismiss() }) {
                    Text(text = translation["button.cancel"])
                }
                Button(onClick = {
                    onConfirm(fieldValue.value.text)
                }) {
                    Text(text = translation["button.ok"])
                }
            }
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    fun MultipleSelectionDialog(property: PropertyPair<*>) {
        val defaultItems = property.value.defaultValues as List<String>
        val toggledStates = property.value.get() as MutableList<String>
        DefaultDialogCard {
            defaultItems.forEach { key ->
                var state by remember { mutableStateOf(toggledStates.contains(key)) }

                fun toggle(value: Boolean? = null) {
                    state = value ?: !state
                    if (state) {
                        toggledStates.add(key)
                    } else {
                        toggledStates.remove(key)
                    }
                }

                Row(
                    modifier = Modifier.clickable { toggle() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TranslatedText(
                        property = property,
                        key = key,
                        modifier = Modifier
                            .weight(1f)
                    )
                    Switch(
                        checked = state,
                        onCheckedChange = {
                            toggle(it)
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun ColorPickerDialog(
        initialColor: Color?,
        setProperty: (Color?) -> Unit,
        dismiss: () -> Unit
    ) {
        var currentColor by remember { mutableStateOf(initialColor) }

        DefaultDialogCard {
            val controller = remember { ColorPickerController().apply {
                if (currentColor == null) {
                    setWheelAlpha(1f)
                    setBrightness(1f, false)
                }
            } }
            var colorHexValue by remember {
                mutableStateOf(currentColor?.toArgb()?.let { Integer.toHexString(it) } ?: "")
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                TextField(
                    value = colorHexValue,
                    onValueChange = { value ->
                        colorHexValue = value
                        runCatching {
                            currentColor = Color(android.graphics.Color.parseColor("#$value")).also {
                                controller.selectByColor(it, true)
                                setProperty(it)
                            }
                        }.onFailure {
                            currentColor = null
                        }
                    },
                    label = { Text(text = "Hex Color") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                    )
                )
            }
            HsvColorPicker(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(10.dp),
                initialColor = remember { currentColor },
                controller = controller,
                onColorChanged = {
                    if (!it.fromUser) return@HsvColorPicker
                    currentColor = it.color
                    colorHexValue = Integer.toHexString(it.color.toArgb())
                    setProperty(it.color)
                }
            )
            AlphaSlider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .height(35.dp),
                initialColor = remember { currentColor },
                controller = controller,
            )
            BrightnessSlider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .height(35.dp),
                initialColor = remember { currentColor },
                controller = controller,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlphaTile(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    controller = controller
                )
                IconButton(onClick = {
                    setProperty(null)
                    dismiss()
                }) {
                    Icon(
                        modifier = Modifier.size(60.dp),
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = null
                    )
                }
            }
        }
    }

    @Composable
    fun ColorPickerPropertyDialog(
        property: PropertyPair<*>,
        dismiss: () -> Unit = {},
    ) {
        var currentColor by remember {
            mutableStateOf((property.value.getNullable() as? Int)?.let { Color(it) })
        }

        ColorPickerDialog(
            initialColor = currentColor,
            setProperty = setProperty@{
                currentColor = it
                property.value.setAny(it?.toArgb())
                if (it == null) {
                    property.value.setAny(property.value.defaultValues?.firstOrNull() ?: return@setProperty)
                }
            },
            dismiss = dismiss
        )
    }

    @Composable
    fun ChooseLocationDialog(
        property: PropertyPair<*>,
        marker: MutableState<Marker?> = remember { mutableStateOf(null) },
        mapView: MutableState<MapView?> = remember { mutableStateOf(null) },
        saveCoordinates: (() -> Unit)? = null,
        dismiss: () -> Unit = {}
    ) {
        val coordinates = remember {
            (property.value.get() as Pair<*, *>).let {
                it.first.toString().toDouble() to it.second.toString().toDouble()
            }
        }
        val context = LocalContext.current

        mapView.value = remember {
            Configuration.getInstance().apply {
                osmdroidBasePath = File(context.cacheDir, "osmdroid")
                load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            }
            MapView(context).apply {
                setMultiTouchControls(true)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                setTileSource(TileSourceFactory.MAPNIK)

                val startPoint = GeoPoint(coordinates.first, coordinates.second)
                controller.setZoom(10.0)
                controller.setCenter(startPoint)

                marker.value = Marker(this).apply {
                    isDraggable = true
                    position = startPoint
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }

                overlays.add(object: Overlay() {
                    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
                        marker.value?.position = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                        mapView.invalidate()
                        return true
                    }
                })

                overlays.add(marker.value)
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                mapView.value?.onDetach()
            }
        }

        var customCoordinatesDialog by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
                .fillMaxHeight(fraction = 0.9f),
        ) {
            AndroidView(
                factory = { mapView.value!! },
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilledIconButton(
                    onClick = {
                        val lat = marker.value?.position?.latitude ?: coordinates.first
                        val lon = marker.value?.position?.longitude ?: coordinates.second
                        property.value.setAny(lat to lon)
                        dismiss()
                    }) {
                    Icon(
                        modifier = Modifier
                            .size(60.dp)
                            .padding(5.dp),
                        imageVector = Icons.Filled.Check,
                        contentDescription = null
                    )
                }
                saveCoordinates?.let {
                    FilledIconButton(
                        onClick = { it() }) {
                        Icon(
                            modifier = Modifier
                                .size(60.dp)
                                .padding(5.dp),
                            imageVector = Icons.Filled.Save,
                            contentDescription = null
                        )
                    }
                }

                FilledIconButton(
                    onClick = {
                        customCoordinatesDialog = true
                    }) {
                    Icon(
                        modifier = Modifier
                            .size(60.dp)
                            .padding(5.dp),
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null
                    )
                }
            }

            if (customCoordinatesDialog) {
                val lat = remember { mutableStateOf(coordinates.first.toString()) }
                val lon = remember { mutableStateOf(coordinates.second.toString()) }

                Dialog(onDismissRequest = {
                    customCoordinatesDialog = false
                }) {
                    DefaultDialogCard(
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        TextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(all = 10.dp),
                            value = lat.value,
                            onValueChange = { lat.value = it },
                            label = { Text(text = "Latitude") },
                            singleLine = true
                        )
                        TextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(all = 10.dp),
                            value = lon.value,
                            onValueChange = { lon.value = it },
                            label = { Text(text = "Longitude") },
                            singleLine = true
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            Button(onClick = {
                                customCoordinatesDialog = false
                            }) {
                                Text(text = translation["button.cancel"])
                            }

                            Button(onClick = {
                                marker.value?.position = GeoPoint(lat.value.toDouble(), lon.value.toDouble())
                                mapView.value?.controller?.setCenter(marker.value?.position)
                                mapView.value?.invalidate()
                                customCoordinatesDialog = false
                            }) {
                                Text(text = translation["button.ok"])
                            }
                        }
                    }
                }
            }
        }
    }
}
