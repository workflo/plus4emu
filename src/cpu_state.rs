/// CPU State for 6510 processor
/// Copyright (C) 2009 Florian Wolff (florian@donuz.de)
/// Rust port 2025
///
/// This program is free software; you can redistribute it and/or
/// modify it under the terms of the GNU General Public License
/// as published by the Free Software Foundation; either version 2
/// of the License, or (at your option) any later version.

#[derive(Debug, Clone)]
pub struct CpuState {
    // Registers
    pub pc: u16,      // Program Counter
    pub acc: u8,      // Accumulator
    pub xr: u8,       // X Register
    pub yr: u8,       // Y Register
    pub sp: u8,       // Stack Pointer

    // Status Flags
    pub c: bool,      // Carry
    pub z: bool,      // Zero
    pub n: bool,      // Negative
    pub b: bool,      // Break
    pub v: bool,      // Overflow
    pub i: bool,      // Interrupt Disable
    pub d: bool,      // Decimal Mode
}

impl Default for CpuState {
    fn default() -> Self {
        Self {
            pc: 0,
            acc: 0,
            xr: 0,
            yr: 0,
            sp: 0xFF,
            c: false,
            z: false,
            n: false,
            b: false,
            v: false,
            i: false,
            d: false,
        }
    }
}

impl CpuState {
    pub fn new() -> Self {
        Self::default()
    }

    // Program Counter operations
    pub fn incr_pc(&mut self, inc: u16) {
        self.pc = self.pc.wrapping_add(inc) & 0xFFFF;
    }

    // Stack Pointer operations
    pub fn incr_sp(&mut self) {
        self.sp = self.sp.wrapping_add(1);
    }

    pub fn decr_sp(&mut self) {
        self.sp = self.sp.wrapping_sub(1);
    }

    // Flag operations
    pub fn neg_flag(&mut self, byte: u8) {
        self.n = (byte & 0x80) != 0;
    }

    pub fn zero_flag(&mut self, byte: u8) {
        self.z = byte == 0;
    }

    // ADC - Add with Carry
    pub fn do_adc(&mut self, data: u8) {
        let old_acc = self.acc;

        if self.d {
            // Decimal mode not implemented yet
            panic!("Decimal mode ADC not implemented");
        } else {
            // Binary mode
            let raw = data as u16 + self.acc as u16 + if self.c { 1 } else { 0 };
            self.acc = raw as u8;
            self.c = raw > 0xFF;

            self.neg_flag(self.acc);
            self.zero_flag(self.acc);

            self.v = false;
            if self.n && (self.acc & 0x80) != 0 && (data & 0x80) == 0 && (old_acc & 0x80) == 0 {
                self.v = true;
            }
            if self.n && (self.acc & 0x80) == 0 && (data & 0x80) != 0 && (old_acc & 0x80) != 0 {
                self.v = true;
            }
            if !self.n && (self.acc & 0x80) == 0 && (data & 0x80) != 0 && (old_acc & 0x80) != 0 {
                self.v = true;
            }
        }
    }

    // AND - Logical AND
    pub fn do_and(&mut self, second: u8) {
        self.acc &= second;
        self.neg_flag(self.acc);
        self.zero_flag(self.acc);
    }

    // ASL - Arithmetic Shift Left
    pub fn do_asl(&mut self, data: u8) -> u8 {
        self.c = (data & 0x80) != 0;
        let result = data << 1;
        self.zero_flag(result);
        self.neg_flag(result);
        result
    }

    // BIT - Bit Test
    pub fn do_bit(&mut self, data: u8) {
        self.v = (data & 64) != 0;
        self.n = (data & 128) != 0;
        self.z = (data & self.acc) == 0;
    }

    // CMP - Compare
    pub fn do_cmp(&mut self, reg: u8, data: u8) {
        self.c = reg >= data;
        self.z = data == reg;
        self.n = reg.wrapping_sub(data) > 0x7F;
    }

    // EOR - Exclusive OR
    pub fn do_eor(&mut self, value: u8) {
        self.acc ^= value;
        self.neg_flag(self.acc);
        self.zero_flag(self.acc);
    }

    // LSR - Logical Shift Right
    pub fn do_lsr(&mut self, data: u8) -> u8 {
        self.c = (data & 0x01) != 0;
        let result = data >> 1;
        self.zero_flag(result);
        self.n = false;
        result
    }

    // ORA - Logical OR
    pub fn do_ora(&mut self, value: u8) {
        self.acc |= value;
        self.neg_flag(self.acc);
        self.zero_flag(self.acc);
    }

