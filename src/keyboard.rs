/// Keyboard matrix mapping for Plus/4
/// Copyright (C) 2009 Florian Wolff (florian@donuz.de)
/// Rust port 2025
///
/// This program is free software; you can redistribute it and/or
/// modify it under the terms of the GNU General Public License
/// as published by the Free Software Foundation; either version 2
/// of the License, or (at your option) any later version.

use macroquad::prelude::*;

/// Plus/4 keyboard matrix is 8x8
/// This maps PC keyboard keys to Plus/4 matrix positions
pub struct KeyboardMatrix {
    matrix: [[bool; 8]; 8],
}

impl KeyboardMatrix {
    pub fn new() -> Self {
        Self {
            matrix: [[false; 8]; 8],
        }
    }

    pub fn update(&mut self) {
        // Clear matrix
        self.matrix = [[false; 8]; 8];

        // Map PC keys to Plus/4 keyboard matrix
        // This is a simplified mapping - full implementation would map all keys
        // Row 0
        if is_key_down(KeyCode::Key7) { self.matrix[0][0] = true; }
        if is_key_down(KeyCode::Key4) { self.matrix[0][1] = true; }
        if is_key_down(KeyCode::Key1) { self.matrix[0][2] = true; }
        if is_key_down(KeyCode::Escape) { self.matrix[0][3] = true; } // ESC

        // Row 1
        if is_key_down(KeyCode::Key8) { self.matrix[1][0] = true; }
        if is_key_down(KeyCode::Key5) { self.matrix[1][1] = true; }
        if is_key_down(KeyCode::Key2) { self.matrix[1][2] = true; }
        if is_key_down(KeyCode::Space) { self.matrix[1][3] = true; }

        // Row 2
        if is_key_down(KeyCode::Key9) { self.matrix[2][0] = true; }
        if is_key_down(KeyCode::Key6) { self.matrix[2][1] = true; }
        if is_key_down(KeyCode::Key3) { self.matrix[2][2] = true; }

        // Letters (simplified mapping)
        if is_key_down(KeyCode::A) { self.matrix[3][0] = true; }
        if is_key_down(KeyCode::B) { self.matrix[3][1] = true; }
        if is_key_down(KeyCode::C) { self.matrix[3][2] = true; }
        if is_key_down(KeyCode::D) { self.matrix[3][3] = true; }

        if is_key_down(KeyCode::Enter) { self.matrix[7][7] = true; }

        // Shift keys
        if is_key_down(KeyCode::LeftShift) || is_key_down(KeyCode::RightShift) {
            self.matrix[0][7] = true;
        }
    }

    pub fn read(&self, row: u8) -> u8 {
        let mut result = 0xFFu8;

        if row < 8 {
            for col in 0..8 {
                if self.matrix[row as usize][col] {
                    result &= !(1 << col);
                }
            }
        }

        result
    }
}
