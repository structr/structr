/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.node;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.structr.common.FileHelper;
import org.structr.common.Path;
import org.structr.core.Services;
import org.structr.core.UnsupportedArgumentError;
import org.structr.core.entity.Image;

/**
 * Save image by downloading it from its URL.
 *
 * If the image file already exists, this command will refresh it.
 *
 * @author amorgner
 */
public class ExtractAndSetImageDimensionsAndFormat extends NodeServiceCommand {

    private static final Logger logger = Logger.getLogger(ExtractAndSetImageDimensionsAndFormat.class.getName());

    /**
     *
     * @param parameters
     * @return
     */
    @Override
    public Object execute(Object... parameters) {

        List<Image> images = new LinkedList<Image>();

        switch (parameters.length) {

            case 1:

                if (parameters[0] instanceof Image) {
                    images.add((Image) parameters[0]);
                    break;
                }
                if (parameters[0] instanceof List) {
                    images.addAll((List) parameters[0]);
                    break;
                }

            default:
                throw new UnsupportedArgumentError("Wrong number of arguments");
        }

        extractAndSetImageDimensionsAndFormat(images);

        return images.size();
    }

    private void extractAndSetImageDimensionsAndFormat(List<Image> images) {

        if (images != null) {

            for (Image imageNode : images) {

                String relativeFilePath = imageNode.getRelativeFilePath();

                if (relativeFilePath != null) {

                    String filePath = Services.getFilePath(Path.Files, relativeFilePath);

                    int width = 0;
                    int height = 0;
                    String format = null;

                    try {

                        BufferedImage img = ImageIO.read(imageNode.getInputStream());

                        try {

                            width = img.getWidth();
                            height = img.getHeight();

                            format = FileHelper.getContentMimeType(new File(filePath));

                            imageNode.setWidth(width);
                            imageNode.setHeight(height);

                            if (format != null) {
                                imageNode.setContentType(format);
                            }

                        } catch (Throwable ignore) {
                            logger.log(Level.SEVERE, "Error while extracting image dimensions or type from {0}", filePath);
                        } finally {
                        }
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Could not read image {0}", filePath);
                    }

                    logger.log(Level.FINE, "Extracted dimensions and format from image node {0} (x,y, format): {1}, {2}, {3}", new Object[]{imageNode.getId(), width, height, format});

                }
            }

        }
    }
}
