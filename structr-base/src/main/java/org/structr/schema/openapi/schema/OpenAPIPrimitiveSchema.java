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
import java.util.TreeMap;

public class OpenAPIPrimitiveSchema extends LinkedHashMap<String, Object> {

	private final Map<String, Object> schema = new TreeMap<>();
	private final Map<String, Object> schemaContent = new TreeMap<>();
	private Map selectedMap = schema;

	public OpenAPIPrimitiveSchema(final String description, final String name, final String type) {
		this(description, name, type, null, null, null, false);
	}

	public OpenAPIPrimitiveSchema(final String description, final String name, final String type, final Object defaultValue) {
		this(description, name, type, defaultValue, null, null, false);
	}

	public OpenAPIPrimitiveSchema(final String description, final String name, final String type, final Object defaultValue, final Object exampleValue, final boolean wrapInExtraMap) {
		this(description, name, type, defaultValue, exampleValue, null, wrapInExtraMap);
	}

	public OpenAPIPrimitiveSchema(final String description, final String name, final String type, final Object defaultValue, final Object exampleValue, final Map<Integer, String> enumValues, final boolean wrapInExtraMap) {

		put(name, schema);

		if (wrapInExtraMap) {
			schema.put("schema", schemaContent);
			selectedMap = schemaContent;
		}

		if (type != null) {
			selectedMap.put("type", type);
		}

		if (description != null) {
			selectedMap.put("description", description);
		}

		if (defaultValue != null) {
			selectedMap.put("default", defaultValue);
		}

		if (exampleValue != null) {
			selectedMap.put("example", exampleValue);
		}

		if (enumValues != null) {
			selectedMap.put("enum", enumValues);
		}
	}

	public OpenAPIPrimitiveSchema required() {
		selectedMap.put("required", true);
		return this;
	}

	public OpenAPIPrimitiveSchema add(final String key, final Object value) {
		selectedMap.put(key, value);
		return this;
	}
}
