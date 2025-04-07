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

import org.structr.api.schema.JsonDateArrayProperty;
import org.structr.api.schema.JsonSchema;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.SchemaPropertyTraitDefinition;
import org.structr.schema.SchemaHelper.Type;

import java.util.Map;
import java.util.TreeMap;

/**
 *
 *
 */
public class StructrDateArrayProperty extends StructrPropertyDefinition implements JsonDateArrayProperty {

	public StructrDateArrayProperty(final StructrTypeDefinition parent, final String name) {
		super(parent, name);
	}

	@Override
	public String getType() {
		return "array";
	}

	@Override
	Map<String, Object> serialize() {

		final Map<String, Object> map   = super.serialize();
		final Map<String, Object> items = new TreeMap<>();

		map.put(JsonSchema.KEY_ITEMS, items);
		items.put(JsonSchema.KEY_TYPE, "date");

		return map;
	}

	@Override
	SchemaProperty createDatabaseSchema(final App app, final AbstractSchemaNode schemaNode) throws FrameworkException {

		final SchemaProperty property = super.createDatabaseSchema(app, schemaNode);
		final Traits traits           = Traits.of(StructrTraits.SCHEMA_PROPERTY);

		property.setProperty(traits.key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), Type.DateArray.name());

		return property;
	}
}
