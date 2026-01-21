use std::ffi::c_void;

use jni::{objects::JObject, sys::jboolean, JNIEnv};

use crate::{common, def_hook, dobby_hook, util::get_jni_string};

def_hook!(
    is_same_object,
    jboolean,
    |env: JNIEnv, obj1: JObject, obj2: JObject| {
        let mut env = env;

        if obj1.is_null() || obj2.is_null() {
            return is_same_object_original.unwrap()(env, obj1, obj2);
        }

        let class = env.find_class("java/lang/Class").unwrap();

        if !env.is_instance_of(&obj1, class).unwrap() {
            return is_same_object_original.unwrap()(env, obj1, obj2);
        }

        let obj1_class_name = env
            .call_method(&obj1, "getName", "()Ljava/lang/String;", &[])
            .unwrap()
            .l()
            .unwrap()
            .into();
        let class_name =
            get_jni_string(&mut env, obj1_class_name).expect("Failed to get class name");

        if class_name.contains("com.snapchat.client.duplex.MessageHandler") {
            debug!("is_same_object hook: MessageHandler");
            return 0;
        }

        is_same_object_original.unwrap()(env, obj1, obj2)
    }
);

pub fn init() {
    common::attach_jni_env(|env| {
        dobby_hook!(
            (**env.get_native_interface()).IsSameObject.unwrap() as *mut c_void,
            is_same_object
        );
    });
}
