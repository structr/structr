/**
 * Copyright (C) 2010-2020 Structr GmbH
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
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.structr.api.graph.Cardinality;
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
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.schema.SchemaService;
import org.structr.api.schema.JsonMethod;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonSchema.Cascade;
import org.structr.web.common.FileHelper;
import org.structr.web.common.ImageHelper;
import org.structr.web.common.ImageHelper.Thumbnail;
import org.structr.web.property.ImageDataProperty;
import org.structr.web.property.ThumbnailProperty;

/**
 * An image whose binary data will be stored on disk.
 */
public interface Image extends File {

	final static String STRUCTR_THUMBNAIL_FOLDER = "._structr_thumbnails/";

	static class Impl { static {

		final JsonSchema schema    = SchemaService.getDynamicSchema();
		final JsonObjectType user  = schema.addType("User");
		final JsonObjectType image = schema.addType("Image");

		image.setImplements(URI.create("https://structr.org/v1.1/definitions/Image"));
		image.setExtends(URI.create("#/definitions/File"));
		image.setCategory("ui");

		image.addIntegerProperty("width",           PropertyView.Public, PropertyView.Ui).setIndexed(true);
		image.addIntegerProperty("height",          PropertyView.Public, PropertyView.Ui).setIndexed(true);
		image.addIntegerProperty("orientation",     PropertyView.Public, PropertyView.Ui).setIndexed(true);
		image.addStringProperty("exifIFD0Data",     PropertyView.Public, PropertyView.Ui);
		image.addStringProperty("exifSubIFDData",   PropertyView.Public, PropertyView.Ui);
		image.addStringProperty("gpsData",          PropertyView.Public, PropertyView.Ui);
		image.addBooleanProperty("isImage",         PropertyView.Public, PropertyView.Ui).setReadOnly(true).addTransformer(ConstantBooleanTrue.class.getName());
		image.addBooleanProperty("isThumbnail",     PropertyView.Public, PropertyView.Ui).setIndexed(true);
		image.addBooleanProperty("isCreatingThumb").setIndexed(true);

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
		image.overrideMethod("getThumbnailParentFolder", false, "final StringBuilder pathBuffer = new StringBuilder(" + Image.class.getName() + ".STRUCTR_THUMBNAIL_FOLDER); if (arg0 != null) { pathBuffer.append(arg0.getPath()); } return " + FileHelper.class.getName() + ".createFolderPath(SecurityContext.getSuperUserInstance(), pathBuffer.toString());");

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

		image.relate(image, "THUMBNAIL",  Cardinality.OneToMany, "originalImage", "thumbnails").setCascadingDelete(Cascade.sourceToTarget);
		image.relate(user,  "PICTURE_OF", Cardinality.OneToOne,  "img", "user");

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

		if ( !thisImage.isThumbnail() ) {

			if (modificationQueue.isPropertyModified(thisImage, name)) {

				final String newImageName = getName();

				for (Image tn : thisImage.getThumbnails()) {

					final String expectedThumbnailName = ImageHelper.getThumbnailName(newImageName, tn.getWidth(), tn.getHeight());
					final String currentThumbnailName  = tn.getName();

					if ( !expectedThumbnailName.equals(currentThumbnailName) ) {

						logger.debug("Auto-renaming Thumbnail({}) from '{}' to '{}'", tn.getUuid(), currentThumbnailName, expectedThumbnailName);
						tn.setProperty(AbstractNode.name, expectedThumbnailName);

					}
				}
			}
		}
	}

