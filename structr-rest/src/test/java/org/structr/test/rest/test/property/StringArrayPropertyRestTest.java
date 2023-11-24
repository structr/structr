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
public class StringArrayPropertyRestTest extends StructrRestTestBase {

	@Test
	public void testStringArrayViaRest() {

		String location = RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { 'stringArrayProperty' : ['one', 'two', 'three', 'four', 'five'] } ")
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
			.body("result[0].stringArrayProperty[0]", equalTo("one"))
			.body("result[0].stringArrayProperty[1]", equalTo("two"))
			.body("result[0].stringArrayProperty[2]", equalTo("three"))
			.body("result[0].stringArrayProperty[3]", equalTo("four"))
			.body("result[0].stringArrayProperty[4]", equalTo("five"))
		.when()
			.get("/TestThree");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { 'stringArrayProperty' : ['three', 'four', 'five'] } ")
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
			.body("result[0].stringArrayProperty[0]", equalTo("three"))
			.body("result[0].stringArrayProperty[1]", equalTo("four"))
			.body("result[0].stringArrayProperty[2]", equalTo("five"))
		.when()
			.get("/TestThree");
	}

	@Test
	public void testStringArraySearch() {

		// create test objects
		final String id1 = getUuidFromLocation(RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { 'stringArrayProperty' : ['one', 'two', 'three', 'four', 'five'] } ")
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
			.get("/TestThree?stringArrayProperty=");


		// test search for empty array property
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
		.expect()
			.statusCode(200)
			.body("result[0].id", equalTo(id1))
		.when()
			.get("/TestThree?stringArrayProperty=[]");



	}


	@Test
	public void testSearchStringArray() {

		// create test objects
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body("["
				+ "{ 'name': 'test1', 'stringArrayProperty' : ['one'] },"
				+ "{ 'name': 'test2', 'stringArrayProperty' : ['one', 'two'] },"
				+ "{ 'name': 'test3', 'stringArrayProperty' : ['one', 'two', 'three'] },"
				+ "{ 'name': 'test4', 'stringArrayProperty' : ['one', 'two', 'three', 'four'] },"
				+ "{ 'name': 'test5', 'stringArrayProperty' : ['one', 'two', 'three', 'four', 'five'] }"
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
			.body("result_count", equalTo(5))

			.body("result[0].stringArrayProperty[0]", equalTo("one"))

			.body("result[1].stringArrayProperty[0]", equalTo("one"))
			.body("result[1].stringArrayProperty[1]", equalTo("two"))

			.body("result[2].stringArrayProperty[0]", equalTo("one"))
			.body("result[2].stringArrayProperty[1]", equalTo("two"))
			.body("result[2].stringArrayProperty[2]", equalTo("three"))

			.body("result[3].stringArrayProperty[0]", equalTo("one"))
			.body("result[3].stringArrayProperty[1]", equalTo("two"))
			.body("result[3].stringArrayProperty[2]", equalTo("three"))
			.body("result[3].stringArrayProperty[3]", equalTo("four"))

			.body("result[4].stringArrayProperty[0]", equalTo("one"))
			.body("result[4].stringArrayProperty[1]", equalTo("two"))
			.body("result[4].stringArrayProperty[2]", equalTo("three"))
			.body("result[4].stringArrayProperty[3]", equalTo("four"))
			.body("result[4].stringArrayProperty[4]", equalTo("five"))

		.when()
			.get("/TestThree?_sort=name&stringArrayProperty=one");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(4))

			.body("result[0].stringArrayProperty[0]", equalTo("one"))
			.body("result[0].stringArrayProperty[1]", equalTo("two"))

			.body("result[1].stringArrayProperty[0]", equalTo("one"))
			.body("result[1].stringArrayProperty[1]", equalTo("two"))
			.body("result[1].stringArrayProperty[2]", equalTo("three"))

			.body("result[2].stringArrayProperty[0]", equalTo("one"))
			.body("result[2].stringArrayProperty[1]", equalTo("two"))
			.body("result[2].stringArrayProperty[2]", equalTo("three"))
			.body("result[2].stringArrayProperty[3]", equalTo("four"))

			.body("result[3].stringArrayProperty[0]", equalTo("one"))
			.body("result[3].stringArrayProperty[1]", equalTo("two"))
			.body("result[3].stringArrayProperty[2]", equalTo("three"))
			.body("result[3].stringArrayProperty[3]", equalTo("four"))
			.body("result[3].stringArrayProperty[4]", equalTo("five"))
		.when()
			.get("/TestThree?_sort=name&stringArrayProperty=two");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(3))

			.body("result[0].stringArrayProperty[0]", equalTo("one"))
			.body("result[0].stringArrayProperty[1]", equalTo("two"))
			.body("result[0].stringArrayProperty[2]", equalTo("three"))

			.body("result[1].stringArrayProperty[0]", equalTo("one"))
			.body("result[1].stringArrayProperty[1]", equalTo("two"))
			.body("result[1].stringArrayProperty[2]", equalTo("three"))
			.body("result[1].stringArrayProperty[3]", equalTo("four"))

			.body("result[2].stringArrayProperty[0]", equalTo("one"))
			.body("result[2].stringArrayProperty[1]", equalTo("two"))
			.body("result[2].stringArrayProperty[2]", equalTo("three"))
			.body("result[2].stringArrayProperty[3]", equalTo("four"))
			.body("result[2].stringArrayProperty[4]", equalTo("five"))
		.when()
			.get("/TestThree?_sort=name&stringArrayProperty=three");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(2))

