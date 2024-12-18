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
package org.structr.schema.export;

import org.structr.api.schema.JsonDynamicProperty;
import org.structr.api.schema.JsonSchema;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;

import java.util.Map;

/**
 *
 *
 */
public abstract class StructrDynamicProperty extends StructrStringProperty implements JsonDynamicProperty {

	protected String typeHint = null;

	public StructrDynamicProperty(final StructrTypeDefinition parent, final String name) {

		super(parent, name);
	}

	@Override
	public JsonDynamicProperty setTypeHint(String typeHint) {

		this.typeHint = typeHint;
		return this;
	}

	@Override
	public String getTypeHint() {
		return typeHint;
	}

	@Override
	Map<String, Object> serialize() {

		final Map<String, Object> map = super.serialize();

		if (typeHint != null) {
			map.put(JsonSchema.KEY_TYPE_HINT, typeHint);
		}

		return map;
	}

	@Override
	void deserialize(final Map<String, Object> source) {

		super.deserialize(source);

		final Object typeHintValue = source.get(JsonSchema.KEY_TYPE_HINT);
		if (typeHintValue != null) {

			if (typeHintValue instanceof String) {

				this.typeHint = (String)typeHintValue;

			} else {

				throw new IllegalStateException("Invalid typeHint for property " + name + ", expected string.");
			}
		}

	}

	@Override
	void deserialize(final Map<String, SchemaNode> schemaNodes, final SchemaProperty property) {

		super.deserialize(schemaNodes, property);

		setTypeHint(property.getTypeHint());
	}

	@Override
	SchemaProperty createDatabaseSchema(final App app, final AbstractSchemaNode schemaNode) throws FrameworkException {

		final SchemaProperty property = super.createDatabaseSchema(app, schemaNode);
		final Traits traits           = Traits.of("SchemaProperty");
		final PropertyMap properties  = new PropertyMap();

		properties.put(traits.key("typeHint"), typeHint);

		property.getWrappedNode().setProperties(SecurityContext.getSuperUserInstance(), properties);

		return property;
	}
}
