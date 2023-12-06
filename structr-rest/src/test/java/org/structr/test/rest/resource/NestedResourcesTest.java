/*
 * Copyright (C) 2010-2023 Structr GmbH
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
	 * For the class TestTwo, the following notation is allowed:
	 *
	 *    /TestTwo
	 *
	 */
	@Test
	public void test010EndpointNotations() {

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

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result_count",       equalTo(1))
				.body("result.id",          equalTo(testTwo))
			.when()
				.get("/TestTwo/" + testTwo);

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result_count",       equalTo(1))
				.body("result.id",          equalTo(testTwo))
			.when()
				.get("/TestTwo/" + testTwo);

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
	 * Test nested attribute "test_ones"
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
				.body("result_count",       equalTo(1))
				.body("result[0].id",          equalTo(testOne))
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
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.expect()
				.statusCode(200)
			.when()
				.get("/TestTwo");


	}
}
