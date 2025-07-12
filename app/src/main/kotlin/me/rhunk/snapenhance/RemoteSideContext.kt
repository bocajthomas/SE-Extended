package me.rhunk.snapenhance

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.CoreComponentFactory
import androidx.documentfile.provider.DocumentFile
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.rhunk.snapenhance.bridge.BridgeService
import me.rhunk.snapenhance.common.BuildConfig
import me.rhunk.snapenhance.common.Constants
import me.rhunk.snapenhance.common.action.EnumAction
import me.rhunk.snapenhance.common.bridge.wrapper.LocaleWrapper
import me.rhunk.snapenhance.common.bridge.wrapper.LoggerWrapper
import me.rhunk.snapenhance.common.bridge.wrapper.MappingsWrapper
import me.rhunk.snapenhance.common.config.ModConfig
import me.rhunk.snapenhance.common.logger.fatalCrash
import me.rhunk.snapenhance.common.util.constantLazyBridge
import me.rhunk.snapenhance.common.util.getPurgeTime
import me.rhunk.snapenhance.e2ee.E2EEImplementation
import me.rhunk.snapenhance.scripting.RemoteScriptManager
import me.rhunk.snapenhance.storage.AppDatabase
import me.rhunk.snapenhance.task.TaskManager
import me.rhunk.snapenhance.ui.manager.MainActivity
import me.rhunk.snapenhance.ui.manager.data.InstallationSummary
import me.rhunk.snapenhance.ui.manager.data.ModInfo
import me.rhunk.snapenhance.ui.manager.data.PlatformInfo
import me.rhunk.snapenhance.ui.manager.data.SnapchatAppInfo
import me.rhunk.snapenhance.ui.overlay.RemoteOverlay
import me.rhunk.snapenhance.ui.setup.Requirements
import me.rhunk.snapenhance.ui.setup.SetupActivity
import java.io.ByteArrayInputStream
import java.lang.ref.WeakReference
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate


