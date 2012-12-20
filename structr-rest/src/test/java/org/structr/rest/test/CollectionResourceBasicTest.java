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
import net.sf.json.JSONNull;
import org.structr.rest.common.StructrRestTest;
import org.structr.rest.entity.TestObject;
import org.structr.rest.entity.TestOne;

/**
 *
 * @author Christian Morgner
 */
public class CollectionResourceBasicTest extends StructrRestTest {
	
	/**
	 * Test the correct response for a non-existing resource.
	 */
	public void test000NotFoundError() {

		// provoke 404 error with GET on non-existing resource
		RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(404)
			.when()
				.get("/nonexisting_resource");
		    
	}
	
	/**
	 * Test the correct response for an empty (existing) resource.
	 */
	public void test001EmptyResource() {

		// check for empty response
		RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result_count",       equalTo(0))
			.when()
				.get("/test_objects");
		    
	}
	
	/**
	 * Test the creation of a single unnamed entity.
	 */
	public void test010CreateEmptyTestObject() {

		// create empty object
		String location = RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(201)
			.when()
				.post("/test_objects")
				.getHeader("Location");
				
		// POST must return a Location header
		assertNotNull(location);
		
		String uuid = getUuidFromLocation(location);
				
		// POST must create a UUID
		assertNotNull(uuid);
		assertFalse(uuid.isEmpty());
		assertTrue(uuid.matches("[a-f0-9]{32}"));
		
		// check for exactly one object
		Object name = RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result_count",       equalTo(1))
				.body("result[0].id",       equalTo(uuid))
			.when()
				.get("/test_objects")
				.jsonPath().get("result[0].name");
		    
		// name must be null
		assertNull(name);
	}
	
	/**
	 * Test the creation of a single entity with generated UUID and
	 * given name. This method also tests the contents of the JSON
	 * response and reasonable query and serialization time.
	 */
	public void test020CreateNamedTestObject() {

		// create named object
		String location = RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
				.body(" { \"name\" : \"test\" } ")
			.expect()
				.statusCode(201)
			.when()
				.post("/test_objects")
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
				.body("result[0]",          isEntity(TestObject.class))
			.when()
				.get("/test_objects");
		    
	}

}
