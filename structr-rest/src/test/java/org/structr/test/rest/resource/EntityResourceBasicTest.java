/*
 * Copyright (C) 2010-2024 Structr GmbH
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
import io.restassured.filter.log.ResponseLoggingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.test.rest.common.StructrRestTestBase;
import org.structr.test.rest.entity.TestObject;
import org.structr.test.rest.entity.TestOne;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.testng.AssertJUnit.*;

/**
 *
 *
 */
public class EntityResourceBasicTest extends StructrRestTestBase {

	private static final Logger logger = LoggerFactory.getLogger(EntityResourceBasicTest.class.getName());

	@Test
	public void testInvokeMethodResult() {

		String id = null;

		try (final Tx tx = app.tx()) {

			final TestOne test = app.create(TestOne.class);

			// store ID for later use
			id = test.getUuid();

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// execute test method, expect sane result (not 500)
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.when()
			.post(concat("/TestOne/", id, "/test01"));


		// execute test method, expect sane result (not 500)
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.when()
			.post(concat("/TestOne/", id, "/test02"));


		// execute test method, expect sane result (not 500)
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.when()
			.post(concat("/TestOne/", id, "/test03"));


		// execute test method, expect sane result (not 500)
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.when()
			.post(concat("/TestOne/", id, "/test04"));

	}

	/**
	 * Test the correct response for a non-existing entity
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
				.get("/TestObject/abc123def456abc123def456abc123de");

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
				.body("query_time",         lessThan("0.1"))
				.body("serialization_time", lessThan("0.02"))
				.body("result.id",          equalTo(uuid))
			.when()
				.get("/TestObject/" + uuid)
				.jsonPath().get("result.name");

		System.out.println("name (should be null): " + name);

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
				.body("result_count",		equalTo(1))
				.body("query_time",		lessThan("0.5"))
				.body("serialization_time",	lessThan("0.05"))
				.body("result",			isEntity(TestObject.class))
			.when()
				.get("/TestObject/" + uuid);

	}

	@Test
	public void test03PutWithExistingUuid() {

		// create object
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

		// test put with existing UUID (should work)
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.body("{name: modified, id: " + uuid + "}")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.expect()
				.statusCode(200)
			.when()
				.put("/TestObject/" + uuid);

		// check for modified name
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.expect()
				.statusCode(200)
				.body("result_count",		equalTo(1))
				.body("query_time",		lessThan("0.5"))
				.body("serialization_time",	lessThan("0.05"))
				.body("result",			isEntity(TestObject.class))
				.body("result.name",            equalTo("modified"))
			.when()
				.get("/TestObject/" + uuid);

	}
}
