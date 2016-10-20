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
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.ConstantBooleanProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.dynamic.File;
import org.structr.schema.SchemaService;
import org.structr.web.common.FileHelper;
import org.structr.web.common.ImageHelper;
import org.structr.web.common.ImageHelper.Thumbnail;
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
public class Image extends org.structr.dynamic.File {

	// register this type as an overridden builtin type
	static {

		SchemaService.registerBuiltinTypeOverride("Image", Image.class.getName());
	}

	private static final Logger logger                            = LoggerFactory.getLogger(Image.class.getName());

	public static final Property<Integer> height                  = new IntProperty("height").cmis().indexed();
	public static final Property<Integer> width                   = new IntProperty("width").cmis().indexed();

	public static final Property<Image> tnSmall                   = new ThumbnailProperty("tnSmall").format("100, 100, false");
	public static final Property<Image> tnMid                     = new ThumbnailProperty("tnMid").format("300, 300, false");

	public static final Property<Boolean> isThumbnail             = new BooleanProperty("isThumbnail").indexed().unvalidated().systemInternal();
	public static final ImageDataProperty imageData               = new ImageDataProperty("imageData");

	public static final Property<Boolean> isImage                 = new ConstantBooleanProperty("isImage", true);

	public static final Property<Boolean> isCreatingThumb         = new BooleanProperty("isCreatingThumb").systemInternal();

	public static final org.structr.common.View uiView            = new org.structr.common.View(Image.class, PropertyView.Ui, type, name, contentType, size, relativeFilePath, width, height, tnSmall, tnMid, isThumbnail, owner, parent, path, isImage);
	public static final org.structr.common.View publicView        = new org.structr.common.View(Image.class, PropertyView.Public, type, name, width, height, tnSmall, tnMid, isThumbnail, owner, parent, path, isImage);

	@Override
	public Object setProperty(final PropertyKey key, final Object value) throws FrameworkException {

		// Copy visibility properties and owner to all thumbnails
		if (visibleToPublicUsers.equals(key) ||
			visibleToAuthenticatedUsers.equals(key) ||
			visibilityStartDate.equals(key) ||
			visibilityEndDate.equals(key) ||
			owner.equals(key)) {

			for (Image tn : getThumbnails()) {

				tn.setProperty(key, value);

			}

		}

		return super.setProperty(key, value);
	}

	@Override
	public void setProperties(final SecurityContext securityContext, final PropertyMap properties) throws FrameworkException {

		if ( !isThumbnail() ) {

			final List<Image> thumbnails = getThumbnails();

			for (final Map.Entry<PropertyKey, Object> attr : properties.entrySet()) {

				final PropertyKey key = attr.getKey();
				final Object value    = attr.getValue();

				for (Image tn : thumbnails) {

					if (visibleToPublicUsers.equals(key) ||
						visibleToAuthenticatedUsers.equals(key) ||
						visibilityStartDate.equals(key) ||
						visibilityEndDate.equals(key) ||
						owner.equals(key)) {

						tn.setProperty(key, value);

					}

				}

			}

		}

		super.setProperties(securityContext, properties);
	}

	@Override
	public boolean onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		if (super.onModification(securityContext, errorBuffer, modificationQueue)) {

			if ( !isThumbnail() ) {

				if (modificationQueue.isPropertyModified(name)) {

					final String newImageName = getName();

					for (Image tn : getThumbnails()) {

						final String expectedThumbnailName = getThumbnailName(newImageName, tn.getWidth(), tn.getHeight());
						final String currentThumbnailName  = tn.getName();

						if ( !expectedThumbnailName.equals(currentThumbnailName) ) {

							logger.debug("Auto-renaming Thumbnail({}) from '{}' to '{}'", tn.getUuid(), currentThumbnailName, expectedThumbnailName);
							tn.setProperty(AbstractNode.name, expectedThumbnailName);

						}

					}

				}

			}

			return true;
		}

