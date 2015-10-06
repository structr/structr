/**
 * Copyright (C) 2010-2015 Structr GmbH
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
package org.structr.rest.test.property;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import org.structr.rest.common.StructrRestTest;
import static org.hamcrest.Matchers.*;

/**
 *
 * @author Christian Morgner
 */
public class ArrayPropertyRestTest extends StructrRestTest {

	public void testViaRest() {

		String location = RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { 'stringArrayProperty' : ['one', 'two', 'three', 'four', 'five'] } ")
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
			.body("result[0].stringArrayProperty[0]", equalTo("one"))
			.body("result[0].stringArrayProperty[1]", equalTo("two"))
			.body("result[0].stringArrayProperty[2]", equalTo("three"))
			.body("result[0].stringArrayProperty[3]", equalTo("four"))
			.body("result[0].stringArrayProperty[4]", equalTo("five"))
		.when()
			.get("/test_threes");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { 'stringArrayProperty' : ['three', 'four', 'five'] } ")
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
			.body("result[0].stringArrayProperty[0]", equalTo("three"))
			.body("result[0].stringArrayProperty[1]", equalTo("four"))
			.body("result[0].stringArrayProperty[2]", equalTo("five"))
		.when()
			.get("/test_threes");
	}

	public void testSearch() {

		// create test objects
		final String id1 = getUuidFromLocation(RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { 'stringArrayProperty' : ['one', 'two', 'three', 'four', 'five'] } ")
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
			.get("/test_threes?stringArrayProperty=");


		// test search for empty array property
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
		.expect()
			.statusCode(200)
			.body("result[0].id", equalTo(id1))
		.when()
			.get("/test_threes?stringArrayProperty=[]");



	}
}
