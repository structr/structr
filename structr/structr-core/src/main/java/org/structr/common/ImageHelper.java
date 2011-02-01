/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.common;

import com.mortennobel.imagescaling.ResampleOp;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.structr.core.entity.Image;

/**
 *
 * @author axel
 */
public abstract class ImageHelper {

    private static final Logger logger = Logger.getLogger(ImageHelper.class.getName());
    private static Thumbnail tn = new Thumbnail();

    public static class Thumbnail {

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

    public static Thumbnail getThumbnailByteArray(final Image originalImage, final int maxWidth, final int maxHeight) {

        //String contentType = (String) originalImage.getProperty(Image.CONTENT_TYPE_KEY);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            // read image
            long start = System.nanoTime();

            BufferedImage source = ImageIO.read(originalImage.getInputStream());

            if (source != null) {

                int sourceWidth = source.getWidth();
                int sourceHeight = source.getHeight();

                //float aspectRatio = sourceWidth/sourceHeight;

                float scaleX = 1.0f * sourceWidth / maxWidth;
                float scaleY = 1.0f * sourceHeight / maxHeight;

                float scale = Math.max(scaleX, scaleY);

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
                    BufferedImage dest = resampleOp.filter(source, null);

//            BufferedImage dest = new BufferedImage(destWidth, destHeight, BufferedImage.TYPE_INT_RGB);
//
//            Graphics2D g = dest.createGraphics();
//
//            // set rendering hints..
//            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
//            g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
//            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
//
//            AffineTransform transform = AffineTransform.getScaleInstance(1.0/scale, 1.0/scale);
//            g.drawRenderedImage(source, transform);
//
                    //BufferedImage thumbnail = Thumbnails.of(originalImage).size(destHeight, destWidth).asBufferedImage();
//            Thumbnails.of(source).alphaInterpolation(AlphaInterpolation.QUALITY).size(destHeight, destWidth).outputFormat("png").toOutputStream(baos);

                    ImageIO.write(dest, "png", baos);
                } else {
                    ImageIO.write(source, "png", baos);
                }
            } else {
                logger.log(Level.SEVERE, "Thumbnail could not be created");
                return null;
            }

            long end = System.nanoTime();
            long time = (end - start) / 1000000;
            logger.log(Level.FINE, "Thumbnail created. Reading, scaling and writing took {0} ms", time);

            tn.setBytes(baos.toByteArray());
            return tn;

        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Error creating thumbnail", t);
        }

        return null;
    }
}
