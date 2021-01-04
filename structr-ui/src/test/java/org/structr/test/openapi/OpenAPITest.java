/*
 * Copyright (C) 2010-2021 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.test.openapi;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.RequestLoggingFilter;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

public class OpenAPITest extends StructrUiTest {

	@Test
	public void testOpenAPIEndpoint() {

		RestAssured.basePath = "/";

		// provoke 404 error with GET on non-existing resource
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(RequestLoggingFilter.logRequestTo(System.out))
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.expect()
				.statusCode(200)
			.when()
				.get("/structr/openapi/schema.json");

	}

}
