package me.rhunk.snapenhance.ui.manager.pages.location

import android.os.Parcel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rhunk.snapenhance.bridge.location.FriendLocation
import me.rhunk.snapenhance.bridge.location.LocationCoordinates
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableStateList
import me.rhunk.snapenhance.common.ui.rememberAsyncUpdateDispatcher
import me.rhunk.snapenhance.common.util.snap.BitmojiSelfie
import me.rhunk.snapenhance.storage.addOrUpdateLocationCoordinate
import me.rhunk.snapenhance.storage.getLocationCoordinates
import me.rhunk.snapenhance.storage.removeLocationCoordinate
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.util.AlertDialogs
import me.rhunk.snapenhance.ui.util.DialogProperties
import me.rhunk.snapenhance.ui.util.coil.BitmojiImage
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class BetterLocationRoot : Routes.Route() {
    private val alertDialogs by lazy { AlertDialogs(context.translation) }

    @Composable
    private fun FriendLocationItem(
        friendLocation: FriendLocation,
        dismiss: () -> Unit
    ) {
        ElevatedCard(onClick = {
            context.config.root.global.betterLocation.coordinates.setAny(friendLocation.latitude to friendLocation.longitude)
            dismiss()
        }, modifier = Modifier.padding(4.dp)) {
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BitmojiImage(
                    context = context,
                    url = BitmojiSelfie.getBitmojiSelfie(
                        friendLocation.bitmojiSelfieId,
                        friendLocation.bitmojiId,
                        BitmojiSelfie.BitmojiSelfieType.NEW_THREE_D
                    ),
                    size = 48,
                    modifier = Modifier.padding(6.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(friendLocation.displayName?.let { "$it (${friendLocation.username})" }
                        ?: friendLocation.username, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = buildString {
                            append(friendLocation.localityPieces.joinToString(", "))
                            append("\n")
                            append("Lat: ${friendLocation.latitude.toFloat()}, Lng: ${friendLocation.longitude.toFloat()}")
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Light,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }

    @Composable
    private fun FriendLocationsDialogs(
        friendsLocation: List<FriendLocation>,
        dismiss: () -> Unit
    ) {
        var search by remember { mutableStateOf("") }
        val filteredFriendsLocation = rememberAsyncMutableStateList(defaultValue = friendsLocation, keys = arrayOf(search)) {
            search.takeIf { it.isNotBlank() }?.let {
                friendsLocation.filter {
                    it.displayName?.contains(search, ignoreCase = true) == true || it.username.contains(search, ignoreCase = true)
                }
            }  ?: friendsLocation
        }

        ElevatedCard(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.padding(top = 32.dp, bottom = 32.dp)
        ) {
            Text(
                translation["teleport_to_friend_title"],
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                value = search,
                onValueChange = { search = it },
                label = { Text(translation["search_bar"]) }
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                item {
                    if (friendsLocation.isEmpty()) {
                        Text(
                            translation["no_friends_map"],
                            fontSize = 16.sp,
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.Light
                        )
                    } else if (filteredFriendsLocation.isEmpty()) {
                        Text(
                            translation["no_friends_found"],
                            fontSize = 16.sp,
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.Light
                        )
                    }
                }
                items(filteredFriendsLocation) { friendLocation ->
                    FriendLocationItem(friendLocation, dismiss)
                }
            }
        }
    }

    override val content: @Composable (NavBackStackEntry) -> Unit = {
        val coordinatesProperty = remember {
            context.config.root.global.betterLocation.getPropertyPair("coordinates")
        }

        val updateDispatcher = rememberAsyncUpdateDispatcher()
        val savedCoordinates = rememberAsyncMutableStateList(
            defaultValue = listOf(),
            updateDispatcher = updateDispatcher
        ) {
            context.database.getLocationCoordinates()
        }
        var showMap by remember { mutableStateOf(false) }
        var addSavedCoordinateDialog by remember { mutableStateOf(false) }
        var showTeleportDialog by remember { mutableStateOf(false) }

        val marker = remember { mutableStateOf<Marker?>(null) }
        val mapView = remember { mutableStateOf<MapView?>(null) }
        var spoofedCoordinates by remember(showTeleportDialog, showMap) { mutableStateOf(coordinatesProperty.value.get() as? Pair<*, *>) }

        fun addSavedCoordinate(id: Int?, locationCoordinates: LocationCoordinates, onSuccess: suspend (id: Int) -> Unit = {}) {
            context.coroutineScope.launch {
                onSuccess(context.database.addOrUpdateLocationCoordinate(id, locationCoordinates))
            }
        }

        if (showTeleportDialog) {
            me.rhunk.snapenhance.ui.util.Dialog(
                properties = DialogProperties(usePlatformDefaultWidth = false),
                onDismissRequest = { showTeleportDialog = false },
                content = {
                    FriendLocationsDialogs(remember { context.locationManager.friendsLocation }) {
                        showTeleportDialog = false
                        context.coroutineScope.launch {
                            context.config.writeConfig()
                        }
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Text(
                translation.format(
                    "spoofed_coordinates_title",
                    "latitude" to ((spoofedCoordinates?.first as? Double)?.toFloat() ?: "0.0").toString(),
                    "longitude" to ((spoofedCoordinates?.second as? Double)?.toFloat() ?: "0.0").toString()
                ),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )

            if (addSavedCoordinateDialog) {
                me.rhunk.snapenhance.ui.util.Dialog(
                    onDismissRequest = { addSavedCoordinateDialog = false },
                    content = {
                        AddCoordinatesDialog(
                            alertDialogs,
                            translation,
                            LocationCoordinates().apply {
                                this.latitude = marker.value?.position?.latitude ?: 0.0
                                this.longitude = marker.value?.position?.longitude ?: 0.0
                            },
                        ) { coordinates ->
                            addSavedCoordinateDialog = false
                            addSavedCoordinate(null, coordinates) {
                                withContext(Dispatchers.Main) {
                                    savedCoordinates.add(0, coordinates.apply { id = it })
                                }
                            }
                        }
                    }
                )
            }

            if (showMap) {
                me.rhunk.snapenhance.ui.util.Dialog(
                    onDismissRequest = { showMap = false },
                    content = {
                        alertDialogs.ChooseLocationDialog(property = coordinatesProperty, marker, mapView, saveCoordinates = {
                            addSavedCoordinateDialog = true
                        }) {
                            showMap = false
                            context.config.writeConfig()
                        }
                        DisposableEffect(Unit) {
                            onDispose {
                                marker.value = null
                            }
                        }
                    }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
            ) {

                item {
                    @Composable
                    fun ConfigToggle(
                        text: String,
                        state: MutableState<Boolean>,
                        onCheckedChange: (Boolean) -> Unit
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = text)
                            Spacer(modifier = Modifier.weight(1f))
                            Switch(
                                checked = state.value,
                                onCheckedChange = {
                                    state.value = it
                                    onCheckedChange(it)
                                }
                            )
                        }
                    }
                    ConfigToggle(
                        translation["spoof_location_toggle"],
                        remember { mutableStateOf(context.config.root.global.betterLocation.spoofLocation.get()) }
                    ) {
                        context.config.root.global.betterLocation.spoofLocation.set(it)
                    }
                    ConfigToggle(
                        translation["suspend_location_updates"],
                        remember { mutableStateOf(context.config.root.global.betterLocation.suspendLocationUpdates.get()) }
                    ) {
                        context.config.root.global.betterLocation.suspendLocationUpdates.set(it)
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = { showMap = true }) {
                            Text(translation["choose_location_button"])
                        }
                        Button(onClick = { showTeleportDialog = true }) {
                            Text(translation["teleport_to_friend_button"])
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            translation["saved_coordinates_title"],
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            lineHeight = 20.sp
                        )
                        IconButton(
                            onClick = {
                                addSavedCoordinateDialog = true
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }
                }
                item {
                    if (savedCoordinates.isEmpty()) {
                        Text(
                            translation["no_saved_coordinates_hint"],
                            fontSize = 16.sp,
                            modifier = Modifier.padding(start = 20.dp),
                            fontWeight = FontWeight.Light
                        )
                    }
                }
                items(savedCoordinates, key = { it.id }) { coordinates ->
                    var mutableCoordinates by remember { mutableStateOf(coordinates) }
                    val isSelected = spoofedCoordinates == mutableCoordinates.latitude to mutableCoordinates.longitude
                    var showDeleteDialog by remember { mutableStateOf(false) }
                    var showEditDialog by remember { mutableStateOf(false) }

                    fun setSpoofedCoordinates() {
                        spoofedCoordinates = mutableCoordinates.latitude to mutableCoordinates.longitude
                        coordinatesProperty.value.setAny(spoofedCoordinates)
                        context.coroutineScope.launch {
                            context.config.writeConfig()
                        }
                    }

                    if (showDeleteDialog) {
                        me.rhunk.snapenhance.ui.util.Dialog(
                            onDismissRequest = { showDeleteDialog = false },
                            content = {
                                alertDialogs.ConfirmDialog(
                                    title = translation["delete_dialog_title"],
                                    message = translation["delete_dialog_message"],
                                    onConfirm = {
                                        showDeleteDialog = false
                                        context.coroutineScope.launch {
                                            context.database.removeLocationCoordinate(coordinates.id)
                                            savedCoordinates.remove(coordinates)
                                        }
                                    },
                                    onDismiss = { showDeleteDialog = false }
                                )
                            }
                        )
                    }

                    if (showEditDialog) {
                        me.rhunk.snapenhance.ui.util.Dialog(
                            onDismissRequest = { showEditDialog = false },
                            content = {
                                AddCoordinatesDialog(
                                    alertDialogs,
                                    translation,
                                    mutableCoordinates
                                ) {
                                    val itemId = coordinates.id
                                    context.coroutineScope.launch {
                                        addSavedCoordinate(itemId, it)
                                    }
                                    Parcel.obtain().apply {
                                        it.writeToParcel(this, 0)
                                        setDataPosition(0)
                                        coordinates.readFromParcel(this)
                                        coordinates.id = itemId
                                        recycle()
                                    }
                                    mutableCoordinates = it
                                    if (isSelected) setSpoofedCoordinates()
                                    showEditDialog = false
                                }
                            }
                        )
                    }

                    ElevatedCard(
                        onClick = {
                            mutableCoordinates = coordinates
                            setSpoofedCoordinates()
                            GeoPoint(coordinates.latitude, coordinates.longitude).also {
                                marker.value?.position = it
                                mapView.value?.controller?.apply {
                                    animateTo(it)
                                    setZoom(16.0)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(5.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .weight(1f)
                            ) {
                                Text(
                                    text = remember(mutableCoordinates) { mutableCoordinates.name },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Light,
                                    fontSize = 16.sp,
                                    lineHeight = 20.sp,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = remember(mutableCoordinates) { "(${mutableCoordinates.latitude.toFloat()}, ${mutableCoordinates.longitude.toFloat()})" },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Light,
                                    fontSize = 12.sp,
                                    lineHeight = 15.sp,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            FilledIconButton(onClick = {
                                showEditDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Delete")
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            FilledIconButton(onClick = {
                                showDeleteDialog = true
                            }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}