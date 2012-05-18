/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.core.entity;

import org.apache.commons.io.FileUtils;

import org.neo4j.graphdb.Direction;

import org.structr.common.Path;
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.Services;
import org.structr.core.entity.RelationClass.Cardinality;
import org.structr.core.node.NodeService.NodeIndex;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;
import java.io.InputStream;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 *
 */
public class File extends Linkable {

	private static final Logger logger = Logger.getLogger(File.class.getName());

	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerPropertyRelation(File.class, Key.parentFolder, Folder.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToOne);
		EntityContext.registerPropertySet(File.class, PropertyView.All, Key.values());
		EntityContext.registerPropertySet(File.class, PropertyView.Ui, Key.values());

//              EntityContext.registerPropertyRelation(File.class, Key.parentFolder, Folder.class, RelType.HAS_CHILD, Direction.INCOMING, Cardinality.ManyToOne);
		EntityContext.registerSearchablePropertySet(File.class, NodeIndex.fulltext.name(), Key.values());
		EntityContext.registerSearchablePropertySet(File.class, NodeIndex.keyword.name(), Key.values());

	}

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey {

		contentType, relativeFilePath, size, url, parentFolder, checksum

	}

	//~--- methods --------------------------------------------------------

//      @Override
//      public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers) {
//              renderers.put(RenderMode.Direct, new FileStreamRenderer());
//      }
	@Override
	public void onNodeDeletion() {

		try {

			java.io.File toDelete = new java.io.File(getFileLocation().toURI());

			if (toDelete.exists() && toDelete.isFile()) {

				toDelete.delete();
			}

		} catch (Throwable t) {

			logger.log(Level.WARNING, "Exception while trying to delete file {0}: {1}", new Object[] { getFileLocation(), t });

		}

	}

	//~--- get methods ----------------------------------------------------

	public String getUrl() {

		return getStringProperty(Key.url.name());

	}

	public String getContentType() {

		return getStringProperty(Key.contentType.name());

	}

	public long getSize() {

		String relativeFilePath = getRelativeFilePath();

		if (relativeFilePath != null) {

			String filePath         = Services.getFilePath(Path.Files, relativeFilePath);
			java.io.File fileOnDisk = new java.io.File(filePath);
			long fileSize           = fileOnDisk.length();

			logger.log(Level.FINE, "File size of node {0} ({1}): {2}", new Object[] { getId(), filePath, fileSize });

			return fileSize;

		}

		return -1;

	}

	public String getChecksum() {

		String storedChecksum = getStringProperty(Key.checksum);

		if (storedChecksum != null) {

			return storedChecksum;
		}

		String relativeFilePath = getRelativeFilePath();

		if (relativeFilePath != null) {

			String filePath         = Services.getFilePath(Path.Files, relativeFilePath);
			java.io.File fileOnDisk = new java.io.File(filePath);
			String checksum;

			try {

				checksum = String.valueOf(FileUtils.checksumCRC32(fileOnDisk));

				logger.log(Level.FINE, "Checksum of file {0} ({1}): {2}", new Object[] { getId(), filePath, checksum });
				setChecksum(checksum);

				return checksum;

			} catch (Exception ex) {

				logger.log(Level.WARNING, "Could not calculate checksum of file " + filePath, ex);

			}

		}

		return null;

	}

	public String getFormattedSize() {

		return FileUtils.byteCountToDisplaySize(getSize());

	}

	public String getRelativeFilePath() {

		return getStringProperty(Key.relativeFilePath.name());

	}

	public URL getFileLocation() {

		String urlString = "file://" + Services.getFilesPath() + "/" + getRelativeFilePath();

		try {

			return new URL(urlString);

		} catch (MalformedURLException mue) {

			logger.log(Level.SEVERE, "Invalid URL: {0}", urlString);

		}

		return null;

	}

	public InputStream getInputStream() {

		URL url        = null;
		InputStream in = null;

		try {

			url = getFileLocation();

			return url.openStream();

		} catch (IOException e) {

			logger.log(Level.SEVERE, "Error while reading from {0}", new Object[] { url, e.getMessage() });

			if (in != null) {

				try {

					in.close();

				} catch (IOException ignore) {}

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

		setProperty(Key.relativeFilePath.name(), filePath);

	}

	public void setUrl(final String url) throws FrameworkException {

		setProperty(Key.url.name(), url);

	}

	public void setContentType(final String contentType) throws FrameworkException {

		setProperty(Key.contentType.name(), contentType);

	}

	public void setSize(final long size) throws FrameworkException {

		setProperty(Key.size.name(), size);

	}

	public void setChecksum(final String checksum) throws FrameworkException {

		setProperty(Key.checksum.name(), checksum);

	}

}
