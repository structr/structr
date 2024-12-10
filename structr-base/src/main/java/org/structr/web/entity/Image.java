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