    // ROL - Rotate Left
    pub fn do_rol(&mut self, data: u8) -> u8 {
        let new_c = (data & 0x80) != 0;
        let mut result = data << 1;
        if self.c {
            result |= 1;
        }
        self.c = new_c;
        self.neg_flag(result);
        self.zero_flag(result);
        result
    }

    // ROR - Rotate Right
    pub fn do_ror(&mut self, data: u8) -> u8 {
        let new_c = (data & 1) != 0;
        let mut result = data >> 1;
        if self.c {
            result |= 0x80;
        }
        self.c = new_c;
        self.neg_flag(result);
        self.zero_flag(result);
        result
    }

    // SBC - Subtract with Carry
    pub fn do_sbc(&mut self, data: u8) {
        let old_acc = self.acc;

        if self.d {
            // Decimal mode
            let mut tmp_dec = (self.acc as i32 & 0x0F) - (data as i32 & 0x0F) - if self.c { 0 } else { 1 };

            if (tmp_dec & 0x10) != 0 {
                tmp_dec = ((tmp_dec - 6) & 0xF) | ((self.acc as i32 & 0xF0) - (data as i32 & 0xF0) - 0x10);
            } else {
                tmp_dec = (tmp_dec & 0xF) | ((self.acc as i32 & 0xF0) - (data as i32 & 0xF0));
            }
            if (tmp_dec & 0x100) != 0 {
                tmp_dec -= 0x60;
            }

            self.acc = tmp_dec as u8;
            self.c = tmp_dec < 0x100;

            self.neg_flag(self.acc);
            self.zero_flag(self.acc);

            self.v = false;
            if !self.n && (self.acc & 0x80) != 0 && (data & 0x80) != 0 && (old_acc & 0x80) == 0 {
                self.v = true;
            }
            if !self.n && (self.acc & 0x80) == 0 && (data & 0x80) == 0 && (old_acc & 0x80) != 0 {
                self.v = true;
            }
            if self.n && (self.acc & 0x80) != 0 && (data & 0x80) != 0 && (old_acc & 0x80) == 0 {
                self.v = true;
            }
        } else {
            // Binary mode
            let raw = self.acc as i32 - data as i32 - if self.c { 0 } else { 1 };

            self.acc = raw as u8;
            self.c = raw < 0x100;

            self.neg_flag(self.acc);
            self.zero_flag(self.acc);

            self.v = false;
            if !self.n && (self.acc & 0x80) != 0 && (data & 0x80) != 0 && (old_acc & 0x80) == 0 {
                self.v = true;
            }
            if !self.n && (self.acc & 0x80) == 0 && (data & 0x80) == 0 && (old_acc & 0x80) != 0 {
                self.v = true;
            }
            if self.n && (self.acc & 0x80) != 0 && (data & 0x80) != 0 && (old_acc & 0x80) == 0 {
                self.v = true;
            }
        }
    }

    // Register operations
    pub fn dey(&mut self) {
        self.yr = self.yr.wrapping_sub(1);
        self.neg_flag(self.yr);
        self.zero_flag(self.yr);
    }

    pub fn iny(&mut self) {
        self.yr = self.yr.wrapping_add(1);
        self.neg_flag(self.yr);
        self.zero_flag(self.yr);
    }

    pub fn dex(&mut self) {
        self.xr = self.xr.wrapping_sub(1);
        self.neg_flag(self.xr);
        self.zero_flag(self.xr);
    }

    pub fn inx(&mut self) {
        self.xr = self.xr.wrapping_add(1);
        self.neg_flag(self.xr);
        self.zero_flag(self.xr);
    }

    // Transfer operations
    pub fn txa(&mut self) {
        self.acc = self.xr;
        self.neg_flag(self.acc);
        self.zero_flag(self.acc);
    }

    pub fn tax(&mut self) {
        self.xr = self.acc;
        self.neg_flag(self.xr);
        self.zero_flag(self.xr);
    }

    pub fn tya(&mut self) {
        self.acc = self.yr;
        self.neg_flag(self.acc);
        self.zero_flag(self.acc);
    }

    pub fn tay(&mut self) {
        self.yr = self.acc;
        self.neg_flag(self.yr);
        self.zero_flag(self.yr);
    }

    pub fn tsx(&mut self) {
        self.xr = self.sp;
        self.neg_flag(self.xr);
        self.zero_flag(self.xr);
    }

    pub fn txs(&mut self) {
        self.sp = self.xr;
    }
}
