use std::{
    collections::HashMap,
    ffi::{c_void, CStr},
    mem::size_of,
    ptr::addr_of_mut,
    sync::Mutex,
};

use jni::{
    objects::{JObject, JString},
    JNIEnv,
};
use nix::libc::{self, pthread_mutex_t};
use once_cell::sync::Lazy;

use crate::{common, def_hook, dobby_hook, sig, util::get_jni_string};

#[repr(C)]
#[derive(Clone, Copy, Debug)]
struct Sqlite3Mutex {
    mutex: pthread_mutex_t,
}

#[repr(C)]
struct Sqlite3 {
    pad: [u8; 3 * size_of::<usize>()],
    mutex: *mut Sqlite3Mutex,
}

static SQLITE3_MUTEX_MAP: Lazy<Mutex<HashMap<String, pthread_mutex_t>>> =
    Lazy::new(|| Mutex::new(HashMap::new()));

def_hook!(
    sqlite3_open,
    i32,
    |filename: *const u8, pp_db: *mut *mut Sqlite3, flags: u32, z_vfs: *const i8| {
        let result = sqlite3_open_original.unwrap()(filename, pp_db, flags, z_vfs);

        if result == 0 {
            let sqlite3_mutex = (**pp_db).mutex;

            if sqlite3_mutex != std::ptr::null_mut() {
                let filename = CStr::from_ptr(filename)
                    .to_string_lossy()
                    .to_string()
                    .split("/")
                    .last()
                    .expect("Failed to get filename")
                    .to_string();
                debug!("sqlite3_open hook {:?}", filename);

                SQLITE3_MUTEX_MAP
                    .lock()
                    .unwrap()
                    .insert(filename, (*sqlite3_mutex).mutex);
            }
        }

        result
    }
);

pub fn lock_database(mut env: JNIEnv, _: *mut c_void, filename: JString, runnable: JObject) {
    let database_filename =
        get_jni_string(&mut env, filename).expect("Failed to get database filename");
    let mutex = SQLITE3_MUTEX_MAP
        .lock()
        .unwrap()
        .get(&database_filename)
        .map(|mutex| *mutex);

    let call_runnable = || {
        env.call_method(runnable, "run", "()V", &[])
            .expect("Failed to call run method");
    };

    if let Some(mut mutex) = mutex {
        if unsafe { libc::pthread_mutex_lock(addr_of_mut!(mutex)) } != 0 {
            error!("pthread_mutex_lock failed");
            return;
        }

        call_runnable();

        if unsafe { libc::pthread_mutex_unlock(addr_of_mut!(mutex)) } != 0 {
            error!("pthread_mutex_unlock failed");
        }
    } else {
        warn!("No mutex found for database: {}", database_filename);
        call_runnable();
    }
}

pub fn init() {
    if let Some(signature) = sig::find_signature(
        &common::CLIENT_MODULE,
        "FF FF 00 A9 3F 00 00 F9",
        -0x3C,
        "9A 46 90 46 78 44 89 46 05 68",
        -0xd,
    ) {
        debug!("Found sqlite3_open signature: {:#x}", signature);
        dobby_hook!(signature as *mut c_void, sqlite3_open);
    } else {
        panic!("Failed to find sqlite3_open signature");
    }
}
