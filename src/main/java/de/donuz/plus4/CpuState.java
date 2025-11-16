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
 * $Id: CpuState.java,v 1.8 2009/06/14 15:49:49 florian Exp $
 */
package de.donuz.plus4;

public class CpuState
{

    public int pc;
    public short acc;
    public short xr;
    public short yr;
    public int sp;
    public boolean c;
    public boolean z;
    public boolean n;
    public boolean b;
    public boolean v;
    public boolean i;
    public boolean d;
    
    
    public void incrPc(int inc)
    {
        pc += inc;
        pc &= 0xffff;
    }


    public void incrSp()
    {
        sp ++;
        sp &= 0xff;
    }


    public void decrSp()
    {
        sp --;
        if (sp < 0) sp = 0xff;
    }


    void NegFlag(short by)
    { 
        n = ((by & 0x80) > 0); 
    }

    void ZeroFlag(short by)
    {
        z = (by == 0);
    }

    public void DoADC(short data)
    {
        short oldAcc = acc;

        if (d) {
            // Decimal Mode (BCD)
            final int carryIn = c ? 1 : 0;

            // Add low nibbles
            int low = (acc & 0x0F) + (data & 0x0F) + carryIn;
            if (low > 0x09) {
                low += 0x06;
            }

            // Add high nibbles
            int high = (acc >> 4) + (data >> 4) + (low > 0x0F ? 1 : 0);

            // Set flags based on binary result for compatibility
            final int binResult = acc + data + carryIn;
            z = ((binResult & 0xFF) == 0);
            n = ((binResult & 0x80) != 0);
            v = (((~(oldAcc ^ data)) & (oldAcc ^ binResult) & 0x80) != 0);

            if (high > 0x09) {
                high += 0x06;
            }

            c = (high > 0x0F);
            acc = toByte(((high << 4) | (low & 0x0F)) & 0xFF);
        } else {
            // Binary Mode:
            final int raw = data + acc + (c ? 1 : 0);
            acc = toByte(raw);
            c = (raw > 0xff);

            NegFlag(acc);
            ZeroFlag(acc);

            // Overflow flag: set if sign bit is incorrect
            v = (((~(oldAcc ^ data)) & (oldAcc ^ acc) & 0x80) != 0);
        }
    }
    
    public void DoAND(short second)
    {
        acc &= second;
        NegFlag(acc);
        ZeroFlag(acc);
    }
    
    public short DoASL(short data)
    {        
        c = ((data & 0x80) > 0);
        data <<= 1;
        data &= 0xff;
        ZeroFlag(data);
        NegFlag(data);
        
        return data;
    }

    public void DoBIT(short data)
    {
        v = ((data & 64) > 0);
        n = ((data & 128) > 0);
        z = ((data & acc) == 0);
    }

    public void DoCMP(short reg, short data)
    {
        c = (reg >= data);
        z = (data == reg);
        n = (toByte(reg - data) > 0x7f);
    }

    public void DoEOR(short value)
    {
        acc ^= value;
        acc &= 0xff;
        NegFlag(acc);
        ZeroFlag(acc);
    }

    public short DoLSR(short data)
    {
        c = ((data & 0x01) > 0);
        data >>= 1;
        ZeroFlag(data);
        n = false;

        return data;
    }

    public void DoORA(short value)
    {
        acc |= value;
        NegFlag(acc);
        ZeroFlag(acc);
    }

    public short DoROL(short data)
    {
        final boolean newC = ((data & 0x80) > 0);
        data <<= 1;
        data &= 0xff;
        if (c) {
            data |= 1;
        }
        c = newC;
        NegFlag(data);
        ZeroFlag(data);
        
        return data;
    }

    public short DoROR(short data)
    {
        final boolean newC = ((data & 1) > 0);
        data >>= 1;
        if (c) { 
            data |= 0x80;
        }
        c = newC;
        NegFlag(data);
        ZeroFlag(data);

        return data;
    }

    public void DoSBC(short data)
    {
        final short oldAcc = acc;

        if (d) {
            int tmpDec = (acc & 0x0f) - (data & 0x0f) - (c ? 0 : 1);

            if ((tmpDec & 0x10) > 0) {                                       
                tmpDec = ((tmpDec - 6) & 0xf) | ((acc & 0xf0) - (data & 0xf0) - 0x10);
            } else {                                                 
                tmpDec = (tmpDec & 0xf) | ((acc & 0xf0) - (data & 0xf0));
            }
            if ((tmpDec & 0x100) > 0) {
                tmpDec -= 0x60;                                                 
            }
            
            acc = toByte(tmpDec);
            c = (tmpDec < 0x100);

            NegFlag(acc);
            ZeroFlag(acc);

            v = false;
            if (!n && (acc & 0x80)>0  && (data & 0x80)>0  && (oldAcc & 0x80)==0) v = true;
            if (!n && (acc & 0x80)==0 && (data & 0x80)==0 && (oldAcc & 0x80)>0 ) v = true;
            if ( n && (acc & 0x80)>0  && (data & 0x80)>0  && (oldAcc & 0x80)==0) v = true;
        } else {
            final int raw = acc - data - (c ? 0 : 1);

            acc = toByte(raw);
            c = (raw >= 0);  // Carry set if no borrow occurred

            NegFlag(acc);
            ZeroFlag(acc);

            // Overflow flag: set if sign bit is incorrect
            v = (((oldAcc ^ data) & (oldAcc ^ acc) & 0x80) != 0);
        }
    }

    public void dey()
    {
        yr--;
        if (yr < 0) {
            yr = 0xff;
        }
        NegFlag(yr);
        ZeroFlag(yr);
    }


    public void iny()
    {
        yr = (short) ((yr + 1) & 0xff);
        NegFlag(yr);
        ZeroFlag(yr);
    }


    public void dex()
    {
        xr--;
        if (xr < 0) {
            xr = 0xff;
        }
        NegFlag(xr);
        ZeroFlag(xr);
    }


    public void inx()
    {
        xr = (short) ((xr + 1) & 0xff);
        NegFlag(xr);
        ZeroFlag(xr);
    }


    public void txa()
    {
        acc = xr;
        NegFlag(acc);
        ZeroFlag(acc);
    }


    public void tax()
    {
        xr = acc;
        NegFlag(xr);
        ZeroFlag(xr);
    }


    public void tya()
    {
        acc = yr;
        NegFlag(acc);
        ZeroFlag(acc);
    }


    public void tay()
    {
        yr = acc;
        NegFlag(yr);
        ZeroFlag(yr);
    }


    public void tsx()
    {
        xr = (short) sp;
        NegFlag(xr);
        ZeroFlag(xr);
    }


    public void txs()
    {
        sp = xr;
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
}
