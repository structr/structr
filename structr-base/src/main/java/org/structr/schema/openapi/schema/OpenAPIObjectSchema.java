/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import java.util.LinkedHashMap;
import java.util.Map;

public class OpenAPIObjectSchema extends LinkedHashMap<String, Object> {

	public OpenAPIObjectSchema(final Map<String, Object>... properties) {
		this(null, properties);
	}

	public OpenAPIObjectSchema(final String description, final Map<String, Object>... properties) {

		final Map<String, Object> map = new LinkedHashMap<>();

		for (final Map<String, Object> schema : properties) {

			map.putAll(schema);
		}

		put("type", "object");

		if (description != null) {
			put("description", description);
		}

		put("properties", map);
	}
}
