/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.test.property;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import com.jayway.restassured.response.Response;
import org.structr.rest.common.StructrRestTest;

import static org.hamcrest.Matchers.*;

/**
 *
 * @author Christian Morgner
 */
public class DoublePropertyRestTest extends StructrRestTest {
	
	public void testBasics() {
		
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { 'doubleProperty' : 3.141592653589793238 } ")
		.expect()
			.statusCode(201)
		.when()
			.post("/test_threes")
			.getHeader("Location");
		
		
		
		Response response = RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
		.when()
			.get("/test_threes");
		
		// FIXME: due to a bug in RestAssured/Groovy, the double value is truncated to a float.
		//        The JSON result contains the correct (full) value, so we just ignore the wrong
		//        result from RestAssured here..
		// assertEquals("3.141592653589793238", response.getBody().jsonPath().getDouble("result[0].doubleProperty"));
		
		assertEquals(3.1415927, response.getBody().jsonPath().getDouble("result[0].doubleProperty"));
		
	}
	
	public void testSearch() {

		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'doubleProperty' : 1.2 } ").expect().statusCode(201).when().post("/test_threes");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'doubleProperty' : 2.3 } ").expect().statusCode(201).when().post("/test_threes");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'doubleProperty' : 3.4 } ").expect().statusCode(201).when().post("/test_threes");
		
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
			.body("result_count", equalTo(3))
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
			.body("result[0].doubleProperty", equalTo(2.3f))
		.when()
			.get("/test_threes?doubleProperty=2.3");
		
		
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
			.get("/test_threes?doubleProperty=[1.1 TO 2.4]");
	
	}
}
