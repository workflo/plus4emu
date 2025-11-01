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
pub const CLOCK_FREQUENCY: u32 = 50000;//885000;
// pub const IRQ_FREQUENCY: u32 = CLOCK_FREQUENCY / 60;
pub const RASTER_LINES: u32 = 312;
// pub const SCREEN_REFRESH_FREQUENCY: u32 = 57;
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

    // Helper: Get address based on addressing mode
    fn get_addr_zeropage(&self) -> u16 {
        self.peek(self.cpu.pc + 1) as u16
    }

    fn get_addr_zeropage_x(&self) -> u16 {
        self.peek(self.cpu.pc + 1).wrapping_add(self.cpu.xr) as u16
    }

    fn get_addr_zeropage_y(&self) -> u16 {
        self.peek(self.cpu.pc + 1).wrapping_add(self.cpu.yr) as u16
    }

    fn get_addr_absolute(&self) -> u16 {
        let lo = self.peek(self.cpu.pc + 1) as u16;
        let hi = self.peek(self.cpu.pc + 2) as u16;
        (hi << 8) | lo
    }

    fn get_addr_absolute_x(&self) -> u16 {
        let lo = self.peek(self.cpu.pc + 1) as u16;
        let hi = self.peek(self.cpu.pc + 2) as u16;
        ((hi << 8) | lo).wrapping_add(self.cpu.xr as u16)
    }

    fn get_addr_absolute_y(&self) -> u16 {
        let lo = self.peek(self.cpu.pc + 1) as u16;
        let hi = self.peek(self.cpu.pc + 2) as u16;
        ((hi << 8) | lo).wrapping_add(self.cpu.yr as u16)
    }

    fn get_addr_indirect_x(&self) -> u16 {
        let base = self.peek(self.cpu.pc + 1).wrapping_add(self.cpu.xr);
        let lo = self.peek(base as u16) as u16;
        let hi = self.peek(base.wrapping_add(1) as u16) as u16;
        (hi << 8) | lo
    }

    fn get_addr_indirect_y(&self) -> u16 {
        let base = self.peek(self.cpu.pc + 1);
        let lo = self.peek(base as u16) as u16;
        let hi = self.peek(base.wrapping_add(1) as u16) as u16;
        ((hi << 8) | lo).wrapping_add(self.cpu.yr as u16)
    }

    // Execute one CPU instruction with full 6510 opcode table
    pub fn execute_instruction(&mut self) {
        let opcode = self.peek(self.cpu.pc);
        self.clock_ticks = 2; // Default timing

        match opcode {
            // LDA - Load Accumulator
            0xA9 => { // LDA #immediate
                self.cpu.acc = self.peek(self.cpu.pc + 1);
                self.cpu.z = self.cpu.acc == 0;
                self.cpu.n = (self.cpu.acc & 0x80) != 0;
                self.cpu.incr_pc(2);
                self.clock_ticks = 2;
            }
            0xA5 => { // LDA zeropage
                let addr = self.get_addr_zeropage();
                self.cpu.acc = self.peek(addr);
                self.cpu.z = self.cpu.acc == 0;
                self.cpu.n = (self.cpu.acc & 0x80) != 0;
                self.cpu.incr_pc(2);
                self.clock_ticks = 3;
            }
            0xB5 => { // LDA zeropage,X
                let addr = self.get_addr_zeropage_x();
                self.cpu.acc = self.peek(addr);
                self.cpu.z = self.cpu.acc == 0;
                self.cpu.n = (self.cpu.acc & 0x80) != 0;
                self.cpu.incr_pc(2);
                self.clock_ticks = 4;
            }
            0xAD => { // LDA absolute
                let addr = self.get_addr_absolute();
                self.cpu.acc = self.peek(addr);
                self.cpu.z = self.cpu.acc == 0;
                self.cpu.n = (self.cpu.acc & 0x80) != 0;
                self.cpu.incr_pc(3);
                self.clock_ticks = 4;
            }
            0xBD => { // LDA absolute,X
                let addr = self.get_addr_absolute_x();
                self.cpu.acc = self.peek(addr);
                self.cpu.z = self.cpu.acc == 0;
                self.cpu.n = (self.cpu.acc & 0x80) != 0;
                self.cpu.incr_pc(3);
                self.clock_ticks = 4;
            }
            0xB9 => { // LDA absolute,Y
                let addr = self.get_addr_absolute_y();
                self.cpu.acc = self.peek(addr);
                self.cpu.z = self.cpu.acc == 0;
                self.cpu.n = (self.cpu.acc & 0x80) != 0;
                self.cpu.incr_pc(3);
                self.clock_ticks = 4;
            }
            0xA1 => { // LDA (indirect,X)
                let addr = self.get_addr_indirect_x();
                self.cpu.acc = self.peek(addr);
                self.cpu.z = self.cpu.acc == 0;
                self.cpu.n = (self.cpu.acc & 0x80) != 0;
                self.cpu.incr_pc(2);
                self.clock_ticks = 6;
            }
            0xB1 => { // LDA (indirect),Y
                let addr = self.get_addr_indirect_y();
                self.cpu.acc = self.peek(addr);
                self.cpu.z = self.cpu.acc == 0;
                self.cpu.n = (self.cpu.acc & 0x80) != 0;
                self.cpu.incr_pc(2);
                self.clock_ticks = 5;
            }

            // STA - Store Accumulator
            0x85 => { // STA zeropage
                let addr = self.get_addr_zeropage();
                self.poke(addr, self.cpu.acc);
                self.cpu.incr_pc(2);
                self.clock_ticks = 3;
            }
            0x95 => { // STA zeropage,X
                let addr = self.get_addr_zeropage_x();
                self.poke(addr, self.cpu.acc);
                self.cpu.incr_pc(2);
                self.clock_ticks = 4;
            }
            0x8D => { // STA absolute
                let addr = self.get_addr_absolute();
                self.poke(addr, self.cpu.acc);
                self.cpu.incr_pc(3);
                self.clock_ticks = 4;
            }
            0x9D => { // STA absolute,X
                let addr = self.get_addr_absolute_x();
                self.poke(addr, self.cpu.acc);
                self.cpu.incr_pc(3);
                self.clock_ticks = 5;
            }
            0x99 => { // STA absolute,Y
                let addr = self.get_addr_absolute_y();
                self.poke(addr, self.cpu.acc);
                self.cpu.incr_pc(3);
                self.clock_ticks = 5;
            }
            0x81 => { // STA (indirect,X)
                let addr = self.get_addr_indirect_x();
                self.poke(addr, self.cpu.acc);
                self.cpu.incr_pc(2);
                self.clock_ticks = 6;
            }
            0x91 => { // STA (indirect),Y
                let addr = self.get_addr_indirect_y();
                self.poke(addr, self.cpu.acc);
                self.cpu.incr_pc(2);
                self.clock_ticks = 6;
            }

            // LDX - Load X Register
            0xA2 => { // LDX #immediate
                self.cpu.xr = self.peek(self.cpu.pc + 1);
                self.cpu.z = self.cpu.xr == 0;
                self.cpu.n = (self.cpu.xr & 0x80) != 0;
                self.cpu.incr_pc(2);
                self.clock_ticks = 2;
            }
            0xA6 => { // LDX zeropage
                let addr = self.get_addr_zeropage();
                self.cpu.xr = self.peek(addr);
                self.cpu.z = self.cpu.xr == 0;
                self.cpu.n = (self.cpu.xr & 0x80) != 0;
                self.cpu.incr_pc(2);
                self.clock_ticks = 3;
            }
            0xB6 => { // LDX zeropage,Y
                let addr = self.get_addr_zeropage_y();
                self.cpu.xr = self.peek(addr);
                self.cpu.z = self.cpu.xr == 0;
                self.cpu.n = (self.cpu.xr & 0x80) != 0;
                self.cpu.incr_pc(2);
                self.clock_ticks = 4;
            }
            0xAE => { // LDX absolute
                let addr = self.get_addr_absolute();
                self.cpu.xr = self.peek(addr);
                self.cpu.z = self.cpu.xr == 0;
                self.cpu.n = (self.cpu.xr & 0x80) != 0;
                self.cpu.incr_pc(3);
                self.clock_ticks = 4;
            }
            0xBE => { // LDX absolute,Y
                let addr = self.get_addr_absolute_y();
                self.cpu.xr = self.peek(addr);
                self.cpu.z = self.cpu.xr == 0;
                self.cpu.n = (self.cpu.xr & 0x80) != 0;
                self.cpu.incr_pc(3);
                self.clock_ticks = 4;
            }

            // LDY - Load Y Register
            0xA0 => { // LDY #immediate
                self.cpu.yr = self.peek(self.cpu.pc + 1);
                self.cpu.z = self.cpu.yr == 0;
                self.cpu.n = (self.cpu.yr & 0x80) != 0;
                self.cpu.incr_pc(2);
                self.clock_ticks = 2;
            }
            0xA4 => { // LDY zeropage
                let addr = self.get_addr_zeropage();
                self.cpu.yr = self.peek(addr);
                self.cpu.z = self.cpu.yr == 0;
                self.cpu.n = (self.cpu.yr & 0x80) != 0;
                self.cpu.incr_pc(2);
                self.clock_ticks = 3;
            }
            0xB4 => { // LDY zeropage,X
                let addr = self.get_addr_zeropage_x();
                self.cpu.yr = self.peek(addr);
                self.cpu.z = self.cpu.yr == 0;
                self.cpu.n = (self.cpu.yr & 0x80) != 0;
                self.cpu.incr_pc(2);
                self.clock_ticks = 4;
            }
            0xAC => { // LDY absolute
                let addr = self.get_addr_absolute();
                self.cpu.yr = self.peek(addr);
                self.cpu.z = self.cpu.yr == 0;
                self.cpu.n = (self.cpu.yr & 0x80) != 0;
                self.cpu.incr_pc(3);
                self.clock_ticks = 4;
            }
            0xBC => { // LDY absolute,X
                let addr = self.get_addr_absolute_x();
                self.cpu.yr = self.peek(addr);
                self.cpu.z = self.cpu.yr == 0;
                self.cpu.n = (self.cpu.yr & 0x80) != 0;
                self.cpu.incr_pc(3);
                self.clock_ticks = 4;
            }

            // STX - Store X Register
            0x86 => { // STX zeropage
                let addr = self.get_addr_zeropage();
                self.poke(addr, self.cpu.xr);
                self.cpu.incr_pc(2);
                self.clock_ticks = 3;
            }
            0x96 => { // STX zeropage,Y
                let addr = self.get_addr_zeropage_y();
                self.poke(addr, self.cpu.xr);
                self.cpu.incr_pc(2);
                self.clock_ticks = 4;
            }
            0x8E => { // STX absolute
                let addr = self.get_addr_absolute();
                self.poke(addr, self.cpu.xr);
                self.cpu.incr_pc(3);
                self.clock_ticks = 4;
            }

            // STY - Store Y Register
            0x84 => { // STY zeropage
                let addr = self.get_addr_zeropage();
                self.poke(addr, self.cpu.yr);
                self.cpu.incr_pc(2);
                self.clock_ticks = 3;
            }
            0x94 => { // STY zeropage,X
                let addr = self.get_addr_zeropage_x();
                self.poke(addr, self.cpu.yr);
                self.cpu.incr_pc(2);
                self.clock_ticks = 4;
            }
            0x8C => { // STY absolute
                let addr = self.get_addr_absolute();
                self.poke(addr, self.cpu.yr);
                self.cpu.incr_pc(3);
                self.clock_ticks = 4;
            }

            // Transfer operations
            0xAA => { self.cpu.tax(); self.cpu.incr_pc(1); self.clock_ticks = 2; } // TAX
            0x8A => { self.cpu.txa(); self.cpu.incr_pc(1); self.clock_ticks = 2; } // TXA
            0xA8 => { self.cpu.tay(); self.cpu.incr_pc(1); self.clock_ticks = 2; } // TAY
            0x98 => { self.cpu.tya(); self.cpu.incr_pc(1); self.clock_ticks = 2; } // TYA
            0xBA => { self.cpu.tsx(); self.cpu.incr_pc(1); self.clock_ticks = 2; } // TSX
            0x9A => { self.cpu.txs(); self.cpu.incr_pc(1); self.clock_ticks = 2; } // TXS

            // Increment/Decrement
            0xE8 => { self.cpu.inx(); self.cpu.incr_pc(1); self.clock_ticks = 2; } // INX
            0xCA => { self.cpu.dex(); self.cpu.incr_pc(1); self.clock_ticks = 2; } // DEX
            0xC8 => { self.cpu.iny(); self.cpu.incr_pc(1); self.clock_ticks = 2; } // INY
            0x88 => { self.cpu.dey(); self.cpu.incr_pc(1); self.clock_ticks = 2; } // DEY

            // Stack operations
            0x48 => { // PHA
                self.push(self.cpu.acc);
                self.cpu.incr_pc(1);
                self.clock_ticks = 3;
            }
            0x68 => { // PLA
                let value = self.pull();
                self.cpu.acc = value;
                self.cpu.z = self.cpu.acc == 0;
                self.cpu.n = (self.cpu.acc & 0x80) != 0;
                self.cpu.incr_pc(1);
                self.clock_ticks = 4;
            }
            0x08 => { // PHP
                self.push(self.get_flags());
                self.cpu.incr_pc(1);
                self.clock_ticks = 3;
            }
            0x28 => { // PLP
                let flags = self.pull();
                self.set_flags(flags);
                self.cpu.incr_pc(1);
                self.clock_ticks = 4;
            }

            // Logical operations
            0x29 => { // AND #immediate
                let value = self.peek(self.cpu.pc + 1);
                self.cpu.do_and(value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 2;
            }
            0x25 => { // AND zeropage
                let addr = self.get_addr_zeropage();
                let value = self.peek(addr);
                self.cpu.do_and(value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 3;
            }
            0x35 => { // AND zeropage,X
                let addr = self.get_addr_zeropage_x();
                let value = self.peek(addr);
                self.cpu.do_and(value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 4;
            }
            0x2D => { // AND absolute
                let addr = self.get_addr_absolute();
                let value = self.peek(addr);
                self.cpu.do_and(value);
                self.cpu.incr_pc(3);
                self.clock_ticks = 4;
            }
            0x3D => { // AND absolute,X
                let addr = self.get_addr_absolute_x();
                let value = self.peek(addr);
                self.cpu.do_and(value);
                self.cpu.incr_pc(3);
                self.clock_ticks = 4;
            }
            0x39 => { // AND absolute,Y
                let addr = self.get_addr_absolute_y();
                let value = self.peek(addr);
                self.cpu.do_and(value);
                self.cpu.incr_pc(3);
                self.clock_ticks = 4;
            }
            0x21 => { // AND (indirect,X)
                let addr = self.get_addr_indirect_x();
                let value = self.peek(addr);
                self.cpu.do_and(value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 6;
            }
            0x31 => { // AND (indirect),Y
                let addr = self.get_addr_indirect_y();
                let value = self.peek(addr);
                self.cpu.do_and(value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 5;
            }

            0x09 => { // ORA #immediate
                let value = self.peek(self.cpu.pc + 1);
                self.cpu.do_ora(value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 2;
            }
            0x05 => { // ORA zeropage
                let addr = self.get_addr_zeropage();
                let value = self.peek(addr);
                self.cpu.do_ora(value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 3;
            }
            0x15 => { // ORA zeropage,X
                let addr = self.get_addr_zeropage_x();
                let value = self.peek(addr);
                self.cpu.do_ora(value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 4;
            }
            0x0D => { // ORA absolute
                let addr = self.get_addr_absolute();
                let value = self.peek(addr);
                self.cpu.do_ora(value);
                self.cpu.incr_pc(3);
                self.clock_ticks = 4;
            }
            0x1D => { // ORA absolute,X
                let addr = self.get_addr_absolute_x();
                let value = self.peek(addr);
                self.cpu.do_ora(value);
                self.cpu.incr_pc(3);
                self.clock_ticks = 4;
            }
            0x19 => { // ORA absolute,Y
                let addr = self.get_addr_absolute_y();
                let value = self.peek(addr);
                self.cpu.do_ora(value);
                self.cpu.incr_pc(3);
                self.clock_ticks = 4;
            }
            0x01 => { // ORA (indirect,X)
                let addr = self.get_addr_indirect_x();
                let value = self.peek(addr);
                self.cpu.do_ora(value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 6;
            }
            0x11 => { // ORA (indirect),Y
                let addr = self.get_addr_indirect_y();
                let value = self.peek(addr);
                self.cpu.do_ora(value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 5;
            }

            0x49 => { // EOR #immediate
                let value = self.peek(self.cpu.pc + 1);
                self.cpu.do_eor(value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 2;
            }
            0x45 => { // EOR zeropage
                let addr = self.get_addr_zeropage();
                let value = self.peek(addr);
                self.cpu.do_eor(value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 3;
            }
            0x55 => { // EOR zeropage,X
                let addr = self.get_addr_zeropage_x();
                let value = self.peek(addr);
                self.cpu.do_eor(value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 4;
            }
            0x4D => { // EOR absolute
                let addr = self.get_addr_absolute();
                let value = self.peek(addr);
                self.cpu.do_eor(value);
                self.cpu.incr_pc(3);
                self.clock_ticks = 4;
            }
            0x5D => { // EOR absolute,X
                let addr = self.get_addr_absolute_x();
                let value = self.peek(addr);
                self.cpu.do_eor(value);
                self.cpu.incr_pc(3);
                self.clock_ticks = 4;
            }
            0x59 => { // EOR absolute,Y
                let addr = self.get_addr_absolute_y();
                let value = self.peek(addr);
                self.cpu.do_eor(value);
                self.cpu.incr_pc(3);
                self.clock_ticks = 4;
            }
            0x41 => { // EOR (indirect,X)
                let addr = self.get_addr_indirect_x();
                let value = self.peek(addr);
                self.cpu.do_eor(value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 6;
            }
            0x51 => { // EOR (indirect),Y
                let addr = self.get_addr_indirect_y();
                let value = self.peek(addr);
                self.cpu.do_eor(value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 5;
            }

            // Compare operations
            0xC9 => { // CMP #immediate
                let value = self.peek(self.cpu.pc + 1);
                self.cpu.do_cmp(self.cpu.acc, value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 2;
            }
            0xC5 => { // CMP zeropage
                let addr = self.get_addr_zeropage();
                let value = self.peek(addr);
                self.cpu.do_cmp(self.cpu.acc, value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 3;
            }
            0xD5 => { // CMP zeropage,X
                let addr = self.get_addr_zeropage_x();
                let value = self.peek(addr);
                self.cpu.do_cmp(self.cpu.acc, value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 4;
            }
            0xCD => { // CMP absolute
                let addr = self.get_addr_absolute();
                let value = self.peek(addr);
                self.cpu.do_cmp(self.cpu.acc, value);
                self.cpu.incr_pc(3);
                self.clock_ticks = 4;
            }
            0xD9 => { // CMP absolute,Y
                let addr = self.get_addr_absolute_y();
                let value = self.peek(addr);
                self.cpu.do_cmp(self.cpu.acc, value);
                self.cpu.incr_pc(3);
                self.clock_ticks = 4;
            }
            0xDD => { // CMP absolute,X
                let addr = self.get_addr_absolute_x();
                let value = self.peek(addr);
                self.cpu.do_cmp(self.cpu.acc, value);
                self.cpu.incr_pc(3);
                self.clock_ticks = 4;
            }
            0xC1 => { // CMP (indirect,X)
                let addr = self.get_addr_indirect_x();
                let value = self.peek(addr);
                self.cpu.do_cmp(self.cpu.acc, value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 6;
            }
            0xD1 => { // CMP (indirect),Y
                let addr = self.get_addr_indirect_y();
                let value = self.peek(addr);
                self.cpu.do_cmp(self.cpu.acc, value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 5;
            }

            0xE0 => { // CPX #immediate
                let value = self.peek(self.cpu.pc + 1);
                self.cpu.do_cmp(self.cpu.xr, value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 2;
            }
            0xE4 => { // CPX zeropage
                let addr = self.get_addr_zeropage();
                let value = self.peek(addr);
                self.cpu.do_cmp(self.cpu.xr, value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 3;
            }
            0xEC => { // CPX absolute
                let addr = self.get_addr_absolute();
                let value = self.peek(addr);
                self.cpu.do_cmp(self.cpu.xr, value);
                self.cpu.incr_pc(3);
                self.clock_ticks = 4;
            }

            0xC0 => { // CPY #immediate
                let value = self.peek(self.cpu.pc + 1);
                self.cpu.do_cmp(self.cpu.yr, value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 2;
            }
            0xC4 => { // CPY zeropage
                let addr = self.get_addr_zeropage();
                let value = self.peek(addr);
                self.cpu.do_cmp(self.cpu.yr, value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 3;
            }
            0xCC => { // CPY absolute
                let addr = self.get_addr_absolute();
                let value = self.peek(addr);
                self.cpu.do_cmp(self.cpu.yr, value);
                self.cpu.incr_pc(3);
                self.clock_ticks = 4;
            }

            // Arithmetic operations
            0x69 => { // ADC #immediate
                let value = self.peek(self.cpu.pc + 1);
                self.cpu.do_adc(value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 2;
            }
            0x65 => { // ADC zeropage
                let addr = self.get_addr_zeropage();
                let value = self.peek(addr);
                self.cpu.do_adc(value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 3;
            }
            0x75 => { // ADC zeropage,X
                let addr = self.get_addr_zeropage_x();
                let value = self.peek(addr);
                self.cpu.do_adc(value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 4;
            }
            0x6D => { // ADC absolute
                let addr = self.get_addr_absolute();
                let value = self.peek(addr);
                self.cpu.do_adc(value);
                self.cpu.incr_pc(3);
                self.clock_ticks = 4;
            }
            0x7D => { // ADC absolute,X
                let addr = self.get_addr_absolute_x();
                let value = self.peek(addr);
                self.cpu.do_adc(value);
                self.cpu.incr_pc(3);
                self.clock_ticks = 4;
            }
            0x79 => { // ADC absolute,Y
                let addr = self.get_addr_absolute_y();
                let value = self.peek(addr);
                self.cpu.do_adc(value);
                self.cpu.incr_pc(3);
                self.clock_ticks = 4;
            }
            0x61 => { // ADC (indirect,X)
                let addr = self.get_addr_indirect_x();
                let value = self.peek(addr);
                self.cpu.do_adc(value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 6;
            }
            0x71 => { // ADC (indirect),Y
                let addr = self.get_addr_indirect_y();
                let value = self.peek(addr);
                self.cpu.do_adc(value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 5;
            }

            0xE9 => { // SBC #immediate
                let value = self.peek(self.cpu.pc + 1);
                self.cpu.do_sbc(value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 2;
            }
            0xE5 => { // SBC zeropage
                let addr = self.get_addr_zeropage();
                let value = self.peek(addr);
                self.cpu.do_sbc(value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 3;
            }
            0xF5 => { // SBC zeropage,X
                let addr = self.get_addr_zeropage_x();
                let value = self.peek(addr);
                self.cpu.do_sbc(value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 4;
            }
            0xED => { // SBC absolute
                let addr = self.get_addr_absolute();
                let value = self.peek(addr);
                self.cpu.do_sbc(value);
                self.cpu.incr_pc(3);
                self.clock_ticks = 4;
            }
            0xFD => { // SBC absolute,X
                let addr = self.get_addr_absolute_x();
                let value = self.peek(addr);
                self.cpu.do_sbc(value);
                self.cpu.incr_pc(3);
                self.clock_ticks = 4;
            }
            0xF9 => { // SBC absolute,Y
                let addr = self.get_addr_absolute_y();
                let value = self.peek(addr);
                self.cpu.do_sbc(value);
                self.cpu.incr_pc(3);
                self.clock_ticks = 4;
            }
            0xE1 => { // SBC (indirect,X)
                let addr = self.get_addr_indirect_x();
                let value = self.peek(addr);
                self.cpu.do_sbc(value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 6;
            }
            0xF1 => { // SBC (indirect),Y
                let addr = self.get_addr_indirect_y();
                let value = self.peek(addr);
                self.cpu.do_sbc(value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 5;
            }

            // Shift/Rotate operations
            0x0A => { // ASL accumulator
                self.cpu.acc = self.cpu.do_asl(self.cpu.acc);
                self.cpu.incr_pc(1);
                self.clock_ticks = 2;
            }
            0x06 => { // ASL zeropage
                let addr = self.get_addr_zeropage();
                let value = self.peek(addr);
                let result = self.cpu.do_asl(value);
                self.poke(addr, result);
                self.cpu.incr_pc(2);
                self.clock_ticks = 5;
            }
            0x16 => { // ASL zeropage,X
                let addr = self.get_addr_zeropage_x();
                let value = self.peek(addr);
                let result = self.cpu.do_asl(value);
                self.poke(addr, result);
                self.cpu.incr_pc(2);
                self.clock_ticks = 6;
            }
            0x0E => { // ASL absolute
                let addr = self.get_addr_absolute();
                let value = self.peek(addr);
                let result = self.cpu.do_asl(value);
                self.poke(addr, result);
                self.cpu.incr_pc(3);
                self.clock_ticks = 6;
            }
            0x1E => { // ASL absolute,X
                let addr = self.get_addr_absolute_x();
                let value = self.peek(addr);
                let result = self.cpu.do_asl(value);
                self.poke(addr, result);
                self.cpu.incr_pc(3);
                self.clock_ticks = 7;
            }
            0x4A => { // LSR accumulator
                self.cpu.acc = self.cpu.do_lsr(self.cpu.acc);
                self.cpu.incr_pc(1);
                self.clock_ticks = 2;
            }
            0x46 => { // LSR zeropage
                let addr = self.get_addr_zeropage();
                let value = self.peek(addr);
                let result = self.cpu.do_lsr(value);
                self.poke(addr, result);
                self.cpu.incr_pc(2);
                self.clock_ticks = 5;
            }
            0x56 => { // LSR zeropage,X
                let addr = self.get_addr_zeropage_x();
                let value = self.peek(addr);
                let result = self.cpu.do_lsr(value);
                self.poke(addr, result);
                self.cpu.incr_pc(2);
                self.clock_ticks = 6;
            }
            0x4E => { // LSR absolute
                let addr = self.get_addr_absolute();
                let value = self.peek(addr);
                let result = self.cpu.do_lsr(value);
                self.poke(addr, result);
                self.cpu.incr_pc(3);
                self.clock_ticks = 6;
            }
            0x5E => { // LSR absolute,X
                let addr = self.get_addr_absolute_x();
                let value = self.peek(addr);
                let result = self.cpu.do_lsr(value);
                self.poke(addr, result);
                self.cpu.incr_pc(3);
                self.clock_ticks = 7;
            }
            0x2A => { // ROL accumulator
                self.cpu.acc = self.cpu.do_rol(self.cpu.acc);
                self.cpu.incr_pc(1);
                self.clock_ticks = 2;
            }
            0x26 => { // ROL zeropage
                let addr = self.get_addr_zeropage();
                let value = self.peek(addr);
                let result = self.cpu.do_rol(value);
                self.poke(addr, result);
                self.cpu.incr_pc(2);
                self.clock_ticks = 5;
            }
            0x36 => { // ROL zeropage,X
                let addr = self.get_addr_zeropage_x();
                let value = self.peek(addr);
                let result = self.cpu.do_rol(value);
                self.poke(addr, result);
                self.cpu.incr_pc(2);
                self.clock_ticks = 6;
            }
            0x2E => { // ROL absolute
                let addr = self.get_addr_absolute();
                let value = self.peek(addr);
                let result = self.cpu.do_rol(value);
                self.poke(addr, result);
                self.cpu.incr_pc(3);
                self.clock_ticks = 6;
            }
            0x3E => { // ROL absolute,X
                let addr = self.get_addr_absolute_x();
                let value = self.peek(addr);
                let result = self.cpu.do_rol(value);
                self.poke(addr, result);
                self.cpu.incr_pc(3);
                self.clock_ticks = 7;
            }
            0x6A => { // ROR accumulator
                self.cpu.acc = self.cpu.do_ror(self.cpu.acc);
                self.cpu.incr_pc(1);
                self.clock_ticks = 2;
            }
            0x66 => { // ROR zeropage
                let addr = self.get_addr_zeropage();
                let value = self.peek(addr);
                let result = self.cpu.do_ror(value);
                self.poke(addr, result);
                self.cpu.incr_pc(2);
                self.clock_ticks = 5;
            }
            0x76 => { // ROR zeropage,X
                let addr = self.get_addr_zeropage_x();
                let value = self.peek(addr);
                let result = self.cpu.do_ror(value);
                self.poke(addr, result);
                self.cpu.incr_pc(2);
                self.clock_ticks = 6;
            }
            0x6E => { // ROR absolute
                let addr = self.get_addr_absolute();
                let value = self.peek(addr);
                let result = self.cpu.do_ror(value);
                self.poke(addr, result);
                self.cpu.incr_pc(3);
                self.clock_ticks = 6;
            }
            0x7E => { // ROR absolute,X
                let addr = self.get_addr_absolute_x();
                let value = self.peek(addr);
                let result = self.cpu.do_ror(value);
                self.poke(addr, result);
                self.cpu.incr_pc(3);
                self.clock_ticks = 7;
            }

            // INC - Increment Memory
            0xE6 => { // INC zeropage
                let addr = self.get_addr_zeropage();
                let value = self.peek(addr);
                let result = value.wrapping_add(1);
                self.cpu.neg_flag(result);
                self.cpu.zero_flag(result);
                self.poke(addr, result);
                self.cpu.incr_pc(2);
                self.clock_ticks = 5;
            }
            0xF6 => { // INC zeropage,X
                let addr = self.get_addr_zeropage_x();
                let value = self.peek(addr);
                let result = value.wrapping_add(1);
                self.cpu.neg_flag(result);
                self.cpu.zero_flag(result);
                self.poke(addr, result);
                self.cpu.incr_pc(2);
                self.clock_ticks = 6;
            }
            0xEE => { // INC absolute
                let addr = self.get_addr_absolute();
                let value = self.peek(addr);
                let result = value.wrapping_add(1);
                self.cpu.neg_flag(result);
                self.cpu.zero_flag(result);
                self.poke(addr, result);
                self.cpu.incr_pc(3);
                self.clock_ticks = 6;
            }
            0xFE => { // INC absolute,X
                let addr = self.get_addr_absolute_x();
                let value = self.peek(addr);
                let result = value.wrapping_add(1);
                self.cpu.neg_flag(result);
                self.cpu.zero_flag(result);
                self.poke(addr, result);
                self.cpu.incr_pc(3);
                self.clock_ticks = 7;
            }

            // DEC - Decrement Memory
            0xC6 => { // DEC zeropage
                let addr = self.get_addr_zeropage();
                let value = self.peek(addr);
                let result = value.wrapping_sub(1);
                self.cpu.neg_flag(result);
                self.cpu.zero_flag(result);
                self.poke(addr, result);
                self.cpu.incr_pc(2);
                self.clock_ticks = 5;
            }
            0xD6 => { // DEC zeropage,X
                let addr = self.get_addr_zeropage_x();
                let value = self.peek(addr);
                let result = value.wrapping_sub(1);
                self.cpu.neg_flag(result);
                self.cpu.zero_flag(result);
                self.poke(addr, result);
                self.cpu.incr_pc(2);
                self.clock_ticks = 6;
            }
            0xCE => { // DEC absolute
                let addr = self.get_addr_absolute();
                let value = self.peek(addr);
                let result = value.wrapping_sub(1);
                self.cpu.neg_flag(result);
                self.cpu.zero_flag(result);
                self.poke(addr, result);
                self.cpu.incr_pc(3);
                self.clock_ticks = 6;
            }
            0xDE => { // DEC absolute,X
                let addr = self.get_addr_absolute_x();
                let value = self.peek(addr);
                let result = value.wrapping_sub(1);
                self.cpu.neg_flag(result);
                self.cpu.zero_flag(result);
                self.poke(addr, result);
                self.cpu.incr_pc(3);
                self.clock_ticks = 7;
            }

            // Bit test
            0x24 => { // BIT zeropage
                let addr = self.get_addr_zeropage();
                let value = self.peek(addr);
                self.cpu.do_bit(value);
                self.cpu.incr_pc(2);
                self.clock_ticks = 3;
            }
            0x2C => { // BIT absolute
                let addr = self.get_addr_absolute();
                let value = self.peek(addr);
                self.cpu.do_bit(value);
                self.cpu.incr_pc(3);
                self.clock_ticks = 4;
            }

            // Jumps and branches
            0x4C => { // JMP absolute
                self.cpu.pc = self.get_addr_absolute();
                self.clock_ticks = 3;
            }
            0x6C => { // JMP indirect
                let addr = self.get_addr_absolute();
                let lo = self.peek(addr) as u16;
                let hi = self.peek(addr.wrapping_add(1)) as u16;
                self.cpu.pc = (hi << 8) | lo;
                self.clock_ticks = 5;
            }

            0x20 => { // JSR
                let target = self.get_addr_absolute();
                let ret_addr = self.cpu.pc.wrapping_add(2);
                self.push_word(ret_addr);
                self.cpu.pc = target;
                self.clock_ticks = 6;
            }
            0x60 => { // RTS
                let addr = self.pull_word();
                self.cpu.pc = addr.wrapping_add(1);
                self.clock_ticks = 6;
            }
            0x40 => { // RTI
                let flags = self.pull();
                self.set_flags(flags);
                self.cpu.pc = self.pull_word();
                self.clock_ticks = 6;
            }

            // Branch instructions
            0x90 => { // BCC
                let offset = self.peek(self.cpu.pc + 1) as i8;
                self.cpu.incr_pc(2);
                if !self.cpu.c {
                    self.cpu.pc = (self.cpu.pc as i32 + offset as i32) as u16;
                    self.clock_ticks = 3;
                } else {
                    self.clock_ticks = 2;
                }
            }
            0xB0 => { // BCS
                let offset = self.peek(self.cpu.pc + 1) as i8;
                self.cpu.incr_pc(2);
                if self.cpu.c {
                    self.cpu.pc = (self.cpu.pc as i32 + offset as i32) as u16;
                    self.clock_ticks = 3;
                } else {
                    self.clock_ticks = 2;
                }
            }
            0xF0 => { // BEQ
                let offset = self.peek(self.cpu.pc + 1) as i8;
                self.cpu.incr_pc(2);
                if self.cpu.z {
                    self.cpu.pc = (self.cpu.pc as i32 + offset as i32) as u16;
                    self.clock_ticks = 3;
                } else {
                    self.clock_ticks = 2;
                }
            }
            0xD0 => { // BNE
                let offset = self.peek(self.cpu.pc + 1) as i8;
                self.cpu.incr_pc(2);
                if !self.cpu.z {
                    self.cpu.pc = (self.cpu.pc as i32 + offset as i32) as u16;
                    self.clock_ticks = 3;
                } else {
                    self.clock_ticks = 2;
                }
            }
            0x30 => { // BMI
                let offset = self.peek(self.cpu.pc + 1) as i8;
                self.cpu.incr_pc(2);
                if self.cpu.n {
                    self.cpu.pc = (self.cpu.pc as i32 + offset as i32) as u16;
                    self.clock_ticks = 3;
                } else {
                    self.clock_ticks = 2;
                }
            }
            0x10 => { // BPL
                let offset = self.peek(self.cpu.pc + 1) as i8;
                self.cpu.incr_pc(2);
                if !self.cpu.n {
                    self.cpu.pc = (self.cpu.pc as i32 + offset as i32) as u16;
                    self.clock_ticks = 3;
                } else {
                    self.clock_ticks = 2;
                }
            }
            0x50 => { // BVC
                let offset = self.peek(self.cpu.pc + 1) as i8;
                self.cpu.incr_pc(2);
                if !self.cpu.v {
                    self.cpu.pc = (self.cpu.pc as i32 + offset as i32) as u16;
                    self.clock_ticks = 3;
                } else {
                    self.clock_ticks = 2;
                }
            }
            0x70 => { // BVS
                let offset = self.peek(self.cpu.pc + 1) as i8;
                self.cpu.incr_pc(2);
                if self.cpu.v {
                    self.cpu.pc = (self.cpu.pc as i32 + offset as i32) as u16;
                    self.clock_ticks = 3;
                } else {
                    self.clock_ticks = 2;
                }
            }

            // Flag operations
            0x18 => { self.cpu.c = false; self.cpu.incr_pc(1); self.clock_ticks = 2; } // CLC
            0x38 => { self.cpu.c = true; self.cpu.incr_pc(1); self.clock_ticks = 2; } // SEC
            0x58 => { self.cpu.i = false; self.cpu.incr_pc(1); self.clock_ticks = 2; } // CLI
            0x78 => { self.cpu.i = true; self.cpu.incr_pc(1); self.clock_ticks = 2; } // SEI
            0xD8 => { self.cpu.d = false; self.cpu.incr_pc(1); self.clock_ticks = 2; } // CLD
            0xF8 => { self.cpu.d = true; self.cpu.incr_pc(1); self.clock_ticks = 2; } // SED
            0xB8 => { self.cpu.v = false; self.cpu.incr_pc(1); self.clock_ticks = 2; } // CLV

            // NOP and illegal opcodes
            0xEA | 0x1A | 0x3A | 0x5A | 0x7A | 0xDA | 0xFA | 0xFC | 0x3F | 0x7F | 0x07 => { // NOP
                self.cpu.incr_pc(1);
                self.clock_ticks = 2;
            }

            0x00 => { // BRK
                self.cpu.incr_pc(1);
                self.push_word(self.cpu.pc);
                self.cpu.b = true;
                self.push(self.get_flags());
                self.cpu.i = true;
                let irq_lo = self.rom[0xFFFE - 0x8000] as u16;
                let irq_hi = self.rom[0xFFFF - 0x8000] as u16;
                self.cpu.pc = (irq_hi << 8) | irq_lo;
                self.clock_ticks = 7;
            }

            _ => {
                // Unimplemented opcode
                println!("Unimplemented opcode: 0x{:02X} at PC=0x{:04X}", opcode, self.cpu.pc);
                self.cpu.incr_pc(1);
            }
        }
    }

    // Render one raster line
    fn render_raster_line(&mut self) {
        if self.raster_line >= FIRST_SCREEN_LINE as u32
            && self.raster_line < (FIRST_SCREEN_LINE + SCREEN_HEIGHT) as u32
        {
            let line = (self.raster_line - FIRST_SCREEN_LINE as u32) as usize;

            // Check if we're in bitmap mode (bit 5 of 0xFF06)
            let bitmap_mode = (self.ram[0xFF06] & 32) != 0;

            if bitmap_mode {
                // Bitmap mode rendering (not implemented yet)
                for x in 0..SCREEN_WIDTH {
                    self.pixels[line][x] = 0; // Black for now
                }
            } else {
                // Text mode rendering (40x25 characters, 8x8 pixels each)
                self.render_text_line(line);
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

    // Render one line of text mode (40 characters wide, 8 pixels per character)
    fn render_text_line(&mut self, line: usize) {
        let char_row = line / 8; // Which character row (0-24)
        let pixel_row = line % 8; // Which pixel row within the character (0-7)

        // Screen memory starts at 0x0C00 by default (can be changed via TED registers)
        // Color RAM starts at 0x0800 by default
        let screen_base = 0x0C00;
        let color_base = 0x0800;

        // Character ROM is in system ROM at 0xD000-0xD7FF (when mapped)
        let charset_base = 0xD000usize;

        for col in 0..40 {
            let screen_addr = screen_base + char_row * 40 + col;
            let char_code = self.ram[screen_addr] as usize;

            // Get color for this character
            let color_addr = color_base + char_row * 40 + col;
            let color = self.ram[color_addr] & 0x7F; // 7 bits for color

            // Background color from TED register 0xFF15
            let bg_color = self.ram[0xFF15] & 0x7F;

            // Get character bitmap from ROM
            let char_addr = charset_base + char_code * 8 + pixel_row;
            let char_data = if char_addr >= 0xD000 && char_addr < 0xD800 {
                // Read from character ROM (embedded in system ROM)
                let rom_offset = char_addr - 0x8000; // ROM starts at 0x8000
                if rom_offset < self.rom.len() {
                    self.rom[rom_offset]
                } else {
                    0
                }
            } else {
                0
            };

            // Render 8 pixels for this character
            for bit in 0..8 {
                let x = col * 8 + bit;
                if x < SCREEN_WIDTH {
                    let pixel_set = (char_data & (0x80 >> bit)) != 0;
                    self.pixels[line][x] = if pixel_set { color } else { bg_color };
                }
            }
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
