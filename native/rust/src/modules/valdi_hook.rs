#![allow(dead_code, unused_imports)]

use super::util::valdi_utils::{ValdiModule, ModuleTag};
use std::{collections::HashMap, ffi::{c_void, CStr}, sync::Mutex};
use jni::{objects::JString, sys::jobject, JNIEnv};
use once_cell::sync::Lazy;
use crate::{common, config, def_hook, dobby_hook, dobby_hook_sym, sig, util::get_jni_string};

const JS_TAG_BIG_DECIMAL: i64 = -11;
const JS_TAG_BIG_INT: i64 = -10;
const JS_TAG_BIG_FLOAT: i64 = -9;
const JS_TAG_SYMBOL: i64 = -8;
const JS_TAG_STRING: i64 = -7;
const JS_TAG_MODULE: i64 = -3;
const JS_TAG_FUNCTION_BYTECODE: i64 = -2;
const JS_TAG_OBJECT: i64 = -1;
const JS_TAG_INT: i64 = 0;
const JS_TAG_BOOL: i64 = 1;
const JS_TAG_NULL: i64 = 2;
const JS_TAG_UNDEFINED: i64 = 3;
const JS_TAG_UNINITIALIZED: i64 = 4;
const JS_TAG_CATCH_OFFSET: i64 = 5;
const JS_TAG_EXCEPTION: i64 = 6;
const JS_TAG_FLOAT64: i64 = 7;

#[repr(C)]
struct JsString {
    /*
    original structure : 
    struct JSString {
        struct JSRefCountHeader {
            int ref_count;
        };
        uint32_t len : 31;
        uint8_t is_wide_char : 1;
        uint32_t hash : 30;
        uint8_t atom_type : 2;
        uint32_t hash_next;

        union {
            uint8_t str8[0];
            uint16_t str16[0];
        } u;
    };
    */
    pad: [u32; 4],
    str8: [u8; 0],
    str16: [u16; 0],
}

#[repr(C)]
#[derive(Copy, Clone)]
union JsValueUnion {
    int32: i32,
    float64: f64,
    ptr: *mut c_void,
}

#[repr(C)]
#[derive(Copy, Clone)]
struct JsValue {
    u: JsValueUnion,
    tag: i64,
}

static AASSET_MAP: Lazy<Mutex<HashMap<usize, Vec<u8>>>> = Lazy::new(|| Mutex::new(HashMap::new()));
static LOADER_DATA: Mutex<Option<String>> = Mutex::new(None);

def_hook!(
    aasset_get_length,
    i32,
    |arg0: *mut c_void| {
        if let Some(buffer) = AASSET_MAP.lock().unwrap().get(&(arg0 as usize)) {
            return buffer.len() as i32;
        }
        aasset_get_length_original.unwrap()(arg0)
    }
);

def_hook!(
    aasset_get_buffer,
    *const c_void,
    |arg0: *mut c_void| {
        if let Some(buffer) = AASSET_MAP.lock().unwrap().get(&(arg0 as usize)) {
            return buffer.as_ptr() as *const c_void;
        }
        aasset_get_buffer_original.unwrap()(arg0)
    }
);

def_hook!(
    aasset_manager_open,
    *mut c_void,
    |arg0: *mut c_void, arg1: *const u8, arg2: i32| {
        let handle = aasset_manager_open_original.unwrap()(arg0, arg1, arg2);

        let path = Lazy::new(|| CStr::from_ptr(arg1).to_str().unwrap());
        if !handle.is_null() && path.starts_with("bridge_observables") {
            let asset_buffer = aasset_get_buffer_original.unwrap()(handle);
            let asset_length = aasset_get_length_original.unwrap()(handle);
            debug!("asset buffer: {:p}, length: {}", asset_buffer, asset_length);

            let loader_data = LOADER_DATA.lock().unwrap().clone().expect("No loader data");

            let archive_buffer: Vec<u8> = std::slice::from_raw_parts(asset_buffer as *const u8, asset_length as usize).to_vec();
            let decompressed = zstd::stream::decode_all(&archive_buffer[..]).expect("Failed to decompress valdi archive");
            let mut valdi_module = ValdiModule::parse(decompressed).expect("Failed to parse valdi module");

            let mut tags = valdi_module.get_tags();
            let mut new_tags = Vec::new();

            for (tag1, _) in tags.iter_mut() {
                let name = tag1.to_string().unwrap();
                if !name.ends_with("src/utils/converter.js") {
                    continue;
                }

                let old_file_name = name.split_once(".").unwrap().0.to_owned() + rand::random::<u32>().to_string().as_str();
                tag1.set_buffer((old_file_name.to_owned() + ".js").as_bytes().to_vec());
                let original_module_path = path.split_once(".").unwrap().0.to_owned() + "/" + &old_file_name;

                let hooked_module = format!("{};module.exports = require(\"{}\");", loader_data, original_module_path);

                new_tags.push(
                    (
                        ModuleTag::new(true, name.as_bytes().to_vec()),
                        ModuleTag::new(true, hooked_module.as_bytes().to_vec())
                    )
                );

                debug!("Valdi loader injected in {}", name);
                break;
            }

            tags.extend(new_tags);
            valdi_module.set_tags(tags);

            let compressed = valdi_module.to_bytes();
            let compressed = zstd::stream::encode_all(&compressed[..], 3).expect("Failed to compress");

            AASSET_MAP.lock().unwrap().insert(handle as usize, compressed);
        }
        handle
    }
);

def_hook!(
    aasset_close,
    c_void,
    |handle: *mut c_void| {
        AASSET_MAP.lock().unwrap().remove(&(handle as usize));
        aasset_close_original.unwrap()(handle)
    }
);

pub fn set_valdi_loader(mut env: JNIEnv, _: *mut c_void, code: JString) {
    let new_code = get_jni_string(&mut env, code).expect("Failed to get loader code");
    LOADER_DATA.lock().unwrap().replace(new_code);
}

pub fn init() {
    if !config::native_config().valdi_hooks {
        return
    }

    dobby_hook_sym!("libandroid.so", "AAsset_getBuffer", aasset_get_buffer);
    dobby_hook_sym!("libandroid.so", "AAsset_getLength", aasset_get_length);
    dobby_hook_sym!("libandroid.so", "AAsset_close", aasset_close);
    dobby_hook_sym!("libandroid.so", "AAssetManager_open", aasset_manager_open);
}

