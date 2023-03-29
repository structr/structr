/*
 * Copyright (C) 2010-2023 Structr GmbH
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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.test.web.rest;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.testng.AssertJUnit.fail;

public class SchemaMethodsTest extends StructrUiTest {

	@Test
	public void test001SimpleGlobalSchemaMethodCallViaRestAsPublicUser() {

		try (final Tx tx = app.tx()) {

			// create global schema which does not have any visibility flags
			app.create(SchemaMethod.class,
				new NodeAttribute<>(SchemaMethod.name, "myTestMethod01"),
				new NodeAttribute<>(SchemaMethod.source, "'hello world!'")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test that resource access grant is required
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
				.statusCode(401)
			.when()
				.post("/globalSchemaMethods/myTestMethod01");


		// Add Grant and allow POST for public users
		try (final Tx tx = app.tx()) {

			app.create(ResourceAccess.class,
				new NodeAttribute<>(ResourceAccess.signature, "MyTestMethod01"),
				new NodeAttribute<>(ResourceAccess.flags, 64L),
				new NodeAttribute<>(ResourceAccess.visibleToPublicUsers, true)
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test that the call succeeds with the grant
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
				.statusCode(200)
				.body("result", equalTo("hello world!"))
			.when()
			 .post("/globalSchemaMethods/myTestMethod01");
	}

	@Test
	public void test002SimpleStaticSchemaMethodCallViaRestAsPublicUser() {

		try (final Tx tx = app.tx()) {

			final SchemaNode testType = app.create(SchemaNode.class, "MyTestType");

			app.create(SchemaMethod.class,
				new NodeAttribute<>(SchemaMethod.schemaNode, testType),
				new NodeAttribute<>(SchemaMethod.name, "testTypeMethod01"),
				new NodeAttribute<>(SchemaMethod.source, "'MyTestType.testTypeMethod01 here'")
			);

			tx.success();

		} catch (FrameworkException ex) {
			fail("Error creating schema node");
		}

		// test that resource access grant is required
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
				.statusCode(401)
			.when()
				.post("/MyTestType/testTypeMethod01");


		// Add Grant and allow POST for public users
		try (final Tx tx = app.tx()) {

			app.create(ResourceAccess.class,
				new NodeAttribute<>(ResourceAccess.signature, "MyTestType/TestTypeMethod01"),
				new NodeAttribute<>(ResourceAccess.flags, 64L),
				new NodeAttribute<>(ResourceAccess.visibleToPublicUsers, true)
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test that the call succeeds with the grant
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
				.statusCode(200)
				.body("result", equalTo("MyTestType.testTypeMethod01 here"))
			.when()
				.post("/MyTestType/testTypeMethod01");
	}
}
