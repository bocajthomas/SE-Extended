package me.rhunk.snapenhance.scripting

import android.annotation.SuppressLint
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.documentfile.provider.DocumentFile
import me.rhunk.snapenhance.RemoteSideContext
import me.rhunk.snapenhance.bridge.scripting.AutoReloadListener
import me.rhunk.snapenhance.bridge.scripting.IPCListener
import me.rhunk.snapenhance.bridge.scripting.IScripting
import me.rhunk.snapenhance.common.scripting.ScriptRuntime
import me.rhunk.snapenhance.common.scripting.bindings.BindingSide
import me.rhunk.snapenhance.common.scripting.impl.ConfigInterface
import me.rhunk.snapenhance.common.scripting.impl.ConfigTransactionType
import me.rhunk.snapenhance.common.scripting.type.ModuleInfo
import me.rhunk.snapenhance.common.scripting.type.readModuleInfo
import me.rhunk.snapenhance.common.util.ktx.await
import me.rhunk.snapenhance.common.util.ktx.toParcelFileDescriptor
import me.rhunk.snapenhance.scripting.impl.IPCListeners
import me.rhunk.snapenhance.scripting.impl.ManagerIPC
import me.rhunk.snapenhance.scripting.impl.ManagerScriptConfig
import me.rhunk.snapenhance.storage.isScriptEnabled
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream
import kotlin.system.exitProcess

