/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.node;

import com.flagstone.transform.Movie;
import com.flagstone.transform.MovieTag;
import com.flagstone.transform.image.DefineImage;
import com.flagstone.transform.image.DefineImage2;
import com.flagstone.transform.image.DefineJPEGImage;
import com.flagstone.transform.image.DefineJPEGImage2;
import com.flagstone.transform.image.DefineJPEGImage3;
import com.flagstone.transform.image.DefineJPEGImage4;
import com.flagstone.transform.image.ImageTag;
import com.flagstone.transform.util.image.BufferedImageEncoder;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import javax.imageio.ImageIO;
import org.apache.commons.io.FileUtils;
import org.structr.common.ImageHelper;
import org.structr.common.Path;
import org.structr.common.RelType;
import org.structr.core.Services;
import org.structr.core.UnsupportedArgumentError;
import org.structr.core.entity.Image;
import org.structr.core.entity.StructrNode;
import org.structr.core.entity.User;

/**
 * Save images from a Adobe Flash file, downloadable at the given URL.
 * The images are saved as sub-nodes of the given parent node.
 *
 * @author amorgner
 */
public class SaveImagesFromFlashUrl extends NodeServiceCommand {

    private static final Logger logger = Logger.getLogger(SaveImagesFromFlashUrl.class.getName());

    /**
     * Takes three parameters.
     * 
     * <ul>
     * <li>1: User
     * <li>2: URL string
     * <li>3: Parent node
     * </ul>
     *
     * @param parameters
     * @return
     */
    @Override
    public Object execute(Object... parameters) {

        User user = null;
        String urlString = null;
        StructrNode parentNode = null;

        List<Image> result = new LinkedList<Image>();

        switch (parameters.length) {

            case 3:

                if (parameters[0] instanceof User) {
                    user = (User) parameters[0];
                }
                if (parameters[1] instanceof String) {
                    urlString = (String) parameters[1];
                }
                if (parameters[2] instanceof StructrNode) {
                    parentNode = (StructrNode) parameters[2];
                }
                break;

            default:

                throw new UnsupportedArgumentError("Wrong number of arguments");

        }

        if (user != null && urlString != null && parentNode != null) {

            result = saveFlashImagesFromUrl(user, urlString, parentNode);

        }

        return result;

    }

