package me.rhunk.snapenhance.core.features.impl.experiments

import android.location.Location
import android.location.LocationManager
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.rhunk.snapenhance.common.ui.OverlayType
import me.rhunk.snapenhance.common.ui.createComposeView
import me.rhunk.snapenhance.common.util.protobuf.EditorContext
import me.rhunk.snapenhance.common.util.protobuf.ProtoEditor
import me.rhunk.snapenhance.common.util.protobuf.ProtoReader
import me.rhunk.snapenhance.core.event.events.impl.AddViewEvent
import me.rhunk.snapenhance.core.event.events.impl.UnaryCallEvent
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.features.FeatureLoadParams
import me.rhunk.snapenhance.core.util.RandomWalking
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.ktx.getId
import me.rhunk.snapenhance.core.util.ktx.isDarkTheme
import me.rhunk.snapenhance.mapper.impl.CallbackMapper
import java.nio.ByteBuffer
import java.util.UUID

data class FriendLocation(
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val lastUpdated: Long,
    val locality: String?,
    val localityPieces: List<String>
)

class BetterLocation : Feature("Better Location", loadParams = FeatureLoadParams.INIT_SYNC) {
    private val locationHistory = mutableMapOf<String, FriendLocation>()

    private val walkRadius by lazy {
        context.config.global.betterLocation.walkRadius.getNullable()
    }

    private val randomWalking by lazy {
        RandomWalking(walkRadius?.toDoubleOrNull())
    }

    private fun getLat() : Double {
        var spoofedLatitude = context.config.global.betterLocation.coordinates.get().first
        walkRadius?.let {
            spoofedLatitude += randomWalking.current_x
        }
        return spoofedLatitude
    }

    private fun getLong() : Double {
        var spoofedLongitude = context.config.global.betterLocation.coordinates.get().second
        walkRadius?.let {
            spoofedLongitude += randomWalking.current_y
        }
        return spoofedLongitude
    }

    private fun editClientUpdate(editor: EditorContext) {
        val config = context.config.global.betterLocation

        editor.apply {
            // SCVSLocationUpdate
            edit(1) {
                if (config.spoofLocation.get()) {
                    randomWalking.updatePosition()
                    remove(1)
                    remove(2)
                    addFixed32(1, getLat().toFloat()) // lat
                    addFixed32(2, getLong().toFloat()) // lng
                }

                if (config.alwaysUpdateLocation.get()) {
                    remove(7)
                    addVarInt(7, System.currentTimeMillis()) // timestamp
                }
            }

            if (context.config.global.betterLocation.suspendLocationUpdates.get()) {
                remove(1)
            }

            // SCVSDeviceData
            edit(3) {
                config.spoofBatteryLevel.getNullable()?.takeIf { it.isNotEmpty() }?.let {
                    val value = it.toIntOrNull()?.toFloat()?.div(100) ?: return@edit
                    remove(2)
                    addFixed32(2, value)
                    if (value == 100F) {
                        remove(3)
                        addVarInt(3, 1) // devicePluggedIn
                    }
                }

                if (config.spoofHeadphones.get()) {
                    remove(4)
                    addVarInt(4, 1) // headphoneOutput
                    remove(6)
                    addVarInt(6, 1) // isOtherAudioPlaying
                }

                edit(10) {
                    remove(1)
                    addVarInt(1, 4) // type = ALWAYS
                    remove(2)
                    addVarInt(2, 1) // precise = true
                }
            }
        }
    }

    private fun onLocationEvent(protoReader: ProtoReader) {
        protoReader.eachBuffer(3, 1) {
            val userId = UUID(getFixed64(1, 1) ?: return@eachBuffer, getFixed64(1, 2) ?: return@eachBuffer).toString()
            val friendCluster = FriendLocation(
                userId = userId,
                latitude = Float.fromBits(getFixed32(4)).toDouble(),
                longitude = Float.fromBits(getFixed32(5)).toDouble(),
                lastUpdated = getVarInt(7, 2) ?: -1L,
                locality = getString(10),
                localityPieces = mutableListOf<String>().also {
                    forEach { index, wire ->
                        if (index != 11) return@forEach
                        it.add((wire.value as ByteArray).toString(Charsets.UTF_8) )
                    }
                }
            )

            locationHistory[userId] = friendCluster
        }
    }

