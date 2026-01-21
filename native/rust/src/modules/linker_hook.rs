use std::{
    collections::HashMap,
    ffi::{c_void, CStr},
    sync::Mutex,
};

use jni::{
    objects::{JByteArray, JString},
    JNIEnv,
};
use nix::libc;
use once_cell::sync::Lazy;

use crate::{def_hook, dobby_hook, modules::util::elf};

static SHARED_LIBRARIES: Lazy<Mutex<HashMap<String, Box<Vec<i8>>>>> =
    Lazy::new(|| Mutex::new(HashMap::new()));

def_hook!(
    linker_openat,
    i32,
    |dir_fd: i32, pathname: *mut u8, flags: i32, mode: i32| {
        let pathname_str = CStr::from_ptr(pathname).to_str().unwrap().to_string();

        if let Some(content) = SHARED_LIBRARIES.lock().unwrap().remove(&pathname_str) {
            let memfd = libc::syscall(libc::SYS_memfd_create, "jit-cache\0".as_ptr(), 0) as i32;
            let content = content.into_boxed_slice();

            if libc::write(
                memfd,
                content.as_ptr() as *const c_void,
                content.len() as libc::size_t,
            ) == -1
            {
                panic!("failed to write to memfd");
            }

            if libc::lseek(memfd, 0, libc::SEEK_SET) == -1 {
                panic!("failed to seek memfd");
            }

            std::mem::forget(content);

            info!("opened shared library: {}", pathname_str);
            return memfd;
        }

        linker_openat_original.unwrap()(dir_fd, pathname, flags, mode)
    }
);

pub fn add_linker_shared_library(
    mut env: JNIEnv,
    _: *mut c_void,
    path: JString,
    content: JByteArray,
) {
    let path = env.get_string(&path).unwrap().to_str().unwrap().to_string();
    let content_length = env
        .get_array_length(&content)
        .expect("Failed to get array length");
    let mut content_buffer = Box::new(vec![0i8; content_length as usize]);

    env.get_byte_array_region(content, 0, content_buffer.as_mut_slice())
        .expect("Failed to get byte array region");

    debug!("added shared library: {}", path);

    SHARED_LIBRARIES
        .lock()
        .unwrap()
        .insert(path, content_buffer);
}

pub fn init() {
    let linker = {
        #[cfg(target_arch = "aarch64")]
        {
            elf::Elf::from_maps("/linker64").expect("Failed to find linker64 in maps")
        }
        #[cfg(target_arch = "arm")]
        {
            elf::Elf::from_maps("/linker").expect("Failed to find linker in maps")
        }
    };

    if let Some(ptr) = linker.get_symbol_address("__dl___openat") {
        dobby_hook!(ptr as _, linker_openat);
    } else {
        panic!("Failed to find open symbol");
    }
}
