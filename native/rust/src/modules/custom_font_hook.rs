use std::{ffi::CStr, fs};

use nix::libc::{self, c_uint};

use crate::{config, def_hook, dobby_hook, modules::util::elf};

def_hook!(open_hook, i32, |path: *const u8,
                           flags: i32,
                           mode: c_uint| {
    if let Ok(pathname) = CStr::from_ptr(path).to_str() {
        if pathname == "/system/fonts/NotoColorEmoji.ttf" {
            if let Some(font_path) = config::native_config().custom_emoji_font_path {
                if fs::metadata(&font_path).is_ok() {
                    return libc::openat(
                        libc::AT_FDCWD,
                        font_path.as_ptr() as *const u8,
                        flags,
                        mode,
                    );
                } else {
                    warn!("custom emoji font path does not exist: {}", font_path);
                }
            }
        }
    }

    open_hook_original.unwrap()(path, flags, mode)
});

pub fn init() {
    if config::native_config().custom_emoji_font_path.is_none() {
        return;
    }

    let libc = elf::Elf::from_maps("/libc.so").expect("Failed to find libc.so in maps");

    if let Some(ptr) = libc.get_symbol_address("open") {
        dobby_hook!(ptr as _, open_hook);
    } else {
        panic!("Failed to find open symbol");
    }
}