    private fun openManagementOverlay() {
        context.bridgeClient.getLocationManager().provideFriendsLocation(
            locationHistory.values.toList().mapNotNull { locationHistory ->
                val friendInfo = context.database.getFriendInfo(locationHistory.userId) ?: return@mapNotNull null

                me.rhunk.snapenhance.bridge.location.FriendLocation().also {
                    it.username = friendInfo.mutableUsername ?: return@mapNotNull null
                    it.displayName = friendInfo.displayName
                    it.bitmojiId = friendInfo.bitmojiAvatarId
                    it.bitmojiSelfieId = friendInfo.bitmojiSelfieId
                    it.latitude = locationHistory.latitude
                    it.longitude = locationHistory.longitude
                    it.lastUpdated = locationHistory.lastUpdated
                    it.locality = locationHistory.locality
                    it.localityPieces = locationHistory.localityPieces
                }
            }
        )
        context.bridgeClient.openOverlay(OverlayType.BETTER_LOCATION)
    }

    override fun init() {
        if (context.config.global.betterLocation.globalState != true) return

        val canSpoofLocation = { context.config.global.betterLocation.spoofLocation.get() }

        LocationManager::class.java.apply {
            hook("isProviderEnabled", HookStage.BEFORE, { canSpoofLocation() }) { it.setResult(true) }
            hook("isProviderEnabledForUser", HookStage.BEFORE, { canSpoofLocation() }) { it.setResult(true) }
        }
        Location::class.java.apply {
            hook("getLatitude", HookStage.BEFORE, { canSpoofLocation() }) { it.setResult(getLat()) }
            hook("getLongitude", HookStage.BEFORE, { canSpoofLocation() }) { it.setResult(getLong()) }
        }

        val mapFeaturesRootId = context.resources.getId("map_features_root")

        context.event.subscribe(AddViewEvent::class) { event ->
            if (event.view.id != mapFeaturesRootId) return@subscribe
            val view = event.view as RelativeLayout

            view.addOnAttachStateChangeListener(object: View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    view.addView(createComposeView(view.context) {
                        val darkTheme = remember { context.androidContext.isDarkTheme() }
                        Box(
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            FilledIconButton(
                                modifier = Modifier.size(40.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = if (darkTheme) Color(0xFF1D1D1D) else Color.White,
                                    contentColor = if (darkTheme) Color.White else Color(0xFF151A1A),
                                ),
                                onClick = { openManagementOverlay() }
                            ) {
                                Icon(Icons.Default.EditLocation, contentDescription = null)
                            }
                        }
                    }.apply {
                        layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                            addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                            setMargins(0, (60 * context.resources.displayMetrics.density).toInt(), 0, 0)
                        }
                    })
                }

                override fun onViewDetachedFromWindow(v: View) {}
            })
        }

        context.event.subscribe(UnaryCallEvent::class) { event ->
            if (event.uri == "/snapchat.valis.Valis/SendClientUpdate") {
                event.buffer = ProtoEditor(event.buffer).apply {
                    edit {
                        editEach(1) {
                            editClientUpdate(this)
                        }
                    }
                }.toByteArray()
            }
        }

        context.mappings.useMapper(CallbackMapper::class) {
            callbacks.getClass("ServerStreamingEventHandler")?.hook("onEvent", HookStage.BEFORE) { param ->
                val buffer = param.argNullable<ByteBuffer>(1)?.let {
                    it.position(0)
                    ByteArray(it.capacity()).also { buffer -> it.get(buffer); it.position(0) }
                } ?: return@hook
                onLocationEvent(ProtoReader(buffer))
            }
        }

        findClass("com.snapchat.client.grpc.ClientStreamSendHandler\$CppProxy").hook("send", HookStage.BEFORE) { param ->
            val array = param.arg<ByteBuffer>(0).let {
                it.position(0)
                ByteArray(it.capacity()).also { buffer -> it.get(buffer); it.position(0) }
            }

            param.setArg(0, ProtoEditor(array).apply {
                edit {
                    editClientUpdate(this)
                }
            }.toByteArray().let {
                ByteBuffer.allocateDirect(it.size).put(it).rewind()
            })
        }
    }
}