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
package org.structr.test.csv.test;

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.internal.RestAssuredResponseImpl;
import java.lang.reflect.InvocationTargetException;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.Tx;
import org.structr.rest.service.HttpServiceServlet;
import org.structr.rest.servlet.CsvServlet;
import org.structr.test.rest.entity.TestOne;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.AssertJUnit.*;

/**
 *
 *
 */
public class CsvTest extends StructrCsvTest {

	private static final Logger logger = LoggerFactory.getLogger(CsvTest.class.getName());

	private final String testOneResource = "/TestOne";
	private final String testOneCSVWithDefaultCharacters5EntriesNoError = "\"id\";\"type\";\"name\";\"anInt\";\"aLong\";\"aDate\"\r\n"
			+ "\"0979aebeb9ae42a7b3594db3da12875e\";\"TestOne\";\"TestOne-1\";\"1\";\"10\";\"2012-09-18T00:33:12+0000\"\r\n"
			+ "\"a3e07672b1064c28a1093b7024c7087d\";\"TestOne\";\"TestOne-2\";\"2\";\"20\";\"2012-09-18T01:33:12+0000\"\r\n"
			+ "\"cd512cb9b7a44d65928794ac2dc9b383\";\"TestOne\";\"TestOne-3\";\"3\";\"30\";\"2012-09-18T02:33:12+0000\"\r\n"
			+ "\"673a27250c204995b4ba6c72edb1df66\";\"TestOne\";\"TestOne-4\";\"4\";\"40\";\"2012-09-18T03:33:12+0000\"\r\n"
			+ "\"9db92fff48df47db8ab81e0847f551ba\";\"TestOne\";\"TestOne-5\";\"5\";\"50\";\"2012-09-18T04:33:12+0000\"\r\n";

	private final String testOneCSVWithSingleQuotes5EntriesNoError = "'id';'type';'name';'anInt';'aLong';'aDate'\r\n"
			+ "'0979aebeb9ae42a7b3594db3da12875e';'TestOne';'TestOne-1';'1';'10';'2012-09-18T00:33:12+0000'\r\n"
			+ "'a3e07672b1064c28a1093b7024c7087d';'TestOne';'TestOne-2';'2';'20';'2012-09-18T01:33:12+0000'\r\n"
			+ "'cd512cb9b7a44d65928794ac2dc9b383';'TestOne';'TestOne-3';'3';'30';'2012-09-18T02:33:12+0000'\r\n"
			+ "'673a27250c204995b4ba6c72edb1df66';'TestOne';'TestOne-4';'4';'40';'2012-09-18T03:33:12+0000'\r\n"
			+ "'9db92fff48df47db8ab81e0847f551ba';'TestOne';'TestOne-5';'5';'50';'2012-09-18T04:33:12+0000'\r\n";

	private final String testOneCSVWithComma5EntriesNoError = "\"id\",\"type\",\"name\",\"anInt\",\"aLong\",\"aDate\"\r\n"
			+ "\"0979aebeb9ae42a7b3594db3da12875e\",\"TestOne\",\"TestOne-1\",\"1\",\"10\",\"2012-09-18T00:33:12+0000\"\r\n"
			+ "\"a3e07672b1064c28a1093b7024c7087d\",\"TestOne\",\"TestOne-2\",\"2\",\"20\",\"2012-09-18T01:33:12+0000\"\r\n"
			+ "\"cd512cb9b7a44d65928794ac2dc9b383\",\"TestOne\",\"TestOne-3\",\"3\",\"30\",\"2012-09-18T02:33:12+0000\"\r\n"
			+ "\"673a27250c204995b4ba6c72edb1df66\",\"TestOne\",\"TestOne-4\",\"4\",\"40\",\"2012-09-18T03:33:12+0000\"\r\n"
			+ "\"9db92fff48df47db8ab81e0847f551ba\",\"TestOne\",\"TestOne-5\",\"5\",\"50\",\"2012-09-18T04:33:12+0000\"\r\n";

