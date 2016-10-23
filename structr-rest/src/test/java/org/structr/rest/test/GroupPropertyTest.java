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
import static org.hamcrest.Matchers.*;
import org.junit.Test;
import org.structr.rest.common.StructrRestTest;

/**
 *
 *
 */
public class GroupPropertyTest extends StructrRestTest{

	@Test
	public void test01GroupProperty(){


		String test011 = createEntity("/test_group_prop_one","{gP1:{sP:text,iP:1337},gP2:{dblP:13.37,dP:01.01.2013}}");

		String test021 = createEntity("/test_group_prop_two", "{gP1:{sP:text,iP:1337,dblP:0.1337,bP:true},gP2:{ep:two}}");

		String test031 = createEntity("/test_group_prop_three","{gP:{sP:text,iP:1337,gpNode:",test011,"}}");
		String test032 = createEntity("/test_group_prop_three","{ggP:{igP:{gpNode:",test021,",isP:Alex}}}");

		// test011 check
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
//				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))

			.expect()
				.statusCode(200)

				.body("result_count", equalTo(1))
				.body("result.id", equalTo(test011))
				.body("result.gP1.sP",equalTo("text"))
				.body("result.gP2.dblP",equalTo(13.37f))

			.when()
				.get(concat("/test_group_prop_one/"+test011));

		// test021 check
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
//				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))

			.expect()
				.statusCode(200)

				.body("result_count", equalTo(1))
				.body("result.id", equalTo(test021))
				.body("result.gP1.dblP",equalTo(0.1337f))
				.body("result.gP1.bP",equalTo(true))

			.when()
				.get(concat("/test_group_prop_two/"+test021));

		// test031 check
		// Node in groupProperty
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
//				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))

			.expect()
				.statusCode(200)

				.body("result_count", equalTo(1))
				.body("result.id", equalTo(test031))
				.body("result.gP.gpNode.id",equalTo(test011))

			.when()
				.get(concat("/test_group_prop_three/"+test031));

		// test032 check
		// Node in GroupProperty in GroupProperty
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))

			.expect()
				.statusCode(200)

				.body("result_count", equalTo(1))
				.body("result.id", equalTo(test032))
				.body("result.ggP.igP.gpNode.id",equalTo(test021))

			.when()
				.get(concat("/test_group_prop_three/"+test032));
	}

	@Test
	public void test02SearchProperty(){

		String test01 = createEntity("/test_group_prop_four","{gP:{sP:text,iP:1337}}");

		// Test find sP in GroupProperty gP
		// expected result is a single object
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))

			.expect()
				.statusCode(200)

				.body("result", hasSize(1))
				.body("result_count", equalTo(1))
				.body("result[0].id", equalTo(test01))
				.body("result[0].gP.sP", equalTo("text"))

			.when()
				.get(concat("/test_group_prop_four/?gP.sP=text"));

		// Test find iP in GroupProperty gP
		// expected result is empty
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))

			.expect()
				.statusCode(200)

				.body("result", hasSize(0))
				.body("result_count", equalTo(0))

			.when()
				.get(concat("/test_group_prop_four/?gP.iP=1336"));

		String test02 = createEntity("/test_group_prop_four","{twitter:{uid:11111}}");
		String test03 = createEntity("/test_group_prop_four","{facebook:{uid:11111}}");

		// Test find uid in GroupProperty twitter
		// expected result is a single object
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))

			.expect()
				.statusCode(200)

				.body("result", hasSize(1))
				.body("result_count", equalTo(1))
				.body("result[0].id", equalTo(test02))

			.when()
				.get(concat("/test_group_prop_four/?twitter.uid=11111"));

		// Test find uid in GroupProperty facebook
		// expected result is single object
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))

			.expect()
				.statusCode(200)

				.body("result", hasSize(1))
				.body("result_count", equalTo(1))
				.body("result[0].id", equalTo(test03))
				.body("result[0].facebook.uid",equalTo("11111"))

			.when()
				.get(concat("/test_group_prop_four/?facebook.uid=11111"));

		String test04 = createEntity("/test_group_prop_four","{facebook:{uid:11111},twitter:{uid:22222}}");
		String test05 = createEntity("/test_group_prop_four","{facebook:{uid:22222},twitter:{uid:11111}}");
		String test06 = createEntity("/test_group_prop_four","{facebook:{uid:33333},twitter:{uid:22222}}");
		String test07 = createEntity("/test_group_prop_four","{facebook:{uid:33333},twitter:{uid:33333}}");

		// find facebook AND Twitter with uid 11111
		// expected result is empty
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))

			.expect()
				.statusCode(200)

				.body("result", hasSize(0))
				.body("result_count", equalTo(0))

			.when()
				.get(concat("/test_group_prop_four/?facebook.uid=11111&twitter.uid=11111"));

		// find facebook with uid 33333
		// expected result is a list of 2 objects
		// sort desc on twitter.uid
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))

			.expect()
				.statusCode(200)

				.body("result", hasSize(2))
				.body("result_count", equalTo(2))
				.body("result[0].id",equalTo(test06))
				.body("result[1].id",equalTo(test07))
				.body("result[0].facebook.uid",equalTo("33333"))
				.body("result[1].facebook.uid",equalTo("33333"))
				.body("result[0].twitter.uid",equalTo("22222"))
				.body("result[1].twitter.uid",equalTo("33333"))

			.when()
				.get(concat("/test_group_prop_four/?facebook.uid=33333&sort=twitter.uid&order=desc"));

		// find twitter with uid 33333
		// expected result is a single object
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))

			.expect()
				.statusCode(200)

				.body("result", hasSize(1))
				.body("result_count", equalTo(1))
				.body("result[0].id", equalTo(test07))
				.body("result[0].twitter.uid",equalTo("33333"))
				.body("result[0].facebook.uid",equalTo("33333"))

			.when()
				.get(concat("/test_group_prop_four/?twitter.uid=33333"));
	}
}
