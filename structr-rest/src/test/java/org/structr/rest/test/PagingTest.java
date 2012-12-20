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
import com.jayway.restassured.internal.RestAssuredResponseImpl;
import org.structr.rest.common.StructrRestTest;
import org.structr.rest.entity.TestOne;

/**
 *
 * @author Axel Morgner
 */
public class PagingTest extends StructrRestTest {

	/**
	 * Test paging
	 */
	public void test01Paging() {

		// create some objects
		
		String resource = "/test_one";
		
		RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestOne-0', 'anInt' : 0, 'aLong' : 0, 'aDate' : '2012-09-18T00:33:12+0200' } ")
			.expect().statusCode(201).when().post(resource).getHeader("Location");
		
		RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestOne-1', 'anInt' : 1, 'aLong' : 10, 'aDate' : '2012-09-18T01:33:12+0200' } ")
			.expect().statusCode(201).when().post(resource).getHeader("Location");

		RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestOne-2', 'anInt' : 2, 'aLong' : 20, 'aDate' : '2012-09-18T02:33:12+0200' } ")
			.expect().statusCode(201).when().post(resource).getHeader("Location");
		
		String location = RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestOne-3', 'anInt' : 3, 'aLong' : 30, 'aDate' : '2012-09-18T03:33:12+0200' } ")
			.expect().statusCode(201).when().post(resource).getHeader("Location");
		
		String offsetId = getUuidFromLocation(location);

		RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestOne-4', 'anInt' : 4, 'aLong' : 40, 'aDate' : '2012-09-18T04:33:12+0200' } ")
			.expect().statusCode(201).when().post(resource).getHeader("Location");

		RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestOne-5', 'anInt' : 5, 'aLong' : 50, 'aDate' : '2012-09-18T05:33:12+0200' } ")
			.expect().statusCode(201).when().post(resource).getHeader("Location");

		RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestOne-6', 'anInt' : 6, 'aLong' : 60, 'aDate' : '2012-09-18T06:33:12+0200' } ")
			.expect().statusCode(201).when().post(resource).getHeader("Location");

		RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestOne-7', 'anInt' : 7, 'aLong' : 70, 'aDate' : '2012-09-18T07:33:12+0200' } ")
			.expect().statusCode(201).when().post(resource).getHeader("Location");

		
		Object result = RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result",			hasSize(2))
				.body("result_count",		equalTo(8))

				.body("result[0]",		isEntity(TestOne.class))
				.body("result[0].name ",	equalTo("TestOne-0"))

				.body("result[1]",		isEntity(TestOne.class))
				.body("result[1].name ",	equalTo("TestOne-1"))

			.when()
				.get(resource + "?sort=name&pageSize=2&page=1");
		
		//System.out.println("result: " + ((RestAssuredResponseImpl) result).prettyPrint());
		
		    
		RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result",			hasSize(2))
				.body("result_count",		equalTo(8))

				.body("result[0]",		isEntity(TestOne.class))
				.body("result[0].name ",	equalTo("TestOne-6"))

				.body("result[1]",		isEntity(TestOne.class))
				.body("result[1].name ",	equalTo("TestOne-7"))

			.when()
				.get(resource + "?sort=name&pageSize=2&page=-1");

		RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result",			hasSize(2))
				.body("result_count",		equalTo(8))

				.body("result[0]",		isEntity(TestOne.class))
				.body("result[0].name ",	equalTo("TestOne-4"))

				.body("result[1]",		isEntity(TestOne.class))
				.body("result[1].name ",	equalTo("TestOne-5"))

			.when()
				.get(resource + "?sort=name&pageSize=2&page=-2");

		RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result",			hasSize(2))
				.body("result_count",		equalTo(8))

				.body("result[0]",		isEntity(TestOne.class))
				.body("result[0].name ",	equalTo("TestOne-2"))

				.body("result[1]",		isEntity(TestOne.class))
				.body("result[1].name ",	equalTo("TestOne-3"))

			.when()
				.get(resource + "?sort=name&pageSize=2&page=-3");

		RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result",			hasSize(2))
				.body("result_count",		equalTo(8))

				.body("result[0]",		isEntity(TestOne.class))
				.body("result[0].name ",	equalTo("TestOne-0"))

				.body("result[1]",		isEntity(TestOne.class))
				.body("result[1].name ",	equalTo("TestOne-1"))

			.when()
				.get(resource + "?sort=name&pageSize=2&page=-4");
		
		RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result",			hasSize(2))
				.body("result_count",		equalTo(8))

				.body("result[0]",		isEntity(TestOne.class))
				.body("result[0].name ",	equalTo("TestOne-0"))

				.body("result[1]",		isEntity(TestOne.class))
				.body("result[1].name ",	equalTo("TestOne-1"))

			.when()
				.get(resource + "?sort=name&pageSize=2&page=-5");

		RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result",			hasSize(2))
				.body("result_count",		equalTo(8))

				.body("result[0]",		isEntity(TestOne.class))
				.body("result[0].name ",	equalTo("TestOne-3"))

				.body("result[1]",		isEntity(TestOne.class))
				.body("result[1].name ",	equalTo("TestOne-4"))

			.when()
				.get(resource + "?sort=name&pageSize=2&page=1&pageStartId=" + offsetId);
	
		RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result",			hasSize(2))
				.body("result_count",		equalTo(8))

				.body("result[0]",		isEntity(TestOne.class))
				.body("result[0].name ",	equalTo("TestOne-1"))

				.body("result[1]",		isEntity(TestOne.class))
				.body("result[1].name ",	equalTo("TestOne-2"))

			.when()
				.get(resource + "?sort=name&pageSize=2&page=-1&pageStartId=" + offsetId);
	
	}
		
}
