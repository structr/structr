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

import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.schema.openapi.common.OpenAPIResponseReference;
import org.structr.schema.openapi.common.OpenAPISchemaReference;
import org.structr.schema.openapi.request.OpenAPIRequestResponse;
import org.structr.schema.openapi.result.OpenAPIExampleAnyResult;
import org.structr.schema.openapi.schema.OpenAPIArraySchema;
import org.structr.schema.openapi.schema.OpenAPIObjectSchema;
import org.structr.schema.openapi.schema.OpenAPIPrimitiveSchema;
import org.structr.schema.openapi.schema.OpenAPIResultSchema;

import java.util.*;

public class OpenAPIRegistrationOperation extends LinkedHashMap<String, Object> {

	public OpenAPIRegistrationOperation() {

		final Map<String, Object> operations = new LinkedHashMap<>();
		final Map<String, Object> post       = new OpenAPIOperation(

			// summary
			"Start user self-registration process",

			// description
			"Sending a request to this endpoint with an e-mail address (and other properties) starts the user self-registration process."
				+ " Structr then sends a registration e-mail with a confirmation link to the given address, and when the new user"
				+ " clicks the on that link, the user object is confirmed and can log into the system afterwards.",

			// operation ID
			"register",

			// tags
			Set.of("Session handling and user management"),

			// parameters
			null,

			// request body
			new OpenAPIRequestResponse(
				"Request body",
				new OpenAPIObjectSchema(
					new OpenAPIPrimitiveSchema("E-mail address of new user", PrincipalTraitDefinition.EMAIL_PROPERTY, "string")
				),
				Map.of(PrincipalTraitDefinition.EMAIL_PROPERTY, "new-user@example.com")
			),

			// responses
			Map.of(
				"200", new OpenAPIResponseReference("#/components/responses/ok"),
				"201", new OpenAPIRequestResponse(
					"The new user was created successfully.",
					new OpenAPIResultSchema(new OpenAPIArraySchema("The UUID(s) of the created object(s).", Map.of("type", "string", "example", NodeServiceCommand.getNextUuid())), false),
					new OpenAPIExampleAnyResult(Arrays.asList(NodeServiceCommand.getNextUuid()), true)
				),
				"401", new OpenAPIResponseReference("#/components/responses/forbidden"),
				"422", new OpenAPIRequestResponse(
					"No e-mail address given.",
					new OpenAPISchemaReference("#/components/schemas/RESTResponse"),
					Map.of("code", "422", "message", "No e-mail address given.", "errors", List.of())
				),
				"503", new OpenAPIRequestResponse(
					"User self-registration is not configured correctly.\n\nYou need to enable and configure user self-registration according to https://docs.structr.com/docs/handling-user-sessions#user-self-registration.",
					new OpenAPISchemaReference("#/components/schemas/RESTResponse"),
					Map.of("code", "503", "message", "User self-registration is not configured correctly.", "errors", List.of())
				)
			)
		);

		// override global security object to indicate that this request does not need authentication
		post.put("security", Set.of());

		operations.put("post", post);
		put("/registration", operations);
	}
}