	/*

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

		final Class<Relation> thumbnailRel              = StructrApp.getConfiguration().getRelationshipEntityClass("ImageTHUMBNAILImage");
		final Iterable<Relation> thumbnailRelationships = thisImage.getOutgoingRelationships(thumbnailRel);
		final SecurityContext securityContext           = thisImage.getSecurityContext();
		final List<Image> oldThumbnails                 = new LinkedList<>();
		Image thumbnail                                 = null;
		final Image originalImage                       = thisImage;
		final Integer origWidth                         = originalImage.getWidth();
		final Integer origHeight                        = originalImage.getHeight();
		final Long currentChecksum                      = originalImage.getChecksum();
		Long newChecksum                                = 0L;

		if (currentChecksum == null || currentChecksum == 0) {

			try {

				newChecksum = FileHelper.getChecksum(originalImage.getFileOnDisk());

				if (newChecksum == null || newChecksum == 0) {

					logger.warn("Unable to calculate checksum of {}", originalImage.getName());
					return null;
				}

			} catch (IOException ex) {
				logger.warn("Unable to calculate checksum of {}: {}", originalImage.getName(), ex.getMessage());
			}

		} else {

			newChecksum = currentChecksum;
		}

		// Read Exif and GPS data from image and update properties
		ImageHelper.getExifData(originalImage);

		// Return self if SVG image
		final String _contentType = thisImage.getContentType();
		if (_contentType != null && (_contentType.startsWith("image/svg") || (_contentType.startsWith("image/") && _contentType.endsWith("icon")))) {

			return thisImage;
		}

		if (origWidth != null && origHeight != null && thumbnailRelationships != null) {

			for (final Relation r : thumbnailRelationships) {

				final Integer w = r.getProperty(StructrApp.key(Image.class, "width"));
				final Integer h = r.getProperty(StructrApp.key(Image.class, "height"));

				if (w != null && h != null) {

					// orginal image is equal or smaller than requested size
					if (((w == maxWidth) && (h <= maxHeight)) || ((w <= maxWidth) && (h == maxHeight)) || ((origWidth <= w) && (origHeight <= h))) {

						thumbnail = (Image)r.getTargetNode();

						// Use thumbnail only if checksum of original image matches with stored checksum
						final Long storedChecksum = r.getProperty(StructrApp.key(Image.class, "checksum"));

						if (storedChecksum != null && storedChecksum.equals(newChecksum)) {

							return thumbnail;

						} else {

							oldThumbnails.add(thumbnail);
						}
					}

				}

			}

		}

		// do not create thumbnails if this transaction is set to read-only
		if (securityContext.isReadOnlyTransaction()) {
			return null;
		}

		if (originalImage.getIsCreatingThumb()) {

			logger.debug("Another thumbnail is being created - waiting....");

		} else {

			try {

				// No thumbnail exists, or thumbnail was too old, so let's create a new one
				logger.debug("Creating thumbnail for {} (w={} h={} crop={})", new Object[] { getName(), maxWidth, maxHeight, cropToFit });

				originalImage.unlockSystemPropertiesOnce();
				originalImage.setIsCreatingThumb(true);

				final App app = StructrApp.getInstance();

				originalImage.unlockSystemPropertiesOnce();
				originalImage.setProperty(StructrApp.key(File.class, "checksum"), newChecksum);

				final Thumbnail thumbnailData = ImageHelper.createThumbnail(originalImage, maxWidth, maxHeight, cropToFit);
				if (thumbnailData != null) {

					final Integer tnWidth  = thumbnailData.getWidth();
					final Integer tnHeight = thumbnailData.getHeight();
					byte[] data            = null;

					try {

						data = thumbnailData.getBytes();
						final String thumbnailName = ImageHelper.getThumbnailName(originalImage.getName(), tnWidth, tnHeight);

						// create thumbnail node
						thumbnail = ImageHelper.createImageNode(securityContext, data, "image/" + Thumbnail.defaultFormat, Image.class, thumbnailName, true);

					} catch (IOException ex) {

						logger.warn("Could not create thumbnail image for " + getUuid(), ex);

					}

					if (thumbnail != null && data != null) {

						// Create a thumbnail relationship
						final PropertyMap relProperties = new PropertyMap();
						relProperties.put(StructrApp.key(Image.class, "width"),                  tnWidth);
						relProperties.put(StructrApp.key(Image.class, "height"),                 tnHeight);
						relProperties.put(StructrApp.key(Image.class, "checksum"),               newChecksum);

						app.create(originalImage, thumbnail, thumbnailRel, relProperties);

						final PropertyMap properties = new PropertyMap();
						properties.put(StructrApp.key(Image.class, "width"),                              tnWidth);
						properties.put(StructrApp.key(Image.class, "height"),                             tnHeight);
						properties.put(StructrApp.key(AbstractNode.class, "hidden"),                      originalImage.getProperty(AbstractNode.hidden));
						properties.put(StructrApp.key(AbstractNode.class, "visibleToAuthenticatedUsers"), originalImage.getProperty(AbstractNode.visibleToAuthenticatedUsers));
						properties.put(StructrApp.key(AbstractNode.class, "visibleToPublicUsers"),        originalImage.getProperty(AbstractNode.visibleToPublicUsers));
						properties.put(StructrApp.key(File.class, "size"),                                Long.valueOf(data.length));
						properties.put(StructrApp.key(AbstractNode.class, "owner"),                       originalImage.getProperty(AbstractNode.owner));
						properties.put(StructrApp.key(File.class, "parent"),                              originalImage.getThumbnailParentFolder(originalImage.getProperty(StructrApp.key(File.class, "parent")), securityContext));
						properties.put(StructrApp.key(File.class, "hasParent"),                           originalImage.getProperty(StructrApp.key(Image.class, "hasParent")));

						thumbnail.unlockSystemPropertiesOnce();
						thumbnail.setProperties(securityContext, properties);

						// Delete outdated thumbnails
						for (final Image tn : oldThumbnails) {
							app.delete(tn);
						}

					}

				} else {

					logger.debug("Could not create thumbnail for image {} ({})", getName(), getUuid());

				}

				originalImage.unlockSystemPropertiesOnce();
				originalImage.setIsCreatingThumb(false);

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
}
