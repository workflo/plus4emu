/// Plus4 Emulator Core
/// Copyright (C) 2009 Florian Wolff (florian@donuz.de)
/// Rust port 2025
///
/// This program is free software; you can redistribute it and/or
/// modify it under the terms of the GNU General Public License
/// as published by the Free Software Foundation; either version 2
/// of the License, or (at your option) any later version.

use crate::cpu_state::CpuState;

// Constants
pub const CLOCK_FREQUENCY: u32 = 885000;
pub const IRQ_FREQUENCY: u32 = CLOCK_FREQUENCY / 60;
pub const RASTER_LINES: u32 = 312;
pub const SCREEN_REFRESH_FREQUENCY: u32 = 57;
pub const TICKS_PER_RASTER_LINE: u32 = 114;
pub const TICKS_PER_BLINK_INTERVAL: u32 = CLOCK_FREQUENCY / 8;
pub const SCREEN_WIDTH: usize = 320;
pub const SCREEN_HEIGHT: usize = 200;
pub const FIRST_SCREEN_LINE: usize = 3;

pub struct Plus4 {
    // Memory
    ram: [u8; 0x10000],
    rom: Vec<u8>,
    rom3plus1: Vec<u8>,

    // ROM configuration
    rom_active: bool,
    rom_config: u8,

    // CPU
    pub cpu: CpuState,

    // Timing
    pub clock_ticks: u32,
    clock_counter: u32,
    flash_counter: u32,
    raster_line: u32,
    flash_on: bool,

    // Timers
    timer_on: [bool; 3],

    // Screen buffer
    pub pixels: [[u8; SCREEN_WIDTH]; SCREEN_HEIGHT],
}

impl Plus4 {
    pub fn new() -> Self {
        Self {
            ram: [0; 0x10000],
            rom: vec![0; 0x8000],
            rom3plus1: vec![0; 0x8000],
            rom_active: true,
            rom_config: 0,
            cpu: CpuState::new(),
            clock_ticks: 0,
            clock_counter: 0,
            flash_counter: 0,
            raster_line: 0,
            flash_on: false,
            timer_on: [false; 3],
            pixels: [[0; SCREEN_WIDTH]; SCREEN_HEIGHT],
        }
    }

    pub fn load_rom(&mut self, rom_data: &[u8], rom3plus1_data: &[u8]) {
        self.rom = rom_data.to_vec();
        self.rom3plus1 = rom3plus1_data.to_vec();
    }

    // Memory access
    pub fn peek(&self, addr: u16) -> u8 {
        let addr = addr as usize;

        // I/O area
        if (addr >= 0xFD00 && addr <= 0xFDFF) || (addr >= 0xFF00 && addr <= 0xFF3F) {
            return self.ram[addr];
        }

        // RAM area or ROM disabled
        if addr < 0x8000 || !self.rom_active {
            return self.ram[addr];
        }

        // ROM banking for 0x8000-0xBFFF
        if addr < 0xC000 {
            match self.rom_config & 3 {
                0 => return self.rom[addr & 0x7FFF],
                1 => return self.rom3plus1[addr & 0x7FFF],
                _ => return 0,
            }
        }

        // System ROM area 0xFC00-0xFCFF
        if addr >= 0xFC00 && addr < 0xFD00 {
            return self.rom[addr & 0x7FFF];
        }

        // ROM banking for 0xC000+
        match (self.rom_config >> 2) & 3 {
            0 => self.rom[addr & 0x7FFF],
            1 => self.rom3plus1[addr & 0x7FFF],
            _ => 0,
        }
    }

    pub fn poke(&mut self, addr: u16, value: u8) {
        let addr = addr as usize;

        match addr {
            0xFF3E => {
                self.rom_active = true;
                return;
            }
            0xFF3F => {
                self.rom_active = false;
                return;
            }
            0xFDD0..=0xFDDF => {
                self.rom_config = (addr & 15) as u8;
                return;
            }
            _ => {}
        }

        // Don't write to keyboard register
        if addr != 0xFF08 {
            self.ram[addr] = value;
        }

        // Handle keyboard input
        if addr == 0xFD30 {
            self.p4_keyboard();
        }

        // TED chip registers
        if addr >= 0xFF00 && addr <= 0xFF1F {
            match addr {
                0xFF00 => self.timer_on[0] = false,
                0xFF01 => self.timer_on[0] = true,
                0xFF02 => self.timer_on[1] = false,
                0xFF03 => self.timer_on[1] = true,
                0xFF04 => self.timer_on[2] = false,
                0xFF05 => self.timer_on[2] = true,
                0xFF08 => {
                    if value == 0xFA {
                        self.p4_joystick(1);
                    }
                    if value == 0xFD {
                        self.p4_joystick(2);
                    }
                    self.p4_keyboard();
                }
                _ => {}
            }
        }
    }

    // Stack operations
    fn push(&mut self, value: u8) {
        self.ram[0x100 + self.cpu.sp as usize] = value;
        self.cpu.decr_sp();
    }

    fn pull(&mut self) -> u8 {
        self.cpu.incr_sp();
        self.ram[0x100 + self.cpu.sp as usize]
    }

