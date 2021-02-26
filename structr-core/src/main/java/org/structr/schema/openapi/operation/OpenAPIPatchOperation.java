/*
 * Copyright (C) 2010-2021 Structr GmbH
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

import java.util.Map;
import org.structr.common.PropertyView;
import org.structr.schema.export.StructrTypeDefinition;
import org.structr.schema.openapi.common.OpenAPIAllOf;
import org.structr.schema.openapi.schema.OpenAPIBaseSchemaWrite;
import org.structr.schema.openapi.common.OpenAPIReference;
import org.structr.schema.openapi.request.OpenAPIRequestResponse;
import org.structr.schema.openapi.schema.OpenAPIStructrTypeSchemaInput;

public class OpenAPIPatchOperation extends OpenAPIOperation {

	public OpenAPIPatchOperation(final StructrTypeDefinition type) {

		super(// summary
			"Updates multiple existing object of type " + type.getName(),

			// description
			"Updates multiple existing object of type " + type.getName(),

			// operationId
			"update" + type.getName() + "List",

			// tags
			type.getTagsForOpenAPI(),

			// parameters
			null,

			// request body
			new OpenAPIRequestResponse("Contents of new " + type.getName() + " object to add.", new OpenAPIAllOf(
				new OpenAPIBaseSchemaWrite(),
				new OpenAPIStructrTypeSchemaInput(type, PropertyView.Ui, 0)
			)),

			// responses
			Map.of(
				"200", new OpenAPIRequestResponse("Ok", null),
				"403", new OpenAPIReference("#/components/responses/forbidden"),
				"404", new OpenAPIReference("#/components/responses/notFound"),
				"422", new OpenAPIReference("#/components/responses/validationError")
			)
		);
	}
}