	private final String testOneCSVWithSingleQuotesAndComma5EntriesNoError = "'id','type','name','anInt','aLong','aDate'\r\n"
			+ "'0979aebeb9ae42a7b3594db3da12875e','TestOne','TestOne-1','1','10','2012-09-18T00:33:12+0000'\r\n"
			+ "'a3e07672b1064c28a1093b7024c7087d','TestOne','TestOne-2','2','20','2012-09-18T01:33:12+0000'\r\n"
			+ "'cd512cb9b7a44d65928794ac2dc9b383','TestOne','TestOne-3','3','30','2012-09-18T02:33:12+0000'\r\n"
			+ "'673a27250c204995b4ba6c72edb1df66','TestOne','TestOne-4','4','40','2012-09-18T03:33:12+0000'\r\n"
			+ "'9db92fff48df47db8ab81e0847f551ba','TestOne','TestOne-5','5','50','2012-09-18T04:33:12+0000'\r\n";

	private final String testOneCSVWith5EntriesAndErrorAfterLine2 = "\"id\";\"type\";\"name\";\"anInt\";\"aLong\";\"aDate\"\r\n"
			+ "\"0979aebeb9ae42a7b3594db3da12875e\";\"TestOne\";\"TestOne-1\";\"1\";\"10\";\"2012-09-18T00:33:12+0000\"\r\n"
			+ "\"a3e07672b1064c28a1093b7024c7087d\";\"TestOne\";\"TestOne-2\";\"2\";\"20\";\"2012-09-18T01:33:12+0000\"\r\n"
			+ "\"cd512cb9b7a44d65928794ac2dc9b383\";\"TestOne\";\"TestOne-3\";\"ERROR3\";\"30\";\"2012-09-18T02:33:12+0000\"\r\n"
			+ "\"673a27250c204995b4ba6c72edb1df66\";\"TestOne\";\"TestOne-4\";\"4\";\"40\";\"2012-09-18T03:33:12+0000\"\r\n"
			+ "\"9db92fff48df47db8ab81e0847f551ba\";\"TestOne\";\"TestOne-5\";\"5\";\"50\";\"2012-09-18T04:33:12+0000\"\r\n";

	private final String testOneCSVWith5EntriesAndErrorAfterLine4 = "\"id\";\"type\";\"name\";\"anInt\";\"aLong\";\"aDate\"\r\n"
			+ "\"0979aebeb9ae42a7b3594db3da12875e\";\"TestOne\";\"TestOne-1\";\"1\";\"10\";\"2012-09-18T00:33:12+0000\"\r\n"
			+ "\"a3e07672b1064c28a1093b7024c7087d\";\"TestOne\";\"TestOne-2\";\"2\";\"20\";\"2012-09-18T01:33:12+0000\"\r\n"
			+ "\"cd512cb9b7a44d65928794ac2dc9b383\";\"TestOne\";\"TestOne-3\";\"3\";\"30\";\"2012-09-18T02:33:12+0000\"\r\n"
			+ "\"673a27250c204995b4ba6c72edb1df66\";\"TestOne\";\"TestOne-4\";\"4\";\"40\";\"2012-09-18T03:33:12+0000\"\r\n"
			+ "\"9db92fff48df47db8ab81e0847f551ba\";\"TestOne\";\"TestOne-5\";\"ERROR5\";\"50\";\"2012-09-18T04:33:12+0000\"\r\n";

	@Test
	public void test01InitServlet() {

		try {
			final HttpServiceServlet servlet = (HttpServiceServlet) Class.forName(CsvServlet.class.getName()).getDeclaredConstructor().newInstance();

			assertNotNull(servlet);

			assertTrue(servlet instanceof CsvServlet);

		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException ex) {
			logger.warn("", ex);
			fail("Unexcepted exception");
		}

	}

