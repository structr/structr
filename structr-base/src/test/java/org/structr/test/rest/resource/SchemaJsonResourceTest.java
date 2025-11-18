/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.test.rest.resource;

import io.restassured.RestAssured;
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsString;

public class SchemaJsonResourceTest extends StructrRestTestBase {

	@Test
	public void testGet() {

		createEntity("/SchemaNode", "{ \"name\": \"TestType0\", \"schemaProperties\": [ { \"name\": \"foo\", \"propertyType\": \"String\" } ] }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")

			.expect()
				.statusCode(200)

				.body("result", containsString("TestType0"))
				.body("result_count", equalTo(1))
			.when()
				.get("/maintenance/_schemaJson");

	}

	@Test
	public void testPost() {

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.body("{ schema: '{ definitions: { TestType: { type: \"object\" }}}' }")

			.expect()
				.statusCode(200)

			.when()
				.post("/maintenance/_schemaJson");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")

			.expect()
				.statusCode(200)

			.when()
				.get("/TestType");
	}
}
