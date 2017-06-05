/**
 * plus4
 * Copyright (C) 2009 Florian Wolff  (florian@donuz.de)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * $Id: Plus4.java,v 1.24 2009/06/14 15:49:49 florian Exp $
 */
package de.donuz.plus4;

import de.donuz.plus4.helper.*;

import java.io.*;

public class Plus4
{
    /** Our Read Only Memory. */
    private final short[] ram = new short[0xffff];

    /** Our Random Access Memory. */
    private final short[] rom = new short[0x8000];

    /** Our 3plus1 Random Access Memory. */
    private final short[] rom3plus1 = new short[0x8000];

    private boolean romActive;

    private int romConfig = 0;

    private final boolean[] timerOn = new boolean[3];

    private final CpuState cpu = new CpuState();

    private int clockTicks = 0;

    private int clockCounter = 0;

    private int flashCounter = 0;

    private int rasterLine = 0;

    private boolean flashOn = false;

    private final Screen screen;

    private final short[][] pixels = new short[SCREENHEIGHT][SCREENWIDTH];
    
    public final static int CLOCKFREQUENCY = 885000;
    public final static int IRQFREQUENCY = (CLOCKFREQUENCY / 60);
    public final static int RASTERLINES = 312;
    public final static int SCREENREFRESHFREQUENCY = 57;
    public final static int _TAKTEPRORASTERZEILE = (CLOCKFREQUENCY / (SCREENREFRESHFREQUENCY * RASTERLINES));
    public final static int TAKTEPRORASTERZEILE = 114;
    public final static int TAKTEPROBLINKINTERVALL = (CLOCKFREQUENCY / 8);
    public final static int FX = 1;
    public final static int SCREENWIDTH = 320;
    public final static int SCREENHEIGHT = 200;
    public final static int FIRSTSCREENLINE = 3;


    public Plus4(Screen screen) throws IOException
    {
        this.screen = screen;
        loadRom();
    }


    private void loadRom() throws IOException
    {
        final InputStream in = Plus4.class.getClassLoader().getResourceAsStream("de/donuz/plus4/rom/rom.bin");

        if (in == null) {
            throw new FileNotFoundException("rom.bin not found");
        } else {
            IOHelper.ReadBytes(rom, in, 0x8000);
        }

        final InputStream in2 = Plus4.class.getClassLoader().getResourceAsStream("de/donuz/plus4/rom/3plus1.bin");

        if (in2 == null) {
            throw new FileNotFoundException("3plus1.bin not found");
        } else {
            IOHelper.ReadBytes(rom3plus1, in2, 0x8000);
        }
    }

    public short Peek(int addr)
    {
        addr = toWord(addr);

        /*if (adresse >= 0xfd00 && adresse < 0xff20)
        {
        return ram[adresse];
        }*/
        if ((addr >= 0xfd00 && addr <= 0xfdff) ||
                (addr >= 0xff00 && addr <= 0xff3f) ) {
            return ram[addr];
        }

        if (addr < 0x8000 || !romActive) {
            return ram[addr];
        }

        if (addr < 0xc000) {
            if ((romConfig & 3) == 0)
                return rom(addr);
            if ((romConfig & 3) == 1)
                return rom3plus1[addr & 0x7fff];
            return 0;
        }

        if (addr >= 0xfc00 && addr < 0xfd00)
            return rom(addr);

        if (((romConfig >> 2) & 3) == 0)
            return rom(addr);
        if (((romConfig >> 2) & 3) == 1)
            return rom3plus1[addr & 0x7fff];

        return 0;
    }


    /*****************************************************************************\
    | Poke                                                                        |
    \*****************************************************************************/
    public void poke(int addr, short by)
    {
        addr = toWord(addr);
        
        if (addr == 0xff3e) {
            // Activate ROM:
            romActive = true;
//            System.out.println("ROM!");
        } else if (addr == 0xff3f) {
            // Actibe RAM:
            romActive = false;
//            System.out.println("RAM!");
        } else if (addr >= 0xfdd0 && addr <= 0xfddf) {
            // Bankswitching:
            romConfig = (addr & 15);
        } else {
            if (addr != 0xff08) {
                ram[addr] = by;
            }
            if (addr == 0xfd30) {
                p4Keyboard();
            }

            // IO:
            /*
       if (ad >= 0xfd00 && ad < 0xff00)
       {
          fprintf(out, "IO-Adresse $%04x = $%02x \"%c\"\n", ad, by, ((by > 31) ? by : ' '));
          if (ad == 0xfec0 + UNIT8OFFSET) Poke (0xfec2 + UNIT8OFFSET, 0x7f);
          return;
       }
             */
            // TED:

            if (addr >= 0xff00 && addr <= 0xff1f) {
                switch(addr) {
                    case 0xff00: timerOn[0] = false; break;
                    case 0xff01: timerOn[0] = true; break;
                    case 0xff02: timerOn[1] = false; break;
                    case 0xff03: timerOn[1] = true; break;
                    case 0xff04: timerOn[2] = false; break;
                    case 0xff05: timerOn[2] = true; break;
                    //         case 0xff06: if (!(ram[0xff06] & 16)) vga_box(0, 0, 319, 199, ram[0xff19] & 127);
                    //                      break;
                    case 0xff08: //P4Tastatur(); break;
                        if (by == 0xfa)// && pcjoy == 1) 
                        {
                            p4Joystick(1);
                        }
                        if (by == 0xfd)// && pcjoy == 2)
                        {
                            p4Joystick(2);
                        }
                        p4Keyboard();
                        break;
                }
            }
        }
    }

    /*****************************************************************************\
    | GetByte                                                                     |
    \*****************************************************************************/
    public short GetByte(AddressMode adrtype)
    {
        switch(adrtype) {
            case UNMITTELBAR:
                return Peek( cpu.pc + 1 );
            case ZEROPAGE:
                return Peek( getAddress(AddressMode.ZEROPAGE) );
            case ABSOLUT:
                return Peek( getAddress(AddressMode.ABSOLUT) );
            case INDIZIERTX:
                return Peek( getAddress(AddressMode.INDIZIERTX) );
            case INDIZIERTY:
                return Peek( getAddress(AddressMode.INDIZIERTY) );
            case ZEROPAGEX:
                return Peek( getAddress(AddressMode.ZEROPAGEX) );
            case ZEROPAGEY:
                return Peek( getAddress(AddressMode.ZEROPAGEY) );
            case INDIREKTINDIZIERTY:
                return Peek( getAddress(AddressMode.INDIREKTINDIZIERTY) );
            case INDIZIERTINDIREKTX:
                return Peek( getAddress(AddressMode.INDIZIERTINDIREKTX) );
            default:
                throw new IllegalStateException();
        }
    }


    /*****************************************************************************\
    | GetAdress                                                                   |
    \*****************************************************************************/
    protected int getAddress(AddressMode type)
    {
        switch(type) {
            case ZEROPAGE:
                return Peek(cpu.pc + 1);

            case ABSOLUT:
                return (Peek(cpu.pc + 1) + (Peek(cpu.pc + 2) << 8));

            case INDIZIERTX:
                if (Peek(cpu.pc + 1) + cpu.xr > 0xff)
                    clockTicks = 1;
                return toWord(Peek(cpu.pc + 1) + (Peek(cpu.pc + 2) << 8) + cpu.xr);

            case INDIZIERTY:
                if (Peek(cpu.pc + 1) + cpu.yr > 0xff)
                    clockTicks = 1;
                return toWord(Peek(cpu.pc + 1) + (Peek(cpu.pc + 2) << 8) + cpu.yr);

            case ZEROPAGEX:
                return (Peek(cpu.pc + 1) + cpu.xr) & 0x00ff;

            case ZEROPAGEY:
                return (Peek(cpu.pc + 1) + cpu.yr) & 0x00ff;

            case INDIREKTINDIZIERTY:
                if ( Peek(Peek(cpu.pc+1)) + cpu.yr > 0xff )
                    clockTicks = 1;
                return toWord(Peek(Peek(cpu.pc+1))
                              + (Peek(Peek(cpu.pc+1)+1) << 8)
                              + cpu.yr   );

            case INDIZIERTINDIREKTX:
                return ( (   Peek(Peek(cpu.pc+1+cpu.xr))
                        + (Peek(Peek(cpu.pc+1+cpu.xr)+1) << 8) ) & 0xff );
                
            default:
                throw new IllegalStateException();
        }
    }


    /*****************************************************************************\
    | Push                                                                        |
    \*****************************************************************************/
    public void Push(short by)
    {
        ram[0x100 + cpu.sp] = by;
        cpu.decrSp();
    }


    /*****************************************************************************\
    | Dush                                                                        |
    \*****************************************************************************/
    public void Dush(int wo)
    {
//        System.out.println("Dush: sp=" + Opcode.formatByte((short) cpu.sp) + " -> " + Opcode.formatByte((short) (wo >> 8)));
        ram[0x100 + cpu.sp] = (short) (wo >> 8);
        cpu.decrSp();
//        System.out.println("Dush: sp=" + Opcode.formatByte((short) cpu.sp) + " -> " + Opcode.formatByte((short) (wo & 0xff)));
        ram[0x100 + cpu.sp] = (short) (wo & 0xff);
        cpu.decrSp();
    }


    /*****************************************************************************\
    | Pull                                                                        |
    \*****************************************************************************/
    public short Pull()
    {
        cpu.incrSp();
        return ram[0x100 + cpu.sp];
    }


