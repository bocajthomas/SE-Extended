use jni::{objects::GlobalRef, JavaVM};
use once_cell::sync::{Lazy, OnceCell};

use crate::mapped_lib::MappedLib;

static NATIVE_LIB_INSTANCE: OnceCell<GlobalRef> = OnceCell::new();
static JAVA_VM: OnceCell<usize> = OnceCell::new();

pub static CLIENT_MODULE: Lazy<MappedLib> = Lazy::new(|| {
    let mut client_module = MappedLib::new("libclient.so".into());

    if let Err(error) = client_module.search() {
        warn!("Unable to find libclient.so: {}", error);

        client_module = MappedLib::new("split_config.arm".into());

        if let Err(error) = client_module.search() {
            panic!("Unable to find split_config.arm: {}", error);
        }
    }

    client_module
});

pub fn set_native_lib_instance(instance: GlobalRef) {
    NATIVE_LIB_INSTANCE
        .set(instance)
        .expect("NativeLib instance already set");
}

pub fn native_lib_instance() -> GlobalRef {
    NATIVE_LIB_INSTANCE
        .get()
        .expect("NativeLib instance not set")
        .clone()
}

pub fn set_java_vm(vm: *mut jni::sys::JavaVM) {
    JAVA_VM.set(vm as usize).expect("JavaVM already set");
}

pub fn java_vm() -> JavaVM {
    unsafe {
        JavaVM::from_raw(*JAVA_VM.get().expect("JavaVM not set") as *mut jni::sys::JavaVM)
            .expect("Failed to get JavaVM")
    }
}

pub fn attach_jni_env(block: impl FnOnce(&mut jni::JNIEnv)) {
    let jvm = java_vm();
    let mut env: jni::AttachGuard = jvm
        .attach_current_thread()
        .expect("Failed to attach to current thread");

    block(&mut env);
}
