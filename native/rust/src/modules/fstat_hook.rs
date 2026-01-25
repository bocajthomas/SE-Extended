use crate::{
    config::{self, native_config},
    def_hook, dobby_hook,
    modules::util::elf,
};
use nix::libc;
use std::fs;

def_hook!(fstat_hook, i32, |fd: i32, statbuf: *mut libc::stat| {
    if let Ok(link) = fs::read_link("/proc/self/fd/".to_owned() + &fd.to_string()) {
        if let Some(filename) = link.file_name().map(|t| t.to_string_lossy()) {
            let config = native_config();
            if config.disable_metrics && filename.contains("files/blizzardv2/queues") {
                if libc::unlink((filename.to_owned() + "\0").as_ptr()) == -1 {
                    warn!("Failed to unlink {}", filename);
                }
                return -1;
            }

            if config.disable_bitmoji && filename.contains("com.snap.file_manager_4_SCContent") {
                return -1;
            }
        }
    }

    fstat_hook_original.unwrap()(fd, statbuf)
});

pub fn init() {
    let config = config::native_config();
    if config.disable_metrics || config.disable_bitmoji {
        let libc = elf::Elf::from_maps("/libc.so").expect("Failed to find libc.so in maps");

        if let Some(ptr) = libc.get_symbol_address("fstat") {
            dobby_hook!(ptr as _, fstat_hook);
        } else {
            panic!("Failed to find fstat symbol");
        }
    }
}
