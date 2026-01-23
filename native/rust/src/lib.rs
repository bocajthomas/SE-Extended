#[macro_use]
extern crate log;

mod common;

mod config;
mod hook;
mod mapped_lib;
mod sig;
mod util;

mod modules;

use android_logger::Config;
use log::LevelFilter;
use modules::{
    custom_font_hook, duplex_hook, fstat_hook, linker_hook, sqlite_hook, unary_call_hook,
    valdi_hook,
};

use jni::objects::{JObject, JString};
use jni::sys::{jint, jstring, JNI_VERSION_1_6};
use jni::{JNIEnv, JavaVM, NativeMethod};
use util::get_jni_string;

use std::ffi::c_void;
use std::thread::JoinHandle;

fn pre_init() {
    debug!("Pre init");
    linker_hook::init();
    custom_font_hook::init();
    fstat_hook::init();
}

fn init(mut env: JNIEnv, _class: JObject, signature_cache: JString) -> jstring {
    debug!("Initializing native lib");

    let start_time = std::time::Instant::now();

    // load signature cache

    if !signature_cache.is_null() {
        let sig_cache_str = get_jni_string(&mut env, signature_cache)
            .expect("Failed to convert mappings to string");

        if let Ok(signature_cache) = serde_json::from_str(sig_cache_str.as_str()) {
            sig::add_signatures(signature_cache);
        } else {
            error!("Failed to load signature cache");
        }
    }

    common::set_native_lib_instance(
        env.new_global_ref(_class)
            .ok()
            .expect("Failed to create global ref"),
    );

    let _ = common::CLIENT_MODULE;

    // initialize modules asynchronously

    let mut threads: Vec<JoinHandle<()>> = Vec::new();

    macro_rules! async_init {
        ($($f:expr),*) => {
            $(
                threads.push(std::thread::spawn(move || {
                    $f;
                }));
            )*
        };
    }

    async_init!(
        duplex_hook::init(),
        unary_call_hook::init(),
        valdi_hook::init(),
        sqlite_hook::init()
    );

    threads.into_iter().for_each(|t| t.join().unwrap());

    info!("native init took {:?}", start_time.elapsed());

    // send back the signature cache
    if let Ok(signature_cache) = serde_json::to_string(&sig::get_signatures()) {
        env.new_string(signature_cache)
            .ok()
            .expect("Failed to create new string")
            .into_raw()
    } else {
        std::ptr::null_mut()
    }
}

#[allow(non_snake_case)]
#[no_mangle]
pub extern "system" fn JNI_OnLoad(_vm: JavaVM, _: *mut c_void) -> jint {
    android_logger::init_once(
        Config::default()
            .with_max_level(LevelFilter::Debug)
            .with_tag("SnapEnhanceNative"),
    );

    info!("JNI_OnLoad called");

    std::panic::set_hook(Box::new(|panic_info| {
        error!("{:?}", panic_info);
    }));

    common::set_java_vm(_vm.get_java_vm_pointer());

    let mut env = _vm.get_env().expect("Failed to get JNIEnv");

    let native_lib_class = env
        .find_class("me/rhunk/snapenhance/nativelib/NativeLib")
        .expect("NativeLib class not found");

    env.register_native_methods(
        native_lib_class,
        &[
            NativeMethod {
                name: "preInit".into(),
                sig: "()V".into(),
                fn_ptr: pre_init as *mut c_void,
            },
            NativeMethod {
                name: "init".into(),
                sig: "(Ljava/lang/String;)Ljava/lang/String;".into(),
                fn_ptr: init as *mut c_void,
            },
            NativeMethod {
                name: "loadConfig".into(),
                sig: "(Lme/rhunk/snapenhance/nativelib/NativeConfig;)V".into(),
                fn_ptr: config::load_config as *mut c_void,
            },
            NativeMethod {
                name: "addLinkerSharedLibrary".into(),
                sig: "(Ljava/lang/String;[B)V".into(),
                fn_ptr: linker_hook::add_linker_shared_library as *mut c_void,
            },
            NativeMethod {
                name: "lockDatabase".into(),
                sig: "(Ljava/lang/String;Ljava/lang/Runnable;)V".into(),
                fn_ptr: sqlite_hook::lock_database as *mut c_void,
            },
            NativeMethod {
                name: "setValdiLoader".into(),
                sig: "(Ljava/lang/String;)V".into(),
                fn_ptr: valdi_hook::set_valdi_loader as *mut c_void,
            },
        ],
    )
    .expect("Failed to register native methods");

    JNI_VERSION_1_6
}