class RemoteScriptManager(
    val context: RemoteSideContext,
) : IScripting.Stub() {
    val runtime = ScriptRuntime(context.androidContext, context.log).apply {
        scripting = this@RemoteScriptManager
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder().build()
    }

    private var autoReloadListener: AutoReloadListener? = null
    private val autoReloadHandler by lazy {
        AutoReloadHandler(context.coroutineScope) {
            runCatching {
                autoReloadListener?.restartApp()
                if (context.config.root.scripting.autoReload.getNullable() == "all") {
                    exitProcess(1)
                }
            }.onFailure {
                context.log.warn("Failed to restart app")
                autoReloadListener = null
            }
        }.apply {
            start()
        }
    }

    private val cachedModuleInfo = mutableMapOf<String, ModuleInfo>()
    private val ipcListeners = IPCListeners()

    fun getSyncedModules(): List<ModuleInfo> {
        return cachedModuleInfo.values.toList()
    }

    fun sync() {
        cachedModuleInfo.clear()
        getScriptFileNames().forEach { name ->
            runCatching {
                getScriptInputStream(name) { stream ->
                    stream?.use {
                        cachedModuleInfo[name] = it.bufferedReader().readModuleInfo()
                    }
                }
            }.onFailure {
                context.log.error("Failed to load module info for $name", it)
            }
        }
    }

    fun init() {
        runtime.buildModuleObject = { module ->
            putConst("currentSide", this, BindingSide.MANAGER.key)
            module.registerBindings(
                ManagerIPC(ipcListeners),
                ManagerScriptConfig(this@RemoteScriptManager)
            )
        }

        sync()
        enabledScripts.forEach { name ->
            runCatching {
                loadScript(name)
            }.onFailure {
                context.log.error("Failed to load script $name", it)
            }
        }
    }

    fun getModulePath(name: String): String? {
        return cachedModuleInfo.entries.find { it.value.name == name }?.key
    }

    fun loadScript(path: String) {
        val content = getScriptContent(path) ?: return
        runtime.load(path, content)
        if (context.config.root.scripting.autoReload.getNullable() != null) {
            autoReloadHandler.addFile(getScriptsFolder()?.findFile(path) ?: return)
        }
    }

    fun unloadScript(scriptPath: String) {
        runtime.unload(scriptPath)
    }

    @SuppressLint("Recycle")
    private fun <R> getScriptInputStream(name: String, callback: (InputStream?) -> R): R {
        val file = getScriptsFolder()?.findFile(name) ?: return callback(null)
        return context.androidContext.contentResolver.openInputStream(file.uri)?.let(callback) ?: callback(null)
    }

    fun getModuleDataFolder(moduleFileName: String): File {
        return context.androidContext.filesDir.resolve("modules").resolve(moduleFileName).also {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
    }

    fun getScriptsFolder() = runCatching {
        DocumentFile.fromTreeUri(context.androidContext, Uri.parse(context.config.root.scripting.moduleFolder.get()))
    }.getOrNull()

    private fun getScriptFileNames(): List<String> {
        return (getScriptsFolder() ?: return emptyList()).listFiles().filter { it.name?.endsWith(".js") ?: false }.map { it.name!! }
    }

    fun importFromUrl(
        url: String,
        filepath: String? = null
    ): ModuleInfo {
        val response = okHttpClient.newCall(Request.Builder().url(url).build()).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to fetch script. Code: ${response.code}")
        }
        response.body.byteStream().use { inputStream ->
            val bufferedInputStream = inputStream.buffered()
            bufferedInputStream.mark(0)
            val moduleInfo = bufferedInputStream.bufferedReader().readModuleInfo()
            bufferedInputStream.reset()

            val scriptPath = filepath ?: (moduleInfo.name + ".js")
            val scriptFile = getScriptsFolder()?.findFile(scriptPath) ?: getScriptsFolder()?.createFile("text/javascript", scriptPath)
                ?: throw Exception("Failed to create script file")

            context.androidContext.contentResolver.openOutputStream(scriptFile.uri, "wt")?.use { output ->
                bufferedInputStream.copyTo(output)
            }

            sync()
            loadScript(scriptPath)
            runtime.removeModule(scriptPath)
            return moduleInfo
        }
    }

    suspend fun checkForUpdate(inputModuleInfo: ModuleInfo): ModuleInfo? {
        return runCatching {
            context.log.verbose("checking for updates for ${inputModuleInfo.name} ${inputModuleInfo.updateUrl}")
            val response = okHttpClient.newCall(Request.Builder().url(inputModuleInfo.updateUrl ?: return@runCatching null).build()).await()
            if (!response.isSuccessful) {
                return@runCatching null
            }
            response.body.byteStream().use { inputStream ->
                val reader = inputStream.buffered().bufferedReader()
                val moduleInfo = reader.readModuleInfo()
                moduleInfo.takeIf {
                   it.version != inputModuleInfo.version
                }
            }
       }.onFailure {
           context.log.error("Failed to check for updates", it)
       }.getOrNull()
    }


    override fun getEnabledScripts(): List<String> {
        return runCatching {
            getScriptFileNames().filter {
                context.database.isScriptEnabled(cachedModuleInfo[it]?.name ?: return@filter false)
            }
        }.onFailure {
            context.log.error("Failed to get enabled scripts", it)
        }.getOrDefault(emptyList())
    }

    override fun getScriptContent(moduleName: String): ParcelFileDescriptor? {
        return getScriptInputStream(moduleName) { it?.toParcelFileDescriptor(context.coroutineScope) }
    }

    override fun registerIPCListener(channel: String, eventName: String, listener: IPCListener) {
        ipcListeners.getOrPut(channel) { mutableMapOf() }.getOrPut(eventName) { mutableSetOf() }.add(listener)
    }

    override fun sendIPCMessage(channel: String, eventName: String, args: Array<out String>) {
        runCatching {
            ipcListeners[channel]?.get(eventName)?.toList()?.forEach {
                it.onMessage(args)
            }
        }.onFailure {
            context.log.error("Failed to send message for $eventName", it)
        }
    }

    override fun configTransaction(
        module: String?,
        action: String,
        key: String?,
        value: String?,
        save: Boolean
    ): String? {
        val scriptConfig = runtime.getModuleByName(module ?: return null)?.getBinding(ConfigInterface::class) ?: return null.also {
            context.log.warn("Failed to get config interface for $module")
        }
        val transactionType = ConfigTransactionType.fromKey(action)

        return runCatching {
            scriptConfig.run {
                if (transactionType == ConfigTransactionType.GET) {
                    return get(key ?: return@runCatching null, value)
                }
                when (transactionType) {
                    ConfigTransactionType.SET -> set(key ?: return@runCatching null, value, save)
                    ConfigTransactionType.SAVE -> save()
                    ConfigTransactionType.LOAD -> load()
                    ConfigTransactionType.DELETE -> deleteConfig()
                    else -> {}
                }
                null
            }
        }.onFailure {
            context.log.error("Failed to perform config transaction", it)
        }.getOrDefault("")
    }

    override fun registerAutoReloadListener(listener: AutoReloadListener?) {
        autoReloadListener = listener
    }
}