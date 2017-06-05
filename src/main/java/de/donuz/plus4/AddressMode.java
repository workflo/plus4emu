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
 * $Id: AddressMode.java,v 1.2 2009/06/01 09:05:14 florian Exp $
 */
package de.donuz.plus4;

public enum AddressMode
{
    UNMITTELBAR,
    ZEROPAGE,
    ABSOLUT, 
    INDIZIERTX,
    INDIZIERTY,
    ZEROPAGEX,
    ZEROPAGEY,
    INDIREKTINDIZIERTY,
    INDIZIERTINDIREKTX,
    REL_BRANCH,
    INDIRECT_JUMP,
    NONE,
    UNDEFINED
}
