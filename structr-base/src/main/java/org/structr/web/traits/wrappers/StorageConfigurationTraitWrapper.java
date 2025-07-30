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
package org.structr.web.traits.wrappers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.storage.StorageProvider;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.entity.StorageConfiguration;
import org.structr.web.entity.StorageConfigurationEntry;
import org.structr.web.traits.definitions.StorageConfigurationEntryTraitDefinition;
import org.structr.web.traits.definitions.StorageConfigurationTraitDefinition;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Storage container for mount configuration entries.
 */

public class StorageConfigurationTraitWrapper extends AbstractNodeTraitWrapper implements StorageConfiguration {

	public StorageConfigurationTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getName() {
		return wrappedObject.getName();
	}

	public Iterable<NodeInterface> getEntries() {
		return wrappedObject.getProperty(traits.key(StorageConfigurationTraitDefinition.ENTRIES_PROPERTY));
	}

	@Override
	public String getProvider() {
		return wrappedObject.getProperty(traits.key(StorageConfigurationTraitDefinition.PROVIDER_PROPERTY));
	}

	public Map<String, String> getConfiguration() {

		final Map<String, String> data = new LinkedHashMap<>();

		for (final NodeInterface node : getEntries()) {

			final StorageConfigurationEntry entry = node.as(StorageConfigurationEntry.class);

			data.put(entry.getName(), entry.getValue());
		}

		return data;
	}

	@Override
	public Class<? extends StorageProvider> getStorageProviderImplementation() {

		final Logger logger = LoggerFactory.getLogger(StorageConfiguration.class);

		try {

			Class<?> foundClass = this.getProvider() != null ? Class.forName(this.getProvider()) : null;


			if (foundClass == null) {

				return StorageProviderFactory.getDefaultStorageProviderClass();
			}

			if (StorageProvider.class.isAssignableFrom(foundClass)) {

				return foundClass.asSubclass(StorageProvider.class);
			}

			logger.error("Found class for given provider fully qualified class name, but found class does not extend the StorageProvider interface. Found Class: {}", foundClass.getName());
			return null;

		} catch (ClassNotFoundException ex) {

			logger.error("Unable to instantiate storage provider {}: {}", this.getName(), ex.getMessage());
			return null;
		}
	}

	@Override
	public NodeInterface addEntry(final String key, final String value) throws FrameworkException {

		final String type        = StructrTraits.STORAGE_CONFIGURATION_ENTRY;
		final Traits entryTraits = Traits.of(type);

		return StructrApp.getInstance().create(type,
			new NodeAttribute<>(entryTraits.key(StorageConfigurationEntryTraitDefinition.CONFIGURATION_PROPERTY), this),
			new NodeAttribute<>(entryTraits.key(StorageConfigurationEntryTraitDefinition.NAME_PROPERTY),          key),
			new NodeAttribute<>(entryTraits.key(StorageConfigurationEntryTraitDefinition.VALUE_PROPERTY),         value)
		);
	}
}