    /*****************************************************************************\
    | Dull                                                                        |
    \*****************************************************************************/
    public int Dull()
    {
        int wo;
//        System.out.println("Dull: sp=" + Opcode.formatByte((short) cpu.sp));
        cpu.incrSp();
        wo = ram[0x100 + cpu.sp];
        cpu.incrSp();
        wo += ram[0x100 + cpu.sp] << 8;
//        System.out.println("Dull: sp=" + Opcode.formatByte((short) cpu.sp) + " -> " + Opcode.formatWord(wo));
        return wo;
    }


    /*****************************************************************************\
    | PLUS4::SetFlags                                                             |
    \*****************************************************************************/
    private void SetFlags(short s)
    {
        cpu.c = ((s & 1) > 0);
        cpu.z = ((s & 2) > 0);
        cpu.i = ((s & 4) > 0);
        cpu.d = ((s & 8) > 0);
        cpu.b = ((s & 16) > 0);
        cpu.v = ((s & 64) > 0);
        cpu.n = ((s & 128) > 0);
    }

    /*****************************************************************************\
    | GetFlags                                                                    |
    \*****************************************************************************/
    private short GetFlags()
    {
        short s = 32;

        if (cpu.c) s |= 1;
        if (cpu.z) s |= 2;
        if (cpu.i) s |= 4;
        if (cpu.d) s |= 8;
        if (cpu.b) s |= 16;
        if (cpu.v) s |= 64;
        if (cpu.n) s |= 128;

        return s;
    }


    /*****************************************************************************\
    | HardReset                                                                   |
    \*****************************************************************************/
    public void hardReset()
    {
        cpu.pc = (rom[0xfffd-0x8000] << 8) + rom[0xfffc-0x8000];
        cpu.sp = 0xff;
        cpu.acc = 0;
        cpu.xr = 0;
        cpu.yr = 0;

        cpu.i = true;
        cpu.n = false;
        cpu.c = false;
        cpu.b = false;
        cpu.z = false;
        cpu.d = false;
        cpu.v = false;

        ram[0xff04] = 4;
        romConfig = 0;
        romActive = true;

        clockCounter = 0;
        flashOn = false;
        flashCounter = 0;
        rasterLine = 0;
    }


    /*****************************************************************************\
    | BefehlAusfuehren                                                            |
    \*****************************************************************************/
    protected void befehlAusfuehren()
    {
        final short cmd = Peek(cpu.pc);

        System.out.println(Opcode.toString(cpu.pc, Peek(cpu.pc), Peek(cpu.pc + 1), Peek(cpu.pc+2)));
        
        switch(cmd) {
            case 0x69:     ADC_69();         break;
            case 0x65:     ADC_65();         break;
            case 0x6d:     ADC_6D();         break;
            case 0x7d:     ADC_7D();         break;
            case 0x79:     ADC_79();         break;
            case 0x75:     ADC_75();         break;
            case 0x61:     ADC_61();         break;
            case 0x71:     ADC_71();         break;
            case 0x29:     AND_29();         break;
            case 0x25:     AND_25();         break;
            case 0x35:     AND_35();         break;
            case 0x2d:     AND_2D();         break;
            case 0x3d:     AND_3D();         break;
            case 0x39:     AND_39();         break;
            case 0x31:     AND_31();         break;
            case 0x21:     AND_21();         break;
            case 0x0a:     ASL_0A();         break;
            case 0x06:     ASL_06();         break;
            case 0x0e:     ASL_0E();         break;
            case 0x1e:     ASL_1E();         break;
            case 0x16:     ASL_16();         break;
            case 0x20:     JSR();            break;
            case 0x60:     RTS();            break;
            case 0x40:     RTI();            break;
            case 0x88:     DEY();            break;
            case 0xc8:     INY();            break;
            case 0xca:     DEX();            break;
            case 0xe8:     INX();            break;
            case 0x18:     CLC();            break;
            case 0x38:     SEC();            break;
            case 0x58:     CLI();            break;
            case 0x78:     SEI();            break;
            case 0xf8:     SED();            break;
            case 0xd8:     CLD();            break;
            case 0xb8:     CLV();            break;
            case 0x8a:     TXA();            break;
            case 0xaa:     TAX();            break;
            case 0x98:     TYA();            break;
            case 0xa8:     TAY();            break;
            case 0xba:     TSX();            break;
            case 0x9a:     TXS();            break;
            case 0x48:     PHA();            break;
            case 0x68:     PLA();            break;
            case 0x08:     PHP();            break;
            case 0x28:     PLP();            break;

            case 0x1a:
            case 0x3a:
            case 0x5a:
            case 0x7a:
            case 0xda:
            case 0xfa:
            case 0xea: 
                NOP(1);
                break;
                
            case 0x80:
            case 0x82:
            case 0x89:
            case 0xc2:
            case 0xe2:
                NOP(2);
                break;
                
            case 0x24:     BIT_24();         break;
            case 0x2c:     BIT_2C();         break;
            case 0xd0:     BNE();            break;
            case 0xf0:     BEQ();            break;
            case 0xb0:     BCS();            break;
            case 0x90:     BCC();            break;
            case 0x30:     BMI();            break;
            case 0x10:     BPL();            break;
            case 0x70:     BVS();            break;
            case 0x50:     BVC();            break;
            case 0xc9:     CMP_C9();         break;
            case 0xc5:     CMP_C5();         break;
            case 0xd5:     CMP_D5();         break;
            case 0xcd:     CMP_CD();         break;
            case 0xdd:     CMP_DD();         break;
            case 0xd9:     CMP_D9();         break;
            case 0xc1:     CMP_C1();         break;
            case 0xd1:     CMP_D1();         break;
            case 0xe0:     CPX_E0();         break;
            case 0xe4:     CPX_E4();         break;
            case 0xec:     CPX_EC();         break;
            case 0xc0:     CPY_C0();         break;
            case 0xc4:     CPY_C4();         break;
            case 0xcc:     CPY_CC();         break;
            case 0xc6:     DEC_C6();         break;
            case 0xd6:     DEC_D6();         break;
            case 0xce:     DEC_CE();         break;
            case 0xde:     DEC_DE();         break;
            case 0x49:     EOR_49();         break;
            case 0x45:     EOR_45();         break;
            case 0x55:     EOR_55();         break;
            case 0x4d:     EOR_4D();         break;
            case 0x5d:     EOR_5D();         break;
            case 0x59:     EOR_59();         break;
            case 0x41:     EOR_41();         break;
            case 0x51:     EOR_51();         break;
            case 0xe6:     INC_E6();         break;
            case 0xf6:     INC_F6();         break;
            case 0xee:     INC_EE();         break;
            case 0xfe:     INC_FE();         break;
            case 0x4c:     JMP_4C();         break;
            case 0x6c:     JMP_6C();         break;
            case 0xa9:     LDA_A9();         break;
            case 0xa5:     LDA_A5();         break;
            case 0xb5:     LDA_B5();         break;
            case 0xad:     LDA_AD();         break;
            case 0xbd:     LDA_BD();         break;
            case 0xb9:     LDA_B9();         break;
            case 0xa1:     LDA_A1();         break;
            case 0xb1:     LDA_B1();         break;
            case 0xa2:     LDX_A2();         break;
            case 0xa6:     LDX_A6();         break;
            case 0xb6:     LDX_B6();         break;
            case 0xae:     LDX_AE();         break;
            case 0xbe:     LDX_BE();         break;
            case 0xa0:     LDY_A0();         break;
            case 0xa4:     LDY_A4();         break;
            case 0xb4:     LDY_B4();         break;
            case 0xac:     LDY_AC();         break;
            case 0xbc:     LDY_BC();         break;
            case 0x4a:     LSR_4A();         break;
            case 0x46:     LSR_46();         break;
            case 0x4e:     LSR_4E();         break;
            case 0x5e:     LSR_5E();         break;
            case 0x56:     LSR_56();         break;
            case 0x09:     ORA_09();         break;
            case 0x05:     ORA_05();         break;
            case 0x15:     ORA_15();         break;
            case 0x0d:     ORA_0D();         break;
            case 0x1d:     ORA_1D();         break;
            case 0x19:     ORA_19();         break;
            case 0x11:     ORA_11();         break;
            case 0x01:     ORA_01();         break;
            case 0x2a:     ROL_2A();         break;
            case 0x2e:     ROL_2E();         break;
            case 0x3e:     ROL_3E();         break;
            case 0x26:     ROL_26();         break;
            case 0x36:     ROL_36();         break;
            case 0x6a:     ROR_6A();         break;
            case 0x6e:     ROR_6E();         break;
            case 0x7e:     ROR_7E();         break;
            case 0x66:     ROR_66();         break;
            case 0x76:     ROR_76();         break;
            case 0xe9:     SBC_E9();         break;
            case 0xe5:     SBC_E5();         break;
            case 0xf5:     SBC_F5();         break;
            case 0xed:     SBC_ED();         break;
            case 0xfd:     SBC_FD();         break;
            case 0xf9:     SBC_F9();         break;
            case 0xe1:     SBC_E1();         break;
            case 0xf1:     SBC_F1();         break;
            case 0x85:     STA_85();         break;
            case 0x95:     STA_95();         break;
            case 0x8d:     STA_8D();         break;
            case 0x9d:     STA_9D();         break;
            case 0x99:     STA_99();         break;
            case 0x81:     STA_81();         break;
            case 0x91:     STA_91();         break;
            case 0x86:     STX_86();         break;
            case 0x96:     STX_96();         break;
            case 0x8e:     STX_8E();         break;
            case 0x84:     STY_84();         break;
            case 0x94:     STY_94();         break;
            case 0x8c:     STY_8C();         break;
            default:       BRK();            break;
//            default:
//                throw new UnsupportedOperationException("Opcode: $" + Opcode.formatByte(cmd));
        }
        
        assert(cpu.acc >= 0);
        assert(cpu.acc <= 255);
        assert(cpu.xr >= 0);
        assert(cpu.xr <= 255);
        assert(cpu.yr >= 0);
        assert(cpu.yr <= 255);
        assert(cpu.sp >= 0);
        assert(cpu.sp <= 255);
        assert(cpu.pc >= 0);
        assert(cpu.pc <= 0xffff);
    }

