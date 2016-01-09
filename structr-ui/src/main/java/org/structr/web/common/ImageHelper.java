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
package org.structr.web.common;

import com.mortennobel.imagescaling.ResampleOp;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.web.entity.Image;
import org.structr.util.Base64;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.apache.commons.io.IOUtils;
import org.structr.common.SecurityContext;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.PropertyMap;
import static org.structr.web.common.FileHelper.setFileData;

/**
 *
 *
 */

import org.structr.dynamic.File;

public abstract class ImageHelper extends FileHelper {

	private static final Logger logger = Logger.getLogger(ImageHelper.class.getName());
	//private static Thumbnail tn        = new Thumbnail();

	//~--- methods --------------------------------------------------------

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

		return createImage(securityContext, IOUtils.toByteArray(imageStream), contentType, imageType, name, markAsThumbnail);

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
	public static Image createImage(final SecurityContext securityContext, final byte[] imageData, final String contentType, final Class<? extends Image> imageType, final String name, final boolean markAsThumbnail)
		throws FrameworkException, IOException {

		PropertyMap props                          = new PropertyMap();

		props.put(AbstractNode.type, imageType == null ? Image.class.getSimpleName() : imageType.getSimpleName());
		props.put(Image.isThumbnail, markAsThumbnail);
		props.put(AbstractNode.name, name);

		Image newImage = StructrApp.getInstance(securityContext).create(imageType, props);

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

	public static Thumbnail createThumbnail(final Image originalImage, final int maxWidth, final int maxHeight) {

		return createThumbnail(originalImage, maxWidth, maxHeight, false);

	}

	public static Thumbnail createThumbnail(final Image originalImage, final int maxWidth, final int maxHeight, final boolean crop) {

		// String contentType = (String) originalImage.getProperty(Image.CONTENT_TYPE_KEY);
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final Thumbnail tn               = new Thumbnail();

		try (final InputStream in = originalImage.getInputStream()) {

			if (in == null) {

				logger.log(Level.FINE, "InputStream of original image {0} ({1}) is null", new Object[] { originalImage.getName(), originalImage.getId() });
				return null;
			}

			final long start     = System.nanoTime();
			BufferedImage source = null;

			// read image
			source = ImageIO.read(in);
			if (source != null) {

				int sourceWidth  = source.getWidth();
				int sourceHeight = source.getHeight();

				// Update image dimensions
				originalImage.setProperty(Image.width, sourceWidth);
				originalImage.setProperty(Image.height, sourceHeight);

				// float aspectRatio = sourceWidth/sourceHeight;
				float scaleX = 1.0f * sourceWidth / maxWidth;
				float scaleY = 1.0f * sourceHeight / maxHeight;
				float scale;

				if (crop) {

					scale = Math.min(scaleX, scaleY);
				} else {

					scale = Math.max(scaleX, scaleY);
				}

				// Don't scale up
				if (scale > 1.0) {

					final int destWidth  = Math.max(4, Math.round(sourceWidth / scale));
					final int destHeight = Math.max(4, Math.round(sourceHeight / scale));

					//System.out.println(destWidth + " / " + destHeight);

					ResampleOp resampleOp   = new ResampleOp(destWidth, destHeight);
					BufferedImage resampled = resampleOp.filter(source, null);
					BufferedImage result    = null;

					if (crop) {

						int offsetX = Math.abs(maxWidth - destWidth) / 2;
						int offsetY = Math.abs(maxHeight - destHeight) / 2;

						logger.log(Level.FINE, "Offset and Size (x,y,w,h): {0},{1},{2},{3}", new Object[] { offsetX, offsetY, maxWidth, maxHeight });

						result = resampled.getSubimage(offsetX, offsetY, maxWidth, maxHeight);

						tn.setWidth(maxWidth);
						tn.setHeight(maxHeight);

					} else {

						result = resampled;

						tn.setWidth(destWidth);
						tn.setHeight(destHeight);

					}

					ImageIO.write(result, Thumbnail.FORMAT, baos);

				} else {

					// Thumbnail is source image
					ImageIO.write(source, Thumbnail.FORMAT, baos);
					tn.setWidth(sourceWidth);
					tn.setHeight(sourceHeight);
				}

			} else {

				logger.log(Level.FINE, "Thumbnail could not be created");
				return null;
			}

			long end  = System.nanoTime();
			long time = (end - start) / 1000000;

			logger.log(Level.FINE, "Thumbnail created. Reading, scaling and writing took {0} ms", time);
			tn.setBytes(baos.toByteArray());

			return tn;

		} catch (Throwable t) {

			logger.log(Level.WARNING, "Unable to create thumbnail for image with ID {0}.", originalImage.getUuid());
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

		ByteArrayInputStream in = new ByteArrayInputStream(original);

		// If JPEG image starts with ff d9 ffd8, strip this sequence from the beginning
		// FF D9 = EOI (end of image)
		// FF D8 = SOI (start of image)
		if ((original[0] == (byte) 0xff) && (original[1] == (byte) 0xd9) && (original[2] == (byte) 0xff) && (original[3] == (byte) 0xd8)) {

			in.skip(4);
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BufferedImage source;

		try {

			source = ImageIO.read(in);

			// If ImageIO cannot read it, return original
			if (source == null) {

				return original;
			}

			ImageIO.write(source, "jpeg", out);

		} catch (IOException ex) {

			logger.log(Level.SEVERE, null, ex);

			if (in != null) {

				try {

					in.close();

				} catch (IOException ignore) {}

			}

		} finally {}

		return out.toByteArray();

	}

	//~--- get methods ----------------------------------------------------

	public static String getBase64String(final File file) {

		try {
         InputStream dataStream = file.getInputStream();

         if (dataStream != null)
			   return Base64.encodeToString(IOUtils.toByteArray(file.getInputStream()), false);

		} catch (IOException ex) {
			logger.log(Level.SEVERE, "Could not get base64 string from file ", ex);
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

		String extension         = urlString.toLowerCase().substring(urlString.lastIndexOf(".") + 1);
		String[] imageExtensions = {

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

		String extension         = urlString.toLowerCase().substring(urlString.lastIndexOf(".") + 1);
		String[] imageExtensions = { "swf" };

		for (String ext : imageExtensions) {

			if (ext.equals(extension)) {

				return true;
			}

		}

		return false;

	}

	//~--- inner classes --------------------------------------------------

	public static class Base64URIData {

		private String contentType;
		private String data;

		//~--- constructors -------------------------------------------

		public Base64URIData(final String rawData) {

			String[] parts = StringUtils.split(rawData, ",");

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

		public static final String FORMAT = "png";

		//~--- fields -------------------------------------------------

		private byte[] bytes;
		private int height;
		private int width;

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

	}

}
