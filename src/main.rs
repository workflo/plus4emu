/// Plus/4 Emulator in Rust with macroquad
/// Copyright (C) 2009 Florian Wolff (florian@donuz.de)
/// Rust port 2025
///
/// This program is free software; you can redistribute it and/or
/// modify it under the terms of the GNU General Public License
/// as published by the Free Software Foundation; either version 2
/// of the License, or (at your option) any later version.

mod cpu_state;
mod plus4;
mod screen;
mod keyboard;

use macroquad::prelude::*;
use plus4::{Plus4, SCREEN_WIDTH, SCREEN_HEIGHT};
use screen::Screen;
use keyboard::KeyboardMatrix;

const SCALE: f32 = 3.0;

fn window_conf() -> Conf {
    Conf {
        window_title: "Plus/4 Emulator (Rust)".to_owned(),
        window_width: (SCREEN_WIDTH as f32 * SCALE) as i32,
        window_height: (SCREEN_HEIGHT as f32 * SCALE) as i32,
        window_resizable: false,
        ..Default::default()
    }
}

#[macroquad::main(window_conf)]
async fn main() {
    // Load ROM files
    let rom_data = include_bytes!("../roms/rom.bin");
    let rom3plus1_data = include_bytes!("../roms/3plus1.bin");

    // Initialize emulator
    let mut emulator = Plus4::new();
    emulator.load_rom(rom_data, rom3plus1_data);
    emulator.hard_reset();

    // Initialize screen
    let mut screen = Screen::new();

    // Initialize keyboard
    let mut keyboard = KeyboardMatrix::new();

    // Emulation state
    let cycles_per_frame = plus4::CLOCK_FREQUENCY / 60;
    let mut accumulated_cycles;

    println!("Plus/4 Emulator started!");
    println!("PC: 0x{:04X}", emulator.cpu.pc);
    println!("Press ESC to exit");

    loop {
        // Input handling
        keyboard.update();

        if is_key_down(KeyCode::Escape) {
            break;
        }

        // Emulation loop - execute instructions until we've done enough for one frame
        accumulated_cycles = 0;
        while accumulated_cycles < cycles_per_frame {
            emulator.step();
            accumulated_cycles += emulator.clock_ticks;

            // Limit to prevent infinite loop on errors
            if accumulated_cycles > cycles_per_frame * 2 {
                break;
            }
        }

        // Update screen with emulator's pixel buffer
        screen.update(&emulator.pixels);

        // Clear background
        clear_background(BLACK);

        // Draw screen
        screen.draw(SCALE);

        // Show FPS
        draw_text(
            &format!("FPS: {}", get_fps()),
            10.0,
            20.0,
            20.0,
            WHITE,
        );

        next_frame().await;
    }

    println!("Emulator stopped.");
}
