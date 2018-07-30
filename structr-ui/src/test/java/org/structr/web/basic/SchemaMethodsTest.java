/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.web.basic;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.File;
import org.structr.web.entity.User;



public class SchemaMethodsTest extends FrontendTest {

	private static final Logger logger = LoggerFactory.getLogger(SchemaMethodsTest.class.getName());

	@Test
	public void test01SchemaMethodOnBuiltinType() {

		final String builtinTypeName = "File";
		final String schemaMethodName = "testFileMethodOnType";

		try (final Tx tx = app.tx()) {

			// Add schema method "testFileMethod" to built-in File class
			SchemaNode fileNodeDef = app.nodeQuery(SchemaNode.class).andName(builtinTypeName).getFirst();

			final PropertyMap testFileMethodProperties = new PropertyMap();
			testFileMethodProperties.put(SchemaMethod.name, schemaMethodName);
			testFileMethodProperties.put(SchemaMethod.source, "(log('test01SchemaMethodOnBuiltinType successful'))");
			testFileMethodProperties.put(SchemaMethod.schemaNode, fileNodeDef);

			SchemaMethod testFileMethod = app.create(SchemaMethod.class, testFileMethodProperties);

			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		User admin = null;
		try (final Tx tx = app.tx()) {

			admin = createAdminUser();
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
					.post(builtinTypeName +"/" + schemaMethodName);

			tx.success();

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}
	}

	@Test
	public void test02SchemaMethodOnCustomType() {

		final String customTypeName = "FooFile";
		final String schemaMethodName = "testFooFileMethodOnType";

		try (final Tx tx = app.tx()) {

			// Add schema method "testFileMethod" to built-in File class
			SchemaNode fooFileDef = app.create(SchemaNode.class, customTypeName);

			final PropertyMap testFooFileMethodProperties = new PropertyMap();
			testFooFileMethodProperties.put(SchemaMethod.name, schemaMethodName);
			testFooFileMethodProperties.put(SchemaMethod.source, "(log('test02SchemaMethodOnCustomType successful'))");
			testFooFileMethodProperties.put(SchemaMethod.schemaNode, fooFileDef);

			SchemaMethod testFooFileMethod = app.create(SchemaMethod.class, testFooFileMethodProperties);

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

		User admin = null;
		try (final Tx tx = app.tx()) {

			admin = createAdminUser();
			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			// Add schema method "testFileMethod" to built-in File class
			SchemaNode fileNodeDef = app.nodeQuery(SchemaNode.class).andName(builtinTypeName).getFirst();

			final PropertyMap testFileMethodProperties = new PropertyMap();
			testFileMethodProperties.put(SchemaMethod.name, schemaMethodName);
			testFileMethodProperties.put(SchemaMethod.source, "(log('test03SchemaMethodOnEntityOfBuiltinType successful'))");
			testFileMethodProperties.put(SchemaMethod.schemaNode, fileNodeDef);

			SchemaMethod testFileMethod = app.create(SchemaMethod.class, testFileMethodProperties);

			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		File testFile = null;
		try (final Tx tx = app.tx()) {

			// Create File instance
			testFile = app.create(File.class, "Test File");
			testFile.setProperty(File.owner, admin);

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
			SchemaNode fooFileDef = app.create(SchemaNode.class, customTypeName);

			final PropertyMap testFooFileMethodProperties = new PropertyMap();
			testFooFileMethodProperties.put(SchemaMethod.name, schemaMethodName);
			testFooFileMethodProperties.put(SchemaMethod.source, "(log('test04SchemaMethodOnEntityOfCustomType successful'))");
			testFooFileMethodProperties.put(SchemaMethod.schemaNode, fooFileDef);

			SchemaMethod testFooFileMethod = app.create(SchemaMethod.class, testFooFileMethodProperties);

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
	public void test05InheritedSchemaMethodOnBuildinType() {

		final String builtinTypeName = "File";
		final String schemaMethodName = "testFileMethodOnInheritingType";

		User admin = null;
		try (final Tx tx = app.tx()) {

			admin = createAdminUser();
			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			// Add schema method "testFileMethod" to built-in File class
			SchemaNode fileNodeDef = app.nodeQuery(SchemaNode.class).andName(builtinTypeName).getFirst();

			final PropertyMap testFileMethodProperties = new PropertyMap();
			testFileMethodProperties.put(SchemaMethod.name, schemaMethodName);
			testFileMethodProperties.put(SchemaMethod.source, "(log('test05InheritedSchemaMethodOnBuildinType successful'))");
			testFileMethodProperties.put(SchemaMethod.schemaNode, fileNodeDef);

			SchemaMethod testFileMethod = app.create(SchemaMethod.class, testFileMethodProperties);

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

		User admin = null;
		try (final Tx tx = app.tx()) {

			admin = createAdminUser();
			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			// Add schema method "testFileMethod" to built-in File class
			SchemaNode fileNodeDef = app.nodeQuery(SchemaNode.class).andName(builtinTypeName).getFirst();

			final PropertyMap testFileMethodProperties = new PropertyMap();
			testFileMethodProperties.put(SchemaMethod.name, schemaMethodName);
			testFileMethodProperties.put(SchemaMethod.source, "(log('test06InheritedSchemaMethodOnEntityOfBuiltinType successful'))");
			testFileMethodProperties.put(SchemaMethod.schemaNode, fileNodeDef);

			SchemaMethod testFileMethod = app.create(SchemaMethod.class, testFileMethodProperties);

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
				"	newGroup.addMember(newUser);\n" +
				"\n" +
				"	return newGroup.members.length;" +
				"}}";

		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext, null);

			assertEquals("Calling group.addMember(user) should be possible from JavaScript!", 1.0, Scripting.evaluate(ctx, null, addMemberScript, "test"));

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
				"	group.removeMember(user);\n" +
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
}
