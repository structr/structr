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
package org.structr.schema.openapi.result;

import java.util.LinkedHashMap;

public class OpenAPIExampleAnyResult extends LinkedHashMap<String, Object> {

	public OpenAPIExampleAnyResult(final Object result, final boolean includeQueryTime) {

		put("result",             result);

		if (includeQueryTime) {
			put("query_time",         "0.001659655");
		}

		put("result_count",       1);
		put("page_count",         1);
		put("result_count_time",  "0.000195496");
		put("serialization_time", "0.001270261");
	}
}
