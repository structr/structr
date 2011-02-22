/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
            logger.log(Level.SEVERE, null, e);
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
