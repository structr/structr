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
package org.structr.rest.resource;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.rest.common.StructrRestTest;
import org.structr.rest.entity.TestOne;

/**
 *
 *
 */
public class StaticRelationshipResourceTest extends StructrRestTest {

	public void testInvokeMethodResult() {

		String id = null;

		try (final Tx tx = app.tx()) {

			final TestOne test = app.create(TestOne.class);

			// store ID for later use
			id = test.getUuid();

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// execute test method, expect sane result (not 500)
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.when()
			.post(concat("/test_ones/", id, "/test01"));


		// execute test method, expect sane result (not 500)
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.when()
			.post(concat("/test_ones/", id, "/test02"));


		// execute test method, expect sane result (not 500)
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.when()
			.post(concat("/test_ones/", id, "/test03"));


		// execute test method, expect sane result (not 500)
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(200)
			.when()
			.post(concat("/test_ones/", id, "/test04"));

	}

}
