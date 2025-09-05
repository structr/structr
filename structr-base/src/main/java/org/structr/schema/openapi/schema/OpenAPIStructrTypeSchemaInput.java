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
package org.structr.schema.openapi.schema;

import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.schema.export.StructrTypeDefinition;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class OpenAPIStructrTypeSchemaInput extends TreeMap<String, Object> {

	public OpenAPIStructrTypeSchemaInput(final StructrTypeDefinition<?> type, final String viewName, final int level) {

		if (level > 3) {
			return;
		}

		final Map<String, Object> properties = new LinkedHashMap<>();
		final String typeName                = type.getName();

		put("type",        "object");
		put("description", typeName);
		put("properties",  properties);

		type.visitProperties(key -> {

			if (!key.isReadOnly()) {

				properties.put(key.jsonName(), key.describeOpenAPIInputType(typeName, viewName, level));
			}

		}, viewName);
	}

	public OpenAPIStructrTypeSchemaInput(final Traits type, final String viewName, final int level) {

		if (level > 3) {
			return;
		}

		final Map<String, Object> properties = new LinkedHashMap<>();
		final String typeName                = type.getName();

		put("type",        "object");
		put("description", typeName);
		put("properties",  properties);

		final Set<PropertyKey> keys = type.getPropertyKeysForView(viewName);

		if (keys != null && !keys.isEmpty()) {

			for (PropertyKey key : keys) {

				if (!key.isReadOnly()) {

					properties.put(key.jsonName(), key.describeOpenAPIInputType(typeName, viewName, level));
				}
			}
		}
	}
}
