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
import java.util.Map;
import java.util.Set;

public class OpenAPIResetPasswordOperation extends LinkedHashMap<String, Object> {

	public OpenAPIResetPasswordOperation() {

		final Map<String, Object> operations = new LinkedHashMap<>();

		put("/reset-password", operations);

		operations.put("post", new OpenAPIOperation(

			// summary
			"Start reset password process for a given user",

			// description
			"[...]",

			// operation ID
			"resetPassword",

			// tags
			Set.of("Session handling and user management"),

			// parameters
			null,

			// request body
			null,

			// responses
			null
		));

	}
}
