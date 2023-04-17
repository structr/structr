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
package org.structr.storage.local;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;

import java.io.IOException;

public abstract class LocalFSHelper {

	public static java.io.File getFileOnDisk(final AbstractFile thisFile) {
		return getFileOnDisk(thisFile, false);
	}

	public static java.io.File getFileOnDisk(final AbstractFile thisFile, final boolean create) {

		final Folder parentFolder = thisFile.getParent();
		if (parentFolder != null) {

			return getFileOnDisk(parentFolder, (File) thisFile, "", create);

		} else {

			return AbstractFile.defaultGetFileOnDisk((File) thisFile, create);
		}
	}

	public static java.io.File getFileOnDisk(final Folder thisFolder, final File file, final String path, final boolean create) {

		final String _mountTarget = thisFolder.getMountTarget();
		final Folder parentFolder = thisFolder.getParent();

		if (_mountTarget != null) {

			final String fullPath         = Folder.removeDuplicateSlashes(_mountTarget + "/" + path + "/" + file.getProperty(File.name));
			final java.io.File fileOnDisk = new java.io.File(fullPath);

			fileOnDisk.getParentFile().mkdirs();

			if (create && !thisFolder.isExternal()) {

				try {

					fileOnDisk.createNewFile();

				} catch (IOException ioex) {

					final Logger logger = LoggerFactory.getLogger(Folder.class);
					logger.error("Unable to create file {}: {}", file, ioex.getMessage());
				}
			}

			return fileOnDisk;

		} else if (parentFolder != null) {

			return getFileOnDisk(parentFolder, file, thisFolder.getProperty(Folder.name) + "/" + path, create);
		}

		// default implementation (store in UUID-indexed tree)
		return AbstractFile.defaultGetFileOnDisk(file, create);
	}
}
