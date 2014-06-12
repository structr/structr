/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import org.structr.rest.common.StructrRestTest;
import static org.hamcrest.Matchers.*;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;

/**
 *
 * @author Christian Morgner
 */
public class AdvancedSearchTest extends StructrRestTest {

	public void testGraphBasedIndexingSearch() {

		String test01 = createEntity("/test_sixs", "{ name: test01, aString: string01, anInt: 1 }");
		String test02 = createEntity("/test_sixs", "{ name: test02, aString: string02, anInt: 2 }");
		String test03 = createEntity("/test_sixs", "{ name: test03, aString: string03, anInt: 3 }");
		String test04 = createEntity("/test_sixs", "{ name: test04, aString: string04, anInt: 4 }");
		String test05 = createEntity("/test_sixs", "{ name: test05, aString: string05, anInt: 5 }");
		String test06 = createEntity("/test_sixs", "{ name: test06, aString: string06, anInt: 6 }");
		String test07 = createEntity("/test_sixs", "{ name: test07, aString: string07, anInt: 7 }");
		String test08 = createEntity("/test_sixs", "{ name: test08, aString: string08, anInt: 8 }");

		String test09 = createEntity("/test_sevens", "{ name: test09, testSixIds: [", test01, ",", test02, "], aString: string09, anInt: 9 }");
		String test10 = createEntity("/test_sevens", "{ name: test10, testSixIds: [", test03, ",", test04, "], aString: string10, anInt: 10 }");
		String test11 = createEntity("/test_sevens", "{ name: test11, testSixIds: [", test05, ",", test06, "], aString: string11, anInt: 11 }");
		String test12 = createEntity("/test_sevens", "{ name: test12, testSixIds: [", test07, ",", test08, "], aString: string12, anInt: 12 }");

		String test13 = createEntity("/test_eights", "{ name: test13, testSixIds: [", test01, ",", test02, "], aString: string13, anInt: 13 }");
		String test14 = createEntity("/test_eights", "{ name: test14, testSixIds: [", test02, ",", test03, "], aString: string14, anInt: 14 }");
		String test15 = createEntity("/test_eights", "{ name: test15, testSixIds: [", test03, ",", test04, "], aString: string15, anInt: 15 }");
		String test16 = createEntity("/test_eights", "{ name: test16, testSixIds: [", test04, ",", test05, "], aString: string16, anInt: 16 }");
		String test17 = createEntity("/test_eights", "{ name: test17, testSixIds: [", test05, ",", test06, "], aString: string17, anInt: 17 }");
		String test18 = createEntity("/test_eights", "{ name: test18, testSixIds: [", test06, ",", test07, "], aString: string18, anInt: 18 }");
		String test19 = createEntity("/test_eights", "{ name: test19, testSixIds: [", test07, ",", test08, "], aString: string19, anInt: 19 }");
		String test20 = createEntity("/test_eights", "{ name: test20, testSixIds: [", test08, ",", test01, "], aString: string20, anInt: 20 }");

		String test21 = createEntity("/test_sixs", "{ name: test21, aString: string21, anInt: 21 }");
		String test22 = createEntity("/test_sixs", "{ name: test22, aString: string22, anInt: 22 }");

		String test23 = createEntity("/test_eights", "{ name: test23, testSixIds: [", test21, ",", test22, "], aString: string23, anInt: 23 }");

		// test simple related search with one object,
		// expected result is a list of two elements:
		// test05 and test07
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")

			.expect()
				.statusCode(200)

				.body("result",	      hasSize(2))
				.body("result_count", equalTo(2))
				.body("result[0].id", equalTo(test01))
				.body("result[1].id", equalTo(test02))

			.when()
				.get(concat("/test_sixs?testSevenName=test09"));

		// test inexact related search with one object,
		// expected result is a list of four elements:
		// test01, test02, test03 and test04, because
		// test09 and test10 contain a "0" in their
		// name and are associated with those four
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")

			.expect()
				.statusCode(200)

				.body("result",	      hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].id", equalTo(test01))
				.body("result[1].id", equalTo(test02))
				.body("result[2].id", equalTo(test03))
				.body("result[3].id", equalTo(test04))

			.when()
				.get(concat("/test_sixs?testSevenName=0&loose=1"));


		// test simple related search with two objects, AND,
		// expected result is empty
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")

			.expect()
				.statusCode(200)

				.body("result",	      hasSize(0))
				.body("result_count", equalTo(0))

			.when()
				.get(concat("/test_sevens?testSixIds=", test01, ",", test06));

		// test simple related search with two objects, OR
		// expected result is a list of two elements:
		// test09 and test11
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")

			.expect()
				.statusCode(200)

				.body("result",	      hasSize(2))
				.body("result_count", equalTo(2))
				.body("result[0].id", equalTo(test09))
				.body("result[1].id", equalTo(test11))

			.when()
				.get(concat("/test_sevens?testSixIds=", test01, ";", test06));

		// test simple related search with two objects, AND,
		// expected result is empty
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")

			.expect()
				.statusCode(200)

				.body("result",	      hasSize(2))
				.body("result_count", equalTo(2))
				.body("result[0].id", equalTo(test13))
				.body("result[1].id", equalTo(test20))

			.when()
				.get(concat("/test_eights?testSixIds=", test01));

		// test simple related search with two objects, OR
		// expected result is a list of two elements:
		// test09 and test11
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")

			.expect()
				.statusCode(200)

				.body("result",	      hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].id", equalTo(test13))
				.body("result[1].id", equalTo(test17))
				.body("result[2].id", equalTo(test18))
				.body("result[3].id", equalTo(test20))

			.when()
				.get(concat("/test_eights?testSixIds=", test01, ";", test06));

		// test related search with two related properties,
		// expected result is a single object:
		// test01
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")

			.expect()
				.statusCode(200)

				.body("result",	      hasSize(1))
				.body("result_count", equalTo(1))
				.body("result[0].id", equalTo(test01))

			.when()
				.get(concat("/test_sixs?testSevenName=test09&testEightStrings=string20"));


		// test related search with a single related property and two
		// indexed properties that should filter the result set.
		// expected result is a single object:
		// test01
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")

			.expect()
				.statusCode(200)

				.body("result",	      hasSize(1))
				.body("result_count", equalTo(1))
				.body("result[0].id", equalTo(test01))

			.when()
				.get(concat("/test_sixs?testSevenName=test09&aString=string01&anInt=1"));

		// test related search with two related properties and one
		// indexed property that should filter the result set.
		// expected result is a single object:
		// test07
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")

			.expect()
				.statusCode(200)

				.body("result",	      hasSize(1))
				.body("result_count", equalTo(1))
				.body("result[0].id", equalTo(test07))

			.when()
				.get(concat("/test_sixs?testEightStrings=string19&testSevenName=test12&anInt=7"));

		// test inexact related search with collection property
		// expected result is a list of four objects:
		// test01, test08, test21, test22
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")

			.expect()
				.statusCode(200)

				.body("result",	      hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].id", equalTo(test01))
				.body("result[1].id", equalTo(test08))
				.body("result[2].id", equalTo(test21))
				.body("result[3].id", equalTo(test22))

			.when()
				.get(concat("/test_sixs?testEightStrings=2&loose=1"));

		// test inexact related search with two collection properties
		// expected result is a list of four objects:
		// test01, test02, test03, test04
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))

			.expect()
				.statusCode(200)

				.body("result",	      hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].id", equalTo(test01))
				.body("result[1].id", equalTo(test02))
				.body("result[2].id", equalTo(test03))
				.body("result[3].id", equalTo(test04))

			.when()
				.get(concat("/test_sixs?testEightStrings=1&testSevenName=0&loose=1"));

		// test inexact related search with collection property and
		// two filter properties,
		// expected result is a single object:
		// test08
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")

			.expect()
				.statusCode(200)

				.body("result",	      hasSize(1))
				.body("result_count", equalTo(1))
				.body("result[0].id", equalTo(test08))

			.when()
				.get(concat("/test_sixs?testEightStrings=2&testSevenName=test12&anInt=8&loose=1"));

		// test related search with one empty related property,
		// expected result is a list of two objects:
		// test21 and test22
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")

			.expect()
				.statusCode(200)

				.body("result",	      hasSize(2))
				.body("result_count", equalTo(2))
				.body("result[0].id", equalTo(test21))
				.body("result[1].id", equalTo(test22))

			.when()
				.get(concat("/test_sixs?testSevenName="));


		// test related search with one related property and one
		// empty related property.
		// expected result is a list of two objects:
		// test21 and test22
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")

			.expect()
				.statusCode(200)

				.body("result",	      hasSize(2))
				.body("result_count", equalTo(2))
				.body("result[0].id", equalTo(test21))
				.body("result[1].id", equalTo(test22))

			.when()
				.get(concat("/test_sixs?testSevenName=&testEightStrings=string23"));

		// test related search with one related property, one empty
		// related property and two indexed properties
		// expected result is a single object:
		// test21
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")

			.expect()
				.statusCode(200)

				.body("result",	      hasSize(1))
				.body("result_count", equalTo(1))
				.body("result[0].id", equalTo(test21))

			.when()
				.get(concat("/test_sixs?testSevenName=&testEightStrings=string23&aString=string21&anInt=21"));

	}

	public void testGraphBasedIndexingSearchCombinedWithGeocoding() {

		String test01 = createEntity("/test_eights", "{ name: test01, aString: string01, anInt: 1 }");
		String test02 = createEntity("/test_eights", "{ name: test02, aString: string02, anInt: 2 }");
		String test03 = createEntity("/test_eights", "{ name: test03, aString: string03, anInt: 3 }");
		String test04 = createEntity("/test_eights", "{ name: test04, aString: string04, anInt: 4 }");

		String test05 = createEntity("/test_nines", "{ name: test05, city: Dortmund, street: Strobelallee, testEightIds: [ ", test01, ",", test02, "] }");
		String test06 = createEntity("/test_nines", "{ name: test06, city: Köln, street: Heumarkt, testEightIds: [ ", test03, ",", test04, "] }");
		String test07 = createEntity("/test_nines", "{ name: test07, city: München, street: Maximiliansplatz }");

		// test geocoding, expected result is a list of 3 objects
		// test05, test06 and test07
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")

			.expect()
				.statusCode(200)

				.body("result",	              hasSize(3))
				.body("result_count",         equalTo(3))
				.body("result[0].id",         equalTo(test05))
				.body("result[0].postalCode", equalTo("44139"))
				.body("result[0].latitude",   notNullValue())
				.body("result[0].longitude",  notNullValue())

			.when()
				.get(concat("/test_nines"));

		// test geocoding, expected result is a list of 3 objects
		// test05, test06 and test07
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")

			.expect()
				.statusCode(200)

				.body("result",	              hasSize(2))
				.body("result_count",         equalTo(2))
				.body("result[0].id",         equalTo(test01))
				.body("result[1].id",         equalTo(test02))

			.when()
				.get(concat("/test_eights?testNinePostalCodes=44139"));

		// test geocoding, expected result is a list of 2 objects
		// test03 and test04.

		// NOTE: This test assumes that the geocoding will NOT find a postal
		// code for the address "Köln, Heumarkt", so this test will fail if
		// the  geocoding information of the given provider changes!
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")

			.expect()
				.statusCode(200)

				.body("result",	              hasSize(2))
				.body("result_count",         equalTo(2))
				.body("result[0].id",         equalTo(test03))
				.body("result[1].id",         equalTo(test04))

			.when()
				.get(concat("/test_eights?testNinePostalCodes="));

		// test geocoding, expected result is a list of 2 objects
		// test03 and test04.

		// NOTE: This test is exactly the same as above, but with a list of
		// the "empty" values for testNinePostalCodes
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")

			.expect()
				.statusCode(200)

				.body("result",	              hasSize(2))
				.body("result_count",         equalTo(2))
				.body("result[0].id",         equalTo(test03))
				.body("result[1].id",         equalTo(test04))

			.when()
				.get(concat("/test_eights?testNinePostalCodes=,,,"));

		// test spatial search, expected result is a single object:
		// test05
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")

			.expect()
				.statusCode(200)

				.body("result",	              hasSize(1))
				.body("result_count",         equalTo(1))
				.body("result[0].id",         equalTo(test05))

			.when()
				.get(concat("/test_nines?distance=2&location=Poststraße,Dortmund"));

		// test spatial search with large radius,
		// expected result is a list of two objects:
		// test05 and test06
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")

			.expect()
				.statusCode(200)

				.body("result",	              hasSize(2))
				.body("result_count",         equalTo(2))
				.body("result[0].id",         equalTo(test05))
				.body("result[1].id",         equalTo(test06))

			.when()
				.get(concat("/test_nines?distance=100&location=Bahnhofstraße,Wuppertal"));

		// test spatial search in combination with graph-based related node search,
		// expected result is a single result:
		// test05
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")

			.expect()
				.statusCode(200)

				.body("result",	              hasSize(1))
				.body("result_count",         equalTo(1))
				.body("result[0].id",         equalTo(test05))

			.when()
				.get(concat("/test_nines?distance=100&location=Bahnhofstraße,Wuppertal&testEightIds=", test01));

		// test spatial search in combination with an empty related node property,
		// expected result is a single result:
		// test06

		// NOTE: This test assumes that the geocoding will NOT find a postal
		// code for the address "Köln, Heumarkt", so this test will fail if
		// the  geocoding information of the given provider changes!
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")

			.expect()
				.statusCode(200)

				.body("result",	              hasSize(1))
				.body("result_count",         equalTo(1))
				.body("result[0].id",         equalTo(test06))

			.when()
				.get(concat("/test_nines?distance=100&location=Bahnhofstraße,Wuppertal&postalCode="));
	}

	public void testMultiValueSearch() {

		String test01 = createEntity("/test_sixs", "{ aString: string01 }");
		String test02 = createEntity("/test_sixs", "{ aString: string02 }");
		String test03 = createEntity("/test_sixs", "{ aString: string03 }");
		String test04 = createEntity("/test_sixs", "{ aString: string04 }");
		String test05 = createEntity("/test_sixs", "{ aString: string05 }");
		String test06 = createEntity("/test_sixs", "{ aString: string06 }");
		String test07 = createEntity("/test_sixs", "{ aString: string07 }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result",	              hasSize(3))
				.body("result_count",         equalTo(3))
				.body("result[0].id",         equalTo(test02))
				.body("result[1].id",         equalTo(test04))
				.body("result[2].id",         equalTo(test06))

			.when()
				.get(concat("/test_sixs?aString=string02;string04;string06"));


	}

	public void testSearchDynamicNodes() {

		/**
		 * This is actually a core test but has to go here because some
		 * of the includes for dynamic types are only available in rest
		 */

		try {

			SchemaNode node = null;

			// setup
			try (final Tx tx = app.tx()) {

				node = app.create(SchemaNode.class, new NodeAttribute(SchemaNode.name, "TestType"));
				node.setProperty(new StringProperty("_test"), "Integer");

				tx.success();
			}

			// fetch dynamic type info
			final Class dynamicType   = StructrApp.getConfiguration().getNodeEntityClass("TestType");
			final PropertyKey testKey = StructrApp.getConfiguration().getPropertyKeyForJSONName(dynamicType, "test");

			// modify schema node but keep reference to "old" type
			try (final Tx tx = app.tx()) {

				node.setProperty(new StringProperty("_test2"), "String");

				tx.success();
			}

			// create test nodes
			try (final Tx tx = app.tx()) {

				app.create(dynamicType, new NodeAttribute(testKey, 10));
				app.create(dynamicType, new NodeAttribute(testKey, 11));
				app.create(dynamicType, new NodeAttribute(testKey, 12));

				tx.success();
			}

			// query test nodes
			try (final Tx tx = app.tx()) {

				/*
				 * If this test fails, the method "allSubtypes" in SearchCommand was not able to identify
				 * a dynamic type as being assignable to itself. This can happen when the reference to an
				 * existing dynamic type is used after the type has been modified, because the class
				 * instances produced by the dynamic schema ClassLoader are not equal even if they have
				 * the same name and package etc.
				 */

				assertEquals("Query for dynamic node should return exactly one result: ", 1, app.nodeQuery(dynamicType).and(testKey, 10).getAsList().size());

				tx.success();
			}




		} catch (FrameworkException ex) {

			ex.printStackTrace();
			fail("Unexpected exception");

		}
	}
}
