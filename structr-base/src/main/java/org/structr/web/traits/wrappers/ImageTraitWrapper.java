/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.traits.wrappers;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.web.agent.ThumbnailTask;
import org.structr.web.common.FileHelper;
import org.structr.web.common.ImageHelper;
import org.structr.web.entity.Folder;
import org.structr.web.entity.Image;

import java.util.ArrayList;
import java.util.List;

/**
 * An image whose binary data will be stored on disk.
 */
public class ImageTraitWrapper extends FileTraitWrapper implements Image {

	public ImageTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public void setIsCreatingThumb(final boolean isCreatingThumb) throws FrameworkException {
		wrappedObject.setProperty(traits.key("isCreatingThumb"), isCreatingThumb);
	}

	@Override
	public boolean isImage() {
		return wrappedObject.getProperty(traits.key("isImage"));
	}

	@Override
	public boolean isThumbnail() {
		return wrappedObject.getProperty(traits.key("isThumbnail"));
	}

	@Override
	public boolean getIsCreatingThumb() {
		return wrappedObject.getProperty(traits.key("isCreatingThumb"));
	}

	@Override
	public boolean thumbnailCreationFailed() {
		return wrappedObject.getProperty(traits.key("thumbnailCreationFailed"));
	}

	@Override
	public Integer getWidth() {
		return wrappedObject.getProperty(traits.key("width"));
	}

	@Override
	public Integer getHeight() {
		return wrappedObject.getProperty(traits.key("height"));
	}

	@Override
	public Image getOriginalImage() {

		final NodeInterface node = wrappedObject.getProperty(traits.key("originalImage"));
		if (node != null) {

			return node.as(Image.class);
		}

		return null;
	}

	@Override
	public String getOriginalImageName() {

		final Integer tnWidth =  getWidth();
		final Integer tnHeight = getHeight();

		return StringUtils.stripEnd(getName(),  "_thumb_" + tnWidth + "x" + tnHeight);
	}

	@Override
	public Iterable<Image> getThumbnails() {

		final Iterable<NodeInterface> nodes = wrappedObject.getProperty(traits.key("thumbnails"));

		return Iterables.map(n -> n.as(Image.class), nodes);
	}

	@Override
	public Folder getThumbnailParentFolder(final Folder originalParentFolder, final SecurityContext securityContext) throws FrameworkException {

		final StringBuilder pathBuffer = new StringBuilder(Image.STRUCTR_THUMBNAIL_FOLDER);

		if (originalParentFolder != null) {
			pathBuffer.append(originalParentFolder.getPath());
		}

		final NodeInterface folder =  FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), pathBuffer.toString());

		if (!folder.isVisibleToAuthenticatedUsers() || !folder.isVisibleToPublicUsers()) {

			folder.setVisibility(true, true);

		}

		return folder.as(Folder.class);
	}

	@Override
	public Image getScaledImage(final String maxWidthString, final String maxHeightString) {
		return getScaledImage(Integer.parseInt(maxWidthString), Integer.parseInt(maxHeightString), false);
	}

	@Override
	public Image getScaledImage(final String maxWidthString, final String maxHeightString, final boolean cropToFit) {
		return getScaledImage(Integer.parseInt(maxWidthString), Integer.parseInt(maxHeightString), cropToFit);
	}

	@Override
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
	 * */
	@Override
	public Image getScaledImage(final int maxWidth, final int maxHeight, final boolean cropToFit) {

		if (isTemplate()) {

			return this;
		}

		final SecurityContext securityContext = wrappedObject.getSecurityContext();
		final String originalContentType      = getContentType();
		final Image existingThumbnail         = getExistingThumbnail(maxWidth, maxHeight, cropToFit);

		if (existingThumbnail != null) {

			return existingThumbnail;
		}

		// Do not create thumbnails if this transaction is set to read-only
		if (securityContext.isReadOnlyTransaction()) {

			return null;
		}

		// do not create thumbnails if thumbnail creation failed before
		if (Boolean.TRUE.equals(thumbnailCreationFailed())) {

			return null;
		}

		// Read Exif and GPS data from image and update properties
		ImageHelper.getExifData(this);

		// Request creation of thumbnail
		StructrApp.getInstance().processTasks(new ThumbnailTask(getUuid(), maxWidth, maxHeight, cropToFit));

		return null;
	}

	/**
	 * Return true if this image is a thumbnail image.
	 *
	 * This is determined by having at least one incoming THUMBNAIL relationship
	 *
	 * @return true if is thumbnail
	public boolean isThumbnail() {

		return getProperty(Image.isThumbnail) || getIncomingRelationship(Thumbnails.class) != null;
	}
	* */

	@Override
	public Image getExistingThumbnail(final int maxWidth, final int maxHeight, final boolean cropToFit) {

		final Iterable<RelationshipInterface> thumbnailRelationships = wrappedObject.getOutgoingRelationships("ImageTHUMBNAILImage");
		final Traits traits                                          = Traits.of("ImageTHUMBNAILImage");
		final List<String> toRemove                                  = new ArrayList<>();

		// Try to find an existing thumbnail that matches the specifications
		if (thumbnailRelationships != null) {

			for (final RelationshipInterface r : thumbnailRelationships) {

				final Integer w = r.getProperty(traits.key("maxWidth"));
				final Integer h = r.getProperty(traits.key("maxHeight"));
				final Boolean c = r.getProperty(traits.key("cropToFit"));

				if (w != null && h != null) {

					if ((w == maxWidth && h == maxHeight) && c == cropToFit) {

						final Image thumbnail = r.getTargetNode().as(Image.class);
						final Long checksum   = r.getProperty(traits.key("checksum"));

						// Check if existing thumbnail rel matches the correct checksum and mark as deprecated otherwise.
						// An empty checksum is probably only because the thumbnail generation task is not finished yet, so we assume everything is finde.
						if (checksum == null || checksum.equals(getChecksum())) {

							return thumbnail;

						} else {

							toRemove.add(thumbnail.getUuid());
						}
					}
				}

			}
		}

		// Queue deprecated thumbnails to be removed
		if (toRemove.size() > 0) {

			TransactionCommand.queuePostProcessProcedure(() -> deleteObsoleteThumbnails(toRemove));
		}

		return null;
	}

	// ----- private methods -----
	private static void deleteObsoleteThumbnails(final List<String> thumbnailUuids) {

		final App app       = StructrApp.getInstance();
		final Logger logger = LoggerFactory.getLogger(Image.class);

		try (final Tx tx = app.tx()) {

			for (final String uuid : thumbnailUuids) {

				final NodeInterface oldThumbnail = app.nodeQuery(StructrTraits.IMAGE).uuid(uuid).getFirst();
				if (oldThumbnail != null) {

					app.delete(oldThumbnail);
				}
			}

			tx.success();

		} catch (FrameworkException ex) {

			logger.error("Unable to delete deprecated thumbnail", ex);
		}
	}
}
