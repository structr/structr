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
package org.structr.test.rest.test.property;

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.apache.commons.lang3.StringUtils;
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.Test;

import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;

/**
 *
 *
 */
public class UnifiedArrayPropertyRestTest extends StructrRestTestBase {

	@Test
	public void testArrayCreationError() {

		testArrayCreationError("boolean", true);
		//testArrayUpdate("byte", (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05);
		testArrayCreationError("date", "2019-02-05T12:34:56+0000");
		testArrayCreationError("double", 1.0f);
		testArrayCreationError("enum", "Status1");
		testArrayCreationError("integer", 1);
		testArrayCreationError("long", 1);
		testArrayCreationError("string", "one");
	}

	private <T> void testArrayCreationError(final String type, final T value1) {

		final String creationValue = join(value1);

		// we don't accept single values for array properties anymore!

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { '" + type + "ArrayProperty' : " + creationValue + " } ")
			.expect()
			.statusCode(422)
			.when()
			.post("/TestThree")
			.getHeader("Location");
	}

	@Test
	public void testArrayUpdate() {

		// boolean, byte, date, double, enum, integer, long, string

		testArrayUpdate("boolean", true, false, true, false, true);
		//testArrayUpdate("byte", (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05);
		testArrayUpdate("date", "2019-02-05T12:34:56+0000", "2019-03-05T23:45:00+0000", "2020-11-24T14:15:16+0000", "2022-12-30T11:12:33+0000", "2025-01-01T18:00:00+0000");
		testArrayUpdate("double", 1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
		testArrayUpdate("enum", "Status1", "Status2", "Status3", "Status4", "Status5");
		testArrayUpdate("integer", 1, 2, 3, 4, 5);
		testArrayUpdate("long", 1, 2, 3, 4, 5);
		testArrayUpdate("string", "one", "two", "three", "four", "five");
	}

	private <T> void testArrayUpdate(final String type, final T value1, final T value2, final T value3, final T value4, final T value5) {

		final String creationValues = join(value1, value2, value3, value4, value5);
		final String updateValues   = join(value3, value4, value5);

		String location = RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { '" + type + "ArrayProperty' : [" + creationValues + "] } ")
		.expect()
			.statusCode(201)
		.when()
			.post("/TestThree")
			.getHeader("Location");

		String uuid = getUuidFromLocation(location);

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result[0]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[0]." + type + "ArrayProperty[1]", equalTo(value2))
			.body("result[0]." + type + "ArrayProperty[2]", equalTo(value3))
			.body("result[0]." + type + "ArrayProperty[3]", equalTo(value4))
			.body("result[0]." + type + "ArrayProperty[4]", equalTo(value5))
		.when()
			.get("/TestThree");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { '" + type + "ArrayProperty' : [" + updateValues + "] } ")
		.expect()
			.statusCode(200)
		.when()
			.put("/TestThree/" + uuid);

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result[0]." + type + "ArrayProperty[0]", equalTo(value3))
			.body("result[0]." + type + "ArrayProperty[1]", equalTo(value4))
			.body("result[0]." + type + "ArrayProperty[2]", equalTo(value5))
		.when()
			.get("/TestThree");

		// remove all test objects
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.when()
			.delete("/TestThree");
	}

	@Test
	public void testArraySearchEmpty() {

		testArraySearchEmpty("boolean", true, false, true, false, true);
		//testArrayUpdate("byte", (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05);
		testArraySearchEmpty("date", "2019-02-05T12:34:56+0000", "2019-03-05T23:45:00+0000", "2020-11-24T14:15:16+0000", "2022-12-30T11:12:33+0000", "2025-01-01T18:00:00+0000");
		testArraySearchEmpty("double", 1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
		testArraySearchEmpty("enum", "Status1", "Status2", "Status3", "Status4", "Status5");
		testArraySearchEmpty("integer", 1, 2, 3, 4, 5);
		testArraySearchEmpty("long", 1, 2, 3, 4, 5);
		testArraySearchEmpty("string", "one", "two", "three", "four", "five");
	}

	private <T> void testArraySearchEmpty(final String type, final T value1, final T value2, final T value3, final T value4, final T value5) {

		// create test object with values
		final String id1 = getUuidFromLocation(RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { '" + type + "ArrayProperty' : [" + join(value1, value2, value3, value4, value5) + "] } ")
		.expect()
			.statusCode(201)
		.when()
			.post("/TestThree")
			.getHeader("Location")
		);

		// create empty test object
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
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result[0].id", equalTo(id2))
		.when()
			.get("/TestThree?" + type + "ArrayProperty=");

		// test search for empty array property
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
		.expect()
			.statusCode(200)
			.body("result[0].id", equalTo(id1))
		.when()
			.get("/TestThree?" + type + "ArrayProperty=[]");

		// remove all test objects
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.when()
			.delete("/TestThree");
	}

	@Test
	public void testArraySearchExact() {

		// unified search doesn't work with booleans because we only have two distinct values
		//testArraySearchExact("boolean", true, false, true, false, true);
		testArraySearchExactWithBoolean("boolean", true, false, true, false, true);

		//testArraySearchExact("byte", (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05);
		testArraySearchExact("date", "2019-02-05T12:34:56+0000", "2019-03-05T23:45:00+0000", "2020-11-24T14:15:16+0000", "2022-12-30T11:12:33+0000", "2025-01-01T18:00:00+0000");
		testArraySearchExact("double", 1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
		testArraySearchExact("enum", "Status1", "Status2", "Status3", "Status4", "Status5");
		testArraySearchExact("integer", 1, 2, 3, 4, 5);
		testArraySearchExact("long", 1, 2, 3, 4, 5);
		testArraySearchExact("string", "one", "two", "three", "four", "five");
	}

	private void testArraySearchExactWithBoolean(final String type, final boolean value1, final boolean value2, final boolean value3, final boolean value4, final boolean value5) {

		// create test objects
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body("["
				+ "{ 'name': 'test1', '" + type + "ArrayProperty' : [" + join(value1) + "] },"
				+ "{ 'name': 'test2', '" + type + "ArrayProperty' : [" + join(value1, value2) + "] },"
				+ "{ 'name': 'test3', '" + type + "ArrayProperty' : [" + join(value1, value2, value3) + "] },"
				+ "{ 'name': 'test4', '" + type + "ArrayProperty' : [" + join(value1, value2, value3, value4) + "] },"
				+ "{ 'name': 'test5', '" + type + "ArrayProperty' : [" + join(value1, value2, value3, value4, value5) + "] }"
				+ "]")
			.expect()
			.statusCode(201)
			.when()
			.post("/TestThree?_sort=name")
			.getHeader("Location");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.when()
			.get("/TestThree?_sort=name");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.body("result_count", equalTo(1))

			.body("result[0]." + type + "ArrayProperty[0]", equalTo(value1))

			.when()
			.get("/TestThree?_sort=name&" + type + "ArrayProperty=" + value1);

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.body("result_count", equalTo(1))

			.body("result[0]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[0]." + type + "ArrayProperty[1]", equalTo(value2))

			.when()
			.get("/TestThree?_sort=name&" + type + "ArrayProperty=" + value1 + "," + value2);

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.body("result_count", equalTo(1))

			.body("result[0]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[0]." + type + "ArrayProperty[1]", equalTo(value2))
			.body("result[0]." + type + "ArrayProperty[2]", equalTo(value3))

			.when()
			.get("/TestThree?_sort=name&" + type + "ArrayProperty=" + value1 + "," + value2 + "," + value3);

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.body("result_count", equalTo(1))

			.body("result[0]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[0]." + type + "ArrayProperty[1]", equalTo(value2))
			.body("result[0]." + type + "ArrayProperty[2]", equalTo(value3))
			.body("result[0]." + type + "ArrayProperty[3]", equalTo(value4))

			.when()
			.get("/TestThree?_sort=name&" + type + "ArrayProperty=" + value1 + "," + value2 + "," + value3 + "," + value4);

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.body("result_count", equalTo(1))

			.body("result[0]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[0]." + type + "ArrayProperty[1]", equalTo(value2))
			.body("result[0]." + type + "ArrayProperty[2]", equalTo(value3))
			.body("result[0]." + type + "ArrayProperty[3]", equalTo(value4))
			.body("result[0]." + type + "ArrayProperty[4]", equalTo(value5))

			.when()
			.get("/TestThree?_sort=name&" + type + "ArrayProperty=" + value1 + "," + value2 + "," + value3 + "," + value4 + "," + value5);

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.body("result_count", equalTo(0))
			.when()
			.get("/TestThree?_sort=name&" + type + "ArrayProperty=" + value2);

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.body("result_count", equalTo(0))
			.when()
			.get("/TestThree?_sort=name&" + type + "ArrayProperty=" + value1 + "," + value3);

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.body("result_count", equalTo(0))
			.when()
			.get("/TestThree?_sort=name&" + type + "ArrayProperty=" + value5 + "," + value1);

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.body("result_count", equalTo(0))
			.when()
			.get("/TestThree?_sort=name&" + type + "ArrayProperty=" + value2 + "," + value3);

		// remove all test objects
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.when()
			.delete("/TestThree");
	}

	private <T> void testArraySearchExact(final String type, final T value1, final T value2, final T value3, final T value4, final T value5) {

		// create test objects
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body("["
				+ "{ 'name': 'test1', '" + type + "ArrayProperty' : [" + join(value1) + "] },"
				+ "{ 'name': 'test2', '" + type + "ArrayProperty' : [" + join(value1, value2) + "] },"
				+ "{ 'name': 'test3', '" + type + "ArrayProperty' : [" + join(value1, value2, value3) + "] },"
				+ "{ 'name': 'test4', '" + type + "ArrayProperty' : [" + join(value1, value2, value3, value4) + "] },"
				+ "{ 'name': 'test5', '" + type + "ArrayProperty' : [" + join(value1, value2, value3, value4, value5) + "] }"
				+ "]")
			.expect()
			.statusCode(201)
			.when()
			.post("/TestThree?_sort=name")
			.getHeader("Location");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.when()
			.get("/TestThree?_sort=name");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.body("result_count", equalTo(1))

			.body("result[0]." + type + "ArrayProperty[0]", equalTo(value1))

			.when()
			.get("/TestThree?_sort=name&" + type + "ArrayProperty=" + value1);

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.body("result_count", equalTo(1))

			.body("result[0]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[0]." + type + "ArrayProperty[1]", equalTo(value2))

			.when()
			.get("/TestThree?_sort=name&" + type + "ArrayProperty=" + value1 + "," + value2);

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.body("result_count", equalTo(1))

			.body("result[0]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[0]." + type + "ArrayProperty[1]", equalTo(value2))
			.body("result[0]." + type + "ArrayProperty[2]", equalTo(value3))
			.when()
			.get("/TestThree?_sort=name&" + type + "ArrayProperty=" + value1 + "," + value2 + "," + value3);

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.body("result_count", equalTo(1))

			.body("result[0]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[0]." + type + "ArrayProperty[1]", equalTo(value2))
			.body("result[0]." + type + "ArrayProperty[2]", equalTo(value3))
			.body("result[0]." + type + "ArrayProperty[3]", equalTo(value4))

			.when()
			.get("/TestThree?_sort=name&" + type + "ArrayProperty=" + value1 + "," + value2 + "," + value3 + "," + value4);

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.body("result_count", equalTo(1))

			.body("result[0]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[0]." + type + "ArrayProperty[1]", equalTo(value2))
			.body("result[0]." + type + "ArrayProperty[2]", equalTo(value3))
			.body("result[0]." + type + "ArrayProperty[3]", equalTo(value4))
			.body("result[0]." + type + "ArrayProperty[4]", equalTo(value5))
			.when()
			.get("/TestThree?_sort=name&" + type + "ArrayProperty=" + value1 + "," + value2 + "," + value3 + "," + value4 + "," + value5);

		// test other combinations that shouldn't return results
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.body("result_count", equalTo(0))
			.when()
			.get("/TestThree?_sort=name&" + type + "ArrayProperty=" + value1 + "," + value3);

		// test other combinations that shouldn't return results
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.body("result_count", equalTo(0))
			.when()
			.get("/TestThree?_sort=name&" + type + "ArrayProperty=" + value5 + "," + value2);

		// test other combinations that shouldn't return results
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.body("result_count", equalTo(0))
			.when()
			.get("/TestThree?_sort=name&" + type + "ArrayProperty=" + value2);

		// test other combinations that shouldn't return results
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.body("result_count", equalTo(0))
			.when()
			.get("/TestThree?_sort=name&" + type + "ArrayProperty=" + value4);

		// remove all test objects
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.when()
			.delete("/TestThree");
	}

	@Test
	public void testArraySearchInexact() {

		// unified search doesn't work with booleans because we only have two distinct values
		//testArraySearchInexact("boolean", true, false, true, false, true);
		testArraySearchInexactWithBoolean("boolean", true, false, true, false, true);

		//testArraySearchInexact("byte", (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05);
		testArraySearchInexact("date", "2019-02-05T12:34:56+0000", "2019-03-05T23:45:00+0000", "2020-11-24T14:15:16+0000", "2022-12-30T11:12:33+0000", "2025-01-01T18:00:00+0000");
		testArraySearchInexact("double", 1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
		testArraySearchInexact("enum", "Status1", "Status2", "Status3", "Status4", "Status5");
		testArraySearchInexact("integer", 1, 2, 3, 4, 5);
		testArraySearchInexact("long", 1, 2, 3, 4, 5);
		testArraySearchInexact("string", "one", "two", "three", "four", "five");
	}

	private void testArraySearchInexactWithBoolean(final String type, final boolean value1, final boolean value2, final boolean value3, final boolean value4, final boolean value5) {

		// create test objects
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body("["
				+ "{ 'name': 'test1', '" + type + "ArrayProperty' : [" + join(value1) + "] },"
				+ "{ 'name': 'test2', '" + type + "ArrayProperty' : [" + join(value1, value2) + "] },"
				+ "{ 'name': 'test3', '" + type + "ArrayProperty' : [" + join(value1, value2, value3) + "] },"
				+ "{ 'name': 'test4', '" + type + "ArrayProperty' : [" + join(value1, value2, value3, value4) + "] },"
				+ "{ 'name': 'test5', '" + type + "ArrayProperty' : [" + join(value1, value2, value3, value4, value5) + "] }"
				+ "]")
			.expect()
			.statusCode(201)
			.when()
			.post("/TestThree?_sort=name")
			.getHeader("Location");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.when()
			.get("/TestThree?_sort=name");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.body("result_count", equalTo(5))

			.body("result[0]." + type + "ArrayProperty[0]", equalTo(value1))

			.body("result[1]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[1]." + type + "ArrayProperty[1]", equalTo(value2))

			.body("result[2]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[2]." + type + "ArrayProperty[1]", equalTo(value2))
			.body("result[2]." + type + "ArrayProperty[2]", equalTo(value3))

			.body("result[3]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[3]." + type + "ArrayProperty[1]", equalTo(value2))
			.body("result[3]." + type + "ArrayProperty[2]", equalTo(value3))
			.body("result[3]." + type + "ArrayProperty[3]", equalTo(value4))

			.body("result[4]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[4]." + type + "ArrayProperty[1]", equalTo(value2))
			.body("result[4]." + type + "ArrayProperty[2]", equalTo(value3))
			.body("result[4]." + type + "ArrayProperty[3]", equalTo(value4))
			.body("result[4]." + type + "ArrayProperty[4]", equalTo(value5))

			.when()
			.get("/TestThree?_sort=name&" + type + "ArrayProperty=" + value1 + "&_inexact=true");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.body("result_count", equalTo(4))

			.body("result[0]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[0]." + type + "ArrayProperty[1]", equalTo(value2))

			.body("result[1]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[1]." + type + "ArrayProperty[1]", equalTo(value2))
			.body("result[1]." + type + "ArrayProperty[2]", equalTo(value3))

			.body("result[2]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[2]." + type + "ArrayProperty[1]", equalTo(value2))
			.body("result[2]." + type + "ArrayProperty[2]", equalTo(value3))
			.body("result[2]." + type + "ArrayProperty[3]", equalTo(value4))

			.body("result[3]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[3]." + type + "ArrayProperty[1]", equalTo(value2))
			.body("result[3]." + type + "ArrayProperty[2]", equalTo(value3))
			.body("result[3]." + type + "ArrayProperty[3]", equalTo(value4))
			.body("result[3]." + type + "ArrayProperty[4]", equalTo(value5))
			.when()
			.get("/TestThree?_sort=name&" + type + "ArrayProperty=" + value2 + "&_inexact=true");

		// remove all test objects
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.when()
			.delete("/TestThree");
	}

	private <T> void testArraySearchInexact(final String type, final T value1, final T value2, final T value3, final T value4, final T value5) {

		// create test objects
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body("["
				+ "{ 'name': 'test1', '" + type + "ArrayProperty' : [" + join(value1) + "] },"
				+ "{ 'name': 'test2', '" + type + "ArrayProperty' : [" + join(value1, value2) + "] },"
				+ "{ 'name': 'test3', '" + type + "ArrayProperty' : [" + join(value1, value2, value3) + "] },"
				+ "{ 'name': 'test4', '" + type + "ArrayProperty' : [" + join(value1, value2, value3, value4) + "] },"
				+ "{ 'name': 'test5', '" + type + "ArrayProperty' : [" + join(value1, value2, value3, value4, value5) + "] }"
				+ "]")
		.expect()
			.statusCode(201)
		.when()
			.post("/TestThree?_sort=name")
			.getHeader("Location");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.when()
		.get("/TestThree?_sort=name");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(5))

			.body("result[0]." + type + "ArrayProperty[0]", equalTo(value1))

			.body("result[1]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[1]." + type + "ArrayProperty[1]", equalTo(value2))

			.body("result[2]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[2]." + type + "ArrayProperty[1]", equalTo(value2))
			.body("result[2]." + type + "ArrayProperty[2]", equalTo(value3))

			.body("result[3]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[3]." + type + "ArrayProperty[1]", equalTo(value2))
			.body("result[3]." + type + "ArrayProperty[2]", equalTo(value3))
			.body("result[3]." + type + "ArrayProperty[3]", equalTo(value4))

			.body("result[4]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[4]." + type + "ArrayProperty[1]", equalTo(value2))
			.body("result[4]." + type + "ArrayProperty[2]", equalTo(value3))
			.body("result[4]." + type + "ArrayProperty[3]", equalTo(value4))
			.body("result[4]." + type + "ArrayProperty[4]", equalTo(value5))

		.when()
			.get("/TestThree?_sort=name&" + type + "ArrayProperty=" + value1 + "&_inexact=true");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(4))

			.body("result[0]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[0]." + type + "ArrayProperty[1]", equalTo(value2))

			.body("result[1]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[1]." + type + "ArrayProperty[1]", equalTo(value2))
			.body("result[1]." + type + "ArrayProperty[2]", equalTo(value3))

			.body("result[2]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[2]." + type + "ArrayProperty[1]", equalTo(value2))
			.body("result[2]." + type + "ArrayProperty[2]", equalTo(value3))
			.body("result[2]." + type + "ArrayProperty[3]", equalTo(value4))

			.body("result[3]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[3]." + type + "ArrayProperty[1]", equalTo(value2))
			.body("result[3]." + type + "ArrayProperty[2]", equalTo(value3))
			.body("result[3]." + type + "ArrayProperty[3]", equalTo(value4))
			.body("result[3]." + type + "ArrayProperty[4]", equalTo(value5))
		.when()
			.get("/TestThree?_sort=name&" + type + "ArrayProperty=" + value2 + "&_inexact=true");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(3))

			.body("result[0]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[0]." + type + "ArrayProperty[1]", equalTo(value2))
			.body("result[0]." + type + "ArrayProperty[2]", equalTo(value3))

			.body("result[1]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[1]." + type + "ArrayProperty[1]", equalTo(value2))
			.body("result[1]." + type + "ArrayProperty[2]", equalTo(value3))
			.body("result[1]." + type + "ArrayProperty[3]", equalTo(value4))

			.body("result[2]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[2]." + type + "ArrayProperty[1]", equalTo(value2))
			.body("result[2]." + type + "ArrayProperty[2]", equalTo(value3))
			.body("result[2]." + type + "ArrayProperty[3]", equalTo(value4))
			.body("result[2]." + type + "ArrayProperty[4]", equalTo(value5))
		.when()
			.get("/TestThree?_sort=name&" + type + "ArrayProperty=" + value3 + "&_inexact=true");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(2))

			.body("result[0]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[0]." + type + "ArrayProperty[1]", equalTo(value2))
			.body("result[0]." + type + "ArrayProperty[2]", equalTo(value3))
			.body("result[0]." + type + "ArrayProperty[3]", equalTo(value4))

			.body("result[1]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[1]." + type + "ArrayProperty[1]", equalTo(value2))
			.body("result[1]." + type + "ArrayProperty[2]", equalTo(value3))
			.body("result[1]." + type + "ArrayProperty[3]", equalTo(value4))
			.body("result[1]." + type + "ArrayProperty[4]", equalTo(value5))
		.when()
			.get("/TestThree?_sort=name&" + type + "ArrayProperty=" + value4 + "&_inexact=true");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(1))

