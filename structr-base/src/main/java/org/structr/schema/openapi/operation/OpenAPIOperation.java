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
package org.structr.schema.openapi.operation;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class OpenAPIOperation extends TreeMap<String, Object> {

	public OpenAPIOperation(final String summary, final String description, final String operationId, final Set<String> tags, final List<Map<String, Object>> parameters, final Map<String, Object> requestBody, final Map<String, Object> responses) {

		put("summary",     summary);
		put("description", description);
		put("operationId", operationId);

		if (tags != null && !tags.isEmpty()) {
			put("tags", tags);
		}

		if (requestBody != null && !requestBody.isEmpty()) {
			put("requestBody",   requestBody);
		}

		if (responses != null && !responses.isEmpty()) {
			put("responses",   responses);
		}

		if (parameters != null && !parameters.isEmpty()) {
			put("parameters", parameters);
		}
	}
}
