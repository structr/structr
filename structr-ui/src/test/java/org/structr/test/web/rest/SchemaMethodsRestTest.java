/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.structr.common.error.ErrorToken;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.Traits;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

public class SchemaMethodsRestTest extends StructrUiTest {

	@Test
	public void test001SimpleGlobalSchemaMethodCallViaRestAsPublicUser() {

		try (final Tx tx = app.tx()) {

			// create global schema which does not have any visibility flags
			app.create("SchemaMethod",
				new NodeAttribute<>(Traits.of("SchemaMethod").key("name"), "myTestMethod01"),
				new NodeAttribute<>(Traits.of("SchemaMethod").key("source"), "'hello world!'")
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
				.post("/myTestMethod01");


		// Add Grant and allow POST for public users
		try (final Tx tx = app.tx()) {

			app.create("ResourceAccess",
				new NodeAttribute<>(Traits.of("ResourceAccess").key("signature"), "myTestMethod01"),
				new NodeAttribute<>(Traits.of("ResourceAccess").key("flags"), 64L),
				new NodeAttribute<>(Traits.of("ResourceAccess").key("visibleToPublicUsers"), true)
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
			 .post("/myTestMethod01");
	}

	@Test
	public void test002SimpleStaticSchemaMethodCallViaRestAsPublicUser() {

		try (final Tx tx = app.tx()) {

			final NodeInterface testType = app.create("SchemaNode", "MyTestType");

			app.create("SchemaMethod",
				new NodeAttribute<>(Traits.of("SchemaMethod").key("schemaNode"),testType),
				new NodeAttribute<>(Traits.of("SchemaMethod").key("name"),"testTypeMethod01"),
				new NodeAttribute<>(Traits.of("SchemaMethod").key("source"), "'MyTestType.testTypeMethod01 here'"),
				new NodeAttribute<>(Traits.of("SchemaMethod").key("isStatic"),true)
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

			app.create("ResourceAccess",
				new NodeAttribute<>(Traits.of("ResourceAccess").key("signature"), "MyTestType/testTypeMethod01"),
				new NodeAttribute<>(Traits.of("ResourceAccess").key("flags"), 64L),
				new NodeAttribute<>(Traits.of("ResourceAccess").key("visibleToPublicUsers"), true)
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

	@Test
	public void test003PrivateSchemaMethodCallViaRest() {

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");

		try (final Tx tx = app.tx()) {

			final NodeInterface testType = app.create("SchemaNode", new NodeAttribute<>(Traits.of("SchemaNode").key("name"), "TestType"));

			// create private method that is not exported via REST
			app.create("SchemaMethod",
				new NodeAttribute<>(Traits.of("SchemaMethod").key("name"), "myTestMethod01"),
				new NodeAttribute<>(Traits.of("SchemaMethod").key("source"), "'hello world!'"),
				new NodeAttribute<>(Traits.of("SchemaMethod").key("schemaNode"), testType)
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
				.headers("x-user", "admin", "x-password", "admin")
			.expect()
				.statusCode(404)
			.when()
				.post("/myTestMethod01");

	}

	@Test
	public void test004AssertLifecycleMethodsNotAvailableViaRest() {

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");

		try (final Tx tx = app.tx()) {

			final NodeInterface testType = app.create("SchemaNode", new NodeAttribute<>(Traits.of("SchemaNode").key("name"), "TestType"));

			// create lifecycle method that is not exported via REST
			app.create("SchemaMethod",
				new NodeAttribute<>(Traits.of("SchemaMethod").key("name"),"onCreate"),
				new NodeAttribute<>(Traits.of("SchemaMethod").key("source"), "{ $.log('hello world!'); }"),
				new NodeAttribute<>(Traits.of("SchemaMethod").key("schemaNode"),testType)
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		final String uuid = createEntityAsUser("admin", "admin", "/TestType", "{ name: 'teeest' }");

		// test that resource access grant is required
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.headers("x-user", "admin", "x-password", "admin")
			.expect()
				.statusCode(404)
			.when()
				.post("/TestType/" + uuid + "/onCreate");

	}

	@Test
	public void test005PrivateMethodsNotAvailableViaRest() {

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");

		try (final Tx tx = app.tx()) {

			final NodeInterface testType = app.create("SchemaNode", new NodeAttribute<>(Traits.of("SchemaNode").key("name"), "TestType"));

			// create private method that is not exported via REST
			app.create("SchemaMethod",
				new NodeAttribute<>(Traits.of("SchemaMethod").key("name"),      "testMethod"),
				new NodeAttribute<>(Traits.of("SchemaMethod").key("isPrivate"), true),
				new NodeAttribute<>(Traits.of("SchemaMethod").key("source"),    "{ $.log('hello world!'); }"),
				new NodeAttribute<>(Traits.of("SchemaMethod").key("schemaNode"),testType)
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		final String uuid = createEntityAsUser("admin", "admin", "/TestType", "{ name: 'teeest' }");

		// test that resource access grant is required
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.headers("x-user", "admin", "x-password", "admin")
			.expect()
				.statusCode(404)
			.when()
				.post("/TestType/" + uuid + "/testMethod");

	}

	@Test
	public void test005PrivateStaticMethodsNotAvailableViaRest() {

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");

		try (final Tx tx = app.tx()) {

			final NodeInterface testType = app.create("SchemaNode", new NodeAttribute<>(Traits.of("SchemaNode").key("name"), "TestType"));

			// create private method that is not exported via REST
			app.create("SchemaMethod",
				new NodeAttribute<>(Traits.of("SchemaMethod").key("name"),      "testMethod"),
				new NodeAttribute<>(Traits.of("SchemaMethod").key("isStatic"),  true),
				new NodeAttribute<>(Traits.of("SchemaMethod").key("isPrivate"), true),
				new NodeAttribute<>(Traits.of("SchemaMethod").key("source"),    "{ $.log('hello world!'); }"),
				new NodeAttribute<>(Traits.of("SchemaMethod").key("schemaNode"),testType)
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		final String uuid = createEntityAsUser("admin", "admin", "/TestType", "{ name: 'teeest' }");

		// test that resource access grant is required
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.headers("x-user", "admin", "x-password", "admin")
			.expect()
				.statusCode(404)
			.when()
				.post("/TestType/testMethod");

	}

	@Test
	public void test006MethodVersusViewNameValidation() {

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");

		try (final Tx tx = app.tx()) {

			final NodeInterface testType = app.create("SchemaNode", new NodeAttribute<>(Traits.of("SchemaNode").key("name"), "TestType"));
			final NodeInterface testView = app.create("SchemaView",
				new NodeAttribute<>(Traits.of("SchemaView").key("schemaNode"), testType),
				new NodeAttribute<>(Traits.of("SchemaView").key("name"), "test")
			);

			final NodeInterface p1   = app.create("SchemaProperty",
				new NodeAttribute<>(Traits.of("SchemaProperty").key("schemaNode"), testType),
				new NodeAttribute<>(Traits.of("SchemaProperty").key("schemaViews"), List.of(testView)),
				new NodeAttribute<>(Traits.of("SchemaProperty").key("propertyType"), "String"),
				new NodeAttribute<>(Traits.of("SchemaProperty").key("name"), "moep")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface testType = app.nodeQuery("SchemaNode").andName("TestType").getFirst();

			// create private method that is not exported via REST
			app.create("SchemaMethod",
				new NodeAttribute<>(Traits.of("SchemaMethod").key("name"),      "test"),
				new NodeAttribute<>(Traits.of("SchemaMethod").key("isStatic"),  true),
				new NodeAttribute<>(Traits.of("SchemaMethod").key("isPrivate"), true),
				new NodeAttribute<>(Traits.of("SchemaMethod").key("source"),    "{ $.log('hello world!'); }"),
				new NodeAttribute<>(Traits.of("SchemaMethod").key("schemaNode"),testType)
			);

			tx.success();

			fail("Creating a method with the same name as a view should fail.");

		} catch (FrameworkException fex) {

			assertEquals("Wrong error code in response",    422,                                               fex.getStatus());
			assertEquals("Wrong error message in response", "Unable to commit transaction, validation failed", fex.getMessage());

			final ErrorToken token = fex.getErrorBuffer().getErrorTokens().get(0);

			assertEquals("Wrong property name in error response", "name",                                          token.getProperty());
			assertEquals("Wrong type in error response", "SchemaMethod",                                           token.getType());
			assertEquals("Wrong token in error response", "already_exists",                                        token.getToken());
			assertEquals("Wrong detail message in error response", "A method cannot have the same name as a view", token.getDetail());
		}
	}
}
