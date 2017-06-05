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
 * $Id: RomListing.java,v 1.3 2009/06/13 18:09:23 florian Exp $
 */
package de.donuz.plus4;

import java.util.*;

public class RomListing
{
    private final static Map<Integer, String> addresses = new HashMap<Integer, String>();
    
    static {
        // ZeroPage:
        addresses.put(0xca, "Cursor X position");
        
        // Extended ZeroPage:
        addresses.put(0x7e7, "Left text window border");
        
        // ROM:
        addresses.put(0x8000, "BASIC-Kaltstart");
        

        addresses.put(0xcf11, "Stop-Taste ueberpruefen");
        
        addresses.put(0xd84e, "EDITOR RESET");
        addresses.put(0xd88b, "CLEAR SCREEN");
        addresses.put(0xd89a, "HOME");
        addresses.put(0xd8c1, "Code aus Tastaturpuffer holen");
        addresses.put(0xd8ea, "Eingabe vom Bildschirm");
        addresses.put(0xd965, "Zeichen vom Bildschirm holen");
        addresses.put(0xd9ba, "Anführungszeichenmodus setzen/löschen");
        addresses.put(0xd9c7, "Abschluss von PRINT");
        addresses.put(0xda21, "SCROLL UP");
        addresses.put(0xda3d, "Bildschirmzeile umkopieren");
        addresses.put(0xda5e, "Leerzeile einfuegen");
        addresses.put(0xdb11, "Tastaturabfrage");

        addresses.put(0xdb70, "Dekoder-Abfrage");
        addresses.put(0xdc49, "PRINT");
        addresses.put(0xdc8c, "RETURN CODE");
        addresses.put(0xdc9b, "ESC-O (Flags löschen)");
        
        addresses.put(0xdcfa, "CURSOR RIHGT");
        addresses.put(0xdd00, "CURSOR DOWN");
        addresses.put(0xdd0d, "CURSOR UP");
        addresses.put(0xdd1c, "CURSOR LEFT");
        addresses.put(0xdd27, "TEXT MODE");
        addresses.put(0xdef6, "ESC-V Scroll Up");
        addresses.put(0xdf04, "ESC-W Scroll Down");
        addresses.put(0xdf1d, "ESC-L Scrolling freigeben");
        addresses.put(0xdf20, "ESC-M Scrolling sperren");
        addresses.put(0xdf26, "ESC-C Auto-Insert aus");
        addresses.put(0xdf2f, "Zeichen vom Bildschirm holen");
        addresses.put(0xdf39, "GETBIT");
        addresses.put(0xdf46, "PUTBIT");
        addresses.put(0xdf4a, "CLRBIT");
        addresses.put(0xdf66, "Erzeugt Bitposition");
        addresses.put(0xdf95, "ESC-K Setzt Cursor ans Zeilenende");
        addresses.put(0xdfd4, "CURSOR LEFT");

        addresses.put(0xeb46, "RS-232-Arbeitsbereich initialisieren");
        
        addresses.put(0xedea, "Teile von IOINIT");

        addresses.put(0xf2a4, "NMI und Start");
        addresses.put(0xf2ce, "RESTOR");
        addresses.put(0xf2d3, "VECTOR");
        addresses.put(0xf30b, "IOINIT");
        addresses.put(0xf352, "RAMTAS");
        
        addresses.put(0xf445, "MONITOR");
        

        addresses.put(0xfbcb, "Stop-Taste pruefen");
        addresses.put(0xfbd8, "Meldung ausgeben");
        addresses.put(0xfc19, "IOBASE");
        addresses.put(0xfc1e, "Modul-Reset");
        addresses.put(0xfc59, "Module initialisieren");
        addresses.put(0xfc7f, "Modul-Zugriff");
        addresses.put(0xfc89, "Modul-Aufruf");
        addresses.put(0xfcb3, "PULS");
        addresses.put(0xfcc9, "Modul einschalten und ausführen");
        
        addresses.put(0xfff6, "SYSTEM START");
    }
    
    public static String getAddressDescription(int addr)
    {
        final String desc = addresses.get(addr);
        
        if (desc != null) {
            return desc;
        } else {
            return "";
        }
    }
}
