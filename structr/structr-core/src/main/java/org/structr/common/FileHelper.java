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



package org.structr.common;

import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicMatch;

import org.apache.commons.io.FileUtils;

import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.IOException;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Utility class
 *
 * @author axel
 */
public class FileHelper {

	private static final String UNKNOWN_MIME_TYPE = "application/octet-stream";
	private static final Logger logger            = Logger.getLogger(FileHelper.class.getName());

	//~--- methods --------------------------------------------------------

	/**
	 * Write binary data to a file and reference the file on disk at the given file node
	 *
	 * @param fileNode
	 * @param data
	 * @throws FrameworkException
	 * @throws IOException
	 */
	public static void writeToFile(final org.structr.core.entity.File fileNode, final byte[] data) throws FrameworkException, IOException {

		String uuid = fileNode.getStringProperty(AbstractNode.Key.uuid);

		if (uuid == null) {

			synchronized (fileNode) {

				uuid = UUID.randomUUID().toString().replaceAll("[\\-]+", "");
				
				fileNode.setProperty(AbstractNode.Key.uuid.name(), uuid);
			}

		}

		fileNode.setRelativeFilePath(org.structr.core.entity.File.getDirectoryPath(uuid) + "/" + uuid);

		java.io.File fileOnDisk = new java.io.File(Services.getFilesPath() + "/" + fileNode.getRelativeFilePath());

		fileOnDisk.getParentFile().mkdirs();
		FileUtils.writeByteArrayToFile(fileOnDisk, data);

	}

	//~--- get methods ----------------------------------------------------

	/**
	 * Return mime type of given file
	 *
	 * @param file
	 * @param ext
	 * @return
	 */
	public static String getContentMimeType(final File file) {

		MagicMatch match;

		try {

			match = Magic.getMagicMatch(file, false, true);

			return match.getMimeType();

		} catch (Exception e) {

			logger.log(Level.WARNING, "Could not determine content type");

		}

		return UNKNOWN_MIME_TYPE;

	}

	/**
	 * Return mime type of given byte array.
	 *
	 * Use on streams.
	 *
	 * @param bytes
	 * @return
	 */
	public static String getContentMimeType(final byte[] bytes) {

		MagicMatch match;

		try {

			match = Magic.getMagicMatch(bytes, true);

			return match.getMimeType();

		} catch (Exception e) {

			logger.log(Level.SEVERE, null, e);

		}

		return UNKNOWN_MIME_TYPE;

	}

	/**
	 * Return mime type of given file
	 *
	 * @param file
	 * @param ext
	 * @return
	 */
	public static String[] getContentMimeTypeAndExtension(final File file) {

		MagicMatch match;

		try {

			match = Magic.getMagicMatch(file, false, true);

			return new String[] { match.getMimeType(), match.getExtension() };

		} catch (Exception e) {

			logger.log(Level.SEVERE, null, e);

		}

		return new String[] { UNKNOWN_MIME_TYPE, ".bin" };

	}

	/**
	 * Return mime type of given byte array.
	 *
	 * Use on streams.
	 *
	 * @param bytes
	 * @return
	 */
	public static String[] getContentMimeTypeAndExtension(final byte[] bytes) {

		MagicMatch match;

		try {

			match = Magic.getMagicMatch(bytes, true);

			return new String[] { match.getMimeType(), match.getExtension() };

		} catch (Exception e) {

			logger.log(Level.SEVERE, null, e);

		}

		return new String[] { UNKNOWN_MIME_TYPE, ".bin" };

	}

}
