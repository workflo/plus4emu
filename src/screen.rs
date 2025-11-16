/// Screen rendering with macroquad
/// Copyright (C) 2009 Florian Wolff (florian@donuz.de)
/// Rust port 2025
///
/// This program is free software; you can redistribute it and/or
/// modify it under the terms of the GNU General Public License
/// as published by the Free Software Foundation; either version 2
/// of the License, or (at your option) any later version.

use macroquad::prelude::*;
use crate::plus4::{SCREEN_WIDTH, SCREEN_HEIGHT};

pub struct Screen {
    texture: Texture2D,
    palette: [Color; 128],
}

impl Screen {
    pub fn new() -> Self {
        let texture = Texture2D::from_rgba8(
            SCREEN_WIDTH as u16,
            SCREEN_HEIGHT as u16,
            &vec![0u8; SCREEN_WIDTH * SCREEN_HEIGHT * 4],
        );
        texture.set_filter(FilterMode::Nearest);

        let mut screen = Self {
            texture,
            palette: [BLACK; 128],
        };
        screen.load_palette();
        screen
    }

    fn load_palette(&mut self) {
        // Plus/4 128-color palette (RGB values from Java version)
        self.palette[0x00] = Color::from_rgba(0, 0, 0, 255);
        self.palette[0x01] = Color::from_rgba(44, 44, 44, 255);
        self.palette[0x02] = Color::from_rgba(98, 19, 7, 255);
        self.palette[0x03] = Color::from_rgba(0, 66, 67, 255);
        self.palette[0x04] = Color::from_rgba(81, 3, 120, 255);
        self.palette[0x05] = Color::from_rgba(0, 78, 0, 255);
        self.palette[0x06] = Color::from_rgba(39, 24, 142, 255);
        self.palette[0x07] = Color::from_rgba(48, 62, 0, 255);
        self.palette[0x08] = Color::from_rgba(88, 33, 0, 255);
        self.palette[0x09] = Color::from_rgba(70, 48, 0, 255);
        self.palette[0x0A] = Color::from_rgba(36, 68, 0, 255);
        self.palette[0x0B] = Color::from_rgba(99, 4, 72, 255);
        self.palette[0x0C] = Color::from_rgba(0, 78, 12, 255);
        self.palette[0x0D] = Color::from_rgba(14, 39, 132, 255);
        self.palette[0x0E] = Color::from_rgba(51, 17, 142, 255);
        self.palette[0x0F] = Color::from_rgba(24, 72, 0, 255);

        self.palette[0x10] = Color::from_rgba(0, 0, 0, 255);
        self.palette[0x11] = Color::from_rgba(59, 59, 59, 255);
        self.palette[0x12] = Color::from_rgba(112, 36, 25, 255);
        self.palette[0x13] = Color::from_rgba(0, 80, 90, 255);
        self.palette[0x14] = Color::from_rgba(96, 22, 133, 255);
        self.palette[0x15] = Color::from_rgba(18, 93, 0, 255);
        self.palette[0x16] = Color::from_rgba(54, 40, 155, 255);
        self.palette[0x17] = Color::from_rgba(63, 76, 0, 255);
        self.palette[0x18] = Color::from_rgba(102, 49, 0, 255);
        self.palette[0x19] = Color::from_rgba(85, 63, 0, 255);
        self.palette[0x1A] = Color::from_rgba(52, 82, 0, 255);
        self.palette[0x1B] = Color::from_rgba(113, 22, 86, 255);
        self.palette[0x1C] = Color::from_rgba(0, 92, 29, 255);
        self.palette[0x1D] = Color::from_rgba(31, 54, 145, 255);
        self.palette[0x1E] = Color::from_rgba(66, 34, 155, 255);
        self.palette[0x1F] = Color::from_rgba(40, 87, 0, 255);

        self.palette[0x20] = Color::from_rgba(119, 119, 119, 255);
        self.palette[0x21] = Color::from_rgba(154, 59, 48, 255);
        self.palette[0x22] = Color::from_rgba(75, 137, 113, 255);
        self.palette[0x23] = Color::from_rgba(138, 43, 156, 255);
        self.palette[0x24] = Color::from_rgba(60, 150, 20, 255);
        self.palette[0x25] = Color::from_rgba(96, 100, 178, 255);
        self.palette[0x26] = Color::from_rgba(105, 133, 0, 255);
        self.palette[0x27] = Color::from_rgba(144, 106, 0, 255);
        self.palette[0x28] = Color::from_rgba(127, 120, 0, 255);
        self.palette[0x29] = Color::from_rgba(93, 140, 0, 255);
        self.palette[0x2A] = Color::from_rgba(155, 65, 109, 255);
        self.palette[0x2B] = Color::from_rgba(51, 149, 53, 255);
        self.palette[0x2C] = Color::from_rgba(73, 111, 169, 255);
        self.palette[0x2D] = Color::from_rgba(108, 95, 178, 255);
        self.palette[0x2E] = Color::from_rgba(82, 144, 0, 255);
        self.palette[0x2F] = Color::from_rgba(0, 0, 0, 255);

        self.palette[0x30] = Color::from_rgba(44, 44, 44, 255);
        self.palette[0x31] = Color::from_rgba(98, 19, 7, 255);
        self.palette[0x32] = Color::from_rgba(0, 66, 67, 255);
        self.palette[0x33] = Color::from_rgba(81, 3, 120, 255);
        self.palette[0x34] = Color::from_rgba(0, 78, 0, 255);
        self.palette[0x35] = Color::from_rgba(39, 24, 142, 255);
        self.palette[0x36] = Color::from_rgba(48, 62, 0, 255);
        self.palette[0x37] = Color::from_rgba(88, 33, 0, 255);
        self.palette[0x38] = Color::from_rgba(70, 48, 0, 255);
        self.palette[0x39] = Color::from_rgba(36, 68, 0, 255);
        self.palette[0x3A] = Color::from_rgba(99, 4, 72, 255);
        self.palette[0x3B] = Color::from_rgba(0, 78, 12, 255);
        self.palette[0x3C] = Color::from_rgba(14, 39, 132, 255);
        self.palette[0x3D] = Color::from_rgba(51, 17, 142, 255);
        self.palette[0x3E] = Color::from_rgba(24, 72, 0, 255);
        self.palette[0x3F] = Color::from_rgba(59, 59, 59, 255);

        self.palette[0x40] = Color::from_rgba(112, 36, 25, 255);
        self.palette[0x41] = Color::from_rgba(0, 80, 90, 255);
        self.palette[0x42] = Color::from_rgba(96, 22, 133, 255);
        self.palette[0x43] = Color::from_rgba(18, 93, 0, 255);
        self.palette[0x44] = Color::from_rgba(54, 40, 155, 255);
        self.palette[0x45] = Color::from_rgba(63, 76, 0, 255);
        self.palette[0x46] = Color::from_rgba(102, 49, 0, 255);
        self.palette[0x47] = Color::from_rgba(85, 63, 0, 255);
        self.palette[0x48] = Color::from_rgba(52, 82, 0, 255);
        self.palette[0x49] = Color::from_rgba(113, 22, 86, 255);
        self.palette[0x4A] = Color::from_rgba(0, 92, 29, 255);
        self.palette[0x4B] = Color::from_rgba(31, 54, 145, 255);
        self.palette[0x4C] = Color::from_rgba(66, 34, 155, 255);
        self.palette[0x4D] = Color::from_rgba(40, 87, 0, 255);
        self.palette[0x4E] = Color::from_rgba(119, 119, 119, 255);
        self.palette[0x4F] = Color::from_rgba(154, 59, 48, 255);

        self.palette[0x50] = Color::from_rgba(75, 137, 113, 255);
        self.palette[0x51] = Color::from_rgba(138, 43, 156, 255);
        self.palette[0x52] = Color::from_rgba(60, 150, 20, 255);
        self.palette[0x53] = Color::from_rgba(96, 100, 178, 255);
        self.palette[0x54] = Color::from_rgba(105, 133, 0, 255);
        self.palette[0x55] = Color::from_rgba(144, 106, 0, 255);
        self.palette[0x56] = Color::from_rgba(127, 120, 0, 255);
        self.palette[0x57] = Color::from_rgba(93, 140, 0, 255);
        self.palette[0x58] = Color::from_rgba(155, 65, 109, 255);
        self.palette[0x59] = Color::from_rgba(51, 149, 53, 255);
        self.palette[0x5A] = Color::from_rgba(73, 111, 169, 255);
        self.palette[0x5B] = Color::from_rgba(108, 95, 178, 255);
        self.palette[0x5C] = Color::from_rgba(82, 144, 0, 255);
        self.palette[0x5D] = Color::from_rgba(178, 178, 178, 255);
        self.palette[0x5E] = Color::from_rgba(212, 124, 107, 255);
        self.palette[0x5F] = Color::from_rgba(134, 195, 171, 255);

        self.palette[0x60] = Color::from_rgba(197, 107, 214, 255);
        self.palette[0x61] = Color::from_rgba(120, 208, 79, 255);
        self.palette[0x62] = Color::from_rgba(155, 164, 237, 255);
        self.palette[0x63] = Color::from_rgba(164, 192, 45, 255);
        self.palette[0x64] = Color::from_rgba(203, 170, 0, 255);
        self.palette[0x65] = Color::from_rgba(186, 179, 0, 255);
        self.palette[0x66] = Color::from_rgba(152, 198, 16, 255);
        self.palette[0x67] = Color::from_rgba(214, 129, 167, 255);
        self.palette[0x68] = Color::from_rgba(111, 208, 111, 255);
        self.palette[0x69] = Color::from_rgba(133, 170, 227, 255);
        self.palette[0x6A] = Color::from_rgba(168, 158, 237, 255);
        self.palette[0x6B] = Color::from_rgba(142, 203, 41, 255);
        self.palette[0x6C] = Color::from_rgba(237, 237, 237, 255);
        self.palette[0x6D] = Color::from_rgba(255, 189, 166, 255);
        self.palette[0x6E] = Color::from_rgba(194, 255, 230, 255);
        self.palette[0x6F] = Color::from_rgba(255, 172, 255, 255);

        self.palette[0x70] = Color::from_rgba(180, 255, 138, 255);
        self.palette[0x71] = Color::from_rgba(215, 229, 255, 255);
        self.palette[0x72] = Color::from_rgba(224, 255, 105, 255);
        self.palette[0x73] = Color::from_rgba(255, 235, 59, 255);
        self.palette[0x74] = Color::from_rgba(245, 244, 59, 255);
        self.palette[0x75] = Color::from_rgba(212, 255, 76, 255);
        self.palette[0x76] = Color::from_rgba(255, 194, 227, 255);
        self.palette[0x77] = Color::from_rgba(171, 255, 171, 255);
        self.palette[0x78] = Color::from_rgba(193, 235, 255, 255);
        self.palette[0x79] = Color::from_rgba(228, 223, 255, 255);
        self.palette[0x7A] = Color::from_rgba(202, 255, 101, 255);
        self.palette[0x7B] = Color::from_rgba(255, 255, 255, 255);
        self.palette[0x7C] = Color::from_rgba(255, 255, 255, 255);
        self.palette[0x7D] = Color::from_rgba(255, 255, 255, 255);
        self.palette[0x7E] = Color::from_rgba(255, 255, 255, 255);
        self.palette[0x7F] = Color::from_rgba(255, 255, 255, 255);
    }