    public static short toByte(int b)
    {
        if (b > 0xff) {
            return (short) (b & 0xff);
        } else {
            while (b < 0) {
                b += 0x100;
            }
        
            return (short) b;
        }
    }

    public static int toWord(int w)
    {
        if (w > 0xffff) {
            return (w & 0xffff);
        } else {
            while (w < 0) {
                w += 0x10000;
            }
        
            return w;
        }
    }

    private short rom(int addr)
    {
        return rom[addr & 0x7fff];
    }


    /*****************************************************************************\
    | AND                    |                                                    |
    \*****************************************************************************/
    void AND_29()
    {
        //  AND #$00
        cpu.DoAND(GetByte(AddressMode.UNMITTELBAR));
        cpu.incrPc(2);
        clockTicks = 2;
    }

    void AND_25()
    {
        //  AND $00
        cpu.DoAND(GetByte(AddressMode.ZEROPAGE));
        cpu.incrPc(2);
        clockTicks = 3;
    }

    void AND_35()
    {
        //  AND $00,X
        cpu.DoAND(GetByte(AddressMode.ZEROPAGEX));
        cpu.incrPc(2);
        clockTicks = 4;
    }

    void AND_2D()
    {
        //  AND $0000
        cpu.DoAND(GetByte(AddressMode.ABSOLUT));
        cpu.incrPc(3);
        clockTicks = 4;
    }

    void AND_3D()
    {
        //  AND $0000,X
        cpu.DoAND(GetByte(AddressMode.INDIZIERTX));
        cpu.incrPc(3);
        clockTicks = 4;
    }

    void AND_39()
    {
        //  AND $0000,Y
        cpu.DoAND(GetByte(AddressMode.INDIZIERTY));
        cpu.incrPc(3);
        clockTicks = 4;
    }

    void AND_31()
    {
        //  AND ($00),Y
        cpu.DoAND(GetByte(AddressMode.INDIREKTINDIZIERTY));
        cpu.incrPc(2);
        clockTicks = 5;
    }

    void AND_21()
    {
        //  AND ($00,X)
        cpu.DoAND(GetByte(AddressMode.INDIZIERTINDIREKTX));
        cpu.incrPc(2);
        clockTicks = 6;
    }

    /*****************************************************************************\
    | ASL                    |                                                    |
    \*****************************************************************************/
    void ASL_0A()
    {
       //  ASL
       cpu.acc = cpu.DoASL(cpu.acc);
       cpu.incrPc(1);
       clockTicks = 2;
    }

    void ASL_06()
    {
       //  ASL $00
       poke(getAddress(AddressMode.ZEROPAGE), cpu.DoASL(GetByte(AddressMode.ZEROPAGE)) );
       cpu.incrPc(2);
       clockTicks = 5;
    }

    void ASL_0E()
    {
       //  ASL $0000
       poke(getAddress(AddressMode.ABSOLUT), cpu.DoASL(GetByte(AddressMode.ABSOLUT)) );
       cpu.incrPc(3);
       clockTicks = 6;
    }

    void ASL_1E()
    {
       //  ASL $0000,X
       poke(getAddress(AddressMode.INDIZIERTX), cpu.DoASL(GetByte(AddressMode.INDIZIERTX)) );
       cpu.incrPc(3);
       clockTicks = 7;
    }

    void ASL_16()
    {
       //  ASL $00,X
       poke(getAddress(AddressMode.ZEROPAGEX), cpu.DoASL(GetByte(AddressMode.ZEROPAGEX)) );
       cpu.incrPc(2);
       clockTicks = 7;
    }

    /*****************************************************************************\
    | ADC                                                                         |
    \*****************************************************************************/
    void ADC_69()
    {
        //  ADC #$00
        cpu.DoADC(GetByte(AddressMode.UNMITTELBAR));
        cpu.incrPc(1);
        clockTicks = 2;
    }

    void ADC_65()
    {
        //  ADC $00
        cpu.DoADC(GetByte(AddressMode.ZEROPAGE));
        cpu.incrPc(2);
        clockTicks = 3;
    }

    void ADC_6D()
    {
        //  ADC $0000
        cpu.DoADC(GetByte(AddressMode.ABSOLUT));
        cpu.incrPc(3);
        clockTicks = 4;
    }

    void ADC_7D()
    {
        //  ADC $0000,X
        cpu.DoADC(GetByte(AddressMode.INDIZIERTX));
        cpu.incrPc(3);
        clockTicks += 4;
    }

    void ADC_79()
    {
        //  ADC $0000,Y
        cpu.DoADC(GetByte(AddressMode.INDIZIERTY));
        cpu.incrPc(3);
        clockTicks += 4;
    }

    void ADC_75()
    {
        //  ADC $00,X
        cpu.DoADC(GetByte(AddressMode.ZEROPAGEX));
        cpu.incrPc(2);
        clockTicks = 4;
    }

    void ADC_61()
    {
        //  ADC ($00,X)
        cpu.DoADC(GetByte(AddressMode.INDIZIERTINDIREKTX));
        cpu.incrPc(2);
        clockTicks = 6;
    }

    void ADC_71()
    {
        //  ADC ($00),Y
        cpu.DoADC(GetByte(AddressMode.INDIREKTINDIZIERTY));
        cpu.incrPc(2);
        clockTicks += 5;
    }


    /*****************************************************************************\
    | JSR                                                                         |
    \*****************************************************************************/
    void JSR()
    {
        //  JSR $0000
        Dush(toWord(cpu.pc + 3));
        cpu.pc = (Peek(cpu.pc + 1) + (Peek(cpu.pc + 2) << 8));
        clockTicks = 6;
    }

    
    /*****************************************************************************\
    | RTS                                                                         |
    \*****************************************************************************/
    void RTS()
    {
        cpu.pc = Dull();
        clockTicks = 6;
//        System.out.println("------ RTS -> " + Opcode.formatWord(cpu.pc));
    }

    
    /*****************************************************************************\
    | RTI                                                                         |
    \*****************************************************************************/
    void RTI()
    {
        SetFlags(Pull());
        cpu.pc = Dull();
        clockTicks = 6;
        cpu.i = false;
    }

    
    /*****************************************************************************\
    | DEY                                                                         |
    \*****************************************************************************/
    void DEY()
    {
        cpu.dey();
        cpu.incrPc(1);
        clockTicks = 2;
    }

    /*****************************************************************************\
    | INY                                                                         |
    \*****************************************************************************/
    void INY()
    {
        cpu.iny();
        cpu.incrPc(1);
        clockTicks = 2;
    }

    /*****************************************************************************\
    | DEX                                                                         |
    \*****************************************************************************/
    void DEX()
    {
        cpu.dex();
        cpu.incrPc(1);
        clockTicks = 2;
    }

    /*****************************************************************************\
    | INX                                                                         |
    \*****************************************************************************/
    void INX()
    {
        cpu.inx();
        cpu.incrPc(1);
        clockTicks = 2;
    }

    /*****************************************************************************\
    | CLC                                                                         |
    \*****************************************************************************/
    void CLC()
    {
        cpu.c = false;
        cpu.incrPc(1);
        clockTicks = 2;
    }

    /*****************************************************************************\
    | SEC                                                                         |
    \*****************************************************************************/
    void SEC()
    {
        cpu.c = true;
        cpu.incrPc(1);
        clockTicks = 2;
    }

    /*****************************************************************************\
    | CLI                                                                         |
    \*****************************************************************************/
    void CLI()
    {
        cpu.i = false;
        cpu.incrPc(1);
        clockTicks = 2;
    }

    /*****************************************************************************\
    | SEI                                                                         |
    \*****************************************************************************/
    void SEI()
    {
        cpu.i = true;
        cpu.incrPc(1);
        clockTicks = 2;
    }

    /*****************************************************************************\
    | SED                                                                         |
    \*****************************************************************************/
    void SED()
    {
        cpu.d = true;
        cpu.incrPc(1);
        clockTicks = 2;
    }

    /*****************************************************************************\
    | CLD                                                                         |
    \*****************************************************************************/
    void CLD()
    {
        cpu.d = false;
        cpu.incrPc(1);
        clockTicks = 2;
    }

    /*****************************************************************************\
    | CLV                                                                         |
    \*****************************************************************************/
    void CLV()
    {
        cpu.v = false;
        cpu.incrPc(1);
        clockTicks = 2;
    }

    /*****************************************************************************\
    | TXA                                                                         |
    \*****************************************************************************/
    void TXA()
    {
        cpu.txa();
        cpu.incrPc(1);
        clockTicks = 2;
    }

    /*****************************************************************************\
    | TAX                                                                         |
    \*****************************************************************************/
    void TAX()
    {
        cpu.tax();
        cpu.incrPc(1);
        clockTicks = 2;
    }

    /*****************************************************************************\
    | TYA                                                                         |
    \*****************************************************************************/
    void TYA()
    {
        cpu.tya();
        cpu.incrPc(1);
        clockTicks = 2;
    }

