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
    pub matrix: [[bool; 8]; 8],
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

        // Plus/4 keyboard matrix mapping based on Java version
        // Row 0 (latch bit 0)
        if is_key_down(KeyCode::Backspace) { self.matrix[0][0] = true; }  // Delete
        if is_key_down(KeyCode::Enter) { self.matrix[0][1] = true; }      // Return
        // Pfund at bit 2 - no mapping
        // Help (F4) at bit 3 - no mapping
        if is_key_down(KeyCode::F1) { self.matrix[0][4] = true; }
        if is_key_down(KeyCode::F2) { self.matrix[0][5] = true; }
        if is_key_down(KeyCode::F3) { self.matrix[0][6] = true; }
        // @ at bit 7 - no mapping

        // Row 1 (latch bit 1)
        if is_key_down(KeyCode::Key3) { self.matrix[1][0] = true; }
        if is_key_down(KeyCode::W) { self.matrix[1][1] = true; }
        if is_key_down(KeyCode::A) { self.matrix[1][2] = true; }
        if is_key_down(KeyCode::Key4) { self.matrix[1][3] = true; }
        if is_key_down(KeyCode::Z) { self.matrix[1][4] = true; }  // Z (German layout Y)
        if is_key_down(KeyCode::S) { self.matrix[1][5] = true; }
        if is_key_down(KeyCode::E) { self.matrix[1][6] = true; }
        if is_key_down(KeyCode::LeftShift) || is_key_down(KeyCode::RightShift) { self.matrix[1][7] = true; }

        // Row 2 (latch bit 2)
        if is_key_down(KeyCode::Key5) { self.matrix[2][0] = true; }
        if is_key_down(KeyCode::R) { self.matrix[2][1] = true; }
        if is_key_down(KeyCode::D) { self.matrix[2][2] = true; }
        if is_key_down(KeyCode::Key6) { self.matrix[2][3] = true; }
        if is_key_down(KeyCode::C) { self.matrix[2][4] = true; }
        if is_key_down(KeyCode::F) { self.matrix[2][5] = true; }
        if is_key_down(KeyCode::T) { self.matrix[2][6] = true; }
        if is_key_down(KeyCode::X) { self.matrix[2][7] = true; }

        // Row 3 (latch bit 3)
        if is_key_down(KeyCode::Key7) { self.matrix[3][0] = true; }
        if is_key_down(KeyCode::Y) { self.matrix[3][1] = true; }  // Y (German layout Z)
        if is_key_down(KeyCode::G) { self.matrix[3][2] = true; }
        if is_key_down(KeyCode::Key8) { self.matrix[3][3] = true; }
        if is_key_down(KeyCode::B) { self.matrix[3][4] = true; }
        if is_key_down(KeyCode::H) { self.matrix[3][5] = true; }
        if is_key_down(KeyCode::U) { self.matrix[3][6] = true; }
        if is_key_down(KeyCode::V) { self.matrix[3][7] = true; }

        // Row 4 (latch bit 4)
        if is_key_down(KeyCode::Key9) { self.matrix[4][0] = true; }
        if is_key_down(KeyCode::I) { self.matrix[4][1] = true; }
        if is_key_down(KeyCode::J) { self.matrix[4][2] = true; }
        if is_key_down(KeyCode::Key0) { self.matrix[4][3] = true; }
        if is_key_down(KeyCode::M) { self.matrix[4][4] = true; }
        if is_key_down(KeyCode::K) { self.matrix[4][5] = true; }
        if is_key_down(KeyCode::O) { self.matrix[4][6] = true; }
        if is_key_down(KeyCode::N) { self.matrix[4][7] = true; }

        // Row 5 (latch bit 5)
        if is_key_down(KeyCode::Down) { self.matrix[5][0] = true; }
        if is_key_down(KeyCode::P) { self.matrix[5][1] = true; }
        if is_key_down(KeyCode::L) { self.matrix[5][2] = true; }
        if is_key_down(KeyCode::Up) { self.matrix[5][3] = true; }
        if is_key_down(KeyCode::Period) { self.matrix[5][4] = true; }  // .
        // [ at bit 5 - mapped to Left bracket
        if is_key_down(KeyCode::LeftBracket) { self.matrix[5][5] = true; }
        if is_key_down(KeyCode::Minus) { self.matrix[5][6] = true; }   // -
        if is_key_down(KeyCode::Comma) { self.matrix[5][7] = true; }   // ,

        // Row 6 (latch bit 6)
        if is_key_down(KeyCode::Left) { self.matrix[6][0] = true; }
        if is_key_down(KeyCode::Slash) { self.matrix[6][1] = true; }       // * (mapped to /)
        if is_key_down(KeyCode::RightBracket) { self.matrix[6][2] = true; } // ]
        if is_key_down(KeyCode::Right) { self.matrix[6][3] = true; }
        if is_key_down(KeyCode::Escape) { self.matrix[6][4] = true; }      // ESC
        if is_key_down(KeyCode::Equal) { self.matrix[6][5] = true; }       // =
        // + at bit 6 - mapped to Equal
        if is_key_down(KeyCode::Backslash) { self.matrix[6][7] = true; }   // / (mapped to Backslash)

        // Row 7 (latch bit 7)
        if is_key_down(KeyCode::Key1) { self.matrix[7][0] = true; }
        if is_key_down(KeyCode::Home) { self.matrix[7][1] = true; }        // Clr/Home
        if is_key_down(KeyCode::LeftControl) || is_key_down(KeyCode::RightControl) { self.matrix[7][2] = true; }
        if is_key_down(KeyCode::Key2) { self.matrix[7][3] = true; }
        if is_key_down(KeyCode::Space) { self.matrix[7][4] = true; }
        if is_key_down(KeyCode::LeftAlt) || is_key_down(KeyCode::RightAlt) { self.matrix[7][5] = true; }  // C= (Commodore key)
        if is_key_down(KeyCode::Q) { self.matrix[7][6] = true; }
        if is_key_down(KeyCode::Tab) { self.matrix[7][7] = true; }         // Run/Stop

        // Debug output only if at least one key is pressed
        // let any_key_pressed = self.matrix.iter().any(|row| row.iter().any(|&pressed| pressed));
        // if any_key_pressed {
        //     println!("Keyboard matrix updated: {}", self.matrix.iter().map(|row| format!("{:?}", row)).collect::<Vec<_>>().join(" | "));
        // }
    }

    /// Read keyboard matrix for a given latch value
    /// Returns the column bits for the selected row(s)
    pub fn read(&self, latch: u8) -> u8 {
        let mut result = 0xFFu8;

        // Invert latch (active low)
        let latch = !latch;

        // Check each row bit
        for row in 0..8 {
            if (latch & (1 << row)) != 0 {
                // This row is selected, check all columns
                for col in 0..8 {
                    if self.matrix[row][col] {
                        result &= !(1 << col);  // Pull bit low if key pressed
                    }
                }
            }
        }

        result
    }
}
