/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.testng.AssertJUnit.assertNotNull;

/**
 *
 *
 */
public class NestedResourcesTest extends StructrRestTestBase {

	/**
	 * Test the correct response for a non-existing nested resource (404)
	 */
	@Test
	public void test000NotFoundError() {

		// create empty object
		String uuid = createEntity("/TestObject", "{}");

		// provoke 404 error with GET on non-existing resource
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(404)
			.when()
				.get("/TestObject/" + uuid + "/nonexisting");

	}

	/**
	 * Test different notations of the REST endpoint (first URI part).
	 *
	 * For the class TestTwo, the following notations are allowed:
	 *
	 *    /TestTwo
	 *
	 */
	@Test
	public void test010EndpointNotations() {

		// Note: this test has become obsolete because we don't allow pluralized
		// resource names any more (e.g. test_twos is not allowed for TestTwo!)

		String testOne = createEntity("/TestOne", "{ \"name\": \"TestOne\" }");
		assertNotNull(testOne);

		System.out.println(testOne);

		String body = "{ \"test_ones\": [ \"" + testOne + "\" ] }";
		System.out.println(body);

		String testTwo = createEntity("/TestTwo", body);
		assertNotNull(testTwo);

		System.out.println(testTwo);

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result_count",       equalTo(1))
				.body("result.id",          equalTo(testTwo))
			.when()
				.get("/TestTwo/" + testTwo);
	}

	/**
	 * Test nested attribute "TestOne"
	 *
	 */
	@Test
	public void test020EndpointNotations() {

		String testOne = createEntity("/TestOne", "{ \"name\": \"TestOne\" }");
		assertNotNull(testOne);

		System.out.println(testOne);

		String body = "{ \"test_ones\": [ \"" + testOne + "\" ] }";
		System.out.println(body);

		String testTwo = createEntity("/TestTwo", body);
		assertNotNull(testTwo);

		System.out.println(testTwo);

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result_count",  equalTo(1))
				.body("result[0].id",  equalTo(testOne))
			.when()
				.get("/TestTwo/" + testTwo + "/test_ones")
			.prettyPrint();

	}

	/**
	 * Test nested attribute "testOnes"
	 *
	 */
	@Test
	public void test030EndpointNotations() {

		String testOne = createEntity("/TestOne", "{ \"name\": \"TestOne\" }");
		assertNotNull(testOne);

		System.out.println(testOne);

		String body = "{ \"testOnes\": [ \"" + testOne + "\" ] }";
		System.out.println(body);

		String testTwo = createEntity("/TestTwo", body);
		assertNotNull(testTwo);

		System.out.println(testTwo);

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result_count",       equalTo(1))
				.body("result[0].id",       equalTo(testOne))
			.when()
				.get("/TestTwo/" + testTwo + "/testOnes");
	}

