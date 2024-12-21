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
package org.structr.web.entity;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.traits.NodeTrait;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Base class for filesystem objects in structr.
 */
public interface AbstractFile extends NodeTrait {

	void setParent(final Folder parent) throws FrameworkException;
	void setHasParent(final boolean hasParent) throws FrameworkException;
	Folder getParent();

	String getPath();
	String getFolderPath();

	StorageConfiguration getStorageConfiguration();

	boolean isMounted();
	boolean isExternal();
	boolean getHasParent();

	void setHasParent() throws FrameworkException;

	boolean isBinaryDataAccessible(final SecurityContext securityContext);
	boolean includeInFrontendExport();

	boolean validateAndRenameFileOnce(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException;
	boolean renameMountedAbstractFile (final Folder thisFolder, final AbstractFile file, final String path, final String previousName);

	static java.io.File defaultGetFileOnDisk(final File fileBase, final boolean create) {

		final String uuid       = fileBase.getUuid();
		final String filePath   = Settings.FilesPath.getValue();
		final String uuidPath   = AbstractFile.getDirectoryPath(uuid);
		final String finalPath  = filePath + "/" + uuidPath + "/" + uuid;
		final Path path         = Paths.get(finalPath);
		final java.io.File file = path.toFile();

		// create parent directory tree
		file.getParentFile().mkdirs();

		// create file only if requested
		if (!file.exists() && create && !fileBase.isExternal()) {

			try {

				file.createNewFile();

			} catch (IOException ioex) {

				final Logger logger = LoggerFactory.getLogger(AbstractFileTraitDefinition.class);
				logger.error("Unable to create file {}: {}", file, ioex.getMessage());
			}
		}

		return file;
	}

	static String getDirectoryPath(final String uuid) {

		return (uuid != null)
			? uuid.substring(0, 1) + "/" + uuid.substring(1, 2) + "/" + uuid.substring(2, 3) + "/" + uuid.substring(3, 4)
			: null;

	}
}
