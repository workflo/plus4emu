/// PRG file loader for Plus/4
/// PRG format: 2 bytes load address (little endian) + program data
/// Copyright (C) 2025
///
/// This program is free software; you can redistribute it and/or
/// modify it under the terms of the GNU General Public License
/// as published by the Free Software Foundation; either version 2
/// of the License, or (at your option) any later version.

use std::fs::File;
use std::io::{Read, Result};
use std::path::Path;

#[derive(Debug, Clone)]
pub struct PrgFile {
    pub load_address: u16,
    pub data: Vec<u8>,
}

impl PrgFile {
    /// Load a PRG file from disk
    pub fn load_from_file<P: AsRef<Path>>(path: P) -> Result<Self> {
        let mut file = File::open(path)?;
        let mut buffer = Vec::new();
        file.read_to_end(&mut buffer)?;

        if buffer.len() < 2 {
            return Err(std::io::Error::new(
                std::io::ErrorKind::InvalidData,
                "PRG file too small (needs at least 2 bytes for load address)",
            ));
        }

        // First two bytes are load address (little endian)
        let load_address = buffer[0] as u16 | ((buffer[1] as u16) << 8);

        // Rest is program data
        let data = buffer[2..].to_vec();

        Ok(Self {
            load_address,
            data,
        })
    }

    /// Create a PRG file from raw data (for testing)
    pub fn from_data(load_address: u16, data: Vec<u8>) -> Self {
        Self {
            load_address,
            data,
        }
    }

    /// Get end address (load_address + data.len())
    pub fn end_address(&self) -> u16 {
        self.load_address.wrapping_add(self.data.len() as u16)
    }

    /// Check if this is a BASIC program (load address $1001)
    pub fn is_basic_program(&self) -> bool {
        self.load_address == 0x1001
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_prg_creation() {
        let prg = PrgFile::from_data(0x1001, vec![0x00, 0x01, 0x02]);
        assert_eq!(prg.load_address, 0x1001);
        assert_eq!(prg.data.len(), 3);
        assert_eq!(prg.end_address(), 0x1004);
        assert!(prg.is_basic_program());
    }

    #[test]
    fn test_non_basic_program() {
        let prg = PrgFile::from_data(0x8000, vec![0xA9, 0x00]);
        assert!(!prg.is_basic_program());
    }
}
