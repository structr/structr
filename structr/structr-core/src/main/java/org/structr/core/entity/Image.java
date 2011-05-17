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
package org.structr.core.entity;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
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
import org.structr.core.node.DeleteNodeCommand;
import org.structr.core.node.DeleteRelationshipCommand;
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

    /** Copy public flag to all thumbnails */
    @Override
    public void setPublic(final boolean publicFlag) {
        super.setPublic(publicFlag);
        for (Image thumbnail : getThumbnails()) {
            thumbnail.setProperty(PUBLIC_KEY, publicFlag);
        }
    }

    /** Copy visible for authenticated users flag to all thumbnails */
    @Override
    public void setVisibleToAuthenticatedUsers(final boolean flag) {
        super.setVisibleToAuthenticatedUsers(flag);
        for (Image thumbnail : getThumbnails()) {
            thumbnail.setProperty(VISIBLE_TO_AUTHENTICATED_USERS_KEY, flag);
        }
    }

    /** Copy hidden flag to all thumbnails */
    @Override
    public void setHidden(final boolean hidden) {
        super.setHidden(hidden);
        for (Image thumbnail : getThumbnails()) {
            thumbnail.setProperty(HIDDEN_KEY, hidden);
        }
    }

    /** Copy deleted flag to all thumbnails */
    @Override
    public void setDeleted(final boolean deleted) {
        super.setDeleted(deleted);
        for (Image thumbnail : getThumbnails()) {
            thumbnail.setProperty(DELETED_KEY, deleted);
        }
    }

    /** Copy visibility start date to all thumbnails */
    @Override
    public void setVisibilityStartDate(final Date date) {
        super.setVisibilityStartDate(date);
        for (Image thumbnail : getThumbnails()) {
            thumbnail.setProperty(VISIBILITY_START_DATE_KEY, date);
        }
    }

    /** Copy visibility end date to all thumbnails */
    @Override
    public void setVisibilityEndDate(final Date date) {
        super.setVisibilityEndDate(date);
        for (Image thumbnail : getThumbnails()) {
            thumbnail.setProperty(VISIBILITY_END_DATE_KEY, date);
        }
    }

    @Override
    public void renderView(StringBuilder out, final AbstractNode startNode,
            final String editUrl, final Long editNodeId) {

        if (editNodeId != null && getId() == editNodeId.longValue()) {

            renderEditFrame(out, editUrl);

        } else {

            String imageUrl = null;

            if (getUrl() == null) {
                imageUrl = getNodePath(startNode);
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
    public void renderDirect(OutputStream out, final AbstractNode startNode,
            final String editUrl, final Long editNodeId) {

        if (isVisible()) {
            super.renderDirect(out, startNode, editUrl, editNodeId);
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
//    public Image createThumbnail(final User user) {
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
//        Command transactionCommand = Services.command(TransactionCommand.class);
//        thumbnail = (Image) transactionCommand.execute(new StructrTransaction() {
//
//            @Override
//            public Object execute() throws Throwable {
//
//                Command createNode = Services.command(CreateNodeCommand.class);
//                Command createRel = Services.command(CreateRelationshipCommand.class);
//                Command findNode = Services.command(FindNodeCommand.class);
//
//                NodeAttribute typeAttr = new NodeAttribute(AbstractNode.TYPE_KEY, Image.class.getSimpleName());
//                NodeAttribute nameAttr = new NodeAttribute(Image.NAME_KEY, originalImage.getName() + "_thumb");
//
//                // create new node
//                AbstractNode newNode = (AbstractNode) createNode.execute(user, typeAttr, nameAttr);
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
//                    byte[] thumbnailData = ImageHelper.createThumbnail(originalImage);
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

    public List<Image> getThumbnails() {
        List<Image> thumbnails = new LinkedList<Image>();

        for (StructrRelationship s : getThumbnailRelationships()) {
            thumbnails.add((Image) s.getEndNode());
        }
        return thumbnails;
    }

    synchronized public void removeThumbnails() {

        Command deleteRelationship = Services.command(DeleteRelationshipCommand.class);
        Command deleteNode = Services.command(DeleteNodeCommand.class);

        for (StructrRelationship s : getThumbnailRelationships()) {

            AbstractNode thumbnail = s.getEndNode();

            if (((Image)thumbnail).equals(this)) {
                logger.log(Level.SEVERE, "Attempted to remove me as thumbnail!!");
                continue;
            }

            deleteRelationship.execute(s);
            deleteNode.execute(thumbnail, new SuperUser());
        }

        // Clear cache
        thumbnailRelationships = null;

    }

    /**
     * Get (cached) thumbnail relationships
     * 
     * @return
     */
    public List<StructrRelationship> getThumbnailRelationships() {
        if (thumbnailRelationships == null) {
            thumbnailRelationships = getRelationships(RelType.THUMBNAIL, Direction.OUTGOING);
        }
        return thumbnailRelationships;
    }

    /**
     * Return true if this image is a thumbnail image.
     *
     * This is determined by having at least one incoming THUMBNAIL relationship
     * 
     * @return
     */
    public boolean isThumbnail() {
        return hasRelationship(RelType.THUMBNAIL, Direction.INCOMING);
    }

    /**
     * Get (down-)scaled image of this image
     *
     * If no scaled image of the requested size exists or the image is newer than the scaled image, create a new one
     *
     * @maxWidthString
     * @maxHeightString
     *
     * @return
     */
    public Image getScaledImage(final String maxWidthString, final String maxHeightString) {
        return getScaledImage(Integer.parseInt(maxWidthString), Integer.parseInt(maxHeightString), false);
    }

    public Image getScaledImage(final String maxWidthString, final String maxHeightString, final boolean cropToFit) {
        return getScaledImage(Integer.parseInt(maxWidthString), Integer.parseInt(maxHeightString), cropToFit);
    }

    public Image getScaledImage(final int maxWidth, final int maxHeight) {
        return getScaledImage(maxWidth, maxHeight, false);
    }

    /**
     * Get (down-)scaled image of this image
     *
     * If no scaled image of the requested size exists or the image is newer than the scaled image, create a new one.
     *
     * Default behaviour is to make the scaled image complete fit inside a rectangle of maxWidth x maxHeight.
     *
     * @maxWidth
     * @maxHeight
     * @cropToFit if true, scale down until the shorter edge fits inside the rectangle, and then crop
     *
     * @return
     */
    synchronized public Image getScaledImage(final int maxWidth, final int maxHeight, final boolean cropToFit) {

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
                            || (cropToFit && ((w == maxWidth && h >= maxHeight) || (w >= maxWidth && h == maxHeight)))
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

        Command transactionCommand = Services.command(TransactionCommand.class);
        thumbnail = (Image) transactionCommand.execute(new StructrTransaction() {

            @Override
            public Object execute() throws Throwable {

                Command createNode = Services.command(CreateNodeCommand.class);
                Command createRel = Services.command(CreateRelationshipCommand.class);
//                Command findNode = Services.command(FindNodeCommand.class);

                NodeAttribute typeAttr = new NodeAttribute(AbstractNode.TYPE_KEY, Image.class.getSimpleName());
                NodeAttribute contentTypeAttr = new NodeAttribute(Image.CONTENT_TYPE_KEY, "image/" + Thumbnail.FORMAT);
                NodeAttribute isHiddenAttr = new NodeAttribute(AbstractNode.HIDDEN_KEY, originalImage.getHidden());
                NodeAttribute isPublicAttr = new NodeAttribute(AbstractNode.PUBLIC_KEY, originalImage.getPublic());
                NodeAttribute isVisibleForAuthenticatedUsersAttr = new NodeAttribute(AbstractNode.VISIBLE_TO_AUTHENTICATED_USERS_KEY, originalImage.getVisibleToAuthenticatedUsers());

                Thumbnail thumbnailData = ImageHelper.createThumbnail(originalImage, maxWidth, maxHeight, cropToFit);

                if (thumbnailData != null) {

                    // create thumbnail node
//                    Image thumbnail = (Image) createNode.execute(user,
                    Image thumbnail = (Image) createNode.execute(originalImage.getOwnerNode(), // Same owner as original image
                            typeAttr,
                            contentTypeAttr,
                            isHiddenAttr,
                            isPublicAttr,
                            isVisibleForAuthenticatedUsersAttr,
                            false);         // Don't index thumbnails

                    if (thumbnail != null) {

                        // Create a thumbnail relationship
                        StructrRelationship thumbnailRelationship = (StructrRelationship) createRel.execute(originalImage, thumbnail, RelType.THUMBNAIL);

                        // Add to cache list
                        thumbnailRelationships.add(thumbnailRelationship);

                        // determine properties
                        String relativeFilePath = thumbnail.getId() + "_" + System.currentTimeMillis();
                        String path = Services.getFilesPath() + "/" + relativeFilePath;
                        java.io.File imageFile = new java.io.File(path);

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
                        thumbnail.setName(originalImage.getName() + "_thumb_" + tnWidth + "x" + tnHeight);
                    }
                    return thumbnail;

                } else {
                    logger.log(Level.WARNING, "Could not create thumbnail for image {0} ({1})", new Object[]{getName(), getId()});
                    return null;
                }
            }
        });

        return thumbnail;

    }
}