			.body("result[0].stringArrayProperty[0]", equalTo("one"))
			.body("result[0].stringArrayProperty[1]", equalTo("two"))
			.body("result[0].stringArrayProperty[2]", equalTo("three"))
			.body("result[0].stringArrayProperty[3]", equalTo("four"))

			.body("result[1].stringArrayProperty[0]", equalTo("one"))
			.body("result[1].stringArrayProperty[1]", equalTo("two"))
			.body("result[1].stringArrayProperty[2]", equalTo("three"))
			.body("result[1].stringArrayProperty[3]", equalTo("four"))
			.body("result[1].stringArrayProperty[4]", equalTo("five"))
		.when()
			.get("/TestThree?_sort=name&stringArrayProperty=four");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(1))

			.body("result[0].stringArrayProperty[0]", equalTo("one"))
			.body("result[0].stringArrayProperty[1]", equalTo("two"))
			.body("result[0].stringArrayProperty[2]", equalTo("three"))
			.body("result[0].stringArrayProperty[3]", equalTo("four"))
			.body("result[0].stringArrayProperty[4]", equalTo("five"))
		.when()
			.get("/TestThree?_sort=name&stringArrayProperty=five");
	}


	@Test
	public void testSearchStringArrayOR() {

		// create test objects
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body("["
				+ "{ 'name': 'test1', 'stringArrayProperty' : ['one'] },"
				+ "{ 'name': 'test2', 'stringArrayProperty' : ['two'] },"
				+ "{ 'name': 'test3', 'stringArrayProperty' : ['three'] }"
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

			.body("result[0].stringArrayProperty[0]", equalTo("one"))

			.body("result[1].stringArrayProperty[0]", equalTo("two"))

		.when()
			.get("/TestThree?_sort=name&stringArrayProperty=one;two");

	}


	@Test
	public void testSearchStringArrayAND() {

		// create test objects
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body("["
				+ "{ 'name': 'test1', 'stringArrayProperty' : ['one'] },"
				+ "{ 'name': 'test2', 'stringArrayProperty' : ['one', 'two'] },"
				+ "{ 'name': 'test3', 'stringArrayProperty' : ['one', 'two', 'three'] }"
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

			.body("result[0].stringArrayProperty[0]", equalTo("one"))
			.body("result[0].stringArrayProperty[1]", equalTo("two"))

			.body("result[1].stringArrayProperty[0]", equalTo("one"))
			.body("result[1].stringArrayProperty[1]", equalTo("two"))
			.body("result[1].stringArrayProperty[2]", equalTo("three"))

		.when()
			.get("/TestThree?_sort=name&stringArrayProperty=one,two");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(1))

			.body("result[0].stringArrayProperty[0]", equalTo("one"))
			.body("result[0].stringArrayProperty[1]", equalTo("two"))
			.body("result[0].stringArrayProperty[2]", equalTo("three"))

		.when()
			.get("/TestThree?_sort=name&stringArrayProperty=one,two,three");
	}
}
