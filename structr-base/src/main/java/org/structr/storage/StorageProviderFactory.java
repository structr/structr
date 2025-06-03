/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.storage.providers.local.LocalFSStorageProvider;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.Folder;
import org.structr.web.entity.StorageConfiguration;
import org.structr.web.traits.definitions.StorageConfigurationTraitDefinition;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Map.Entry;

public abstract class StorageProviderFactory {

	private static final Logger logger = LoggerFactory.getLogger(StorageProviderFactory.class);

	/**
	 * Creates a new storage configuration. Caution, this method does not check for uniqueness
	 * of the configuration name or validates the implementation class.
	 *
	 * @param name the name
	 * @param impl the class implementing StorageProvider
	 * @param configuration optional configuration options
	 * @return a new storage provider config with the given configuration, or null
	 */
	public static StorageConfiguration createConfig(final String name, final Class<? extends StorageProvider> impl, final Map<String, String> configuration) throws FrameworkException {

		final Traits traits = Traits.of(StructrTraits.STORAGE_CONFIGURATION);
		final App app       = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.create(StructrTraits.STORAGE_CONFIGURATION,
				new NodeAttribute<>(traits.key(StorageConfigurationTraitDefinition.NAME_PROPERTY),     name),
				new NodeAttribute<>(traits.key(StorageConfigurationTraitDefinition.PROVIDER_PROPERTY), impl.getName())
			);

			final StorageConfiguration sc = node.as(StorageConfiguration.class);

			if (configuration != null) {


				for (final Entry<String, String> c : configuration.entrySet()) {
					sc.addEntry(c.getKey(), c.getValue());
				}
			}

			tx.success();

			return sc;
		}
	}

	public static StorageProvider getStorageProvider(final AbstractFile file) {

		final StorageConfiguration config = StorageProviderFactory.getStorageConfiguration(file);
		if (config != null) {

			return getSpecificStorageProvider(file, config);
		}

		return getDefaultStorageProvider(file);
	}

	public static StorageProvider getSpecificStorageProvider(final AbstractFile file, final NodeInterface config) {

		if (config != null) {

			// Get config by name and get provider class to instantiate via reflection
			final Class<? extends StorageProvider> storageProviderClass = config.as(StorageConfiguration.class).getStorageProviderImplementation();
			if (storageProviderClass != null) {

				try {

					// Try to instantiate requested provider with given file and config
					return storageProviderClass.getDeclaredConstructor(AbstractFile.class, StorageConfiguration.class).newInstance(file, config);

				} catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {

					logger.error("Could not instantiate storage provider.", ex);
				}
			}
		}

		return getDefaultStorageProvider(file);
	}

	public static StorageProvider getDefaultStorageProvider(final AbstractFile file) {
		return new LocalFSStorageProvider(file);
	}

	public static AbstractFile getStorageConfigurationSupplier(final AbstractFile abstractFile) {

		// Check if abstract file itself offers a provider
		if (abstractFile.getStorageConfiguration() != null) {

			return abstractFile;
		}

		// check the files parent
		final Folder parentFolder = abstractFile.getParent();

		if (parentFolder != null) {

			if (parentFolder.getStorageConfiguration() != null) {

				return parentFolder;

			} else {

				// Parent did not have info, so recursively go up the hierarchy
				return getStorageConfigurationSupplier(parentFolder);
			}
		}

		return null;
	}

	// ----- private methods -----
	private static StorageConfiguration getStorageConfiguration(final AbstractFile abstractFile) {

		final AbstractFile supplier = getStorageConfigurationSupplier(abstractFile);
		if (supplier != null) {

			return supplier.getStorageConfiguration();
		}

		return null;
	}
}
