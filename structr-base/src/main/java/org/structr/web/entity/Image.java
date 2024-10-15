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
package org.structr.web.entity;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonMethod;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonSchema.Cascade;
import org.structr.common.ConstantBooleanTrue;
import org.structr.common.Permission;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.core.property.*;
import org.structr.schema.SchemaService;
import org.structr.web.agent.ThumbnailTask;
import org.structr.web.common.FileHelper;
import org.structr.web.common.ImageHelper;
import org.structr.web.entity.relationship.FolderCONTAINSImage;
import org.structr.web.entity.relationship.ImagePICTURE_OFUser;
import org.structr.web.entity.relationship.ImageTHUMBNAILImage;
import org.structr.web.property.ImageDataProperty;
import org.structr.web.property.ThumbnailProperty;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * An image whose binary data will be stored on disk.
 */
public interface Image extends File {

	Property<Folder> imageParentProperty         = new StartNode<>("imageParent", FolderCONTAINSImage.class).partOfBuiltInSchema();
	Property<User> imageOfUser                   = new EndNode<>("imageOfUser", ImagePICTURE_OFUser.class).partOfBuiltInSchema();
	Property<Iterable<Image>> thumbnailsProperty = new EndNodes<>("thumbnails", ImageTHUMBNAILImage.class).partOfBuiltInSchema();
	Property<Image> originalImageProperty        = new StartNode<>("originalImage", ImageTHUMBNAILImage.class).partOfBuiltInSchema();

	final static String STRUCTR_THUMBNAIL_FOLDER = "._structr_thumbnails/";

	static class Impl { static {

		final JsonSchema schema    = SchemaService.getDynamicSchema();
		final JsonObjectType image = schema.addType("Image");

		image.setImplements(URI.create("https://structr.org/v1.1/definitions/Image"));
		image.setExtends(URI.create("#/definitions/File"));
		image.setCategory("ui");

		image.addIntegerProperty("width",           PropertyView.Public, PropertyView.Ui);
		image.addIntegerProperty("height",          PropertyView.Public, PropertyView.Ui);
		image.addIntegerProperty("orientation",     PropertyView.Public, PropertyView.Ui);
		image.addStringProperty("exifIFD0Data",     PropertyView.Public, PropertyView.Ui);
		image.addStringProperty("exifSubIFDData",   PropertyView.Public, PropertyView.Ui);
		image.addStringProperty("gpsData",          PropertyView.Public, PropertyView.Ui);
		image.addBooleanProperty("isImage",         PropertyView.Public, PropertyView.Ui).setReadOnly(true).addTransformer(ConstantBooleanTrue.class.getName());
		image.addBooleanProperty("isThumbnail",     PropertyView.Public, PropertyView.Ui).setIndexed(true);
		image.addBooleanProperty("isCreatingThumb").setIndexed(true);
		image.addBooleanProperty("thumbnailCreationFailed");

		image.addCustomProperty("imageData", ImageDataProperty.class.getName()).setTypeHint("String");
		image.addCustomProperty("tnSmall",   ThumbnailProperty.class.getName(), PropertyView.Public, PropertyView.Ui).setTypeHint("Image").setFormat("100, 100, false");
		image.addCustomProperty("tnMid",     ThumbnailProperty.class.getName(), PropertyView.Public, PropertyView.Ui).setTypeHint("Image").setFormat("300, 300, false");

		image.addPropertyGetter("isCreatingThumb", Boolean.TYPE);
		image.addPropertySetter("isCreatingThumb", Boolean.TYPE);
		image.addPropertyGetter("originalImage",   Image.class);
		image.addPropertyGetter("thumbnails",      Iterable.class);

		image.addPropertyGetter("width",       Integer.class);
		image.addPropertySetter("width",       Integer.class);
		image.addPropertyGetter("height",      Integer.class);
		image.addPropertySetter("height",      Integer.class);

		// TODO: sysinternal and unvalidated properties are not possible right now
		image.overrideMethod("isImage",              false, "return getProperty(isImageProperty);");
		image.overrideMethod("isThumbnail",          false, "return getProperty(isThumbnailProperty);");
		image.overrideMethod("getOriginalImageName", false, "return " + Image.class.getName() + ".getOriginalImageName(this);");
		image.overrideMethod("setProperty",          true,  "return " + Image.class.getName() + ".setProperty(this, arg0, arg1);");
		image.overrideMethod("onModification",       true,  Image.class.getName() + ".onModification(this, arg0, arg1, arg2);");
		image.overrideMethod("setProperties",        true,  Image.class.getName() + ".setProperties(this, arg0, arg1);");
		image.overrideMethod("isGranted",            false, "if (this.isThumbnail()) { final org.structr.web.entity.Image originalImage = getOriginalImage(); if (originalImage != null) { return originalImage.isGranted(arg0, arg1); } } return super.isGranted(arg0, arg1);");
		image.overrideMethod("getThumbnailParentFolder", false, "final StringBuilder pathBuffer = new StringBuilder(" + Image.class.getName() + ".STRUCTR_THUMBNAIL_FOLDER); if (arg0 != null) { pathBuffer.append(arg0.getPath()); } final " + Folder.class.getName() + " folder =  " + FileHelper.class.getName() + ".createFolderPath(SecurityContext.getSuperUserInstance(), pathBuffer.toString()); if (!folder.isVisibleToAuthenticatedUsers() || !folder.isVisibleToPublicUsers()) { folder.setProperty(AbstractNode.visibleToAuthenticatedUsers, true); folder.setProperty(AbstractNode.visibleToPublicUsers, true);  } return folder;");

