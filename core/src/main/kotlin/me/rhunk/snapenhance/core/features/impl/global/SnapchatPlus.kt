package me.rhunk.snapenhance.core.features.impl.global

import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.util.dataBuilder
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.hook.hookConstructor
import me.rhunk.snapenhance.mapper.impl.PlusSubscriptionMapper

class SnapchatPlus: Feature("SnapchatPlus") {
    private val originalSubscriptionTime = (System.currentTimeMillis() - 7776000000L)
    private val expirationTimeMillis = (System.currentTimeMillis() + 15552000000L)

    override fun init() {
        val snapchatPlusTier = context.config.global.snapchatPlus.getNullable()

        if (snapchatPlusTier != null) {
            context.mappings.useMapper(PlusSubscriptionMapper::class) {
                classReference.get()?.hookConstructor(HookStage.AFTER) { param ->
                    param.thisObject<Any>().dataBuilder {
                        //subscription tier
                        if (get<Any>(tierField.getAsString()!!)?.javaClass?.isEnum == true) {
                            set(tierField.getAsString()!!, when (snapchatPlusTier) {
                                "not_subscribed" -> "NO_ACCESS"
                                "basic" -> "SNAPCHAT_PLUS"
                                "ad_free" -> "SNAPCHAT_PLUS_AD_FREE"
                                else -> "SNAPCHAT_PLUS"
                            })
                        } else {
                            set(tierField.getAsString()!!, when (snapchatPlusTier) {
                                "not_subscribed" -> 1
                                "basic" -> 2
                                "ad_free" -> 3
                                else -> 2
                            })
                        }

                        //subscription status
                        set(statusField.getAsString()!!, 2)

                        set(originalSubscriptionTimeMillisField.getAsString()!!, originalSubscriptionTime)
                        set(expirationTimeMillisField.getAsString()!!, expirationTimeMillis)
                    }
                }
            }
        }

        if (context.config.experimental.hiddenSnapchatPlusFeatures.get()) {
            findClass("com.snap.plus.FeatureCatalog").methods.last {
                !it.name.contains("init") &&
                it.parameterTypes.isNotEmpty() &&
                it.parameterTypes[0].name != "java.lang.Boolean"
            }.hook(HookStage.BEFORE) { param ->
                val instance = param.thisObject<Any>()
                val firstArg = param.argNullable<Any>(0) ?: return@hook

                instance::class.java.declaredFields.filter { it.type == firstArg::class.java }.forEach {
                    it.isAccessible = true
                    it.set(instance, firstArg)
                }
            }
        }
    }
}