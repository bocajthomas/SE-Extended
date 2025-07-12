use std::sync::Mutex;

use procfs::process::MMPermissions;

use crate::mapped_lib::MappedLib;


static SIGNATURE_CACHE: Mutex<Vec<(String, Vec<usize>)>> = Mutex::new(Vec::new());

pub fn add_signatures(signatures: Vec<(String, Vec<usize>)>) {
    SIGNATURE_CACHE.lock().unwrap().extend(signatures);
}

pub fn get_signatures() -> Vec<(String, Vec<usize>)> {
    SIGNATURE_CACHE.lock().unwrap().clone()
}

pub fn find_signatures(module_base: usize, size: usize, pattern: &str, once: bool) -> Vec<usize> {
    let mut results = Vec::new();
    let mut bytes = Vec::new();
    let mut mask = Vec::new();
    let mut i = 0;

    if let Some(cache) = SIGNATURE_CACHE.lock().unwrap().iter().find(|(sig, _)| sig == pattern) {
        return cache.1.clone().into_iter().map(|offset| module_base + offset).collect();
    }

    while i < pattern.len() {
        if pattern.chars().nth(i).unwrap() == '?' {
            bytes.push(0);
            mask.push('?');
        } else {
            bytes.push(u8::from_str_radix(&pattern[i..i+2], 16).unwrap());
            mask.push('x');
        }
        i += 3;
    }

    let mut i = 0;
    let size = size - bytes.len();
    while i < size {
        let mut found = true;
        let mut j = 0;

        while j < bytes.len() {
            if mask[j] == '?' || bytes[j] == unsafe { *(module_base as *const u8).offset(i as isize + j as isize) } {
                j += 1;
                continue;
            }
            found = false;
            break;
        }
        if found {
            if once {
                SIGNATURE_CACHE.lock().unwrap().push((pattern.to_string(), vec![i]));
                return vec![module_base + i];
            }
            results.push(module_base + i);
        }
        i += 1;
    }

    SIGNATURE_CACHE.lock().unwrap().push((pattern.to_string(), results.clone()));
    results
}

pub fn find_signature_executable(mapped_lib: &MappedLib, pattern: &str) -> Option<usize> {
    let executable_regions = mapped_lib.regions.iter().filter(|region| {
        region.perms.contains(MMPermissions::EXECUTE) && region.perms.contains(MMPermissions::READ)
    }).collect::<Vec<_>>();

    for region in executable_regions {
        let size = (region.end - region.start) as usize;
        let module_base = region.start as usize;

        if size > 0 {
            let results = find_signatures(module_base, size, pattern, true);

            if results.is_empty() {
                warn!("Signature not found in region: {:#x} - {:#x}", region.start, region.end);
            } else {
                debug!("Found {} results in region: {:#x} - {:#x}", results.len(), region.start, region.end);
                return Some(results[0]);
            }
        }
    }

    None
}

pub fn find_signature(mapped_lib: &MappedLib, _arm64_pattern: &str, _arm64_offset: i64, _arm32_pattern: &str, _arm32_offset: i64) -> Option<usize> {
    #[cfg(target_arch = "aarch64")]
    {
        return find_signature_executable(mapped_lib, _arm64_pattern).map(|address| (address as i64 + _arm64_offset) as usize);
    }
    #[cfg(target_arch = "arm")]
    {
        return find_signature_executable(mapped_lib, _arm32_pattern).map(|address| (address as i64 + _arm32_offset) as usize);
    }
}