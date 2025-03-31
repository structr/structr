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
package org.structr.schema.openapi.schema;

import org.structr.common.PropertyView;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.schema.export.StructrTypeDefinition;
import org.structr.schema.openapi.common.OpenAPISchemaReference;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class OpenAPIStructrTypeSchemaOutput extends TreeMap<String, Object> {

	public OpenAPIStructrTypeSchemaOutput(final String description, final String type, final Map properties) {

		put("description", description);
		put("type",       type);

		if (type == "object") {
			put("properties",  properties);
		} else {
			put("items",  properties);
		}
	}

	public OpenAPIStructrTypeSchemaOutput(final StructrTypeDefinition<?> type, final String viewName, final int level) {

		if (level > 3) {
			return;
		}

		final Map<String, Object> properties = new LinkedHashMap<>();
		final String typeName                = type.getName();

		put("type",        "object");
		put("description", "schema for type " + typeName + " with view " + viewName);
		put("properties",  properties);

		type.visitProperties(key -> {

			properties.put(key.jsonName(), key.describeOpenAPIOutputType(typeName, viewName, level));

		}, viewName);


	}

	public OpenAPIStructrTypeSchemaOutput(final String type, final String viewName, final int level)  {

		final Set<String> builtInViews = Set.of( "ui", "public", "all", "custom" );

		// if viewName is a builtin view, we only render public view of connected nodes
		if (level > 0 && builtInViews.contains(viewName)) {

			this.putAll(new OpenAPISchemaReference(type, PropertyView.Public));
			return;

		} else if (level > 0) {

			this.putAll(new OpenAPISchemaReference(type, viewName));
			return;
		}

		if (level > 3) {
			return;
		}

		final Map<String, Object> properties = new LinkedHashMap<>();
		final String typeName                = type;

		put("type",        "object");
		put("description", typeName);
		put("properties",  properties);

		final Traits traits                = Traits.of(type);
		final Set<PropertyKey> keys        = traits.getPropertyKeysForView(viewName);

		if (keys != null && !keys.isEmpty()) {

			for (PropertyKey key : keys) {

				properties.put(key.jsonName(), key.describeOpenAPIOutputType(typeName, viewName, level));
			}

		} else {

			final PropertyKey idKey   = Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.ID_PROPERTY);
			final PropertyKey typeKey = Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.TYPE_PROPERTY);
			final PropertyKey nameKey = Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);

			// default properties
			properties.put("id",   idKey.describeOpenAPIOutputType(typeName, viewName, level));
			properties.put("type", typeKey.describeOpenAPIOutputType(typeName, viewName, level));
			properties.put("name", nameKey.describeOpenAPIOutputType(typeName, viewName, level));
		}
	}
}
