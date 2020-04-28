/**
 * Copyright (C) 2010-2020 Structr GmbH
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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.property.PropertyMap;
import org.structr.schema.SchemaHelper.Type;
import org.structr.api.schema.JsonEnumProperty;
import org.structr.api.schema.JsonSchema;

/**
 *
 *
 */
public class StructrEnumProperty extends StructrStringProperty implements JsonEnumProperty {

	protected Set<String> enums = new TreeSet<>();
	protected String fqcn       = null;

	public StructrEnumProperty(final StructrTypeDefinition parent, final String name) {

		super(parent, name);
	}

	@Override
	public JsonEnumProperty setEnumType(final Class type) {

		this.fqcn = type.getName().replace("$", ".");

		return this;
	}

	@Override
	public JsonEnumProperty setEnums(String... values) {

		for (final String value : values) {
			enums.add(value.trim());
		}

		return this;
	}

	@Override
	public Set<String> getEnums() {
		return enums;
	}

	@Override
	Map<String, Object> serialize() {

		final Map<String, Object> map = super.serialize();

		if (fqcn != null) {

			map.put(JsonSchema.KEY_FQCN, fqcn);

		} else {

			map.put(JsonSchema.KEY_ENUM, enums);
		}

		map.remove(JsonSchema.KEY_FORMAT);

		return map;
	}

	@Override
	void deserialize(final Map<String, Object> source) {

		super.deserialize(source);

		final Object enumValues = source.get(JsonSchema.KEY_ENUM);
		final Object typeValue  = source.get(JsonSchema.KEY_FQCN);

		if (enumValues != null) {

			if (enumValues instanceof List) {

				enums.addAll((List)enumValues);

			} else {

				throw new IllegalStateException("Invalid enum values for property " + name + ", expected array.");
			}

		} else if (typeValue != null && typeValue instanceof String) {

			this.fqcn = typeValue.toString();

		} else {

			throw new IllegalStateException("Missing enum values for property " + name);
		}
	}

	@Override
	void deserialize(final Map<String, SchemaNode> schemaNodes, final SchemaProperty schemaProperty) {

		super.deserialize(schemaNodes, schemaProperty);

		setEnums(schemaProperty.getEnumDefinitions().toArray(new String[0]));
	}

	@Override
	SchemaProperty createDatabaseSchema(final App app, final AbstractSchemaNode schemaNode) throws FrameworkException {

		final SchemaProperty property = super.createDatabaseSchema(app, schemaNode);
		final PropertyMap properties  = new PropertyMap();

		properties.put(SchemaProperty.fqcn, this.fqcn);

		property.setProperties(SecurityContext.getSuperUserInstance(), properties);

		return property;
	}

	@Override
	public String getFormat() {
		return StringUtils.join(getEnums(), ", ");
	}

	// ----- protected methods -----
	@Override
	protected Type getTypeToSerialize() {
		return Type.Enum;
	}
}
