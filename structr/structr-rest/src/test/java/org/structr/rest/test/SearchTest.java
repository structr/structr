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
package org.structr.rest.test;

import static org.hamcrest.Matchers.*;
import com.jayway.restassured.RestAssured;
import org.structr.rest.common.StructrRestTest;
import org.structr.rest.entity.TestOne;

/**
 *
 * @author Axel Morgner
 */
public class SearchTest extends StructrRestTest {

	/**
	 * Test a more complex object
	 */
	public void test01CreateTestOne() {

		// create named object
		String location = RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
				.body(" { 'name' : 'a TestOne object', 'anInt' : 123, 'aLong' : 234, 'aDate' : '2012-09-18T06:33:12+0200' } ")
			.expect()
				.statusCode(201)
			.when()
				.post("/test_one")
				.getHeader("Location");
		    
		// POST must return a Location header
		assertNotNull(location);
		
		String uuid = getUuidFromLocation(location);
				
		// POST must create a UUID
		assertNotNull(uuid);
		assertFalse(uuid.isEmpty());
		assertTrue(uuid.matches("[a-f0-9]{32}"));

		// check for exactly one object
		RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result_count",       equalTo(1))
				.body("query_time",         lessThan("0.1"))
				.body("serialization_time", lessThan("0.01"))
				.body("result[0]",          isEntity(TestOne.class))
			.when()
				.get("/test_one");
		    
	}


	/**
	 * Test a more complex object
	 */
	public void test02SearchTestOne() {

		// create some objects
		
		String resource = "/test_one";
		
		RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestOne-3', 'anInt' : 3, 'aLong' : 30, 'aDate' : '2012-09-18T03:33:12+0200' } ")
			.expect().statusCode(201).when().post(resource).getHeader("Location");

		RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestOne-1', 'anInt' : 2, 'aLong' : 10, 'aDate' : '2012-09-18T01:33:12+0200' } ")
			.expect().statusCode(201).when().post(resource).getHeader("Location");
		
		RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestOne-4', 'anInt' : 4, 'aLong' : 40, 'aDate' : '2012-09-18T04:33:12+0200' } ")
			.expect().statusCode(201).when().post(resource).getHeader("Location");

		RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestOne-2', 'anInt' : 1, 'aLong' : 20, 'aDate' : '2012-09-18T02:33:12+0200' } ")
			.expect().statusCode(201).when().post(resource).getHeader("Location");
		
		// sort by name
		RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result_count",       equalTo(4))
//				.body("query_time",         lessThan("0.1"))
//				.body("serialization_time", lessThan("0.01"))

				.body("result[0]",          isEntity(TestOne.class))
				.body("result[0].aLong",    equalTo(10))
				.body("result[0].aDate",    equalTo("2012-09-18T01:33:12+0200"))

				.body("result[1]",          isEntity(TestOne.class))
				.body("result[1].aLong",    equalTo(20))
				.body("result[1].aDate",    equalTo("2012-09-18T02:33:12+0200"))

				.body("result[2]",          isEntity(TestOne.class))
				.body("result[2].aLong",    equalTo(30))
				.body("result[2].aDate",    equalTo("2012-09-18T03:33:12+0200"))

				.body("result[3]",          isEntity(TestOne.class))
				.body("result[3].aLong",    equalTo(40))
				.body("result[3].aDate",    equalTo("2012-09-18T04:33:12+0200"))

			.when()
				.get(resource + "?sort=name&pageSize=10&page=1");
		    

		// sort by date, descending
		RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result_count",       equalTo(4))
//				.body("query_time",         lessThan("0.1"))
//				.body("serialization_time", lessThan("0.01"))

				.body("result[0]",          isEntity(TestOne.class))
				.body("result[0].aLong",    equalTo(40))
				.body("result[0].aDate",    equalTo("2012-09-18T04:33:12+0200"))

				.body("result[1]",          isEntity(TestOne.class))
				.body("result[1].aLong",    equalTo(30))
				.body("result[1].aDate",    equalTo("2012-09-18T03:33:12+0200"))

				.body("result[2]",          isEntity(TestOne.class))
				.body("result[2].aLong",    equalTo(20))
				.body("result[2].aDate",    equalTo("2012-09-18T02:33:12+0200"))

				.body("result[3]",          isEntity(TestOne.class))
				.body("result[3].aLong",    equalTo(10))
				.body("result[3].aDate",    equalTo("2012-09-18T01:33:12+0200"))

			.when()
				.get(resource + "?sort=aDate&order=desc&pageSize=10&page=1");
	
		// sort by int
		RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result_count",       equalTo(4))
//				.body("query_time",         lessThan("0.1"))
//				.body("serialization_time", lessThan("0.01"))

				.body("result[0]",          isEntity(TestOne.class))
				.body("result[0].aLong",    equalTo(20))

				.body("result[1]",          isEntity(TestOne.class))
				.body("result[1].aLong",    equalTo(10))

				.body("result[2]",          isEntity(TestOne.class))
				.body("result[2].aLong",    equalTo(30))

				.body("result[3]",          isEntity(TestOne.class))
				.body("result[3].aLong",    equalTo(40))

			.when()
				.get(resource + "?sort=anInt&pageSize=10&page=1");
	
	}
	
	
}
