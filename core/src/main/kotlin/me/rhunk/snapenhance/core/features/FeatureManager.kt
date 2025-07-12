package me.rhunk.snapenhance.core.features

import android.app.Activity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.rhunk.snapenhance.core.ModContext
import me.rhunk.snapenhance.core.features.impl.*
import me.rhunk.snapenhance.core.features.impl.downloader.MediaDownloader
import me.rhunk.snapenhance.core.features.impl.downloader.ProfilePictureDownloader
import me.rhunk.snapenhance.core.features.impl.experiments.*
import me.rhunk.snapenhance.core.features.impl.global.*
import me.rhunk.snapenhance.core.features.impl.messaging.*
import me.rhunk.snapenhance.core.features.impl.spying.FriendTracker
import me.rhunk.snapenhance.core.features.impl.spying.HalfSwipeNotifier
import me.rhunk.snapenhance.core.features.impl.spying.MessageLogger
import me.rhunk.snapenhance.core.features.impl.spying.StealthMode
import me.rhunk.snapenhance.core.features.impl.tweaks.*
import me.rhunk.snapenhance.core.features.impl.ui.*
import me.rhunk.snapenhance.core.logger.CoreLogger
import me.rhunk.snapenhance.core.ui.menu.MenuViewInjector
import kotlin.reflect.KClass
import kotlin.system.measureTimeMillis

class FeatureManager(
    private val context: ModContext
) {
    private val features = mutableMapOf<KClass<out Feature>, Feature>()
    private val onActivityCreateListeners = mutableListOf<(Activity) -> Unit>()

    fun addActivityCreateListener(block: (Activity) -> Unit) {
        onActivityCreateListeners.add(block)
    }

    private fun register(vararg featureList: Feature) {
        if (context.bridgeClient.getDebugProp("disable_feature_loading") == "true") {
            context.log.warn("Feature loading is disabled")
            return
        }

        runBlocking {
            featureList.forEach { feature ->
                launch(Dispatchers.IO) {
                    runCatching {
                        feature.context = context
                        feature.registerNextActivityCallback = { block -> onActivityCreateListeners.add(block) }
                        synchronized(features) {
                            features[feature::class] = feature
                        }
                    }.onFailure {
                        CoreLogger.xposedLog("Failed to register feature ${feature.key}", it)
                    }
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Feature> get(featureClass: KClass<T>): T? {
        return features[featureClass] as? T
    }

    fun getRuleFeatures() = features.values.filterIsInstance<MessagingRuleFeature>().sortedBy { it.ruleType.ordinal }

    fun init() {
        register(
            Debug(),
            EndToEndEncryption(),
            ScopeSync(),
            PreventMessageListAutoScroll(),
            Messaging(),
            FriendMutationObserver(),
            AutoMarkAsRead(),
            MediaDownloader(),
            StealthMode(),
            MenuViewInjector(),
            MessageLogger(),
            ConvertMessageLocally(),
            SnapchatPlus(),
            DisableMetrics(),
            PreventMessageSending(),
            Notifications(),
            AutoSave(),
            UITweaks(),
            ConfigurationOverride(),
            COFOverride(),
            UnsaveableMessages(),
            SendOverride(),
            UnlimitedSnapViewTime(),
            BypassVideoLengthRestriction(),
            MediaUploadQualityOverride(),
            MeoPasscodeBypass(),
            AppLock(),
            CameraTweaks(),
            InfiniteStoryBoost(),
            PinConversations(),
            DeviceSpooferHook(),
            ClientBootstrapOverride(),
            GooglePlayServicesDialogs(),
            NoFriendScoreDelay(),
            ProfilePictureDownloader(),
            AddFriendSourceSpoof(),
            DisableReplayInFF(),
            OldBitmojiSelfie(),
            FriendFeedMessagePreview(),
            HideStreakRestore(),
            HideFriendFeedEntry(),
            RequerySqlite(),
            CallButtonsOverride(),
            SnapPreview(),
            BypassScreenshotDetection(),
            HalfSwipeNotifier(),
            DisableConfirmationDialogs(),
            MixerStories(),
            MessageIndicators(),
            EditTextOverride(),
            PreventForcedLogout(),
            ConversationToolbox(),
            SpotlightCommentsUsername(),
            OperaViewerParamsOverride(),
            StealthModeIndicator(),
            DisablePermissionRequests(),
            FriendTracker(),
            DefaultVolumeControls(),
            CallRecorder(),
            DisableMemoriesSnapFeed(),
            AccountSwitcher(),
            RemoveGroupsLockedStatus(),
            BypassMessageActionRestrictions(),
            BetterLocation(),
            MediaFilePicker(),
            HideActiveMusic(),
            AutoOpenSnaps(),
            CustomStreaksExpirationFormat(),
            ComposerHooks(),
            DisableCustomTabs(),
            BestFriendPinning(),
            ContextMenuFix(),
            DisableTelecomFramework(),
            BetterTranscript(),
            VoiceNoteOverride(),
            FriendNotes(),
            DoubleTapChatAction(),
            SnapScoreChanges(),
            DisableSnapModeRestrictions(),
        )

        features.values.toList().forEach { feature ->
            runCatching {
                measureTimeMillis {
                    feature.init()
                }
            }.onFailure {
                context.log.error("Failed to init feature ${feature.key}", it)
                context.longToast("Failed to init feature ${feature.key}! Check logcat for more details.")
            }
        }
    }

    fun onActivityCreate(activity: Activity) {
        context.log.verbose("Activity created: ${activity.javaClass.simpleName}")
        onActivityCreateListeners.toList().also {
            onActivityCreateListeners.clear()
        }.forEach { activityListener ->
            measureTimeMillis {
                runCatching {
                    activityListener(activity)
                }.onFailure {
                    context.log.error("Failed to run activity listener ${activityListener::class.simpleName}", it)
                }
            }
        }
    }
}
