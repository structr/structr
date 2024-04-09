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

import org.apache.commons.lang3.StringUtils;
import org.structr.schema.export.StructrMethodDefinition;
import org.structr.schema.openapi.common.OpenAPISchemaReference;

import java.util.Map;
import java.util.Set;

public class OpenAPIGlobalSchemaMethodOperation extends OpenAPIOperation {

	public OpenAPIGlobalSchemaMethodOperation(final StructrMethodDefinition method) {


		super(
			// summary
			StringUtils.isBlank(method.getSummary()) ? "Executes global schema method " + method.getName() + "()." : method.getSummary(),

			// description
			StringUtils.isBlank(method.getDescription()) ? "Executes global schema method " + method.getName() + "()." : method.getDescription(),

			// operationId
			"executeGlobal." + method.getName(),

			// tags
			Set.of("Global schema methods"),

			// parameters
			null,

			// request body
			method.getOpenAPIRequestBody(),

			// responses
			Map.of(
				"200", method.getOpenAPISuccessResponse(),
				"401", new OpenAPISchemaReference("#/components/responses/unauthorized"),
				"422", new OpenAPISchemaReference("#/components/responses/validationError")
			)
		);
	}
}
