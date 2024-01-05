/*
 * Copyright (C) 2010-2024 Structr GmbH
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
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OpenAPIMaintenanceOperationSnapshot extends LinkedHashMap<String, Object> {

    public OpenAPIMaintenanceOperationSnapshot() {

        final Map<String, Object> operations = new LinkedHashMap<>();

        put("/maintenance/snapshot", operations);

        operations.put("post", new OpenAPIOperation(

                // summary
                "Creates a JSON schema export",

                // description
		"This command creates a schema snapshot and writes it to a local file on the server the Structr instance is running on."
	      + " It is used internally in the Schema section to manage schema snapshots. Note that the output file will always be read"
	      + " or written from / to relative to the `snapshots` folder.",

                // operation ID
                "snapshot",

                // tags
                Set.of("Maintenance commands (admin only)"),

                // parameters
                null,

                // request body
                new OpenAPIRequestResponse(
                        "Request body",
			    new OpenAPIObjectSchema(
                        	new OpenAPIPrimitiveSchema("Snapshot mode",                                                    "mode",  "string", null, "export", Map.of(0, "export", 1, "restore", 2, "add", 3, "delete", 4, "purge"), false),
                        	new OpenAPIPrimitiveSchema("Name of the input / output file relative to the snapshots folder", "name",  "string", null, "schema-snapshot.json", false),
                        	new OpenAPIPrimitiveSchema("Optional list of types to export",                                 "types", "array",  null, List.of("Project", "Task"), false).add("items", Map.of("type", "string"))
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