    /*****************************************************************************\
    | TAY                                                                         |
    \*****************************************************************************/
    void TAY()
    {
        cpu.tay();
        cpu.incrPc(1);
        clockTicks = 2;
    }

    /*****************************************************************************\
    | TSX                                                                         |
    \*****************************************************************************/
    void TSX()
    {
        cpu.tsx();
        cpu.incrPc(1);
        clockTicks = 2;
    }

    /*****************************************************************************\
    | TXS                                                                         |
    \*****************************************************************************/
    void TXS()
    {
        cpu.txs();
        cpu.incrPc(1);
        clockTicks = 2;
    }

    /*****************************************************************************\
    | PHA                                                                         |
    \*****************************************************************************/
    void PHA()
    {
        Push(cpu.acc);
        cpu.incrPc(1);
        clockTicks = 3;
    }

    /*****************************************************************************\
    | PLA                                                                         |
    \*****************************************************************************/
    void PLA()
    {
        cpu.acc = Pull();
        cpu.NegFlag(cpu.acc);
        cpu.ZeroFlag(cpu.acc);
        cpu.incrPc(1);
        clockTicks = 4;
    }

    /*****************************************************************************\
    | PHP                                                                         |
    \*****************************************************************************/
    void PHP()
    {
        Push(GetFlags());
        cpu.incrPc(1);
        clockTicks = 3;
    }

    /*****************************************************************************\
    | PLP                                                                         |
    \*****************************************************************************/
    void PLP()
    {
        SetFlags(Pull());
        cpu.incrPc(1);
        clockTicks = 4;
    }

    /*****************************************************************************\
    | NOP                                                                         |
    \*****************************************************************************/
    void NOP(int pcInc)
    {
        cpu.incrPc(pcInc);
        clockTicks = 2;
    }


    /*****************************************************************************\
    | BIT                                                                         |
    \*****************************************************************************/
    void BIT_24()
    {
        //  BIT $00
        cpu.DoBIT(GetByte(AddressMode.ZEROPAGE));
        cpu.incrPc(2);
        clockTicks = 3;
    }

    void BIT_2C()
    {
        //  BIT $0000
        cpu.DoBIT(GetByte(AddressMode.ABSOLUT));
        cpu.incrPc(3);
        clockTicks = 4;
    }


    /*****************************************************************************\
    | JumpAdress                                                                  |
    \*****************************************************************************/
    int JumpAddress()
    {
        if ((Peek(cpu.pc + 1) & 0x80) > 0) {
            return toWord(cpu.pc - 126 + (Peek(cpu.pc + 1) & 127));
        } else {
            return toWord(cpu.pc + Peek(cpu.pc + 1) + 2);
        }
    }

    /*****************************************************************************\
    | BNE                                                                         |
    \*****************************************************************************/
    void BNE()
    {
        //  BNE $0000
        final int ziel;

        clockTicks = 2;
        if (!cpu.z) {
            ziel = JumpAddress();
            if ((ziel & 0xff00) != (cpu.pc & 0xff00))
                clockTicks +=2;
            else
                clockTicks ++;
            cpu.pc = ziel;
        } else {
            cpu.incrPc(2);
        }
    }

    /*****************************************************************************\
    | BEQ                    |                                                    |
    \*****************************************************************************/
    void BEQ()
    {
        //  BEQ $0000
        final int ziel;

        clockTicks = 2;
        if (cpu.z) {
            ziel = JumpAddress();
            if ((ziel & 0xff00) != (cpu.pc & 0xff00))
                clockTicks +=2;
            else
                clockTicks ++;
            cpu.pc = ziel;
        } else {
            cpu.incrPc(2);
        }
    }

    /*****************************************************************************\
    | BCS                    |                                                    |
    \*****************************************************************************/
    void BCS()
    {
        //  BCS $0000
        final int ziel;

        clockTicks = 2;
        if (cpu.c) {
            ziel = JumpAddress();
            if ((ziel & 0xff00) != (cpu.pc & 0xff00)) clockTicks +=2; else clockTicks ++;
            cpu.pc = ziel;
        } else {
            cpu.incrPc(2);
        }
    }

    /*****************************************************************************\
    | BCC                    |                                                    |
    \*****************************************************************************/
    void BCC()
    {
        //  BCC $0000
        final int ziel;

        clockTicks = 2;
        if (!cpu.c) {
            ziel = JumpAddress();
            if ((ziel & 0xff00) != (cpu.pc & 0xff00)) clockTicks +=2; else clockTicks ++;
            cpu.pc = ziel;
        } else {
            cpu.incrPc(2);
        }
    }

    /*****************************************************************************\
    | BMI                    |                                                    |
    \*****************************************************************************/
    void BMI()
    {
        //  BMI $0000
        final int ziel;

        clockTicks = 2;
        if (cpu.n) {
            ziel = JumpAddress();
            if ((ziel & 0xff00) != (cpu.pc & 0xff00)) clockTicks +=2; else clockTicks ++;
            cpu.pc = ziel;
        } else {
            cpu.incrPc(2);
        }
    }

    /*****************************************************************************\
    | BPL                    |                                                    |
    \*****************************************************************************/
    void BPL()
    {
        //  BPL $0000

        clockTicks = 2;
        if (!cpu.n) {
            final int ziel = JumpAddress();
            if ((ziel & 0xff00) != (cpu.pc & 0xff00)) clockTicks +=2; else clockTicks ++;
            cpu.pc = ziel;
        } else {
            cpu.incrPc(2);
        }
    }

    /*****************************************************************************\
    | BVS                    |                                                    |
    \*****************************************************************************/
    void BVS()
    {
        //  BVS $0000
        final int ziel;

        clockTicks = 2;
        if (cpu.v) {
            ziel = JumpAddress();
            if ((ziel & 0xff00) != (cpu.pc & 0xff00)) clockTicks +=2; else clockTicks ++;
            cpu.pc = ziel;
        } else {
            cpu.incrPc(2);
        }
    }

    /*****************************************************************************\
    | BVC                    |                                                    |
    \*****************************************************************************/
    void BVC()
    {
        //  BVC $0000
        final int ziel;

        clockTicks = 2;
        if (!cpu.v) {
            ziel = JumpAddress();
            if ((ziel & 0xff00) != (cpu.pc & 0xff00)) clockTicks +=2; else clockTicks ++;
            cpu.pc = ziel;
        } else {
            cpu.incrPc(2);
        }
    }


    /*****************************************************************************\
    | BRK                    |                                                    |
    \*****************************************************************************/
    void BRK()
    {
        cpu.i = true;
        cpu.b = true;
        Dush(toWord(cpu.pc +2));
        Push(GetFlags());
        cpu.pc = (rom(0xffff) << 8) + rom(0xfffe);
        clockTicks = 7;
    }


    /*****************************************************************************\
    | CMP                    |                                                    |
    \*****************************************************************************/
    void CMP_C9()
    {
        //  CMP #$00
        cpu.DoCMP(cpu.acc, GetByte(AddressMode.UNMITTELBAR));
        cpu.incrPc(2);
        clockTicks = 2;
    }

    void CMP_C5()
    {
        //  CMP $00
        cpu.DoCMP(cpu.acc, GetByte(AddressMode.ZEROPAGE));
        cpu.incrPc(2);
        clockTicks = 3;
    }

    void CMP_D5()
    {
        //  CMP $00,X
        cpu.DoCMP(cpu.acc, GetByte(AddressMode.ZEROPAGEX));
        cpu.incrPc(2);
        clockTicks = 4;
    }

    void CMP_CD()
    {
        //  CMP $0000
        cpu.DoCMP(cpu.acc, GetByte(AddressMode.ABSOLUT));
        cpu.incrPc(3);
        clockTicks = 4;
    }

    void CMP_DD()
    {
        //  CMP $0000,X
        cpu.DoCMP(cpu.acc, GetByte(AddressMode.INDIZIERTX));
        cpu.incrPc(3);
        clockTicks += 4;
    }

    void CMP_D9()
    {
        //  CMP $0000,Y
        cpu.DoCMP(cpu.acc, GetByte(AddressMode.INDIZIERTY));
        cpu.incrPc(3);
        clockTicks += 4;
    }

    void CMP_C1()
    {
        //  CMP ($00,X)
        cpu.DoCMP(cpu.acc, GetByte(AddressMode.INDIZIERTINDIREKTX));
        cpu.incrPc(2);
        clockTicks = 6;
    }

    void CMP_D1()
    {
        //  CMP ($00),Y
        cpu.DoCMP(cpu.acc, GetByte(AddressMode.INDIREKTINDIZIERTY));
        cpu.incrPc(2);
        clockTicks += 5;
    }

    /*****************************************************************************\
    | CPX                    |                                                    |
    \*****************************************************************************/
    void CPX_E0()
    {
        //  CPX #$00
        cpu.DoCMP(cpu.xr, GetByte(AddressMode.UNMITTELBAR));
        cpu.incrPc(2);
        clockTicks = 2;
    }

    void CPX_E4()
    {
        //  CPX $00
        cpu.DoCMP(cpu.xr, GetByte(AddressMode.ZEROPAGE));
        cpu.incrPc(2);
        clockTicks = 3;
    }

    void CPX_EC()
    {
        //  CPX $0000
        cpu.DoCMP(cpu.xr, GetByte(AddressMode.ABSOLUT));
        cpu.incrPc(3);
        clockTicks = 4;
    }

