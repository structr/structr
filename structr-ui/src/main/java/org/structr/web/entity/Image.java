/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import static org.structr.core.GraphObject.type;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.Relation;
import static org.structr.core.graph.NodeInterface.name;
import static org.structr.core.graph.NodeInterface.owner;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.dynamic.File;
import org.structr.schema.SchemaService;
import org.structr.web.common.FileHelper;
import org.structr.web.common.ImageHelper;
import org.structr.web.common.ImageHelper.Thumbnail;
import static org.structr.web.entity.FileBase.relativeFilePath;
import static org.structr.web.entity.FileBase.size;
import org.structr.web.entity.relation.Thumbnails;
import org.structr.web.property.ImageDataProperty;
import org.structr.web.property.ThumbnailProperty;

//~--- classes ----------------------------------------------------------------

/**
 * An image whose binary data will be stored on disk.
 *
 *
 *
 */
public class Image extends File {

	// register this type as an overridden builtin type
	static {

		SchemaService.registerBuiltinTypeOverride("Image", Image.class.getName());
	}

	private static final Logger logger = Logger.getLogger(Image.class.getName());

	public static final Property<Integer> height = new IntProperty("height").cmis().indexed();
	public static final Property<Integer> width  = new IntProperty("width").cmis().indexed();

	public static final Property<Image> tnSmall       = new ThumbnailProperty("tnSmall").format("100, 100, false");
	public static final Property<Image> tnMid         = new ThumbnailProperty("tnMid").format("300, 300, false");

	public static final Property<Boolean> isThumbnail = new BooleanProperty("isThumbnail").indexed().unvalidated().readOnly();
	public static final ImageDataProperty imageData   = new ImageDataProperty("imageData");

	public static final Property<Boolean> isImage     = new BooleanProperty("isImage").defaultValue(true).readOnly();

	public static final org.structr.common.View uiView              = new org.structr.common.View(Image.class, PropertyView.Ui, type, name, contentType, size, relativeFilePath, width, height, tnSmall, tnMid, isThumbnail, owner, parent, path, isImage);
	public static final org.structr.common.View publicView          = new org.structr.common.View(Image.class, PropertyView.Public, type, name, width, height, tnSmall, tnMid, isThumbnail, owner, parent, path, isImage);

	@Override
	public Object setProperty(final PropertyKey key, final Object value) throws FrameworkException {

		// Copy visibility properties and owner to all thumbnails
		if (AbstractNode.visibleToPublicUsers.equals(key)
		 || AbstractNode.visibleToAuthenticatedUsers.equals(key)
		 || AbstractNode.visibilityStartDate.equals(key)
		 || AbstractNode.visibleToAuthenticatedUsers.equals(key)
		 || AbstractNode.owner.equals(key)) {

			for (Image tn : getThumbnails()) {

				tn.setProperty(key, value);

			}

		}

		return super.setProperty(key, value);
	}

	//~--- get methods ----------------------------------------------------

	public Integer getWidth() {

		return getProperty(Image.width);

	}

	public Integer getHeight() {

		return getProperty(Image.height);

	}

	public List<Image> getThumbnails() {

		List<Image> thumbnails = new LinkedList<>();

		for (AbstractRelationship s : getThumbnailRelationships()) {

			thumbnails.add((Image) s.getTargetNode());
		}

		return thumbnails;

	}

	/**
	 * Get thumbnail relationships
	 *
	 * @return thumbnails
	 */
	public Iterable<Thumbnails> getThumbnailRelationships() {
		return getOutgoingRelationships(Thumbnails.class);

	}

