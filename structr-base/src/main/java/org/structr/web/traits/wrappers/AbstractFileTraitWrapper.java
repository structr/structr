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
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.files.sync.StorageSyncService;
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
		wrappedObject.setProperty(traits.key(AbstractFileTraitDefinition.PARENT_PROPERTY), parent);
	}

	@Override
	public void setHasParent(final boolean hasParent) throws FrameworkException {
		wrappedObject.setProperty(traits.key(AbstractFileTraitDefinition.HAS_PARENT_PROPERTY), hasParent);
	}

	@Override
	public Folder getParent() {

		final NodeInterface node = wrappedObject.getProperty(traits.key(AbstractFileTraitDefinition.PARENT_PROPERTY));
		if (node != null) {

			return node.as(Folder.class);
		}

		return null;
	}

	@Override
	public String getPath() {
		return wrappedObject.getProperty(traits.key(AbstractFileTraitDefinition.PATH_PROPERTY));
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

		final NodeInterface node = wrappedObject.getProperty(traits.key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY));
		if (node != null) {

			return node.as(StorageConfiguration.class);
		}

		return null;
	}

	@Override
	public boolean isMounted() {

		final StorageSyncService service = StructrApp.getInstance().getService(StorageSyncService.class);
		if (service != null && service.isSynchronized(getUuid())) {

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
		return wrappedObject.getProperty(traits.key(AbstractFileTraitDefinition.IS_EXTERNAL_PROPERTY));
	}

	@Override
	public boolean getHasParent() {
		return wrappedObject.getProperty(traits.key(AbstractFileTraitDefinition.HAS_PARENT_PROPERTY));
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
	public boolean includeInFrontendExport(final boolean recursive) {

		if (wrappedObject.getProperty(traits.key(AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY))) {

			return true;
		}

		if (recursive) {

			final NodeInterface parent = getParent();
			if (parent != null) {

				return parent.as(Folder.class).includeInFrontendExport(true);
			}
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

			wrappedObject.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), newName);

			valid = validatePath(securityContext, errorBuffer);

			if (valid) {

				logger.warn("File {} already exists, renaming to {}", originalPath, newName);

			} else {

				logger.warn("File {} already existed. Tried renaming to {} and failed. Aborting.", originalPath, newName);
			}
		}

		return valid;
	}

	// ----- private methods -----
	private boolean validatePath(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		final String filePath = getPath();
		if (filePath != null) {

			final PropertyKey<String> pathKey = traits.key(AbstractFileTraitDefinition.PATH_PROPERTY);
			final List<NodeInterface> files   = StructrApp.getInstance().nodeQuery(StructrTraits.ABSTRACT_FILE).key(pathKey, filePath).getAsList();

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