    /*****************************************************************************\
    | CPY                    |                                                    |
    \*****************************************************************************/
    void CPY_C0()
    {
        //  CPY #$00
        cpu.DoCMP(cpu.yr, GetByte(AddressMode.UNMITTELBAR));
        cpu.incrPc(2);
        clockTicks = 2;
    }

    void CPY_C4()
    {
        //  CPY $00
        cpu.DoCMP(cpu.yr, GetByte(AddressMode.ZEROPAGE));
        cpu.incrPc(2);
        clockTicks = 3;
    }

    void CPY_CC()
    {
        //  CPY $0000
        cpu.DoCMP(cpu.yr, GetByte(AddressMode.ABSOLUT));
        cpu.incrPc(3);
        clockTicks = 4;
    }

    /*****************************************************************************\
    | DEC                    |                                                    |
    \*****************************************************************************/
    void DEC_C6()
    {
        //  DEC $00
        final short result = toByte(GetByte(AddressMode.ZEROPAGE) - 1);
        cpu.NegFlag(result);
        cpu.ZeroFlag(result);
        poke(getAddress(AddressMode.ZEROPAGE), result);
        cpu.incrPc(2);
        clockTicks = 5;
    }

    void DEC_D6()
    {
        //  DEC $00,X
        final short result = toByte(GetByte(AddressMode.ZEROPAGEX) - 1);
        cpu.NegFlag(result);
        cpu.ZeroFlag(result);
        poke(getAddress(AddressMode.ZEROPAGEX), result);
        cpu.incrPc(2);
        clockTicks = 6;
    }

    void DEC_CE()
    {
        //  DEC $0000
        final short result = toByte(GetByte(AddressMode.ABSOLUT) - 1);
        cpu.NegFlag(result);
        cpu.ZeroFlag(result);
        poke(getAddress(AddressMode.ABSOLUT), result);
        cpu.incrPc(3);
        clockTicks = 3;
    }

    void DEC_DE()
    {
        //  DEC $0000,X
        final short result = toByte(GetByte(AddressMode.INDIZIERTX) - 1);
        cpu.NegFlag(result);
        cpu.ZeroFlag(result);
        poke(getAddress(AddressMode.INDIZIERTX), result);
        cpu.incrPc(3);
        clockTicks = 7;
    }

    /*****************************************************************************\
    | EOR                    |                                                    |
    \*****************************************************************************/
    void EOR_49()
    {
        //  EOR #$00
        cpu.DoEOR(GetByte(AddressMode.UNMITTELBAR));
        cpu.incrPc(2);
        clockTicks = 2;
    }

    void EOR_45()
    {
        //  EOR $00
        cpu.DoEOR(GetByte(AddressMode.ZEROPAGE));
        cpu.incrPc(2);
        clockTicks = 3;
    }

    void EOR_55()
    {
        //  EOR $00,X
        cpu.DoEOR(GetByte(AddressMode.ZEROPAGEX));
        cpu.incrPc(2);
        clockTicks = 4;
    }

    void EOR_4D()
    {
        //  EOR $0000
        cpu.DoEOR(GetByte(AddressMode.ABSOLUT));
        cpu.incrPc(3);
        clockTicks = 4;
    }

    void EOR_5D()
    {
        //  EOR $0000,X
        cpu.DoEOR(GetByte(AddressMode.INDIZIERTX));
        cpu.incrPc(3);
        clockTicks += 4;
    }

    void EOR_59()
    {
        //  EOR $0000,Y
        cpu.DoEOR(GetByte(AddressMode.INDIZIERTY));
        cpu.incrPc(3);
        clockTicks += 4;
    }

    void EOR_41()
    {
        //  EOR ($00,X)
        cpu.DoEOR(GetByte(AddressMode.INDIZIERTINDIREKTX));
        cpu.incrPc(2);
        clockTicks = 6;
    }

    void EOR_51()
    {
        //  EOR ($00),Y
        cpu.DoEOR(GetByte(AddressMode.INDIREKTINDIZIERTY));
        cpu.incrPc(2);
        clockTicks += 5;
    }

    /*****************************************************************************\
    | INC                    |                                                    |
    \*****************************************************************************/
    void INC_E6()
    {
        //  INC $00
        final short result = toByte(GetByte(AddressMode.ZEROPAGE) + 1);
        cpu.NegFlag(result);
        cpu.ZeroFlag(result);
        poke(getAddress(AddressMode.ZEROPAGE), result);
        cpu.incrPc(2);
        clockTicks = 5;
    }

    void INC_F6()
    {
        //  INC $00,X
        final short result = toByte(GetByte(AddressMode.ZEROPAGEX) + 1);
        cpu.NegFlag(result);
        cpu.ZeroFlag(result);
        poke(getAddress(AddressMode.ZEROPAGEX), result);
        cpu.incrPc(2);
        clockTicks = 6;
    }

    void INC_EE()
    {
        //  INC $0000
        final short result = toByte(GetByte(AddressMode.ABSOLUT) + 1);
        cpu.NegFlag(result);
        cpu.ZeroFlag(result);
        poke(getAddress(AddressMode.ABSOLUT), result);
        cpu.incrPc(3);
        clockTicks = 6;
    }

    void INC_FE()
    {
        //  INC $0000,X
        final short result = toByte(GetByte(AddressMode.INDIZIERTX) + 1);
        cpu.NegFlag(result);
        cpu.ZeroFlag(result);
        poke(getAddress(AddressMode.INDIZIERTX), result);
        cpu.incrPc(3);
        clockTicks = 7;
    }
    
    /*****************************************************************************\
    | JMP                    |                                                    |
    \*****************************************************************************/
    void JMP_4C()
    {
        //  JMP $0000
        cpu.pc = toWord(Peek(cpu.pc + 1) + (Peek(cpu.pc + 2) << 8));
        clockTicks = 3;
    }

    void JMP_6C()
    {
        //  JMP ($0000)
        int ad;

        ad = Peek(cpu.pc + 1) + (Peek(cpu.pc + 2) << 8);
        cpu.pc = Peek(ad) + (Peek(ad+1) << 8);
        clockTicks = 5;
    }

    /*****************************************************************************\
    | LDA                    |                                                    |
    \*****************************************************************************/
    void LDA_A9()
    {
        //  LDA #$00
        cpu.acc = GetByte(AddressMode.UNMITTELBAR);
        cpu.NegFlag(cpu.acc);
        cpu.ZeroFlag(cpu.acc);
        cpu.incrPc(2);
        clockTicks = 2;
    }

    void LDA_A5()
    {
        //  LDA $00
        cpu.acc = GetByte(AddressMode.ZEROPAGE);
        cpu.NegFlag(cpu.acc);
        cpu.ZeroFlag(cpu.acc);
        cpu.incrPc(2);
        clockTicks = 3;
    }

    void LDA_B5()
    {
        //  LDA $00,X
        cpu.acc = GetByte(AddressMode.ZEROPAGEX);
        cpu.NegFlag(cpu.acc);
        cpu.ZeroFlag(cpu.acc);
        cpu.incrPc(2);
        clockTicks = 4;
    }

    void LDA_AD()
    {
        //  LDA $0000
        cpu.acc = GetByte(AddressMode.ABSOLUT);
        cpu.NegFlag(cpu.acc);
        cpu.ZeroFlag(cpu.acc);
        cpu.incrPc(3);
        clockTicks = 4;
    }

    void LDA_BD()
    {
        //  LDA $0000,X
        cpu.acc = GetByte(AddressMode.INDIZIERTX);
        cpu.NegFlag(cpu.acc);
        cpu.ZeroFlag(cpu.acc);
        cpu.incrPc(3);
        clockTicks += 4;
    }

    void LDA_B9()
    {
        //  LDA $0000,Y
        cpu.acc = GetByte(AddressMode.INDIZIERTY);
        cpu.NegFlag(cpu.acc);
        cpu.ZeroFlag(cpu.acc);
        cpu.incrPc(3);
        clockTicks += 4;
    }

    void LDA_A1()
    {
        //  LDA ($00,X)
        cpu.acc = GetByte(AddressMode.INDIZIERTINDIREKTX);
        cpu.NegFlag(cpu.acc);
        cpu.ZeroFlag(cpu.acc);
        cpu.incrPc(2);
        clockTicks = 6;
    }

    void LDA_B1()
    {
        //  LDA ($00),Y
        cpu.acc = GetByte(AddressMode.INDIREKTINDIZIERTY);
        cpu.NegFlag(cpu.acc);
        cpu.ZeroFlag(cpu.acc);
        cpu.incrPc(2);
        clockTicks += 5;
    }

    /*****************************************************************************\
    | LDX                    |                                                    |
    \*****************************************************************************/
    void LDX_A2()
    {
        //  LDX #$00
        cpu.xr = GetByte(AddressMode.UNMITTELBAR);
        cpu.NegFlag(cpu.xr);
        cpu.ZeroFlag(cpu.xr);
        cpu.incrPc(2);
        clockTicks = 2;
    }

    void LDX_A6()
    {
        //  LDX $00
        cpu.xr = GetByte(AddressMode.ZEROPAGE);
        cpu.NegFlag(cpu.xr);
        cpu.ZeroFlag(cpu.xr);
        cpu.incrPc(2);
        clockTicks = 3;
    }

    void LDX_B6()
    {
        //  LDX $00,Y
        cpu.xr = GetByte(AddressMode.ZEROPAGEY);
        cpu.NegFlag(cpu.xr);
        cpu.ZeroFlag(cpu.xr);
        cpu.incrPc(2);
        clockTicks = 4;
    }

