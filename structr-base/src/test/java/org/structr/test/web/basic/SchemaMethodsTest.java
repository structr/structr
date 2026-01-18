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
package org.structr.test.web.basic;

import io.restassured.RestAssured;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.schema.export.StructrSchema;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.testng.AssertJUnit.fail;

public class SchemaMethodsTest extends FrontendTest {

	private static final Logger logger = LoggerFactory.getLogger(SchemaMethodsTest.class.getName());

	@Test
	public void test01SchemaMethodOnBuiltinType() {

		final String builtinTypeName = StructrTraits.FILE;
		final String schemaMethodName = "testFileMethodOnType";

		try (final Tx tx = app.tx()) {

			final PropertyMap testFileMethodProperties = new PropertyMap();
			testFileMethodProperties.put(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),                   schemaMethodName);
			testFileMethodProperties.put(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.STATIC_SCHEMA_NODE_NAME_PROPERTY), StructrTraits.FILE);
			testFileMethodProperties.put(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY),                  "(log('test01SchemaMethodOnBuiltinType successful'))");
			testFileMethodProperties.put(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.IS_STATIC_PROPERTY),               true);

			final NodeInterface testFileMethod = app.create(StructrTraits.SCHEMA_METHOD, testFileMethodProperties);

			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		NodeInterface admin = null;
		try (final Tx tx = app.tx()) {

			admin = createAdminUser();
			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.headers(X_USER_HEADER, ADMIN_USERNAME , X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.body("{}")
			.expect()
				.statusCode(200)

			.when()
				.post(builtinTypeName +"/" + schemaMethodName);
}

	@Test
	public void test02SchemaMethodOnCustomType() {

		final String customTypeName = "FooFile";
		final String schemaMethodName = "testFooFileMethodOnType";

		try (final Tx tx = app.tx()) {

			// Add schema method "testFileMethod" to built-in File class
			final NodeInterface fooFileDef = app.create(StructrTraits.SCHEMA_NODE, customTypeName);

			final PropertyMap testFooFileMethodProperties = new PropertyMap();
			testFooFileMethodProperties.put(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),      schemaMethodName);
			testFooFileMethodProperties.put(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY),    "(log('test02SchemaMethodOnCustomType successful'))");
			testFooFileMethodProperties.put(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY), fooFileDef);
			testFooFileMethodProperties.put(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.IS_STATIC_PROPERTY),   true);

			app.create(StructrTraits.SCHEMA_METHOD, testFooFileMethodProperties);

			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			createAdminUser();
			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			RestAssured

				.given()
					.contentType("application/json; charset=UTF-8")
					.headers(X_USER_HEADER, ADMIN_USERNAME , X_PASSWORD_HEADER, ADMIN_PASSWORD)
				.body("{}")
				.expect()
					.statusCode(200)

				.when()
					.post(customTypeName + "/" + schemaMethodName);

			tx.success();

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}
	}

	@Test
	public void test03SchemaMethodOnEntityOfBuiltinType() {

		final String builtinTypeName = StructrTraits.FILE;
		final String schemaMethodName = "testFileMethodOnEntity";

		NodeInterface admin = null;
		try (final Tx tx = app.tx()) {

			admin = createAdminUser();
			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			// Add schema method "testFileMethod" to built-in File class
			final PropertyMap testFileMethodProperties = new PropertyMap();
			testFileMethodProperties.put(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), schemaMethodName);
			testFileMethodProperties.put(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), "(log('test03SchemaMethodOnEntityOfBuiltinType successful'))");
			testFileMethodProperties.put(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.STATIC_SCHEMA_NODE_NAME_PROPERTY), builtinTypeName);

			app.create(StructrTraits.SCHEMA_METHOD, testFileMethodProperties);

			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		NodeInterface testFile = null;
		try (final Tx tx = app.tx()) {

			// Create File instance
			testFile = app.create(StructrTraits.FILE, "Test File");
			testFile.setProperty(Traits.of(StructrTraits.FILE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY), admin);

			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			RestAssured

				.given()
					.contentType("application/json; charset=UTF-8")
					.headers(X_USER_HEADER, ADMIN_USERNAME , X_PASSWORD_HEADER, ADMIN_PASSWORD)
				.body("{}")
				.expect()
					.statusCode(200)

				.when()
					.post(builtinTypeName + "/" + testFile.getUuid() + "/" + schemaMethodName);

			tx.success();

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}
	}

	@Test
	public void test04SchemaMethodOnEntityOfCustomType() {

		final String customTypeName = "FooFileNew";
		final String schemaMethodName = "testFooFileMethodOnEntity";

		try (final Tx tx = app.tx()) {

			// Add schema method "testFooFileMethodOnEntity" to built-in File class
			final NodeInterface fooFileDef = app.create(StructrTraits.SCHEMA_NODE, customTypeName);

			final PropertyMap testFooFileMethodProperties = new PropertyMap();
			testFooFileMethodProperties.put(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), schemaMethodName);
			testFooFileMethodProperties.put(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), "(log('test04SchemaMethodOnEntityOfCustomType successful'))");
			testFooFileMethodProperties.put(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY), fooFileDef);

			app.create(StructrTraits.SCHEMA_METHOD, testFooFileMethodProperties);

			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			createAdminUser();
			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		String id = createEntityAsSuperUser(customTypeName, "{'name':'Test Foo File'}");

		try (final Tx tx = app.tx()) {

			RestAssured

				.given()
					.contentType("application/json; charset=UTF-8")
					.headers(X_USER_HEADER, ADMIN_USERNAME , X_PASSWORD_HEADER, ADMIN_PASSWORD)
				.body("{}")
				.expect()
					.statusCode(200)

				.when()
					.post(customTypeName + "/" + id + "/" + schemaMethodName);

			tx.success();

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}
	}

	@Test
	public void test05InheritedSchemaMethodOnBuiltInType() {

		final String builtinTypeName = StructrTraits.FILE;
		final String schemaMethodName = "testFileMethodOnInheritingType";

		NodeInterface admin = null;
		try (final Tx tx = app.tx()) {

			admin = createAdminUser();
			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			// Add schema method "testFileMethod" to built-in File class
			final PropertyMap testFileMethodProperties = new PropertyMap();
			testFileMethodProperties.put(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), schemaMethodName);
			testFileMethodProperties.put(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), "(log('test05InheritedSchemaMethodOnBuildinType successful'))");
			testFileMethodProperties.put(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.STATIC_SCHEMA_NODE_NAME_PROPERTY), builtinTypeName);
			testFileMethodProperties.put(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.IS_STATIC_PROPERTY), true);

			app.create(StructrTraits.SCHEMA_METHOD, testFileMethodProperties);

			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			RestAssured

				.given()
					.contentType("application/json; charset=UTF-8")
					.headers(X_USER_HEADER, ADMIN_USERNAME , X_PASSWORD_HEADER, ADMIN_PASSWORD)
				.body("{}")
				.expect()
					.statusCode(200)

				.when()
					.post("/Image/" + schemaMethodName);

			tx.success();

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}

	}

	@Test
	public void test06InheritedSchemaMethodOnEntityOfBuiltinType() {

		final String builtinTypeName = StructrTraits.FILE;
		final String schemaMethodName = "testFileMethodOnInheritingEntity";

		NodeInterface admin = null;
		try (final Tx tx = app.tx()) {

			admin = createAdminUser();
			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			// Add schema method "testFileMethod" to built-in File class
			final PropertyMap testFileMethodProperties = new PropertyMap();
			testFileMethodProperties.put(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), schemaMethodName);
			testFileMethodProperties.put(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), "(log('test06InheritedSchemaMethodOnEntityOfBuiltinType successful'))");
			testFileMethodProperties.put(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.STATIC_SCHEMA_NODE_NAME_PROPERTY), builtinTypeName);

			app.create(StructrTraits.SCHEMA_METHOD, testFileMethodProperties);

			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		String id = createEntityAsAdmin(StructrTraits.IMAGE, "{'name': 'Test Image'}");


		try (final Tx tx = app.tx()) {

			RestAssured

				.given()
					.contentType("application/json; charset=UTF-8")
					.headers(X_USER_HEADER, ADMIN_USERNAME , X_PASSWORD_HEADER, ADMIN_PASSWORD)
				.body("{}")
				.expect()
					.statusCode(200)

				.when()
					.post("/Image/" + id + "/" + schemaMethodName);

			tx.success();

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}
	}

	@Test
	public void testErrorMethodAndResponse() {

		// this method tests global schema methods as well
		try (final Tx tx = app.tx()) {

			final JsonSchema schema   = StructrSchema.createFromDatabase(app);
			final JsonObjectType type = schema.addType("Test");

			type.addMethod("sendError", "error('test', 'test_error', 'errorrr')");

			StructrSchema.replaceDatabaseSchema(app, schema);

			// create global schema method for JavaScript
			app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "globalTest1"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), "{ Structr.find('Test')[0].sendError(); }")
			);

			// create global schema method for StructrScript
			app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "globalTest2"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), "first(find('Test')).sendError")
			);

			tx.success();

		} catch (Throwable t) {
			fail("Unexpected exception");
			t.printStackTrace();
		}

		try (final Tx tx = app.tx()) {

			// create instance of Test type to be able to call a method on
			app.create("Test", "test");

			// create admin user to call global schema methods with
			createAdminUser();

			tx.success();

		} catch (FrameworkException t) {
			fail("Unexpected exception");
			t.printStackTrace();
		}

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.headers(X_USER_HEADER, ADMIN_USERNAME , X_PASSWORD_HEADER, ADMIN_PASSWORD)
				.body("{}")

			.expect()
				.statusCode(422)
				.body("code",                equalTo(422))
				.body("message",             equalTo("Server-side scripting error"))
				.body("errors[0].type",      equalTo("Test"))
				.body("errors[0].property",  equalTo("test"))
				.body("errors[0].token",     equalTo("test_error"))
				.body("errors[0].detail",    equalTo("errorrr"))

			.when()
				.post("/globalTest1");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.headers(X_USER_HEADER, ADMIN_USERNAME , X_PASSWORD_HEADER, ADMIN_PASSWORD)
				.body("{}")

			.expect()
				.statusCode(422)
				.body("code",                equalTo(422))
				.body("message",             equalTo("Server-side scripting error"))
				.body("errors[0].type",      equalTo("Test"))
				.body("errors[0].property",  equalTo("test"))
				.body("errors[0].token",     equalTo("test_error"))
				.body("errors[0].detail",    equalTo("errorrr"))

			.when()
				.post("/globalTest2");
	}

	@Test
	public void testDeprecatedGlobalSchemaMethodURL() {

		// this method tests global schema methods as well
		try (final Tx tx = app.tx()) {

			// create global schema method for JavaScript
			app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "globalTest1"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), "{ return { success: true, value: 123 }; }")
			);

			// create admin user to call global schema methods with
			createAdminUser();

			tx.success();

		} catch (Throwable t) {
			fail("Unexpected exception");
			t.printStackTrace();
		}

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.headers(X_USER_HEADER, ADMIN_USERNAME , X_PASSWORD_HEADER, ADMIN_PASSWORD)
				.body("{}")

			.expect()
				.statusCode(200)
				.body("result.success", equalTo(true))
				.body("result.value",   equalTo(123))

			.when()
				.post("/maintenance/globalSchemaMethods/globalTest1");
	}

	@Test
	public void testSchemaMethodCache() {

		final String customTypeName   = "Test";
		final String schemaMethodName = "testMethod";
		final String schemaMethodId   = "abcdef0123456789fedcba9876543210";

		try (final Tx tx = app.tx()) {

			final NodeInterface fooFileDef = app.create(StructrTraits.SCHEMA_NODE, customTypeName);
			final PropertyMap testFooFileMethodProperties = new PropertyMap();

			testFooFileMethodProperties.put(Traits.of(StructrTraits.SCHEMA_METHOD).key(GraphObjectTraitDefinition.ID_PROPERTY), schemaMethodId);
			testFooFileMethodProperties.put(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), schemaMethodName);
			testFooFileMethodProperties.put(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), "{ return 'test'; }");
			testFooFileMethodProperties.put(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY), fooFileDef);
			testFooFileMethodProperties.put(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.IS_STATIC_PROPERTY), true);

			app.create(StructrTraits.SCHEMA_METHOD, testFooFileMethodProperties);

			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			createAdminUser();
			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		// 1. execute method
		try (final Tx tx = app.tx()) {

			RestAssured

				.given()
				.contentType("application/json; charset=UTF-8")
				.headers(X_USER_HEADER, ADMIN_USERNAME , X_PASSWORD_HEADER, ADMIN_PASSWORD)
				.expect()
				.statusCode(200)
				.body("result", equalTo("test"))
				.when()
				.post(customTypeName + "/" + schemaMethodName);

			tx.success();

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}

		// 2. try to change the method
		try (final Tx tx = app.tx()) {

			RestAssured

				.given()
				.contentType("application/json; charset=UTF-8")
				.headers(X_USER_HEADER, ADMIN_USERNAME , X_PASSWORD_HEADER, ADMIN_PASSWORD)
				.body("{ source: '{ return \"test2\"; }' }")
				.expect()
				.statusCode(200)
				.when()
				.put("/" + schemaMethodId);

			tx.success();

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}

	}
}
