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

import org.structr.api.schema.JsonSchema;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.SchemaPropertyTraitDefinition;
import org.structr.schema.SchemaHelper.Type;

import java.util.Map;

/**
 *
 *
 */
public class StructrCustomProperty extends StructrDynamicProperty {

	protected String fqcn = null;

	public StructrCustomProperty(final StructrTypeDefinition parent, final String name) {

		super(parent, name);
	}

	@Override
	public String getType() {
		return "custom";
	}

	public void setFqcn(final String value) {
		this.fqcn = value;
	}

	public String getFqcn() {
		return fqcn;
	}

	@Override
	Map<String, Object> serialize() {

		final Map<String, Object> map = super.serialize();

		map.put(JsonSchema.KEY_FQCN, fqcn);

		return map;
	}

	@Override
	void deserialize(final Map<String, Object> source) {

		super.deserialize(source);

		final Object fqcnValue = source.get(JsonSchema.KEY_FQCN);
		if (fqcnValue != null) {

			if (fqcnValue instanceof String) {

				this.fqcn = (String)fqcnValue;

			} else {

				throw new IllegalStateException("Invalid fqcn for property " + name + ", expected string.");
			}

		} else {

			throw new IllegalStateException("Missing fqcn value for property " + name);
		}
	}

	@Override
	void deserialize(final Map<String, SchemaNode> schemaNodes, final SchemaProperty schemaProperty) {

		super.deserialize(schemaNodes, schemaProperty);

		setFormat(schemaProperty.getFormat());
		setFqcn(schemaProperty.getFqcn());
	}

	@Override
	SchemaProperty createDatabaseSchema(final App app, final AbstractSchemaNode schemaNode) throws FrameworkException {

		final SchemaProperty property = super.createDatabaseSchema(app, schemaNode);
		final Traits traits           = Traits.of(StructrTraits.SCHEMA_PROPERTY);
		final PropertyMap properties  = new PropertyMap();

		properties.put(traits.key(SchemaPropertyTraitDefinition.FQCN_PROPERTY), fqcn);

		property.setProperties(SecurityContext.getSuperUserInstance(), properties);

		return property;
	}

	// ----- protected methods -----
	@Override
	protected Type getTypeToSerialize() {
		return Type.Custom;
	}
}
