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
package org.structr.schema.openapi.operation.maintenance;

import org.structr.schema.openapi.common.OpenAPISchemaReference;
import org.structr.schema.openapi.operation.OpenAPIOperation;
import org.structr.schema.openapi.request.OpenAPIRequestResponse;
import org.structr.schema.openapi.schema.OpenAPIObjectSchema;
import org.structr.schema.openapi.schema.OpenAPIPrimitiveSchema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class OpenAPIMaintenanceOperationRebuildIndex extends LinkedHashMap<String, Object> {

    public OpenAPIMaintenanceOperationRebuildIndex() {

        final Map<String, Object> operations = new LinkedHashMap<>();

        put("/maintenance/rebuildIndex", operations);

        operations.put("post", new OpenAPIOperation(

			// summary
			"Rebuilds the internal indexes",

			// description
			"Removes all objects from the index and adds them again with all properties that have the indexed flag set." +
			" This command can be used to update the indexes when adding new indexed properties to existing nodes.",

			// operation ID
			"rebuildIndex",

			// tags
			Set.of("Maintenance commands (admin only)"),

			// parameters
			null,

			// request body
			new OpenAPIRequestResponse(
					"Request body",
				new OpenAPIObjectSchema(
						new OpenAPIPrimitiveSchema("Limit the index rebuild to a certain node type",              "type",    "string"),
						new OpenAPIPrimitiveSchema("Limit the index rebuild to a certain relationship type",      "relType", "string"),
						new OpenAPIPrimitiveSchema("Rebuild the index only for nodes, or only for relationships", "mode",    "string", null, "nodesOnly", Map.of(0, "nodesOnly", 1, "relsOnly"), false)
				)
			),

			// responses
			Map.of(
					"200", new OpenAPISchemaReference("#/components/responses/ok"),
					"401", new OpenAPISchemaReference("#/components/responses/unauthorized")
			)
        ));
    }
}
