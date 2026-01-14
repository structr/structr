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
package org.structr.schema.openapi.operation;

import org.structr.schema.export.StructrTypeDefinition;
import org.structr.schema.openapi.common.OpenAPIAnyOf;
import org.structr.schema.openapi.common.OpenAPIResponseReference;
import org.structr.schema.openapi.common.OpenAPISchemaReference;
import org.structr.schema.openapi.parameter.OpenAPIPathParameter;
import org.structr.schema.openapi.request.OpenAPIRequestResponse;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class OpenAPIPutSingleOperation extends OpenAPIOperation {

	public OpenAPIPutSingleOperation(final StructrTypeDefinition type, final Set<String> viewNames) {

		super(// summary
			"Updates an existing object of type " + type.getName(),

			// description
			"Updates an existing object of type " + type.getName(),

			// operationId
			"update" + type.getName(),

			// tags
			type.getTagsForOpenAPI(),

			// parameters
			List.of(
				new OpenAPIPathParameter("uuid", "The UUID of the existing object", Map.of("type", "string"), true)
			),

			// request body
			new OpenAPIRequestResponse("Properties to update.",
				new OpenAPIAnyOf(
						viewNames.stream().map( viewName -> new OpenAPISchemaReference(type, viewName)).collect(Collectors.toList())
				)
			),

			// responses
			Map.of(
				"200", new OpenAPIResponseReference("#/components/responses/ok"),
				"401", new OpenAPIResponseReference("#/components/responses/unauthorized"),
				"403", new OpenAPIResponseReference("#/components/responses/forbidden"),
				"404", new OpenAPIResponseReference("#/components/responses/notFound"),
				"422", new OpenAPIResponseReference("#/components/responses/validationError")
			)
		);
	}
}
