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

public class OpenAPIMaintenanceOperationDirectFileImport extends LinkedHashMap<String, Object> {

    public OpenAPIMaintenanceOperationDirectFileImport() {

        final Map<String, Object> operations = new LinkedHashMap<>();

        put("/maintenance/directFileImport", operations);

        operations.put("post", new OpenAPIOperation(

                // summary
                "Imports files from a folder in the local filesystem",

                // description
				"The files can either be copied or moved (i.e. deleted after copying into Structr), depending on the `mode` parameter."
				 + " The `existing` parameter determines how Structr handles existing files in the Structr Filesystem. The `index` parameter"
				 + " allows you to enable or disable indexing for the imported files.",

                // operation ID
                "directFileImport",

                // tags
                Set.of("Maintenance commands (admin only)"),

                // parameters
                null,

                // request body
                new OpenAPIRequestResponse(
						"Request body",
						new OpenAPIObjectSchema(
								new OpenAPIPrimitiveSchema("Import mode",                            "mode",     "string", null, "copy", Map.of(0, "copy", 1, "move"), false),
								new OpenAPIPrimitiveSchema("Source folder to import files from",     "source",   "string"),
								new OpenAPIPrimitiveSchema("How to handle existing files",           "existing", "string", null, "skip", Map.of(0, "skip", 1, "overwrite", 2, "rename"), false),
								new OpenAPIPrimitiveSchema("Whether the imported files are indexed", "index",    "boolean", true)
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
