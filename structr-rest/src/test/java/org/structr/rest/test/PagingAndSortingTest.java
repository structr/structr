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
package org.structr.rest.test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import java.text.SimpleDateFormat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.structr.rest.common.StructrRestTest;
import org.structr.rest.entity.TestOne;

/**
 *
 *
 */
public class PagingAndSortingTest extends StructrRestTest {

	private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

	private final String aDate = dateFormat.format(System.currentTimeMillis());
	private final String aDate3 = dateFormat.format(System.currentTimeMillis() - 200000);	// 2012-09-18T03:33:12+0200
	private final String aDate2 = dateFormat.format(System.currentTimeMillis() - 400000);	// 2012-09-18T01:33:12+0200
	private final String aDate4 = dateFormat.format(System.currentTimeMillis() - 100000);	// 2012-09-18T04:33:12+0200
	private final String aDate1 = dateFormat.format(System.currentTimeMillis() - 300000);	// 2012-09-18T02:33:12+0200

	@Test
	public void test01CreateTestOne() {

		// create named object
		String location = RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.body(" { 'name' : 'a TestOne object', 'anInt' : 123, 'aLong' : 234, 'aDate' : '" + aDate + "' } ")
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
				.body("query_time",         lessThan("0.5"))
				.body("serialization_time", lessThan("0.05"))
				.body("result[0]",          isEntity(TestOne.class))
			.when()
				.get("/test_one");

	}


	/**
	 * Test a more complex object
	 */
	@Test
	public void test02SortTestOne() {

		// create some objects

		String resource = "/test_one";

		RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestOne-3', 'anInt' : 3, 'aLong' : 30, 'aDate' : '" + aDate3 + "' } ")
			.expect().statusCode(201).when().post(resource).getHeader("Location");

		RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestOne-1', 'anInt' : 2, 'aLong' : 10, 'aDate' : '" + aDate2 + "' } ")
			.expect().statusCode(201).when().post(resource).getHeader("Location");

		RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestOne-4', 'anInt' : 4, 'aLong' : 40, 'aDate' : '" + aDate4 + "' } ")
			.expect().statusCode(201).when().post(resource).getHeader("Location");

		RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestOne-2', 'anInt' : 1, 'aLong' : 20, 'aDate' : '" + aDate1 + "' } ")
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
				.body("result[0].aDate",    equalTo(aDate2))

				.body("result[1]",          isEntity(TestOne.class))
				.body("result[1].aLong",    equalTo(20))
				.body("result[1].aDate",    equalTo(aDate1))

				.body("result[2]",          isEntity(TestOne.class))
				.body("result[2].aLong",    equalTo(30))
				.body("result[2].aDate",    equalTo(aDate3))

				.body("result[3]",          isEntity(TestOne.class))
				.body("result[3].aLong",    equalTo(40))
				.body("result[3].aDate",    equalTo(aDate4))

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
				.body("result[0].aDate",    equalTo(aDate4))

				.body("result[1]",          isEntity(TestOne.class))
				.body("result[1].aLong",    equalTo(30))
				.body("result[1].aDate",    equalTo(aDate3))

				.body("result[2]",          isEntity(TestOne.class))
				.body("result[2].aLong",    equalTo(20))
				.body("result[2].aDate",    equalTo(aDate1))

				.body("result[3]",          isEntity(TestOne.class))
				.body("result[3].aLong",    equalTo(10))
				.body("result[3].aDate",    equalTo(aDate2))

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



	/**
	 * Test paging
	 */
	@Test
	public void test03Paging() {

		// create some objects

		String resource = "/test_one";

		RestAssured.given().contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
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
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
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
