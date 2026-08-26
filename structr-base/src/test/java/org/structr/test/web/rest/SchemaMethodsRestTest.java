/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.test.web.rest;

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import java.util.Set;
import java.util.function.BiFunction;
import org.neo4j.function.TriFunction;
import org.structr.common.error.ErrorToken;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.*;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
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
				.headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
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
				.headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
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
				.headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
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
				.headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
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

			final NodeInterface testType = app.nodeQuery(StructrTraits.SCHEMA_NODE).name("TestType").getFirst();

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

	@Test
	public void test007AssertFunctionInfoFunctionWorksViaREST() {

		createAdminUser();

		final String testTypeName          = "TestType";
		final String userDefinedMethodName = "userDefinedMethod";
		final String staticMethodName      = "staticMethod";
		final String instanceMethodName    = "instanceMethod";

		final BiFunction<String, String, String> getFunctionBody = (String name, String context) -> """
		{
			let info = $.functionInfo();
			$.assert((info != null),       422, "functionInfo() should not return null in %s '%s'");
			$.assert((info.name === '%s'), 422, "functionInfo() should return the correct name in %s '%s'");
		}""".formatted(context, name, name, context, name);

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.SCHEMA_METHOD,
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),   userDefinedMethodName),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), getFunctionBody.apply(userDefinedMethodName, "user-defined method"))
			);

			final NodeInterface testType = app.create(StructrTraits.SCHEMA_NODE, new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_NODE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), testTypeName));
			final Set<String> lifecycleMethods = Set.of("onNodeCreation", "onCreate", "afterCreate", "onSave", "afterSave", "onDelete", "afterDelete");

			for (final String methodName : lifecycleMethods) {

				app.create(StructrTraits.SCHEMA_METHOD,
						new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),       methodName),
						new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY),      getFunctionBody.apply(methodName, "lifecycle method")),
						new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY), testType)
				);
			}

			app.create(StructrTraits.SCHEMA_METHOD,
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),       staticMethodName),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.IS_STATIC_PROPERTY),   true),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY),      getFunctionBody.apply(staticMethodName, "static method")),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY), testType)
			);

			app.create(StructrTraits.SCHEMA_METHOD,
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),       instanceMethodName),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY),      getFunctionBody.apply(instanceMethodName, "instance method")),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY), testType)
			);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception.");
		}

		// test user-defined function
		RestAssured
				.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
				.expect()
				.statusCode(200)
				.when()
				.post("/" + userDefinedMethodName);

		// test static method
		RestAssured
				.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
				.expect()
				.statusCode(200)
				.when()
				.post("/" + testTypeName + "/" + staticMethodName);

		// test onNodeCreation/onCreate/afterCreate
		final String uuid = createEntityAsUser(ADMIN_USERNAME, ADMIN_PASSWORD, "/" + testTypeName, "{ name: 'teeest' }");

		// test instance method
		RestAssured
				.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
				.expect()
				.statusCode(200)
				.when()
				.post("/" + testTypeName + "/" + uuid + "/" + instanceMethodName);

		// test onSave/afterSave
		RestAssured
				.given()
				.contentType("application/json; charset=UTF-8")
				.body("{ name: 'newName' }")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
				.expect()
				.statusCode(200)
				.when()
				.put("/" + testTypeName + "/" + uuid);

		// test onDelete/afterDelete
		RestAssured
				.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
				.expect()
				.statusCode(200)
				.when()
				.delete("/" + testTypeName + "/" + uuid);
	}

	@Test
	public void test008EnsureResponseKeywordDoesNotBreakRESTRequest() {

		final String expected = "yes, response keyword is not available via REST (and therefor does not break REST)";

		try (final Tx tx = app.tx()) {

			// create global schema which does not have any visibility flags
			app.create(StructrTraits.SCHEMA_METHOD, new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "myTestMethod01"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), """
				{
					let res = $.response;
					$.assert(res === null, 422, 'response keyword should not be available via REST, only in a render context (in a page)');
					return '%s';
				}
				""".formatted(expected))
			);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception.");
		}

		// test that resource access grant is required
		RestAssured
				.given()
				.contentType("application/json; charset=UTF-8")
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
				.expect()
				.statusCode(200)
				.body("result", equalTo(expected))
				.when()
				.post("/myTestMethod01");
	}

	@Test
	public void test009EnsureNullValuesInArrayWork() {

		createAdminUser();

		try (final Tx tx = app.tx()) {

			final PropertyKey<String> name   = Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
			final PropertyKey<String> source = Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY);

			app.create(StructrTraits.SCHEMA_METHOD, new NodeAttribute<>(name, "nullTestEmptyArray"), new NodeAttribute<>(source, "{ return []; }"));

			app.create(StructrTraits.SCHEMA_METHOD, new NodeAttribute<>(name, "nullTestArrayLen1_test1"), new NodeAttribute<>(source, "{ return [ 'test1' ]; }"));
			app.create(StructrTraits.SCHEMA_METHOD, new NodeAttribute<>(name, "nullTestArrayLen1_test2"), new NodeAttribute<>(source, "{ return [ null    ]; }"));

			app.create(StructrTraits.SCHEMA_METHOD, new NodeAttribute<>(name, "nullTestArrayLen2_test1"), new NodeAttribute<>(source, "{ return [ 'test1', 'test2' ]; }"));
			app.create(StructrTraits.SCHEMA_METHOD, new NodeAttribute<>(name, "nullTestArrayLen2_test2"), new NodeAttribute<>(source, "{ return [ 'test1', null    ]; }"));
			app.create(StructrTraits.SCHEMA_METHOD, new NodeAttribute<>(name, "nullTestArrayLen2_test3"), new NodeAttribute<>(source, "{ return [ null,    'test2' ]; }"));
			app.create(StructrTraits.SCHEMA_METHOD, new NodeAttribute<>(name, "nullTestArrayLen2_test4"), new NodeAttribute<>(source, "{ return [ null,     null   ]; }"));

			app.create(StructrTraits.SCHEMA_METHOD, new NodeAttribute<>(name, "nullTestArrayLen3_test1"), new NodeAttribute<>(source, "{ return [ 'test1', 'test2', 'test3' ]; }"));
			app.create(StructrTraits.SCHEMA_METHOD, new NodeAttribute<>(name, "nullTestArrayLen3_test2"), new NodeAttribute<>(source, "{ return [ 'test1', null,    'test3' ]; }"));
			app.create(StructrTraits.SCHEMA_METHOD, new NodeAttribute<>(name, "nullTestArrayLen3_test3"), new NodeAttribute<>(source, "{ return [ null,    'test2', 'test3' ]; }"));
			app.create(StructrTraits.SCHEMA_METHOD, new NodeAttribute<>(name, "nullTestArrayLen3_test4"), new NodeAttribute<>(source, "{ return [ null,    null,    'test3' ]; }"));
			app.create(StructrTraits.SCHEMA_METHOD, new NodeAttribute<>(name, "nullTestArrayLen3_test5"), new NodeAttribute<>(source, "{ return [ 'test1', 'test2',  null   ]; }"));
			app.create(StructrTraits.SCHEMA_METHOD, new NodeAttribute<>(name, "nullTestArrayLen3_test6"), new NodeAttribute<>(source, "{ return [ 'test1', null,     null   ]; }"));
			app.create(StructrTraits.SCHEMA_METHOD, new NodeAttribute<>(name, "nullTestArrayLen3_test7"), new NodeAttribute<>(source, "{ return [ null,    'test2',  null   ]; }"));
			app.create(StructrTraits.SCHEMA_METHOD, new NodeAttribute<>(name, "nullTestArrayLen3_test8"), new NodeAttribute<>(source, "{ return [ null,    null,     null   ]; }"));

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception.");
		}

		// empty array
		{
			RestAssured
					.given().contentType("application/json; charset=UTF-8").headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
					.expect().statusCode(200)
					.body("result", hasSize(0))
					.when().post("/nullTestEmptyArray");
		}

		// array with one entry
		{
			RestAssured
					.given().contentType("application/json; charset=UTF-8").headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
					.expect().statusCode(200)
					.body("result", hasSize(1))
					.body("result[0]", equalTo("test1"))
					.when().post("/nullTestArrayLen1_test1");

			RestAssured
					.given().contentType("application/json; charset=UTF-8").headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
					.expect().statusCode(200)
					.body("result", hasSize(1))
					.body("result[0]", equalTo(null))
					.when().post("/nullTestArrayLen1_test2");
		}

		// array with two entries
		{
			RestAssured
					.given().contentType("application/json; charset=UTF-8").headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
					.expect().statusCode(200)
					.body("result", hasSize(2))
					.body("result[0]", equalTo("test1"))
					.body("result[1]", equalTo("test2"))
					.when().post("/nullTestArrayLen2_test1");

			RestAssured
					.given().contentType("application/json; charset=UTF-8").headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
					.expect().statusCode(200)
					.body("result", hasSize(2))
					.body("result[0]", equalTo("test1"))
					.body("result[1]", equalTo(null))
					.when().post("/nullTestArrayLen2_test2");

			RestAssured
					.given().contentType("application/json; charset=UTF-8").headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
					.expect().statusCode(200)
					.body("result", hasSize(2))
					.body("result[0]", equalTo(null))
					.body("result[1]", equalTo("test2"))
					.when().post("/nullTestArrayLen2_test3");

			RestAssured
					.given().contentType("application/json; charset=UTF-8").headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
					.expect().statusCode(200)
					.body("result", hasSize(2))
					.body("result[0]", equalTo(null))
					.body("result[1]", equalTo(null))
					.when().post("/nullTestArrayLen2_test4");
		}

		// array with three entries
		{
			RestAssured
					.given().contentType("application/json; charset=UTF-8").headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
					.expect().statusCode(200)
					.body("result", hasSize(3))
					.body("result[0]", equalTo("test1"))
					.body("result[1]", equalTo("test2"))
					.body("result[2]", equalTo("test3"))
					.when().post("/nullTestArrayLen3_test1");

			RestAssured
					.given().contentType("application/json; charset=UTF-8").headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
					.expect().statusCode(200)
					.body("result", hasSize(3))
					.body("result[0]", equalTo("test1"))
					.body("result[1]", equalTo(null))
					.body("result[2]", equalTo("test3"))
					.when().post("/nullTestArrayLen3_test2");

			RestAssured
					.given().contentType("application/json; charset=UTF-8").headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
					.expect().statusCode(200)
					.body("result", hasSize(3))
					.body("result[0]", equalTo(null))
					.body("result[1]", equalTo("test2"))
					.body("result[2]", equalTo("test3"))
					.when().post("/nullTestArrayLen3_test3");

			RestAssured
					.given().contentType("application/json; charset=UTF-8").headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
					.expect().statusCode(200)
					.body("result", hasSize(3))
					.body("result[0]", equalTo(null))
					.body("result[1]", equalTo(null))
					.body("result[2]", equalTo("test3"))
					.when().post("/nullTestArrayLen3_test4");

			RestAssured
					.given().contentType("application/json; charset=UTF-8").headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
					.expect().statusCode(200)
					.body("result", hasSize(3))
					.body("result[0]", equalTo("test1"))
					.body("result[1]", equalTo("test2"))
					.body("result[2]", equalTo(null))
					.when().post("/nullTestArrayLen3_test5");

			RestAssured
					.given().contentType("application/json; charset=UTF-8").headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
					.expect().statusCode(200)
					.body("result", hasSize(3))
					.body("result[0]", equalTo("test1"))
					.body("result[1]", equalTo(null))
					.body("result[2]", equalTo(null))
					.when().post("/nullTestArrayLen3_test6");

			RestAssured
					.given().contentType("application/json; charset=UTF-8").headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
					.expect().statusCode(200)
					.body("result", hasSize(3))
					.body("result[0]", equalTo(null))
					.body("result[1]", equalTo("test2"))
					.body("result[2]", equalTo(null))
					.when().post("/nullTestArrayLen3_test7");

			RestAssured
					.given().contentType("application/json; charset=UTF-8").headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
					.expect().statusCode(200)
					.body("result", hasSize(3))
					.body("result[0]", equalTo(null))
					.body("result[1]", equalTo(null))
					.body("result[2]", equalTo(null))
					.when().post("/nullTestArrayLen3_test8");
		}
	}

	@Test
	public void test010EnsureNullValuesInArrayInputWork() {

		createAdminUser();

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.SCHEMA_METHOD,
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "myTestMethod01"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), "{ return $.methodParameters.input; }")
			);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception.");
		}

		final Function<String, String> toString = (String val) -> {

			if (val == null) {

				return "null";
			}

			return "'" + val + "'";
		};

		final Function<String, Boolean> testWithArraySizeOne = (String p1) -> {

			RestAssured
					.given()
					.body("{ input: [%s] }".formatted(toString.apply(p1)))
					.contentType("application/json; charset=UTF-8")
					.headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
					.expect()
					.statusCode(200)
					.body("result[0]", equalTo(p1))
					.when()
					.post("/myTestMethod01");

			return true;
		};

		final BiFunction<String, String, Boolean> testWithArraySizeTwo = (String p1, String p2) -> {

			RestAssured
					.given()
					.body("{ input: [%s, %s] }".formatted(toString.apply(p1), toString.apply(p2)))
					.contentType("application/json; charset=UTF-8")
					.headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
					.expect()
					.statusCode(200)
					.body("result[0]", equalTo(p1))
					.body("result[1]", equalTo(p2))
					.when()
					.post("/myTestMethod01");

			return true;
		};

		final TriFunction<String, String, String, Boolean> testWithArraySizeThree = (String p1, String p2, String p3) -> {

			RestAssured
					.given()
					.body("{ input: [%s, %s, %s] }".formatted(toString.apply(p1), toString.apply(p2),  toString.apply(p3)))
					.contentType("application/json; charset=UTF-8")
					.headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
					.expect()
					.statusCode(200)
					.body("result[0]", equalTo(p1))
					.body("result[1]", equalTo(p2))
					.body("result[2]", equalTo(p3))
					.when()
					.post("/myTestMethod01");

			return true;
		};

		testWithArraySizeOne.apply("test");
		testWithArraySizeOne.apply(null);

		testWithArraySizeTwo.apply("test1", "test2");
		testWithArraySizeTwo.apply("test1", null);
		testWithArraySizeTwo.apply(null, "test2");
		testWithArraySizeTwo.apply(null, null);

		testWithArraySizeThree.apply("test1", "test2", "test3");
		testWithArraySizeThree.apply("test1", null, "test3");
		testWithArraySizeThree.apply(null, "test2", "test3");
		testWithArraySizeThree.apply(null, null, "test3");
		testWithArraySizeThree.apply("test1", "test2", null);
		testWithArraySizeThree.apply("test1", null, null);
		testWithArraySizeThree.apply(null, "test2", null);
		testWithArraySizeThree.apply(null, null, null);
	}

	/**
	 * A method that returns NOTHING must answer {@code "result": null} -- not {@code [ null ]}, and
	 * not {@code []}. A method resource declares isCollection() == false, so nothing it returns is
	 * wrapped in an array; there is exactly one thing to write, and that thing is null.
	 *
	 * <p>This is the counterpart to test009: a method that returns a LIST containing null still
	 * answers {@code [ null ]}, because the array there is the method's own return value rather than
	 * a wrapper the serializer added. The two cases produced the same JSON before, which is what made
	 * the bug hard to name -- {@code [ null ]} was both a correct and an incorrect answer depending on
	 * which method produced it.</p>
	 */
	@Test
	public void test011AMethodThatReturnsNothingAnswersNull() {

		createAdminUser();

		try (final Tx tx = app.tx()) {

			final PropertyKey<String> name   = Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
			final PropertyKey<String> source = Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY);

			app.create(StructrTraits.SCHEMA_METHOD, new NodeAttribute<>(name, "nullTestEmptyBody"),   new NodeAttribute<>(source, "{}"));
			app.create(StructrTraits.SCHEMA_METHOD, new NodeAttribute<>(name, "nullTestReturnNull"),  new NodeAttribute<>(source, "{ return null; }"));
			app.create(StructrTraits.SCHEMA_METHOD, new NodeAttribute<>(name, "nullTestNoReturn"),    new NodeAttribute<>(source, "{ let unused = 1; }"));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		for (final String methodName : List.of("nullTestEmptyBody", "nullTestReturnNull", "nullTestNoReturn")) {

			RestAssured
					.given().contentType("application/json; charset=UTF-8").headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
					.expect().statusCode(200)
					.body("result",       equalTo(null))
					.body("result_count", equalTo(0))
					.when().post("/" + methodName);
		}
	}

	/**
	 * The regression guard for the fix above: an array the method actually RETURNED keeps its
	 * brackets, whatever is in it. Only the wrapper the serializer would have added is dropped, so
	 * shortening the "no result" case to null must not shorten these too.
	 */
	@Test
	public void test012AReturnedArrayKeepsItsBrackets() {

		createAdminUser();

		try (final Tx tx = app.tx()) {

			final PropertyKey<String> name   = Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
			final PropertyKey<String> source = Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY);

			app.create(StructrTraits.SCHEMA_METHOD, new NodeAttribute<>(name, "keepsBracketsEmpty"),      new NodeAttribute<>(source, "{ return []; }"));
			app.create(StructrTraits.SCHEMA_METHOD, new NodeAttribute<>(name, "keepsBracketsOneNull"),    new NodeAttribute<>(source, "{ return [ null ]; }"));
			app.create(StructrTraits.SCHEMA_METHOD, new NodeAttribute<>(name, "keepsBracketsEmptyObject"),new NodeAttribute<>(source, "{ return {}; }"));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// an empty returned array is an array, not "nothing"
		RestAssured
				.given().contentType("application/json; charset=UTF-8").headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
				.expect().statusCode(200)
				.body("result", hasSize(0))
				.when().post("/keepsBracketsEmpty");

		// a returned array holding one null is NOT the same as returning nothing
		RestAssured
				.given().contentType("application/json; charset=UTF-8").headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
				.expect().statusCode(200)
				.body("result",    hasSize(1))
				.body("result[0]", equalTo(null))
				.when().post("/keepsBracketsOneNull");

		// and an object stays an object
		RestAssured
				.given().contentType("application/json; charset=UTF-8").headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
				.expect().statusCode(200)
				.body("result", equalTo(Map.of()))
				.when().post("/keepsBracketsEmptyObject");
	}

	/**
	 * A method whose source was never set at all -- not an empty string, no property -- must behave
	 * like a method that returns nothing, not fail. ScriptMethod.execute() built its snippet as
	 * {@code "${" + source.trim() + "}"} without the null check that getSnippet() already had, so the
	 * call died with a NullPointerException and answered 500.
	 *
	 * <p>The UI writes an empty string rather than leaving the property unset, which is why this went
	 * unnoticed: the shape is reachable through the REST API and through a deployment import, not
	 * through the schema editor.</p>
	 */
	@Test
	public void test013AMethodWithNoSourceAtAllDoesNotFail() {

		createAdminUser();

		try (final Tx tx = app.tx()) {

			final PropertyKey<String> name   = Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
			final PropertyKey<String> source = Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY);

			// no SOURCE_PROPERTY at all -- the property is absent, not empty
			app.create(StructrTraits.SCHEMA_METHOD, new NodeAttribute<>(name, "methodWithoutSource"));

			// and the shape the schema editor actually writes, which must keep behaving the same
			app.create(StructrTraits.SCHEMA_METHOD, new NodeAttribute<>(name, "methodWithEmptySource"), new NodeAttribute<>(source, ""));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		for (final String methodName : List.of("methodWithoutSource", "methodWithEmptySource")) {

			RestAssured
					.given().contentType("application/json; charset=UTF-8").headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
					.expect().statusCode(200)
					.body("result",       equalTo(null))
					.body("result_count", equalTo(0))
					.when().post("/" + methodName);
		}
	}
}
