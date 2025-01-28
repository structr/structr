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
package org.structr.test.rest.test.property;

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;

/**
 *
 *
 */
public class IntegerPropertyRestTest extends StructrRestTestBase {

	@Test
	public void testBasics() {

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { 'integerProperty' : 2345 } ")
		.expect()
			.statusCode(201)
		.when()
			.post("/TestThree")
			.getHeader("Location");



		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result[0].integerProperty", equalTo(2345))
		.when()
			.get("/TestThree");

	}

	@Test
	public void testSearch() {

		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'integerProperty' : 1 } ").expect().statusCode(201).when().post("/TestThree");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'integerProperty' : 2 } ").expect().statusCode(201).when().post("/TestThree");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'integerProperty' : 3 } ").expect().statusCode(201).when().post("/TestThree");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'name'       : 'test' } ").expect().statusCode(201).when().post("/TestThree");

		// test for three elements
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(4))
		.when()
			.get("/TestThree");

		// test strict search
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result[0].integerProperty", equalTo(2))
		.when()
			.get("/TestThree?integerProperty=2");

		// test empty value
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(1))
			.body("result[0].name", equalTo("test"))
		.when()
			.get("/TestThree?integerProperty=");
	}

	@Test
	public void testRangeSearch() {

		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'integerProperty' : 1 } ").expect().statusCode(201).when().post("/TestThree");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'integerProperty' : 2 } ").expect().statusCode(201).when().post("/TestThree");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'integerProperty' : 3 } ").expect().statusCode(201).when().post("/TestThree");

		// test range query
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(2))
		.when()
			.get("/TestThree?integerProperty=[1 TO 2]");

	}

	@Test
	public void testConverters() {

		// test int property on regular node
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'integerProperty' : asdf } ").expect().statusCode(422).when().post("/TestThree");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'integerProperty' : 'asdf' } ").expect().statusCode(422).when().post("/TestThree");

		// test int property on dynamic node
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'name': 'Test', schemaProperties: [ { name: integerProperty, propertyType: Integer } ] } ").expect().statusCode(201).when().post("/SchemaNode");

		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'integerProperty' : asdf } ").expect().statusCode(422).when().post("/Test");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'integerProperty' : 'asdf' } ").expect().statusCode(422).when().post("/Test");
	}
}
