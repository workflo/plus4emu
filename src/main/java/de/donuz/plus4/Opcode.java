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
 * $Id: Opcode.java,v 1.7 2009/06/13 18:09:23 florian Exp $
 */
package de.donuz.plus4;

public class Opcode
{
    public final String mnemonic;
    public final AddressMode addrMode;

    public Opcode(String mnemonic, AddressMode addrMode)
    {
        this.mnemonic = mnemonic;
        this.addrMode = addrMode;
    }
    
    
    public final static Opcode[] opcodes = new Opcode[] {
        new Opcode("BRK", AddressMode.NONE), // $00
        new Opcode("ORA", AddressMode.INDIZIERTINDIREKTX), // $01
        new Opcode("???", AddressMode.UNDEFINED), // $02
        new Opcode("???", AddressMode.UNDEFINED), // $03
        new Opcode("???", AddressMode.UNDEFINED), // $04
        new Opcode("ORA", AddressMode.ZEROPAGE), // $05
        new Opcode("ASL", AddressMode.ZEROPAGE), // $06
        new Opcode("???", AddressMode.UNDEFINED), // $07
        new Opcode("PHP", AddressMode.NONE), // $08
        new Opcode("ORA", AddressMode.UNMITTELBAR), // $09
        new Opcode("ASL", AddressMode.NONE), // $0a
        new Opcode("???", AddressMode.UNDEFINED), // $0b
        new Opcode("???", AddressMode.UNDEFINED), // $0c
        new Opcode("ORA", AddressMode.ABSOLUT), // $0d
        new Opcode("ASL", AddressMode.ABSOLUT), // $0e
        new Opcode("???", AddressMode.UNDEFINED), // $0f
        new Opcode("BPL", AddressMode.REL_BRANCH), // $10
        new Opcode("ORA", AddressMode.INDIREKTINDIZIERTY), // $11
        new Opcode("???", AddressMode.UNDEFINED), // $12
        new Opcode("???", AddressMode.UNDEFINED), // $13
        new Opcode("???", AddressMode.UNDEFINED), // $14
        new Opcode("ORA", AddressMode.ZEROPAGEX), // $15
        new Opcode("???", AddressMode.UNDEFINED), // $16
        new Opcode("???", AddressMode.UNDEFINED), // $17
        new Opcode("CLC", AddressMode.NONE), // $18
        new Opcode("ORA", AddressMode.INDIZIERTY), // $19
        new Opcode("???", AddressMode.UNDEFINED), // $1a
        new Opcode("???", AddressMode.UNDEFINED), // $1b
        new Opcode("???", AddressMode.UNDEFINED), // $1c
        new Opcode("ORA", AddressMode.INDIZIERTX), // $1d
        new Opcode("ASL", AddressMode.INDIZIERTX), // $1e
        new Opcode("???", AddressMode.UNDEFINED), // $1f
        new Opcode("JSR", AddressMode.ABSOLUT), // $20
        new Opcode("AND", AddressMode.INDIREKTINDIZIERTY), // $21
        new Opcode("???", AddressMode.UNDEFINED), // $22
        new Opcode("???", AddressMode.UNDEFINED), // $23
        new Opcode("BIT", AddressMode.ZEROPAGE), // $24
        new Opcode("AND", AddressMode.ZEROPAGE), // $25
        new Opcode("ROL", AddressMode.ZEROPAGE), // $26
        new Opcode("???", AddressMode.UNDEFINED), // $27
        new Opcode("PLP", AddressMode.NONE), // $28
        new Opcode("AND", AddressMode.UNMITTELBAR), // $29
        new Opcode("ROL", AddressMode.NONE), // $2a
        new Opcode("???", AddressMode.UNDEFINED), // $2b
        new Opcode("BIT", AddressMode.ABSOLUT), // $2c
        new Opcode("AND", AddressMode.ABSOLUT), // $2d
        new Opcode("ROL", AddressMode.ABSOLUT), // $2e
        new Opcode("???", AddressMode.UNDEFINED), // $2f
        new Opcode("BMI", AddressMode.REL_BRANCH), // $30
        new Opcode("AND", AddressMode.INDIZIERTINDIREKTX), // $31
        new Opcode("???", AddressMode.UNDEFINED), // $32
        new Opcode("???", AddressMode.UNDEFINED), // $33
        new Opcode("???", AddressMode.UNDEFINED), // $34
        new Opcode("AND", AddressMode.ZEROPAGEX), // $35
        new Opcode("ROL", AddressMode.ZEROPAGEX), // $36
        new Opcode("???", AddressMode.UNDEFINED), // $37
        new Opcode("SEC", AddressMode.NONE), // $38
        new Opcode("AND", AddressMode.INDIZIERTY), // $39
        new Opcode("???", AddressMode.UNDEFINED), // $3a
        new Opcode("???", AddressMode.UNDEFINED), // $3b
        new Opcode("???", AddressMode.UNDEFINED), // $3c
        new Opcode("AND", AddressMode.INDIZIERTX), // $3d
        new Opcode("ROL", AddressMode.INDIZIERTX), // $3e
        new Opcode("???", AddressMode.UNDEFINED), // $3f
        new Opcode("RTI", AddressMode.NONE), // $40
        new Opcode("EOR", AddressMode.INDIZIERTINDIREKTX), // $41
        new Opcode("???", AddressMode.UNDEFINED), // $42
        new Opcode("???", AddressMode.UNDEFINED), // $43
        new Opcode("???", AddressMode.UNDEFINED), // $44
        new Opcode("EOR", AddressMode.ZEROPAGE), // $45
        new Opcode("LSR", AddressMode.ZEROPAGE), // $46
        new Opcode("???", AddressMode.UNDEFINED), // $47
        new Opcode("PHA", AddressMode.NONE), // $48
        new Opcode("EOR", AddressMode.UNMITTELBAR), // $49
        new Opcode("LSR", AddressMode.NONE), // $4a
        new Opcode("???", AddressMode.UNDEFINED), // $4b
        new Opcode("JMP", AddressMode.ABSOLUT), // $4c
        new Opcode("EOR", AddressMode.ABSOLUT), // $4d
        new Opcode("LSR", AddressMode.ABSOLUT), // $4e
        new Opcode("???", AddressMode.UNDEFINED), // $4f
        new Opcode("BVC", AddressMode.REL_BRANCH), // $50
        new Opcode("EOR", AddressMode.INDIREKTINDIZIERTY), // $51
        new Opcode("???", AddressMode.UNDEFINED), // $52
        new Opcode("???", AddressMode.UNDEFINED), // $53
        new Opcode("???", AddressMode.UNDEFINED), // $54
        new Opcode("EOR", AddressMode.ZEROPAGEX), // $55
        new Opcode("LSR", AddressMode.ZEROPAGEX), // $56
        new Opcode("???", AddressMode.UNDEFINED), // $57
        new Opcode("CLI", AddressMode.NONE), // $58
        new Opcode("EOR", AddressMode.INDIZIERTY), // $59
        new Opcode("???", AddressMode.UNDEFINED), // $5a
        new Opcode("???", AddressMode.UNDEFINED), // $5b
        new Opcode("???", AddressMode.UNDEFINED), // $5c
        new Opcode("EOR", AddressMode.INDIZIERTX), // $5d
        new Opcode("LSR", AddressMode.INDIZIERTX), // $5e
        new Opcode("???", AddressMode.UNDEFINED), // $5f
        new Opcode("RTS", AddressMode.NONE), // $60
        new Opcode("ADC", AddressMode.INDIREKTINDIZIERTY), // $61
        new Opcode("???", AddressMode.UNDEFINED), // $62
        new Opcode("???", AddressMode.UNDEFINED), // $63
        new Opcode("???", AddressMode.UNDEFINED), // $64
        new Opcode("ADC", AddressMode.ZEROPAGE), // $65
        new Opcode("ROR", AddressMode.ZEROPAGE), // $66
        new Opcode("???", AddressMode.UNDEFINED), // $67
        new Opcode("PLA", AddressMode.NONE), // $68
        new Opcode("ADC", AddressMode.UNMITTELBAR), // $69
        new Opcode("ROR", AddressMode.NONE), // $6a
        new Opcode("???", AddressMode.UNDEFINED), // $6b
        new Opcode("JMP", AddressMode.INDIRECT_JUMP), // $6c
        new Opcode("ADC", AddressMode.ABSOLUT), // $6d
        new Opcode("ROR", AddressMode.ABSOLUT), // $6e
        new Opcode("???", AddressMode.UNDEFINED), // $6f
        new Opcode("BVS", AddressMode.REL_BRANCH), // $70
        new Opcode("ADC", AddressMode.INDIZIERTINDIREKTX), // $71
        new Opcode("???", AddressMode.UNDEFINED), // $72
        new Opcode("???", AddressMode.UNDEFINED), // $73
        new Opcode("???", AddressMode.UNDEFINED), // $74
        new Opcode("ADC", AddressMode.ZEROPAGEX), // $75
        new Opcode("ROR", AddressMode.ZEROPAGEX), // $76
        new Opcode("???", AddressMode.UNDEFINED), // $77
        new Opcode("SEI", AddressMode.NONE), // $78
        new Opcode("ADC", AddressMode.INDIZIERTY), // $79
        new Opcode("???", AddressMode.UNDEFINED), // $7a
        new Opcode("???", AddressMode.UNDEFINED), // $7b
        new Opcode("???", AddressMode.UNDEFINED), // $7c
        new Opcode("ADC", AddressMode.INDIZIERTX), // $7d
        new Opcode("ROR", AddressMode.INDIZIERTX), // $7e
        new Opcode("???", AddressMode.UNDEFINED), // $7f
        new Opcode("???", AddressMode.UNDEFINED), // $80
        new Opcode("STA", AddressMode.INDIREKTINDIZIERTY), // $81
        new Opcode("???", AddressMode.UNDEFINED), // $82
        new Opcode("???", AddressMode.UNDEFINED), // $83
        new Opcode("STY", AddressMode.ZEROPAGE), // $84
        new Opcode("STA", AddressMode.ZEROPAGE), // $85
        new Opcode("STX", AddressMode.ZEROPAGE), // $86
        new Opcode("???", AddressMode.UNDEFINED), // $87
        new Opcode("DEY", AddressMode.NONE), // $88
        new Opcode("???", AddressMode.UNDEFINED), // $89
        new Opcode("TXA", AddressMode.NONE), // $8a
        new Opcode("???", AddressMode.UNDEFINED), // $8b
        new Opcode("STY", AddressMode.ABSOLUT), // $8c
        new Opcode("STA", AddressMode.ABSOLUT), // $8d
        new Opcode("STX", AddressMode.ABSOLUT), // $8e
        new Opcode("???", AddressMode.UNDEFINED), // $8f
        new Opcode("BCC", AddressMode.REL_BRANCH), // $90
        new Opcode("STA", AddressMode.INDIREKTINDIZIERTY), // $91
        new Opcode("???", AddressMode.UNDEFINED), // $92
        new Opcode("???", AddressMode.UNDEFINED), // $93
        new Opcode("STY", AddressMode.ZEROPAGEX), // $94
        new Opcode("STA", AddressMode.INDIZIERTX), // $95
        new Opcode("STX", AddressMode.INDIZIERTY), // $96
        new Opcode("???", AddressMode.UNDEFINED), // $97
        new Opcode("TAY", AddressMode.NONE), // $98
        new Opcode("STA", AddressMode.INDIZIERTY), // $99
        new Opcode("TXS", AddressMode.NONE), // $9a
        new Opcode("???", AddressMode.UNDEFINED), // $9b
        new Opcode("???", AddressMode.UNDEFINED), // $9c
        new Opcode("STA", AddressMode.INDIZIERTX), // $9d
        new Opcode("???", AddressMode.UNDEFINED), // $9e
        new Opcode("???", AddressMode.UNDEFINED), // $9f
        new Opcode("LDY", AddressMode.UNMITTELBAR), // $a0
        new Opcode("LDA", AddressMode.INDIZIERTINDIREKTX), // $a1
        new Opcode("LDX", AddressMode.UNMITTELBAR), // $a2
        new Opcode("???", AddressMode.UNDEFINED), // $a3
        new Opcode("LDY", AddressMode.ZEROPAGE), // $a4
        new Opcode("LDA", AddressMode.ZEROPAGE), // $a5
        new Opcode("LDX", AddressMode.ZEROPAGE), // $a6
        new Opcode("???", AddressMode.UNDEFINED), // $a7
        new Opcode("TAY", AddressMode.NONE), // $a8
        new Opcode("LDA", AddressMode.UNMITTELBAR), // $a9
        new Opcode("TAX", AddressMode.NONE), // $aa
        new Opcode("???", AddressMode.UNDEFINED), // $ab
        new Opcode("LDY", AddressMode.ABSOLUT), // $ac
        new Opcode("LDA", AddressMode.ABSOLUT), // $ad
        new Opcode("LDX", AddressMode.ABSOLUT), // $ae
        new Opcode("???", AddressMode.UNDEFINED), // $af
        new Opcode("BCS", AddressMode.REL_BRANCH), // $b0
        new Opcode("LDA", AddressMode.INDIREKTINDIZIERTY), // $b1
        new Opcode("???", AddressMode.UNDEFINED), // $b2
        new Opcode("???", AddressMode.UNDEFINED), // $b3
        new Opcode("LDY", AddressMode.ZEROPAGEX), // $b4
        new Opcode("LDA", AddressMode.ZEROPAGEX), // $b5
        new Opcode("LDX", AddressMode.INDIZIERTY), // $b6
        new Opcode("???", AddressMode.UNDEFINED), // $b7
        new Opcode("CLV", AddressMode.NONE), // $b8
        new Opcode("LDA", AddressMode.INDIZIERTY), // $b9
        new Opcode("TSX", AddressMode.NONE), // $ba
        new Opcode("???", AddressMode.UNDEFINED), // $bb
        new Opcode("LDY", AddressMode.INDIZIERTX), // $bc
        new Opcode("LDA", AddressMode.INDIZIERTX), // $bd
        new Opcode("LDX", AddressMode.INDIZIERTY), // $be
        new Opcode("???", AddressMode.UNDEFINED), // $bf
        new Opcode("CPY", AddressMode.UNMITTELBAR), // $c0
        new Opcode("CMP", AddressMode.INDIZIERTINDIREKTX), // $c1
        new Opcode("???", AddressMode.UNDEFINED), // $c2
        new Opcode("???", AddressMode.UNDEFINED), // $c3
        new Opcode("CPY", AddressMode.ZEROPAGE), // $c4
        new Opcode("CMP", AddressMode.ZEROPAGE), // $c5
        new Opcode("DEC", AddressMode.ZEROPAGE), // $c6
        new Opcode("???", AddressMode.UNDEFINED), // $c7
        new Opcode("INY", AddressMode.NONE), // $c8
        new Opcode("CMP", AddressMode.UNMITTELBAR), // $c9
        new Opcode("DEX", AddressMode.NONE), // $ca
        new Opcode("???", AddressMode.UNDEFINED), // $cb
        new Opcode("CPY", AddressMode.ABSOLUT), // $cc
        new Opcode("CMP", AddressMode.ABSOLUT), // $cd
        new Opcode("DEC", AddressMode.ABSOLUT), // $ce
        new Opcode("???", AddressMode.UNDEFINED), // $cf
        new Opcode("BNE", AddressMode.REL_BRANCH), // $d0
        new Opcode("CMP", AddressMode.INDIREKTINDIZIERTY), // $d1
        new Opcode("???", AddressMode.UNDEFINED), // $d2
        new Opcode("???", AddressMode.UNDEFINED), // $d3
        new Opcode("???", AddressMode.UNDEFINED), // $d4
        new Opcode("CMP", AddressMode.ZEROPAGEX), // $d5
        new Opcode("DEC", AddressMode.ZEROPAGEX), // $d6
        new Opcode("???", AddressMode.UNDEFINED), // $d7
        new Opcode("CLD", AddressMode.NONE), // $d8
        new Opcode("CMP", AddressMode.INDIZIERTY), // $d9
        new Opcode("???", AddressMode.UNDEFINED), // $da
        new Opcode("???", AddressMode.UNDEFINED), // $db
        new Opcode("???", AddressMode.UNDEFINED), // $dc
        new Opcode("CMP", AddressMode.INDIZIERTX), // $dd
        new Opcode("DEC", AddressMode.INDIZIERTX), // $de
        new Opcode("???", AddressMode.UNDEFINED), // $df
        new Opcode("CPX", AddressMode.UNMITTELBAR), // $e0
        new Opcode("SBC", AddressMode.INDIZIERTINDIREKTX), // $e1
        new Opcode("???", AddressMode.UNDEFINED), // $e2
        new Opcode("???", AddressMode.UNDEFINED), // $e3
        new Opcode("CPX", AddressMode.ZEROPAGE), // $e4
        new Opcode("SBC", AddressMode.ZEROPAGE), // $e5
        new Opcode("INC", AddressMode.ZEROPAGE), // $e6
        new Opcode("???", AddressMode.UNDEFINED), // $e7
        new Opcode("INX", AddressMode.NONE), // $e8
        new Opcode("SBC", AddressMode.UNMITTELBAR), // $e9
        new Opcode("NOP", AddressMode.NONE), // $ea
        new Opcode("???", AddressMode.UNDEFINED), // $eb
        new Opcode("CPX", AddressMode.ABSOLUT), // $ec
        new Opcode("SBC", AddressMode.ABSOLUT), // $ed
        new Opcode("INC", AddressMode.ABSOLUT), // $ee
        new Opcode("???", AddressMode.UNDEFINED), // $ef
        new Opcode("BEQ", AddressMode.REL_BRANCH), // $f0
        new Opcode("SBC", AddressMode.INDIREKTINDIZIERTY), // $f1
        new Opcode("???", AddressMode.UNDEFINED), // $f2
        new Opcode("???", AddressMode.UNDEFINED), // $f3
        new Opcode("???", AddressMode.UNDEFINED), // $f4
        new Opcode("SBC", AddressMode.INDIZIERTX), // $f5
        new Opcode("INC", AddressMode.ZEROPAGEX), // $f6
        new Opcode("???", AddressMode.UNDEFINED), // $f7
        new Opcode("SED", AddressMode.NONE), // $f8
        new Opcode("???", AddressMode.UNDEFINED), // $f9
        new Opcode("???", AddressMode.UNDEFINED), // $fa
        new Opcode("???", AddressMode.UNDEFINED), // $fb
        new Opcode("???", AddressMode.UNDEFINED), // $fc
        new Opcode("SBC", AddressMode.INDIZIERTX), // $fd
        new Opcode("INC", AddressMode.INDIZIERTX), // $fe
        new Opcode("???", AddressMode.UNDEFINED), // $ff
    };

