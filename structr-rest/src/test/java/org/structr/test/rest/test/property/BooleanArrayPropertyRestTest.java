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
public class BooleanArrayPropertyRestTest extends StructrRestTestBase {

	@Test
	public void testStringArrayViaRest() {

		String location = RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { 'booleanArrayProperty' : [true, false, true, true, false] } ")
		.expect()
			.statusCode(201)
		.when()
			.post("/TestThree")
			.getHeader("Location");

		String uuid = getUuidFromLocation(location);

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result[0].booleanArrayProperty[0]", equalTo(true))
			.body("result[0].booleanArrayProperty[1]", equalTo(false))
			.body("result[0].booleanArrayProperty[2]", equalTo(true))
			.body("result[0].booleanArrayProperty[3]", equalTo(true))
			.body("result[0].booleanArrayProperty[4]", equalTo(false))
		.when()
			.get("/TestThree");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { 'booleanArrayProperty' : [true, true, false] } ")
		.expect()
			.statusCode(200)
		.when()
			.put("/TestThree/" + uuid);

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result[0].booleanArrayProperty[0]", equalTo(true))
			.body("result[0].booleanArrayProperty[1]", equalTo(true))
			.body("result[0].booleanArrayProperty[2]", equalTo(false))
		.when()
			.get("/TestThree");
	}

	@Test
	public void testStringArraySearch() {

		// create test objects
		final String id1 = getUuidFromLocation(RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { 'booleanArrayProperty' : [true, false, true, true, false] } ")
		.expect()
			.statusCode(201)
		.when()
			.post("/TestThree")
			.getHeader("Location")
		);

		// create test objects
		final String id2 = getUuidFromLocation(RestAssured.given()
			.contentType("application/json; charset=UTF-8")
		.expect()
			.statusCode(201)
		.when()
			.post("/TestThree")
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
			.get("/TestThree?booleanArrayProperty=");


		// test search for empty array property
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
		.expect()
			.statusCode(200)
			.body("result[0].id", equalTo(id1))
		.when()
			.get("/TestThree?booleanArrayProperty=[]");
	}


	@Test
	public void testSearchStringArray() {

		// create test objects
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body("["
				+ "{ 'name': 'test1', 'booleanArrayProperty' : [true] },"
				+ "{ 'name': 'test2', 'booleanArrayProperty' : [true, true] },"
				+ "{ 'name': 'test3', 'booleanArrayProperty' : [true, false, true] },"
				+ "{ 'name': 'test4', 'booleanArrayProperty' : [false, false] }"
				+ "]")
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
			.body("result_count", equalTo(3))

			.body("result[0].booleanArrayProperty[0]", equalTo(true))

			.body("result[1].booleanArrayProperty[0]", equalTo(true))
			.body("result[1].booleanArrayProperty[1]", equalTo(true))

			.body("result[2].booleanArrayProperty[0]", equalTo(true))
			.body("result[2].booleanArrayProperty[1]", equalTo(false))
			.body("result[2].booleanArrayProperty[2]", equalTo(true))

		.when()
			.get("/TestThree?_sort=name&booleanArrayProperty=true");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(2))

			.body("result[0].booleanArrayProperty[0]", equalTo(true))
			.body("result[0].booleanArrayProperty[1]", equalTo(false))
			.body("result[0].booleanArrayProperty[2]", equalTo(true))

			.body("result[1].booleanArrayProperty[0]", equalTo(false))
			.body("result[1].booleanArrayProperty[1]", equalTo(false))
		.when()
			.get("/TestThree?_sort=name&booleanArrayProperty=false");
	}


	@Test
	public void testSearchStringArrayOR() {

		// create test objects
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body("["
				+ "{ 'name': 'test1', 'booleanArrayProperty' : [true] },"
				+ "{ 'name': 'test2', 'booleanArrayProperty' : [true, false] },"
				+ "{ 'name': 'test3', 'booleanArrayProperty' : [true, false, true] }"
				+ "]")
		.expect()
			.statusCode(201)
		.when()
			.post("/TestThree?_sort=name")
			.getHeader("Location");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(3))

			.body("result[0].booleanArrayProperty[0]", equalTo(true))

			.body("result[1].booleanArrayProperty[0]", equalTo(true))
			.body("result[1].booleanArrayProperty[1]", equalTo(false))

			.body("result[2].booleanArrayProperty[0]", equalTo(true))
			.body("result[2].booleanArrayProperty[0]", equalTo(true))
			.body("result[2].booleanArrayProperty[1]", equalTo(false))

		.when()
			.get("/TestThree?_sort=name&booleanArrayProperty=true;false");

	}


	@Test
	public void testSearchStringArrayAND() {

		// create test objects
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body("["
				+ "{ 'name': 'test1', 'booleanArrayProperty' : [true] },"
				+ "{ 'name': 'test2', 'booleanArrayProperty' : [true, false] },"
				+ "{ 'name': 'test3', 'booleanArrayProperty' : [true, false, true] }"
				+ "]")
		.expect()
			.statusCode(201)
		.when()
			.post("/TestThree?_sort=name")
			.getHeader("Location");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(2))

			.body("result[0].booleanArrayProperty[0]", equalTo(true))
			.body("result[0].booleanArrayProperty[1]", equalTo(false))

			.body("result[1].booleanArrayProperty[0]", equalTo(true))
			.body("result[1].booleanArrayProperty[1]", equalTo(false))
			.body("result[1].booleanArrayProperty[2]", equalTo(true))

		.when()
			.get("/TestThree?_sort=name&booleanArrayProperty=true,false");
	}
}