    void LDX_AE()
    {
        //  LDcpu.xr $0000
        cpu.xr = GetByte(AddressMode.ABSOLUT);
        cpu.NegFlag(cpu.xr);
        cpu.ZeroFlag(cpu.xr);
        cpu.incrPc(3);
        clockTicks = 4;
    }

    void LDX_BE()
    {
        //  LDX $0000,Y
        cpu.xr = GetByte(AddressMode.INDIZIERTY);
        cpu.NegFlag(cpu.xr);
        cpu.ZeroFlag(cpu.xr);
        cpu.incrPc(3);
        clockTicks += 4;
    }

    /*****************************************************************************\
    | LDY                    |                                                    |
    \*****************************************************************************/
    void LDY_A0()
    {
        //  LDY #$00
        cpu.yr = GetByte(AddressMode.UNMITTELBAR);
        cpu.NegFlag(cpu.yr);
        cpu.ZeroFlag(cpu.yr);
        cpu.incrPc(2);
        clockTicks = 2;
    }

    void LDY_A4()
    {
        //  LDY $00
        cpu.yr = GetByte(AddressMode.ZEROPAGE);
        cpu.NegFlag(cpu.yr);
        cpu.ZeroFlag(cpu.yr);
        cpu.incrPc(2);
        clockTicks = 3;
    }

    void LDY_B4()
    {
        //  LDY $00,X
        cpu.yr = GetByte(AddressMode.ZEROPAGEX);
        cpu.NegFlag(cpu.yr);
        cpu.ZeroFlag(cpu.yr);
        cpu.incrPc(2);
        clockTicks = 4;
    }

    void LDY_AC()
    {
        //  LDY $0000
        cpu.yr = GetByte(AddressMode.ABSOLUT);
        cpu.NegFlag(cpu.yr);
        cpu.ZeroFlag(cpu.yr);
        cpu.incrPc(3);
        clockTicks = 4;
    }

    void LDY_BC()
    {
        //  LDY $0000,X
        cpu.yr = GetByte(AddressMode.INDIZIERTX);
        cpu.NegFlag(cpu.yr);
        cpu.ZeroFlag(cpu.yr);
        cpu.incrPc(3);
        clockTicks += 4;
    }


    /*****************************************************************************\
    | LSR                    |                                                    |
    \*****************************************************************************/
    void LSR_4A()
    {
        //  LSR
        cpu.acc = cpu.DoLSR(cpu.acc);
        cpu.incrPc(1);
        clockTicks = 2;
    }

    void LSR_46()
    {
        //  LSR $00
        poke(getAddress(AddressMode.ZEROPAGE), cpu.DoLSR(GetByte(AddressMode.ZEROPAGE)) );
        cpu.incrPc(2);
        clockTicks = 5;
    }

    void LSR_4E()
    {
        //  LSR $0000
        poke(getAddress(AddressMode.ABSOLUT), cpu.DoLSR(GetByte(AddressMode.ABSOLUT)) );
        cpu.incrPc(3);
        clockTicks = 6;
    }

    void LSR_5E()
    {
        //  LSR $0000,X
        poke(getAddress(AddressMode.INDIZIERTX), cpu.DoLSR(GetByte(AddressMode.INDIZIERTX)) );
        cpu.incrPc(3);
        clockTicks = 7;
    }

    void LSR_56()
    {
        //  LSR $00,X
        poke(getAddress(AddressMode.ZEROPAGEX), cpu.DoLSR(GetByte(AddressMode.ZEROPAGEX)) );
        cpu.incrPc(2);
        clockTicks = 6;
    }

    /*****************************************************************************\
    | ORA                    |                                                    |
    \*****************************************************************************/
    void ORA_09()
    {
        //  ORA #$00
        cpu.DoORA(GetByte(AddressMode.UNMITTELBAR));
        cpu.incrPc(2);
        clockTicks = 2;
    }

    void ORA_05()
    {
        //  ORA $00
        cpu.DoORA(GetByte(AddressMode.ZEROPAGE));
        cpu.incrPc(2);
        clockTicks = 3;
    }

    void ORA_15()
    {
        //  ORA $00,X
        cpu.DoORA(GetByte(AddressMode.ZEROPAGEX));
        cpu.incrPc(2);
        clockTicks = 4;
    }

    void ORA_0D()
    {
        //  ORA $0000
        cpu.DoORA(GetByte(AddressMode.ABSOLUT));
        cpu.incrPc(3);
        clockTicks = 4;
    }

    void ORA_1D()
    {
        //  ORA $0000,X
        cpu.DoORA(GetByte(AddressMode.INDIZIERTX));
        cpu.incrPc(3);
        clockTicks += 4;
    }

    void ORA_19()
    {
        //  ORA $0000,Y
        cpu.DoORA(GetByte(AddressMode.INDIZIERTY));
        cpu.incrPc(3);
        clockTicks += 4;
    }

    void ORA_11()
    {
        //  ORA ($00),Y
        cpu.DoORA(GetByte(AddressMode.INDIREKTINDIZIERTY));
        cpu.incrPc(2);
        clockTicks += 5;
    }

    void ORA_01()
    {
        //  ORA ($00,X)
        cpu.DoORA(GetByte(AddressMode.INDIZIERTINDIREKTX));
        cpu.incrPc(2);
        clockTicks += 5;
    }


    /*****************************************************************************\
    | ROL                    |                                                    |
    \*****************************************************************************/
    void ROL_2A()
    {
        //  ROL
        cpu.acc = cpu.DoROL(cpu.acc);
        cpu.incrPc(1);
        clockTicks = 2;
    }

    void ROL_2E()
    {
        //  ROL $0000
        poke(getAddress(AddressMode.ABSOLUT), cpu.DoROL(GetByte(AddressMode.ABSOLUT)) );
        cpu.incrPc(3);
        clockTicks = 6;
    }

    void ROL_3E()
    {
        //  ROL $0000,X
        poke(getAddress(AddressMode.INDIZIERTX), cpu.DoROL(GetByte(AddressMode.INDIZIERTX)) );
        cpu.incrPc(3);
        clockTicks = 7;
    }

    void ROL_26()
    {
        //  ROL $00
        poke(getAddress(AddressMode.ZEROPAGE), cpu.DoROL(GetByte(AddressMode.ZEROPAGE)) );
        cpu.incrPc(2);
        clockTicks = 5;
    }

    void ROL_36()
    {
        //  ROL $00,X
        poke(getAddress(AddressMode.ZEROPAGEX), cpu.DoROL(GetByte(AddressMode.ZEROPAGEX)) );
        cpu.incrPc(2);
        clockTicks = 6;
    }


    /*****************************************************************************\
    | ROR                    |                                                    |
    \*****************************************************************************/
    void ROR_6A()
    {
        //  ROR
        cpu.acc = cpu.DoROR(cpu.acc);
        cpu.incrPc(1);
        clockTicks = 2;
    }

    void ROR_6E()
    {
        //  ROR $0000
        poke(getAddress(AddressMode.ABSOLUT), cpu.DoROR(GetByte(AddressMode.ABSOLUT)) );
        cpu.incrPc(3);
        clockTicks = 6;
    }

    void ROR_7E()
    {
        //  ROR $0000,X
        poke(getAddress(AddressMode.INDIZIERTX), cpu.DoROR(GetByte(AddressMode.INDIZIERTX)) );
        cpu.incrPc(3);
        clockTicks = 7;
    }

    void ROR_66()
    {
        //  ROR $00
        poke(getAddress(AddressMode.ZEROPAGE), cpu.DoROR(GetByte(AddressMode.ZEROPAGE)) );
        cpu.incrPc(2);
        clockTicks = 5;
    }

    void ROR_76()
    {
        //  ROR $00,X
        poke(getAddress(AddressMode.ZEROPAGEX), cpu.DoROR(GetByte(AddressMode.ZEROPAGEX)) );
        cpu.incrPc(2);
        clockTicks = 6;
    }
    

    /*****************************************************************************\
    | SBC                    |                                                    |
    \*****************************************************************************/
    void SBC_E9()
    {
        //  SBC #$00
        cpu.DoSBC(GetByte(AddressMode.UNMITTELBAR));
        cpu.incrPc(2);
        clockTicks = 2;
    }

    void SBC_E5()
    {
        //  SBC $00
        cpu.DoSBC(GetByte(AddressMode.ZEROPAGE));
        cpu.incrPc(2);
        clockTicks = 3;
    }

    void SBC_F5()
    {
        //  SBC $00,X
        cpu.DoSBC(GetByte(AddressMode.ZEROPAGEX));
        cpu.incrPc(2);
        clockTicks = 4;
    }

    void SBC_ED()
    {
        //  SBC $0000
        cpu.DoSBC(GetByte(AddressMode.ABSOLUT));
        cpu.incrPc(3);
        clockTicks = 4;
    }

    void SBC_FD()
    {
        //  SBC $0000,X
        cpu.DoSBC(GetByte(AddressMode.INDIZIERTX));
        cpu.incrPc(3);
        clockTicks += 4;
    }

    void SBC_F9()
    {
        //  SBC $0000,Y
        cpu.DoSBC(GetByte(AddressMode.INDIZIERTY));
        cpu.incrPc(3);
        clockTicks += 4;
    }

    void SBC_E1()
    {
        //  SBC ($00,X)
        cpu.DoSBC(GetByte(AddressMode.INDIZIERTINDIREKTX));
        cpu.incrPc(2);
        clockTicks = 6;
    }