		final JsonMethod getScaledImage1 = image.addMethod("getScaledImage");
		getScaledImage1.setReturnType(Image.class.getName());
		getScaledImage1.setSource("return "+ Image.class.getName() + ".getScaledImage(this, arg0, arg1);");
		getScaledImage1.addParameter("arg0", "String");
		getScaledImage1.addParameter("arg1", "String");

		final JsonMethod getScaledImage2 = image.addMethod("getScaledImage");
		getScaledImage2.setReturnType(Image.class.getName());
		getScaledImage2.setSource("return "+ Image.class.getName() + ".getScaledImage(this, arg0, arg1, arg2);");
		getScaledImage2.addParameter("arg0", "String");
		getScaledImage2.addParameter("arg1", "String");
		getScaledImage2.addParameter("arg2", "boolean");

		final JsonMethod getScaledImage3 = image.addMethod("getScaledImage");
		getScaledImage3.setReturnType(Image.class.getName());
		getScaledImage3.setSource("return "+ Image.class.getName() + ".getScaledImage(this, arg0, arg1);");
		getScaledImage3.addParameter("arg0", "int");
		getScaledImage3.addParameter("arg1", "int");

		final JsonMethod getScaledImage4 = image.addMethod("getScaledImage");
		getScaledImage4.setReturnType(Image.class.getName());
		getScaledImage4.setSource("return "+ Image.class.getName() + ".getScaledImage(this, arg0, arg1, arg2);");
		getScaledImage4.addParameter("arg0", "int");
		getScaledImage4.addParameter("arg1", "int");
		getScaledImage4.addParameter("arg2", "boolean");

		// view configuration
		image.addViewProperty(PropertyView.Public, "parent");
	}}

	void setIsCreatingThumb(final boolean isCreatingThumb) throws FrameworkException;

	boolean isImage();
	boolean isThumbnail();
	boolean getIsCreatingThumb();

	Integer getWidth();
	Integer getHeight();

	Image getOriginalImage();
	String getOriginalImageName();

	Image getScaledImage(final String maxWidthString, final String maxHeightString);
	Image getScaledImage(final String maxWidthString, final String maxHeightString, final boolean cropToFit);

	Image getScaledImage(final int maxWidth, final int maxHeight);
	Image getScaledImage(final int maxWidth, final int maxHeight, final boolean cropToFit);

	Iterable<Image> getThumbnails();

	Folder getThumbnailParentFolder(final Folder originalParentFolder, final SecurityContext securityContext) throws FrameworkException;

	public static boolean isGranted(final Image thisImage, final Permission permission, final SecurityContext context) {

		if (thisImage.isThumbnail()) {

			final Image originalImage = thisImage.getOriginalImage();
			if (originalImage != null) {

				return originalImage.isGranted(permission, context);
			}
		}

		return thisImage.isGranted(permission, context);
	}

	public static Object setProperty(final Image thisImage, final PropertyKey key, final Object value) throws FrameworkException {

		// Copy visibility properties and owner to all thumbnails
		if (visibleToPublicUsers.equals(key) || visibleToAuthenticatedUsers.equals(key) || owner.equals(key)) {

			for (Image tn : thisImage.getThumbnails()) {

				if (!tn.getUuid().equals(thisImage.getUuid())) {

					tn.setProperty(key, value);
				}
			}
		}

		return null;
	}

	public static void setProperties(final Image thisImage, final SecurityContext securityContext, final PropertyMap properties) throws FrameworkException {

		if ( !thisImage.isThumbnail() ) {

			final PropertyMap propertiesCopiedToAllThumbnails = new PropertyMap();

			for (final PropertyKey key : properties.keySet()) {

					if (visibleToPublicUsers.equals(key) || visibleToAuthenticatedUsers.equals(key) || owner.equals(key)) {

						propertiesCopiedToAllThumbnails.put(key, properties.get(key));
					}
			}

			if ( !propertiesCopiedToAllThumbnails.isEmpty() ) {

				for (final Image tn : thisImage.getThumbnails()) {

					if (!tn.getUuid().equals(thisImage.getUuid())) {

						tn.setProperties(tn.getSecurityContext(), propertiesCopiedToAllThumbnails);
					}
				}
			}
		}
	}

