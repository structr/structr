/*
 * Copyright (C) 2010-2023 Structr GmbH
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
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.PropertyKey;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.export.StructrTypeDefinition;
import org.structr.schema.openapi.common.OpenAPISchemaReference;

import java.util.*;

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

	public OpenAPIStructrTypeSchemaOutput(final Class type, final String viewName, final int level)  {
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
		final String typeName                = type.getSimpleName();

		put("type",        "object");
		put("description", typeName);
		put("properties",  properties);

		final ConfigurationProvider config = StructrApp.getConfiguration();
		final Set<PropertyKey> keys        = config.getPropertySet(type, viewName);

		if (keys != null && !keys.isEmpty()) {

			for (PropertyKey key : keys) {

				properties.put(key.jsonName(), key.describeOpenAPIOutputType(typeName, viewName, level));
			}

		} else {

			// default properties
			List.of(AbstractNode.id, AbstractNode.type, AbstractNode.name).stream().forEach(key -> {

				properties.put(key.jsonName(), key.describeOpenAPIOutputType(typeName, viewName, level));
			});
		}
	}
}
