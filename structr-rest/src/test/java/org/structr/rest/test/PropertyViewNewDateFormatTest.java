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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.hasSize;
import org.structr.rest.common.StructrRestTest;
import org.structr.rest.entity.TestTwo;

/**
 *
 *
 */
public class PropertyViewNewDateFormatTest extends StructrRestTest {

	@Override
	protected void setUp() throws Exception {
		super.setUp(toMap("DateProperty.defaultFormat", "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
	}
	
	public void testPropertyViewsAndResultSetLayout() {

		
		
		String resource = "/test_twos";

		// create entity
		final String uuid = getUuidFromLocation(RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header("Accept", "application/json; charset=UTF-8")
				.body(" { 'name' : 'TestTwo-0', 'anInt' : 0, 'aLong' : 0, 'aDate' : '2012-09-18T00:33:12+0200' } ")

			.expect()
				.statusCode(201)

			.when()
				.post(resource).getHeader("Location")
		);

		// test default view with properties in it
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header("Accept", "application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))

			.expect()
				.statusCode(200)
				.body("query_time",                 notNullValue())
				.body("serialization_time",         notNullValue())
				.body("result_count",               equalTo(1))
				.body("result",                     hasSize(1))

				.body("result[0]",                  isEntity(TestTwo.class))

				.body("result[0].id",               equalTo(uuid))
				.body("result[0].type",	            equalTo(TestTwo.class.getSimpleName()))
				.body("result[0].name",             equalTo("TestTwo-0"))
				.body("result[0].anInt",            equalTo(0))
				.body("result[0].aLong",            equalTo(0))
				.body("result[0].aDate",            equalTo("2012-09-17T22:33:12.000Z"))

			.when()
				.get(resource);


		// test all view with properties in it
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header("Accept", "application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))

			.expect()
				.statusCode(200)
				.body("query_time",                            notNullValue())
				.body("serialization_time",                    notNullValue())
				.body("result_count",                          equalTo(1))
				.body("result",                                hasSize(1))

				.body("result[0]",                             isEntity(TestTwo.class))

				.body("result[0].id",                          equalTo(uuid))
				.body("result[0].type",	                       equalTo(TestTwo.class.getSimpleName()))
				.body("result[0].name",                        equalTo("TestTwo-0"))
				.body("result[0].anInt",                       equalTo(0))
				.body("result[0].aLong",                       equalTo(0))
				.body("result[0].aDate",                       equalTo("2012-09-17T22:33:12.000Z"))
				.body("result[0].test_ones",                   notNullValue())
				.body("result[0].base",                        nullValue())
				.body("result[0].createdDate",                 notNullValue())
				.body("result[0].lastModifiedDate",            notNullValue())
				.body("result[0].visibleToPublicUsers",        equalTo(false))
				.body("result[0].visibleToAuthenticatedUsers", equalTo(false))
				.body("result[0].visibilityStartDate",         nullValue())
				.body("result[0].visibilityEndDate",           nullValue())
				.body("result[0].createdBy",                   nullValue())
				.body("result[0].deleted",                     equalTo(false))
				.body("result[0].hidden",                      equalTo(false))
				.body("result[0].owner",                       nullValue())
				.body("result[0].ownerId",                     nullValue())

			.when()
				.get(concat(resource, "/all"));



	}
}
