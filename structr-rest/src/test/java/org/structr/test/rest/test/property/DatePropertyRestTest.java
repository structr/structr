/*
 * Copyright (C) 2010-2021 Structr GmbH
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

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import static org.hamcrest.Matchers.*;
import org.testng.annotations.Test;
import org.structr.test.rest.common.StructrRestTestBase;

/**
 *
 *
 */
public class DatePropertyRestTest extends StructrRestTestBase {

	@Test
	public void testBasics() {

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { 'dateProperty' : '2013-04-05T10:43:40+0200' } ")
		.expect()
			.statusCode(201)
		.when()
			.post("/test_threes")
			.getHeader("Location");



		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result[0].dateProperty", equalTo("2013-04-05T08:43:40+0000"))
		.when()
			.get("/test_threes");

	}

	@Test
	public void testSearch() {

		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'dateProperty' : '2013-04-03T10:34:56+0000' } ").expect().statusCode(201).when().post("/test_threes");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'dateProperty' : '2013-04-05T08:43:40+0000' } ").expect().statusCode(201).when().post("/test_threes");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'dateProperty' : '2013-04-07T08:43:40+0000' } ").expect().statusCode(201).when().post("/test_threes");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'name'         : 'test'                     } ").expect().statusCode(201).when().post("/test_threes");

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
			.get("/test_threes");

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
			.body("result[0].dateProperty", equalTo("2013-04-03T10:34:56+0000"))
		.when()
			.get("/test_threes?dateProperty=2013-04-03T10:34:56+0000");

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
			.get("/test_threes?dateProperty=");
	}

	@Test
	public void testRangeSearch1() {

		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'dateProperty' : '2013-04-03T10:34:56+0000' } ").expect().statusCode(201).when().post("/test_threes");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'dateProperty' : '2013-04-05T08:43:40+0000' } ").expect().statusCode(201).when().post("/test_threes");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'dateProperty' : '2013-04-07T08:43:40+0000' } ").expect().statusCode(201).when().post("/test_threes");

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
			.get("/test_threes?dateProperty=[2013-04-03T10:34:56+0000 TO 2013-04-06T23:59:59+0000]");

	}

	@Test
	public void testRangeSearch2() {

		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'dateProperty' : '2013-04-03T10:34:56+0000' } ").expect().statusCode(201).when().post("/test_threes");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'dateProperty' : '2013-04-05T08:43:40+0000' } ").expect().statusCode(201).when().post("/test_threes");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'dateProperty' : '2013-04-07T08:43:40+0000' } ").expect().statusCode(201).when().post("/test_threes");

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
			.get("/test_threes?dateProperty=[2013-04-01T00:00:00+0000 TO 2013-04-06T23:59:59+0000]");

	}
}