		return false;
	}

	//~--- get methods ----------------------------------------------------

	public Integer getWidth() {

		return getProperty(Image.width);

	}

	public Integer getHeight() {

		return getProperty(Image.height);

	}

	public List<Image> getThumbnails() {

		final List<Image> thumbnails = new LinkedList<>();

		for (final AbstractRelationship s : getThumbnailRelationships()) {

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

		final Iterable<Thumbnails> thumbnailRelationships = getThumbnailRelationships();
		final List<Image> oldThumbnails                   = new LinkedList<>();
		Image thumbnail                                   = null;
		final Image originalImage                         = this;
		final Integer origWidth                           = originalImage.getWidth();
		final Integer origHeight                          = originalImage.getHeight();
		final Long currentChecksum                        = originalImage.getProperty(Image.checksum);
		final Long newChecksum;

		if (currentChecksum == null || currentChecksum == 0) {

			newChecksum = FileHelper.getChecksum(originalImage);

		} else {

			newChecksum = currentChecksum;
		}

		if (origWidth != null && origHeight != null && thumbnailRelationships != null) {

			for (final Thumbnails r : thumbnailRelationships) {

				final Integer w = r.getProperty(Image.width);
				final Integer h = r.getProperty(Image.height);

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

		if (originalImage.getProperty(Image.isCreatingThumb).equals(Boolean.TRUE)) {

			logger.debug("Another thumbnail is being created - waiting....");

		} else {

			try {

				// No thumbnail exists, or thumbnail was too old, so let's create a new one
				logger.debug("Creating thumbnail for {} (w={} h={} crop={})", new Object[] { getName(), maxWidth, maxHeight, cropToFit });

				originalImage.unlockSystemPropertiesOnce();
				originalImage.setProperty(Image.isCreatingThumb, Boolean.TRUE);

				final App app = StructrApp.getInstance(securityContext);

				originalImage.unlockSystemPropertiesOnce();
				originalImage.setProperty(File.checksum, newChecksum);

				final Thumbnail thumbnailData = ImageHelper.createThumbnail(originalImage, maxWidth, maxHeight, cropToFit);
				if (thumbnailData != null) {

					final Integer tnWidth  = thumbnailData.getWidth();
					final Integer tnHeight = thumbnailData.getHeight();
					byte[] data            = null;

					try {

						data = thumbnailData.getBytes();
						final String thumbnailName = getThumbnailName(originalImage.getName(), tnWidth, tnHeight);

						// create thumbnail node
						thumbnail = ImageHelper.createImage(securityContext, data, "image/" + Thumbnail.FORMAT, Image.class, thumbnailName, true);

					} catch (IOException ex) {

						logger.warn("Could not create thumbnail image for " + getUuid(), ex);

					}

					if (thumbnail != null && data != null) {

						// Create a thumbnail relationship
						final PropertyMap relProperties = new PropertyMap();
						relProperties.put(Image.width,                  tnWidth);
						relProperties.put(Image.height,                 tnHeight);
						relProperties.put(Image.checksum,               newChecksum);

						app.create(originalImage, thumbnail, Thumbnails.class, relProperties);

						final PropertyMap properties = new PropertyMap();
						properties.put(Image.width,                              tnWidth);
						properties.put(Image.height,                             tnHeight);
						properties.put(AbstractNode.hidden,                      originalImage.getProperty(AbstractNode.hidden));
						properties.put(AbstractNode.visibleToAuthenticatedUsers, originalImage.getProperty(AbstractNode.visibleToAuthenticatedUsers));
						properties.put(AbstractNode.visibleToPublicUsers,        originalImage.getProperty(AbstractNode.visibleToPublicUsers));


						thumbnail.setProperties(securityContext, properties);

						thumbnail.setProperty(AbstractNode.owner, originalImage.getProperty(AbstractNode.owner));
						thumbnail.setProperty(File.parent, originalImage.getProperty(File.parent));

						thumbnail.unlockSystemPropertiesOnce();
						thumbnail.setProperty(File.size, Long.valueOf(data.length));

						// Delete outdated thumbnails
						for (final Image tn : oldThumbnails) {
							app.delete(tn);
						}

					}

				} else {

					logger.debug("Could not create thumbnail for image {} ({})", getName(), getUuid());

				}

				originalImage.unlockSystemPropertiesOnce();
				originalImage.removeProperty(Image.isCreatingThumb);

			} catch (FrameworkException fex) {

				logger.warn("Unable to create thumbnail for " + getUuid(), fex);

			}

		}

		return thumbnail;

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

	/**
	 * @param originalImageName The filename of the image which this thumbnail belongs to
	 * @param tnWidth The width of the thumbnail
	 * @param tnHeight The height of the thumbnail
	 * @return the thumbnail name for the thumbnail with the given dimensions
	 */
	public String getThumbnailName(final String originalImageName, final Integer tnWidth, final Integer tnHeight) {

		return originalImageName + "_thumb_" + tnWidth + "x" + tnHeight;

	}

}
