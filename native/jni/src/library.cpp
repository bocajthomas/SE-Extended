#include <jni.h>
#include <string>
#include <dobby.h>
#include <vector>
#include <thread>

#include "logger.h"
#include "common.h"
#include "dobby_helper.h"
#include "hooks/unary_call.h"
#include "hooks/fstat_hook.h"
#include "hooks/sqlite_mutex.h"
#include "hooks/duplex_hook.h"
#include "hooks/composer_hook.h"
#include "hooks/custom_emoji_font.h"

bool JNICALL init(JNIEnv *env, jobject clazz) {
    LOGD("Initializing native");
    using namespace common;
    util::remap_sections([](const std::string &line, size_t size) {
        return line.find(BUILD_PACKAGE) != std::string::npos;
    }, native_config->remap_executable);

    native_lib_object = env->NewGlobalRef(clazz);
    client_module = util::get_module("libclient.so");

    if (client_module.base == 0) {
        LOGD("libclient.so not found, trying split_config");
        client_module = util::get_module(("split_config." + std::string(ARM64 ? "arm64_v8a" : "armeabi-v7a") + ".apk").c_str());
        if (client_module.base == 0) {
            LOGE("can't find split_config!");
            return false;
        }
    }

    LOGD("client_module offset=0x%lx, size=0x%zx", client_module.base, client_module.size);

    auto threads = std::vector<std::thread>();

    #define RUN(body) threads.push_back(std::thread([&] { body; }))

    RUN(UnaryCallHook::init(env));
    RUN(FstatHook::init());
    RUN(SqliteMutexHook::init());
    RUN(DuplexHook::init(env));
    if (common::native_config->custom_emoji_font_path[0] != 0) {
        RUN(CustomEmojiFont::init());
    }
    if (common::native_config->composer_hooks) {
        RUN(ComposerHook::init());
    }

    for (auto &thread : threads) {
        thread.join();
    }

    LOGD("Native initialized");
    return true;
}

void JNICALL load_config(JNIEnv *env, jobject, jobject config_object) {
    auto native_config_clazz = env->GetObjectClass(config_object);
#define GET_CONFIG_BOOL(name) env->GetBooleanField(config_object, env->GetFieldID(native_config_clazz, name, "Z"))
    auto native_config = common::native_config;

    native_config->disable_bitmoji = GET_CONFIG_BOOL("disableBitmoji");
    native_config->disable_metrics = GET_CONFIG_BOOL("disableMetrics");
    native_config->composer_hooks = GET_CONFIG_BOOL("composerHooks");
    native_config->remap_executable = GET_CONFIG_BOOL("remapExecutable");

    memset(native_config->custom_emoji_font_path, 0, sizeof(native_config->custom_emoji_font_path));
    auto custom_emoji_font_path = env->GetObjectField(config_object, env->GetFieldID(native_config_clazz, "customEmojiFontPath", "Ljava/lang/String;"));
    if (custom_emoji_font_path != nullptr) {
        auto custom_emoji_font_path_str = env->GetStringUTFChars((jstring) custom_emoji_font_path, nullptr);
        strncpy(native_config->custom_emoji_font_path, custom_emoji_font_path_str, sizeof(native_config->custom_emoji_font_path));
        env->ReleaseStringUTFChars((jstring) custom_emoji_font_path, custom_emoji_font_path_str);
    }
}

void JNICALL lock_database(JNIEnv *env, jobject, jstring database_name, jobject runnable) {
    auto database_name_str = env->GetStringUTFChars(database_name, nullptr);
    auto mutex = SqliteMutexHook::mutex_map[database_name_str];
    env->ReleaseStringUTFChars(database_name, database_name_str);

    if (mutex != nullptr) {
        auto lock_result = pthread_mutex_lock(&mutex->mutex);
        if (lock_result != 0) {
            LOGE("pthread_mutex_lock failed: %d", lock_result);
            return;
        }
    }

    env->CallVoidMethod(runnable, env->GetMethodID(env->GetObjectClass(runnable), "run", "()V"));

    if (mutex != nullptr) {
        pthread_mutex_unlock(&mutex->mutex);
    }
}

void JNICALL hide_anonymous_dex_files(JNIEnv *, jobject) {
    util::remap_sections([](const std::string &line, size_t size) {
        return (
            (common::native_config->remap_executable && size == PAGE_SIZE && line.find("r-xp 00000000 00") != std::string::npos && line.find("[v") == std::string::npos) ||
            line.find("dalvik-DEX") != std::string::npos ||
            line.find("dalvik-classes") != std::string::npos
        );
    }, common::native_config->remap_executable);
}

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *_) {
    common::java_vm = vm;
    JNIEnv *env = nullptr;
    vm->GetEnv((void **)&env, JNI_VERSION_1_6);

    auto methods = std::vector<JNINativeMethod>();
    methods.push_back({"init", "()Z", (void *)init});
    methods.push_back({"loadConfig", "(L" BUILD_NAMESPACE "/NativeConfig;)V", (void *)load_config});
    methods.push_back({"lockDatabase", "(Ljava/lang/String;Ljava/lang/Runnable;)V", (void *)lock_database});
    methods.push_back({"setComposerLoader", "(Ljava/lang/String;)V", (void *) ComposerHook::setComposerLoader});
    methods.push_back({"composerEval", "(Ljava/lang/String;)Ljava/lang/String;",(void *) ComposerHook::composerEval});
    methods.push_back({"hideAnonymousDexFiles", "()V", (void *)hide_anonymous_dex_files});

    env->RegisterNatives(env->FindClass(std::string(BUILD_NAMESPACE "/NativeLib").c_str()), methods.data(), methods.size());
    return JNI_VERSION_1_6;
}