	/**
	 * Test CSV output
	 */
	@Test
	public void test02CsvOutput() {

		try { Thread.sleep(2000); } catch (Throwable t) {}

		// create some objects

		String test0Id = getUuidFromLocation(RestAssured.given().contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.body(" { 'name' : 'TestOne-0', 'anInt' : 0, 'aLong' : 0, 'aDate' : '2012-09-18T00:33:12+0200' } ")
			.expect().statusCode(201).when().post(restUrl + testOneResource).getHeader("Location"));

		String test1Id = getUuidFromLocation(RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestOne-1', 'anInt' : 1, 'aLong' : 10, 'aDate' : '2012-09-18T01:33:12+0200' } ")
			.expect().statusCode(201).when().post(restUrl + testOneResource).getHeader("Location"));

		String test2Id = getUuidFromLocation(RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestOne-2', 'anInt' : 2, 'aLong' : 20, 'aDate' : '2012-09-18T02:33:12+0200' } ")
			.expect().statusCode(201).when().post(restUrl + testOneResource).getHeader("Location"));

		String test3Id = getUuidFromLocation(RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestOne-3', 'anInt' : 3, 'aLong' : 30, 'aDate' : '2012-09-18T03:33:12+0200' } ")
			.expect().statusCode(201).when().post(restUrl + testOneResource).getHeader("Location"));

		String test4Id = getUuidFromLocation(RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestOne-4', 'anInt' : 4, 'aLong' : 40, 'aDate' : '2012-09-18T04:33:12+0200' } ")
			.expect().statusCode(201).when().post(restUrl + testOneResource).getHeader("Location"));

		String test5Id = getUuidFromLocation(RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestOne-5', 'anInt' : 5, 'aLong' : 50, 'aDate' : '2012-09-18T05:33:12+0200' } ")
			.expect().statusCode(201).when().post(restUrl + testOneResource).getHeader("Location"));

		String test6Id = getUuidFromLocation(RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestOne-6', 'anInt' : 6, 'aLong' : 60, 'aDate' : '2012-09-18T06:33:12+0200' } ")
			.expect().statusCode(201).when().post(restUrl + testOneResource).getHeader("Location"));

		String test7Id = getUuidFromLocation(RestAssured.given().contentType("application/json; charset=UTF-8")
			.body(" { 'name' : 'TestOne-7', 'anInt' : 7, 'aLong' : 70, 'aDate' : '2012-09-18T07:33:12+0200' } ")
			.expect().statusCode(201).when().post(restUrl + testOneResource).getHeader("Location"));


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
				.get(csvUrl + testOneResource + "?_sort=name");

		System.out.println("result: " + ((RestAssuredResponseImpl) result).prettyPrint());

		String expected =  "\"id\";\"type\";\"name\";\"anInt\";\"aLong\";\"aDate\"\r\n"
				+ "\"" + test0Id + "\";\"TestOne\";\"TestOne-0\";\"0\";\"0\";\"2012-09-17T22:33:12+0000\"\r\n"
				+ "\"" + test1Id + "\";\"TestOne\";\"TestOne-1\";\"1\";\"10\";\"2012-09-17T23:33:12+0000\"\r\n"
				+ "\"" + test2Id + "\";\"TestOne\";\"TestOne-2\";\"2\";\"20\";\"2012-09-18T00:33:12+0000\"\r\n"
				+ "\"" + test3Id + "\";\"TestOne\";\"TestOne-3\";\"3\";\"30\";\"2012-09-18T01:33:12+0000\"\r\n"
				+ "\"" + test4Id + "\";\"TestOne\";\"TestOne-4\";\"4\";\"40\";\"2012-09-18T02:33:12+0000\"\r\n"
				+ "\"" + test5Id + "\";\"TestOne\";\"TestOne-5\";\"5\";\"50\";\"2012-09-18T03:33:12+0000\"\r\n"
				+ "\"" + test6Id + "\";\"TestOne\";\"TestOne-6\";\"6\";\"60\";\"2012-09-18T04:33:12+0000\"\r\n"
				+ "\"" + test7Id + "\";\"TestOne\";\"TestOne-7\";\"7\";\"70\";\"2012-09-18T05:33:12+0000\"\r\n";

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
				.get(csvUrl + testOneResource + "?_sort=name&_pageSize=2&_page=1");

		System.out.println("result: " + ((RestAssuredResponseImpl) result).prettyPrint());

		expected = "\"id\";\"type\";\"name\";\"anInt\";\"aLong\";\"aDate\"\r\n"
			+ "\"" + test0Id + "\";\"TestOne\";\"TestOne-0\";\"0\";\"0\";\"2012-09-17T22:33:12+0000\"\r\n"
			+ "\"" + test1Id + "\";\"TestOne\";\"TestOne-1\";\"1\";\"10\";\"2012-09-17T23:33:12+0000\"\r\n";


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
				.get(csvUrl + testOneResource + "?_sort=name&_pageSize=2&_page=2");

		System.out.println("result: " + ((RestAssuredResponseImpl) result).prettyPrint());

		expected = "\"id\";\"type\";\"name\";\"anInt\";\"aLong\";\"aDate\"\r\n"
			+ "\"" + test2Id + "\";\"TestOne\";\"TestOne-2\";\"2\";\"20\";\"2012-09-18T00:33:12+0000\"\r\n"
			+ "\"" + test3Id + "\";\"TestOne\";\"TestOne-3\";\"3\";\"30\";\"2012-09-18T01:33:12+0000\"\r\n";



		resultString = ((RestAssuredResponseImpl) result).asString();

		System.out.println(StringEscapeUtils.escapeJava(expected));
		System.out.println(StringEscapeUtils.escapeJava(resultString));

		assertEquals(expected, resultString);
	}

