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
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.ResourceAccessTraitDefinition;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.core.traits.definitions.SchemaPropertyTraitDefinition;
import org.structr.core.traits.definitions.SchemaViewTraitDefinition;
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
			app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "myTestMethod01"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), "'hello world!'")
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

			app.create(StructrTraits.RESOURCE_ACCESS,
				new NodeAttribute<>(Traits.of(StructrTraits.RESOURCE_ACCESS).key(ResourceAccessTraitDefinition.SIGNATURE_PROPERTY), "myTestMethod01"),
				new NodeAttribute<>(Traits.of(StructrTraits.RESOURCE_ACCESS).key(ResourceAccessTraitDefinition.FLAGS_PROPERTY), 64L),
				new NodeAttribute<>(Traits.of(StructrTraits.RESOURCE_ACCESS).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), true)
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

			final NodeInterface testType = app.create(StructrTraits.SCHEMA_NODE, "MyTestType");

			app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY),testType),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),"testTypeMethod01"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), "'MyTestType.testTypeMethod01 here'"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.IS_STATIC_PROPERTY),true)
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

			app.create(StructrTraits.RESOURCE_ACCESS,
				new NodeAttribute<>(Traits.of(StructrTraits.RESOURCE_ACCESS).key(ResourceAccessTraitDefinition.SIGNATURE_PROPERTY), "MyTestType/testTypeMethod01"),
				new NodeAttribute<>(Traits.of(StructrTraits.RESOURCE_ACCESS).key(ResourceAccessTraitDefinition.FLAGS_PROPERTY), 64L),
				new NodeAttribute<>(Traits.of(StructrTraits.RESOURCE_ACCESS).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), true)
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

			final NodeInterface testType = app.create(StructrTraits.SCHEMA_NODE, new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_NODE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "TestType"));

			// create private method that is not exported via REST
			app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "myTestMethod01"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), "'hello world!'"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY), testType)
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

			final NodeInterface testType = app.create(StructrTraits.SCHEMA_NODE, new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_NODE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "TestType"));

			// create lifecycle method that is not exported via REST
			app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),"onCreate"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), "{ $.log('hello world!'); }"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY),testType)
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

			final NodeInterface testType = app.create(StructrTraits.SCHEMA_NODE, new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_NODE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "TestType"));

			// create private method that is not exported via REST
			app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),       "testMethod"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.IS_PRIVATE_PROPERTY),  true),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY),      "{ $.log('hello world!'); }"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY), testType)
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

			final NodeInterface testType = app.create(StructrTraits.SCHEMA_NODE, new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_NODE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "TestType"));

			// create private method that is not exported via REST
			app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),       "testMethod"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.IS_STATIC_PROPERTY),   true),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.IS_PRIVATE_PROPERTY),  true),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY),      "{ $.log('hello world!'); }"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY), testType)
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

			final NodeInterface testType = app.create(StructrTraits.SCHEMA_NODE, new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_NODE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "TestType"));
			final NodeInterface testView = app.create(StructrTraits.SCHEMA_VIEW,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.SCHEMA_NODE_PROPERTY), testType),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "test")
			);

			final NodeInterface p1   = app.create(StructrTraits.SCHEMA_PROPERTY,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY), testType),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_VIEWS_PROPERTY), List.of(testView)),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "String"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "moep")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface testType = app.nodeQuery(StructrTraits.SCHEMA_NODE).andName("TestType").getFirst();

			// create private method that is not exported via REST
			app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),      "test"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.IS_STATIC_PROPERTY),   true),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.IS_PRIVATE_PROPERTY),  true),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY),     "{ $.log('hello world!'); }"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY), testType)
			);

			tx.success();

			fail("Creating a method with the same name as a view should fail.");

		} catch (FrameworkException fex) {

			assertEquals("Wrong error code in response",    422,                                               fex.getStatus());
			assertEquals("Wrong error message in response", "Unable to commit transaction, validation failed", fex.getMessage());

			final ErrorToken token = fex.getErrorBuffer().getErrorTokens().get(0);

			assertEquals("Wrong property name in error response", "name",                                          token.getProperty());
			assertEquals("Wrong type in error response", StructrTraits.SCHEMA_METHOD,                                      token.getType());
			assertEquals("Wrong token in error response", "already_exists",                                        token.getToken());
			assertEquals("Wrong detail message in error response", "A view with name 'test' already exists, cannot create method with the same name", token.getDetail());
		}
	}
}
