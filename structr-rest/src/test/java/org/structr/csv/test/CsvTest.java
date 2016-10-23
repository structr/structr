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
package org.structr.csv.test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import com.jayway.restassured.internal.RestAssuredResponseImpl;
import static junit.framework.TestCase.assertEquals;
import org.apache.commons.lang3.StringEscapeUtils;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.rest.service.HttpServiceServlet;
import org.structr.rest.servlet.CsvServlet;

/**
 *
 *
 */
public class CsvTest extends StructrCsvTest {

	private static final Logger logger = LoggerFactory.getLogger(CsvTest.class.getName());

	public void test01InitServlet() {

		try {
			final HttpServiceServlet servlet = (HttpServiceServlet) Class.forName(CsvServlet.class.getName()).newInstance();

			assertNotNull(servlet);

			assertTrue(servlet instanceof CsvServlet);

		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
			logger.warn("", ex);
			fail("Unexcepted exception");
		}

	}

	/**
	 * Test CSV output
	 */
	public void test02CsvOutput() {

		// create some objects

		String resource = "/test_one";

		String test0Id = getUuidFromLocation(RestAssured.given().contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.body(" { 'name' : 'TestOne-0', 'anInt' : 0, 'aLong' : 0, 'aDate' : '2012-09-18T00:33:12+0200' } ")
			.expect().statusCode(201).when().post(resource).getHeader("Location"));

		String test1Id = getUuidFromLocation(RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestOne-1', 'anInt' : 1, 'aLong' : 10, 'aDate' : '2012-09-18T01:33:12+0200' } ")
			.expect().statusCode(201).when().post(resource).getHeader("Location"));

		String test2Id = getUuidFromLocation(RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestOne-2', 'anInt' : 2, 'aLong' : 20, 'aDate' : '2012-09-18T02:33:12+0200' } ")
			.expect().statusCode(201).when().post(resource).getHeader("Location"));

		String test3Id = getUuidFromLocation(RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestOne-3', 'anInt' : 3, 'aLong' : 30, 'aDate' : '2012-09-18T03:33:12+0200' } ")
			.expect().statusCode(201).when().post(resource).getHeader("Location"));

		String test4Id = getUuidFromLocation(RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestOne-4', 'anInt' : 4, 'aLong' : 40, 'aDate' : '2012-09-18T04:33:12+0200' } ")
			.expect().statusCode(201).when().post(resource).getHeader("Location"));

		String test5Id = getUuidFromLocation(RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestOne-5', 'anInt' : 5, 'aLong' : 50, 'aDate' : '2012-09-18T05:33:12+0200' } ")
			.expect().statusCode(201).when().post(resource).getHeader("Location"));

		String test6Id = getUuidFromLocation(RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestOne-6', 'anInt' : 6, 'aLong' : 60, 'aDate' : '2012-09-18T06:33:12+0200' } ")
			.expect().statusCode(201).when().post(resource).getHeader("Location"));

		String test7Id = getUuidFromLocation(RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestOne-7', 'anInt' : 7, 'aLong' : 70, 'aDate' : '2012-09-18T07:33:12+0200' } ")
			.expect().statusCode(201).when().post(resource).getHeader("Location"));


		Object result = RestAssured

			.given()
				.contentType("application/csv; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
				.statusCode(200)
			.when()
				.get("http://" + host + ":" + httpPort + csvUrl + resource + "?sort=name");

		System.out.println("result: " + ((RestAssuredResponseImpl) result).prettyPrint());

		String expected =  "\"id\";\"type\";\"name\";\"anInt\";\"aLong\";\"aDate\"\r\n"
				+ "\"" + test0Id + "\";\"TestOne\";\"TestOne-0\";\"0\";\"0\";\"Mon Sep 17 22:33:12 UTC 2012\"\r\n"
				+ "\"" + test1Id + "\";\"TestOne\";\"TestOne-1\";\"1\";\"10\";\"Mon Sep 17 23:33:12 UTC 2012\"\r\n"
				+ "\"" + test2Id + "\";\"TestOne\";\"TestOne-2\";\"2\";\"20\";\"Tue Sep 18 00:33:12 UTC 2012\"\r\n"
				+ "\"" + test3Id + "\";\"TestOne\";\"TestOne-3\";\"3\";\"30\";\"Tue Sep 18 01:33:12 UTC 2012\"\r\n"
				+ "\"" + test4Id + "\";\"TestOne\";\"TestOne-4\";\"4\";\"40\";\"Tue Sep 18 02:33:12 UTC 2012\"\r\n"
				+ "\"" + test5Id + "\";\"TestOne\";\"TestOne-5\";\"5\";\"50\";\"Tue Sep 18 03:33:12 UTC 2012\"\r\n"
				+ "\"" + test6Id + "\";\"TestOne\";\"TestOne-6\";\"6\";\"60\";\"Tue Sep 18 04:33:12 UTC 2012\"\r\n"
				+ "\"" + test7Id + "\";\"TestOne\";\"TestOne-7\";\"7\";\"70\";\"Tue Sep 18 05:33:12 UTC 2012\"\r\n";

		String resultString = ((RestAssuredResponseImpl) result).asString();

		System.out.println(StringEscapeUtils.escapeJava(expected));
		System.out.println(StringEscapeUtils.escapeJava(resultString));

		assertEquals(expected, resultString);

		result = RestAssured

			.given()
				.contentType("application/csv; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
				.statusCode(200)
			.when()
				.get("http://" + host + ":" + httpPort + csvUrl + resource + "?sort=name&pageSize=2&page=1");

		System.out.println("result: " + ((RestAssuredResponseImpl) result).prettyPrint());

		expected = "\"id\";\"type\";\"name\";\"anInt\";\"aLong\";\"aDate\"\r\n"
			+ "\"" + test0Id + "\";\"TestOne\";\"TestOne-0\";\"0\";\"0\";\"Mon Sep 17 22:33:12 UTC 2012\"\r\n"
			+ "\"" + test1Id + "\";\"TestOne\";\"TestOne-1\";\"1\";\"10\";\"Mon Sep 17 23:33:12 UTC 2012\"\r\n";


		resultString = ((RestAssuredResponseImpl) result).asString();

		System.out.println(StringEscapeUtils.escapeJava(expected));
		System.out.println(StringEscapeUtils.escapeJava(resultString));

		assertEquals(expected, resultString);

		result = RestAssured

			.given()
				.contentType("application/csv; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
				.statusCode(200)
			.when()
				.get("http://" + host + ":" + httpPort + csvUrl + resource + "?sort=name&pageSize=2&page=2");

		System.out.println("result: " + ((RestAssuredResponseImpl) result).prettyPrint());

		expected = "\"id\";\"type\";\"name\";\"anInt\";\"aLong\";\"aDate\"\r\n"
			+ "\"" + test2Id + "\";\"TestOne\";\"TestOne-2\";\"2\";\"20\";\"Tue Sep 18 00:33:12 UTC 2012\"\r\n"
			+ "\"" + test3Id + "\";\"TestOne\";\"TestOne-3\";\"3\";\"30\";\"Tue Sep 18 01:33:12 UTC 2012\"\r\n";



		resultString = ((RestAssuredResponseImpl) result).asString();

		System.out.println(StringEscapeUtils.escapeJava(expected));
		System.out.println(StringEscapeUtils.escapeJava(resultString));

		assertEquals(expected, resultString);
	}

}
