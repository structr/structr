/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.PathHelper;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.storage.StorageProviderFactory;
import org.structr.util.Base64;
import org.structr.web.entity.File;
import org.structr.web.entity.Image;
import org.structr.web.property.ThumbnailProperty;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.web.traits.definitions.ImageTraitDefinition;
import org.structr.web.traits.relationships.ImageTHUMBNAILImage;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

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
	public static NodeInterface createImage(final SecurityContext securityContext, final InputStream imageStream, final String contentType, final String imageType, final String name, final boolean markAsThumbnail)
			throws FrameworkException, IOException {

		final PropertyMap props = new PropertyMap();
		final Traits traits     = Traits.of(StructrTraits.IMAGE);

		props.put(traits.key(GraphObjectTraitDefinition.TYPE_PROPERTY),        imageType == null ? StructrTraits.IMAGE : imageType);
		props.put(traits.key(ImageTraitDefinition.IS_THUMBNAIL_PROPERTY), markAsThumbnail);
		props.put(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),        name);

		final Image newImage = StructrApp.getInstance(securityContext).create(imageType, props).as(Image.class);
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
	public static NodeInterface createImageNode(final SecurityContext securityContext, final byte[] imageData, final String contentType, final String imageType, final String name, final boolean markAsThumbnail)
		throws FrameworkException, IOException {

		final PropertyMap props = new PropertyMap();
		final Traits traits     = Traits.of(StructrTraits.IMAGE);

		props.put(traits.key(GraphObjectTraitDefinition.TYPE_PROPERTY),        imageType == null ? StructrTraits.IMAGE : imageType);
		props.put(traits.key(ImageTraitDefinition.IS_THUMBNAIL_PROPERTY), markAsThumbnail);
		props.put(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),        name);

		final NodeInterface newImage = StructrApp.getInstance(securityContext).create(imageType, props);

		if (imageData != null && imageData.length > 0) {

			setFileData(newImage.as(File.class), imageData, contentType);
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
	public static void setImageData(final Image img, final byte[] imageData, final String contentType) throws FrameworkException, IOException {

		setFileData(img, imageData, contentType);
	}

	public static void findAndReconnectThumbnails(final Image originalImage) {

		final String thumbnailRel         = StructrTraits.IMAGE_THUMBNAIL_IMAGE;
		final Traits traits               = Traits.of(StructrTraits.IMAGE);
		final PropertyKey<String> pathKey = traits.key(AbstractFileTraitDefinition.PATH_PROPERTY);
		final App app                     = StructrApp.getInstance();
		final Integer origWidth           = originalImage.getWidth();
		final Integer origHeight          = originalImage.getHeight();

		if (origWidth == null || origHeight == null) {

			if (!Arrays.asList("image/svg+xml", "image/x-icon", "image/x-photoshop").contains(originalImage.getContentType())) {

				logger.info("Could not determine width and height for {}", originalImage.getName());
			}

			return;
		}

		final String originalFolderPath = PathHelper.getFolderPath(originalImage.getPath());

		for (final ThumbnailProperty spec : getThumbnailSpecs()) {

			final String tnName = getExpectedThumbnailName(originalImage, spec);
			if (tnName == null) {

				continue;
			}

			try {

				// Thumbnails live in the thumbnail folder mirroring the original's folder structure. Older
				// data has them next to the original image, so that location is tried as a fallback.
				NodeInterface thumbnail = app.nodeQuery(StructrTraits.IMAGE).key(pathKey, joinPath(Image.STRUCTR_THUMBNAIL_FOLDER, originalFolderPath, tnName)).getFirst();
				if (thumbnail == null) {

					thumbnail = app.nodeQuery(StructrTraits.IMAGE).key(pathKey, joinPath(originalFolderPath, tnName)).getFirst();
				}

				// A thumbnail can have only one incoming THUMBNAIL relationship (the source multiplicity is
				// One), so connecting it a second time would silently replace the first connection. That
				// happens when one thumbnail serves several specs, i.e. for images too small to be scaled
				// down: the smallest spec wins, any other one recreates the thumbnail when it is requested.
				if (thumbnail != null && thumbnail.getIncomingRelationship(thumbnailRel) == null) {

					app.create(originalImage, thumbnail.as(Image.class), thumbnailRel, getThumbnailRelationshipProperties(originalImage, spec));
				}

			} catch (FrameworkException ex) {

				logger.debug("Error reconnecting thumbnail " + tnName + " to original image " + originalImage.getName(), ex);
			}

		}
	}

	public static void findAndReconnectOriginalImage(final Image thumbnail) {

		final String thumbnailRel         = StructrTraits.IMAGE_THUMBNAIL_IMAGE;
		final Traits traits               = Traits.of(StructrTraits.IMAGE);
		final PropertyKey<String> pathKey = traits.key(AbstractFileTraitDefinition.PATH_PROPERTY);
		final String originalImageName    = thumbnail.getOriginalImageName();

		try {

			final App app = StructrApp.getInstance();

			// The original image sits in the folder the thumbnail's folder mirrors; for older data, where
			// thumbnails are stored next to the original, that is the thumbnail's own folder.
			final String thumbnailFolderPath = PathHelper.getFolderPath(thumbnail.getPath());
			final String originalFolderPath  = stripThumbnailFolder(thumbnailFolderPath);
			NodeInterface originalNode = app.nodeQuery(StructrTraits.IMAGE).key(pathKey, joinPath(originalFolderPath, originalImageName)).getFirst();

			if (originalNode == null && !originalFolderPath.equals(thumbnailFolderPath)) {

				originalNode = app.nodeQuery(StructrTraits.IMAGE).key(pathKey, joinPath(thumbnailFolderPath, originalImageName)).getFirst();
			}

			if (originalNode != null) {

				final Image originalImage    = originalNode.as(Image.class);
				final ThumbnailProperty spec = findMatchingThumbnailSpec(originalImage, thumbnail.getName());

				if (spec != null) {

					app.create(originalImage, thumbnail, thumbnailRel, getThumbnailRelationshipProperties(originalImage, spec));

				} else {

					// Not one of the built-in thumbnail sizes (or the original's dimensions are unknown), so
					// the specs cannot be reconstructed. Link the two nodes anyway - the graph is restored,
					// but this thumbnail will not be found by getExistingThumbnail() and gets recreated once.
					logger.debug("Unable to determine thumbnail specs for {}, reconnecting without specs", thumbnail.getName());

					app.create(originalImage, thumbnail, thumbnailRel);
				}
			}

		} catch (FrameworkException ex) {

			logger.debug("Error reconnecting thumbnail " + thumbnail.getName() + " to original image " + originalImageName, ex);
		}

	}

	/**
	 * The built-in thumbnail specs, i.e. the sizes Image.tnSmall and Image.tnMid are created for, smallest
	 * first. The order matters and must stay deterministic: an image smaller than both sizes is not scaled
	 * up, so one thumbnail serves both specs and only the first one gets connected (see below).
	 */
	private static List<ThumbnailProperty> getThumbnailSpecs() {

		final Traits traits = Traits.of(StructrTraits.IMAGE);

		return List.of(
			(ThumbnailProperty) (PropertyKey) traits.key(ImageTraitDefinition.TN_SMALL_PROPERTY),
			(ThumbnailProperty) (PropertyKey) traits.key(ImageTraitDefinition.TN_MID_PROPERTY)
		);
	}

	/**
	 * The name a thumbnail of the given original image has when created for the given spec, or null if it
	 * cannot be derived. Mirrors how createThumbnail() determines the thumbnail's dimensions, because the
	 * name is built from them - a name derived any other way would either not match anything, or match the
	 * thumbnail of a different spec and connect the wrong image.
	 */
	private static String getExpectedThumbnailName(final Image originalImage, final ThumbnailProperty spec) {

		final Integer origWidth  = originalImage.getWidth();
		final Integer origHeight = originalImage.getHeight();

		if (origWidth == null || origHeight == null) {

			return null;
		}

		if (spec.getCrop()) {

			// With cropToFit the dimensions come from finalImageDimensions() rather than from the scale
			// ratio, and the ratio-based name is the name of the NON-cropped thumbnail of the same size.
			// Deriving it here would connect that thumbnail and label it as cropped, so don't guess.

			return null;
		}

		final float scale = getScaleRatio(origWidth, origHeight, spec.getWidth(), spec.getHeight(), spec.getCrop());

		// createThumbnail() does not scale up: an image smaller than the requested size keeps its dimensions
		final int tnWidth  = scale > 1.0 ? getThumbnailWidth(origWidth, scale)   : origWidth;
		final int tnHeight = scale > 1.0 ? getThumbnailHeight(origHeight, scale) : origHeight;

		return getThumbnailName(originalImage.getName(), tnWidth, tnHeight);
	}

	/** The spec a thumbnail of the given name was created for, or null if it matches none of them. */
	private static ThumbnailProperty findMatchingThumbnailSpec(final Image originalImage, final String thumbnailName) {

		for (final ThumbnailProperty spec : getThumbnailSpecs()) {

			if (thumbnailName.equals(getExpectedThumbnailName(originalImage, spec))) {

				return spec;
			}
		}

		return null;
	}

	/**
	 * The properties a THUMBNAIL relationship needs to carry so that getExistingThumbnail() can match it:
	 * the specs the thumbnail was created for, plus the original's checksum to detect a changed original.
	 * The thumbnail's actual dimensions are deliberately not stored here - they live on the thumbnail node,
	 * and the relationship type declares no keys that could read them back.
	 */
	private static PropertyMap getThumbnailRelationshipProperties(final Image originalImage, final ThumbnailProperty spec) {

		final Traits relTraits          = Traits.of(StructrTraits.IMAGE_THUMBNAIL_IMAGE);
		final PropertyMap relProperties = new PropertyMap();

		relProperties.put(relTraits.key(ImageTHUMBNAILImage.MAX_WIDTH_PROPERTY),   spec.getWidth());
		relProperties.put(relTraits.key(ImageTHUMBNAILImage.MAX_HEIGHT_PROPERTY),  spec.getHeight());
		relProperties.put(relTraits.key(ImageTHUMBNAILImage.CROP_TO_FIT_PROPERTY), spec.getCrop());
		relProperties.put(relTraits.key(ImageTHUMBNAILImage.CHECKSUM_PROPERTY),    originalImage.getChecksum());

		return relProperties;
	}

	/** The given path with the thumbnail folder prefix removed, or the path itself if it has none. */
	private static String stripThumbnailFolder(final String path) {

		final String thumbnailFolder = PathHelper.PATH_SEP + PathHelper.clean(Image.STRUCTR_THUMBNAIL_FOLDER);
		if (path.equals(thumbnailFolder)) {

			return PathHelper.PATH_SEP;
		}

		if (path.startsWith(thumbnailFolder + PathHelper.PATH_SEP)) {

			return path.substring(thumbnailFolder.length());
		}

		return path;
	}

	/** Joins path segments into an absolute path, tolerating missing and duplicated separators. */
	private static String joinPath(final String... parts) {

		final StringBuilder buf = new StringBuilder();

		for (final String part : parts) {

			final String cleaned = part == null ? null : PathHelper.clean(part);
			if (StringUtils.isNotBlank(cleaned)) {

				buf.append(PathHelper.PATH_SEP).append(cleaned);
			}
		}

		return buf.isEmpty() ? PathHelper.PATH_SEP : buf.toString();
	}

	public static float getScaleRatio(final int sourceWidth, final int sourceHeight, final int maxWidth, final int maxHeight, final boolean crop) {

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

		final ScaleOptions options = new ScaleOptions();

		options.maxWidth  = maxWidth;
		options.maxHeight = maxHeight;
		options.crop      = crop;
		options.offsetX   = reqOffsetX;
		options.offsetY   = reqOffsetY;
		options.format    = formatString != null ? formatString : ("original".equals(Settings.ThumbnailFormat.getValue()) ? null : Thumbnail.defaultFormat.name());

		try {

			final long start   = System.nanoTime();
			final Thumbnail tn = scaleImage(originalImage, options, true);
			final long time    = (System.nanoTime() - start) / 1000000;

			logger.info("Thumbnail ({}, {}, {}) created for image {} ({}). Reading, scaling and writing took {} ms", maxWidth, maxHeight, crop, originalImage.getName(), originalImage.getUuid(), time);

			return tn;

		} catch (Throwable t) {

			logger.warn("Unable to create thumbnail for image with ID {}: {}", originalImage.getUuid(), t.getMessage());
		}

		return null;
	}

	/** Cuts the given region (in pixels of the upright original) out of the image without scaling it. */
	public static Thumbnail createCroppedImage(final Image originalImage, final int width, final int height, final Integer offsetX, final Integer offsetY, final String formatString) {

		final ScaleOptions options = new ScaleOptions();

		options.maxWidth  = width;
		options.maxHeight = height;
		options.scale     = 1.0;
		options.crop      = true;
		options.offsetX   = offsetX != null ? offsetX : 0;
		options.offsetY   = offsetY != null ? offsetY : 0;
		options.format    = formatString;

		try {

			return scaleImage(originalImage, options, true);

		} catch (Throwable t) {

			logger.warn("Unable to create cropped image for image with ID {}: {}", originalImage.getUuid(), t.getMessage());
		}

		return null;
	}

	/**
	 * Scales and optionally crops the given image and encodes the result.
	 *
	 * The size comes from scale (a factor) or from the bounding box maxWidth x maxHeight; neither scales up. With crop, the image is
	 * scaled to cover the box and the box is cut out at offsetX/offsetY (in pixels of the scaled image, centered by default).
	 * The format defaults to the original's format if it can be written, else PNG; quality applies to JPEG only.
	 *
	 * @param originalImage the image to scale
	 * @param options the scale options
	 * @param updateDimensions whether to store the width and height of the original on the original image node
	 * @return the encoded image, never empty
	 * @throws FrameworkException 422 if the image cannot be read or the result cannot be written
	 */
	public static Thumbnail scaleImage(final Image originalImage, final ScaleOptions options, final boolean updateDimensions) throws FrameworkException {

		final String label   = "'" + originalImage.getName() + "' (" + originalImage.getUuid() + ")";
		final String format  = resolveFormat(options.format, originalImage, label);
		BufferedImage source = getRotatedImage(originalImage);

		if (source == null) {

			throw new FrameworkException(422, "Unable to read image " + label);
		}

		if (updateDimensions) {

			final PropertyMap properties = new PropertyMap();
			final Traits traits          = Traits.of(StructrTraits.IMAGE);

			properties.put(traits.key(ImageTraitDefinition.WIDTH_PROPERTY),  source.getWidth());
			properties.put(traits.key(ImageTraitDefinition.HEIGHT_PROPERTY), source.getHeight());

			originalImage.setProperties(originalImage.getSecurityContext(), properties);
		}

		return scaleImage(source, options, format, label);
	}

	public static Thumbnail scaleImage(final BufferedImage source, final ScaleOptions options, final String format, final String label) throws FrameworkException {

		final int sourceWidth  = source.getWidth();
		final int sourceHeight = source.getHeight();
		final Integer maxWidth  = positiveOrNull(options.maxWidth);
		final Integer maxHeight = positiveOrNull(options.maxHeight);
		int width              = sourceWidth;
		int height             = sourceHeight;

		if (options.scale != null) {

			if (options.scale <= 0) {

				throw new FrameworkException(422, "Invalid scale " + options.scale + " for image " + label);
			}

			final double factor = Math.min(options.scale, 1.0);

			width  = Math.max(1, (int)Math.round(sourceWidth * factor));
			height = Math.max(1, (int)Math.round(sourceHeight * factor));

		} else if (maxWidth != null || maxHeight != null) {

			// same ratio and rounding as before, so existing thumbnails keep their names
			final float ratioX = maxWidth  != null ? 1.0f * sourceWidth  / maxWidth  : 0;
			final float ratioY = maxHeight != null ? 1.0f * sourceHeight / maxHeight : 0;
			final float ratio  = (maxWidth == null) ? ratioY : (maxHeight == null) ? ratioX : (options.crop ? Math.min(ratioX, ratioY) : Math.max(ratioX, ratioY));

			if (ratio > 1.0f) {

				width  = getThumbnailWidth(sourceWidth, ratio);
				height = getThumbnailHeight(sourceHeight, ratio);
			}
		}

		final boolean alpha  = !"jpeg".equals(format);
		BufferedImage result = resize(source, width, height, alpha);

		if (options.crop) {

			final int cropWidth  = maxWidth  != null ? Math.min(maxWidth, width)   : width;
			final int cropHeight = maxHeight != null ? Math.min(maxHeight, height) : height;
			final int offsetX    = clamp(options.offsetX != null ? options.offsetX : (width - cropWidth) / 2, 0, width - cropWidth);
			final int offsetY    = clamp(options.offsetY != null ? options.offsetY : (height - cropHeight) / 2, 0, height - cropHeight);

			result = result.getSubimage(offsetX, offsetY, cropWidth, cropHeight);
		}

		final byte[] bytes = encode(result, format, options.quality);
		if (bytes == null || bytes.length == 0) {

			throw new FrameworkException(422, "Unable to write image " + label + " as " + format);
		}

		final Thumbnail tn = new Thumbnail(bytes, result.getWidth(), result.getHeight());

		tn.setFormat(Thumbnail.Format.valueOf(format));

		return tn;
	}

	/** Options for scaleImage(), all optional. */
	public static class ScaleOptions {

		public Integer maxWidth;
		public Integer maxHeight;
		public Double scale;
		public boolean crop;
		public Integer offsetX;
		public Integer offsetY;
		public String format;
		public Float quality;
	}

	public static final float DEFAULT_JPEG_QUALITY = 0.85f;

	private static String resolveFormat(final String requested, final Image originalImage, final String label) throws FrameworkException {

		if (requested != null) {

			final String format = normalizeFormat(requested);
			if (format == null) {

				throw new FrameworkException(422, "Unsupported image format '" + requested + "' for image " + label + ", supported are png, jpeg and gif");
			}

			return format;
		}

		final String original = normalizeFormat(getImageFormatString(originalImage));

		return original != null ? original : Thumbnail.defaultFormat.name();
	}

	private static String normalizeFormat(final String format) {

		if (format == null) {
			return null;
		}

		return switch (format.trim().toLowerCase()) {
			case "png"         -> "png";
			case "jpg", "jpeg" -> "jpeg";
			case "gif"         -> "gif";
			default            -> null;
		};
	}

	private static Integer positiveOrNull(final Integer value) {

		return (value != null && value > 0) ? value : null;
	}

	private static int clamp(final int value, final int min, final int max) {

		return Math.max(min, Math.min(max, value));
	}

	/** Bicubic resize, halving step by step for strong downscaling; without alpha, transparent pixels become white. */
	private static BufferedImage resize(final BufferedImage source, final int width, final int height, final boolean alpha) {

		final int type        = alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
		BufferedImage current = source;
		int currentWidth      = source.getWidth();
		int currentHeight     = source.getHeight();

		do {

			currentWidth  = (currentWidth  > width  * 2) ? currentWidth  / 2 : width;
			currentHeight = (currentHeight > height * 2) ? currentHeight / 2 : height;

			final BufferedImage next = new BufferedImage(currentWidth, currentHeight, type);
			final Graphics2D g       = next.createGraphics();

			if (!alpha) {

				g.setColor(Color.WHITE);
				g.fillRect(0, 0, currentWidth, currentHeight);
			}

			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.drawImage(current, 0, 0, currentWidth, currentHeight, null);
			g.dispose();

			current = next;

		} while (currentWidth != width || currentHeight != height);

		return current;
	}

	private static byte[] encode(final BufferedImage image, final String format, final Float quality) throws FrameworkException {

		final Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format);
		if (!writers.hasNext()) {

			return null;
		}

		final ImageWriter writer               = writers.next();
		final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();

		try (final ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {

			final ImageWriteParam param = writer.getDefaultWriteParam();

			if ("jpeg".equals(format)) {

				final float q = quality != null ? quality : DEFAULT_JPEG_QUALITY;
				if (q < 0.0f || q > 1.0f) {

					throw new FrameworkException(422, "Invalid quality " + q + ", must be between 0.0 and 1.0");
				}

				param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				param.setCompressionQuality(q);
			}

			writer.setOutput(ios);
			writer.write(null, new IIOImage(image, null, null), param);

		} catch (IOException ex) {

			logger.warn("Unable to write image as {}: {}", format, ex.getMessage());
			return null;

		} finally {

			writer.dispose();
		}

		return out.toByteArray();
	}

	private static BufferedImage getRotatedImage(final File originalImage) {

		try {

			// no need for try-with-resources for the below InputStream because ImageIO.read() closes its input stream
			final ImageInputStream in = ImageIO.createImageInputStream(originalImage.getInputStream());
			final int orientation     = getOrientation(originalImage);
			BufferedImage source      = ImageIO.read(in);

			if (source != null) {

				final AffineTransform affineTransform = new AffineTransform();
				final int sourceWidth                 = source.getWidth();
				final int sourceHeight                = source.getHeight();
				boolean swapWidthAndHeight            = false;

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
						swapWidthAndHeight = true;
						break;
					case 6: // -PI/2 and -width
						affineTransform.translate(sourceHeight, 0);
						affineTransform.rotate(Math.PI / 2);
						swapWidthAndHeight = true;
						break;
					case 7: // PI/2 and Flip
						affineTransform.scale(-1.0, 1.0);
						affineTransform.translate(-sourceHeight, 0);
						affineTransform.translate(0, sourceWidth);
						affineTransform.rotate(3 * Math.PI / 2);
						swapWidthAndHeight = true;
						break;
					case 8: // PI / 2
						affineTransform.translate(0, sourceWidth);
						affineTransform.rotate(3 * Math.PI / 2);
						swapWidthAndHeight = true;
						break;
					default:
						break;
				}

				final AffineTransformOp op     = new AffineTransformOp(affineTransform, AffineTransformOp.TYPE_BICUBIC);
				BufferedImage destinationImage = new BufferedImage(swapWidthAndHeight ? source.getHeight() : source.getWidth(), swapWidthAndHeight ? source.getWidth() : source.getHeight(), source.getType());
				final Graphics2D g = destinationImage.createGraphics();

				//g.setBackground(Color.WHITE);
				g.clearRect(0, 0, destinationImage.getWidth(), destinationImage.getHeight());

				destinationImage = op.filter(source, destinationImage);

				return destinationImage;
			}

		} catch (Throwable t) {

			logger.debug("Unable to transform image", t);
		}

		return null;
	}

	private static BufferedImage getScaledImage(final BufferedImage source, final double sx, final double sy) {

		try {

			final AffineTransform affineTransform = AffineTransform.getScaleInstance(sx, sy);
			final AffineTransformOp op            = new AffineTransformOp(affineTransform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
			BufferedImage destinationImage        = op.createCompatibleDestImage(source, source.getColorModel());

			destinationImage = op.filter(source, destinationImage);

			return destinationImage;

		} catch (Throwable t) {

			logger.debug("Unable to scale image", t);
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

		}

		return out.toByteArray();
	}

	/**
	 * Update width and height
	 *
	 * @param image the image
	 * @throws FrameworkException
	 */
	public static void updateMetadata(final File image) throws FrameworkException, IOException {

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
				final Traits traits   = Traits.of(StructrTraits.IMAGE);

				map.put(traits.key(ImageTraitDefinition.WIDTH_PROPERTY),       sourceWidth);
				map.put(traits.key(ImageTraitDefinition.HEIGHT_PROPERTY),      sourceHeight);
				map.put(traits.key(ImageTraitDefinition.ORIENTATION_PROPERTY), ImageHelper.getOrientation(image));

				image.setProperties(image.getSecurityContext(), map);

			}

		} catch (IOException ex) {

			logger.warn("Unable to read image data", ex);
		}
	}

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

				return Base64.encodeToString(IOUtils.toByteArray(dataStream), false);
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

		final String extension = StringUtils.substringAfterLast(urlString.toLowerCase(), ".");
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
			if (exifIFD0Directory != null && exifIFD0Directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION) && exifIFD0Directory.hasTagName(ExifIFD0Directory.TAG_ORIENTATION)) {

				final Integer orientation = exifIFD0Directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
				final Traits traits       = Traits.of(StructrTraits.IMAGE);

				originalImage.setProperty(traits.key(ImageTraitDefinition.ORIENTATION_PROPERTY), orientation);

				return orientation;
			}

		} catch (MetadataException | JSONException | FrameworkException ex) {

			logger.warn("Unable to store orientation information on image {} ({})", originalImage.getName(), originalImage.getUuid());
		}

		return 1;
	}

	public static JSONObject getExifData(final File originalImage) {

		if (originalImage.isTemplate()) {

			return null;
		}

		try {

			// Get new instance with superuser context to be able to update EXIF data
			final NodeInterface node = StructrApp.getInstance().getNodeById(StructrTraits.FILE, originalImage.getUuid());
			if (node != null) {

				final Image image             = node.as(Image.class);
				final JSONObject exifDataJson = new JSONObject();
				final Metadata metadata       = getMetadata(image);
				final Traits traits           = Traits.of(StructrTraits.IMAGE);
				final ExifIFD0Directory   exifIFD0Directory   = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
				final ExifSubIFDDirectory exifSubIFDDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
				final GpsDirectory        gpsDirectory        = metadata.getFirstDirectoryOfType(GpsDirectory.class);

				if (exifIFD0Directory != null) {

					final JSONObject exifIFD0DataJson = new JSONObject();

					exifIFD0Directory.getTags().forEach((tag) -> {
						exifIFD0DataJson.put(tag.getTagName(), exifIFD0Directory.getDescription(tag.getTagType()));
					});

					node.setProperty(traits.key(ImageTraitDefinition.EXIF_IFD0_DATA_PROPERTY), exifIFD0DataJson.toString());
					exifDataJson.putOnce(ImageTraitDefinition.EXIF_IFD0_DATA_PROPERTY, exifIFD0DataJson);
				}

				if (exifSubIFDDirectory != null) {

					final JSONObject exifSubIFDDataJson = new JSONObject();

					exifSubIFDDirectory.getTags().forEach((tag) -> {
						exifSubIFDDataJson.put(tag.getTagName(), exifSubIFDDirectory.getDescription(tag.getTagType()));
					});

					node.setProperty(traits.key(ImageTraitDefinition.EXIF_SUB_IFD_DATA_PROPERTY), exifSubIFDDataJson.toString());
					exifDataJson.putOnce(ImageTraitDefinition.EXIF_SUB_IFD_DATA_PROPERTY, exifSubIFDDataJson);
				}

				if (gpsDirectory != null) {

					final JSONObject exifGpsDataJson = new JSONObject();

					gpsDirectory.getTags().forEach((tag) -> {
						exifGpsDataJson.put(tag.getTagName(), gpsDirectory.getDescription(tag.getTagType()));
					});

					node.setProperty(traits.key(ImageTraitDefinition.GPS_DATA_PROPERTY), exifGpsDataJson.toString());
					exifDataJson.putOnce(ImageTraitDefinition.GPS_DATA_PROPERTY, exifGpsDataJson);
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

	public static class Base64URIData {

		private final String contentType;
		private final String data;

		public Base64URIData(final String rawData) {

			final String[] parts = StringUtils.split(rawData, ",");

			data        = parts[1];
			contentType = StringUtils.substringBetween(parts[0], "data:", ";base64");
		}

		public String getContentType() {

			return contentType;
		}

		public String getData() {

			return data;
		}
	}

	public static class Thumbnail {

		public enum Format {

			png, jpg, jpeg, gif
		}

		public static Format defaultFormat = Format.png;

		private byte[] bytes;
		private int height;
		private int width;
		private Format format;

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

		public String getContentType() {

			final Format f = format != null ? format : defaultFormat;

			return "image/" + (f == Format.jpg ? "jpeg" : f.name());
		}
	}
}