    fn push_word(&mut self, word: u16) {
        self.ram[0x100 + self.cpu.sp as usize] = (word >> 8) as u8;
        self.cpu.decr_sp();
        self.ram[0x100 + self.cpu.sp as usize] = (word & 0xFF) as u8;
        self.cpu.decr_sp();
    }

    fn pull_word(&mut self) -> u16 {
        self.cpu.incr_sp();
        let lo = self.ram[0x100 + self.cpu.sp as usize] as u16;
        self.cpu.incr_sp();
        let hi = self.ram[0x100 + self.cpu.sp as usize] as u16;
        lo + (hi << 8)
    }

    // Flags
    fn set_flags(&mut self, flags: u8) {
        self.cpu.c = (flags & 1) != 0;
        self.cpu.z = (flags & 2) != 0;
        self.cpu.i = (flags & 4) != 0;
        self.cpu.d = (flags & 8) != 0;
        self.cpu.b = (flags & 16) != 0;
        self.cpu.v = (flags & 64) != 0;
        self.cpu.n = (flags & 128) != 0;
    }

    fn get_flags(&self) -> u8 {
        let mut flags = 32u8;
        if self.cpu.c { flags |= 1; }
        if self.cpu.z { flags |= 2; }
        if self.cpu.i { flags |= 4; }
        if self.cpu.d { flags |= 8; }
        if self.cpu.b { flags |= 16; }
        if self.cpu.v { flags |= 64; }
        if self.cpu.n { flags |= 128; }
        flags
    }

    // Reset
    pub fn hard_reset(&mut self) {
        // Get reset vector from ROM
        let reset_lo = self.rom[0xFFFC - 0x8000] as u16;
        let reset_hi = self.rom[0xFFFD - 0x8000] as u16;
        self.cpu.pc = (reset_hi << 8) + reset_lo;

        self.cpu.sp = 0xFF;
        self.cpu.acc = 0;
        self.cpu.xr = 0;
        self.cpu.yr = 0;

        self.cpu.i = true;
        self.cpu.n = false;
        self.cpu.c = false;
        self.cpu.b = false;
        self.cpu.z = false;
        self.cpu.d = false;
        self.cpu.v = false;

        self.ram[0xFF04] = 4;
        self.rom_config = 0;
        self.rom_active = true;

        self.clock_counter = 0;
        self.flash_on = false;
        self.flash_counter = 0;
        self.raster_line = 0;
    }

    // Input placeholders
    fn p4_keyboard(&mut self) {
        // Keyboard matrix implementation - placeholder
        self.ram[0xFF08] = 0xFF;
    }

    fn p4_joystick(&mut self, _port: u8) {
        // Joystick implementation - placeholder
        self.ram[0xFF08] = 0xFF;
    }

    // Execute one CPU instruction (simplified - will need full opcode table)
    pub fn execute_instruction(&mut self) {
        let opcode = self.peek(self.cpu.pc);
        self.clock_ticks = 2; // Default timing

        // Simplified instruction execution
        // Full implementation would have all 256 opcodes
        match opcode {
            0xEA => { // NOP
                self.cpu.incr_pc(1);
                self.clock_ticks = 2;
            }
            0x4C => { // JMP absolute
                let lo = self.peek(self.cpu.pc + 1) as u16;
                let hi = self.peek(self.cpu.pc + 2) as u16;
                self.cpu.pc = (hi << 8) | lo;
                self.clock_ticks = 3;
            }
            _ => {
                // Unimplemented opcode - skip for now
                println!("Unimplemented opcode: 0x{:02X} at PC=0x{:04X}", opcode, self.cpu.pc);
                self.cpu.incr_pc(1);
            }
        }
    }

    // Render one raster line (simplified)
    fn render_raster_line(&mut self) {
        if self.raster_line >= FIRST_SCREEN_LINE as u32
            && self.raster_line < (FIRST_SCREEN_LINE + SCREEN_HEIGHT) as u32
        {
            let line = (self.raster_line - FIRST_SCREEN_LINE as u32) as usize;

            // Simple black screen for now
            for x in 0..SCREEN_WIDTH {
                self.pixels[line][x] = 0; // Black
            }
        }

        self.raster_line += 1;
        if self.raster_line >= RASTER_LINES {
            self.raster_line = 0;
        }

        // Flash counter for cursor blink
        self.flash_counter += TICKS_PER_RASTER_LINE;
        if self.flash_counter >= TICKS_PER_BLINK_INTERVAL {
            self.flash_counter = 0;
            self.flash_on = !self.flash_on;
        }
    }

    // Main emulation step
    pub fn step(&mut self) {
        self.clock_ticks = 0;
        self.execute_instruction();

        let clock_multiplier = if (self.ram[0xFF06] & 16) != 0 { 2 } else { 1 };

        self.clock_counter += self.clock_ticks * clock_multiplier;

        // Timer updates would go here

        // Render raster line if needed
        if self.clock_counter >= TICKS_PER_RASTER_LINE {
            self.clock_counter -= TICKS_PER_RASTER_LINE;
            self.render_raster_line();
        }
    }
}
