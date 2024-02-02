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
import java.util.List;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;
import org.structr.common.error.ErrorToken;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.entity.SchemaView;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

public class SchemaMethodsRestTest extends StructrUiTest {

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
				.post("/myTestMethod01");


		// Add Grant and allow POST for public users
		try (final Tx tx = app.tx()) {

			app.create(ResourceAccess.class,
				new NodeAttribute<>(ResourceAccess.signature, "myTestMethod01"),
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
			 .post("/myTestMethod01");
	}

	@Test
	public void test002SimpleStaticSchemaMethodCallViaRestAsPublicUser() {

		try (final Tx tx = app.tx()) {

			final SchemaNode testType = app.create(SchemaNode.class, "MyTestType");

			app.create(SchemaMethod.class,
				new NodeAttribute<>(SchemaMethod.schemaNode, testType),
				new NodeAttribute<>(SchemaMethod.name, "testTypeMethod01"),
				new NodeAttribute<>(SchemaMethod.source, "'MyTestType.testTypeMethod01 here'"),
				new NodeAttribute<>(SchemaMethod.isStatic, true)
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
				new NodeAttribute<>(ResourceAccess.signature, "MyTestType/testTypeMethod01"),
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

	@Test
	public void test003PrivateSchemaMethodCallViaRest() {

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");

		try (final Tx tx = app.tx()) {

			final SchemaNode testType = app.create(SchemaNode.class, new NodeAttribute<>(SchemaNode.name, "TestType"));

			// create private method that is not exported via REST
			app.create(SchemaMethod.class,
				new NodeAttribute<>(SchemaMethod.name, "myTestMethod01"),
				new NodeAttribute<>(SchemaMethod.source, "'hello world!'"),
				new NodeAttribute<>(SchemaMethod.schemaNode, testType)
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

			final SchemaNode testType = app.create(SchemaNode.class, new NodeAttribute<>(SchemaNode.name, "TestType"));

			// create lifecycle method that is not exported via REST
			app.create(SchemaMethod.class,
				new NodeAttribute<>(SchemaMethod.name, "onCreate"),
				new NodeAttribute<>(SchemaMethod.source, "{ $.log('hello world!'); }"),
				new NodeAttribute<>(SchemaMethod.schemaNode, testType)
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

			final SchemaNode testType = app.create(SchemaNode.class, new NodeAttribute<>(SchemaNode.name, "TestType"));

			// create private method that is not exported via REST
			app.create(SchemaMethod.class,
				new NodeAttribute<>(SchemaMethod.name,       "testMethod"),
				new NodeAttribute<>(SchemaMethod.isPrivate,  true),
				new NodeAttribute<>(SchemaMethod.source,     "{ $.log('hello world!'); }"),
				new NodeAttribute<>(SchemaMethod.schemaNode, testType)
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

			final SchemaNode testType = app.create(SchemaNode.class, new NodeAttribute<>(SchemaNode.name, "TestType"));

			// create private method that is not exported via REST
			app.create(SchemaMethod.class,
				new NodeAttribute<>(SchemaMethod.name,       "testMethod"),
				new NodeAttribute<>(SchemaMethod.isStatic,   true),
				new NodeAttribute<>(SchemaMethod.isPrivate,  true),
				new NodeAttribute<>(SchemaMethod.source,     "{ $.log('hello world!'); }"),
				new NodeAttribute<>(SchemaMethod.schemaNode, testType)
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

			final SchemaNode testType = app.create(SchemaNode.class, new NodeAttribute<>(SchemaNode.name, "TestType"));
			final SchemaView testView = app.create(SchemaView.class,
				new NodeAttribute<>(SchemaView.schemaNode, testType),
				new NodeAttribute<>(SchemaView.name, "test")
			);

			final SchemaProperty p1   = app.create(SchemaProperty.class,
				new NodeAttribute<>(SchemaProperty.schemaNode, testType),
				new NodeAttribute<>(SchemaProperty.schemaViews, List.of(testView)),
				new NodeAttribute<>(SchemaProperty.propertyType, "String"),
				new NodeAttribute<>(SchemaProperty.name, "moep")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final SchemaNode testType = app.nodeQuery(SchemaNode.class).andName("TestType").getFirst();

			// create private method that is not exported via REST
			app.create(SchemaMethod.class,
				new NodeAttribute<>(SchemaMethod.name,       "test"),
				new NodeAttribute<>(SchemaMethod.isStatic,   true),
				new NodeAttribute<>(SchemaMethod.isPrivate,  true),
				new NodeAttribute<>(SchemaMethod.source,     "{ $.log('hello world!'); }"),
				new NodeAttribute<>(SchemaMethod.schemaNode, testType)
			);

			tx.success();

			fail("Creating a method with the same name as a view should fail.");

		} catch (FrameworkException fex) {

			assertEquals("Wrong error code in response",    422,                                                                fex.getStatus());
			assertEquals("Wrong error message in response", "Unable to commit transaction, transaction post processing failed", fex.getMessage());

			final ErrorToken token = fex.getErrorBuffer().getErrorTokens().get(0);

			assertEquals("Wrong property name in error response", "name",                                          token.getProperty());
			assertEquals("Wrong type in error response", "SchemaMethod",                                           token.getType());
			assertEquals("Wrong token in error response", "already_exists",                                        token.getToken());
			assertEquals("Wrong detail message in error response", "A method cannot have the same name as a view", token.getDetail());
		}
	}
}
