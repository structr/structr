/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.storage.providers.local;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.StorageConfiguration;

import java.io.IOException;
import java.nio.file.Path;

public class LocalFSHelper {

	private static final String MOUNT_TARGET_KEY = "mountTarget";
	private final StorageConfiguration config;

	public LocalFSHelper(final StorageConfiguration config) {
		this.config = config;
	}

	public java.io.File getFileOnDisk(final AbstractFile thisFile) {
		return getFileOnDisk(thisFile, false);
	}

	public java.io.File getFileOnDisk(final AbstractFile thisFile, final boolean create) {

		final Folder parentFolder = thisFile.getParent();
		return getFileOnDisk(parentFolder, thisFile.as(File.class), create);
	}

	public java.io.File getFileOnDisk(final Folder parentFolder, final File file, final boolean create) {

		// Check if configuration contains a mountTarget, indicating a mounted/mapped folder
		final String _mountTarget = config != null ? config.getConfiguration().get(MOUNT_TARGET_KEY) : null;
		if (_mountTarget != null) {

			final AbstractFile configSupplier = StorageProviderFactory.getStorageConfigurationSupplier(file);
			final Path relativeParentPath     = parentFolder != null ? Path.of(configSupplier.getPath()).relativize(Path.of(parentFolder.getPath())) : Path.of("/");

			final String fullPath             = Folder.removeDuplicateSlashes(_mountTarget + "/" + relativeParentPath + "/" + file.getName());
			final java.io.File fileOnDisk     = new java.io.File(fullPath);

			fileOnDisk.getParentFile().mkdirs();

			if (create && !parentFolder.isExternal()) {

				try {

					fileOnDisk.createNewFile();

				} catch (IOException ioex) {

					final Logger logger = LoggerFactory.getLogger(Folder.class);
					logger.error("Unable to create file {}: {}", file, ioex.getMessage());
				}
			}

			return fileOnDisk;

		}

		// default implementation (store in UUID-indexed tree)
		return AbstractFile.defaultGetFileOnDisk(file, create);
	}
}
