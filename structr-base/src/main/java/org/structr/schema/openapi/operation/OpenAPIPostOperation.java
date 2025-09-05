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
package org.structr.schema.openapi.operation;

import org.structr.schema.export.StructrTypeDefinition;
import org.structr.schema.openapi.common.OpenAPIAnyOf;
import org.structr.schema.openapi.common.OpenAPIResponseReference;
import org.structr.schema.openapi.common.OpenAPISchemaReference;
import org.structr.schema.openapi.request.OpenAPIRequestResponse;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class OpenAPIPostOperation extends OpenAPIOperation {

	public OpenAPIPostOperation(final StructrTypeDefinition type, final Set<String> viewNames) {

		super(// summary
			"Creates a new object of type " + type.getName(),

			// description
			"Creates a new object of type " + type.getName(),

			// operationId
			"create" + type.getName(),

			// tags
			type.getTagsForOpenAPI(),

			// parameters
			null,

			// request body
			new OpenAPIRequestResponse("Contents of new " + type.getName() + " object to add.",
					new OpenAPIAnyOf(
							viewNames.stream().map( viewName -> new OpenAPISchemaReference(type, viewName)).collect(Collectors.toList())
					)
			),

			// response
			Map.of(
				"201", new OpenAPIResponseReference("#/components/responses/created"),
				"401", new OpenAPIResponseReference("#/components/responses/unauthorized"),
				"422", new OpenAPIResponseReference("#/components/responses/validationError")
			)
		);
	}
}
