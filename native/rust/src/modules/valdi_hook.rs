use super::util::valdi_utils::{ModuleTag, ValdiModule};
use crate::{config, def_hook, dobby_hook, modules::util::elf, util::get_jni_string};
use jni::{objects::JString, JNIEnv};
use once_cell::sync::Lazy;
use std::{
    collections::HashMap,
    ffi::{c_void, CStr},
    sync::Mutex,
};

static AASSET_MAP: Lazy<Mutex<HashMap<usize, Vec<u8>>>> = Lazy::new(|| Mutex::new(HashMap::new()));
static LOADER_DATA: Mutex<Option<String>> = Mutex::new(None);

def_hook!(aasset_get_length, i32, |arg0: *mut c_void| {
    if let Some(buffer) = AASSET_MAP.lock().unwrap().get(&(arg0 as usize)) {
        return buffer.len() as i32;
    }
    aasset_get_length_original.unwrap()(arg0)
});

def_hook!(aasset_get_buffer, *const c_void, |arg0: *mut c_void| {
    if let Some(buffer) = AASSET_MAP.lock().unwrap().get(&(arg0 as usize)) {
        return buffer.as_ptr() as *const c_void;
    }
    aasset_get_buffer_original.unwrap()(arg0)
});

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

            let archive_buffer: Vec<u8> =
                std::slice::from_raw_parts(asset_buffer as *const u8, asset_length as usize)
                    .to_vec();
            let decompressed = zstd::stream::decode_all(&archive_buffer[..])
                .expect("Failed to decompress valdi archive");
            let mut valdi_module =
                ValdiModule::parse(decompressed).expect("Failed to parse valdi module");

            let mut tags = valdi_module.get_tags();
            let mut new_tags = Vec::new();

            for (tag1, _) in tags.iter_mut() {
                let name = tag1.to_string().unwrap();
                if !name.ends_with("src/utils/converter.js") {
                    continue;
                }

                let old_file_name = name.split_once(".").unwrap().0.to_owned()
                    + rand::random::<u32>().to_string().as_str();
                tag1.set_buffer((old_file_name.to_owned() + ".js").as_bytes().to_vec());
                let original_module_path =
                    path.split_once(".").unwrap().0.to_owned() + "/" + &old_file_name;

                let hooked_module = format!(
                    "{};module.exports = require(\"{}\");",
                    loader_data, original_module_path
                );

                new_tags.push((
                    ModuleTag::new(true, name.as_bytes().to_vec()),
                    ModuleTag::new(true, hooked_module.as_bytes().to_vec()),
                ));

                debug!("Valdi loader injected in {}", name);
                break;
            }

            tags.extend(new_tags);
            valdi_module.set_tags(tags);

            let compressed = valdi_module.to_bytes();
            let compressed =
                zstd::stream::encode_all(&compressed[..], 3).expect("Failed to compress");

            AASSET_MAP
                .lock()
                .unwrap()
                .insert(handle as usize, compressed);
        }
        handle
    }
);

def_hook!(aasset_close, c_void, |handle: *mut c_void| {
    AASSET_MAP.lock().unwrap().remove(&(handle as usize));
    aasset_close_original.unwrap()(handle)
});

pub fn set_valdi_loader(mut env: JNIEnv, _: *mut c_void, code: JString) {
    let new_code = get_jni_string(&mut env, code).expect("Failed to get loader code");
    LOADER_DATA.lock().unwrap().replace(new_code);
}

pub fn init() {
    if !config::native_config().valdi_hooks {
        return;
    }

    let lib_android =
        elf::Elf::from_maps("/libandroid.so").expect("Failed to find libandroid.so in maps");

    if let Some(ptr) = lib_android.get_symbol_address("AAsset_getBuffer") {
        dobby_hook!(ptr as _, aasset_get_buffer);
    } else {
        panic!("Failed to find AAsset_getBuffer symbol");
    }

    if let Some(ptr) = lib_android.get_symbol_address("AAsset_getLength") {
        dobby_hook!(ptr as _, aasset_get_length);
    } else {
        panic!("Failed to find AAsset_getLength symbol");
    }

    if let Some(ptr) = lib_android.get_symbol_address("AAsset_close") {
        dobby_hook!(ptr as _, aasset_close);
    } else {
        panic!("Failed to find AAsset_close symbol");
    }

    if let Some(ptr) = lib_android.get_symbol_address("AAssetManager_open") {
        dobby_hook!(ptr as _, aasset_manager_open);
    } else {
        panic!("Failed to find AAssetManager_open symbol");
    }
}
