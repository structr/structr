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
package org.structr.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.storage.config.StorageProviderConfig;
import org.structr.storage.config.StorageProviderConfigFactory;
import org.structr.storage.local.LocalFSStorageProvider;
import org.structr.storage.memory.InMemoryStorageProvider;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.Folder;

import java.lang.reflect.InvocationTargetException;

public abstract class StorageProviderFactory {
	private static final Logger logger = LoggerFactory.getLogger(StorageProviderFactory.class);
	public static StorageProvider getStorageProvider(final AbstractFile file) {

		return getSpecificStorageProvider(file, getProviderConfigName(file));
	}

	public static StorageProvider getSpecificStorageProvider(final AbstractFile file, final String forcedProvider) {
		final StorageProviderConfig config = StorageProviderConfigFactory.getConfigByName(forcedProvider);
		final Class<? extends StorageProvider> storageProviderClass = config.StorageProviderClass();

		if (storageProviderClass != null) {

			try {

				return storageProviderClass.getDeclaredConstructor(AbstractFile.class, StorageProviderConfig.class).newInstance(file, config);
			} catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {

				logger.error("Could not instantiate storage provider.", ex);
			}
		}

		return null;
	}

	private static String getProviderConfigName(final AbstractFile abstractFile) {

		if (abstractFile.getStorageProvider() != null) {

			return abstractFile.getStorageProvider();
		}

		final Folder parentFolder = abstractFile.getParent();

		if (parentFolder != null) {

			if (parentFolder.getStorageProvider() != null) {

				return parentFolder.getStorageProvider();
			} else {

				return getProviderConfigName(parentFolder);
			}
		}

		return null;
	}
}
