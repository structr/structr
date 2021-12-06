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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.structr.schema.openapi.common.OpenAPIOneOf;
import org.structr.schema.openapi.common.OpenAPISchemaReference;
import org.structr.schema.openapi.request.OpenAPIRequestResponse;
import org.structr.schema.openapi.schema.OpenAPIObjectSchema;
import org.structr.schema.openapi.schema.OpenAPIPrimitiveSchema;
import org.structr.schema.openapi.schema.OpenAPIResultSchema;

public class OpenAPILoginOperation extends LinkedHashMap<String, Object> {

	public OpenAPILoginOperation() {


		final Map<String, Object> operations = new LinkedHashMap<>();

		OpenAPIPrimitiveSchema openAPIPrimitiveSchema = new OpenAPIPrimitiveSchema("Sets the JSESSIONID cookie.", "Set-Cookie", "string", null, "JSESSIONID=0d47152b8e7b6c85a07994d2687250f5114rzrrnhat2wn80ump8x8iqp0.0d47152b8e7b6c85a07994d2687250f5;Path=/", true);


		final Map<String, Object> post       = new OpenAPIOperation(

			// summary
			"Login",

			// description
			"Authenticate a session with eMail or username and password.",

			// operation ID
			"login",

			// tags
			Set.of("Session handling and user management"),

			// parameters
			null,

			// request body
			new OpenAPIRequestResponse(
				"Request body",
				new OpenAPIOneOf(
					new OpenAPIObjectSchema(
						new OpenAPIPrimitiveSchema("Username of user to log in.", "name",     "string"),
						new OpenAPIPrimitiveSchema("Password of the user.",       "password", "string")
					),
					new OpenAPIObjectSchema(
						new OpenAPIPrimitiveSchema("eMail of user to log in.", "eMail",    "string"),
						new OpenAPIPrimitiveSchema("Password of the user.",    "password", "string")
					)
				),
				Map.of(
					"name",     "admin",
					"password", "admin"
				)
			),

			// responses
			Map.of(
					"200", new OpenAPIRequestResponse(
							"Login successful.",
							new OpenAPIResultSchema(new OpenAPISchemaReference("#/components/schemas/User"), false),
							null,
							openAPIPrimitiveSchema,
							false
					),
					"401", new OpenAPIRequestResponse(
							"Access denied or wrong password.\n\nIf the error message is \"Access denied\", you need to configure a resource access grant for the `_login` endpoint."
									+ " otherwise the error message is \"Wrong username or password, or user is blocked. Check caps lock. Note: Username is case sensitive!\".",
							new OpenAPISchemaReference("#/components/schemas/RESTResponse"),
							Map.of("code", "401", "message", "Access denied", "errors", List.of())
					)
			)
		);




		// override global security object to indicate that this request does not need authentication
		post.put("security", Set.of());

		operations.put("post", post);
		put("/login", operations);
	}
}
