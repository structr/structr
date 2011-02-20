package org.structr.core.entity;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.neo4j.graphdb.Direction;
import org.structr.common.ImageHelper;
import org.structr.common.ImageHelper.Thumbnail;
import org.structr.common.RelType;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

/**
 * 
 * @author amorgner
 * 
 */
public class Image extends File {

    private final static String ICON_SRC = "/images/image.png";
    private static final Logger logger = Logger.getLogger(Image.class.getName());

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }
    public final static String WIDTH_KEY = "width";
    public final static String HEIGHT_KEY = "height";
    // Cached list with relationships to thumbnails
    private List<StructrRelationship> thumbnailRelationships = null;

    public int getWidth() {
        return getIntProperty(WIDTH_KEY);
    }

    public int getHeight() {
        return getIntProperty(HEIGHT_KEY);
    }

    public void setWidth(int width) {
        setProperty(WIDTH_KEY, width);
    }

    public void setHeight(int height) {
        setProperty(HEIGHT_KEY, height);
    }

    @Override
    public void renderView(StringBuilder out, final StructrNode startNode,
            final String editUrl, final Long editNodeId, final User user) {

        if (editNodeId != null && getId() == editNodeId.longValue()) {

            renderEditFrame(out, editUrl);

        } else {

            String imageUrl = null;

            if (getUrl() == null) {
                imageUrl = getNodePath(user, startNode);
            } else {
                imageUrl = getUrl();
            }

            // FIXME: title shoud be rendered dependent of locale
            if (isVisible()) {
                //out.append("<img src=\"").append(getNodeURL(renderMode, contextPath)).append("\" title=\"").append(getTitle()).append("\" alt=\"").append(getTitle()).append("\" width=\"").append(getWidth()).append("\" height=\"").append(getHeight()).append("\">");
                out.append("<img src=\"").append(imageUrl).append("\" title=\"").append(getTitle()).append("\" alt=\"").append(getTitle()).append("\" width=\"").append(getWidth()).append("\" height=\"").append(getHeight()).append("\">");
            }
        }
    }

    /**
     * Stream content directly to output.
     *
     * @param filesPath
     * @param out
     */
    @Override
    public void renderDirect(OutputStream out, final StructrNode startNode,
            final String editUrl, final Long editNodeId, final User user) {

        if (isVisible()) {
            super.renderDirect(out, startNode, editUrl, editNodeId, user);
        }
    }
