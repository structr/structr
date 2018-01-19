/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import java.net.URI;
import java.util.List;
import org.structr.common.ConstantBooleanTrue;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Relation.Cardinality;
import org.structr.schema.SchemaService;
import org.structr.schema.json.JsonMethod;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonSchema.Cascade;
import org.structr.web.property.ImageDataProperty;
import org.structr.web.property.ThumbnailProperty;

/**
 * An image whose binary data will be stored on disk.
 */
public interface Image extends File {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("Image");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Image"));
		type.setExtends(URI.create("#/definitions/File"));

		type.addIntegerProperty("width",           PropertyView.Public).setIndexed(true);
		type.addIntegerProperty("height",          PropertyView.Public).setIndexed(true);
		type.addIntegerProperty("orientation",     PropertyView.Public).setIndexed(true);
		type.addStringProperty("exifIFD0Data",     PropertyView.Public).setIndexed(true);
		type.addStringProperty("exifSubIFDData",   PropertyView.Public).setIndexed(true);
		type.addStringProperty("gpsData",          PropertyView.Public).setIndexed(true);
		type.addBooleanProperty("isImage",         PropertyView.Public).setReadOnly(true).addTransformer(ConstantBooleanTrue.class.getName());
		type.addBooleanProperty("isThumbnail",     PropertyView.Public).setIndexed(true);
		type.addBooleanProperty("isCreatingThumb", PropertyView.Public).setIndexed(true);

		type.addCustomProperty("imageData", ImageDataProperty.class.getName());
		type.addCustomProperty("tnSmall",   ThumbnailProperty.class.getName(), PropertyView.Public).setFormat("100, 100, false");
		type.addCustomProperty("tnMid",     ThumbnailProperty.class.getName(), PropertyView.Public).setFormat("300, 300, false");

		type.addPropertyGetter("isCreatingThumb", Boolean.TYPE);
		type.addPropertySetter("isCreatingThumb", Boolean.TYPE);
		type.addPropertyGetter("thumbnails",      List.class);

		type.addPropertyGetter("width",       Integer.class);
		type.addPropertySetter("width",       Integer.class);
		type.addPropertyGetter("height",      Integer.class);
		type.addPropertySetter("height",      Integer.class);

		// TODO: sysinternal and unvalidated properties are not possible right now
		type.overrideMethod("isImage",              false, "return getProperty(isImageProperty);");
		type.overrideMethod("isThumbnail",          false, "return getProperty(isThumbnailProperty);");
		type.overrideMethod("getOriginalImageName", false, "return " + Image.class.getName() + ".getOriginalImageName(this);");
		type.overrideMethod("setProperty",          true,  "return " + Image.class.getName() + ".setProperty(this, arg0, arg1);");
		type.overrideMethod("onModification",       true,  Image.class.getName() + ".onModification(this, arg0, arg1, arg2);");
		type.overrideMethod("setProperties",        true,  Image.class.getName() + ".setProperties(this, arg0, arg1);");

		final JsonMethod getScaledImage1 = type.addMethod("getScaledImage");
		getScaledImage1.setReturnType(Image.class.getName());
		getScaledImage1.setSource("return "+ Image.class.getName() + ".getScaledImage(this, arg0, arg1);");
		getScaledImage1.addParameter("arg0", "String");
		getScaledImage1.addParameter("arg1", "String");

		final JsonMethod getScaledImage2 = type.addMethod("getScaledImage");
		getScaledImage2.setReturnType(Image.class.getName());
		getScaledImage2.setSource("return "+ Image.class.getName() + ".getScaledImage(this, arg0, arg1, arg2);");
		getScaledImage2.addParameter("arg0", "String");
		getScaledImage2.addParameter("arg1", "String");
		getScaledImage2.addParameter("arg2", "boolean");

		final JsonMethod getScaledImage3 = type.addMethod("getScaledImage");
		getScaledImage3.setReturnType(Image.class.getName());
		getScaledImage3.setSource("return "+ Image.class.getName() + ".getScaledImage(this, arg0, arg1);");
		getScaledImage3.addParameter("arg0", "int");
		getScaledImage3.addParameter("arg1", "int");

