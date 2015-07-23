/**
 * Copyright (C) 2010-2015 Morgner UG (haftungsbeschränkt)
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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.parboiled.common.StringUtils;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import static org.structr.core.GraphObject.type;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import static org.structr.core.graph.NodeInterface.name;
import static org.structr.core.graph.NodeInterface.owner;
import org.structr.core.graph.Tx;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.schema.action.JavaScriptSource;
import org.structr.web.common.FileHelper;
import org.structr.web.common.ImageHelper;
import static org.structr.web.entity.AbstractFile.parent;
import org.structr.web.entity.relation.Folders;
import org.structr.web.property.PathProperty;

/**
 *
 * @author Christian Morgner
 */
public class FileBase extends AbstractFile implements Linkable, JavaScriptSource {

	private static final Logger logger = Logger.getLogger(FileBase.class.getName());

	public static final Property<String> contentType             = new StringProperty("contentType").indexedWhenEmpty();
	public static final Property<String> relativeFilePath        = new StringProperty("relativeFilePath").readOnly();
	public static final Property<Long> size                      = new LongProperty("size").indexed().readOnly();
	public static final Property<String> url                     = new StringProperty("url");
	public static final Property<Long> checksum                  = new LongProperty("checksum").unvalidated().readOnly();
	public static final Property<Integer> cacheForSeconds        = new IntProperty("cacheForSeconds");
	public static final Property<Integer> version                = new IntProperty("version").indexed().readOnly();
	public static final Property<String> path                    = new PathProperty("path").indexed().readOnly();
	public static final Property<Boolean> isFile                 = new BooleanProperty("isFile").defaultValue(true).readOnly();

	public static final View publicView = new View(FileBase.class, PropertyView.Public, type, name, contentType, size, url, owner, path, isFile);
	public static final View uiView = new View(FileBase.class, PropertyView.Ui, type, contentType, relativeFilePath, size, url, parent, checksum, version, cacheForSeconds, owner, path, isFile);

	@Override
	public void onNodeCreation() {

		final String uuid = getUuid();
		final String filePath = getDirectoryPath(uuid) + "/" + uuid;

		try {
			unlockReadOnlyPropertiesOnce();
			setProperty(relativeFilePath, filePath);
		} catch (Throwable t) {

			logger.log(Level.WARNING, "Exception while trying to set relative file path {0}: {1}", new Object[]{filePath, t});

		}
	}

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

			unlockReadOnlyPropertiesOnce();
			setProperty(checksum, FileHelper.getChecksum(FileBase.this));

			unlockReadOnlyPropertiesOnce();
			setProperty(version, 0);

			long fileSize = FileHelper.getSize(FileBase.this);
			if (fileSize > 0) {
				unlockReadOnlyPropertiesOnce();
				setProperty(size, fileSize);
			}

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, "Could not create file", ex);

		}

	}

	@Override
	public String getPath() {
		return FileHelper.getFolderPath(this);
	}

	public String getRelativeFilePath() {

		return getProperty(FileBase.relativeFilePath);

	}

	public String getUrl() {

		return getProperty(FileBase.url);

	}

	public String getContentType() {

		return getProperty(FileBase.contentType);

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

	public static String getDirectoryPath(final String uuid) {

		return (uuid != null)
			? uuid.substring(0, 1) + "/" + uuid.substring(1, 2) + "/" + uuid.substring(2, 3) + "/" + uuid.substring(3, 4)
			: null;

	}

	public void increaseVersion() throws FrameworkException {

		final Integer _version = getProperty(FileBase.version);

		unlockReadOnlyPropertiesOnce();
		if (_version == null) {

			setProperty(FileBase.version, 1);

		} else {

			setProperty(FileBase.version, _version + 1);
		}
	}

	public InputStream getInputStream() {

		final String path = getRelativeFilePath();

		if (path != null) {

			final String filePath = FileHelper.getFilePath(path);

			FileInputStream fis = null;
			try {

				java.io.File fileOnDisk = new java.io.File(filePath);

				// Return file input stream and save checksum and size after closing
				fis = new FileInputStream(fileOnDisk);

				return fis;

			} catch (FileNotFoundException e) {
				logger.log(Level.FINE, "File not found: {0}", new Object[]{path});

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

				final java.io.File fileOnDisk = new java.io.File(filePath);

				// Return file output stream and save checksum and size after closing
				FileOutputStream fos = new FileOutputStream(fileOnDisk) {

					private boolean closed = false;

					@Override
					public void close() throws IOException {

						if (closed) {
							return;
						}

						try (Tx tx = StructrApp.getInstance().tx()) {

							super.close();

							final String _contentType = FileHelper.getContentMimeType(FileBase.this);

							unlockReadOnlyPropertiesOnce();
							setProperty(checksum, FileHelper.getChecksum(FileBase.this));

							unlockReadOnlyPropertiesOnce();
							setProperty(size, FileHelper.getSize(FileBase.this));
							setProperty(contentType, _contentType);

							if (StringUtils.startsWith(_contentType, "image") || ImageHelper.isImageType(getProperty(name))) {
								setProperty(NodeInterface.type, Image.class.getSimpleName());
							}

							increaseVersion();


							tx.success();

						} catch (FrameworkException ex) {

							logger.log(Level.SEVERE, "Could not determine or save checksum and size after closing file output stream", ex);

						}

						closed = true;
					}
				};

				return fos;

			} catch (FileNotFoundException e) {
				logger.log(Level.SEVERE, "File not found: {0}", new Object[]{path});
			}

		}

		return null;

	}

	public java.io.File getFileOnDisk() {

		final String path = getRelativeFilePath();
		if (path != null) {

			return new java.io.File(FileHelper.getFilePath(path));
		}

		return null;
	}

	// ----- interface Syncable -----
	@Override
	public List<GraphObject> getSyncData() throws FrameworkException {

		final List<GraphObject> data = super.getSyncData();

		// nodes
		data.add(getProperty(parent));
		data.add(getIncomingRelationship(Folders.class));

		return data;
	}

	// ----- interface JavaScriptSource -----
	@Override
	public String getJavascriptLibraryCode() {

		try (final InputStream is = getInputStream()) {

			return IOUtils.toString(is);

		} catch (IOException ioex) {
			ioex.printStackTrace();
		}

		return null;
	}
}
