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
mod prg_loader;

use macroquad::prelude::*;
use plus4::{Plus4, SCREEN_WIDTH, SCREEN_HEIGHT};
use screen::Screen;
use keyboard::KeyboardMatrix;
use prg_loader::PrgFile;

const SCALE: f32 = 3.0;

// Create a simple test PRG for testing
// This creates a minimal BASIC program: 10 PRINT "HELLO"
fn create_test_prg() -> PrgFile {
    // BASIC program structure for Plus/4:
    // $1001: Start of BASIC program
    // Format: [next_line_ptr_lo] [next_line_ptr_hi] [line_num_lo] [line_num_hi] [tokens...] [0x00]

    let mut data = Vec::new();

    // Line 10: PRINT "HELLO PLUS/4!"
    // Next line pointer (points to end, $0000 = no next line)
    let next_line = 0x1001 + 20; // Approximate end
    data.push((next_line & 0xFF) as u8);
    data.push(((next_line >> 8) & 0xFF) as u8);

    // Line number: 10
    data.push(10);
    data.push(0);

    // PRINT token ($99 on Plus/4)
    data.push(0x99);

    // Space
    data.push(0x20);

    // String: "HELLO PLUS/4!"
    data.push(0x22); // "
    for &c in b"HELLO PLUS/4!" {
        data.push(c);
    }
    data.push(0x22); // "

    // End of line
    data.push(0x00);

    // End of program marker
    data.push(0x00);
    data.push(0x00);

    PrgFile::from_data(0x1001, data)
}

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

    // PRG loading state
    let mut prg_loaded = false;

    println!("Plus/4 Emulator started!");
    println!("Press ESC to exit");
    println!("Press F12 to load test.prg");

    loop {
        // Input handling
        keyboard.update();

        if is_key_down(KeyCode::Escape) {
            break;
        }

        // Update emulator keyboard state
        emulator.update_keyboard(keyboard.matrix);

        // Magic hotkey F12: Load test PRG file
        if is_key_pressed(KeyCode::F12) && !prg_loaded {
            println!("\n=== Loading test.prg ===");
            match PrgFile::load_from_file("prg\\COBRA.PRG") {
                Ok(prg) => {
                    println!("PRG file loaded: ${:04X} - ${:04X}",
                             prg.load_address, prg.end_address());
                    emulator.load_and_run_prg(&prg);
                    prg_loaded = true;
                    println!("=== PRG loaded and started ===\n");
                }
                Err(e) => {
                    println!("Error loading test.prg: {}", e);
                    println!("Creating embedded test PRG instead...");

                    // Create a simple test PRG in memory
                    // This is a simple BASIC program: 10 PRINT "HELLO PLUS/4!"
                    let test_prg = create_test_prg();
                    emulator.load_and_run_prg(&test_prg);
                    prg_loaded = true;
                    println!("=== Embedded test PRG loaded ===\n");
                }
            }
        }

        // R key: Reset emulator
        if is_key_pressed(KeyCode::F11) {
            println!("Resetting emulator...");
            emulator.hard_reset();
            prg_loaded = false;
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

        // Show FPS and status
        draw_text(
            &format!("FPS: {}", get_fps()),
            10.0,
            20.0,
            20.0,
            WHITE,
        );

        // Show PC and PRG status
        draw_text(
            &format!("PC: ${:04X}", emulator.cpu.pc),
            10.0,
            40.0,
            20.0,
            WHITE,
        );

        // if prg_loaded {
        //     draw_text(
        //         "PRG Loaded",
        //         10.0,
        //         60.0,
        //         20.0,
        //         GREEN,
        //     );
        // } else {
        //     draw_text(
        //         "Press F12 to load PRG",
        //         10.0,
        //         60.0,
        //         20.0,
        //         YELLOW,
        //     );
        // }

        next_frame().await;
    }

    println!("Emulator stopped.");
}
