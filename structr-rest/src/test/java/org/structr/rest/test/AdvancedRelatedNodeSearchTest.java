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
import org.structr.rest.common.StructrRestTest;
import static org.hamcrest.Matchers.*;

/**
 *
 *
 */
public class AdvancedRelatedNodeSearchTest extends StructrRestTest {

	public void testStaticRelationshipResourceFilter() {
		
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

		// test simple related node search
		RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			
			.expect()
				.statusCode(200)

				.body("result",	      hasSize(1))
				.body("result_count", equalTo(1))
				.body("result[0].id", equalTo(test13))

			.when()
				.get(concat("/test_sixs/", test01, "/testEights?aString=string13&anInt=13"));

		// test simple related node search with range query
		RestAssured
		    
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			
			.expect()
				.statusCode(200)

				.body("result",	      hasSize(2))
				.body("result_count", equalTo(2))
				.body("result[0].id", equalTo(test14))
				.body("result[1].id", equalTo(test15))

			.when()
				.get(concat("/test_sixs/", test03, "/testEights?anInt=[14 TO 18]"));
	}
}
