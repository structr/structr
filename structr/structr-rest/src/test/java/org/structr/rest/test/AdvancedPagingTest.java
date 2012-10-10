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
public class AdvancedPagingTest extends StructrRestTest {

	
	/**
	 * Paging of subresources
	 */
	public void test01PagingOfSubresources() {

		// create a root object
		
		String resource = "/test_two";
		
		String location = RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestTwo-0', 'anInt' : 0, 'aLong' : 0, 'aDate' : '2012-09-18T00:33:12+0200' } ")
			.expect().statusCode(201).when().post(resource).getHeader("Location");
		
		String baseId = getUuidFromLocation(location);
		
		resource = resource.concat("/").concat(baseId).concat("/test_one");
		
		String offsetId = null;
		
		// create sub objects
		for (int i=0; i<10; i++) {
			
			location = RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestOne-" + i + "', 'anInt' : " + i + ", 'aLong' : " + i + ", 'aDate' : '2012-09-18T0" + i + ":33:12+0200' } ")
			.expect().statusCode(201).when().post(resource).getHeader("Location");
			
			String id = getUuidFromLocation(location);
			
			if (i == 3) {
				offsetId = id;
			}
			
			System.out.println("Object created: " + id);
			
		}
		
		System.out.println("Offset ID: " + offsetId);
		
		resource = "/test_two/" + baseId + "/test_one";
		
		for (int page=1; page<5; page++) {
			
			String url = resource + "?sort=name&pageSize=2&page=" + page;
			
			System.out.println("Testing page " + page + " with URL " + url);
		
			RestAssured

				.given()
					.contentType("application/json; charset=UTF-8")
				.expect()
					.statusCode(200)
					.body("result",			hasSize(2))
					.body("result_count",		equalTo(10))

					.body("result[0]",		isEntity(TestOne.class))
					.body("result[0].name ",	equalTo("TestOne-" + ((2*page)-2)))

					.body("result[1]",		isEntity(TestOne.class))
					.body("result[1].name ",	equalTo("TestOne-" + ((2*page)-1)))

				.when()
					.get(url);		
		
		}
		
		RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result",			hasSize(2))
				.body("result_count",		equalTo(10))

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
				.body("result_count",		equalTo(10))

				.body("result[0]",		isEntity(TestOne.class))
				.body("result[0].name ",	equalTo("TestOne-1"))

				.body("result[1]",		isEntity(TestOne.class))
				.body("result[1].name ",	equalTo("TestOne-2"))

			.when()
				.get(resource + "?sort=name&pageSize=2&page=-1&pageStartId=" + offsetId);		

		
		// with empty pageSize: Should start at offset element (index 3) and return 0, 1, 2
		
		RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result",			hasSize(3))
				.body("result_count",		equalTo(10))

				.body("result[0]",		isEntity(TestOne.class))
				.body("result[0].name ",	equalTo("TestOne-0"))

				.body("result[1]",		isEntity(TestOne.class))
				.body("result[1].name ",	equalTo("TestOne-1"))

				.body("result[2]",		isEntity(TestOne.class))
				.body("result[2].name ",	equalTo("TestOne-2"))

			.when()
				.get(resource + "?sort=name&page=-1&pageStartId=" + offsetId);		
		
		
	}
	
	
		
}
