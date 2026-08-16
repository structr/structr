/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import io.restassured.response.Response;
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

public class DoublePropertyRestTest extends StructrRestTestBase {

	@Test
	public void testBasics() {

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { 'doubleProperty' : 3.141592653589793238 } ")
		.expect()
			.statusCode(201)
		.when()
			.post("/TestThree")
			.getHeader("Location");

		Response response = RestAssured.given()
			.contentType("application/json; charset=UTF-8")
		.expect()
			.statusCode(200)
		.when()
			.get("/TestThree");

		// The full double precision is asserted on the response body itself: RestAssured parses JSON
		// numbers through Groovy, which narrows them to float, so the value read via jsonPath() is not
		// the value the server sent.

		assertTrue("Response must contain the full double precision, was: " + response.getBody().asString(),
			response.getBody().asString().contains("3.141592653589793"));

		// the same value as seen through RestAssured, i.e. narrowed to float
		assertEquals(3.1415927, response.getBody().jsonPath().getDouble("result[0].doubleProperty"), 0.0);

	}

	@Test
	public void testSearch() {

		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'doubleProperty' : 1.2 } ").expect().statusCode(201).when().post("/TestThree");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'doubleProperty' : 2.3 } ").expect().statusCode(201).when().post("/TestThree");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'doubleProperty' : 3.4 } ").expect().statusCode(201).when().post("/TestThree");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'name'        : 'test' } ").expect().statusCode(201).when().post("/TestThree");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'doubleProperty' : NaN } ").expect().statusCode(201).when().post("/TestThree");

		// test for five elements
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(5))
		.when()
			.get("/TestThree");

		// test strict search
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
		.expect()
			.statusCode(200)
			.body("result[0].doubleProperty", equalTo(2.3f))
		.when()
			.get("/TestThree?doubleProperty=2.3");

		// test range query
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(2))
		.when()
			.get("/TestThree?doubleProperty=[1.1 TO 2.4]");

		// test empty value
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(1))
			.body("result[0].name", equalTo("test"))
		.when()
			.get("/TestThree?doubleProperty=");

		/* temporarily disabled because Cypher cannot handle NaN yet..
		// test NaN value
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(1))
		.when()
			.get("/TestThree?doubleProperty=NaN");
		*/
	}

	@Test
	public void testConverters() {

		// test double property on regular node
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'doubleProperty' : asdf } ").expect().statusCode(422).when().post("/TestThree");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'doubleProperty' : 'asdf' } ").expect().statusCode(422).when().post("/TestThree");

		// test double property on dynamic node
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'name': 'Test', schemaProperties: [ { name: doubleProperty, propertyType: 'Double' } ] } ").expect().statusCode(201).when().post("/SchemaNode");

		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'doubleProperty' : asdf } ").expect().statusCode(422).when().post("/Test");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'doubleProperty' : 'asdf' } ").expect().statusCode(422).when().post("/Test");

		// test NaN value on regular nod/TestThree		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'doubleProperty' : NaN } ").expect().statusCode(201).when().post("/test_threes");

	}
}
