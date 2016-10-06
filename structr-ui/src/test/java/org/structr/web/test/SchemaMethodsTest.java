/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.web.test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import static junit.framework.TestCase.fail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.Tx;
import org.structr.dynamic.File;
import org.structr.web.entity.FileBase;
import org.structr.web.entity.Image;
import org.structr.web.entity.User;



public class SchemaMethodsTest extends FrontendTest {

	private static final Logger logger = LoggerFactory.getLogger(SchemaMethodsTest.class.getName());

	public void test01SchemaMethodOnBuiltinType() {

		final String builtinTypeName = "File";
		final String schemaMethodName = "testFileMethod";
		
		try (final Tx tx = app.tx()) {

			// Add schema method "testFileMethod" to built-in File class
			SchemaNode fileNodeDef = app.nodeQuery(SchemaNode.class).andName(builtinTypeName).getFirst();
			
			SchemaMethod testFileMethod = app.create(SchemaMethod.class, schemaMethodName);
			testFileMethod.setProperty(SchemaMethod.source, "()");
			testFileMethod.setProperty(SchemaMethod.schemaNode, fileNodeDef);

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

	public void test02SchemaMethodOnCustomType() {

		final String customTypeName = "FooFile";
		final String schemaMethodName = "testFooFileMethod";
		
		try (final Tx tx = app.tx()) {

			// Add schema method "testFileMethod" to built-in File class
			SchemaNode fooFileDef = app.create(SchemaNode.class, customTypeName);
			
			SchemaMethod testFooFileMethod = app.create(SchemaMethod.class, schemaMethodName);
			testFooFileMethod.setProperty(SchemaMethod.source, "()");
			testFooFileMethod.setProperty(SchemaMethod.schemaNode, fooFileDef);

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
	
	
	public void test03SchemaMethodOnEntityOfBuiltinType() {

		final String builtinTypeName = "File";
		final String schemaMethodName = "testFileMethod";
		
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
			
			SchemaMethod testFileMethod = app.create(SchemaMethod.class, schemaMethodName);
			testFileMethod.setProperty(SchemaMethod.source, "()");
			testFileMethod.setProperty(SchemaMethod.schemaNode, fileNodeDef);

			tx.success();
			
		} catch (Exception ex) {
			logger.error("", ex);
		}

		FileBase testFile = null;
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

	public void test04SchemaMethodOnEntityOfCustomType() {

		final String customTypeName = "FooFile";
		final String schemaMethodName = "testFooFileMethod";
		
		try (final Tx tx = app.tx()) {

			// Add schema method "testFileMethod" to built-in File class
			SchemaNode fooFileDef = app.create(SchemaNode.class, customTypeName);
			
			SchemaMethod testFooFileMethod = app.create(SchemaMethod.class, schemaMethodName);
			testFooFileMethod.setProperty(SchemaMethod.source, "()");
			testFooFileMethod.setProperty(SchemaMethod.schemaNode, fooFileDef);

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
	public void test05InheritedSchemaMethodOnBuildinType() {

		final String builtinTypeName = "File";
		final String schemaMethodName = "testFileMethod";
		
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
			
			SchemaMethod testFileMethod = app.create(SchemaMethod.class, schemaMethodName);
			testFileMethod.setProperty(SchemaMethod.source, "()");
			testFileMethod.setProperty(SchemaMethod.schemaNode, fileNodeDef);

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

	public void test06InheritedSchemaMethodOnEntityOfBuiltinType() {

		final String builtinTypeName = "File";
		final String schemaMethodName = "testFileMethod";
		
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
			
			SchemaMethod testFileMethod = app.create(SchemaMethod.class, schemaMethodName);
			testFileMethod.setProperty(SchemaMethod.source, "()");
			testFileMethod.setProperty(SchemaMethod.schemaNode, fileNodeDef);

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
}
