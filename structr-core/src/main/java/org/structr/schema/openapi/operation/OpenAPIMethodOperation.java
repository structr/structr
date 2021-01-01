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

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.structr.schema.export.StructrMethodDefinition;
import org.structr.schema.openapi.parameter.OpenAPIPathParameter;
import org.structr.schema.openapi.common.OpenAPIReference;

public class OpenAPIMethodOperation extends OpenAPIOperation {

	public OpenAPIMethodOperation(final StructrMethodDefinition method) {

		super(
			// summary
			StringUtils.isBlank(method.getSummary()) ? "Executes " + method.getName() + "() on the entity with the given UUID." : method.getSummary(),

			// description
			StringUtils.isBlank(method.getDescription()) ? "Executes " + method.getName() + "() on the entity with the given UUID." : method.getDescription(),

			// operationId
			"execute" + method.getParent().getName() + "." + method.getName(),

			// tags
			Set.of(method.getParent().getName()),

			// parameters
			List.of(
				new OpenAPIPathParameter("uuid", "The UUID of the target object", Map.of("type", "string"))
			),

			// request body
			method.getOpenAPIRequestBody(),

			// responses
			Map.of(
				"200", method.getOpenAPISuccessResponse(),
				"403", new OpenAPIReference("#/components/responses/forbidden"),
				"422", new OpenAPIReference("#/components/responses/validationError")
			)
		);
	}
}
