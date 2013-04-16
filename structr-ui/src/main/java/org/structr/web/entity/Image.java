/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.web.entity;

import org.structr.web.common.FileHelper;
import org.neo4j.graphdb.Direction;

import org.structr.web.common.ImageHelper;
import org.structr.web.common.ImageHelper.Thumbnail;
import org.structr.common.PropertyView;
import org.structr.web.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.Services;
import org.structr.core.graph.CreateRelationshipCommand;
import org.structr.core.graph.DeleteNodeCommand;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.ThumbnailParameters;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeService.NodeIndex;
import org.structr.core.property.BooleanProperty;
import org.structr.web.property.ThumbnailProperty;

//~--- classes ----------------------------------------------------------------

/**
 * An image whose binary data will be stored on disk.
 * 
 * @author Axel Morgner
 *
 */
public class Image extends File {

	private static final Logger logger = Logger.getLogger(Image.class.getName());

	public static final Property<Integer> height = new IntProperty("height");
	public static final Property<Integer> width  = new IntProperty("width");
	
	public static final Property<Image> tnSmall       = new ThumbnailProperty("tnSmall", new ThumbnailParameters(100, 100, false));
	public static final Property<Image> tnMid         = new ThumbnailProperty("tnMid", new ThumbnailParameters(300, 300, false));
	
	public static final Property<Boolean> isThumbnail = new BooleanProperty("isThumbnail").systemProperty().readOnly();
	
//	public static final CollectionProperty<Image> thumbnails = new CollectionProperty("thumbnails", Image.class, RelType.THUMBNAIL, Direction.OUTGOING, true, Relation.DELETE_OUTGOING);

	public static final org.structr.common.View uiView              = new org.structr.common.View(Image.class, PropertyView.Ui, type, name, contentType, size, relativeFilePath, width, height, tnSmall, tnMid, isThumbnail, owner);
	public static final org.structr.common.View publicView          = new org.structr.common.View(Image.class, PropertyView.Public, type, name, width, height, tnSmall, tnMid, isThumbnail, owner);
	
	static {
		EntityContext.registerSearchablePropertySet(Image.class, NodeIndex.keyword.name(), uuid, type, name, contentType, size, relativeFilePath, width, height, isThumbnail, owner);
		EntityContext.registerSearchablePropertySet(Image.class, NodeIndex.fulltext.name(), uuid, type, name, contentType, size, relativeFilePath, width, height, isThumbnail, owner);
	}

	//~--- methods --------------------------------------------------------

//	@Override
//	public void afterDeletion(SecurityContext securityContext) {
//
//		removeThumbnails();
//
//	}
	
	private void removeThumbnails() {

		DeleteNodeCommand deleteNode                 = Services.command(securityContext, DeleteNodeCommand.class);

		for (AbstractRelationship s : getThumbnailRelationships()) {

			AbstractNode thumbnail = s.getEndNode();

			if (((Image) thumbnail).equals(this)) {

				logger.log(Level.SEVERE, "Attempted to remove me as thumbnail!!");

				continue;

			}

			try {

				deleteNode.execute(thumbnail, true);

			} catch (FrameworkException fex) {

				logger.log(Level.WARNING, "Unable to remove thumbnail", fex);

			}

		}

	}

	//~--- get methods ----------------------------------------------------

	public Integer getWidth() {

		return getIntProperty(Image.width);

	}

	public Integer getHeight() {

		return getIntProperty(Image.height);

	}

	public List<Image> getThumbnails() {

		List<Image> thumbnails = new LinkedList<Image>();

		for (AbstractRelationship s : getThumbnailRelationships()) {

			thumbnails.add((Image) s.getEndNode());
		}

		return thumbnails;

	}

