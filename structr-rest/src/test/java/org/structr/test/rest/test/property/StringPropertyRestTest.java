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
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.structr.api.DatabaseFeature;
import org.structr.core.Services;
import org.structr.test.rest.common.IndexingTest;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;

/**
 *
 *
 */
public class StringPropertyRestTest extends IndexingTest {

	@Test
	public void testBasics() {

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { 'stringProperty' : 'This is a test!' } ")
		.expect()
			.statusCode(201)
		.when()
			.post("/TestThree")
			.getHeader("Location");



		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result[0].stringProperty", equalTo("This is a test!"))
		.when()
			.get("/TestThree");

	}

	@Test
	public void testSearch() {

		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'stringProperty' : 'test1' } ").expect().statusCode(201).when().post("/TestThree");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'stringProperty' : 'test2' } ").expect().statusCode(201).when().post("/TestThree");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'stringProperty' : 'test3' } ").expect().statusCode(201).when().post("/TestThree");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'name'           : 'test4' } ").expect().statusCode(201).when().post("/TestThree");

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
			.body("result[0].stringProperty", equalTo("test2"))
		.when()
			.get("/test_threes?stringProperty=test2");


		// test loose search
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(3))
		.when()
			.get("/test_threes?stringProperty=test&_loose=1");


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
			.get("/test_threes?stringProperty=[test1 TO test2]");


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
			.body("result[0].name", equalTo("test4"))
		.when()
			.get("/test_threes?stringProperty=");

	}

	/*@Test*/
	public void testLargeStrings() {

		// this test needs the schema index on string properties to be present
		// it is disabled for now since index updates are disabled during test execution

		// This test is designed to fail if the actual indexable size of a string in the database changes
		final boolean supportsLargeStrings = Services.getInstance().getDatabaseService().supportsFeature(DatabaseFeature.LargeStringIndexing);
		final int errorStatusCode          = supportsLargeStrings ? 201 : 422;

		testLargeString(4000, 201);
		testLargeString(4032, 201);
		testLargeString(4033, 201);
		testLargeString(4034, 201);
		testLargeString(4035, 201);
		testLargeString(4036, 201);
		testLargeString(4037, errorStatusCode);
		testLargeString(4038, errorStatusCode);
		testLargeString(4039, errorStatusCode);
		testLargeString(4040, errorStatusCode);
		testLargeString(4100, errorStatusCode);
		testLargeString(5000, errorStatusCode);
	}

	private void testLargeString(final int length, final int expectedStatusCode) {

		final StringBuilder buf = new StringBuilder();

		for (int i=0; i<length; i++) {
			buf.append("0");
		}

		String largeString = buf.toString();

		// cleanup
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(RequestLoggingFilter.logRequestTo(System.out))
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
		.expect()
			.statusCode(200)
		.when()
			.delete("/TestThree");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(RequestLoggingFilter.logRequestTo(System.out))
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.body(" { 'name': '" + length + "', 'stringProperty' : '" + largeString + "' } ")
		.expect()
			.statusCode(expectedStatusCode)
		.when()
			.post("/TestThree")
			.getHeader("Location");

		if (expectedStatusCode == 201) {

			RestAssured.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
				.statusCode(200)
				.body("result[0].stringProperty", equalTo(largeString))
			.when()
				.get("/TestThree");
		}
	}
}
