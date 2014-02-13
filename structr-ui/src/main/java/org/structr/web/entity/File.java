/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Structr is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr. If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import org.apache.commons.io.FileUtils;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.IntProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.Property;
import org.structr.core.Services;

//~--- JDK imports ------------------------------------------------------------
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.property.StringProperty;
import org.structr.web.common.FileHelper;

//~--- classes ----------------------------------------------------------------
/**
 * A file that stores its binary content on disk.
 *
 * @author Axel Morgner
 */
public class File extends AbstractFile implements Linkable {

	private static final Logger logger = Logger.getLogger(File.class.getName());

	public static final Property<String> contentType = new StringProperty("contentType").indexedWhenEmpty();
	public static final Property<String> relativeFilePath = new StringProperty("relativeFilePath");
	public static final Property<Long> size = new LongProperty("size").indexed();
	public static final Property<String> url = new StringProperty("url");
	public static final Property<Long> checksum = new LongProperty("checksum").unvalidated();
	public static final Property<Integer> cacheForSeconds = new IntProperty("cacheForSeconds");

	public static final View publicView = new View(File.class, PropertyView.Public, type, name, contentType, size, url, owner);
	public static final View uiView = new View(File.class, PropertyView.Ui, type, contentType, relativeFilePath, size, url, parent, checksum, cacheForSeconds, owner);

	@Override
	public void onNodeDeletion() {

		String filePath = null;
		try {
			final String path = getRelativeFilePath();

			if (path != null) {

				filePath = FileHelper.getFilePath(path);

				java.io.File toDelete = new java.io.File(filePath);

				if (toDelete.exists() && toDelete.isFile()) {

					toDelete.delete();
				}
			}

		} catch (Throwable t) {

			logger.log(Level.WARNING, "Exception while trying to delete file {0}: {1}", new Object[]{filePath, t});

		}

	}

	@Override
	public void afterCreation(SecurityContext securityContext) {

		try {

			final String filesPath = Services.getInstance().getConfigurationValue(Services.FILES_PATH);
			java.io.File fileOnDisk = new java.io.File(filesPath + "/" + getRelativeFilePath());

			if (fileOnDisk.exists()) {
				return;
			}

			fileOnDisk.getParentFile().mkdirs();

			try {

				fileOnDisk.createNewFile();

			} catch (IOException ex) {

				logger.log(Level.SEVERE, "Could not create file", ex);
				return;
			}

			setProperty(checksum, FileHelper.getChecksum(File.this));

			long fileSize = FileHelper.getSize(File.this);
			if (fileSize > 0) {
				setProperty(size, fileSize);
			}

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, "Could not create file", ex);

		}

	}

	public String getUrl() {

		return getProperty(File.url);

	}

	public String getContentType() {

		return getProperty(File.contentType);

	}

	public Long getSize() {

		return getProperty(size);

	}

	public Long getChecksum() {

		return getProperty(checksum);

	}

	public String getFormattedSize() {

		return FileUtils.byteCountToDisplaySize(getSize());

	}

	public String getRelativeFilePath() {

		return getProperty(File.relativeFilePath);

	}

//	public URL getFileLocation() {
//
//		final String filesPath = Services.getInstance().getConfigurationValue(Services.FILES_PATH);
//		final String urlString = "file://" + filesPath + "/" + getRelativeFilePath();
//
//		try {
//
//			return new URL(urlString);
//
//		} catch (MalformedURLException mue) {
//
//			logger.log(Level.SEVERE, "Invalid URL: {0}", urlString);
//
//		}
//
//		return null;
//
//	}
	public InputStream getInputStream() {

		final String path = getRelativeFilePath();

		if (path != null) {

			final String filePath = FileHelper.getFilePath(path);
			final App app = StructrApp.getInstance(securityContext);

			FileInputStream fis = null;
			try {

				java.io.File fileOnDisk = new java.io.File(filePath);

				// Return file input stream and save checksum and size after closing
				fis = new FileInputStream(fileOnDisk);

				return fis;

			} catch (FileNotFoundException e) {
				logger.log(Level.SEVERE, "File not found: {0}", new Object[]{path});

				if (fis != null) {

					try {

						fis.close();

					} catch (IOException ignore) {
					}

				}
			}
		}

		return null;

	}

	public OutputStream getOutputStream() {

		final String path = getRelativeFilePath();

		if (path != null) {

			final String filePath = FileHelper.getFilePath(path);

			try {

				java.io.File fileOnDisk = new java.io.File(filePath);

				// Return file output stream and save checksum and size after closing
				FileOutputStream fos = new FileOutputStream(fileOnDisk) {

					@Override
					public void close() throws IOException {

						super.close();

						try {
							setProperty(checksum, FileHelper.getChecksum(File.this));
							setProperty(size, FileHelper.getSize(File.this));

						} catch (FrameworkException ex) {

							logger.log(Level.SEVERE, "Could not determine or save checksum and size after closing file output stream", ex);

						}
					}
				};

				return fos;

			} catch (FileNotFoundException e) {
				logger.log(Level.SEVERE, "File not found: {0}", new Object[]{path});
			}

		}

		return null;

	}

	public static String getDirectoryPath(final String uuid) {

		return (uuid != null)
			? uuid.substring(0, 1) + "/" + uuid.substring(1, 2) + "/" + uuid.substring(2, 3) + "/" + uuid.substring(3, 4)
			: null;

	}

	//~--- set methods ----------------------------------------------------
	public void setRelativeFilePath(final String filePath) throws FrameworkException {
		setProperty(File.relativeFilePath, filePath);
	}

	public void setUrl(final String url) throws FrameworkException {
		setProperty(File.url, url);
	}

	public void setContentType(final String contentType) throws FrameworkException {
		setProperty(File.contentType, contentType);
	}

	public void setSize(final Long size) throws FrameworkException {
		setProperty(File.size, size);
	}

	public void setChecksum(final Long checksum) throws FrameworkException {
		setProperty(File.checksum, checksum);
	}
}
