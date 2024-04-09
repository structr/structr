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

import org.structr.api.graph.PropagationDirection;
import org.structr.api.graph.PropagationMode;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.SchemaService;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.graph.Cardinality;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.storage.StorageProvider;

/**
 * Storage container for mount configuration entries.
 */

public interface StorageConfiguration extends NodeInterface {

	static class Impl { static {

		final JsonSchema schema    = SchemaService.getDynamicSchema();
		final JsonObjectType type  = schema.addType("StorageConfiguration");
		final JsonObjectType entry = schema.addType("StorageConfigurationEntry");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/StorageConfiguration"));
		type.setExtends(URI.create("#/definitions/AbstractNode"));
		type.setCategory("core");

		// unique name of a storage configuration
		type.addStringProperty("name", PropertyView.Ui).setIndexed(true).setUnique(true).setRequired(true);

		// fully-qualified class name of the storage provider to use
		type.addStringProperty("provider", PropertyView.Ui).setIndexed(true).setRequired(true);

		type.relate(entry, "CONFIG_ENTRY", Cardinality.OneToMany, "configuration", "entries").setPermissionPropagation(PropagationDirection.Both).setReadPermissionPropagation(PropagationMode.Add).setCascadingCreate(JsonSchema.Cascade.sourceToTarget).setCascadingDelete(JsonSchema.Cascade.sourceToTarget);

		type.addPropertyGetter("entries", Iterable.class);

		type.addViewProperty(PropertyView.Ui, "entries");

		// implement method
		type.overrideMethod("getConfiguration", false, "return " + StorageConfiguration.class.getName() + ".getConfiguration(this);");
	}}

	Iterable<StorageConfigurationEntry> getEntries();

	Map<String, String> getConfiguration();

	static Map<String, String> getConfiguration(final StorageConfiguration config) {

		final Map<String, String> data = new LinkedHashMap<>();

		for (final StorageConfigurationEntry entry : config.getEntries()) {

			data.put(entry.getName(), entry.getValue());
		}

		return data;
	}

	// ----- default methods -----
	default Class<? extends StorageProvider> getStorageProviderImplementation() {

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
	default StorageConfigurationEntry addEntry(final String key, final String value) throws FrameworkException {

		return StructrApp.getInstance().create(StorageConfigurationEntry.class,
			new NodeAttribute<>(StructrApp.key(StorageConfigurationEntry.class, "configuration"), this),
			new NodeAttribute<>(StructrApp.key(StorageConfigurationEntry.class, "name"),          key),
			new NodeAttribute<>(StructrApp.key(StorageConfigurationEntry.class, "value"),         value)
		);
	}
}