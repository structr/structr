/*
 * Copyright (C) 2010-2021 Structr GmbH
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
package org.structr.test.rest.resource;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import static org.hamcrest.Matchers.equalTo;
import org.testng.annotations.Test;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.test.rest.common.StructrRestTestBase;
import static org.testng.AssertJUnit.fail;

/**
 *
 *
 */
public class GlobalSchemaMethodResourceTest extends StructrRestTestBase {

	@Test
	public void testFailedGlobalSchemaMethodCall() {

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
				.statusCode(400)
			.when()
				.post(concat("/maintenance/globalSchemaMethods/nonexistingTestMethod"));

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
				.statusCode(400)
			.when()
				.post(concat("/maintenance/globalSchemaMethods"));

	}

	@Test
	public void test001SimpleGlobalSchemaMethodCallViaRest() {

		try (final Tx tx = app.tx()) {

			app.create(SchemaMethod.class,
				new NodeAttribute<>(SchemaMethod.name, "myTestMethod01"),
				new NodeAttribute<>(SchemaMethod.source, "'hello world!'")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
				.statusCode(200)
				.body("result", equalTo("hello world!"))
			.when()
				.post(concat("/maintenance/globalSchemaMethods/myTestMethod01"));

	}

	@Test
	public void test002GlobalSchemaMethodCallViaRest() {

		try (final Tx tx = app.tx()) {

			app.create(SchemaMethod.class,
				new NodeAttribute<>(SchemaMethod.name, "myTestMethod02"),
				new NodeAttribute<>(SchemaMethod.source, "{ return { name: 'test', count: 1 }}")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
				.statusCode(200)
				.body("result.name",  equalTo("test"))
				.body("result.count", equalTo(1))
			.when()
				.post(concat("/maintenance/globalSchemaMethods/myTestMethod02"));

	}
}
