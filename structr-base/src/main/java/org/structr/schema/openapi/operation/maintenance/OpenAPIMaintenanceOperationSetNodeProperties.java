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

public class OpenAPIMaintenanceOperationSetNodeProperties extends LinkedHashMap<String, Object> {

    public OpenAPIMaintenanceOperationSetNodeProperties() {

        final Map<String, Object> operations = new LinkedHashMap<>();

        put("/maintenance/setNodeProperties", operations);

        operations.put("post", new OpenAPIOperation(

                // summary
                "Sets property values on all nodes of a certain type",

                // description
		"Sets one or more properties on all nodes of a given type.\n\n**Note** that if you use this command to modify the"
	      + " type of a node, you must use the property name `newType` for the value, since `type` is already taken.",

                // operation ID
                "setNodeProperties",

                // tags
                Set.of("Maintenance commands (admin only)"),

                // parameters
                null,

                // request body
                new OpenAPIRequestResponse(
                        "Request body",
			    new OpenAPIObjectSchema(
                        new OpenAPIPrimitiveSchema("Select the node type on which to set the properties", "type", "string"),
                        new OpenAPIPrimitiveSchema("Example key to illustrate the usage",                 "key",  "string", null, "value", false)
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
