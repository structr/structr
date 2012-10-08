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



package org.structr.core.entity;

import org.apache.commons.io.FileUtils;

import org.neo4j.graphdb.Direction;

import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.EntityContext;
import org.structr.core.Services;
import org.structr.core.entity.RelationClass.Cardinality;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.DeleteNodeCommand;
import org.structr.core.node.DeleteRelationshipCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.ImageHelper;
import org.structr.common.ImageHelper.Thumbnail;
import org.structr.core.converter.ImageConverter;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 *
 */
public class Image extends File {

	private static final Logger logger = Logger.getLogger(Image.class.getName());

	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerEntityRelation(Image.class, Folder.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToOne);
		
		EntityContext.registerPropertySet(Image.class, PropertyView.All, Key.values());
		EntityContext.registerPropertySet(Image.class, PropertyView.Ui, Key.values());
		
		// Write data to disk (and convert, if it's a base64 encoded string)
		EntityContext.registerPropertyConverter(Image.class, HiddenKey.imageData, ImageConverter.class);

	}

	//~--- fields ---------------------------------------------------------

	// Cached list with relationships to thumbnails
	private List<AbstractRelationship> thumbnailRelationships = null;

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey{ height, width; }
	
	public enum HiddenKey implements PropertyKey{ imageData }

	//~--- methods --------------------------------------------------------

	public void removeThumbnails() {

		Command deleteRelationship = Services.command(securityContext, DeleteRelationshipCommand.class);
		Command deleteNode         = Services.command(securityContext, DeleteNodeCommand.class);

		for (AbstractRelationship s : getThumbnailRelationships()) {

			AbstractNode thumbnail = s.getEndNode();

			if (((Image) thumbnail).equals(this)) {

				logger.log(Level.SEVERE, "Attempted to remove me as thumbnail!!");

				continue;

			}

			try {

				deleteRelationship.execute(s);
				deleteNode.execute(thumbnail);

			} catch (FrameworkException fex) {

				logger.log(Level.WARNING, "Unable to remove thumbnail", fex);

			}

		}

		// Clear cache
		thumbnailRelationships = null;

	}

	//~--- get methods ----------------------------------------------------

	public Integer getWidth() {

		return getIntProperty(Key.width.name());

	}

	public Integer getHeight() {

		return getIntProperty(Key.height.name());

	}

	public List<Image> getThumbnails() {

		List<Image> thumbnails = new LinkedList<Image>();

		for (AbstractRelationship s : getThumbnailRelationships()) {

			thumbnails.add((Image) s.getEndNode());
		}

		return thumbnails;

	}

	/**
	 * Get (cached) thumbnail relationships
	 *
	 * @return
	 */
	public List<AbstractRelationship> getThumbnailRelationships() {

		if (thumbnailRelationships == null) {

			thumbnailRelationships = getRelationships(RelType.THUMBNAIL, Direction.OUTGOING);
		}

		return thumbnailRelationships;

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
	public Image getScaledImage(final int maxWidth, final int maxHeight, final boolean cropToFit) {

		thumbnailRelationships = getThumbnailRelationships();

		Image thumbnail           = null;
		final Image originalImage = this;
		Integer origWidth         = originalImage.getWidth();
		Integer origHeight        = originalImage.getHeight();

		if ((origWidth != null) && (origHeight != null)) {

			if ((thumbnailRelationships != null) &&!(thumbnailRelationships.isEmpty())) {

				for (AbstractRelationship r : thumbnailRelationships) {

					Integer w = (Integer) r.getProperty(Key.width.name());
					Integer h = (Integer) r.getProperty(Key.height.name());

					if ((w != null) && (h != null)) {

						if (((w == maxWidth) && (h <= maxHeight)) || ((w <= maxWidth) && (h == maxHeight))

//                                              || (cropToFit && ((w == maxWidth && h >= maxHeight) || (w >= maxWidth && h == maxHeight)))
						|| ((origWidth <= w) && (origHeight <= h)))    // orginal image is equal or smaller than requested size
						{

//                                                      if ((w == maxWidth && h <= maxHeight)
//                                                              || (w <= maxWidth && h == maxHeight)
//                                                              || (cropToFit && ((w == maxWidth && h >= maxHeight) || (w >= maxWidth && h == maxHeight)))
//                                                              || (origWidth <= w && origHeight <= h)) // orginal image is equal or smaller than requested size
//                                                      {
							thumbnail = (Image) r.getEndNode();

							// Check age: Use thumbnail only if younger than original image
							if (!(originalImage.getLastModifiedDate().after(thumbnail.getLastModifiedDate()))) {

								return thumbnail;
							}
						}

					}

				}

			}

		}

		// No thumbnail exists, or thumbnail is too old, so let's create a new one
		logger.log(Level.INFO, "Creating thumbnail for {0}", getName());

		try {

			Command transactionCommand = Services.command(securityContext, TransactionCommand.class);

			thumbnail = (Image) transactionCommand.execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					Command createNode = Services.command(securityContext, CreateNodeCommand.class);
					Command createRel  = Services.command(securityContext, CreateRelationshipCommand.class);

					// Command findNode = Services.command(securityContext, FindNodeCommand.class);
					NodeAttribute typeAttr                           = new NodeAttribute(AbstractNode.Key.type.name(), Image.class.getSimpleName());
					NodeAttribute contentTypeAttr                    = new NodeAttribute(File.Key.contentType.name(), "image/" + Thumbnail.FORMAT);
					NodeAttribute isHiddenAttr                       = new NodeAttribute(AbstractNode.Key.hidden.name(), originalImage.getHidden());
					NodeAttribute isPublicAttr                       = new NodeAttribute(AbstractNode.Key.visibleToPublicUsers.name(), originalImage.getVisibleToPublicUsers());
					NodeAttribute isVisibleForAuthenticatedUsersAttr = new NodeAttribute(AbstractNode.Key.visibleToAuthenticatedUsers.name(),
												   originalImage.getVisibleToAuthenticatedUsers());
					Thumbnail thumbnailData = ImageHelper.createThumbnail(originalImage, maxWidth, maxHeight, cropToFit);

					if (thumbnailData != null) {

						// create thumbnail node
						// Image thumbnail = (Image) createNode.execute(user,
						Image thumbnail = (Image) createNode.execute(originalImage.getOwnerNode(),                                    // Same owner as original image
							typeAttr, contentTypeAttr, isHiddenAttr, isPublicAttr, isVisibleForAuthenticatedUsersAttr, false);    // Don't index thumbnails

						if (thumbnail != null) {

							// Create a thumbnail relationship
							AbstractRelationship thumbnailRelationship = (AbstractRelationship) createRel.execute(originalImage, thumbnail, RelType.THUMBNAIL);

							// Add to cache list
							thumbnailRelationships.add(thumbnailRelationship);

							// determine properties
							String relativeFilePath = thumbnail.getId() + "_" + System.currentTimeMillis();
							String path             = Services.getFilesPath() + "/" + relativeFilePath;
							java.io.File imageFile  = new java.io.File(path);

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

							Integer tnWidth  = thumbnailData.getWidth();
							Integer tnHeight = thumbnailData.getHeight();

							thumbnailRelationship.setProperty(Key.width.name(), tnWidth);
							thumbnailRelationship.setProperty(Key.height.name(), tnHeight);

							// set local file url
							thumbnail.setRelativeFilePath(relativeFilePath);

							// Set name to reflect thumbnail size
							thumbnail.setName(originalImage.getName() + "_thumb_" + tnWidth + "x" + tnHeight);
						}

						return thumbnail;
					} else {

						logger.log(Level.WARNING, "Could not create thumbnail for image {0} ({1})", new Object[] { getName(), getId() });

						return null;

					}

				}

			});

		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Unable to create thumbnail", fex);

		}

		return thumbnail;

	}

	public boolean isNotThumbnail() {

		return !isThumbnail();

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

	//~--- set methods ----------------------------------------------------

	public void setWidth(Integer width) throws FrameworkException {

		setProperty(Key.width.name(), width);

	}

	public void setHeight(Integer height) throws FrameworkException {

		setProperty(Key.height.name(), height);

	}

	/** Copy public flag to all thumbnails */
	@Override
	public void setVisibleToPublicUsers(final boolean publicFlag) throws FrameworkException {

		super.setVisibleToPublicUsers(publicFlag);

		for (Image thumbnail : getThumbnails()) {

			thumbnail.setProperty(AbstractNode.Key.visibleToPublicUsers.name(), publicFlag);
		}

	}

	/** Copy visible for authenticated users flag to all thumbnails */
	@Override
	public void setVisibleToAuthenticatedUsers(final boolean flag) throws FrameworkException {

		super.setVisibleToAuthenticatedUsers(flag);

		for (Image thumbnail : getThumbnails()) {

			thumbnail.setProperty(AbstractNode.Key.visibleToAuthenticatedUsers.name(), flag);
		}

	}

	/** Copy hidden flag to all thumbnails */
	@Override
	public void setHidden(final boolean hidden) throws FrameworkException {

		super.setHidden(hidden);

		for (Image thumbnail : getThumbnails()) {

			thumbnail.setProperty(AbstractNode.Key.hidden.name(), hidden);
		}

	}

	/** Copy deleted flag to all thumbnails */
	@Override
	public void setDeleted(final boolean deleted) throws FrameworkException {

		super.setDeleted(deleted);

		for (Image thumbnail : getThumbnails()) {

			thumbnail.setProperty(AbstractNode.Key.deleted.name(), deleted);
		}

	}

	/** Copy visibility start date to all thumbnails */
	@Override
	public void setVisibilityStartDate(final Date date) throws FrameworkException {

		super.setVisibilityStartDate(date);

		for (Image thumbnail : getThumbnails()) {

			thumbnail.setProperty(AbstractNode.Key.visibilityStartDate.name(), date);
		}

	}

	/** Copy visibility end date to all thumbnails */
	@Override
	public void setVisibilityEndDate(final Date date) throws FrameworkException {

		super.setVisibilityEndDate(date);

		for (Image thumbnail : getThumbnails()) {

			thumbnail.setProperty(AbstractNode.Key.visibilityEndDate.name(), date);
		}

	}

}
