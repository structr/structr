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
import java.util.Map;
import java.util.Set;

public class OpenAPIMaintenanceOperationDeployData extends LinkedHashMap<String, Object> {

    public OpenAPIMaintenanceOperationDeployData() {

        final Map<String, Object> operations = new LinkedHashMap<>();

        put("/maintenance/deployData", operations);

        operations.put("post", new OpenAPIOperation(

				// summary
				"Exports or imports the user data",

				// description
				"This command reads or writes a text-based export of the application data (**not the application**).",

                // operation ID
                "deployData",

                // tags
                Set.of("Maintenance commands (admin only)"),

                // parameters
                null,

                // request body
                new OpenAPIRequestResponse(
						"Request body",
						new OpenAPIObjectSchema(
								new OpenAPIPrimitiveSchema("Deployment mode",                              "mode",   "string", null, "export", Map.of(0, "import", 1, "export"), false),
								new OpenAPIPrimitiveSchema("Source folder when **importing**",             "source", "string"),
								new OpenAPIPrimitiveSchema("Target folder when **exporting**",             "target", "string", null, "/home/user/structr-app/webapp/data", false),
								new OpenAPIPrimitiveSchema("Comma-separated list of data types to export", "types",  "string", null, "Project, Task, Customer", false)
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
