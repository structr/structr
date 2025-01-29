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
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StartNodes;
import org.structr.core.property.StringProperty;
import org.structr.storage.StorageProvider;
import org.structr.web.entity.relationship.AbstractFileCONFIGURED_BYStorageConfiguration;
import org.structr.web.entity.relationship.StorageConfigurationCONFIG_ENTRYStorageConfigurationEntry;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Storage container for mount configuration entries.
 */

public class StorageConfiguration extends AbstractNode {

	public static final Property<Iterable<StorageConfigurationEntry>> entriesProperty = new EndNodes<>("entries", StorageConfigurationCONFIG_ENTRYStorageConfigurationEntry.class).partOfBuiltInSchema();
	public static final Property<Iterable<AbstractFile>> foldersProperty              = new StartNodes<>("folders", AbstractFileCONFIGURED_BYStorageConfiguration.class).partOfBuiltInSchema();
	//type.relate(StorageConfiguration.class, "CONFIGURED_BY", Cardinality.ManyToOne, "folders", "storageConfiguration").setPermissionPropagation(PropagationDirection.Both).setReadPermissionPropagation(PropagationMode.Add).setCascadingCreate(JsonSchema.Cascade.sourceToTarget);

	public static final Property<String> nameProperty     = new StringProperty("name").indexed().unique().notNull().partOfBuiltInSchema();
	public static final Property<String> providerProperty = new StringProperty("provider").indexed().notNull().partOfBuiltInSchema();

	public static final View uiView = new View(StorageConfiguration.class, PropertyView.Ui,
		nameProperty, providerProperty, entriesProperty
	);

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidPropertyNotNull(this, StorageConfiguration.nameProperty, errorBuffer);
		valid &= ValidationHelper.isValidUniqueProperty(this, StorageConfiguration.nameProperty, errorBuffer);
		valid &= ValidationHelper.isValidPropertyNotNull(this, StorageConfiguration.providerProperty, errorBuffer);

		return valid;
	}

	public Iterable<StorageConfigurationEntry> getEntries() {
		return getProperty(entriesProperty);
	}

	public Map<String, String> getConfiguration() {

		final Map<String, String> data = new LinkedHashMap<>();

		for (final StorageConfigurationEntry entry : getEntries()) {

			data.put(entry.getName(), entry.getValue());
		}

		return data;
	}

	// ----- default methods -----
	public Class<? extends StorageProvider> getStorageProviderImplementation() {

		final Logger logger = LoggerFactory.getLogger(StorageConfiguration.class);

		try {

			Class<?> foundClass = Class.forName(this.getProperty("provider"));
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

	public StorageConfigurationEntry addEntry(final String key, final String value) throws FrameworkException {

		return StructrApp.getInstance().create(StorageConfigurationEntry.class,
			new NodeAttribute<>(StructrApp.key(StorageConfigurationEntry.class, "configuration"), this),
			new NodeAttribute<>(StructrApp.key(StorageConfigurationEntry.class, "name"),          key),
			new NodeAttribute<>(StructrApp.key(StorageConfigurationEntry.class, "value"),         value)
		);
	}
}