    /**
     * Extract images from flash file and save them as image nodes
     * 
     * @param imageNode
     * @return image list
     */
    private List<Image> saveFlashImagesFromUrl(final User user, final String urlString, final StructrNode parentNode) {

        String flashObjectName = urlString.substring(urlString.lastIndexOf("/") + 1);
        String tmpFilePath = Services.getFilePath(Path.Temp, "_ " + flashObjectName + "_" + System.nanoTime());

        File flashFile = new File(tmpFilePath);
        URL flashUrl = null;

        try {
            flashUrl = new URL(urlString);
        } catch (MalformedURLException ex) {
            logger.log(Level.SEVERE, "Malformed URL: {0}", ex);
        }

        List<Image> result = new LinkedList<Image>();

        if (flashUrl != null) {

            try {
                // Download Flash object to temporary file
                FileUtils.copyURLToFile(flashUrl, flashFile);

                Movie movie = new Movie();
                try {

                    movie.decodeFromFile(flashFile);

                    List<MovieTag> flashObjects = movie.getObjects();
                    BufferedImageEncoder enc = new BufferedImageEncoder();
                    for (MovieTag t : flashObjects) {

                        if (t instanceof ImageTag) {

                            ImageTag image = (ImageTag) t;
                            enc.setImage(image);

                            String format = null;

                            String fileExtension, contentType;

                            BufferedImage bufferedImage = null;
                            byte[] imageAsByteArray = null;

                            if (t instanceof DefineJPEGImage) {

                                contentType = "image/jpeg";
                                fileExtension = ".jpg";
                                imageAsByteArray = ((DefineJPEGImage) t).getImage();
                                format = DefineJPEGImage.class.getSimpleName();

                            } else if (t instanceof DefineJPEGImage2) {

                                image = (DefineJPEGImage2) t;
                                contentType = "image/jpeg";
                                fileExtension = ".jpg";
                                imageAsByteArray = ((DefineJPEGImage2) t).getImage();
                                format = DefineJPEGImage2.class.getSimpleName();

                            } else if (t instanceof DefineJPEGImage3) {

                                image = (DefineJPEGImage3) t;
                                contentType = "image/jpeg";
                                fileExtension = ".jpg";
                                imageAsByteArray = ((DefineJPEGImage3) t).getImage();
                                format = DefineJPEGImage3.class.getSimpleName();

                            } else if (t instanceof DefineJPEGImage4) {

                                image = (DefineJPEGImage4) t;
                                contentType = "image/jpeg";
                                fileExtension = ".jpg";
                                imageAsByteArray = ((DefineJPEGImage4) t).getImage();
                                format = DefineJPEGImage4.class.getSimpleName();

                            } else if (t instanceof DefineImage) {

                                image = (DefineImage) t;
                                contentType = "image/png";
                                fileExtension = ".png";
                                enc.setImage((DefineImage) t);
                                bufferedImage = enc.getBufferedImage();
                                format = DefineImage.class.getSimpleName();

                            } else if (t instanceof DefineImage2) {

                                image = (DefineImage2) t;
                                contentType = "image/png";
                                fileExtension = ".png";
                                enc.setImage((DefineImage2) t);
                                bufferedImage = enc.getBufferedImage();
                                format = DefineImage2.class.getSimpleName();

                            } else {
                                logger.log(Level.INFO, "Unsupported image");
                                continue; // Next image
                            }

                            int width = image.getWidth();
                            int height = image.getHeight();
                            int id = image.getIdentifier();

                            logger.log(Level.INFO, "Found image (w, h, id, format): {0}, {1}, {2}, {3}", new Object[]{width, height, id, format});

                            String name = flashObjectName + "_" + id + "_" + width + "x" + height + fileExtension;

                            // Create new image node
                            Image newImageNode = (Image) Services.command(CreateNodeCommand.class).execute(user,
                                    new NodeAttribute(StructrNode.TYPE_KEY, Image.class.getSimpleName()),
                                    new NodeAttribute(StructrNode.NAME_KEY, name),
                                    new NodeAttribute(Image.WIDTH_KEY, width),
                                    new NodeAttribute(Image.HEIGHT_KEY, height),
                                    new NodeAttribute(Image.CONTENT_TYPE_KEY, contentType),
                                    true);  // Update index

                            // Establish HAS_CHILD relationship from parent node
                            Services.command(CreateRelationshipCommand.class).execute(parentNode, newImageNode, RelType.HAS_CHILD);

                            String relativeFilePath = newImageNode.getId() + "_" + System.currentTimeMillis();


                            String path = Services.getFilePath(Path.Files, relativeFilePath);
                            File imageFile = new File(path);

                            if (imageAsByteArray != null) {

                                writeBufferToFile(imageFile, ImageHelper.normalizeJpegImage(imageAsByteArray));

                            } else if (bufferedImage != null) {

                                ImageIO.write(bufferedImage, "png", imageFile);
                                
                            }

                            newImageNode.setRelativeFilePath(relativeFilePath);

                            logger.log(Level.INFO, "Image file for image node {0} ({1}) written to disk", new Object[]{name, newImageNode.getId()});
                            flashFile.delete();

                            result.add(newImageNode);

                        }
                    }

                } catch (DataFormatException ex) {
                    logger.log(Level.SEVERE, "Error while decoding Flash file: {0}", ex);
                    flashFile.delete();
                }

            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Download of Flash file failed: {0}", ex);
                flashFile.delete();
            }
        }

        return result;

    }

    private void writeBufferToFile(final File imageFile, final byte[] buffer) throws IOException {
        FileUtils.writeByteArrayToFile(imageFile, buffer);
    }
}
