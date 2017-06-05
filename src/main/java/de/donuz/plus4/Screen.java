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
 * $Id: Screen.java,v 1.10 2009/06/07 12:24:14 florian Exp $
 */
package de.donuz.plus4;

import java.awt.*;

public class Screen extends Canvas
{
    /** Comment for <code>serialVersionUID</code> */
    private static final long serialVersionUID = 1802538017667471105L;

    private final int sizeFactor = 3;
  
    private final Color[] palette = new Color[128];

    private final short[][] pixels = new short[Plus4.SCREENHEIGHT][Plus4.SCREENWIDTH];

    
    public Screen()
    {
        setSize(Plus4.SCREENWIDTH*sizeFactor, Plus4.SCREENHEIGHT*sizeFactor);
        loadPalette();
    }
    
    
    private void loadPalette() 
    {        
        palette[0x00] = new Color(0, 0, 0);
        palette[0x01] = new Color(44, 44, 44);
        palette[0x02] = new Color(98, 19, 7);
        palette[0x03] = new Color(0, 66, 67);
        palette[0x04] = new Color(81, 3, 120);
        palette[0x05] = new Color(0, 78, 0);
        palette[0x06] = new Color(39, 24, 142);
        palette[0x07] = new Color(48, 62, 0);
        palette[0x08] = new Color(88, 33, 0);
        palette[0x09] = new Color(70, 48, 0);
        palette[0x0A] = new Color(36, 68, 0);
        palette[0x0B] = new Color(99, 4, 72);
        palette[0x0C] = new Color(0, 78, 12);
        palette[0x0D] = new Color(14, 39, 132);
        palette[0x0E] = new Color(51, 17, 142);
        palette[0x0F] = new Color(24, 72, 0);
        palette[0x10] = new Color(0, 0, 0);
        palette[0x11] = new Color(59, 59, 59);
        palette[0x12] = new Color(112, 36, 25);
        palette[0x13] = new Color(0, 80, 90);
        palette[0x14] = new Color(96, 22, 133);
        palette[0x15] = new Color(18, 93, 0);
        palette[0x16] = new Color(54, 40, 155);
        palette[0x17] = new Color(63, 76, 0);
        palette[0x18] = new Color(102, 49, 0);
        palette[0x19] = new Color(85, 63, 0);
        palette[0x1A] = new Color(52, 82, 0);
        palette[0x1B] = new Color(113, 22, 86);
        palette[0x1C] = new Color(0, 92, 29);
        palette[0x1D] = new Color(31, 54, 145);
        palette[0x1E] = new Color(66, 34, 155);
        palette[0x1F] = new Color(40, 87, 0);
        palette[0x20] = new Color(0, 0, 0);
        palette[0x21] = new Color(66, 66, 66);
        palette[0x22] = new Color(119, 44, 33);
        palette[0x23] = new Color(5, 88, 97);
        palette[0x24] = new Color(102, 30, 140);
        palette[0x25] = new Color(27, 100, 0);
        palette[0x26] = new Color(62, 48, 162);
        palette[0x27] = new Color(71, 84, 0);
        
        palette[0x28] = new Color(109, 57, 0);
        palette[0x29] = new Color(92, 71, 0);
        palette[0x2A] = new Color(59, 89, 0);
        palette[0x2B] = new Color(119, 31, 93);
        palette[0x2C] = new Color(4, 99, 37);
        palette[0x2D] = new Color(39, 62, 152);
        palette[0x2E] = new Color(73, 42, 161);
        palette[0x2F] = new Color(48, 94, 0);
        palette[0x30] = new Color(0, 0, 0);
        palette[0x31] = new Color(81, 81, 81);
        palette[0x32] = new Color(132, 59, 49);
        palette[0x33] = new Color(23, 101, 111);
        palette[0x34] = new Color(116, 46, 153);
        palette[0x35] = new Color(43, 113, 0);
        palette[0x36] = new Color(76, 63, 175);
        palette[0x37] = new Color(85, 98, 0);
        palette[0x38] = new Color(122, 71, 9);
        palette[0x39] = new Color(106, 85, 0);
        palette[0x3A] = new Color(74, 103, 0);
        palette[0x3B] = new Color(133, 47, 107);
        palette[0x3C] = new Color(23, 113, 53);
        palette[0x3D] = new Color(54, 76, 165);
        palette[0x3E] = new Color(87, 57, 174);
        palette[0x3F] = new Color(63, 107, 0);
        palette[0x40] = new Color(0, 0, 0);
        palette[0x41] = new Color(122, 122, 122);
        palette[0x42] = new Color(172, 102, 92);
        palette[0x43] = new Color(70, 142, 151);
        palette[0x44] = new Color(156, 90, 192);
        palette[0x45] = new Color(87, 153, 46);
        palette[0x46] = new Color(118, 106, 213);
        palette[0x47] = new Color(126, 138, 19);
        palette[0x48] = new Color(162, 113, 58);
        palette[0x49] = new Color(146, 126, 32);
        palette[0x4A] = new Color(116, 143, 20);
        palette[0x4B] = new Color(172, 90, 147);
        palette[0x4C] = new Color(69, 153, 96);
        palette[0x4D] = new Color(98, 118, 203);
        palette[0x4E] = new Color(128, 100, 212);
        palette[0x4F] = new Color(106, 148, 25);
        
        palette[0x50] = new Color(0, 0, 0);
        palette[0x51] = new Color(149, 149, 149);
        palette[0x52] = new Color(197, 129, 120);
        palette[0x53] = new Color(98, 168, 177);
        palette[0x54] = new Color(182, 117, 217);
        palette[0x55] = new Color(115, 179, 76);
        palette[0x56] = new Color(145, 133, 237);
        palette[0x57] = new Color(153, 164, 51);
        palette[0x58] = new Color(187, 140, 87);
        palette[0x59] = new Color(172, 153, 62);
        palette[0x5A] = new Color(143, 170, 52);
        palette[0x5B] = new Color(198, 118, 173);
        palette[0x5C] = new Color(98, 179, 123);
        palette[0x5D] = new Color(125, 145, 228);
        palette[0x5E] = new Color(155, 128, 237);
        palette[0x5F] = new Color(133, 174, 56);
        palette[0x60] = new Color(0, 0, 0);
        palette[0x61] = new Color(175, 175, 175);
        palette[0x62] = new Color(222, 155, 147);
        palette[0x63] = new Color(125, 194, 202);
        palette[0x64] = new Color(207, 144, 242);
        palette[0x65] = new Color(141, 205, 104);
        palette[0x66] = new Color(171, 159, 255);
        palette[0x67] = new Color(179, 190, 81);
        palette[0x68] = new Color(213, 166, 115);
        palette[0x69] = new Color(198, 179, 91);
        palette[0x6A] = new Color(169, 195, 81);
        palette[0x6B] = new Color(223, 145, 199);
        palette[0x6C] = new Color(125, 204, 150);
        palette[0x6D] = new Color(151, 171, 253);
        palette[0x6E] = new Color(181, 154, 255);
        palette[0x6F] = new Color(159, 199, 85);
        palette[0x70] = new Color(0, 0, 0);
        palette[0x71] = new Color(225, 225, 225);
        palette[0x72] = new Color(255, 207, 198);
        palette[0x73] = new Color(178, 244, 252);
        palette[0x74] = new Color(255, 196, 255);
        palette[0x75] = new Color(193, 154, 157);
        palette[0x76] = new Color(221, 210, 255);
        palette[0x77] = new Color(229, 240, 136);
        
        palette[0x78] = new Color(255, 217, 168);
        palette[0x79] = new Color(247, 229, 145);
        palette[0x7A] = new Color(219, 245, 136);
        palette[0x7B] = new Color(255, 196, 249);
        palette[0x7C] = new Color(177, 254, 201);
        palette[0x7D] = new Color(203, 221, 225);
        palette[0x7E] = new Color(231, 205, 255);
        palette[0x7F] = new Color(210, 249, 140);
    }


    public void paint(Graphics g)
    {
        for (int y = 0; y < Plus4.SCREENHEIGHT; y++) {
            for (int x = 0; x < Plus4.SCREENWIDTH; x++) {
                g.setColor(palette[pixels[y][x] & 0x7f]);
                g.fillRect(x*sizeFactor, y*sizeFactor, sizeFactor, sizeFactor);
            }
        }
    }


    public void updateLine(int y, short[] line)
    {
        System.arraycopy(line, 0, pixels[y], 0, Plus4.SCREENWIDTH);
        repaint();
    }
}
