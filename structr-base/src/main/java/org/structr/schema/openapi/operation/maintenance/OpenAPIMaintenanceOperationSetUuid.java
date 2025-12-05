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

public class OpenAPIMaintenanceOperationSetUuid extends LinkedHashMap<String, Object> {

    public OpenAPIMaintenanceOperationSetUuid() {

        final Map<String, Object> operations = new LinkedHashMap<>();

        put("/maintenance/setUuid", operations);

        operations.put("post", new OpenAPIOperation(

			// summary
			"Initializes Structr UUIDs in non-Structr nodes",

			// description
			"This command can be used to initialize non-Structr nodes with a UUID so they can be used with Structr. Please note" +
			" that Structr will store the UUIDs in the `id` property of the node, and only do so if the property is empty before.",

			// operation ID
			"setUuid",

			// tags
			Set.of("Maintenance commands (admin only)"),

			// parameters
			null,

			// request body
			new OpenAPIRequestResponse(
					"Request body",
					new OpenAPIObjectSchema(
							new OpenAPIPrimitiveSchema("Limit the index rebuild to a certain node type",         "type",     "string"),
                        	new OpenAPIPrimitiveSchema("Limit the index rebuild to a certain relationship type", "relType",  "string"),
                        	new OpenAPIPrimitiveSchema("Process all nodes",                                      "allNodes", "boolean"),
                        	new OpenAPIPrimitiveSchema("Process all relationships",                              "allRels",  "boolean")
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
