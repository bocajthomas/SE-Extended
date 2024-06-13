package me.rhunk.snapenhance.common.config.impl

import me.rhunk.snapenhance.common.config.ConfigContainer
import me.rhunk.snapenhance.common.config.ConfigFlag

class Spoof : ConfigContainer(hasGlobalState = true) {
    val overridePlayStoreInstallerPackageName = boolean("play_store_installer_package_name") { requireRestart() }
    val fingerprint = string("fingerprint") { addFlags(ConfigFlag.SENSITIVE); requireRestart() }
    val androidId = string("android_id") { addFlags(ConfigFlag.SENSITIVE); requireRestart() }
    val removeVpnTransportFlag = boolean("remove_vpn_transport_flag") { requireRestart() }
    val removeMockLocationFlag = boolean("remove_mock_location_flag") { requireRestart() }
    val randomizePersistentDeviceToken = boolean("randomize_persistent_device_token") { requireRestart() }
}