class RemoteSideContext(
    val androidContext: Context
) {
    val coroutineScope = CoroutineScope(Dispatchers.IO)

    private var _activity: WeakReference<ComponentActivity>? = null
    var bridgeService: BridgeService? = null

    var activity: ComponentActivity?
        get() = _activity?.get()
        set(value) { _activity?.clear(); _activity = WeakReference(value) }

    val sharedPreferences: SharedPreferences get() = androidContext.getSharedPreferences("prefs", 0)
    val fileHandleManager = RemoteFileHandleManager(this)
    val config = ModConfig(androidContext, constantLazyBridge { fileHandleManager })
    val translation = LocaleWrapper(constantLazyBridge { fileHandleManager })
    val mappings = MappingsWrapper(constantLazyBridge { fileHandleManager })
    val taskManager = TaskManager(this)
    val database = AppDatabase(this)
    val streaksReminder = StreaksReminder(this)
    val log = LogManager(this)
    val scriptManager = RemoteScriptManager(this)
    val remoteOverlay = RemoteOverlay(this)
    val e2eeImplementation = E2EEImplementation(this)
    val messageLogger by lazy { LoggerWrapper(androidContext) }
    val tracker = RemoteTracker(this)
    val accountStorage = RemoteAccountStorage(this)
    val locationManager = RemoteLocationManager(this)

    //used to load bitmoji selfies and download previews
    val imageLoader by lazy {
        ImageLoader.Builder(androidContext)
            .dispatcher(Dispatchers.IO)
            .memoryCache {
                MemoryCache.Builder(androidContext)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(androidContext.cacheDir.resolve("coil-disk-cache"))
                    .maxSizeBytes(1024 * 1024 * 100) // 100MB
                    .build()
            }
            .components { add(VideoFrameDecoder.Factory()) }.build()
    }

    val gson: Gson by lazy { GsonBuilder().setPrettyPrinting().create() }

    fun reload() {
        runCatching {
            runBlocking(Dispatchers.IO) {
                log.init()
                log.verbose("Loading RemoteSideContext")
                config.load()
                launch {
                    mappings.apply {
                        init(androidContext)
                    }
                }
                translation.apply {
                    userLocale = config.locale
                    load()
                }
                database.init()
                streaksReminder.init()
                scriptManager.init()
                launch {
                    taskManager.init()
                    config.root.messaging.messageLogger.takeIf {
                        it.globalState == true
                    }?.autoPurge?.let { getPurgeTime(it.getNullable()) }?.let {
                        messageLogger.purgeAll(it)
                    }

                    config.root.friendTracker.takeIf {
                        it.globalState == true
                    }?.autoPurge?.let { getPurgeTime(it.getNullable()) }?.let {
                        messageLogger.purgeTrackerLogs(it)
                    }
                }
            }
        }.onFailure {
            log.error("Failed to load RemoteSideContext", it)
            androidContext.fatalCrash(it)
        }

        scriptManager.runtime.eachModule {
            callFunction("module.onSnapEnhanceLoad", androidContext)
        }
    }

    val installationSummary by lazy {
        InstallationSummary(
            snapchatInfo = mappings.getSnapchatPackageInfo()?.let {
                SnapchatAppInfo(
                    packageName = it.packageName,
                    version = it.versionName,
                    versionCode = it.longVersionCode,
                    isLSPatched = it.applicationInfo.appComponentFactory != CoreComponentFactory::class.java.name,
                    isSplitApk = it.splitNames?.isNotEmpty() ?: false
                )
            },
            modInfo = ModInfo(
                loaderPackageName = MainActivity::class.java.`package`?.name,
                buildPackageName = androidContext.packageName,
                buildVersion = BuildConfig.VERSION_NAME,
                buildVersionCode = BuildConfig.VERSION_CODE.toLong(),
                buildIssuer = androidContext.packageManager.getPackageInfo(androidContext.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                    ?.signingInfo?.apkContentsSigners?.firstOrNull()?.let {
                        val certFactory = CertificateFactory.getInstance("X509")
                        val cert = certFactory.generateCertificate(ByteArrayInputStream(it.toByteArray())) as X509Certificate
                        cert.issuerDN.toString()
                    } ?: throw Exception("Failed to get certificate info"),
                gitHash = BuildConfig.GIT_HASH,
                isDebugBuild = BuildConfig.DEBUG,
                mappingVersion = mappings.getGeneratedBuildNumber(),
                mappingsOutdated = mappings.isMappingsOutdated()
            ),
            platformInfo = PlatformInfo(
                device = Build.DEVICE,
                androidVersion = Build.VERSION.RELEASE,
                systemAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
            )
        )
    }

    fun longToast(message: Any) {
        androidContext.mainExecutor.execute {
            Toast.makeText(androidContext, message.toString(), Toast.LENGTH_LONG).show()
        }
        log.debug(message.toString())
    }

    fun shortToast(message: Any) {
        androidContext.mainExecutor.execute {
            Toast.makeText(androidContext, message.toString(), Toast.LENGTH_SHORT).show()
        }
        log.debug(message.toString())
    }

    fun hasMessagingBridge() = bridgeService != null && bridgeService?.messagingBridge != null && bridgeService?.messagingBridge?.asBinder()?.pingBinder() == true

    fun checkForRequirements(overrideRequirements: Int? = null): Boolean {
        var requirements = overrideRequirements ?: 0
        if (!config.wasPresent) {
            requirements = requirements or Requirements.FIRST_RUN
        }

        config.root.downloader.saveFolder.get().let {
            if (it.isEmpty() || run {
                    val documentFile = runCatching { DocumentFile.fromTreeUri(androidContext, Uri.parse(it)) }.getOrNull()
                    documentFile == null || !documentFile.exists() || !documentFile.canWrite()
                }) {
                requirements = requirements or Requirements.SAVE_FOLDER
            }
        }

        if (!sharedPreferences.getBoolean("debug_disable_mapper", false) && mappings.getSnapchatPackageInfo() != null && mappings.isMappingsOutdated()) {
            requirements = requirements or Requirements.MAPPINGS
        }

        if (requirements == 0) return false

        val currentContext = activity ?: androidContext

        Intent(currentContext, SetupActivity::class.java).apply {
            putExtra("requirements", requirements)
            if (currentContext !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            currentContext.startActivity(this)
            return true
        }
    }

    fun launchActionIntent(action: EnumAction) {
        val intent = androidContext.packageManager.getLaunchIntentForPackage(
            Constants.SNAPCHAT_PACKAGE_NAME
        )
        if (intent == null) {
            shortToast("Can't execute action: Snapchat is not installed")
            return
        }
        intent.putExtra(EnumAction.ACTION_PARAMETER, action.key)
        androidContext.startActivity(intent)
    }
}