	/**
	 * Test CSV import with default settings
	 */
	@Test
	public void test03CsvImportDefaultSettings() {

		RestAssured.given().contentType("text/csv; charset=UTF-8").body(testOneCSVWithDefaultCharacters5EntriesNoError).expect().statusCode(201).when().post(csvUrl + testOneResource);

		try (final Tx tx = app.tx()) {

			final int testOneCount = app.nodeQuery(TestOne.class).getAsList().size();
			assertEquals(5, testOneCount);

			tx.success();

		} catch (FrameworkException ex) {
			fail();
		}

	}

	/**
	 * Test CSV import with default settings and paging
	 */
	@Test
	public void test03CsvImportDefaultSettingsAndPaging01() {

		RestAssured
			.given()
				.contentType("text/csv; charset=UTF-8")
				.header(CsvServlet.DEFAULT_RANGE_HEADER_NAME, "2-3")
				.body(testOneCSVWithDefaultCharacters5EntriesNoError)
			.expect()
				.statusCode(201)
			.when()
			.post(csvUrl + testOneResource);

		try (final Tx tx = app.tx()) {


			final List<TestOne> result = app.nodeQuery(TestOne.class).sort(AbstractNode.name).getAsList();

			assertEquals(2, result.size());
			assertEquals("a3e07672b1064c28a1093b7024c7087d", result.get(0).getUuid());
			assertEquals("cd512cb9b7a44d65928794ac2dc9b383", result.get(1).getUuid());

			tx.success();

		} catch (FrameworkException ex) {
			fail();
		}

	}

	/**
	 * Test CSV import with default settings and paging
	 */
	@Test
	public void test03CsvImportDefaultSettingsAndPaging02() {

		RestAssured
			.given()
				.contentType("text/csv; charset=UTF-8")
				.header(CsvServlet.DEFAULT_RANGE_HEADER_NAME, "1,2,4")
				.body(testOneCSVWithDefaultCharacters5EntriesNoError)
			.expect()
				.statusCode(201)
			.when()
			.post(csvUrl + testOneResource);

		try (final Tx tx = app.tx()) {

			final List<TestOne> result = app.nodeQuery(TestOne.class).sort(AbstractNode.name).getAsList();

			assertEquals(3, result.size());
			assertEquals("0979aebeb9ae42a7b3594db3da12875e", result.get(0).getUuid());
			assertEquals("a3e07672b1064c28a1093b7024c7087d", result.get(1).getUuid());
			assertEquals("673a27250c204995b4ba6c72edb1df66", result.get(2).getUuid());

			tx.success();

		} catch (FrameworkException ex) {
			fail();
		}

	}

