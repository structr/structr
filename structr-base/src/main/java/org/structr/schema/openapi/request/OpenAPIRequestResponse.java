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
package org.structr.schema.openapi.request;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class OpenAPIRequestResponse extends TreeMap<String, Object> {

	public OpenAPIRequestResponse(final String description, final Map<String, Object> schema) {
		this(description, schema, null);
	}

	public OpenAPIRequestResponse(final String description, final Map<String, Object> schema, final Map<String, Object> example) {
		this(description, schema, example, null, false);
	}

	public OpenAPIRequestResponse(final String description, final Map<String, Object> schema, final Map<String, Object> example, final Map<String, Object> headers, final boolean addItemsToSchema, final String type) {
		final Map<String, Object> content = new LinkedHashMap<>();

		put("description", description);
		put("content",     Map.of("application/json", content));

		if (headers != null) {
			put("headers", headers);
		}

		if (schema != null) {
			Map<String, Object> mapOrSchema = schema;
			if (addItemsToSchema) {
				Map<String, Object> map = new HashMap<>();
				map.put("items", schema);
				mapOrSchema = map;
			}

			if (type != null) {
				mapOrSchema.put("type", type);
			}

			content.put("schema", mapOrSchema);
		}

		if (example != null) {
			content.put("example", example);
		}



	}

	public OpenAPIRequestResponse(final String description, final Map<String, Object> schema, final Map<String, Object> example, final Map<String, Object> headers, final boolean addItemsToSchema) {
		this(description, schema, example, headers, addItemsToSchema, null);
	}
}
