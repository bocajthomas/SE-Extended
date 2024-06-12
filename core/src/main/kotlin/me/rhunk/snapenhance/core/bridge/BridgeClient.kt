package me.rhunk.snapenhance.core.bridge


import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import me.rhunk.snapenhance.bridge.*
import me.rhunk.snapenhance.bridge.e2ee.E2eeInterface
import me.rhunk.snapenhance.bridge.location.LocationManager
import me.rhunk.snapenhance.bridge.logger.LoggerInterface
import me.rhunk.snapenhance.bridge.logger.TrackerInterface
import me.rhunk.snapenhance.bridge.scripting.IScripting
import me.rhunk.snapenhance.bridge.snapclient.MessagingBridge
import me.rhunk.snapenhance.bridge.storage.FileHandleManager
import me.rhunk.snapenhance.common.Constants
import me.rhunk.snapenhance.common.data.MessagingFriendInfo
import me.rhunk.snapenhance.common.data.MessagingGroupInfo
import me.rhunk.snapenhance.common.data.MessagingRuleType
import me.rhunk.snapenhance.common.data.SocialScope
import me.rhunk.snapenhance.common.ui.OverlayType
import me.rhunk.snapenhance.common.util.toSerialized
import me.rhunk.snapenhance.core.ModContext
import java.util.concurrent.Executors
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class BridgeClient(
    private val context: ModContext
):  ServiceConnection {
    private var continuation: Continuation<Boolean>? = null
    private val connectSemaphore = Semaphore(permits = 1)
    private val reconnectSemaphore = Semaphore(permits = 1)
    private lateinit var service: BridgeInterface

    private val onConnectedCallbacks = mutableListOf<suspend () -> Unit>()
    private var cacheSnapEnhanceApkPath: String? = null

    fun addOnConnectedCallback(callback: suspend () -> Unit) {
        synchronized(onConnectedCallbacks) {
            onConnectedCallbacks.add(callback)
        }
    }

    private fun resumeContinuation(state: Boolean) {
        runBlocking {
            connectSemaphore.withPermit {
                runCatching { continuation?.resume(state) }
                continuation = null
            }
        }
    }

    suspend fun connect(onFailure: (Throwable) -> Unit): Boolean? {
        if (this::service.isInitialized && service.asBinder().pingBinder()) {
            return true
        }

        return withTimeoutOrNull(10000L) {
            suspendCancellableCoroutine { cancellableContinuation ->
                continuation = cancellableContinuation
                with(context.androidContext) {
                    //ensure the remote process is running
                    runCatching {
                        startActivity(Intent()
                            .setClassName(Constants.SE_PACKAGE_NAME, "me.rhunk.snapenhance.bridge.ForceStartActivity")
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                        )
                    }

                    runCatching {
                        val intent = Intent()
                            .setClassName(Constants.SE_PACKAGE_NAME, "me.rhunk.snapenhance.bridge.BridgeService")
                        runCatching {
                            if (this@BridgeClient::service.isInitialized) {
                                unbindService(this@BridgeClient)
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            bindService(
                                intent,
                                Context.BIND_AUTO_CREATE,
                                Executors.newSingleThreadExecutor(),
                                this@BridgeClient
                            )
                        } else {
                            XposedHelpers.callMethod(
                                this,
                                "bindServiceAsUser",
                                intent,
                                this@BridgeClient,
                                Context.BIND_AUTO_CREATE,
                                Handler(HandlerThread("BridgeClient").apply {
                                    start()
                                }.looper),
                                Process.myUserHandle()
                            )
                        }
                    }.onFailure {
                        onFailure(it)
                        resumeContinuation(false)
                    }
                }
            }
        }
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        this.service = BridgeInterface.Stub.asInterface(service)
        runBlocking {
            onConnectedCallbacks.forEach {
                runCatching {
                    it()
                }.onFailure {
                    context.log.error("Failed to run onConnectedCallback", it)
                }
            }
        }
        cacheSnapEnhanceApkPath = this.service.applicationApkPath.also {
            if (cacheSnapEnhanceApkPath != null && cacheSnapEnhanceApkPath != it) {
                context.log.verbose("Restarting Snapchat due to SnapEnhance update")
                context.softRestartApp()
                return
            }
        }
        resumeContinuation(true)
    }

    override fun onNullBinding(name: ComponentName) {
        resumeContinuation(false)
    }

    override fun onServiceDisconnected(name: ComponentName) {
        continuation = null
    }

    private fun tryReconnect() {
        runBlocking {
            reconnectSemaphore.withPermit {
                if (service.asBinder().pingBinder()) return@runBlocking
                Log.d("BridgeClient", "service is dead, restarting")
                val canLoad = connect {
                    Log.e("BridgeClient", "connection failed", it)
                    context.softRestartApp()
                }
                if (canLoad != true) {
                    Log.e("BridgeClient", "failed to reconnect to service, result=$canLoad")
                    context.softRestartApp()
                }
            }
        }
    }

    private fun <T> safeServiceCall(block: () -> T): T {
        return runCatching {
            block()
        }.getOrElse { throwable ->
            if (throwable is DeadObjectException) {
                tryReconnect()
                return@getOrElse runCatching {
                    block()
                }.getOrElse {
                    Log.e("BridgeClient", "service call failed", it)
                    if (it is DeadObjectException) {
                        context.softRestartApp()
                    }
                    throw it
                }
            }
            throw throwable
        }
    }

    fun broadcastLog(tag: String, level: String, message: String) {
        message.chunked(1024 * 256).forEach {
            safeServiceCall {
                service.broadcastLog(tag, level, it)
            }
        }
    }

    fun getApplicationApkPath(): String = safeServiceCall { service.applicationApkPath }

    fun enqueueDownload(intent: Intent, callback: DownloadCallback) = safeServiceCall {
        service.enqueueDownload(intent, callback)
    }

    fun convertMedia(
        input: ParcelFileDescriptor,
        inputExtension: String,
        outputExtension: String,
        audioCodec: String?,
        videoCodec: String?
    ): ParcelFileDescriptor? = safeServiceCall {
        service.convertMedia(input, inputExtension, outputExtension, audioCodec, videoCodec)
    }

    fun sync(callback: SyncCallback) {
        if (!context.database.hasMain()) return
        safeServiceCall {
            service.sync(callback)
        }
    }

    fun triggerSync(scope: SocialScope, id: String) = safeServiceCall {
        service.triggerSync(scope.key, id)
    }

    fun passGroupsAndFriends(groups: List<MessagingGroupInfo>, friends: List<MessagingFriendInfo>) =
        safeServiceCall {
            service.passGroupsAndFriends(
                groups.mapNotNull { it.toSerialized() },
                friends.mapNotNull { it.toSerialized() }
            )
        }

    fun getRules(targetUuid: String): List<MessagingRuleType> = safeServiceCall {
        service.getRules(targetUuid).mapNotNull { MessagingRuleType.getByName(it) }
    }

    fun getRuleIds(ruleType: MessagingRuleType): List<String> = safeServiceCall {
        service.getRuleIds(ruleType.key)
    }

    fun setRule(targetUuid: String, type: MessagingRuleType, state: Boolean) = safeServiceCall {
        service.setRule(targetUuid, type.key, state)
    }

    fun getScriptingInterface(): IScripting = safeServiceCall { service.scriptingInterface }

    fun getE2eeInterface(): E2eeInterface = safeServiceCall { service.e2eeInterface }

    fun getMessageLogger(): LoggerInterface = safeServiceCall { service.logger }

    fun getTracker(): TrackerInterface = safeServiceCall { service.tracker }

    fun getAccountStorage(): AccountStorage = safeServiceCall { service.accountStorage }

    fun getFileHandlerManager(): FileHandleManager = safeServiceCall { service.fileHandleManager }

    fun getLocationManager(): LocationManager = safeServiceCall { service.locationManager }

    fun registerMessagingBridge(bridge: MessagingBridge) = safeServiceCall { service.registerMessagingBridge(bridge) }

    fun openOverlay(type: OverlayType) = safeServiceCall { service.openOverlay(type.key) }
    fun closeOverlay() = safeServiceCall { service.closeOverlay() }

    fun registerConfigStateListener(listener: ConfigStateListener) = safeServiceCall { service.registerConfigStateListener(listener) }

    fun getDebugProp(name: String, defaultValue: String? = null): String? = safeServiceCall { service.getDebugProp(name, defaultValue) }
}