	/**
	 * Get (down-)scaled image of this image
	 *
	 * If no scaled image of the requested size exists or the image is newer than the scaled image, create a new one
	 *
	 * @param maxWidthString
	 * @param maxHeightString
	 *
	 * @return scaled image
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
	 * @param maxWidth
	 * @param maxHeight
	 * @param cropToFit if true, scale down until the shorter edge fits inside the rectangle, and then crop
	 *
	 * @return scaled image
	 */
	public Image getScaledImage(final int maxWidth, final int maxHeight, final boolean cropToFit) {

		Iterable<Thumbnails> thumbnailRelationships = getThumbnailRelationships();
		final List<Image> oldThumbnails             = new LinkedList<>();
		Image thumbnail                             = null;
		final Image originalImage                   = this;
		Integer origWidth                           = originalImage.getWidth();
		Integer origHeight                          = originalImage.getHeight();
		Long currentChecksum                        = originalImage.getProperty(Image.checksum);
		final Long newChecksum;

		if (currentChecksum == null || currentChecksum == 0) {

			newChecksum = FileHelper.getChecksum(originalImage);

		} else {

			newChecksum = currentChecksum;
		}

		if (origWidth != null && origHeight != null && thumbnailRelationships != null) {

			for (final Thumbnails r : thumbnailRelationships) {

				Integer w = r.getProperty(Image.width);
				Integer h = r.getProperty(Image.height);

				if (w != null && h != null) {

					// orginal image is equal or smaller than requested size
					if (((w == maxWidth) && (h <= maxHeight)) || ((w <= maxWidth) && (h == maxHeight)) || ((origWidth <= w) && (origHeight <= h))) {

						thumbnail = r.getTargetNode();

						// Use thumbnail only if checksum of original image matches with stored checksum
						final Long storedChecksum = r.getProperty(Image.checksum);

						if (storedChecksum != null && storedChecksum.equals(newChecksum)) {

							return thumbnail;

						} else {

							oldThumbnails.add(thumbnail);
						}
					}

				}

			}


		}

		// No thumbnail exists, or thumbnail was too old, so let's create a new one
		logger.log(Level.FINE, "Creating thumbnail for {0}", getName());

		final App app = StructrApp.getInstance(securityContext);

		try {

			originalImage.unlockReadOnlyPropertiesOnce();
			originalImage.setProperty(File.checksum, newChecksum);

			// check size requirements for thumbnail
			if (origWidth != null && origHeight != null) {

				if (origWidth <= maxWidth && origHeight <= maxHeight) {

					return originalImage;
				}
			}

			Thumbnail thumbnailData = ImageHelper.createThumbnail(originalImage, maxWidth, maxHeight, cropToFit);
			if (thumbnailData != null) {

				Integer tnWidth  = thumbnailData.getWidth();
				Integer tnHeight = thumbnailData.getHeight();
				byte[] data      = null;

				try {

					data = thumbnailData.getBytes();
					final String thumbnailName = originalImage.getName() + "_thumb_" + tnWidth + "x" + tnHeight;

					// create thumbnail node
					thumbnail = ImageHelper.createImage(securityContext, data, "image/" + Thumbnail.FORMAT, Image.class, thumbnailName, true);

				} catch (IOException ex) {

					logger.log(Level.WARNING, "Could not create thumbnail image", ex);

				}

				if (thumbnail != null && data != null) {

					// Create a thumbnail relationship
					final Thumbnails thumbnailRelationship = app.create(originalImage, thumbnail, Thumbnails.class);

					// Thumbnails always have to be removed along with origin image
					thumbnailRelationship.setProperty(AbstractRelationship.cascadeDelete, Relation.SOURCE_TO_TARGET);

					// Add to cache list
					// thumbnailRelationships.add(thumbnailRelationship);
					long size = data.length;

					thumbnail.unlockReadOnlyPropertiesOnce();
					thumbnail.setProperty(File.size, size);
					thumbnail.setProperty(Image.width, tnWidth);
					thumbnail.setProperty(Image.height, tnHeight);

					thumbnail.setProperty(AbstractNode.hidden,			originalImage.getProperty(AbstractNode.hidden));
					thumbnail.setProperty(AbstractNode.visibleToAuthenticatedUsers, originalImage.getProperty(AbstractNode.visibleToAuthenticatedUsers));
					thumbnail.setProperty(AbstractNode.visibleToPublicUsers,		originalImage.getProperty(AbstractNode.visibleToPublicUsers));
					thumbnail.setProperty(AbstractNode.owner,			originalImage.getProperty(AbstractNode.owner));

					thumbnailRelationship.setProperty(Image.width, tnWidth);
					thumbnailRelationship.setProperty(Image.height, tnHeight);

					thumbnailRelationship.unlockReadOnlyPropertiesOnce();
					thumbnailRelationship.setProperty(Image.checksum, newChecksum);

					// Delete outdated thumbnails
					for (final Image tn : oldThumbnails) {
						app.delete(tn);
					}
				}

			} else {

				logger.log(Level.FINE, "Could not create thumbnail for image {0} ({1})", new Object[] { getName(), getUuid() });
			}

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
	 * @return true if is thumbnail
	 */
	public boolean isThumbnail() {

		return getProperty(Image.isThumbnail) || getIncomingRelationship(Thumbnails.class) != null;

	}

}