	/**
	 * Test CSV import with
	 *
	 *  - Periodic commit enabled
	 *  - periodic commit interval 1
	 *
	 * tests that periodic commit works
	 */
	@Test
	public void test04CsvImportWithPeriodicCommitWithoutError() {

		RestAssured.given().contentType("text/csv; charset=UTF-8")
				.header(CsvServlet.DEFAULT_PERIODIC_COMMIT_HEADER_NAME, true)
				.header(CsvServlet.DEFAULT_PERIODIC_COMMIT_INTERVAL_HEADER_NAME, 1)
				.body(testOneCSVWithDefaultCharacters5EntriesNoError).expect().statusCode(201).when().post(csvUrl + testOneResource);

		try (final Tx tx = app.tx()) {

			final int testOneCount = app.nodeQuery(TestOne.class).getAsList().size();
			assertEquals(5, testOneCount);

			tx.success();

		} catch (FrameworkException ex) {
			fail();
		}

	}

	/**
	 * Test CSV import with
	 *
	 *  - Periodic commit enabled
	 *  - periodic commit interval 2
	 *  - uneven number of CSV lines
	 *
	 * Tests that all CSV lines are imported even though the last chunk is not 'full' (meaning that the number of lines is not evenly divisible by the periodic commit interval)
	 */
	@Test
	public void test05CsvImportWithPeriodicCommitWithoutError() {

		RestAssured.given().contentType("text/csv; charset=UTF-8")
				.header(CsvServlet.DEFAULT_PERIODIC_COMMIT_HEADER_NAME, true)
				.header(CsvServlet.DEFAULT_PERIODIC_COMMIT_INTERVAL_HEADER_NAME, 2)
				.body(testOneCSVWithDefaultCharacters5EntriesNoError).expect().statusCode(201).when().post(csvUrl + testOneResource);

		try (final Tx tx = app.tx()) {

			final int testOneCount = app.nodeQuery(TestOne.class).getAsList().size();
			assertEquals(5, testOneCount);

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail();
		}

	}

	/**
	 * Test CSV import with default settings and an error in the CSV
	 *
	 * Tests that no CSV lines are imported because the whole transaction is rolled back
	 */
	@Test
	public void test06CsvImportWithPeriodicCommitWithError() {

		RestAssured.given().contentType("text/csv; charset=UTF-8").body(testOneCSVWith5EntriesAndErrorAfterLine2).expect().statusCode(422).when().post(csvUrl + testOneResource);

		try (final Tx tx = app.tx()) {

			final int testOneCount = app.nodeQuery(TestOne.class).getAsList().size();
			assertEquals(0, testOneCount);

			tx.success();

		} catch (FrameworkException ex) {
			fail();
		}

	}


	/**
	 * Test CSV import with
	 *
	 *  - Periodic commit enabled
	 *  - periodic commit interval 2
	 *  - uneven number of CSV lines
	 *  - error after line 2
	 *
	 * Tests that with periodic commit enabled, all chunks before the error are imported (with the error being in the first line of the next chunk)
	 */
	@Test
	public void test07CsvImportWithPeriodicCommitWithError() {

		final int errorAfterLine = 2;
		final int periodicCommitInterval = 2;
		final int shouldCreateNumberOfObjects = errorAfterLine - errorAfterLine % periodicCommitInterval;

		RestAssured.given().contentType("text/csv; charset=UTF-8")
				.header(CsvServlet.DEFAULT_PERIODIC_COMMIT_HEADER_NAME, "true")
				.header(CsvServlet.DEFAULT_PERIODIC_COMMIT_INTERVAL_HEADER_NAME, ""+periodicCommitInterval)
				.body(testOneCSVWith5EntriesAndErrorAfterLine2).expect().statusCode(422).when().post(csvUrl + testOneResource);

		try (final Tx tx = app.tx()) {

			final int testOneCount = app.nodeQuery(TestOne.class).getAsList().size();
			assertEquals(shouldCreateNumberOfObjects, testOneCount);

			tx.success();

		} catch (FrameworkException ex) {
			fail();
		}

	}

