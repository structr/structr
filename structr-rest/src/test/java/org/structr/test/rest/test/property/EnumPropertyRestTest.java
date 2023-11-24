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
import io.restassured.response.Response;
import org.structr.test.rest.common.StructrRestTestBase;
import org.structr.test.rest.common.TestEnum;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.testng.AssertJUnit.assertEquals;

/**
 *
 *
 */
public class EnumPropertyRestTest extends StructrRestTestBase {

	@Test
	public void testBasics() {

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { 'enumProperty' : 'Status2' } ")
		.expect()
			.statusCode(201)
		.when()
			.post("/TestThree")
			.getHeader("Location");

		Response response = RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
		.when()
			.get("/TestThree");

		assertEquals(TestEnum.Status2.name(), response.getBody().jsonPath().get("result[0].enumProperty"));

	}

	@Test
	public void testSearchOnNode() {

		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'enumProperty' : 'Status1' } ").expect().statusCode(201).when().post("/TestThree");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'enumProperty' : 'Status2' } ").expect().statusCode(201).when().post("/TestThree");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'enumProperty' : 'Status3' } ").expect().statusCode(201).when().post("/TestThree");

		// test for three elements
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(3))
		.when()
			.get("/TestThree");

		// test strict search
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result[0].enumProperty", equalTo(TestEnum.Status2.name()))
		.when()
			.get("/TestThree?enumProperty=Status2");

	}

	@Test
	public void testSearchOnRelationship() {

		final String test01   = createEntity("/TestOne", "{ name: test01 }");
		final String test02   = createEntity("/TestTwo", "{ name: test02, test_ones: [", test01, "] }");
		final String resource = "/TwoOneOneToMany";

		String id = RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(1))
		.when()
			.get(resource)
			.body().path("result[0].id");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body("{ enumProperty: Status2 }")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
		.when()
			.put(concat(resource, "/", id));

		// test strict search
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(1))
			.body("result[0].enumProperty", equalTo(TestEnum.Status2.name()))
		.when()
			.get(concat(resource, "?enumProperty=Status2"));

	}

	@Test
	public void testValidation() {

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { 'enumProperty' : 'Unknown' } ")
		.expect()
			.statusCode(422)
			.body("code",    equalTo(422))
			.body("message", equalTo("Cannot parse input for property enumProperty"))
			.body("errors[0].type",     equalTo("TestThree"))
			.body("errors[0].property", equalTo("enumProperty"))
			.body("errors[0].token",    equalTo("must_be_one_of"))
			.body("errors[0].detail",   equalTo("Status1, Status2, Status3, Status4"))
		.when()
			.post("/TestThree")
			.getHeader("Location");
	}
}