			.body("result[0]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[0]." + type + "ArrayProperty[1]", equalTo(value2))
			.body("result[0]." + type + "ArrayProperty[2]", equalTo(value3))
			.body("result[0]." + type + "ArrayProperty[3]", equalTo(value4))
			.body("result[0]." + type + "ArrayProperty[4]", equalTo(value5))
		.when()
			.get("/TestThree?_sort=name&" + type + "ArrayProperty=" + value5 + "&_inexact=true");

		// remove all test objects
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.when()
			.delete("/TestThree");
	}


	@Test
	public void testArraySearchWithOR() {

		// unified search doesn't work with booleans because we only have two distinct values
		//testArraySearchWithOR("boolean", true, false, true);
		testArraySearchWithORAndBoolean("boolean", true, false);

		//testArraySearchWithOR("byte", (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05);
		testArraySearchWithOR("date", "2019-02-05T12:34:56+0000", "2019-03-05T23:45:00+0000", "2020-11-24T14:15:16+0000");
		testArraySearchWithOR("double", 1.0f, 2.0f, 3.0f);
		testArraySearchWithOR("enum", "Status1", "Status2", "Status3");
		testArraySearchWithOR("integer", 1, 2, 3);
		testArraySearchWithOR("long", 1, 2, 3);
		testArraySearchWithOR("string", "one", "two", "three");
	}

	public void testArraySearchWithORAndBoolean(final String type, final boolean value1, final boolean value2) {

		// create test objects
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body("["
				+ "{ 'name': 'test1', '" + type + "ArrayProperty' : [" + join(value1) + "] },"
				+ "{ 'name': 'test2', '" + type + "ArrayProperty' : [" + join(value2) + "] },"
				+ "{ 'name': 'test3', '" + type + "ArrayProperty' : [" + join(value1, value2) + "] }"
				+ "]")
			.expect()
			.statusCode(201)
			.when()
			.post("/TestThree?_sort=name")
			.getHeader("Location");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.body("result_count", equalTo(3))
			.body("result[0]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[1]." + type + "ArrayProperty[0]", equalTo(value2))
			.body("result[2]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[2]." + type + "ArrayProperty[1]", equalTo(value2))

			.when()
			.get("/TestThree?_sort=name&" + type + "ArrayProperty=" + value1 + ";" + value2 + "&_inexact=1");

		// remove all test objects
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.when()
			.delete("/TestThree");

	}

	public <T> void testArraySearchWithOR(final String type, final T value1, final T value2, final T value3) {

		// create test objects
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body("["
				+ "{ 'name': 'test1', '" + type + "ArrayProperty' : [" + join(value1) + "] },"
				+ "{ 'name': 'test2', '" + type + "ArrayProperty' : [" + join(value2) + "] },"
				+ "{ 'name': 'test3', '" + type + "ArrayProperty' : [" + join(value3) + "] }"
				+ "]")
		.expect()
			.statusCode(201)
		.when()
			.post("/TestThree?_sort=name")
			.getHeader("Location");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(2))

			.body("result[0]." + type + "ArrayProperty[0]", equalTo(value1))

			.body("result[1]." + type + "ArrayProperty[0]", equalTo(value2))

		.when()
			.get("/TestThree?_sort=name&" + type + "ArrayProperty=" + value1 + ";" + value2);

		// remove all test objects
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.when()
			.delete("/TestThree");

	}


	@Test
	public void testArraySearchWithAND() {

		// unified search doesn't work with booleans because we only have two distinct values
		//testArraySearchWithOR("boolean", true, false, true);
		testArraySearchWithANDAndBoolean("boolean", true, false, true);

		//testArraySearchWithAND("byte", (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05);
		testArraySearchWithAND("date", "2019-02-05T12:34:56+0000", "2019-03-05T23:45:00+0000", "2020-11-24T14:15:16+0000");
		testArraySearchWithAND("double", 1.0f, 2.0f, 3.0f);
		testArraySearchWithAND("enum", "Status1", "Status2", "Status3");
		testArraySearchWithAND("integer", 1, 2, 3);
		testArraySearchWithAND("long", 1, 2, 3);
		testArraySearchWithAND("string", "one", "two", "three");
	}

	public void testArraySearchWithANDAndBoolean(final String type, final boolean value1, final boolean value2, final boolean value3) {

		// create test objects
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body("["
				+ "{ 'name': 'test1', '" + type + "ArrayProperty' : [" + join(value1) + "] },"
				+ "{ 'name': 'test2', '" + type + "ArrayProperty' : [" + join(value1, value2) + "] },"
				+ "{ 'name': 'test3', '" + type + "ArrayProperty' : [" + join(value1, value2, value3) + "] }"
				+ "]")
			.expect()
			.statusCode(201)
			.when()
			.post("/TestThree?_sort=name")
			.getHeader("Location");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.body("result_count", equalTo(1))

			.body("result[0]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[0]." + type + "ArrayProperty[1]", equalTo(value2))

			.when()
			.get("/TestThree?_sort=name&" + type + "ArrayProperty=" + value1 + "," + value2);

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.body("result_count", equalTo(1))

			.body("result[0]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[0]." + type + "ArrayProperty[1]", equalTo(value2))
			.body("result[0]." + type + "ArrayProperty[2]", equalTo(value3))

			.when()
			.get("/TestThree?_sort=name&" + type + "ArrayProperty=" + value1 + "," + value2 + "," + value3);

		// remove all test objects
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.when()
			.delete("/TestThree");
	}

	public <T> void testArraySearchWithAND(final String type, final T value1, final T value2, final T value3) {

		// create test objects
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body("["
				+ "{ 'name': 'test1', '" + type + "ArrayProperty' : [" + join(value1) + "] },"
				+ "{ 'name': 'test2', '" + type + "ArrayProperty' : [" + join(value1, value2) + "] },"
				+ "{ 'name': 'test3', '" + type + "ArrayProperty' : [" + join(value1, value2, value3) + "] }"
				+ "]")
		.expect()
			.statusCode(201)
		.when()
			.post("/TestThree?_sort=name")
			.getHeader("Location");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(1))

			.body("result[0]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[0]." + type + "ArrayProperty[1]", equalTo(value2))

		.when()
			.get("/TestThree?_sort=name&" + type + "ArrayProperty=" + value1 + "," + value2);

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(1))

			.body("result[0]." + type + "ArrayProperty[0]", equalTo(value1))
			.body("result[0]." + type + "ArrayProperty[1]", equalTo(value2))
			.body("result[0]." + type + "ArrayProperty[2]", equalTo(value3))

		.when()
			.get("/TestThree?_sort=name&" + type + "ArrayProperty=" + value1 + "," + value2 + "," + value3);

		// remove all test objects
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.when()
			.delete("/TestThree");
	}

	@Test
	public void testArrayPropertyValidation() {

		// unified search doesn't work with booleans because we only have two distinct values
		//testArraySearchWithOR("boolean", true, false, true);
		testArrayPropertyValidation("boolean", true, false, "nope", 123);
		testArrayPropertyValidation("byte", 0, 1, "test");
		testArrayPropertyValidation("date", "2019-02-05T12:34:56+0000", 22, "wrong", "2020-11-24T14:15:16+0000");
		testArrayPropertyValidation("double", 1.0f, 2.0f, "wrong");
		testArrayPropertyValidation("enum", "Status1", "Status2", "NOPE");
		testArrayPropertyValidation("integer", 1, 2, 3, "wrong");
		testArrayPropertyValidation("long", 1, 2, 3, "wrong");
		testArrayPropertyValidation("string", "one", "two", "three", 2, false);
	}

	private void testArrayPropertyValidation(final String type, final Object... values) {

		RestAssured.given()

			.contentType("application/json; charset=UTF-8")
			.body(" { 'longArrayProperty' : [ " + join(values) + " ] } ")
			.expect()
			.statusCode(422)
			.when()
			.post("/TestThree");
	}

	private <T> String join(final T... values) {

		final List<String> list = new LinkedList<>();

		for (final T t : values) {

			if (t instanceof String s)  {

				list.add("\"" + s + "\"");

			} else {

				list.add(t.toString());
			}
		}

		return StringUtils.join(list, ", ");
	}
}
