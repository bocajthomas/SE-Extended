package me.rhunk.snapenhance.core.features.impl.experiments

import android.annotation.SuppressLint
import android.location.Location
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.util.LSPatchUpdater
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook

class DeviceSpooferHook: Feature("Device Spoofer")  {
	private fun hookInstallerPackageName() {
		context.androidContext.packageManager::class.java.hook("getInstallerPackageName", HookStage.BEFORE) { param ->
			param.setResult("com.android.vending")
		}
	}

	@SuppressLint("MissingPermission")
	override fun init() {
		// force installer package name for lspatch users
		if (LSPatchUpdater.HAS_LSPATCH) {
			hookInstallerPackageName()
		}

		if (context.config.experimental.spoof.globalState != true) return

		val removeMockLocationFlag by context.config.experimental.spoof.removeMockLocationFlag
		val overridePlayStoreInstallerPackageName by context.config.experimental.spoof.overridePlayStoreInstallerPackageName
		val removeVpnTransportFlag by context.config.experimental.spoof.removeVpnTransportFlag

		//Installer package name
		if(overridePlayStoreInstallerPackageName) {
			hookInstallerPackageName()
		}

		if (removeMockLocationFlag) {
			Location::class.java.hook("isMock", HookStage.BEFORE) { param ->
				param.setResult(false)
			}
		}

		if (removeVpnTransportFlag) {
			ConnectivityManager::class.java.hook("getAllNetworks", HookStage.AFTER) { param ->
				val instance = param.thisObject() as? ConnectivityManager ?: return@hook
				val networks = param.getResult() as? Array<*> ?: return@hook

				param.setResult(networks.filterIsInstance<Network>().filter { network ->
					val capabilities = instance.getNetworkCapabilities(network) ?: return@filter false
					!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
				}.toTypedArray())
			}
		}
	}
}