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
package org.structr.web.traits.wrappers;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UniqueToken;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.files.external.DirectoryWatchService;
import org.structr.storage.StorageProvider;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.Folder;
import org.structr.web.entity.StorageConfiguration;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;

import java.util.List;

/**
 * Base class for filesystem objects in structr.
 */
public class AbstractFileTraitWrapper extends AbstractNodeTraitWrapper implements AbstractFile {

	public AbstractFileTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getName() {
		return wrappedObject.getName();
	}

	@Override
	public void setParent(final Folder parent) throws FrameworkException {
		wrappedObject.setProperty(traits.key("parent"), parent);
	}

	@Override
	public void setHasParent(final boolean hasParent) throws FrameworkException {
		wrappedObject.setProperty(traits.key("hasParent"), hasParent);
	}

	@Override
	public Folder getParent() {

		final NodeInterface node = wrappedObject.getProperty(traits.key("parent"));
		if (node != null) {

			return node.as(Folder.class);
		}

		return null;
	}

	@Override
	public String getPath() {
		return wrappedObject.getProperty(traits.key("path"));
	}

	@Override
	public String getFolderPath() {

		String folderPath = wrappedObject.getName();
		if (folderPath == null) {

			folderPath = wrappedObject.getUuid();
		}

		if (getHasParent()) {

			NodeInterface parentFolder = getParent();
			while (parentFolder != null) {

				folderPath   = parentFolder.getName().concat("/").concat(folderPath);
				parentFolder = parentFolder.as(Folder.class).getParent();
			}
		}

		final String path = "/".concat(folderPath);

		return path;
	}

	@Override
	public StorageConfiguration getStorageConfiguration() {

		final NodeInterface node = wrappedObject.getProperty(traits.key("storageConfiguration"));
		if (node != null) {

			return node.as(StorageConfiguration.class);
		}

		return null;
	}

	@Override
	public boolean isMounted() {

		final StorageProvider provider             = StorageProviderFactory.getStorageProvider(this);
		final boolean hasMountTarget               = provider.getConfig() != null && provider.getConfig().getConfiguration().get("mountTarget") != null;
		final boolean watchServiceHasMountedFolder = Settings.Services.getValue("").contains("DirectoryWatchService") && StructrApp.getInstance().getService(DirectoryWatchService.class) != null && StructrApp.getInstance().getService(DirectoryWatchService.class).isMounted(getUuid());

		if (hasMountTarget && watchServiceHasMountedFolder) {
			return true;
		}

		final NodeInterface parent = getParent();
		if (parent != null) {

			// recurse
			return parent.as(Folder.class).isMounted();
		}

		return false;
	}

	@Override
	public boolean isExternal() {
		return wrappedObject.getProperty(traits.key("isExternal"));
	}

	@Override
	public boolean getHasParent() {
		return wrappedObject.getProperty(traits.key("hasParent"));
	}

	@Override
	public void setHasParent() throws FrameworkException {

		synchronized (wrappedObject) {

			final SecurityContext ctx = wrappedObject.getSecurityContext();

			wrappedObject.setSecurityContext(SecurityContext.getSuperUserInstance());
			setHasParent(getParent() != null);
			wrappedObject.setSecurityContext(ctx);

		}
	}

	@Override
	public boolean isBinaryDataAccessible(final SecurityContext securityContext) {
		return !isExternal() || isMounted();
	}

	@Override
	public boolean includeInFrontendExport() {

		if (wrappedObject.getProperty(traits.key("includeInFrontendExport"))) {

			return true;
		}

		final NodeInterface parent = getParent();
		if (parent != null) {

			// recurse
			return parent.as(Folder.class).includeInFrontendExport();
		}

		return false;
	}

	@Override
	public boolean validateAndRenameFileOnce(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		boolean valid = validatePath(securityContext, null);

		if (!valid) {

			final Logger logger       = LoggerFactory.getLogger(AbstractFileTraitDefinition.class);
			final String originalPath = getPath();
			final String newName      = getRenamedFilename(getName());

			wrappedObject.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), newName);

