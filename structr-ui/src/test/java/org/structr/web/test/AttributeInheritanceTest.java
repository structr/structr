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
import java.util.logging.Level;
import java.util.logging.Logger;
import static junit.framework.TestCase.fail;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.graph.Tx;
import org.structr.web.auth.UiAuthenticator;
import static org.structr.web.test.ResourceAccessTest.createResourceAccess;

/**
 * Test attribute inheritance.
 * 
 * There are the following inheritance chains:
 * 
 * org.structr.dynamic.File extends org.structr.web.entity.FileBase
 * org.structr.web.entity.Image extends org.structr.dynamic.File
 * 
 * Attributes of File must show up on Image and also all dynamic classes that inherit from File or Image.
 */


public class AttributeInheritanceTest extends FrontendTest {

	private static final Logger logger = Logger.getLogger(AttributeInheritanceTest.class.getName());
	
	public void test01InheritanceOfFileAttributesToImage() {

		try (final Tx tx = app.tx()) {

			createAdminUser();
			createResourceAccess("_schema", UiAuthenticator.AUTH_USER_GET);
			tx.success();

		} catch (Exception ex) {
			logger.log(Level.SEVERE, null, ex);
		}

		try (final Tx tx = app.tx()) {

			SchemaNode fileNodeDef = app.nodeQuery(SchemaNode.class).andName("File").getFirst();
			
			SchemaProperty testFileProperty = app.create(SchemaProperty.class);
			testFileProperty.setProperty(SchemaProperty.name, "testFile");
			testFileProperty.setProperty(SchemaProperty.propertyType, "String");
			testFileProperty.setProperty(SchemaProperty.schemaNode, fileNodeDef);

			tx.success();
			
		} catch (Exception ex) {
			logger.log(Level.SEVERE, null, ex);
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

				.expect()
					.statusCode(200)

					.body("result",	                   hasSize(31))
					.body("result[30].jsonName",       equalTo("testFile"))
					.body("result[30].declaringClass", equalTo("_FileHelper"))
				
				.when()
					.get("/_schema/File/ui");
	
			tx.success();
			
		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");
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

				.expect()
					.statusCode(200)

					.body("result",	                   hasSize(37))
					.body("result[36].jsonName",       equalTo("testFile"))
					.body("result[36].declaringClass", equalTo("_FileHelper"))
				
				.when()
					.get("/_schema/Image/ui");
	
			tx.success();
			
		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");
		}
	}
	
}