	/**
	 * Test CSV import with
	 *
	 *  - Periodic commit enabled
	 *  - periodic commit interval 3
	 *  - uneven number of CSV lines
	 *  - error after line 4
	 *
	 * Tests that with periodic commit enabled, all chunks before the error are imported
	 * The error is NOT the first line in the next chunk, so this tests that the error-free lines in the chunk before the error are rolled back
	 */
	@Test
	public void test08CsvImportWithPeriodicCommitWithError() {

		final int errorAfterLine = 4;
		final int periodicCommitInterval = 3;
		final int expectedNumberOfObjects = errorAfterLine - errorAfterLine % periodicCommitInterval;

		RestAssured.given().contentType("text/csv; charset=UTF-8")
				.header(CsvServlet.DEFAULT_PERIODIC_COMMIT_HEADER_NAME, true)
				.header(CsvServlet.DEFAULT_PERIODIC_COMMIT_INTERVAL_HEADER_NAME, periodicCommitInterval)
				.body(testOneCSVWith5EntriesAndErrorAfterLine4).expect().statusCode(422).when().post(csvUrl + testOneResource);

		try (final Tx tx = app.tx()) {

			final int testOneCount = app.nodeQuery(TestOne.class).getAsList().size();
			assertEquals(expectedNumberOfObjects, testOneCount);

			tx.success();

		} catch (FrameworkException ex) {
			fail();
		}

	}

	/**
	 * Test CSV import with quote character = '
	 */
	@Test
	public void test09CsvImportWithSingleQuote() {

		RestAssured.given().contentType("text/csv; charset=UTF-8")
				.header(CsvServlet.DEFAULT_QUOTE_CHARACTER_HEADER_NAME, "'")
				.body(testOneCSVWithSingleQuotes5EntriesNoError).expect().statusCode(201).when().post(csvUrl + testOneResource);


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
				.get(csvUrl + testOneResource + "?_sort=name");

		final String resultString = ((RestAssuredResponseImpl) result).asString();

		assertEquals(testOneCSVWithDefaultCharacters5EntriesNoError, resultString);

	}

	/**
	 * Test CSV import with field separator = ,
	 */
	@Test
	public void test10CsvImportWithComma() {

		RestAssured.given().contentType("text/csv; charset=UTF-8")
				.header(CsvServlet.DEFAULT_FIELD_SEPARATOR_HEADER_NAME, ",")
				.body(testOneCSVWithComma5EntriesNoError).expect().statusCode(201).when().post(csvUrl + testOneResource);


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
				.get(csvUrl + testOneResource + "?_sort=name");

		final String resultString = ((RestAssuredResponseImpl) result).asString();

		assertEquals(testOneCSVWithDefaultCharacters5EntriesNoError, resultString);

	}

	/**
	 * Test CSV import with field separator = , and quote character = '
	 */
	@Test
	public void test10CsvImportWithCommaAndSingleQuote() {

		RestAssured.given().contentType("text/csv; charset=UTF-8")
				.header(CsvServlet.DEFAULT_FIELD_SEPARATOR_HEADER_NAME, ",")
				.header(CsvServlet.DEFAULT_QUOTE_CHARACTER_HEADER_NAME, "'")
				.body(testOneCSVWithSingleQuotesAndComma5EntriesNoError).expect().statusCode(201).when().post(csvUrl + testOneResource);


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
				.get(csvUrl + testOneResource + "?_sort=name");

		final String resultString = ((RestAssuredResponseImpl) result).asString();

		assertEquals(testOneCSVWithDefaultCharacters5EntriesNoError, resultString);

	}
}
