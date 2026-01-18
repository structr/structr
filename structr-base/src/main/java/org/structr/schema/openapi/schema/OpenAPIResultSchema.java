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

import org.structr.schema.openapi.common.OpenAPIResponseReference;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class OpenAPIResultSchema extends TreeMap<String, Object> {

	public OpenAPIResultSchema(final Map<String, Object> result, final boolean includeQueryTime) {

		final Map<String, Object> properties = new LinkedHashMap<>();

		put("type",       "object");
		put("properties", properties);

		properties.put("result", result);

		if (includeQueryTime) {
			properties.put("query_time", Map.of("type", "string", "example", "0.003547842"));
		}

		properties.put("result_count",       Map.of("type", "integer", "example", 1));
		properties.put("page_count",         Map.of("type", "integer", "example", 1));
		properties.put("result_count_time",  Map.of("type", "string", "example", "0.004132365"));
		properties.put("serialization_time", Map.of("type", "string", "example", "0.000642111"));
	}

	public OpenAPIResultSchema(final List result, final boolean includeQueryTime) {

		final Map<String, Object> properties = new LinkedHashMap<>();

		put("type",       "object");
		put("properties", properties);

		properties.put("result", result);

		if (includeQueryTime) {
			properties.put("query_time", Map.of("type", "string", "example", "0.003547842"));
		}

		properties.put("result_count",       Map.of("type", "integer", "example", 1));
		properties.put("page_count",         Map.of("type", "integer", "example", 1));
		properties.put("result_count_time",  Map.of("type", "string", "example", "0.004132365"));
		properties.put("serialization_time", Map.of("type", "string", "example", "0.000642111"));
	}

	public OpenAPIResultSchema(final OpenAPIResponseReference schemaReference, final boolean includeQueryTime) {

		final Map<String, Object> properties = new LinkedHashMap<>();

		put("type",       "object");
		put("properties", properties);

		properties.put("result", schemaReference);

		if (includeQueryTime) {
			properties.put("query_time", Map.of("type", "string", "example", "0.003547842"));
		}

		properties.put("result_count",       Map.of("type", "integer", "example", 1));
		properties.put("page_count",         Map.of("type", "integer", "example", 1));
		properties.put("result_count_time",  Map.of("type", "string", "example", "0.004132365"));
		properties.put("serialization_time", Map.of("type", "string", "example", "0.000642111"));
	}
}
