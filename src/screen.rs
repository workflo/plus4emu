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

        // Continue with more palette entries (simplified for now)
        // Full palette would have all 128 colors
        for i in 0x20..128 {
            let factor = (i - 0x20) as f32 / 108.0;
            self.palette[i] = Color::from_rgba(
                (factor * 255.0) as u8,
                (factor * 255.0) as u8,
                (factor * 255.0) as u8,
                255,
            );
        }
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
