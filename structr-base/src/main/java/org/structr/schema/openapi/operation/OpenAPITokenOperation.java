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

import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.schema.openapi.common.OpenAPIOneOf;
import org.structr.schema.openapi.common.OpenAPISchemaReference;
import org.structr.schema.openapi.request.OpenAPIRequestResponse;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class OpenAPITokenOperation extends LinkedHashMap<String, Object> {

	public OpenAPITokenOperation() {

		final Map<String, Object> operations = new LinkedHashMap<>();
		final Map<String, Object> post	   = new OpenAPIOperation(

			// summary
			"Token",

			// description
			"Creates a new bearer and refresh token for the given username and password credentials. Instead of username and password the resource also accepts a previously created refresh token as credentials.",

			// operation ID
			"token",

			// tags
			Set.of("Session handling and user management"),

			// parameters
			null,

			// request body
			new OpenAPIRequestResponse(
				"Request body",
				new OpenAPIOneOf(
					new OpenAPISchemaReference("UsernameLoginBody"),
					new OpenAPISchemaReference("EMailLoginBody"),
					new OpenAPISchemaReference("RefreshTokenLoginBody")
				),
				Map.of(
					NodeInterfaceTraitDefinition.NAME_PROPERTY, "admin",
					PrincipalTraitDefinition.PASSWORD_PROPERTY, "admin"
				)
			),

			// responses
			Map.of(
				"200", new OpenAPISchemaReference("#/components/responses/tokenResponse"),
				"401", new OpenAPISchemaReference("#/components/responses/tokenError")
			)
		);

		// override global security object to indicate that this request does not need authentication
		post.put("security", Set.of());

		operations.put("post", post);
		put("/token", operations);

	}
}
