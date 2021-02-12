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

import org.structr.schema.openapi.common.OpenAPIOneOf;
import org.structr.schema.openapi.common.OpenAPIReference;
import org.structr.schema.openapi.request.OpenAPIRequestResponse;
import org.structr.schema.openapi.schema.OpenAPIObjectSchema;
import org.structr.schema.openapi.schema.OpenAPIPrimitiveSchema;
import org.structr.schema.openapi.schema.OpenAPIResultSchema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class OpenAPITokenOperation extends LinkedHashMap<String, Object> {
    public OpenAPITokenOperation() {

        final Map<String, Object> operations = new LinkedHashMap<>();

        put("/token", operations);

        operations.put("post", new OpenAPIOperation(

                // summary
                "Token",

                // description
                "Creates a new Bearer token and refresh token for username and password credentials. Instead of username and password the resource also accepts a previously created refresh token as credentials.",

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
                                new OpenAPIObjectSchema(
                                        new OpenAPIPrimitiveSchema("Username of user to log in.", "name",     "string"),
                                        new OpenAPIPrimitiveSchema("Password of the user.",       "password", "string")
                                ),
                                new OpenAPIObjectSchema(
                                        new OpenAPIPrimitiveSchema("A refresh token from a previous call to the resource.", "refresh_token",    "string")
                                )
                        ),
                        new OpenAPIOneOf(
                            Map.of(
                                    "name",     "admin",
                                    "password", "admin"
                            ),
                            Map.of(
                                    "refresh_token",     "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsImV4cCI6MTYxMzIxOTM4MywiaXNzIjoic3RydWN0ciIsInRva2VuSWQiOiJkNDdiNjE2ZWNkYTM0MTI2OWM0MDNhNzY2MzIwOWQzOF8xNjEzMjE5MzgzMDUxIiwidG9rZW5UeXBlIjoicmVmcmVzaF90b2tlbiJ9.Qu_y9aqTBQqgj1nqH-Syjoka3zc3iRHzO96GLOndP0mGZbZmvjY2PmBpirOWtAVOIHlRKnNF_icV0voDMZFBhg"
                            )
                        )
                ),

                // responses
                Map.of(
                        "200", new OpenAPIRequestResponse(
                            "Success response",
                           new OpenAPIResultSchema(new OpenAPIReference("#/components/schemas/TokenResponse"), false),
                           null,
                           null
                        ),
                        "401", new OpenAPIReference("#/components/responses/tokenError"),
                        "403", new OpenAPIReference("#/components/responses/forbidden")
                )
        ));

    }
}
