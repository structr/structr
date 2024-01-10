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
public class DateArrayPropertyRestTest extends StructrRestTestBase {

	@Test
	public void testDateArrayViaRest() {

		String location = RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { 'dateArrayProperty' : [ '2019-02-05T12:34:56+0000', '2019-03-05T23:45:00+0000', '2020-11-24T14:15:16+0000' ] } ")
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
			.body("result[0].dateArrayProperty[0]", equalTo("2019-02-05T12:34:56+0000"))
			.body("result[0].dateArrayProperty[1]", equalTo("2019-03-05T23:45:00+0000"))
			.body("result[0].dateArrayProperty[2]", equalTo("2020-11-24T14:15:16+0000"))
		.when()
			.get("/test_threes");

	}

	@Test
	public void testDateArraySearch() {

		// create test objects
		final String id1 = getUuidFromLocation(RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { 'dateArrayProperty' : [ '2019-02-05T12:34:56+0000', '2019-03-05T23:45:00+0000', '2020-11-24T14:15:16+0000' ] } ")
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
			.get("/test_threes?dateArrayProperty=");


		// test search for empty array property
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
		.expect()
			.statusCode(200)
			.body("result[0].id", equalTo(id1))
		.when()
			.get("/test_threes?dateArrayProperty=[]");



	}


	@Test
	public void testSearchDateArray() {

		// create test objects
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body("["
				+ "{ 'name': 'test1', 'dateArrayProperty' : [ '2019-02-05T12:34:56+0000' ] },"
				+ "{ 'name': 'test2', 'dateArrayProperty' : [ '2019-02-05T12:34:56+0000', '2019-03-05T23:45:00+0000' ] },"
				+ "{ 'name': 'test3', 'dateArrayProperty' : [ '2019-02-05T12:34:56+0000', '2019-03-05T23:45:00+0000', '2020-11-24T14:15:16+0000' ] }"
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
			.body("result_count", equalTo(3))

			.body("result[0].dateArrayProperty[0]", equalTo("2019-02-05T12:34:56+0000"))

			.body("result[1].dateArrayProperty[0]", equalTo("2019-02-05T12:34:56+0000"))
			.body("result[1].dateArrayProperty[1]", equalTo("2019-03-05T23:45:00+0000"))

			.body("result[2].dateArrayProperty[0]", equalTo("2019-02-05T12:34:56+0000"))
			.body("result[2].dateArrayProperty[1]", equalTo("2019-03-05T23:45:00+0000"))
			.body("result[2].dateArrayProperty[2]", equalTo("2020-11-24T14:15:16+0000"))

		.when()
			.get("/test_threes?_sort=name&dateArrayProperty=2019-02-05T12:34:56+0000");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(2))

			.body("result[0].dateArrayProperty[0]", equalTo("2019-02-05T12:34:56+0000"))
			.body("result[0].dateArrayProperty[1]", equalTo("2019-03-05T23:45:00+0000"))

			.body("result[1].dateArrayProperty[0]", equalTo("2019-02-05T12:34:56+0000"))
			.body("result[1].dateArrayProperty[1]", equalTo("2019-03-05T23:45:00+0000"))
			.body("result[1].dateArrayProperty[2]", equalTo("2020-11-24T14:15:16+0000"))
		.when()
			.get("/test_threes?_sort=name&dateArrayProperty=2019-03-05T23:45:00+0000");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(1))

			.body("result[0].dateArrayProperty[0]", equalTo("2019-02-05T12:34:56+0000"))
			.body("result[0].dateArrayProperty[1]", equalTo("2019-03-05T23:45:00+0000"))
			.body("result[0].dateArrayProperty[2]", equalTo("2020-11-24T14:15:16+0000"))

		.when()
			.get("/test_threes?_sort=name&dateArrayProperty=2020-11-24T14:15:16+0000");

	}


	@Test
	public void testSearchDateArrayOR() {

		// create test objects
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body("["
				+ "{ 'name': 'test1', 'dateArrayProperty' : ['2019-02-05T12:34:56+0000'] },"
				+ "{ 'name': 'test2', 'dateArrayProperty' : ['2019-03-05T23:45:00+0000'] },"
				+ "{ 'name': 'test3', 'dateArrayProperty' : ['2020-11-24T14:15:16+0000'] }"
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

			.body("result[0].dateArrayProperty[0]", equalTo("2019-02-05T12:34:56+0000"))

			.body("result[1].dateArrayProperty[0]", equalTo("2019-03-05T23:45:00+0000"))

		.when()
			.get("/test_threes?_sort=name&dateArrayProperty=2019-02-05T12:34:56+0000;2019-03-05T23:45:00+0000");

	}


	@Test
	public void testSearchDateArrayAND() {

		// create test objects
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body("["
				+ "{ 'name': 'test1', 'dateArrayProperty' : [ '2019-02-05T12:34:56+0000' ] },"
				+ "{ 'name': 'test2', 'dateArrayProperty' : [ '2019-02-05T12:34:56+0000', '2019-03-05T23:45:00+0000' ] },"
				+ "{ 'name': 'test3', 'dateArrayProperty' : [ '2019-02-05T12:34:56+0000', '2019-03-05T23:45:00+0000', '2020-11-24T14:15:16+0000' ] }"
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

			.body("result[0].dateArrayProperty[0]", equalTo("2019-02-05T12:34:56+0000"))
			.body("result[0].dateArrayProperty[1]", equalTo("2019-03-05T23:45:00+0000"))

			.body("result[1].dateArrayProperty[0]", equalTo("2019-02-05T12:34:56+0000"))
			.body("result[1].dateArrayProperty[1]", equalTo("2019-03-05T23:45:00+0000"))
			.body("result[1].dateArrayProperty[2]", equalTo("2020-11-24T14:15:16+0000"))

		.when()
			.get("/test_threes?_sort=name&dateArrayProperty=2019-02-05T12:34:56+0000,2019-03-05T23:45:00+0000");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(1))

			.body("result[0].dateArrayProperty[0]", equalTo("2019-02-05T12:34:56+0000"))
			.body("result[0].dateArrayProperty[1]", equalTo("2019-03-05T23:45:00+0000"))
			.body("result[0].dateArrayProperty[2]", equalTo("2020-11-24T14:15:16+0000"))

		.when()
			.get("/test_threes?_sort=name&dateArrayProperty=2019-02-05T12:34:56+0000,2019-03-05T23:45:00+0000,2020-11-24T14:15:16+0000");
	}
}
