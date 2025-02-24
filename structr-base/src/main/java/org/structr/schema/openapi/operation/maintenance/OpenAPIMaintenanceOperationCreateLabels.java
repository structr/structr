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

import org.structr.core.traits.StructrTraits;
import org.structr.schema.openapi.common.OpenAPISchemaReference;
import org.structr.schema.openapi.operation.OpenAPIOperation;
import org.structr.schema.openapi.request.OpenAPIRequestResponse;
import org.structr.schema.openapi.schema.OpenAPIObjectSchema;
import org.structr.schema.openapi.schema.OpenAPIPrimitiveSchema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class OpenAPIMaintenanceOperationCreateLabels extends LinkedHashMap<String, Object> {

    public OpenAPIMaintenanceOperationCreateLabels() {

        final Map<String, Object> operations = new LinkedHashMap<>();

        put("/maintenance/createLabels", operations);

        operations.put("post", new OpenAPIOperation(

                // summary
                "Updates the type labels of a node",

                // description
		"This command looks at the value in the type property of an object and tries to identify a corresponding schema type."
	      + " If the schema type exists, it creates a type label on the object for each type in the inheritance hierarchy and"
	      + " removes labels that don’t have a corresponding type.\n\n**Note** this command will only work for objects that have"
	      + " a value in their type property.",

                // operation ID
                "createLabels",

                // tags
                Set.of("Maintenance commands (admin only)"),

                // parameters
                null,

                // request body
                new OpenAPIRequestResponse(
                   "Request body",
			        new OpenAPIObjectSchema(
                    new OpenAPIPrimitiveSchema("Node type to limit creation of labels to", "type", "string", null, StructrTraits.GROUP, false)
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