		final JsonMethod getScaledImage4 = type.addMethod("getScaledImage");
		getScaledImage4.setReturnType(Image.class.getName());
		getScaledImage4.setSource("return "+ Image.class.getName() + ".getScaledImage(this, arg0, arg1, arg2);");
		getScaledImage4.addParameter("arg0", "int");
		getScaledImage4.addParameter("arg1", "int");
		getScaledImage4.addParameter("arg2", "boolean");

		type.relate(type, "THUMBNAIL", Cardinality.OneToMany, "originalImage", "thumbnails").setCascadingDelete(Cascade.sourceToTarget);
	}}

	void setIsCreatingThumb(final boolean isCreatingThumb) throws FrameworkException;

	boolean isImage();
	boolean isThumbnail();
	boolean getIsCreatingThumb();

	Integer getWidth();
	Integer getHeight();

	String getOriginalImageName();

	Image getScaledImage(final String maxWidthString, final String maxHeightString);
	Image getScaledImage(final String maxWidthString, final String maxHeightString, final boolean cropToFit);

	Image getScaledImage(final int maxWidth, final int maxHeight);
	Image getScaledImage(final int maxWidth, final int maxHeight, final boolean cropToFit);

	List<Image> getThumbnails();

	/*

	private static final String STRUCTR_THUMBNAIL_FOLDER = "._structr_thumbnails/";

	// register this type as an overridden builtin type
	static {

		SchemaService.registerBuiltinTypeOverride("Image", Image.class.getName());
	}

	private static final Logger logger                            = LoggerFactory.getLogger(Image.class.getName());

	public static final Property<Integer> height                  = new IntProperty("height").cmis().indexed();
	public static final Property<Integer> width                   = new IntProperty("width").cmis().indexed();

	public static final Property<Integer> orientation             = new IntProperty("orientation").cmis().indexed();
	public static final Property<String>  exifIFD0Data            = new StringProperty("exifIFD0Data").cmis().indexed();
	public static final Property<String>  exifSubIFDData          = new StringProperty("exifSubIFDData").cmis().indexed();
	public static final Property<String>  gpsData                 = new StringProperty("gpsData").cmis().indexed();

	public static final Property<Image> tnSmall                   = new ThumbnailProperty("tnSmall").format("100, 100, false");
	public static final Property<Image> tnMid                     = new ThumbnailProperty("tnMid").format("300, 300, false");

	public static final Property<Boolean> isThumbnail             = new BooleanProperty("isThumbnail").indexed().unvalidated().systemInternal();
	public static final ImageDataProperty imageData               = new ImageDataProperty("imageData");

	public static final Property<Boolean> isImage                 = new ConstantBooleanProperty("isImage", true);

	public static final Property<Boolean> isCreatingThumb         = new BooleanProperty("isCreatingThumb").systemInternal();

	public static final org.structr.common.View uiView            = new org.structr.common.View(Image.class, PropertyView.Ui, type, name, contentType, size, width, height, orientation, exifIFD0Data, exifSubIFDData, gpsData, tnSmall, tnMid, isThumbnail, owner, parent, path, isImage);
	public static final org.structr.common.View publicView        = new org.structr.common.View(Image.class, PropertyView.Public, type, name, width, height, orientation, exifIFD0Data, exifSubIFDData, gpsData, tnSmall, tnMid, isThumbnail, owner, parent, path, isImage);

	@Override
	public Object setProperty(final PropertyKey key, final Object value) throws FrameworkException {

		// Copy visibility properties and owner to all thumbnails
		if (visibleToPublicUsers.equals(key) ||
			visibleToAuthenticatedUsers.equals(key) ||
			visibilityStartDate.equals(key) ||
			visibilityEndDate.equals(key) ||
			owner.equals(key)) {

			for (Image tn : getThumbnails()) {

				if (!tn.getUuid().equals(getUuid())) {
					tn.setProperty(key, value);
				} else {
//					logger.info("Ignoring recursive setProperty for thumbnail where image is its own thumbnail");
				}

			}

		}

		return super.setProperty(key, value);
	}

	@Override
	public void setProperties(final SecurityContext securityContext, final PropertyMap properties) throws FrameworkException {

		if ( !isThumbnail() ) {

			final PropertyMap propertiesCopiedToAllThumbnails = new PropertyMap();

			for (final PropertyKey key : properties.keySet()) {

					if (visibleToPublicUsers.equals(key) ||
						visibleToAuthenticatedUsers.equals(key) ||
						visibilityStartDate.equals(key) ||
						visibilityEndDate.equals(key) ||
						owner.equals(key)) {

						propertiesCopiedToAllThumbnails.put(key, properties.get(key));
					}
			}

			if ( !propertiesCopiedToAllThumbnails.isEmpty() ) {

				final List<Image> thumbnails = getThumbnails();

				for (Image tn : thumbnails) {

					if (!tn.getUuid().equals(getUuid())) {
						tn.setProperties(tn.getSecurityContext(), propertiesCopiedToAllThumbnails);
					} else {
//						logger.info("Ignoring recursive setProperty for thumbnail where image is its own thumbnail");
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

				if (modificationQueue.isPropertyModified(this, name)) {

					final String newImageName = getName();

					for (Image tn : getThumbnails()) {

						final String expectedThumbnailName = ImageHelper.getThumbnailName(newImageName, tn.getWidth(), tn.getHeight());
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
	public Image getScaledImage(final int maxWidth, final int maxHeight, final boolean cropToFit) {

		final Iterable<Thumbnails> thumbnailRelationships = getThumbnailRelationships();
		final List<Image> oldThumbnails                   = new LinkedList<>();
		Image thumbnail                                   = null;
		final Image originalImage                         = this;
		final Integer origWidth                           = originalImage.getWidth();
		final Integer origHeight                          = originalImage.getHeight();
		final Long currentChecksum                        = originalImage.getProperty(Image.checksum);
		Long newChecksum                                  = 0L;

		if (currentChecksum == null || currentChecksum == 0) {

			try {

				newChecksum = FileHelper.getChecksum(originalImage.getFileOnDisk());

				if (newChecksum == null || newChecksum == 0) {

					logger.debug("Unable to create scaled image, file {} is not ready.", originalImage.getName());
					return null;
				}
			} catch (IOException ioex) {
				logger.warn("Unable to calculate checksum of {}: {}", originalImage.getFileOnDisk(), ioex.getMessage());
			}

		} else {

			newChecksum = currentChecksum;
		}

		// Read Exif and GPS data from image and update properties
		ImageHelper.getExifData(originalImage);

		// Return self if SVG image
		final String _contentType = getProperty(Image.contentType);
		if (_contentType != null && (_contentType.startsWith("image/svg") || (_contentType.startsWith("image/") && _contentType.endsWith("icon")))) {

			return this;
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
						final String thumbnailName = ImageHelper.getThumbnailName(originalImage.getName(), tnWidth, tnHeight);

						// create thumbnail node
						thumbnail = ImageHelper.createImageNode(securityContext, data, "image/" + Thumbnail.defaultFormat, Image.class, thumbnailName, true);

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
						properties.put(File.size,                                Long.valueOf(data.length));
						properties.put(AbstractNode.owner,                       originalImage.getProperty(AbstractNode.owner));
						properties.put(File.parent,                              getThumbnailParentFolder(originalImage.getProperty(Folder.parent)));
						properties.put(File.hasParent,                           originalImage.getProperty(File.hasParent));

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
	public boolean isThumbnail() {

		return getProperty(Image.isThumbnail) || getIncomingRelationship(Thumbnails.class) != null;
	}

	/**
	 * @return the name of the original image
	public String getOriginalImageName() {

		final Integer tnWidth =  getWidth();
		final Integer tnHeight = getHeight();

		return StringUtils.stripEnd(getName(),  "_thumb_" + tnWidth + "x" + tnHeight);
	}

	// ----- private methods -----
	private Folder getThumbnailParentFolder(final Folder originalParentFolder) throws FrameworkException {

		final StringBuilder pathBuffer = new StringBuilder(STRUCTR_THUMBNAIL_FOLDER);

		if (originalParentFolder != null) {

			pathBuffer.append(originalParentFolder.getPath());

		}

		return FileHelper.createFolderPath(securityContext, pathBuffer.toString());
	}
	*/
}
