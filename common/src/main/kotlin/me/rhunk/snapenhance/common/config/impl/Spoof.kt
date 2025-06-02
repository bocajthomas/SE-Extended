package me.rhunk.snapenhance.common.config.impl

import me.rhunk.snapenhance.common.config.ConfigContainer
import me.rhunk.snapenhance.common.config.ConfigFlag

class Spoof : ConfigContainer(hasGlobalState = true) {
    val overridePlayStoreInstallerPackageName = boolean("play_store_installer_package_name") { requireRestart() }
    val removeVpnTransportFlag = boolean("remove_vpn_transport_flag") { requireRestart() }
    val removeMockLocationFlag = boolean("remove_mock_location_flag") { requireRestart() }
}