	public static void onModification(final Image thisImage, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		if ( !thisImage.isThumbnail() && !thisImage.isTemplate() ) {

			if (modificationQueue.isPropertyModified(thisImage, name)) {

				final String newImageName = thisImage.getName();

				for (Image tn : thisImage.getThumbnails()) {

					final String expectedThumbnailName = ImageHelper.getThumbnailName(newImageName, tn.getWidth(), tn.getHeight());
					final String currentThumbnailName  = tn.getName();

					if ( !expectedThumbnailName.equals(currentThumbnailName) ) {

						final Logger logger = LoggerFactory.getLogger(Image.class);
						logger.debug("Auto-renaming Thumbnail({}) from '{}' to '{}'", tn.getUuid(), currentThumbnailName, expectedThumbnailName);
						tn.setProperty(AbstractNode.name, expectedThumbnailName);

					}
				}
			}
		}
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
	public static Image getScaledImage(final Image thisImage, final String maxWidthString, final String maxHeightString) {
		return getScaledImage(thisImage, Integer.parseInt(maxWidthString), Integer.parseInt(maxHeightString), false);
	}

	public static Image getScaledImage(final Image thisImage, final String maxWidthString, final String maxHeightString, final boolean cropToFit) {
		return getScaledImage(thisImage, Integer.parseInt(maxWidthString), Integer.parseInt(maxHeightString), cropToFit);
	}

	public static Image getScaledImage(final Image thisImage, final int maxWidth, final int maxHeight) {
		return getScaledImage(thisImage, maxWidth, maxHeight, false);
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
	public static Image getScaledImage(final Image thisImage, final int maxWidth, final int maxHeight, final boolean cropToFit) {
		if (thisImage.isTemplate()) {

			return thisImage;
		}

		final SecurityContext securityContext           = thisImage.getSecurityContext();
		final String originalContentType                = thisImage.getContentType();
		final Image existingThumbnail                   = getExistingThumbnail(thisImage, maxWidth, maxHeight, cropToFit);

		if (existingThumbnail != null) {

			return existingThumbnail;
		}

		// Do not create thumbnails if this transaction is set to read-only
		if (securityContext.isReadOnlyTransaction()) {

			return null;
		}

		// do not create thumbnails if thumbnail creation failed before
		if (Boolean.TRUE.equals(thisImage.getProperty(StructrApp.key(Image.class, "thumbnailCreationFailed")))) {

			return null;
		}

		// Read Exif and GPS data from image and update properties
		ImageHelper.getExifData(thisImage);

		// Request creation of thumbnail
		StructrApp.getInstance().processTasks(new ThumbnailTask(thisImage.getUuid(), maxWidth, maxHeight, cropToFit));

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

	/**
	 * @return the name of the original image
	 */
	public static String getOriginalImageName(final Image thisImage) {

		final Integer tnWidth =  thisImage.getWidth();
		final Integer tnHeight = thisImage.getHeight();

		return StringUtils.stripEnd(thisImage.getName(),  "_thumb_" + tnWidth + "x" + tnHeight);
	}

	public static Image getExistingThumbnail(final Image thisImage, final int maxWidth, final int maxHeight, final boolean cropToFit) {
		final Class<Relation> thumbnailRel              = StructrApp.getConfiguration().getRelationshipEntityClass("ImageTHUMBNAILImage");
		final Iterable<Relation> thumbnailRelationships = thisImage.getOutgoingRelationships(thumbnailRel);
		final List<String> deprecatedThumbnails         = new ArrayList<>();

		// Try to find an existing thumbnail that matches the specifications
		if (thumbnailRelationships != null) {

			for (final Relation r : thumbnailRelationships) {

				final Integer w = r.getProperty(new IntProperty("maxWidth"));
				final Integer h = r.getProperty(new IntProperty("maxHeight"));
				final Boolean c = r.getProperty(new BooleanProperty("cropToFit"));

				if (w != null && h != null) {

					if ((w == maxWidth && h == maxHeight) && c == cropToFit) {

						//FIXME: Implement deletion of mismatching thumbnails, since they have become obsolete
						final Image thumbnail = (Image) r.getTargetNode();

						final Long checksum = r.getProperty(StructrApp.key(Image.class, "checksum"));

						// Check if existing thumbnail rel matches the correct checksum and mark as deprecated otherwise.
						// An empty checksum is probably only because the thumbnail generation task is not finished yet, so we assume everything is finde.
						if (checksum == null || checksum.equals(thisImage.getChecksum())) {

							return thumbnail;
						} else {

							deprecatedThumbnails.add(thumbnail.getUuid());
						}
					}
				}

			}
		}

		// Queue deprecated thumbnails to be removed
		if (deprecatedThumbnails.size() > 0) {

			TransactionCommand.queuePostProcessProcedure(() -> deleteDeprecatedThumbnails(deprecatedThumbnails));
		}

		return null;
	}

	/** Private Methods **/

	private static void deleteDeprecatedThumbnails(final List<String> thumbnailUuids) {

		final App app = StructrApp.getInstance();
		final Logger logger = LoggerFactory.getLogger(Image.class);

		try (final Tx tx = app.tx()) {

			for (final String uuid : thumbnailUuids) {

				final Image oldThumbnail = app.nodeQuery(Image.class).uuid(uuid).getFirst();

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
