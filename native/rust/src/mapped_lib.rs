use std::error::Error;

use procfs::process::{MMPermissions, MMapPath};

#[derive(Debug)]
pub(crate) struct MappedRegion {
    pub start: u64,
    pub end: u64,
    pub perms: MMPermissions,
}

#[derive(Debug)]
pub(crate) struct MappedLib {
    name: String,
    pub regions: Vec<MappedRegion>,
}

impl MappedLib {
    pub fn new(name: String) -> Self {
        Self {
            name,
            regions: Vec::new(),
        }
    }

    pub fn search(&mut self) -> Result<&Self, Box<dyn Error>> {
        procfs::process::Process::myself()?
            .maps()?
            .iter()
            .for_each(|map| {
                let pathname = &map.pathname;

                if let MMapPath::Path(path_buffer) = pathname {
                    let path = path_buffer.to_string_lossy();

                    if path.contains(&self.name) {
                        self.regions.push(MappedRegion {
                            start: map.address.0,
                            end: map.address.1,
                            perms: map.perms,
                        });
                    }
                }
            });

        if self.regions.is_empty() {
            return Err(format!("No regions found for {}", self.name).into());
        }

        Ok(self)
    }
}