//
//    /**
//     * Get thumbnail image of this image
//     *
//     * If no thumbnail exists or the image is newer than the thumbnail, create a new one
//     *
//     * @return
//     */
//    public Image getThumbnail(final User user) {
//
//        List<StructrRelationship> thumbnailRelationships = getRelationships(RelType.THUMBNAIL, Direction.OUTGOING);
//
//        Image thumbnail = null;
//        final Image originalImage = this;
//
//        if (thumbnailRelationships != null && !(thumbnailRelationships.isEmpty())) {
//            thumbnail = (Image) thumbnailRelationships.get(0).getEndNode();
//
//            // Check age: Use thumbnail only if younger than original image
//            if (!(originalImage.getLastModifiedDate().after(thumbnail.getLastModifiedDate()))) {
//                return thumbnail;
//            }
//        }
//
//        // No thumbnail exists, or thumbnail is too old, so let's create a new one
//        logger.log(Level.INFO, "Creating thumbnail for {0}", getName());
//
//        Command transactionCommand = Services.createCommand(TransactionCommand.class);
//        thumbnail = (Image) transactionCommand.execute(new StructrTransaction() {
//
//            @Override
//            public Object execute() throws Throwable {
//
//                Command createNode = Services.createCommand(CreateNodeCommand.class);
//                Command createRel = Services.createCommand(CreateRelationshipCommand.class);
//                Command findNode = Services.createCommand(FindNodeCommand.class);
//
//                NodeAttribute typeAttr = new NodeAttribute(StructrNode.TYPE_KEY, Image.class.getSimpleName());
//                NodeAttribute nameAttr = new NodeAttribute(Image.NAME_KEY, originalImage.getName() + "_thumb");
//
//                // create new node
//                StructrNode newNode = (StructrNode) createNode.execute(user, typeAttr, nameAttr);
//
//                if (newNode != null) {
//
//                    // re-find node as image node
//                    Image tn = (Image) findNode.execute(user, newNode.getId());
//
//                    // create a thumbnail relationship
//                    createRel.execute(originalImage, tn, RelType.THUMBNAIL);
//
//                    // determine properties
//                    String relativeFilePath = tn.getId() + "_" + System.currentTimeMillis();
//                    String path = Services.getFilesPath() + "/" + relativeFilePath;
//
//                    java.io.File imageFile = new java.io.File(path);
//
//                    byte[] thumbnailData = ImageHelper.getThumbnailByteArray(originalImage);
//                    try {
//                        // copy url to file
//                        FileUtils.writeByteArrayToFile(imageFile, thumbnailData);
//                    } catch (IOException ex) {
//                        logger.log(Level.SEVERE, "Could not write thumbnail data to file", ex);
//                        return null;
//                    }
//
//                    // set size
//                    long size = imageFile.length();
//                    tn.setSize(size);
//
//                    // set local file url
//                    tn.setRelativeFilePath(relativeFilePath);
//
//                    return tn;
//
//                } else {
//                    return null;
//                }
//            }
//        });
//
//        return thumbnail;
//
//    }

    /**
     * Get (cached) thumbnail relationships
     * 
     * @return
     */
    private List<StructrRelationship> getThumbnailRelationships() {
        if (thumbnailRelationships == null) {
            thumbnailRelationships = getRelationships(RelType.THUMBNAIL, Direction.OUTGOING);
        }
        return thumbnailRelationships;
    }

    public boolean isThumbnail() {
        return !(getRelationships(RelType.THUMBNAIL, Direction.INCOMING).isEmpty());
    }

    /**
     * Get (down-)scaled image of this image
     *
     * If no scaled image of the requested size exists or the image is newer than the scaled image, create a new one
     *
     * @return
     */
    public Image getScaledImage(final User user, final int maxWidth, final int maxHeight) {

        thumbnailRelationships = getThumbnailRelationships();

        Image thumbnail = null;
        final Image originalImage = this;

        int origWidth = originalImage.getWidth();
        int origHeight = originalImage.getHeight();

        if (thumbnailRelationships != null && !(thumbnailRelationships.isEmpty())) {

            for (StructrRelationship r : thumbnailRelationships) {

                Integer w = (Integer) r.getProperty(Image.WIDTH_KEY);
                Integer h = (Integer) r.getProperty(Image.HEIGHT_KEY);

                if (w != null && h != null) {

                    if ((w == maxWidth && h <= maxHeight)
                            || (w <= maxWidth && h == maxHeight)
                            || (origWidth <= w && origHeight <= h)) // orginal image is equal or smaller than requested size
                    {
                        thumbnail = (Image) r.getEndNode();

                        // Check age: Use thumbnail only if younger than original image
                        if (!(originalImage.getLastModifiedDate().after(thumbnail.getLastModifiedDate()))) {
                            return thumbnail;
                        }
                    }
                }

            }

        }

        // No thumbnail exists, or thumbnail is too old, so let's create a new one
        logger.log(Level.INFO, "Creating thumbnail for {0}", getName());

        Command transactionCommand = Services.createCommand(TransactionCommand.class);
        thumbnail = (Image) transactionCommand.execute(new StructrTransaction() {

            @Override
            public Object execute() throws Throwable {

                Command createNode = Services.createCommand(CreateNodeCommand.class);
                Command createRel = Services.createCommand(CreateRelationshipCommand.class);
//                Command findNode = Services.createCommand(FindNodeCommand.class);

                NodeAttribute typeAttr = new NodeAttribute(StructrNode.TYPE_KEY, Image.class.getSimpleName());
                NodeAttribute contentTypeAttr = new NodeAttribute(Image.CONTENT_TYPE_KEY, originalImage.getContentType());

                // create thumbnail node
                Image thumbnail = (Image) createNode.execute(user,
                        typeAttr,
                        contentTypeAttr, // Copy content type from original image
                        false);         // Don't index

                if (thumbnail != null) {

                    // create a thumbnail relationship
                    StructrRelationship thumbnailRelationship = (StructrRelationship) createRel.execute(originalImage, thumbnail, RelType.THUMBNAIL);

                    // determine properties
                    String relativeFilePath = thumbnail.getId() + "_" + System.currentTimeMillis();
                    String path = Services.getFilesPath() + "/" + relativeFilePath;
                    java.io.File imageFile = new java.io.File(path);

                    Thumbnail thumbnailData = ImageHelper.getThumbnailByteArray(originalImage, maxWidth, maxHeight);
                    try {
                        // copy url to file
                        FileUtils.writeByteArrayToFile(imageFile, thumbnailData.getBytes());
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, "Could not write thumbnail data to file", ex);
                        return null;
                    }

                    // set size
                    long size = imageFile.length();
                    thumbnail.setSize(size);

                    int tnWidth = thumbnailData.getWidth();
                    int tnHeight = thumbnailData.getHeight();

                    thumbnailRelationship.setProperty(Image.WIDTH_KEY, tnWidth);
                    thumbnailRelationship.setProperty(Image.HEIGHT_KEY, tnHeight);

                    // set local file url
                    thumbnail.setRelativeFilePath(relativeFilePath);

                    // Set name to reflect thumbnail size
                    thumbnail.setName(thumbnail.getName() + "_" + tnWidth + "x" + tnHeight);

                    return thumbnail;

                } else {
                    return null;
                }
            }
        });

        return thumbnail;

    }
}