			valid = validatePath(securityContext, errorBuffer);

			if (valid) {

				logger.warn("File {} already exists, renaming to {}", new Object[] { originalPath, newName });

			} else {

				logger.warn("File {} already existed. Tried renaming to {} and failed. Aborting.", new Object[] { originalPath, newName });
			}
		}

		return valid;
	}

	@Override
	public boolean renameMountedAbstractFile(final Folder thisFolder, final AbstractFile file, final String path, final String previousName) {

		// ToDo: Implement renameMountedAbstractFile for new fs layer
		throw new UnsupportedOperationException("Not implemented for new fs abstraction layer");
		/*
		final String _mountTarget = thisFolder.getMountTarget();
		final Folder parentFolder = thisFolder.getParent();

		if (_mountTarget != null) {

			final String fullOldPath         = Folder.removeDuplicateSlashes(_mountTarget + "/" + path + "/" + previousName);
			final String fullNewPath         = Folder.removeDuplicateSlashes(_mountTarget + "/" + path + "/" + file.getProperty(File.name));
			final java.io.File oldFileOnDisk = new java.io.File(fullOldPath);
			final java.io.File newFileOnDisk = new java.io.File(fullNewPath);

			if (newFileOnDisk.exists()) {

				final Logger logger = LoggerFactory.getLogger(Folder.class);
				logger.error("Preventing renaming file {} from {} to {} because a file with the target name already exists", file, previousName, file.getProperty(File.name));

			} else if (oldFileOnDisk.exists()) {

				try {

					final boolean renameResult = oldFileOnDisk.renameTo(newFileOnDisk);

					if (!renameResult) {
						final Logger logger = LoggerFactory.getLogger(Folder.class);
						logger.error("Renaming file failed {}: From {} to {}", file, previousName, file.getProperty(File.name));
					}

					return renameResult;

				} catch (Throwable t) {

					final Logger logger = LoggerFactory.getLogger(Folder.class);
					logger.error("Unable to rename file {}: {}", file, t.getMessage());
				}
			}

		} else if (parentFolder != null) {

			return AbstractFile.renameMountedAbstractFile(parentFolder, file, thisFolder.getProperty(Folder.name) + "/" + path, previousName);

		} else {

			// this should not happen. This means a file/folder marked as "isExternal" has no mounted folder in its parents
			final Logger logger = LoggerFactory.getLogger(Folder.class);
			logger.error("Unable to rename file {}: Mount target not found!", file);

		}

		return false;

		 */
	}

	// ----- private methods -----
	private boolean validatePath(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		final String filePath = getPath();
		if (filePath != null) {

			final PropertyKey<String> pathKey = traits.key("path");
			final List<NodeInterface> files   = StructrApp.getInstance().nodeQuery(StructrTraits.ABSTRACT_FILE).and(pathKey, filePath).getAsList();

			for (final NodeInterface file : files) {

				if (!file.getUuid().equals(getUuid())) {

					if (errorBuffer != null) {

						final UniqueToken token = new UniqueToken(StructrTraits.ABSTRACT_FILE, pathKey.jsonName(), file.getUuid(), getUuid(), filePath);
						token.setValue(filePath);

						errorBuffer.add(token);
					}

					return false;
				}
			}
		}

		return true;
	}

	private String getRenamedFilename(final String oldName) {

		final String insertionPosition  = Settings.UniquePathsInsertionPosition.getValue();
		final String timestamp          = FileHelper.getDateString();

		switch (insertionPosition) {

			case "beforeextension":
				if (oldName.contains(".")) {
					final int lastDot = oldName.lastIndexOf(".");
					return oldName.substring(0, lastDot).concat("_").concat(timestamp).concat(oldName.substring(lastDot));

				} else {
					return oldName.concat("_").concat(timestamp);
				}

			case "start":
				return timestamp.concat("_").concat(oldName);

			case "end":
			default:
				return oldName.concat("_").concat(timestamp);
		}

	}
}
