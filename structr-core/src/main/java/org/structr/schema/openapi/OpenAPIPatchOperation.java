/*
 * Copyright (C) 2010-2020 Structr GmbH
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
package org.structr.schema.openapi;

import java.util.Map;
import org.structr.common.PropertyView;
import org.structr.schema.export.StructrTypeDefinition;

public class OpenAPIPatchOperation extends OpenAPIOperation {

	public OpenAPIPatchOperation(final StructrTypeDefinition type) {

		super(
			// summary
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
				type.serializeOpenAPI(PropertyView.Ui, true)
			)),

			// responses
			Map.of(
				"200", new OpenAPIRequestResponse("Ok", null),
				"401", new OpenAPIReference("#/components/responses/forbidden"),
				"404", new OpenAPIReference("#/components/responses/notFound"),
				"422", new OpenAPIReference("#/components/responses/validationError")
			)
		);
	}
}
