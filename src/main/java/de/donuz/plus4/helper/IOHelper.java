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
 * $Id: IOHelper.java,v 1.2 2009/05/31 16:21:43 florian Exp $
 */
package de.donuz.plus4.helper;

import java.io.*;

public class IOHelper
{
    /**
     * Read out an exact amount of byte from a stream.
     * InputStream.read(byte[]) does not guarentee to read as many bytes as there are in
     * the stream. This method implements magic to read all of the bytes or fail. The stream is
     * not closed.
     *
     * @param howmany number of bytes to read
     * @return null when the Stream failed to deliver howmany bytes
     *
     */
    public final static void ReadBytes(short[] buffer, InputStream in, int howmany) throws IOException
    {
        for (int idx = 0; idx < howmany; idx++) {
            buffer[idx] = (short) in.read();
        }
    }

}
