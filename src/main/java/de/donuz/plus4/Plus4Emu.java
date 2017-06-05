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
 * $Id: Plus4Emu.java,v 1.9 2009/06/07 15:06:57 florian Exp $
 */
package de.donuz.plus4;

import java.io.*;

import javax.swing.*;

public class Plus4Emu
{
    public static void main(String[] args) throws IOException
    {
        final JFrame mainFrame = new JFrame("Plus/4-Emulator");
        final Screen screen = new Screen();
        final Plus4 p4 = new Plus4(screen);
        mainFrame.add(screen);
        mainFrame.setSize(Plus4.SCREENWIDTH*3, Plus4.SCREENHEIGHT*3);
        mainFrame.setVisible(true);
        
        final Thread t = new Thread(new Runnable() {
            public void run()
            {
                p4.hardReset();
                p4.run();
            }});
        t.setDaemon(true);
        t.start();
//        
//        short d1 = 0x0;
//        short a1 = 0x7fff;

//        System.out.println("d1=" + Plus4.toByte(d1));
//        d1--;
//        System.out.println("d1=" + Plus4.toByte(d1));
//        d1--;
//        System.out.println("d1=" + Plus4.toByte(d1));
//        d1 = 0xff;
//        System.out.println("d1=" + Plus4.toByte(d1));
//        d1++;
//        d1++;
//        System.out.println("d1=" + Plus4.toByte(d1));
//        
//        a1++;
//        d1++;
//        
//        System.out.println("d1=" + d1);
//        System.out.println("a1=" + a1);
//        System.out.println("a=" + (80000 + d1));
//        System.out.println("a=" + (80000 + d1));
    }
}
