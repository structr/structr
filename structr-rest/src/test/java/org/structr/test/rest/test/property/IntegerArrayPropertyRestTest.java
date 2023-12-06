/*
 * Copyright (C) 2010-2023 Structr GmbH
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
public class IntegerArrayPropertyRestTest extends StructrRestTestBase {

	@Test
	public void testIntegerArrayViaRest() {

		String location = RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { 'integerArrayProperty' : [1, 2, 3, 4, 5] } ")
		.expect()
			.statusCode(201)
		.when()
			.post("/test_threes")
			.getHeader("Location");

		String uuid = getUuidFromLocation(location);

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result[0].integerArrayProperty[0]", equalTo(1))
			.body("result[0].integerArrayProperty[1]", equalTo(2))
			.body("result[0].integerArrayProperty[2]", equalTo(3))
			.body("result[0].integerArrayProperty[3]", equalTo(4))
			.body("result[0].integerArrayProperty[4]", equalTo(5))
		.when()
			.get("/test_threes");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { 'integerArrayProperty' : [3, 4, 5] } ")
		.expect()
			.statusCode(200)
		.when()
			.put("/test_threes/" + uuid);

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result[0].integerArrayProperty[0]", equalTo(3))
			.body("result[0].integerArrayProperty[1]", equalTo(4))
			.body("result[0].integerArrayProperty[2]", equalTo(5))
		.when()
			.get("/test_threes");
	}

	@Test
	public void testIntegerArraySearch() {

		// create test objects
		final String id1 = getUuidFromLocation(RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { 'integerArrayProperty' : [1, 2, 3, 4, 5] } ")
		.expect()
			.statusCode(201)
		.when()
			.post("/test_threes")
			.getHeader("Location")
		);

		// create test objects
		final String id2 = getUuidFromLocation(RestAssured.given()
			.contentType("application/json; charset=UTF-8")
		.expect()
			.statusCode(201)
		.when()
			.post("/test_threes")
			.getHeader("Location")
		);

		// test search for empty array property
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result[0].id", equalTo(id2))
		.when()
			.get("/test_threes?integerArrayProperty=");

		// test search for empty array property
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
		.expect()
			.statusCode(200)
			.body("result[0].id", equalTo(id1))
		.when()
			.get("/test_threes?integerArrayProperty=[]");
	}


	@Test
	public void testSearchIntegerArray() {

		// create test objects
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body("["
				+ "{ 'name': 'test1', 'integerArrayProperty' : [1] },"
				+ "{ 'name': 'test2', 'integerArrayProperty' : [1, 2] },"
				+ "{ 'name': 'test3', 'integerArrayProperty' : [1, 2, 3] },"
				+ "{ 'name': 'test4', 'integerArrayProperty' : [1, 2, 3, 4] },"
				+ "{ 'name': 'test5', 'integerArrayProperty' : [1, 2, 3, 4, 5] }"
				+ "]")
		.expect()
			.statusCode(201)
		.when()
			.post("/test_threes?_sort=name")
			.getHeader("Location");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(5))

			.body("result[0].integerArrayProperty[0]", equalTo(1))

			.body("result[1].integerArrayProperty[0]", equalTo(1))
			.body("result[1].integerArrayProperty[1]", equalTo(2))

			.body("result[2].integerArrayProperty[0]", equalTo(1))
			.body("result[2].integerArrayProperty[1]", equalTo(2))
			.body("result[2].integerArrayProperty[2]", equalTo(3))

			.body("result[3].integerArrayProperty[0]", equalTo(1))
			.body("result[3].integerArrayProperty[1]", equalTo(2))
			.body("result[3].integerArrayProperty[2]", equalTo(3))
			.body("result[3].integerArrayProperty[3]", equalTo(4))

			.body("result[4].integerArrayProperty[0]", equalTo(1))
			.body("result[4].integerArrayProperty[1]", equalTo(2))
			.body("result[4].integerArrayProperty[2]", equalTo(3))
			.body("result[4].integerArrayProperty[3]", equalTo(4))
			.body("result[4].integerArrayProperty[4]", equalTo(5))

		.when()
			.get("/test_threes?_sort=name&integerArrayProperty=1");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(4))

			.body("result[0].integerArrayProperty[0]", equalTo(1))
			.body("result[0].integerArrayProperty[1]", equalTo(2))

			.body("result[1].integerArrayProperty[0]", equalTo(1))
			.body("result[1].integerArrayProperty[1]", equalTo(2))
			.body("result[1].integerArrayProperty[2]", equalTo(3))

			.body("result[2].integerArrayProperty[0]", equalTo(1))
			.body("result[2].integerArrayProperty[1]", equalTo(2))
			.body("result[2].integerArrayProperty[2]", equalTo(3))
			.body("result[2].integerArrayProperty[3]", equalTo(4))

			.body("result[3].integerArrayProperty[0]", equalTo(1))
			.body("result[3].integerArrayProperty[1]", equalTo(2))
			.body("result[3].integerArrayProperty[2]", equalTo(3))
			.body("result[3].integerArrayProperty[3]", equalTo(4))
			.body("result[3].integerArrayProperty[4]", equalTo(5))
		.when()
			.get("/test_threes?_sort=name&integerArrayProperty=2");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(3))

			.body("result[0].integerArrayProperty[0]", equalTo(1))
			.body("result[0].integerArrayProperty[1]", equalTo(2))
			.body("result[0].integerArrayProperty[2]", equalTo(3))

			.body("result[1].integerArrayProperty[0]", equalTo(1))
			.body("result[1].integerArrayProperty[1]", equalTo(2))
			.body("result[1].integerArrayProperty[2]", equalTo(3))
			.body("result[1].integerArrayProperty[3]", equalTo(4))

			.body("result[2].integerArrayProperty[0]", equalTo(1))
			.body("result[2].integerArrayProperty[1]", equalTo(2))
			.body("result[2].integerArrayProperty[2]", equalTo(3))
			.body("result[2].integerArrayProperty[3]", equalTo(4))
			.body("result[2].integerArrayProperty[4]", equalTo(5))
		.when()
			.get("/test_threes?_sort=name&integerArrayProperty=3");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(2))

			.body("result[0].integerArrayProperty[0]", equalTo(1))
			.body("result[0].integerArrayProperty[1]", equalTo(2))
			.body("result[0].integerArrayProperty[2]", equalTo(3))
			.body("result[0].integerArrayProperty[3]", equalTo(4))

			.body("result[1].integerArrayProperty[0]", equalTo(1))
			.body("result[1].integerArrayProperty[1]", equalTo(2))
			.body("result[1].integerArrayProperty[2]", equalTo(3))
			.body("result[1].integerArrayProperty[3]", equalTo(4))
			.body("result[1].integerArrayProperty[4]", equalTo(5))
		.when()
			.get("/test_threes?_sort=name&integerArrayProperty=4");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(1))

			.body("result[0].integerArrayProperty[0]", equalTo(1))
			.body("result[0].integerArrayProperty[1]", equalTo(2))
			.body("result[0].integerArrayProperty[2]", equalTo(3))
			.body("result[0].integerArrayProperty[3]", equalTo(4))
			.body("result[0].integerArrayProperty[4]", equalTo(5))
		.when()
			.get("/test_threes?_sort=name&integerArrayProperty=5");
	}


	@Test
	public void testSearchIntegerArrayOR() {

		// create test objects
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body("["
				+ "{ 'name': 'test1', 'integerArrayProperty' : [1] },"
				+ "{ 'name': 'test2', 'integerArrayProperty' : [2] },"
				+ "{ 'name': 'test3', 'integerArrayProperty' : [3] }"
				+ "]")
		.expect()
			.statusCode(201)
		.when()
			.post("/test_threes?_sort=name")
			.getHeader("Location");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(2))

			.body("result[0].integerArrayProperty[0]", equalTo(1))

			.body("result[1].integerArrayProperty[0]", equalTo(2))

		.when()
			.get("/test_threes?_sort=name&integerArrayProperty=1;2");

	}


	@Test
	public void testSearchIntegerArrayAND() {

		// create test objects
		RestAssured.given()
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.contentType("application/json; charset=UTF-8")
			.body("["
				+ "{ 'name': 'test1', 'integerArrayProperty' : [1] },"
				+ "{ 'name': 'test2', 'integerArrayProperty' : [1, 2] },"
				+ "{ 'name': 'test3', 'integerArrayProperty' : [1, 2, 3] }"
				+ "]")
		.expect()
			.statusCode(201)
		.when()
			.post("/test_threes?_sort=name")
			.getHeader("Location");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(2))

			.body("result[0].integerArrayProperty[0]", equalTo(1))
			.body("result[0].integerArrayProperty[1]", equalTo(2))

			.body("result[1].integerArrayProperty[0]", equalTo(1))
			.body("result[1].integerArrayProperty[1]", equalTo(2))
			.body("result[1].integerArrayProperty[2]", equalTo(3))

		.when()
			.get("/test_threes?_sort=name&integerArrayProperty=1,2");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(1))

			.body("result[0].integerArrayProperty[0]", equalTo(1))
			.body("result[0].integerArrayProperty[1]", equalTo(2))
			.body("result[0].integerArrayProperty[2]", equalTo(3))

		.when()
			.get("/test_threes?_sort=name&integerArrayProperty=1,2,3");
	}
}