    void SBC_F1()
    {
        //  SBC ($00),Y
        cpu.DoSBC(GetByte(AddressMode.INDIREKTINDIZIERTY));
        cpu.incrPc(2);
        clockTicks += 5;
    }

    /*****************************************************************************\
    | STA                    |                                                    |
    \*****************************************************************************/
    void STA_85()
    {
        //  STA $00
        poke(getAddress(AddressMode.ZEROPAGE), cpu.acc);
        cpu.incrPc(2);
        clockTicks = 3;
    }

    void STA_95()
    {
        //  STA $00,X
        poke(getAddress(AddressMode.ZEROPAGEX), cpu.acc);
        cpu.incrPc(2);
        clockTicks = 4;
    }

    void STA_8D()
    {
        //  STA $0000
        poke(getAddress(AddressMode.ABSOLUT), cpu.acc);
        cpu.incrPc(3);
        clockTicks = 4;
    }

    void STA_9D()
    {
        //  STA $0000,X
        poke(getAddress(AddressMode.INDIZIERTX), cpu.acc);
        cpu.incrPc(3);
        clockTicks = 5;
    }

    void STA_99()
    {
        //  STA $0000,Y
        poke(getAddress(AddressMode.INDIZIERTY), cpu.acc);
        cpu.incrPc(3);
        clockTicks = 5;
    }

    void STA_81()
    {
        //  STA ($00,X)
        poke(getAddress(AddressMode.INDIZIERTINDIREKTX), cpu.acc);
        cpu.incrPc(2);
        clockTicks = 6;
    }

    void STA_91()
    {
        //  STA ($00),Y
        poke(getAddress(AddressMode.INDIREKTINDIZIERTY), cpu.acc);
        cpu.incrPc(2);
        clockTicks = 6;
    }

    /*****************************************************************************\
    | STX                    |                                                    |
    \*****************************************************************************/
    void STX_86()
    {
        //  STX $00
        poke(getAddress(AddressMode.ZEROPAGE), cpu.xr);
        cpu.incrPc(2);
        clockTicks = 3;
    }

    void STX_96()
    {
        //  STX $00,Y
        poke(getAddress(AddressMode.ZEROPAGEY), cpu.xr);
        cpu.incrPc(2);
        clockTicks = 4;
    }

    void STX_8E()
    {
        //  STX $0000
        poke(getAddress(AddressMode.ABSOLUT), cpu.xr);
        cpu.incrPc(3);
        clockTicks = 4;
    }

    /*****************************************************************************\
    | STY                    |                                                    |
    \*****************************************************************************/
    void STY_84()
    {
        //  STY $00
        poke(getAddress(AddressMode.ZEROPAGE), cpu.yr);
        cpu.incrPc(2);
        clockTicks = 3;
    }

    void STY_94()
    {
        //  STY $00,X
        poke(getAddress(AddressMode.ZEROPAGEX), cpu.yr);
        cpu.incrPc(2);
        clockTicks = 4;
    }

    void STY_8C()
    {
        //  STY $0000
        poke(getAddress(AddressMode.ABSOLUT), cpu.yr);
        cpu.incrPc(3);
        clockTicks = 4;
    }


    /*****************************************************************************\
    | P4Keyboard                                                                  |
    \*****************************************************************************/
    private void p4Keyboard()
    {
        short rc = 0, latch;
        /*

    ZAEHLER idx;
    ZEICHEN buf[5];
    ZEICHEN zeile[2000];

    zeile[0]=0;
    for (idx=0;idx<256;idx++)
       if (KEYDOWN(idx))
       {
       sprintf(buf, "%02x ", idx);
       strcat(zeile, buf);
       }
    fprintf(ticker, "%s\r\n", zeile);

         */

        /*  if (RAM[0xff08] == 0xfa)
       {
          Joystick(1);
          return;
       }
       else
       if (RAM[0xff08] == 0xfd)
       {
          Joystick(2);
          return;
       }
         */
        latch = ram[0xfd30];

        if (latch == 0xff) {
            ram[0xff08] = 0xff;
            return;
        }

        latch ^= 0xff; // Logical NOTs

        /*   if (latch & 1)
       {
          if (KEYDOWN(VK_BACK)) rc |= 1;         // Delete (Backspace)
          if (KEYDOWN(VK_RETURN)) rc |= 2;       // Return
          if (KEYDOWN(0xbb)) rc |= 4;            // Pfund
//          if (KEYDOWN(VK_F4)) rc |= 8;           // Help (F4)
          if (KEYDOWN(VK_F1)) rc |= 16;          // F1
          if (KEYDOWN(VK_F2)) rc |= 32;          // F2
          if (KEYDOWN(VK_F3)) rc |= 64;          // F3
          if (KEYDOWN(0xba)) rc |= 128;          // @
       }

       if (latch & 2)
       {
          if (KEYDOWN('3')) rc |= 1;             // 3
          if (KEYDOWN('W')) rc |= 2;             // W
          if (KEYDOWN('A')) rc |= 4;             // A
          if (KEYDOWN('4')) rc |= 8;             // 4
          if (KEYDOWN('Y')) rc |= 16;            // Z
          if (KEYDOWN('S')) rc |= 32;            // S
          if (KEYDOWN('E')) rc |= 64;            // E
          if (KEYDOWN(VK_SHIFT)) rc |= 128;      // Shift
       }

       if (latch & 4)
       {
          if (KEYDOWN('5')) rc |= 1;             // 5
          if (KEYDOWN('R')) rc |= 2;             // R
          if (KEYDOWN('D')) rc |= 4;             // D
          if (KEYDOWN('6')) rc |= 8;             // 6
          if (KEYDOWN('C')) rc |= 16;            // C
          if (KEYDOWN('F')) rc |= 32;            // F
          if (KEYDOWN('T')) rc |= 64;            // T
          if (KEYDOWN('X')) rc |= 128;           // X
       }

       if (latch & 8)
       {
          if (KEYDOWN('7')) rc |= 1;             // 7
          if (KEYDOWN('Z')) rc |= 2;             // Y
          if (KEYDOWN('G')) rc |= 4;             // G
          if (KEYDOWN('8')) rc |= 8;             // 8
          if (KEYDOWN('B')) rc |= 16;            // B
          if (KEYDOWN('H')) rc |= 32;            // H
          if (KEYDOWN('U')) rc |= 64;            // U
          if (KEYDOWN('V')) rc |= 128;           // V
       }

       if (latch & 16)
       {
          if (KEYDOWN('9')) rc |= 1;             // 9
          if (KEYDOWN('I')) rc |= 2;             // I
          if (KEYDOWN('J')) rc |= 4;             // J
          if (KEYDOWN('0')) rc |= 8;             // 0
          if (KEYDOWN('M')) rc |= 16;            // M
          if (KEYDOWN('K')) rc |= 32;            // K
          if (KEYDOWN('O')) rc |= 64;            // O
          if (KEYDOWN('N')) rc |= 128;           // N
       }

       if (latch & 32)
       {
          if (KEYDOWN(VK_DOWN)) rc |= 1;         // Down
          if (KEYDOWN('P')) rc |= 2;             // P
          if (KEYDOWN('L')) rc |= 4;             // L
          if (KEYDOWN(VK_UP)) rc |= 8;           // Up
          if (KEYDOWN(0xbe)) rc |= 16;           // .
          if (KEYDOWN(0xc0)) rc |= 32;           // [
          if (KEYDOWN(0xdd)) rc |= 64;           // -
          if (KEYDOWN(0xbc)) rc |= 128;          // ,
       }

       if (latch & 64)
       {
          if (KEYDOWN(VK_LEFT)) rc |= 1;         // Right
          if (KEYDOWN(0xbf)) rc |= 2;            // *
          if (KEYDOWN(0xde)) rc |= 4;            // ]
          if (KEYDOWN(VK_RIGHT)) rc |= 8;        // Left
          if (KEYDOWN(VK_ESCAPE)) rc |= 16;      // ESC
          if (KEYDOWN(VK_INSERT)) rc |= 32;      // = (Einf)
          if (KEYDOWN(0xdb)) rc |= 64;           // +
          if (KEYDOWN(0xbd)) rc |= 128;          // /
       }

       if (latch & 128)
       {
          if (KEYDOWN('1')) rc |= 1;             // 1
          if (KEYDOWN(VK_HOME)) rc |= 2;         // Clr/Home (Pos1)
          if (KEYDOWN(VK_CONTROL)) rc |= 4;      // Ctrl
          if (KEYDOWN('2')) rc |= 8;             // 2
          if (KEYDOWN(' ')) rc |= 16;            // Space
          if (KEYDOWN(0x12)) rc |= 32;           // C= (Alt)
          if (KEYDOWN('Q')) rc |= 64;            // Q
          if (KEYDOWN(VK_TAB)) rc |= 128;        // Run/Stop (TAB)
       }
         */
        rc ^= 0xff; // Logical NOT

        ram[0xff08] = rc;
    }


    /*****************************************************************************\
    | P4Joystick                                                                  |
    \*****************************************************************************/
    private void p4Joystick(int port)
    {
        short rc = 0;

//        if (pcjoy != port) {
            //          ram[0xff08] = 0xff;
//            return;
//        }
        
//       if (KEYDOWN(VK_NUMPAD8))                      rc = 1;         // 1
//       if (KEYDOWN(VK_NUMPAD2))                      rc = 2;         // 5
//       if (KEYDOWN(VK_NUMPAD4))                      rc = 4;         // 7
//       if (KEYDOWN(VK_NUMPAD6))                      rc = 8;         // 3
//       if (KEYDOWN(VK_NUMPAD3))                      rc = 10;        // 4
//       if (KEYDOWN(VK_NUMPAD1))                      rc = 6;         // 6
//       if (KEYDOWN(VK_NUMPAD7))                      rc = 5;         // 8
//       if (KEYDOWN(VK_NUMPAD9))                      rc = 9;         // 2
//
//       if (KEYDOWN(VK_NUMPAD0))
//          rc |= 16|32|64|128;

       rc ^= 0xff;

       ram[0xff08] = rc;
    }


