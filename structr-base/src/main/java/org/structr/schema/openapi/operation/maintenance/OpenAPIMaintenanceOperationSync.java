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
package org.structr.schema.openapi.operation.maintenance;

import org.structr.schema.openapi.common.OpenAPISchemaReference;
import org.structr.schema.openapi.operation.OpenAPIOperation;
import org.structr.schema.openapi.request.OpenAPIRequestResponse;
import org.structr.schema.openapi.schema.OpenAPIObjectSchema;
import org.structr.schema.openapi.schema.OpenAPIPrimitiveSchema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class OpenAPIMaintenanceOperationSync extends LinkedHashMap<String, Object> {

    public OpenAPIMaintenanceOperationSync() {

        final Map<String, Object> operations = new LinkedHashMap<>();

        put("/maintenance/sync", operations);

        operations.put("post", new OpenAPIOperation(

                // summary
                "Imports or exports database contents in a binary format",

                // description
		"This command imports or exports data from the database from / to a ZIP file. When exporting, you can select the export data using a Cypher query.",

                // operation ID
                "sync",

                // tags
                Set.of("Maintenance commands (admin only)"),

                // parameters
                null,

                // request body
                new OpenAPIRequestResponse(
                        "Request body",
			new OpenAPIObjectSchema(
                        	new OpenAPIPrimitiveSchema("Operation mode",                             "mode",      "string", null, "export", Map.of(0, "import", 1, "export"), false),
                        	new OpenAPIPrimitiveSchema("Name of the input / output ZIP file",        "file",      "string", null, "data.zip", false),
                        	new OpenAPIPrimitiveSchema("Cypher query that selects the export set",   "query",     "string", null, "MATCH (n) RETURN n", false),
                        	new OpenAPIPrimitiveSchema("Enables or disables import validation",      "validate",  "boolean"),
                        	new OpenAPIPrimitiveSchema("Specifies the batch size for large imports", "batchSize", "number", 200)
                        )
                ),

                // responses
                Map.of(
                        "200", new OpenAPISchemaReference("#/components/responses/ok"),
                        "400", new OpenAPISchemaReference("#/components/responses/badRequest"),
                        "401", new OpenAPISchemaReference("#/components/responses/unauthorized")
                )
        ));

    }
}

/*
chrisi@star2:~/structr-ui$ post /maintenance/sync '{ validate: true }'
{
  "code": 400,
  "message": "Please specify sync file.",
  "errors": []
}

*/