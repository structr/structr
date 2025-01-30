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
package org.structr.test.web.basic;

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.script.Scripting;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;
import org.structr.schema.export.StructrSchema;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;



public class SchemaMethodsTest extends FrontendTest {

	private static final Logger logger = LoggerFactory.getLogger(SchemaMethodsTest.class.getName());

	@Test
	public void test01SchemaMethodOnBuiltinType() {

		final String builtinTypeName = "File";
		final String schemaMethodName = "testFileMethodOnType";

		try (final Tx tx = app.tx()) {

			// Add schema method "testFileMethod" to built-in File class
			final NodeInterface fileNodeDef = app.nodeQuery("SchemaNode").andName(builtinTypeName).getFirst();

			final PropertyMap testFileMethodProperties = new PropertyMap();
			testFileMethodProperties.put(Traits.of("SchemaMethod").key("name"),      schemaMethodName);
			testFileMethodProperties.put(Traits.of("SchemaMethod").key("source"),    "(log('test01SchemaMethodOnBuiltinType successful'))");
			testFileMethodProperties.put(Traits.of("SchemaMethod").key("schemaNode"), fileNodeDef);
			testFileMethodProperties.put(Traits.of("SchemaMethod").key("isStatic"),   true);

			final NodeInterface testFileMethod = app.create("SchemaMethod", testFileMethodProperties);

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
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.headers("X-User", ADMIN_USERNAME , "X-Password", ADMIN_PASSWORD)
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
			final NodeInterface fooFileDef = app.create("SchemaNode", customTypeName);

			final PropertyMap testFooFileMethodProperties = new PropertyMap();
			testFooFileMethodProperties.put(Traits.of("SchemaMethod").key("name"),      schemaMethodName);
			testFooFileMethodProperties.put(Traits.of("SchemaMethod").key("source"),    "(log('test02SchemaMethodOnCustomType successful'))");
			testFooFileMethodProperties.put(Traits.of("SchemaMethod").key("schemaNode"), fooFileDef);
			testFooFileMethodProperties.put(Traits.of("SchemaMethod").key("isStatic"),   true);

			app.create("SchemaMethod", testFooFileMethodProperties);

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
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
					.headers("X-User", ADMIN_USERNAME , "X-Password", ADMIN_PASSWORD)
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

		final String builtinTypeName = "File";
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
			final NodeInterface fileNodeDef = app.nodeQuery("SchemaNode").andName(builtinTypeName).getFirst();

			final PropertyMap testFileMethodProperties = new PropertyMap();
			testFileMethodProperties.put(Traits.of("SchemaMethod").key("name"), schemaMethodName);
			testFileMethodProperties.put(Traits.of("SchemaMethod").key("source"), "(log('test03SchemaMethodOnEntityOfBuiltinType successful'))");
			testFileMethodProperties.put(Traits.of("SchemaMethod").key("schemaNode"), fileNodeDef);

			app.create("SchemaMethod", testFileMethodProperties);

			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		NodeInterface testFile = null;
		try (final Tx tx = app.tx()) {

			// Create File instance
			testFile = app.create("File", "Test File");
			testFile.setProperty(Traits.of("File").key("owner"), admin);

			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			RestAssured

				.given()
					.contentType("application/json; charset=UTF-8")
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
					.headers("X-User", ADMIN_USERNAME , "X-Password", ADMIN_PASSWORD)
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
			final NodeInterface fooFileDef = app.create("SchemaNode", customTypeName);

			final PropertyMap testFooFileMethodProperties = new PropertyMap();
			testFooFileMethodProperties.put(Traits.of("SchemaMethod").key("name"), schemaMethodName);
			testFooFileMethodProperties.put(Traits.of("SchemaMethod").key("source"), "(log('test04SchemaMethodOnEntityOfCustomType successful'))");
			testFooFileMethodProperties.put(Traits.of("SchemaMethod").key("schemaNode"), fooFileDef);

			app.create("SchemaMethod", testFooFileMethodProperties);

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
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
					.headers("X-User", ADMIN_USERNAME , "X-Password", ADMIN_PASSWORD)
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

		final String builtinTypeName = "File";
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
			final NodeInterface fileNodeDef = app.nodeQuery("SchemaNode").andName(builtinTypeName).getFirst();

			final PropertyMap testFileMethodProperties = new PropertyMap();
			testFileMethodProperties.put(Traits.of("SchemaMethod").key("name"), schemaMethodName);
			testFileMethodProperties.put(Traits.of("SchemaMethod").key("source"), "(log('test05InheritedSchemaMethodOnBuildinType successful'))");
			testFileMethodProperties.put(Traits.of("SchemaMethod").key("schemaNode"), fileNodeDef);
			testFileMethodProperties.put(Traits.of("SchemaMethod").key("isStatic"), true);

			app.create("SchemaMethod", testFileMethodProperties);

			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			RestAssured

				.given()
					.contentType("application/json; charset=UTF-8")
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
					.headers("X-User", ADMIN_USERNAME , "X-Password", ADMIN_PASSWORD)
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

		final String builtinTypeName = "File";
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
			final NodeInterface fileNodeDef = app.nodeQuery("SchemaNode").andName(builtinTypeName).getFirst();

			final PropertyMap testFileMethodProperties = new PropertyMap();
			testFileMethodProperties.put(Traits.of("SchemaMethod").key("name"), schemaMethodName);
			testFileMethodProperties.put(Traits.of("SchemaMethod").key("source"), "(log('test06InheritedSchemaMethodOnEntityOfBuiltinType successful'))");
			testFileMethodProperties.put(Traits.of("SchemaMethod").key("schemaNode"), fileNodeDef);

			app.create("SchemaMethod", testFileMethodProperties);

			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		String id = createEntityAsAdmin("Image", "{'name': 'Test Image'}");


		try (final Tx tx = app.tx()) {

			RestAssured

				.given()
					.contentType("application/json; charset=UTF-8")
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
					.headers("X-User", ADMIN_USERNAME , "X-Password", ADMIN_PASSWORD)
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
	public void testCallingExportMethods() {

		final String addMemberScript =  "${{\n" +
				"\n" +
				"	var newGroup = Structr.create('Group', 'name', 'testGroup');\n" +
				"	var newUser  = Structr.create('User', 'name', 'testUser');\n" +
				"\n" +
				"	newGroup.addMember({ user: newUser });\n" +
				"\n" +
				"	return newGroup.members.length;" +
				"}}";

		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext, null);

			assertEquals("Calling group.addMember(user) should be possible from JavaScript!", 1, Scripting.evaluate(ctx, null, addMemberScript, "test"));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		final String removeMemberScript =  "${{\n" +
				"\n" +
				"	var group = Structr.find('Group', 'name', 'testGroup')[0];\n" +
				"	var user  = Structr.find('User', 'name', 'testUser')[0];\n" +
				"\n" +
				"	var beforeRemove = 'before: ' + group.members.length;" +
				"\n" +
				"	group.removeMember({ user: user });\n" +
				"\n" +
				"	return beforeRemove + ' - after: ' + group.members.length;" +
				"}}";

		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext, null);

			// we need to .toString() the evaluation result because a ConsString is returned from javascript
			assertEquals("Calling group.removeMember(user) should be possible from JavaScript!", "before: 1 - after: 0", Scripting.evaluate(ctx, null, removeMemberScript, "test").toString());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
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
			app.create("SchemaMethod",
				new NodeAttribute<>(Traits.of("SchemaMethod").key("name"), "globalTest1"),
				new NodeAttribute<>(Traits.of("SchemaMethod").key("source"), "{ Structr.find('Test')[0].sendError(); }")
			);

			// create global schema method for StructrScript
			app.create("SchemaMethod",
				new NodeAttribute<>(Traits.of("SchemaMethod").key("name"), "globalTest2"),
				new NodeAttribute<>(Traits.of("SchemaMethod").key("source"), "first(find('Test')).sendError")
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
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.headers("X-User", ADMIN_USERNAME , "X-Password", ADMIN_PASSWORD)
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
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.headers("X-User", ADMIN_USERNAME , "X-Password", ADMIN_PASSWORD)
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
			app.create("SchemaMethod",
				new NodeAttribute<>(Traits.of("SchemaMethod").key("name"), "globalTest1"),
				new NodeAttribute<>(Traits.of("SchemaMethod").key("source"), "{ return { success: true, value: 123 }; }")
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
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.headers("X-User", ADMIN_USERNAME , "X-Password", ADMIN_PASSWORD)
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

			final NodeInterface fooFileDef = app.create("SchemaNode", customTypeName);
			final PropertyMap testFooFileMethodProperties = new PropertyMap();

			testFooFileMethodProperties.put(Traits.of("SchemaMethod").key("id"), schemaMethodId);
			testFooFileMethodProperties.put(Traits.of("SchemaMethod").key("name"), schemaMethodName);
			testFooFileMethodProperties.put(Traits.of("SchemaMethod").key("source"), "{ return 'test'; }");
			testFooFileMethodProperties.put(Traits.of("SchemaMethod").key("schemaNode"), fooFileDef);
			testFooFileMethodProperties.put(Traits.of("SchemaMethod").key("isStatic"), true);

			app.create("SchemaMethod", testFooFileMethodProperties);

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
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.headers("X-User", ADMIN_USERNAME , "X-Password", ADMIN_PASSWORD)
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
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.headers("X-User", ADMIN_USERNAME , "X-Password", ADMIN_PASSWORD)
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