    pub fn update(&mut self, pixels: &[[u8; SCREEN_WIDTH]; SCREEN_HEIGHT]) {
        let mut rgba_data = vec![0u8; SCREEN_WIDTH * SCREEN_HEIGHT * 4];

        for y in 0..SCREEN_HEIGHT {
            for x in 0..SCREEN_WIDTH {
                let color_idx = pixels[y][x] as usize % 128;
                let color = self.palette[color_idx];
                let idx = (y * SCREEN_WIDTH + x) * 4;
                rgba_data[idx] = (color.r * 255.0) as u8;
                rgba_data[idx + 1] = (color.g * 255.0) as u8;
                rgba_data[idx + 2] = (color.b * 255.0) as u8;
                rgba_data[idx + 3] = 255;
            }
        }

        let image = Image {
            bytes: rgba_data,
            width: SCREEN_WIDTH as u16,
            height: SCREEN_HEIGHT as u16,
        };
        self.texture.update(&image);
    }

    pub fn draw(&self, scale: f32) {
        let width = SCREEN_WIDTH as f32 * scale;
        let height = SCREEN_HEIGHT as f32 * scale;

        draw_texture_ex(
            &self.texture,
            0.0,
            0.0,
            WHITE,
            DrawTextureParams {
                dest_size: Some(Vec2::new(width, height)),
                ..Default::default()
            },
        );
    }
}
