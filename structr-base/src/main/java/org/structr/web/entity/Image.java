/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.web.entity;

import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;

public interface Image extends File {

	String STRUCTR_THUMBNAIL_FOLDER = "._structr_thumbnails/";

	void setIsCreatingThumb(final boolean isCreatingThumb) throws FrameworkException;
	boolean isImage();
	boolean isThumbnail();
	boolean getIsCreatingThumb();
	boolean thumbnailCreationFailed();
	Integer getWidth();
	Integer getHeight();
	Image getOriginalImage();
	String getOriginalImageName();
	Image getExistingThumbnail(int maxWidth, int maxHeight, boolean cropToFit);
	Image getScaledImage(final String maxWidthString, final String maxHeightString);
	Image getScaledImage(final String maxWidthString, final String maxHeightString, final boolean cropToFit);
	Image getScaledImage(final int maxWidth, final int maxHeight);
	Image getScaledImage(final int maxWidth, final int maxHeight, final boolean cropToFit);
	Iterable<Image> getThumbnails();
	Folder getThumbnailParentFolder(final Folder originalParentFolder, final SecurityContext securityContext) throws FrameworkException;

	static boolean isGranted(Image thisImage, Permission permission, SecurityContext context) {

		if (thisImage.isThumbnail()) {

			final Image originalImage = thisImage.getOriginalImage();
			if (originalImage != null) {

				return originalImage.isGranted(permission, context);
			}
		}

		return thisImage.isGranted(permission, context);
	}

}
