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
package org.structr.schema.openapi.operation;

import org.structr.common.PropertyView;
import org.structr.schema.export.StructrTypeDefinition;
import org.structr.schema.openapi.common.OpenAPIResponseReference;

import java.util.Map;

public class OpenAPIDeleteMultipleOperation extends OpenAPIOperation {

	public OpenAPIDeleteMultipleOperation(final StructrTypeDefinition type) {

		super(
			// summary
			"Deletes all objects of type " + type.getName(),

			// description
			"Deletes all objects of type " + type.getName() + ". *Caution*: this is a potentially dangerous operation because it is irreversible.",

			// operationId
			"delete" + type.getName() + "List",

			// tags
			type.getTagsForOpenAPI(),

			// parameters
			type.getOpenAPIParameters(PropertyView.All, 0, false),

			// request body
			null,

			// responses
			Map.of(
				"200", new OpenAPIResponseReference("#/components/responses/ok"),
				"401", new OpenAPIResponseReference("#/components/responses/unauthorized"),
				"403", new OpenAPIResponseReference("#/components/responses/forbidden"),
				"422", new OpenAPIResponseReference("#/components/responses/validationError")
			)
		);
	}
}
