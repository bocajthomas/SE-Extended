use procfs::process::MMapPath;

pub struct Elf<'elf> {
    base_address: usize,
    elf: goblin::elf::Elf<'elf>,
    _buffer: &'elf [u8],
}

impl<'elf> Elf<'elf> {
    pub fn from_maps(lib: &str) -> Option<Self> {
        let maps = procfs::process::Process::myself().ok()?.maps().ok()?;

        for memory_map in maps.iter() {
            if let MMapPath::Path(path) = &memory_map.pathname {
                let path = path.to_string_lossy();

                if !path.contains(lib) {
                    continue;
                }

                let file_data = std::fs::read(path.to_string()).ok()?;
                let file_buffer = Box::leak(file_data.into_boxed_slice());

                if let Ok(elf) = goblin::elf::Elf::parse(file_buffer) {
                    return Some(Elf {
                        base_address: memory_map.address.0 as usize,
                        elf,
                        _buffer: file_buffer,
                    });
                } else {
                    warn!(
                        "Failed to parse ELF for library {} at address {:x}",
                        lib, memory_map.address.0
                    );
                }
            }
        }

        None
    }

    pub fn get_symbol_address(&self, symbol: &str) -> Option<usize> {
        for sym in &self.elf.dynsyms {
            if let Some(name) = self.elf.dynstrtab.get_at(sym.st_name) {
                if name == symbol {
                    return Some(self.base_address + sym.st_value as usize);
                }
            }
        }

        for sym in &self.elf.syms {
            if let Some(name) = self.elf.strtab.get_at(sym.st_name) {
                if name == symbol {
                    return Some(self.base_address + sym.st_value as usize);
                }
            }
        }

        None
    }
}
