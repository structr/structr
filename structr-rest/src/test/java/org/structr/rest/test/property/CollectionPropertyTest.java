/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.rest.test.property;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import static org.hamcrest.Matchers.*;
import org.junit.Test;
import org.structr.rest.common.StructrRestTest;

/**
 *
 *
 */
public class CollectionPropertyTest extends StructrRestTest {

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

		// POST to create a TestOne object with relationships to the three TestOnes
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
