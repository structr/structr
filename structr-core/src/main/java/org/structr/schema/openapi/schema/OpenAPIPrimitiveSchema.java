/*
 * Copyright (C) 2010-2021 Structr GmbH
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

public class OpenAPIPrimitiveSchema extends LinkedHashMap<String, Object> {

	public OpenAPIPrimitiveSchema(final String description, final String name, final String type) {
		this(description, name, type, null);
	}

	public OpenAPIPrimitiveSchema(final String description, final String name, final String type, final Object defaultValue) {
		this(description, name, type, defaultValue, null);
	}

	public OpenAPIPrimitiveSchema(final String description, final String name, final String type, final Object defaultValue, final Object exampleValue) {

		final Map<String, Object> schema = new LinkedHashMap<>();

		put(name, schema);

		schema.put("type", type);

		if (description != null) {
			schema.put("description", description);
		}

		if (defaultValue != null) {
			schema.put("default", defaultValue);
		}

		if (exampleValue != null) {
			schema.put("example", exampleValue);
		}
	}
}
