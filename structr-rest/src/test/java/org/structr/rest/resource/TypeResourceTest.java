/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.rest.resource;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import static org.hamcrest.Matchers.equalTo;
import org.structr.rest.common.StructrRestTest;

/**
 */
public class TypeResourceTest extends StructrRestTest {

	public void test00CreationOfDerivedTypeObjects() {

		// Verify that the creation of derived type objects is possible
		// using the REST resource of the base type. This is important
		// when a user wants to create a more specific type using the
		// base type resource URL.

		createEntity("/SchemaNode", "{ name: BaseType }");
		createEntity("/SchemaNode", "{ name: DerivedType, extendsClass: 'org.structr.dynamic.BaseType' }");

		createEntity("/BaseType", "{ name: BaseType }");
		createEntity("/BaseType", "{ name: DerivedType, type: DerivedType }");

		// Check nodes exist
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
				.statusCode(200)
				.body("result_count",       equalTo(2))
				.body("result[0].type",     equalTo("BaseType"))
				.body("result[1].type",     equalTo("DerivedType"))
			.when()
				.get("/BaseType");
	}
}