	@Test
	public void testPostExistingObjectToRelationProperty() {

		// PropertyResource.doPost() applies the relation property's notion when the POSTed property set
		// consists of the notion's primary property key alone. Relation properties use ObjectNotion by
		// default, whose primary key is "id", so { "id": ... } links the existing object identified by it
		// to the object addressed by the URL, instead of creating a new one.

		final String testOne = createEntity("/TestOne", "{ name: 'existing' }");
		final String testTwo = createEntity("/TestTwo", "{ }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.body(" { \"id\": \"" + testOne + "\" } ")
			.expect()
				.statusCode(200)
			.when()
				.post("/TestTwo/" + testTwo + "/testOnes");

		// the existing object is linked, and no second one was created
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result_count",   equalTo(1))
				.body("result[0].id",   equalTo(testOne))
				.body("result[0].name", equalTo("existing"))
			.when()
				.get("/TestTwo/" + testTwo + "/testOnes");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result_count", equalTo(1))
			.when()
				.get("/TestOne");

		// a property set that is not the primary key alone still creates a new node and links it
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.body(" { \"name\": \"created-and-linked\" } ")
			.expect()
				.statusCode(201)
			.when()
				.post("/TestTwo/" + testTwo + "/testOnes");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result_count", equalTo(2))
			.when()
				.get("/TestTwo/" + testTwo + "/testOnes");
	}

	@Test
	public void testPostExistingObjectToRelationPropertyIsIdempotent() {

		// Linking an existing object is idempotent: repeating the request must not add a second
		// relationship between the same two objects.

		final String testOne      = createEntity("/TestOne", "{ }");
		final String otherTestOne = createEntity("/TestOne", "{ }");
		final String testTwo      = createEntity("/TestTwo", "{ }");

		for (int i = 0; i < 3; i++) {

			RestAssured

				.given()
					.contentType("application/json; charset=UTF-8")
					.body(" { \"id\": \"" + testOne + "\" } ")
				.expect()
					.statusCode(200)
				.when()
					.post("/TestTwo/" + testTwo + "/testOnes");
		}

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result_count", equalTo(1))
				.body("result[0].id", equalTo(testOne))
			.when()
				.get("/TestTwo/" + testTwo + "/testOnes");

		// only one relationship exists between the two objects
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result_count", equalTo(1))
			.when()
				.get("/TwoOneOneToMany");

		// a different object still gets linked, i.e. the check does not skip new links
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.body(" { \"id\": \"" + otherTestOne + "\" } ")
			.expect()
				.statusCode(200)
			.when()
				.post("/TestTwo/" + testTwo + "/testOnes");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result_count", equalTo(2))
			.when()
				.get("/TestTwo/" + testTwo + "/testOnes");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result_count", equalTo(2))
			.when()
				.get("/TwoOneOneToMany");
	}

	@Test
	public void testPostExistingObjectToSingleValuedRelationProperty() {

		// The same for a single-valued relation property: repeating the request changes nothing, and a
		// different object replaces the link (the property can only hold one).

		final String testEleven      = createEntity("/TestEleven", "{ }");
		final String otherTestEleven = createEntity("/TestEleven", "{ }");
		final String testTwo         = createEntity("/TestTwo", "{ }");

		for (int i = 0; i < 3; i++) {

			RestAssured

				.given()
					.contentType("application/json; charset=UTF-8")
					.body(" { \"id\": \"" + testEleven + "\" } ")
				.expect()
					.statusCode(200)
				.when()
					.post("/TestTwo/" + testTwo + "/testEleven");
		}

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result_count", equalTo(1))
			.when()
				.get("/ElevenTwoOneToMany");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.body(" { \"id\": \"" + otherTestEleven + "\" } ")
			.expect()
				.statusCode(200)
			.when()
				.post("/TestTwo/" + testTwo + "/testEleven");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result_count", equalTo(1))
				.body("result.id",    equalTo(otherTestEleven))
			.when()
				.get("/TestTwo/" + testTwo + "/testEleven");
	}

	@Test
	public void testPostUnknownIdToRelationProperty() {

		// an identifier the notion cannot resolve must be reported, not silently ignored
		final String testTwo = createEntity("/TestTwo", "{ }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.body(" { \"id\": \"01234567890123456789012345678901\" } ")
			.expect()
				.statusCode(422)
			.when()
				.post("/TestTwo/" + testTwo + "/testOnes");

		// a syntactically valid id of the wrong type must be reported as well
		final String otherType = createEntity("/TestThree", "{ }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.body(" { \"id\": \"" + otherType + "\" } ")
			.expect()
				.statusCode(422)
			.when()
				.post("/TestTwo/" + testTwo + "/testOnes");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result_count", equalTo(0))
			.when()
				.get("/TestTwo/" + testTwo + "/testOnes");
	}

	@Test
	public void testSimpleOneToMany() {

		final StringBuilder buf = new StringBuilder();

		buf.append("\"");
		buf.append(createEntity("/TestOne", "{ }"));
		buf.append("\", \"");
		buf.append(createEntity("/TestOne", "{ }"));
		buf.append("\",\"");
		buf.append(createEntity("/TestOne", "{ }"));
		buf.append("\",\"");
		buf.append(createEntity("/TestOne", "{ }"));
		buf.append("\",\"");
		buf.append(createEntity("/TestOne", "{ }"));
		buf.append("\",\"");
		buf.append(createEntity("/TestOne", "{ }"));
		buf.append("\"");

		createEntity("/TestTwo","{ }");
		createEntity("/TestTwo","{ }");
		createEntity("/TestTwo","{ testOnes: [ " + buf.toString() + " ] }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
			.when()
				.get("/TestTwo");

	}
}
