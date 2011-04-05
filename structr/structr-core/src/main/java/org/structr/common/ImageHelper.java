/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.common;

import com.mortennobel.imagescaling.ResampleOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringUtils;
import org.structr.core.entity.Image;

/**
 *
 * @author axel
 */
public abstract class ImageHelper {

    private static final Logger logger = Logger.getLogger(ImageHelper.class.getName());
    private static Thumbnail tn = new Thumbnail();

    public static class Thumbnail {

        public static final String FORMAT = "png";
        private byte[] bytes;
        private int width;
        private int height;

        public Thumbnail() {
        }

        public Thumbnail(final byte[] bytes) {
            this.bytes = bytes;
        }

        public Thumbnail(final int width, final int height) {
            this.width = width;
            this.height = height;
        }

        public Thumbnail(final byte[] bytes, final int width, final int height) {
            this.bytes = bytes;
            this.width = width;
            this.height = height;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public void setBytes(final byte[] bytes) {
            this.bytes = bytes;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(final int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(final int height) {
            this.height = height;
        }
    }

    public static Thumbnail createThumbnail(final Image originalImage, final int maxWidth, final int maxHeight) {
        return createThumbnail(originalImage, maxWidth, maxHeight, false);
    }

    public static Thumbnail createThumbnail(final Image originalImage, final int maxWidth, final int maxHeight, final boolean crop) {

        //String contentType = (String) originalImage.getProperty(Image.CONTENT_TYPE_KEY);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            // read image
            long start = System.nanoTime();

            BufferedImage source = null;
            try {
                source = ImageIO.read(originalImage.getInputStream());
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Could not read original image {0} ({1})", new Object[]{originalImage.getName(), originalImage.getId()});
            }

            if (source != null) {

                int sourceWidth = source.getWidth();
                int sourceHeight = source.getHeight();

                // Update image dimensions
                originalImage.setWidth(sourceWidth);
                originalImage.setHeight(sourceHeight);

                //float aspectRatio = sourceWidth/sourceHeight;

                float scaleX = 1.0f * sourceWidth / maxWidth;
                float scaleY = 1.0f * sourceHeight / maxHeight;

                float scale;
                if (crop) {
                    scale = Math.min(scaleX, scaleY);
                } else {
                    scale = Math.max(scaleX, scaleY);
                }

//            System.out.println("Source (w,h): " + sourceWidth + ", " + sourceHeight + ", Scale (x,y,res): " + scaleX + ", " + scaleY + ", " + scale);

                // Don't scale up
                if (scale > 1.0000f) {

                    int destWidth = Math.max(3, Math.round(sourceWidth / scale));
                    int destHeight = Math.max(3, Math.round(sourceHeight / scale));

                    tn.setWidth(destWidth);
                    tn.setHeight(destHeight);

//                System.out.println("Dest (w,h): " + destWidth + ", " + destHeight);

                    ResampleOp resampleOp = new ResampleOp(destWidth, destHeight);
                    //resampleOp.setUnsharpenMask(AdvancedResizeOp.UnsharpenMask.Soft);
                    BufferedImage resampled = resampleOp.filter(source, null);

                    BufferedImage result = null;
                    if (crop) {

                        int offsetX = Math.abs(maxWidth - destWidth) / 2;
                        int offsetY = Math.abs(maxHeight - destHeight) / 2;
                        logger.log(Level.INFO, "Offset and Size (x,y,w,h): {0},{1},{2},{3}", new Object[]{offsetX, offsetY, maxWidth, maxHeight});
                        result = resampled.getSubimage(offsetX, offsetY, maxWidth, maxHeight);
                    } else {
                        result = resampled;
                    }

                    ImageIO.write(result, Thumbnail.FORMAT, baos);
                } else {
                    ImageIO.write(source, Thumbnail.FORMAT, baos);
                }
            } else {
                logger.log(Level.WARNING, "Thumbnail could not be created");
                return null;
            }

            long end = System.nanoTime();
            long time = (end - start) / 1000000;
            logger.log(Level.FINE, "Thumbnail created. Reading, scaling and writing took {0} ms", time);

            tn.setBytes(baos.toByteArray());
            return tn;

        } catch (Throwable t) {
            logger.log(Level.WARNING, "Error creating thumbnail");
        }

        return null;
    }

    /**
     * Let ImageIO read and write a JPEG image. This should normalize all types of weird
     * image sub formats, e.g. when extracting images from a flash file.
     *
     * Some images can not be read by ImageIO (or the browser) because they
     * have an JPEG EOI and SOI marker at the beginning of the file.
     *
     * This method detects and removes the bytes, so that the image
     * can be read again.
     *
     * @param original
     * @return normalized image
     */
    public static byte[] normalizeJpegImage(final byte[] original) {

        if (original == null) {
            return new byte[]{};
        }

        ByteArrayInputStream in = new ByteArrayInputStream(original);

        // If JPEG image starts with ff d9 ffd8, strip this sequence from the beginning

        // FF D9 = EOI (end of image)
        // FF D8 = SOI (start of image)

        if (original[0] == (byte) 0xff && original[1] == (byte) 0xd9 && original[2] == (byte) 0xff && original[3] == (byte) 0xd8) {
            in.skip(4);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BufferedImage source;

        try {

            source = ImageIO.read(in);

            // If ImageIO cannot read it, return original
            if (source == null) {
                return original;
            }

            ImageIO.write(source, "jpeg", out);

        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }

        return out.toByteArray();
    }

    /**
     * Check if url points to an image by extension
     *
     * TODO: Improve method to check file type by peeping at the
     * content
     *
     * @param urlString
     * @return
     */
    public static boolean isImageType(final String urlString) {

        if (urlString == null || StringUtils.isBlank(urlString)) {
            return false;
        }
        String extension = urlString.toLowerCase().substring(urlString.lastIndexOf(".") + 1);
        String[] imageExtensions = {"png", "gif", "jpg", "jpeg", "bmp", "tif", "tiff"};

        for (String ext : imageExtensions) {
            if (ext.equals(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if url points to an Flash object by extension
     *
     * TODO: Improve method to check file type by peeping at the
     * content
     *
     * @param urlString
     * @return
     */
    public static boolean isSwfType(final String urlString) {

        if (urlString == null || StringUtils.isBlank(urlString)) {
            return false;
        }

        String extension = urlString.toLowerCase().substring(urlString.lastIndexOf(".") + 1);
        String[] imageExtensions = {"swf"};

        for (String ext : imageExtensions) {
            if (ext.equals(extension)) {
                return true;
            }
        }
        return false;
    }
}
