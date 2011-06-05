/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.common.xpath;

/**
 *
 * Encode a path expression to work with JXPath
 *
 * @author amorgner
 */
public class XPathEncoder {

    public static String encode(String path) {

        String encodedPath = path
                .replaceAll(" ", "0x20")
                .replaceAll("\\[", "0x5B")
                .replaceAll("]", "0x5D")
                .replaceAll(":", "0x3A")
                .replaceAll("\\$", "0x24")
                .replaceAll("%", "0x25")
                .replaceAll("<", "0x3C")
                .replaceAll(">", "0x3E")
                .replaceAll("\\(", "0x28")
                .replaceAll("\\)", "0x29")
                .replaceAll("@", "0x40")
                .replaceAll("'", "0x27")
                .replaceAll("\\+", "0x2B")
                .replaceAll("-", "0x2D")
                .replaceAll("=", "0x3D")
                .replaceAll(",", "0x2C")
                .replaceAll("#", "0x23")
                .replaceAll("\\*", "0x2A")
                .replaceAll("!", "0x21")
                .replaceAll("\\.", "0x2E");

        return encodedPath;
    }
}
