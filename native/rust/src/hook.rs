use std::sync::Mutex;

pub static MUTEX: Mutex<()> = Mutex::new(());

#[macro_export]
macro_rules! def_hook {
    ($func:ident, $ret:ty, | $($arg:ident : $arg_type:ty),* | $body:block) => {
        paste::item! {
            #[allow(non_upper_case_globals)]
            static mut [<$func _original>]: std::option::Option<extern "C" fn($($arg_type),*) -> $ret> = None;

            fn $func($($arg: $arg_type),*) -> $ret {
                {
                    #[allow(unused_unsafe)]
                    unsafe {
                        $body
                    }
                }
            }
        }
    };
}

#[macro_export]
macro_rules! dobby_hook {
    ($sym:expr, $hook:expr) => {
        paste::item! {
            unsafe {
                if let Ok(_) = crate::hook::MUTEX.lock() {
                    if let Some(ptr) = dobby_rs::hook($sym, $hook as *mut std::ffi::c_void).ok().map(|x| x as *mut std::ffi::c_void) {
                        [<$hook _original>] = std::mem::transmute(ptr);
                    }
                }
            }
        }
    };
}
