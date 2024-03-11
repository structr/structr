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

import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.SchemaService;

import java.net.URI;
import org.structr.common.error.FrameworkException;

/**
 * Storage object for mount configuration data.
 */

public interface StorageConfigurationEntry extends NodeInterface {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("StorageConfigurationEntry");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/StorageConfigurationEntry"));
		type.setExtends(URI.create("#/definitions/AbstractNode"));
		type.setCategory("core");

		type.addStringProperty("name", PropertyView.Ui);
		type.addEncryptedProperty("value", PropertyView.Ui);

		type.addPropertyGetter("configuration", StorageConfiguration.class);
		type.addViewProperty(PropertyView.Ui, "configuration");

		type.addPropertySetter("name", String.class);

		type.addPropertyGetter("value", String.class);
		type.addPropertySetter("value", String.class);

	}}

	StorageConfiguration getConfiguration();

	void setName(final String name) throws FrameworkException;

	String getValue();
	void setValue(final String value) throws FrameworkException;

}