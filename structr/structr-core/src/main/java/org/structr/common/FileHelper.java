/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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
package org.structr.common;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicMatch;

/**
 * Utility class
 *
 * @author axel
 */
public class FileHelper {

    private static final Logger logger = Logger.getLogger(FileHelper.class.getName());

    private static final String UNKNOWN_MIME_TYPE = "application/octet-stream";

    /**
     * Return mime type of given file
     *
     * @param file
     * @param ext
     * @return
     */
    public static String getContentMimeType(final File file) {

        MagicMatch match;
        try {
            match = Magic.getMagicMatch(file, false, true);
            return match.getMimeType();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not determine content type");
        }

        return UNKNOWN_MIME_TYPE;

    }

    /**
     * Return mime type of given byte array.
     *
     * Use on streams.
     *
     * @param bytes
     * @return
     */
    public static String getContentMimeType(final byte[] bytes) {

        MagicMatch match;
        try {
            match = Magic.getMagicMatch(bytes, true);
            return match.getMimeType();
        } catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
        }

        return UNKNOWN_MIME_TYPE;

    }

    /**
     * Return mime type of given file
     *
     * @param file
     * @param ext
     * @return
     */
    public static String[] getContentMimeTypeAndExtension(final File file) {

        MagicMatch match;
        try {
            match = Magic.getMagicMatch(file, false, true);
            return new String[]{match.getMimeType(), match.getExtension()};
        } catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
        }

        return new String[]{UNKNOWN_MIME_TYPE, ".bin"};

    }

    /**
     * Return mime type of given byte array.
     *
     * Use on streams.
     *
     * @param bytes
     * @return
     */
    public static String[] getContentMimeTypeAndExtension(final byte[] bytes) {

        MagicMatch match;
        try {
            match = Magic.getMagicMatch(bytes, true);
            return new String[]{match.getMimeType(), match.getExtension()};
        } catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
        }

        return new String[]{UNKNOWN_MIME_TYPE, ".bin"};

    }
}
