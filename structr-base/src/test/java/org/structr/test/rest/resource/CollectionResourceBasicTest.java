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
package org.structr.test.rest.resource;

import io.restassured.RestAssured;
import io.restassured.response.ResponseBody;
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.testng.AssertJUnit.*;

/**
 *
 *
 */
public class CollectionResourceBasicTest extends StructrRestTestBase {

	/**
	 * Test the correct response for a non-existing resource.
	 */
	@Test
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
	@Test
	public void test001EmptyResource() {

		// check for empty response
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result_count",       equalTo(0))
			.when()
				.get("/TestObject");

	}

	/**
	 * Test the creation of a single unnamed entity.
	 */
	@Test
	public void test010CreateEmptyTestObject() {

		// create empty object
		String location = RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(201)
			.when()
				.post("/TestObject")
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
				.get("/TestObject")
				.jsonPath().get("result[0].name");

		// name must be null
		assertNull(name);
	}

	/**
	 * Test the creation of a single entity with generated UUID and
	 * given name. This method also tests the contents of the JSON
	 * response and reasonable query and serialization time.
	 */
	@Test
	public void test020CreateNamedTestObject() {

		// create named object
		String location = RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.body(" { \"name\" : \"test\" } ")
			.expect()
				.statusCode(201)
			.when()
				.post("/TestObject")
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
				.body("result[0]",          isEntity("TestObject"))
			.when()
				.get("/TestObject");

	}

	@Test
	public void testOrderOfIDsWhenPostingMultipleObjects() {

		// make sure that the order of UUIDs returned by a POST
		// request matches the order of posted objects

		final ResponseBody response = RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.body("[ { name: group4 }, { name: group2 }, { name: group3 }, { name: group5 }, { name: group1 } ]")

			.expect()
				.statusCode(201)

			.when()
				.post("/Group")
				.getBody();

		// collect results exactly as ordered in POST data (4, 2, 3, 5, 1)
		final String id4 = response.jsonPath().getString("result[0]");
		final String id2 = response.jsonPath().getString("result[1]");
		final String id3 = response.jsonPath().getString("result[2]");
		final String id5 = response.jsonPath().getString("result[3]");
		final String id1 = response.jsonPath().getString("result[4]");

		final String ct  = "application/json; charset=UTF-8";
		final String rc  = "result_count";
		final String rn  = "result.name";
		final String ri  = "result.id";

		// check for correct assignment of id and name
		RestAssured.given().contentType(ct).expect().statusCode(200).body(rc, equalTo(1)).body(rn, equalTo("group1")).body(ri, equalTo(id1)).when().get("/Group/" + id1);
		RestAssured.given().contentType(ct).expect().statusCode(200).body(rc, equalTo(1)).body(rn, equalTo("group2")).body(ri, equalTo(id2)).when().get("/Group/" + id2);
		RestAssured.given().contentType(ct).expect().statusCode(200).body(rc, equalTo(1)).body(rn, equalTo("group3")).body(ri, equalTo(id3)).when().get("/Group/" + id3);
		RestAssured.given().contentType(ct).expect().statusCode(200).body(rc, equalTo(1)).body(rn, equalTo("group4")).body(ri, equalTo(id4)).when().get("/Group/" + id4);
		RestAssured.given().contentType(ct).expect().statusCode(200).body(rc, equalTo(1)).body(rn, equalTo("group5")).body(ri, equalTo(id5)).when().get("/Group/" + id5);
	}
}