	/**
	 * Get thumbnail relationships
	 *
	 * @return
	 */
	public List<AbstractRelationship> getThumbnailRelationships() {

		return getRelationships(RelType.THUMBNAIL, Direction.OUTGOING);

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

		List<AbstractRelationship> thumbnailRelationships = getThumbnailRelationships();
		final List<Image> oldThumbnails                   = new LinkedList();
		Image thumbnail                                   = null;
		final Image originalImage                         = this;
		Integer origWidth                                 = originalImage.getWidth();
		Integer origHeight                                = originalImage.getHeight();
		Long currentChecksum                              = originalImage.getProperty(Image.checksum);
		final Long newChecksum;

		if (currentChecksum == null || currentChecksum == 0) {

			newChecksum = FileHelper.getChecksum(originalImage);
		} else {

			newChecksum = currentChecksum;
		}

		if ((origWidth != null) && (origHeight != null)) {

			if ((thumbnailRelationships != null) && !(thumbnailRelationships.isEmpty())) {

				for (final AbstractRelationship r : thumbnailRelationships) {

					Integer w = (Integer) r.getProperty(Image.width);
					Integer h = (Integer) r.getProperty(Image.height);

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

							// Use thumbnail only if checksum of original image matches with stored checksum
							Long storedChecksum = r.getProperty(Image.checksum);

//                                                      System.out.println("Rel ID: " + r.getUuid() + ", orig. image ID: " + originalImage.getUuid() + ", stored checksum: " + storedChecksum + ", current checksum: " + currentChecksum);
//                                                      if (!(originalImage.getLastModifiedDate().after(thumbnail.getLastModifiedDate()))) {
							if (storedChecksum != null && storedChecksum.equals(newChecksum)) {

								return thumbnail;
							} else {

								oldThumbnails.add(thumbnail);
							}
						}

					}

				}

			}

		}

		// No thumbnail exists, or thumbnail was too old, so let's create a new one
		logger.log(Level.FINE, "Creating thumbnail for {0}", getName());

		try {

			thumbnail = Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction<Image>() {

				@Override
				public Image execute() throws FrameworkException {

					try {

						originalImage.setChecksum(newChecksum);

					} catch (Exception ex) {

						logger.log(Level.SEVERE, "Could not store checksum for original image", ex);

					}

					CreateRelationshipCommand createRel = Services.command(securityContext, CreateRelationshipCommand.class);
					Thumbnail thumbnailData             = ImageHelper.createThumbnail(originalImage, maxWidth, maxHeight, cropToFit);

					if (thumbnailData != null) {

						Integer tnWidth  = thumbnailData.getWidth();
						Integer tnHeight = thumbnailData.getHeight();
						Image thumbnail  = null;
						byte[] data      = null;

						try {

							data = thumbnailData.getBytes();

							// create thumbnail node
							thumbnail = ImageHelper.createImage(securityContext, data, "image/" + Thumbnail.FORMAT, Image.class, true);

						} catch (IOException ex) {

							logger.log(Level.WARNING, "Could not create thumbnail image", ex);

						}

						if (thumbnail != null) {

							// Create a thumbnail relationship
							AbstractRelationship thumbnailRelationship = createRel.execute(originalImage, thumbnail, RelType.THUMBNAIL, true);
							
							// Thumbnails always have to be removed along with origin image
							thumbnailRelationship.setProperty(AbstractRelationship.cascadeDelete, Relation.DELETE_OUTGOING);

							// Add to cache list
							// thumbnailRelationships.add(thumbnailRelationship);
							long size = data.length;

							thumbnail.setSize(size);
							thumbnail.setName(originalImage.getName() + "_thumb_" + tnWidth + "x" + tnHeight);
							thumbnail.setWidth(tnWidth);
							thumbnail.setHeight(tnHeight);
							
							thumbnail.setProperty(Image.hidden,				originalImage.getProperty(Image.hidden));
							thumbnail.setProperty(Image.visibleToAuthenticatedUsers,		originalImage.getProperty(Image.visibleToAuthenticatedUsers));
							thumbnail.setProperty(Image.visibleToPublicUsers,		originalImage.getProperty(Image.visibleToPublicUsers));
							
							thumbnailRelationship.setProperty(Image.width, tnWidth);
							thumbnailRelationship.setProperty(Image.height, tnHeight);
							thumbnailRelationship.setProperty(Image.checksum, newChecksum);

//                                                      System.out.println("Thumbnail ID: " + thumbnail.getUuid() + ", orig. image ID: " + originalImage.getUuid() + ", orig. image checksum: " + originalImage.getProperty(checksum));
							// Soft-delete outdated thumbnails
							for (Image tn : oldThumbnails) {

								tn.setDeleted(true);
							}
						}

						// Services.command(securityContext, IndexNodeCommand.class).execute(thumbnail);
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

		return getProperty(Image.isThumbnail) || hasRelationship(RelType.THUMBNAIL, Direction.INCOMING);

	}

	//~--- set methods ----------------------------------------------------

	public void setWidth(Integer width) throws FrameworkException {

		setProperty(Image.width, width);

	}

	public void setHeight(Integer height) throws FrameworkException {

		setProperty(Image.height, height);

	}
//
//	/** Copy public flag to all thumbnails */
//	@Override
//	public void setVisibleToPublicUsers(final boolean publicFlag) throws FrameworkException {
//
//		super.setVisibleToPublicUsers(publicFlag);
//
//		for (Image thumbnail : getThumbnails()) {
//
//			thumbnail.setProperty(AbstractNode.visibleToPublicUsers, publicFlag);
//		}
//
//	}
//
//	/** Copy visible for authenticated users flag to all thumbnails */
//	@Override
//	public void setVisibleToAuthenticatedUsers(final boolean flag) throws FrameworkException {
//
//		super.setVisibleToAuthenticatedUsers(flag);
//
//		for (Image thumbnail : getThumbnails()) {
//
//			thumbnail.setProperty(AbstractNode.visibleToAuthenticatedUsers, flag);
//		}
//
//	}
//
//	/** Copy hidden flag to all thumbnails */
//	@Override
//	public void setHidden(final boolean hidden) throws FrameworkException {
//
//		super.setHidden(hidden);
//
//		for (Image thumbnail : getThumbnails()) {
//
//			thumbnail.setProperty(AbstractNode.hidden, hidden);
//		}
//
//	}
//
//	/** Copy deleted flag to all thumbnails */
//	@Override
//	public void setDeleted(final boolean deleted) throws FrameworkException {
//
//		super.setDeleted(deleted);
//
//		for (Image thumbnail : getThumbnails()) {
//
//			thumbnail.setProperty(AbstractNode.deleted, deleted);
//		}
//
//	}
//
//	/** Copy visibility start date to all thumbnails */
//	@Override
//	public void setVisibilityStartDate(final Date date) throws FrameworkException {
//
//		super.setVisibilityStartDate(date);
//
//		for (Image thumbnail : getThumbnails()) {
//
//			thumbnail.setProperty(AbstractNode.visibilityStartDate, date);
//		}
//
//	}
//
//	/** Copy visibility end date to all thumbnails */
//	@Override
//	public void setVisibilityEndDate(final Date date) throws FrameworkException {
//
//		super.setVisibilityEndDate(date);
//
//		for (Image thumbnail : getThumbnails()) {
//
//			thumbnail.setProperty(AbstractNode.visibilityEndDate, date);
//		}
//
//	}

}
