/*
 * Copyright (C) 2010-2023 Structr GmbH
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
package org.structr.web.common;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.twelvemonkeys.image.AffineTransformOp;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PathHelper;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Relation;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.storage.StorageProviderFactory;
import org.structr.util.Base64;
import org.structr.web.entity.File;
import org.structr.web.entity.Image;
import org.structr.web.property.ThumbnailProperty;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public abstract class ImageHelper extends FileHelper {

	private static final Logger logger = LoggerFactory.getLogger(ImageHelper.class.getName());

	/**
	 * Create a new image node from the given image data
	 *
	 * @param securityContext
	 * @param imageStream
	 * @param contentType
	 * @param imageType defaults to Image.class if null
	 * @param name
	 * @param markAsThumbnail
	 * @return image
	 * @throws FrameworkException
	 * @throws IOException
	 */
	public static Image createImage(final SecurityContext securityContext, final InputStream imageStream, final String contentType, final Class<? extends Image> imageType, final String name, final boolean markAsThumbnail)
			throws FrameworkException, IOException {

		final PropertyMap props = new PropertyMap();

		props.put(AbstractNode.type, imageType == null ? Image.class.getSimpleName() : imageType.getSimpleName());
		props.put(StructrApp.key(Image.class, "isThumbnail"), markAsThumbnail);
		props.put(AbstractNode.name, name);

		final Image newImage = StructrApp.getInstance(securityContext).create(imageType, props);
		setFileData(newImage, imageStream, contentType);

		return newImage;

	}

	/**
	 * Create a new image node from the given image data
	 *
	 * @param securityContext
	 * @param imageData
	 * @param contentType
	 * @param imageType defaults to Image.class if null
	 * @param name
	 * @param markAsThumbnail
	 * @return image
	 * @throws FrameworkException
	 * @throws IOException
	 */
	public static Image createImageNode(final SecurityContext securityContext, final byte[] imageData, final String contentType, final Class<? extends Image> imageType, final String name, final boolean markAsThumbnail)
		throws FrameworkException, IOException {

		final PropertyMap props = new PropertyMap();

		props.put(AbstractNode.type, imageType == null ? Image.class.getSimpleName() : imageType.getSimpleName());
		props.put(StructrApp.key(Image.class, "isThumbnail"), markAsThumbnail);
		props.put(AbstractNode.name, name);

		final Image newImage = StructrApp.getInstance(securityContext).create(imageType, props);

		if (imageData != null && imageData.length > 0) {

			setFileData(newImage, imageData, contentType);
		}

		return newImage;

	}

	/**
	 * Write image data to the given image node and set checksum and size.
	 *
	 * @param img
	 * @param imageData
	 * @param contentType
	 * @throws FrameworkException
	 * @throws IOException
	 */
	public static void setImageData(final Image img, final byte[] imageData, final String contentType)
		throws FrameworkException, IOException {

		setFileData(img, imageData, contentType);
	}

	public static void findAndReconnectThumbnails(final Image originalImage) {

		final Class<Relation> thumbnailRel  = StructrApp.getConfiguration().getRelationshipEntityClass("ImageTHUMBNAILImage");
		final PropertyKey<Image> tnSmallKey = StructrApp.key(Image.class, "tnSmall");
		final PropertyKey<Image> tnMidKey   = StructrApp.key(Image.class, "tnMid");
		final PropertyKey<String> pathKey   = StructrApp.key(Image.class, "path");
		final App app                       = StructrApp.getInstance();

		final Integer origWidth  = originalImage.getWidth();
		final Integer origHeight = originalImage.getHeight();

		if (origWidth == null || origHeight == null) {

			if (!Arrays.asList("image/svg+xml", "image/x-icon", "image/x-photoshop").contains(originalImage.getContentType())) {
				logger.info("Could not determine width and height for {}", originalImage.getName());
			}

			return;
		}

		for (ThumbnailProperty tnProp : new ThumbnailProperty[]{ (ThumbnailProperty) tnSmallKey, (ThumbnailProperty) tnMidKey }) {

			int maxWidth  = tnProp.getWidth();
			int maxHeight = tnProp.getHeight();
			boolean crop  = tnProp.getCrop();

			final float scale = getScaleRatio(origWidth, origHeight, maxWidth, maxHeight, crop);

			final String tnName = ImageHelper.getThumbnailName(originalImage.getName(),
					getThumbnailWidth(origWidth, scale),
					getThumbnailHeight(origHeight, scale));

			try {

				final Image thumbnail = (Image) app.nodeQuery(Image.class).and(pathKey, PathHelper.getFolderPath(originalImage.getProperty(pathKey)) + PathHelper.PATH_SEP + tnName).getFirst();

				if (thumbnail != null) {
					app.create(originalImage, thumbnail, thumbnailRel);
				}

			} catch (FrameworkException ex) {
				logger.debug("Error reconnecting thumbnail " + tnName + " to original image " + originalImage.getName(), ex);
			}

		}
	}

	public static void findAndReconnectOriginalImage(final Image thumbnail) {

		final Class<Relation> thumbnailRel = StructrApp.getConfiguration().getRelationshipEntityClass("ImageTHUMBNAILImage");
		final PropertyKey<String> pathKey  = StructrApp.key(Image.class, "path");
		final String originalImageName     = thumbnail.getOriginalImageName();

		try {

			final App app = StructrApp.getInstance();
			final Image originalImage = (Image) app.nodeQuery(Image.class).and(pathKey, PathHelper.getFolderPath(thumbnail.getProperty(pathKey)) + PathHelper.PATH_SEP + originalImageName).getFirst();

			if (originalImage != null) {

				final PropertyMap relProperties = new PropertyMap();
				relProperties.put(StructrApp.key(Image.class, "width"),                  thumbnail.getWidth());
				relProperties.put(StructrApp.key(Image.class, "height"),                 thumbnail.getHeight());
				relProperties.put(StructrApp.key(Image.class, "checksum"),               originalImage.getChecksum());

				app.create(originalImage, thumbnail, thumbnailRel, relProperties);
			}

		} catch (FrameworkException ex) {
			logger.debug("Error reconnecting thumbnail " + thumbnail.getName() + " to original image " + originalImageName, ex);
		}

	}

	public static float getScaleRatio(final int sourceWidth, final int sourceHeight, final int maxWidth, final int maxHeight, final boolean crop) {

		// float aspectRatio = sourceWidth/sourceHeight;
		final float scaleX = 1.0f * sourceWidth / maxWidth;
		final float scaleY = 1.0f * sourceHeight / maxHeight;
		final float scale;

		if (crop) {

			scale = Math.min(scaleX, scaleY);

		} else {

			scale = Math.max(scaleX, scaleY);
		}

		return scale;
	}

	public static int getThumbnailWidth(final int sourceWidth, final float scale) {
		return Math.max(4, Math.round(sourceWidth / scale));
	}

	public static int getThumbnailHeight(final int sourceHeight, final float scale) {
		return Math.max(4, Math.round(sourceHeight / scale));
	}

	public static int getThumbnailWidth(final Image originalImage, final int maxWidth, final int maxHeight, final boolean crop) {
		return getThumbnailWidth(originalImage.getWidth(), getScaleRatio(originalImage.getWidth(), originalImage.getHeight(), maxWidth, maxHeight, crop));
	}

	public static int getThumbnailHeight(final Image originalImage, final int maxWidth, final int maxHeight, final boolean crop) {
		return getThumbnailHeight(originalImage.getHeight(), getScaleRatio(originalImage.getWidth(), originalImage.getHeight(), maxWidth, maxHeight, crop));
	}


	public static Thumbnail createThumbnail(final Image originalImage, final int maxWidth, final int maxHeight) {
		return createThumbnail(originalImage, maxWidth, maxHeight, false);
	}

	public static Thumbnail createThumbnail(final Image originalImage, final int maxWidth, final int maxHeight, final boolean crop) {
		return createThumbnail(originalImage, maxWidth, maxHeight, null, crop, null, null);
	}

	public static Thumbnail createThumbnail(final Image originalImage, final int maxWidth, final int maxHeight, final String formatString, final boolean crop, final Integer reqOffsetX, final Integer reqOffsetY) {

		Thumbnail.Format format = Thumbnail.defaultFormat;

		try {

			final String imageFormatString = getImageFormatString(originalImage);
			format = formatString != null ? Thumbnail.Format.valueOf(formatString) : (imageFormatString != null ? Thumbnail.Format.valueOf(imageFormatString) : Thumbnail.defaultFormat);

		} catch (IllegalArgumentException iae) {

			logger.debug("Unsupported thumbnail format - using default");
		}

		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final Thumbnail tn               = new Thumbnail();

		try {

			final long start = System.nanoTime();
			BufferedImage source = getRotatedImage(originalImage);

			if (source != null) {

				final int sourceWidth  = source.getWidth();
				final int sourceHeight = source.getHeight();

				// Update image dimensions
				final PropertyMap properties = new PropertyMap();
				properties.put(StructrApp.key(Image.class, "width"), sourceWidth);
				properties.put(StructrApp.key(Image.class, "height"), sourceHeight);
				originalImage.setProperties(originalImage.getSecurityContext(), properties);

				// float aspectRatio = sourceWidth/sourceHeight;
				final float scale = getScaleRatio(sourceWidth, sourceHeight, maxWidth, maxHeight, crop);

				// Don't scale up
				if (scale > 1.0) {

					final int destWidth  = getThumbnailWidth(sourceWidth, scale);
					final int destHeight = getThumbnailHeight(sourceHeight, scale);

					if (crop) {

						final int offsetX = reqOffsetX != null ? reqOffsetX : Math.abs(maxWidth - destWidth) / 2;
						final int offsetY = reqOffsetY != null ? reqOffsetY : Math.abs(maxHeight - destHeight) / 2;

						final Integer[] dims = finalImageDimensions(offsetX, offsetY, maxWidth, maxHeight, sourceWidth, sourceHeight);

						logger.debug("Offset and Size (x,y,w,h): {},{},{},{}", new Object[] { dims[0], dims[1], dims[2], dims[3] });

						Thumbnails.of(source)
								.scale(1.0f / scale)
								.sourceRegion((int) (dims[0]*scale), (int) (dims[1]*scale), (int) (dims[2]*scale), (int) (dims[3]*scale))
								.outputFormat(format.name())
								.toOutputStream(baos);


						tn.setWidth(dims[2]);
						tn.setHeight(dims[3]);

					} else {

						Thumbnails.of(source)
								.scale(1.0f / scale)
								.outputFormat(format.name())
								.toOutputStream(baos);

						tn.setWidth(destWidth);
						tn.setHeight(destHeight);

					}

				} else {

					// Thumbnail is source image
					ImageIO.write(source, format.name(), baos);
					tn.setWidth(sourceWidth);
					tn.setHeight(sourceHeight);
				}

			} else {

				logger.debug("Thumbnail could not be created");

				return null;

			}

			final long end  = System.nanoTime();
			final long time = (end - start) / 1000000;

			logger.info("Thumbnail created for image {} ({}). Reading, scaling and writing took {} ms", new Object[] { originalImage.getName(), originalImage.getUuid(), time });

			tn.setBytes(baos.toByteArray());

			return tn;

		} catch (Throwable t) {

			logger.warn("Unable to create thumbnail for image with ID {}.", originalImage.getUuid(), t);
		}

		return null;
	}

	public static Thumbnail createCroppedImage(final Image originalImage, final int maxWidth, final int maxHeight, final Integer reqOffsetX, final Integer reqOffsetY, final String formatString) {


		// String contentType = (String) originalImage.getProperty(Image.CONTENT_TYPE_KEY);
		Thumbnail tn;

		try (final InputStream in = originalImage.getInputStream()) {

			if (in == null || in.available() <= 0) {

				logger.debug("InputStream of original image {} ({}) is null or not available ({} bytes)", new Object[] { originalImage.getName(), originalImage.getUuid(), in != null ? in.available() : -1 });
				return null;
			}

			final long start = System.nanoTime();
			final BufferedImage source = getRotatedImage(originalImage);

			tn = createThumbnailFromBufferedImage(source, originalImage, reqOffsetX, reqOffsetX, maxWidth, maxHeight, formatString);

			final long end  = System.nanoTime();
			final long time = (end - start) / 1000000;

			logger.debug("Cropped image created. Reading, scaling and writing took {} ms", time);

			return tn;

		} catch (Throwable t) {

			logger.warn("Unable to create cropped image for image with ID {}.", originalImage.getUuid(), t);
		}

		return null;
	}

	private static Thumbnail createThumbnailFromBufferedImage(final BufferedImage source, final Image originalImage, final Integer reqOffsetX, final Integer reqOffsetY, final Integer maxWidth, final Integer maxHeight, final String formatString) {

		final String imageFormatString = getImageFormatString(originalImage);
		final Thumbnail.Format format = formatString != null ? Thumbnail.Format.valueOf(formatString) : (imageFormatString != null ? Thumbnail.Format.valueOf(imageFormatString) : Thumbnail.defaultFormat);

		final Thumbnail tn = new Thumbnail();
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();

		if (source != null) {

			final int sourceWidth  = source.getWidth();
			final int sourceHeight = source.getHeight();

			// Update image dimensions
			final PropertyMap properties = new PropertyMap();
			properties.put(StructrApp.key(Image.class, "width"), sourceWidth);
			properties.put(StructrApp.key(Image.class, "height"), sourceHeight);

			try {
				if (originalImage != null) {
					originalImage.setProperties(originalImage.getSecurityContext(), properties);
				}
			} catch (final FrameworkException ex) {
				logger.debug("Unable to set properties of original image with ID {}", originalImage.getUuid(), ex);
			}

			final int offsetX = reqOffsetX != null ? reqOffsetX : 0;
			final int offsetY = reqOffsetY != null ? reqOffsetY : 0;

			final Integer[] dims = finalImageDimensions(offsetX, offsetY, maxWidth, maxHeight, sourceWidth, sourceHeight);

			logger.debug("Offset and Size (x,y,w,h): {},{},{},{}", new Object[] { dims[0], dims[1], dims[2], dims[3] });

			try {

				Thumbnails.of(source)
						.sourceRegion(dims[0], dims[1], dims[2], dims[3])
						.scale(1)
						.outputFormat(format.name())
						.toOutputStream(baos);

			} catch (Throwable t) {
				logger.warn("Unable to create thumbnail of source image", t);
			}

			tn.setWidth(dims[2]);
			tn.setHeight(dims[3]);

			tn.setBytes(baos.toByteArray());

			return tn;

		} else {

			logger.warn("Cropped image could not be created for '{}'", originalImage.getPath());
		}

		return null;
	}

	public static Thumbnail createThumbnailForPdf(final File originalFile, final int page, final int maxWidth, final int maxHeight, final String formatString) {

		final Thumbnail.Format format = formatString != null ? Thumbnail.Format.valueOf(formatString) : Thumbnail.defaultFormat;

		try {

			final PDDocument pdfDocument  = PDDocument.load(StorageProviderFactory.getStorageProvider(originalFile).getInputStream());
			final PDFRenderer pdfRenderer = new PDFRenderer(pdfDocument);

			// Create thumbnail of page
			final BufferedImage source = pdfRenderer.renderImage(page);
			return createThumbnailFromBufferedImage(source, null, null, null, maxWidth, maxHeight, null);

		} catch (final Throwable t) {
			logger.warn("Unable to create PDDocument from original file with ID {}.", originalFile.getUuid(), t);
		}

		return null;
	}

	private static BufferedImage getRotatedImage(final File originalImage) {

		try {

			// no need for try-with-resources for the below InputStream because ImageIO.read() closes its input stream
			final ImageInputStream in = ImageIO.createImageInputStream(originalImage.getInputStream());
			final int           orientation = getOrientation(originalImage);
			final BufferedImage source      = ImageIO.read(in);

			if (source != null) {

				final int sourceWidth  = source.getWidth();
				final int sourceHeight = source.getHeight();

				final AffineTransform affineTransform = new AffineTransform();

				switch (orientation) {
				case 1:
					break;
				case 2: // Flip X
					affineTransform.scale(-1.0, 1.0);
					affineTransform.translate(-sourceWidth, 0);
					break;
				case 3: // PI rotation
					affineTransform.translate(sourceWidth, sourceHeight);
					affineTransform.rotate(Math.PI);
					break;
				case 4: // Flip Y
					affineTransform.scale(1.0, -1.0);
					affineTransform.translate(0, -sourceHeight);
					break;
				case 5: // - PI/2 and Flip X
					affineTransform.rotate(-Math.PI / 2);
					affineTransform.scale(-1.0, 1.0);
					break;
				case 6: // -PI/2 and -width
					affineTransform.translate(sourceHeight, 0);
					affineTransform.rotate(Math.PI / 2);
					break;
				case 7: // PI/2 and Flip
					affineTransform.scale(-1.0, 1.0);
					affineTransform.translate(-sourceHeight, 0);
					affineTransform.translate(0, sourceWidth);
					affineTransform.rotate(3 * Math.PI / 2);
					break;
				case 8: // PI / 2
					affineTransform.translate(0, sourceWidth);
					affineTransform.rotate(3 * Math.PI / 2);
					break;
				default:
					break;
				}

				final AffineTransformOp op = new AffineTransformOp(affineTransform, AffineTransformOp.TYPE_BICUBIC);
				BufferedImage destinationImage = op.createCompatibleDestImage(source, ColorModel.getRGBdefault());

				final Graphics2D g = destinationImage.createGraphics();
				g.setBackground(Color.WHITE);
				g.clearRect(0, 0, destinationImage.getWidth(), destinationImage.getHeight());

				destinationImage = op.filter(source, destinationImage);

				return destinationImage;
			}

		} catch (IOException ex) {
			logger.debug("Unable to rotate image", ex);
		}

		return null;
	}

	/**
	 * Let ImageIO read and write a JPEG image. This should normalize all types of weird
	 * image sub formats, e.g. when extracting images from a flash file.
	 *
	 * Some images can not be read by ImageIO (or the browser) because they
	 * have an JPEG EOI and SOI marker at the beginning of the file.
	 *
	 * This method detects and removes the bytes, so that the image
	 * can be read again.
	 *
	 * @param original
	 * @return normalized image
	 */
	public static byte[] normalizeJpegImage(final byte[] original) {

		if (original == null) {

			return new byte[] {};
		}

		final ByteArrayInputStream in = new ByteArrayInputStream(original);

		// If JPEG image starts with ff d9 ffd8, strip this sequence from the beginning
		// FF D9 = EOI (end of image)
		// FF D8 = SOI (start of image)
		if ((original[0] == (byte) 0xff) && (original[1] == (byte) 0xd9) && (original[2] == (byte) 0xff) && (original[3] == (byte) 0xd8)) {

			in.skip(4);
		}

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		BufferedImage source;

		try {

			source = ImageIO.read(in);

			// If ImageIO cannot read it, return original
			if (source == null) {

				return original;
			}

			ImageIO.write(source, "jpeg", out);

		} catch (IOException ex) {

			logger.error("", ex);


			try {

				in.close();

			} catch (IOException ignore) {}

		} finally {}

		return out.toByteArray();
	}

	/**
	 * Update width and height
	 *
	 * @param image the image
	 * @throws FrameworkException
	 */
	public static void updateMetadata(final File image) throws FrameworkException {

		updateMetadata(image, image.getInputStream());
	}

	/**
	 * Update width and height
	 *
	 * @param image the image
	 * @param fis file input stream
	 * @throws FrameworkException
	 */
	public static void updateMetadata(final File image, final InputStream fis) throws FrameworkException {

		try (final InputStream is = fis) {

			final BufferedImage source = ImageIO.read(is);
			if (source != null) {

				final int sourceWidth  = source.getWidth();
				final int sourceHeight = source.getHeight();

				final PropertyMap map = new PropertyMap();

				map.put(StructrApp.key(Image.class, "width"),       sourceWidth);
				map.put(StructrApp.key(Image.class, "height"),      sourceHeight);
				map.put(StructrApp.key(Image.class, "orientation"), ImageHelper.getOrientation(image));

				image.setProperties(image.getSecurityContext(), map);

			}

		} catch (IOException ex) {
			logger.warn("Unable to read image data", ex);
		}
	}

	//~--- static methods ----------------------------------------------------

	public static Integer[] finalImageDimensions(final int offsetX, final int offsetY, final int requestedWidth, final int requestedHeight, final int sourceWidth, final int sourceHeight) {

		final Integer[] finalDimensions = new Integer[4];

		final int overhangLeftX   = Math.min(offsetX, 0); // negative value
		final int overhangRightX  = Math.max(offsetX + requestedWidth - sourceWidth, 0); // positive value
		final int overhangTopY    = Math.min(offsetY, 0); // negative value
		final int overhangBottomY = Math.max(offsetY + requestedHeight - sourceHeight, 0); // positive value

		finalDimensions[0] = Math.min(Math.max(offsetX, 0), sourceWidth);
		finalDimensions[1] = Math.min(Math.max(offsetY, 0), sourceHeight);
		finalDimensions[2] = requestedWidth + overhangLeftX - overhangRightX;
		finalDimensions[3] = requestedHeight + overhangTopY - overhangBottomY;

		return finalDimensions;

	}

	public static String getBase64String(final File file) {

		try (final InputStream dataStream = file.getInputStream()) {

			if (dataStream != null) {
				return Base64.encodeToString(IOUtils.toByteArray(file.getInputStream()), false);
			}

		} catch (IOException ex) {
			logger.error("Could not get base64 string from file ", ex);
		}

		return null;
	}

	/**
	 * Check if url points to an image by extension
	 *
	 * TODO: Improve method to check file type by peeping at the
	 * content
	 *
	 * @param urlString
	 * @return true if is of image type
	 */
	public static boolean isImageType(final String urlString) {

		if ((urlString == null) || StringUtils.isBlank(urlString)) {

			return false;
		}

		final String extension         = StringUtils.substringAfterLast(urlString.toLowerCase(), ".");
		final String[] imageExtensions = {

			"png", "gif", "jpg", "jpeg", "bmp", "tif", "tiff"
		};

		for (String ext : imageExtensions) {

			if (ext.equals(extension)) {

				return true;
			}

		}

		return false;

	}

	/**
	 * Return image format string derived from last part of content type.
	 *
	 * @param img The image to get the format of.
	 * @return the image format string
	 */
	public static String getImageFormatString(final Image img) {

		final String contentType = img.getContentType();

		if (contentType == null) return null;

		return StringUtils.substringAfterLast(contentType.toLowerCase(), "/");
	}

	/**
	 * Check if url points to an Flash object by extension
	 *
	 * TODO: Improve method to check file type by peeping at the
	 * content
	 *
	 * @param urlString
	 * @return true if is swf file
	 */
	public static boolean isSwfType(final String urlString) {

		if ((urlString == null) || StringUtils.isBlank(urlString)) {

			return false;
		}

		final String extension         = StringUtils.substringAfterLast(urlString.toLowerCase(), ".");
		final String[] swfExtensions = { "swf" };

		for (final String ext : swfExtensions) {

			if (ext.equals(extension)) {

				return true;
			}

		}

		return false;
	}

	private static Metadata getMetadata(final File originalImage) {

		Metadata metadata = new Metadata();

		if (originalImage.isTemplate()) {

			return metadata;
		}

		try (final InputStream in = originalImage.getInputStream()) {

			if (in != null && in.available() > 0) {

				metadata = ImageMetadataReader.readMetadata(in);
			}

		} catch (NegativeArraySizeException | ImageProcessingException | IOException ex) {
			logger.debug("Unable to get metadata information from image stream", ex);
		}

		return metadata;
	}

	public static int getOrientation(final File originalImage) {

		if (originalImage.isTemplate()) {

			return 1;
		}

		try {

			final ExifIFD0Directory exifIFD0Directory = getMetadata(originalImage).getFirstDirectoryOfType(ExifIFD0Directory.class);

			if (exifIFD0Directory != null && exifIFD0Directory.hasTagName(ExifIFD0Directory.TAG_ORIENTATION)) {

				final Integer orientation = exifIFD0Directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
				originalImage.setProperty(StructrApp.key(Image.class, "orientation"), orientation);

				return orientation;
			}

		} catch (MetadataException | JSONException | FrameworkException ex) {
			logger.warn("Unable to store orientation information on image {} ({})", new Object[] { originalImage.getName(), originalImage.getUuid() });
		}

		return 1;
	}

	public static JSONObject getExifData(final File originalImage) {

		if (originalImage.isTemplate()) {

			return null;
		}

		try {

			// Get new instance with superuser context to be able to update EXIF data
			final File image = StructrApp.getInstance().get(File.class, originalImage.getUuid());

			if (image != null) {

				final JSONObject exifDataJson = new JSONObject();
				final Metadata metadata = getMetadata(image);

				final ExifIFD0Directory   exifIFD0Directory   = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
				final ExifSubIFDDirectory exifSubIFDDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
				final GpsDirectory        gpsDirectory        = metadata.getFirstDirectoryOfType(GpsDirectory.class);

				if (exifIFD0Directory != null) {

					final JSONObject exifIFD0DataJson = new JSONObject();

					exifIFD0Directory.getTags().forEach((tag) -> {
						exifIFD0DataJson.put(tag.getTagName(), exifIFD0Directory.getDescription(tag.getTagType()));
					});

					image.setProperty(StructrApp.key(Image.class, "exifIFD0Data"), exifIFD0DataJson.toString());
					exifDataJson.putOnce("exifIFD0Data", exifIFD0DataJson);
				}

				if (exifSubIFDDirectory != null) {

					final JSONObject exifSubIFDDataJson = new JSONObject();

					exifSubIFDDirectory.getTags().forEach((tag) -> {
						exifSubIFDDataJson.put(tag.getTagName(), exifSubIFDDirectory.getDescription(tag.getTagType()));
					});

					image.setProperty(StructrApp.key(Image.class, "exifSubIFDData"), exifSubIFDDataJson.toString());
					exifDataJson.putOnce("exifSubIFDData", exifSubIFDDataJson);
				}

				if (gpsDirectory != null) {

					final JSONObject exifGpsDataJson = new JSONObject();

					gpsDirectory.getTags().forEach((tag) -> {
						exifGpsDataJson.put(tag.getTagName(), gpsDirectory.getDescription(tag.getTagType()));
					});

					image.setProperty(StructrApp.key(Image.class, "gpsData"), exifGpsDataJson.toString());
					exifDataJson.putOnce("gpsData", exifGpsDataJson);
				}

				return exifDataJson;

			} else {

				logger.warn("Image does not exist anymore. {}: {}", originalImage.getUuid(), originalImage.getName());
				return null;
			}

		} catch (final Exception ex) {
			logger.warn("Unable to extract EXIF metadata.", ex);
		}

		return null;
	}

	public static String getExifDataString(final File originalImage) {
		JSONObject data = getExifData(originalImage);
		return data != null ? data.toString() : null;
	}

	/**
	 * @param originalImageName The filename of the original image
	 * @param width The width of the new image variant
	 * @param height The height of the new image variant
	 * @param variant The variant type of the new image, e.g. _thumb_, _cropped_, _scaled_
	 * @return the name for the new image variant with the given dimensions
	 */
	public static String getVariantName(final String originalImageName, final Integer width, final Integer height, final String variant) {

		return originalImageName + variant + width + "x" + height;
	}

	/**
	 * @param originalImageName The filename of the image which this thumbnail belongs to
	 * @param tnWidth The width of the thumbnail
	 * @param tnHeight The height of the thumbnail
	 * @return the thumbnail name for the thumbnail with the given dimensions
	 */
	public static String getThumbnailName(final String originalImageName, final Integer tnWidth, final Integer tnHeight) {

		return getVariantName(originalImageName, tnWidth, tnHeight, "_thumb_");
	}

	//~--- inner classes --------------------------------------------------

	public static class Base64URIData {

		private String contentType;
		private String data;

		//~--- constructors -------------------------------------------

		public Base64URIData(final String rawData) {

			final String[] parts = StringUtils.split(rawData, ",");

			data        = parts[1];
			contentType = StringUtils.substringBetween(parts[0], "data:", ";base64");

		}

		//~--- get methods --------------------------------------------

		public String getContentType() {

			return contentType;

		}

		public String getData() {

			return data;

		}

		public byte[] getBinaryData() {

			return Base64.decode(data);

		}

	}


	public static class Thumbnail {


		public static enum Format {
			png, jpg, jpeg, gif;
		}

		public static Format defaultFormat = Format.jpeg;

		//~--- fields -------------------------------------------------

		private byte[] bytes;
		private int height;
		private int width;
		private Format format;

		//~--- constructors -------------------------------------------

		public Thumbnail() {}

		public Thumbnail(final byte[] bytes) {
			this.bytes = bytes;
		}

		public Thumbnail(final int width, final int height) {
			this.width  = width;
			this.height = height;
		}

		public Thumbnail(final byte[] bytes, final int width, final int height) {
			this.bytes  = bytes;
			this.width  = width;
			this.height = height;
		}

		public Thumbnail(final byte[] bytes, final int width, final int height, final String formatString) {
			this.bytes  = bytes;
			this.width  = width;
			this.height = height;
			this.format = Format.valueOf(formatString);
		}

		//~--- get methods --------------------------------------------

		public byte[] getBytes() {
			return bytes;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		public Format getFormat() {
			return format;
		}

		public String getFormatAsString() {
			return format.name();
		}

		//~--- set methods --------------------------------------------

		public void setBytes(final byte[] bytes) {
			this.bytes = bytes;
		}

		public void setWidth(final int width) {
			this.width = width;
		}

		public void setHeight(final int height) {
			this.height = height;
		}

		public void setFormat(final Format format) {
			this.format = format;
		}

		public void setFormatByString(final String formatString) {
			this.format = Format.valueOf(formatString);
		}

	}

}
