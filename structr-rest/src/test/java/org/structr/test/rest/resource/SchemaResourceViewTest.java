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
package org.structr.test.rest.resource;

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/**
 *
 *
 */
public class SchemaResourceViewTest extends StructrRestTestBase {

	@Test
	public void testCustomSchema0() {

		String id = createEntity("/SchemaNode", "{ \"name\": \"TestType0\", schemaProperties: [ { name: a, propertyType: String }, { name: b, propertyType: String }, { name: c, propertyType: String },  { name: d, propertyType: String } ] }");
		createEntity("/SchemaView", "{ \"name\": \"view1\", \"nonGraphProperties\": \"b,d,c,a\", \"schemaNode\": \"" + id + "\" }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result",	      hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].type", equalTo("String"))
				.body("result[0].jsonName", equalTo("b"))
				.body("result[1].type", equalTo("String"))
				.body("result[1].jsonName", equalTo("d"))
				.body("result[2].type", equalTo("String"))
				.body("result[2].jsonName", equalTo("c"))
				.body("result[3].type", equalTo("String"))
				.body("result[3].jsonName", equalTo("a"))

			.when()
				.get("/_schema/TestType0/view1");

	}
}