    public static String toString(int addr, short cmd, short by1, short by2)
    {
        final StringBuffer sb = new StringBuffer();
        final Opcode opcode = opcodes[cmd];
        
        sb.append(formatWord(addr))
          .append(" ")
          .append(formatByte(cmd))
          .append(" ");
        
        switch (opcode.addrMode) {
            case NONE:
                sb.append("      ")
                  .append(opcode.mnemonic);
                break;
            case UNMITTELBAR:
                sb.append(formatByte(by1))
                  .append("    ")
                  .append(opcode.mnemonic).append(" ")
                  .append("#$").append(formatByte(by1));
                break;
            case ZEROPAGE:
                sb.append(formatByte(by1))
                  .append("    ")
                  .append(opcode.mnemonic).append(" ")
                  .append("$").append(formatByte(by1))
                  .append("      ").append(RomListing.getAddressDescription(by1));
                break;
            case ABSOLUT:
                sb.append(formatByte(by1))
                  .append(" ")
                  .append(formatByte(by2))
                  .append(" ")
                  .append(opcode.mnemonic).append(" ")
                  .append("$").append(formatWord(by1 + (by2 << 8)))
                  .append("    ").append(RomListing.getAddressDescription(by1 + (by2<<8)));
                break;
            case INDIZIERTX:
                sb.append(formatByte(by1))
                  .append(" ")
                  .append(formatByte(by2))
                  .append(" ")
                  .append(opcode.mnemonic).append(" ")
                  .append("$").append(formatWord(by1 + (by2 << 8)))
                  .append(",X");
                break;
            case INDIZIERTY:
                sb.append(formatByte(by1))
                  .append(" ")
                  .append(formatByte(by2))
                  .append(" ")
                  .append(opcode.mnemonic).append(" ")
                  .append("$").append(formatWord(by1 + (by2 << 8)))
                  .append(",Y");
                break;
            case INDIREKTINDIZIERTY:

                sb.append(formatByte(by1))
                  .append("    ")
                  .append(opcode.mnemonic).append(" ")
                  .append("($").append(formatByte(by1))
                  .append("),Y");
                break;
            case REL_BRANCH:
                final int dest;
                if ((by1 & 0x80) > 0) {
                    dest = Plus4.toWord(addr - 126 + (by1 & 127));
                } else {
                    dest = Plus4.toWord(addr + by1 + 2);
                }
                sb.append(formatByte(by1))
                  .append("    ")
                  .append(opcode.mnemonic).append(" ")
                  .append("$").append(formatWord(dest))
                  .append("     ");
                break;
            case INDIRECT_JUMP:
                sb.append(formatByte(by1))
                .append(" ")
                .append(formatByte(by2))
                .append(" ")
                .append(opcode.mnemonic).append(" ")
                .append("($").append(formatWord(by1 + (by2 << 8)))
                .append(")   ");
              break;
        }
        
        return sb.toString();
    }

    public static String formatByte(short by)
    {
        if (by <= 0x0f) {
            return "0" + Long.toHexString(by);
        } else {
            return Long.toHexString(by);
        }
    }

    public static String formatWord(int wo)
    {
        if (wo <= 0x0f) {
            return "000" + Long.toHexString(wo);
        } else if (wo <= 0x0ff) {
            return "00" + Long.toHexString(wo);
        } else if (wo <= 0x0fff) {
            return "0" + Long.toHexString(wo);
        } else {
            return Long.toHexString(wo);
        }
    }
}
