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

public class OpenAPIMaintenanceOperationChangeNodePropertyKey extends LinkedHashMap<String, Object> {

	public OpenAPIMaintenanceOperationChangeNodePropertyKey() {

		final Map<String, Object> operations = new LinkedHashMap<>();

		put("/maintenance/changeNodePropertyKey", operations);

		operations.put("post", new OpenAPIOperation(

			// summary
			"Migrates property values from one property key to another",

			// description
			"This command can for example be used to move all name values to a description property.",

			// operation ID
			"changeNodePropertyKey",

			// tags
			Set.of("Maintenance commands (admin only)"),

			// parameters
			null,

			// request body
			new OpenAPIRequestResponse(
					"Request body",
                    new OpenAPIObjectSchema(
                            new OpenAPIPrimitiveSchema("Source key to migrate the value from", "oldKey", "string", null, "surname", false),
                            new OpenAPIPrimitiveSchema("target key to migrate the value to",   "newKey", "string", null, "lastName", false)
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
