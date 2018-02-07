/**
 * Copyright (C) 2010-2018 Structr GmbH
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
import static org.hamcrest.Matchers.equalTo;
import org.junit.Test;
import org.structr.rest.common.StructrRestTest;

/**
 *
 *
 */
public class FunctionPropertyTest extends StructrRestTest {

	@Test
	public void testFunctionProperty() {

		final String uuid = createEntity("/TestTen");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
		.expect()
			.statusCode(200)
			.body("result[0].type",                               equalTo("TestTen"))
			.body("result[0].id",                                 equalTo(uuid))
			.body("result[0].functionTest.name",                  equalTo("test"))
			.body("result[0].functionTest.value",                 equalTo(123))
			.body("result[0].functionTest.me.type",               equalTo("TestTen"))
			.body("result[0].functionTest.me.id",                 equalTo(uuid))
			.body("result[0].functionTest.me.functionTest.name",  equalTo("test"))
			.body("result[0].functionTest.me.functionTest.value", equalTo(123))
		.when()
			.get("/TestTen");

	}
}
