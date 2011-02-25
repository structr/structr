/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.node;

import java.io.File;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.structr.common.Path;
import org.structr.common.RelType;
import org.structr.core.Services;
import org.structr.core.UnsupportedArgumentError;
import org.structr.core.entity.Image;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 * Save image by downloading it from its URL.
 *
 * If the image file already exists, this command will refresh it.
 *
 * @author amorgner
 */
public class SaveImageFromUrl extends NodeServiceCommand {

    private static final Logger logger = Logger.getLogger(SaveImageFromUrl.class.getName());

    /**
     * Takes either one or three parameters.
     * 
     * <ul>
     * <li>1: Image node
     * </ul>
     * 
     * <ul>
     * <li>1: User
     * <li>2: URL string
     * <li>3: Parent node
     * </ul>
     * 
     *
     * @param parameters
     * @return
     */
    @Override
    public Object execute(Object... parameters) {

        User user = null;
        String urlString = null;
        AbstractNode parentNode = null;

        Image imageNode = null;

        switch (parameters.length) {

            case 1:

                if (parameters[0] instanceof Image) {
                    imageNode = (Image) parameters[0];
                }
                break;

            case 3:

                if (parameters[0] instanceof User) {
                    user = (User) parameters[0];
                }
                if (parameters[1] instanceof String) {
                    urlString = (String) parameters[1];
                }
                if (parameters[2] instanceof AbstractNode) {
                    parentNode = (AbstractNode) parameters[2];
                }
                break;

            default:
                throw new UnsupportedArgumentError("Wrong number of arguments");
        }

        if (imageNode != null) {

            refreshImageFromUrl(imageNode);
            
        } else if (user != null && urlString != null && parentNode != null) {

            // Create new image node first
            Image newImageNode = (Image) Services.command(CreateNodeCommand.class).execute(user,
                    new NodeAttribute(AbstractNode.TYPE_KEY, Image.class.getSimpleName()),
                    new NodeAttribute(Image.URL_KEY, urlString),
                    true);  // Update index

            Services.command(CreateRelationshipCommand.class).execute(parentNode, newImageNode, RelType.HAS_CHILD);

            // Then save image from URL
            refreshImageFromUrl(newImageNode);

            return newImageNode;

        }

        return null;

    }

    /**
     * Refresh image file of an existing image node
     * 
     * @param imageNode
     */
    private void refreshImageFromUrl(final Image imageNode) {

        if (imageNode != null) {

            String url = imageNode.getUrl();
            String relativeFilePath = imageNode.getRelativeFilePath();
            String imageName = url.substring(url.lastIndexOf("/") + 1);

            imageNode.setName(imageName);

            if (relativeFilePath == null) {
                relativeFilePath = imageNode.getId() + "_" + System.currentTimeMillis();
            }
            
            String path = Services.getFilePath(Path.Files, relativeFilePath);

            if (StringUtils.isNotBlank(url)) {
                try {

                    File imageFile = new File(path);
                    URL imageUrl = new URL(url);

                    // copy url to file
                    FileUtils.copyURLToFile(imageUrl, imageFile);
                    imageNode.setRelativeFilePath(relativeFilePath);

                    logger.log(Level.INFO, "Image {0} ({1}) saved from URL {2}", new Object[]{imageNode.getName(), imageNode.getId(), url});

                } catch (Throwable t) {
                    logger.log(Level.SEVERE, "Error while saving image from URL {0}", url);
                    t.printStackTrace(System.err);
                }
            }

        }
    }
}