    /*****************************************************************************\
    | run                                                                         |
    \*****************************************************************************/
    public void run()
    {
        boolean timerOverflow[] = new boolean[3];
        short bit = 0;
        int clockMultiplier;
//        int keyboardzaehler = 0;
        int endeZaehler = 0;

        do {
            endeZaehler ++;
//            if (endeZaehler > 500000)
//                return;

//            System.out.println("" + endeZaehler);
            
//            /* Gelegentlich mal nach Windows schauen *******************************/
//            keyboardzaehler += clock;
//            if (keyboardzaehler > 3000) {
//                keyboardzaehler = 0;
//            }

            /* Falls der Datasettenmotor läuft, Daten senden ***********************/
            //      if (!(ram[0xfd10] & 0x04))
            //         DatasetteAbfragen();

            /* Für Debugger die letzten Assemblerbefehle merken ********************/
            /*      if (debugger->mode != DB_STEPOVER)
                {
                PCHistoryEintragen();
                }*/

            /* Assemblerbefhl über Spruntabelle ausführen **************************/
            clockTicks = 0;
            befehlAusfuehren();

            if ((ram[0xff06] & 16) > 0)
                clockMultiplier = 2;
            else
                clockMultiplier = 1;

            /* Systemtakt und Blinkzähler hochzählen *******************************/
            clockCounter += clockTicks*clockMultiplier;
            flashCounter += clockTicks;

            // TED-Timer A, B & C:

            for (int timerIdx = 0; timerIdx < 3; timerIdx++) {
                if (timerOn[timerIdx]) {
                    int timerValue = ram[0xff00 + timerIdx*2] + ((ram[0xff01 + timerIdx*2]) << 8);
                    timerOverflow[timerIdx] = (timerValue < clockTicks*clockMultiplier);

                    timerValue = toWord(timerValue - clockTicks*clockMultiplier);
                    if (timerOverflow[timerIdx] && timerIdx == 0)
                        timerValue -= 0xc60e;

                    ram[0xff00 + timerIdx*2] = (short) (timerValue & 0xff);
                    ram[0xff01 + timerIdx*2] = (short) ((timerValue & 0xff00) >> 8);
                }
            }
            
            for (int timerIdx = 0; timerIdx < 3; timerIdx++) {
                if (timerOn[timerIdx]) {
                    // Timer löst Interrupt aus:
                    if (timerOverflow[timerIdx]) {
                        switch (timerIdx) {
                            case 0:  bit = 8;    // IRR: Bit 3 = IRQ durch Timer A
                            break;
                            case 1:  bit = 16;   // IRR: Bit 4 = IRQ durch Timer B
                            break;
                            case 2:  bit = 64;   // IRR: Bit 6 = IRQ durch Timer C
                            break;
                        }

                        ram[0xff09] |= bit+128;

                        if (!cpu.i && (ram[0xff0a] & bit) > 0) {   // IMR
                            Dush(cpu.pc);                  // Register sichern
                            Push(GetFlags());
                            cpu.i = true;                     // IRQs abschalten
                            cpu.pc = (rom[0xffff - 0x8000] << 8) + rom[0xfffe - 0x8000];
                        }
                    }
                }
            }
            
            /* Eventuell Rasterzeile darstellen ************************************/
            if (clockCounter > TAKTEPRORASTERZEILE*FX) {
                clockCounter -= TAKTEPRORASTERZEILE*FX;
                rasterzeileDarstellen();
            }
        } while(true);
    }


    /*****************************************************************************\
    | ZeichenAnzeigen                                                             |
    \*****************************************************************************/
    private void zeichenAnzeigen(int z, int x, int y,
                                 int cy, 
                                 short color, short bgColor,
                                 boolean isCursorChar)
    {
        short by;
        final int charsetbase = ((ram[0xff13] & 252) << 8);

        if (isCursorChar && flashOn) {
            z ^= 0x80;
        }

        if ((ram[0xff07] & 128) > 0) {
            // Zeichensatz aus 256 Zeichen
            if ((ram[0xff12] & 4) > 0)
                by = rom[toWord(charsetbase + z*8 + cy - 0x8000)];
            else
                by = ram[toWord(charsetbase + z*8 + cy)];
        } else {
            // Zeichen über 127 invers darstellen
            if (z > 127) {
                if ((ram[0xff12] & 4) > 0)
                    by = (short) ~(rom(charsetbase + (z-128)*8 + cy));
                else
                    by = (short) ~(ram[toWord(charsetbase + (z-128)*8 + cy)]);
            } else {
                if ((ram[0xff12] & 4) > 0)
                    by = rom(charsetbase + z*8 + cy);
                else
                    by = ram[toWord(charsetbase + z*8 + cy)];
            }
        }

        if ((ram[0xff07] & 16) > 0 && (color & 8) > 0) {
            // Multicolor
            for (int cx = 0; cx < 4; cx++) {
                short pixel;
                switch(by & 3) {
                    case 3:
                        pixel = (short) (color & ~8);
                        break;
                    case 2:
                        pixel = ram[0xff17];
                        break;
                    case 1: 
                        pixel = ram[0xff18];
                        break;
                    case 0: 
                        pixel = bgColor;
                        break;
                    default:
                        throw new IllegalStateException();
                }
                pixel &= 127;
                pixels[y][x*8 + 6-cx*2] = pixel;
                pixels[y][x*8 + 7-cx*2] = pixel;

                by = (short) (by >> 2);
            }
        } else {
            for (int cx = 0; cx < 8; cx++) {
                if ((by & (1 << (7 - cx))) > 0 && !((color & 0x80) > 0 && !flashOn)) {
                    pixels[y][cx+x*8] = color;
                } else {
                    pixels[y][cx+x*8] = bgColor;
                }
            }
        }
    }


    /*****************************************************************************\
    | RasterzeileDarstellen                                                       |
    \*****************************************************************************/
    private void rasterzeileDarstellen()
    {
        boolean isCursorChar;

        if (flashCounter >= TAKTEPROBLINKINTERVALL) {
            flashCounter = 0;
            flashOn = !flashOn;
        }

        ram[0xff1c] = (short) ((rasterLine >> 8) | 0xfe);
        ram[0xff1d] = (short) (rasterLine & 255);

        ram[0xff1e] = 0xff;                     // Dummy!!!!

        final int rasterInterruptLine = ((ram[0xff0a] & 1) << 8) + ram[0xff0b];

        if (rasterLine >= FIRSTSCREENLINE && rasterLine < SCREENHEIGHT+FIRSTSCREENLINE) {
            final int videoMatrixAddress = (ram[0xff14] & 248) << 8;
            final short bgColor = (short) (ram[0xff15] & 0x7f);
            final int hiresBase = (ram[0xff12] >> 3) * 0x2000;

            final int textY = ((rasterLine - FIRSTSCREENLINE) >> 3);
            final int charY = (rasterLine - FIRSTSCREENLINE) & 7;
            final int pixelY = rasterLine - FIRSTSCREENLINE;

            if ((ram[0xff06] & 16) > 0) { // Screen is On
                if ((ram[0xff06] & 32) > 0) {             /* HiRes */
                    for (int x = 0; x < 320; x++)
                        if ((ram[hiresBase + (pixelY/8)*320+(pixelY & 7) + (x/8)*8] & (1 << (7-(x & 7)))) > 0) {
                            pixels[pixelY][x] = (short) (ram[0x1800 + (pixelY/8)*40 + (x/8)] & 0x7f);
                        } else {
                            pixels[pixelY][x] = bgColor;
                        }
                } else  if ((ram[0xff06] & 64) > 0) {     /* Multi Color Mode */
                    throw new UnsupportedOperationException("MultiColorMode");
                } else {
                    /* Textmodus */
                    final int cursorAddress = (ram[0xff0c] << 8) + ram[0xff0d];
                    for (int x = 0; x < 40; x++) {
                        isCursorChar = (cursorAddress == x+textY*40 && flashOn && !((ram[0xff06] & 32) > 0));
                        zeichenAnzeigen(ram[(videoMatrixAddress+1024+x+textY*40) & 0xffff],
                                        x, pixelY, charY,
                                        ram[(videoMatrixAddress+x+textY*40) & 0xffff],
                                        bgColor, isCursorChar);
                    }
                }
            } else {
                // Blank whole screen
                final short frameColor = (short) (ram[0xff19] & 127);
                for (int x = 0; x < 320; x++) {
                    pixels[pixelY][x] = frameColor;
                }
            }
            screen.updateLine(pixelY, pixels[pixelY]);
        }

        rasterLine ++;
        if (rasterLine >= RASTERLINES) {
            rasterLine = 0;
        }

        if (rasterLine == rasterInterruptLine) {
            ram[0xff09] |= 2+128;

            if (!cpu.i && (ram[0xff0a] & 2)  > 0) {
                Dush(cpu.pc);                  // Register sichern
                Push(GetFlags());
                cpu.i = true;                     // IRQs abschalten
                cpu.pc = (rom[0xffff - 0x8000] << 8) + rom[0xfffe - 0x8000];
            }
        }
    }
}
