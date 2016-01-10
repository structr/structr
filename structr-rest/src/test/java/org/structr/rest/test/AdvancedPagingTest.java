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

import static org.hamcrest.Matchers.*;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import java.util.LinkedList;
import java.util.List;
import org.structr.rest.common.StructrRestTest;
import org.structr.rest.entity.TestOne;

/**
 *
 *
 */
public class AdvancedPagingTest extends StructrRestTest {


	/**
	 * Paging with offsetId
	 */
	public void test01Paging() {

		// create a root object
		String resource = "/test_twos";

		String location = RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestTwo-0', 'anInt' : 0, 'aLong' : 0, 'aDate' : '2012-09-18T00:33:12+0200' } ")
			.expect().statusCode(201).when().post(resource).getHeader("Location");

		String baseId = getUuidFromLocation(location);

		resource = resource.concat("/").concat(baseId).concat("/test_ones");

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

		resource = "/test_ones";

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


	/**
	 * Paging of subresources
	 */
	public void test02PagingOfSubresources() {

		// create a root object

		String resource = "/test_twos";

		String location = RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestTwo-0', 'anInt' : 0, 'aLong' : 0, 'aDate' : '2012-09-18T00:33:12+0200' } ")
			.expect().statusCode(201).when().post(resource).getHeader("Location");

		String baseId = getUuidFromLocation(location);

		resource = resource.concat("/").concat(baseId).concat("/test_ones");

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

		}

		resource = "/test_twos/" + baseId + "/test_ones";

		for (int page=1; page<5; page++) {

			String url = resource + "?sort=name&pageSize=2&page=" + page;

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

	public void test03RangeHeader() {

		// create a root object

		final List<String> testOneIDs = new LinkedList<>();
		String resource               = "/test_twos";

		String location = RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestTwo-0', 'anInt' : 0, 'aLong' : 0, 'aDate' : '2012-09-18T00:33:12+0200' } ")
			.expect().statusCode(201).when().post(resource).getHeader("Location");

		String baseId = getUuidFromLocation(location);

		resource = resource.concat("/").concat(baseId).concat("/test_ones");

		// create sub objects
		for (int i=0; i<20; i++) {

			final String subLocation = RestAssured.given().contentType("application/json; charset=UTF-8")
				.body(" { 'name' : 'TestOne-" + i + "', 'anInt' : " + i + ", 'aLong' : " + i + ", 'aDate' : '2012-09-18T0" + i + ":33:12+0200' } ")
				.expect().statusCode(201).when().post(resource).getHeader("Location");

			testOneIDs.add(getUuidFromLocation(subLocation));
		}

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header("Range", "test_ones=0-3")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.expect()
				.statusCode(200)
				.body("result",                    hasSize(1))
				.body("result_count",              equalTo(1))

				.body("result[0]",                 isEntity(TestOne.class))
				.body("result[0].test_ones",       hasSize(3))
				.body("result[0].test_ones[0].id", equalTo(testOneIDs.get(0)))
				.body("result[0].test_ones[1].id", equalTo(testOneIDs.get(1)))
				.body("result[0].test_ones[2].id", equalTo(testOneIDs.get(2)))

			.when()
				.get("/test_twos");


		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header("Range", "test_ones=3-6")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.expect()
				.statusCode(200)
				.body("result",                    hasSize(1))
				.body("result_count",              equalTo(1))

				.body("result[0]",                 isEntity(TestOne.class))
				.body("result[0].test_ones",       hasSize(3))
				.body("result[0].test_ones[0].id", equalTo(testOneIDs.get(3)))
				.body("result[0].test_ones[1].id", equalTo(testOneIDs.get(4)))
				.body("result[0].test_ones[2].id", equalTo(testOneIDs.get(5)))

			.when()
				.get("/test_twos");


		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header("Range", "test_ones=10-20")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.expect()
				.statusCode(200)
				.body("result",                    hasSize(1))
				.body("result_count",              equalTo(1))

				.body("result[0]",                 isEntity(TestOne.class))
				.body("result[0].test_ones",       hasSize(10))
				.body("result[0].test_ones[0].id", equalTo(testOneIDs.get(10)))
				.body("result[0].test_ones[1].id", equalTo(testOneIDs.get(11)))
				.body("result[0].test_ones[2].id", equalTo(testOneIDs.get(12)))
				.body("result[0].test_ones[3].id", equalTo(testOneIDs.get(13)))
				.body("result[0].test_ones[4].id", equalTo(testOneIDs.get(14)))
				.body("result[0].test_ones[5].id", equalTo(testOneIDs.get(15)))
				.body("result[0].test_ones[6].id", equalTo(testOneIDs.get(16)))
				.body("result[0].test_ones[7].id", equalTo(testOneIDs.get(17)))
				.body("result[0].test_ones[8].id", equalTo(testOneIDs.get(18)))
				.body("result[0].test_ones[9].id", equalTo(testOneIDs.get(19)))

			.when()
				.get("/test_twos");


	}


}
