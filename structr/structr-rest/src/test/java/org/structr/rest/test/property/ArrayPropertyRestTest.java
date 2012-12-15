/*
 *  Copyright (C) 2012 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
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
		
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { 'stringArrayProperty' : ['one', 'two', 'three', 'four', 'five'] } ")
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
			.body("result[0].stringArrayProperty[0]", equalTo("one"))
			.body("result[0].stringArrayProperty[1]", equalTo("two"))
			.body("result[0].stringArrayProperty[2]", equalTo("three"))
			.body("result[0].stringArrayProperty[3]", equalTo("four"))
			.body("result[0].stringArrayProperty[4]", equalTo("five"))
		.when()
			.get("/test_threes");
		
		
		
	}
}
