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
package org.structr.test.rest.test.property;

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.hasItems;

/**
 *
 *
 */
public class CollectionPropertyTest extends StructrRestTestBase {

	@Test
	public void testManyToMany() throws Exception {

		String[] testOneIds = new String[3];

		for (int i=0; i<3; i++) {

			String location = RestAssured.given()
				.contentType("application/json; charset=UTF-8")
				//.body(" { 'name' : 'TestOne-'" + i + "' } ")
			.expect()
				.statusCode(201)
			.when()
				.post("/test_ones")
				.getHeader("Location");

			testOneIds[i] = getUuidFromLocation(location);

		}

		// POST to create a TestFive object with relationships to the three TestOnes
		String location = RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { 'manyToManyTestOnes' : ['" + testOneIds[0] + "', '" + testOneIds[1] + "', '" + testOneIds[2] + "'] } ")
		.expect()
			.statusCode(201)
		.when()
			.post("/test_fives")
			.getHeader("Location");

		String testFiveId = getUuidFromLocation(location);

		System.out.println("ID of TestFive node: " + testFiveId);

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
		.expect()
			.statusCode(200)
			.body("result.manyToManyTestOnes", hasItems(testOneIds[0], testOneIds[1], testOneIds[2]))
		.when()
			.get("/test_fives/" + testFiveId);


	}




	/**
	 * Test the creation of a test object with a non-existing collection
	 * property.
	 *
	 * It should fail with a 422 status and an error message indicating that
	 * the collection property doesn't exist.
	@Test
	public void test010CreateTestObjectWithUnknownCollectionProperty() {

		String testOneId;

		String location = RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			//.body(" { 'name' : 'TestOne-'" + i + "' } ")
		.expect()
			.statusCode(201)
		.when()
			.post("/TestOne")
			.getHeader("Location");

		// POST must return a Location header
		assertNotNull(location);

		testOneId = getUuidFromLocation(location);

		// POST must create a UUID
		assertNotNull(testOneId);
		assertFalse(testOneId.isEmpty());
		assertTrue(testOneId.matches("[a-f0-9]{32}"));

		// POST to create a TestFour object with relationship to the TestOne object
		location = RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { 'manyToManyTestOnes' : [ {'id': '" + testOneId + "'}] } ")
		.expect()
			.statusCode(201)
		.when()
			.post("/TestFour")
			.getHeader("Location");

		String testFiveId = getUuidFromLocation(location);
		assertNotNull(testFiveId);
		assertFalse(testFiveId.isEmpty());
		assertTrue(testFiveId.matches("[a-f0-9]{32}"));

		// Now almost same POST but with wrong (=unknown) attribute name
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { 'foo' : [ {'id': '" + testOneId + "'}] } ")
		.expect()
			.statusCode(422)
		.when()
			.post("/TestFour")
			.getHeader("Location");


	}
	 */

	@Test
	public void testOneToMany() throws Exception {


	}


	/**
	 * Test of typeName method, of class CollectionProperty.
	 */
	@Test
	public void testTypeName() {

	}

	/**
	 * Test of databaseConverter method, of class CollectionProperty.
	 */
	@Test
	public void testDatabaseConverter() {

	}

	/**
	 * Test of inputConverter method, of class CollectionProperty.
	 */
	@Test
	public void testInputConverter() {

	}

	/**
	 * Test of relatedType method, of class CollectionProperty.
	 */
	@Test
	public void testRelatedType() {

	}

	/**
	 * Test of isCollection method, of class CollectionProperty.
	 */
	@Test
	public void testIsCollection() {

	}

	/**
	 * Test of getNotion method, of class CollectionProperty.
	 */
	@Test
	public void testGetNotion() {

	}

	/**
	 * Test of isOneToMany method, of class CollectionProperty.
	 */
	@Test
	public void testIsOneToMany() {

	}
}
