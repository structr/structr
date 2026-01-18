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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class OpenAPIMaintenanceOperationFlushCaches extends LinkedHashMap<String, Object> {

    public OpenAPIMaintenanceOperationFlushCaches() {

        final Map<String, Object> operations = new LinkedHashMap<>();

        put("/maintenance/flushCaches", operations);

        operations.put("post", new OpenAPIOperation(

                // summary
                "Clears all internal caches",

                // description
		"This command can be used to reduce the amount of memory consumed by Structr, or to fix possible cache invalidation errors.",

                // operation ID
                "flushCaches",

                // tags
                Set.of("Maintenance commands (admin only)"),

                // parameters
                null,

                // request body
		null,

                // responses
                Map.of(
                        "200", new OpenAPISchemaReference("#/components/responses/ok"),
                        "401", new OpenAPISchemaReference("#/components/responses/unauthorized")
                )
        ));

    }
}
