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

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.error.FrameworkException;
import org.structr.schema.export.StructrSchema;
import org.structr.test.web.StructrUiTest;
import org.structr.web.auth.UiAuthenticator;
import org.testng.annotations.Test;

import java.net.URI;

import static org.hamcrest.Matchers.equalTo;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

/**
 *
 *
 */
public class NestedResourcesTest extends StructrUiTest {

	/**
	 * Test the creation of related objects using JSON objecs and plain strings.
	 */
	@Test
	public void testNodeAssociationOnCreate() {

		this.grant("User",            UiAuthenticator.NON_AUTH_USER_POST, true);
		this.grant("TestThree",       UiAuthenticator.AUTH_USER_POST, false);
		this.grant("TestFour",        UiAuthenticator.AUTH_USER_POST | UiAuthenticator.AUTH_USER_GET, false);
		this.grant("TestFour/_Test",  UiAuthenticator.AUTH_USER_GET, false);

		createEntity("/User", "{ name: user1, password: password1 }");
		createEntity("/User", "{ name: user2, password: password2 }");

		// Associate related object upon CREATION using a simple string
		{
			String testThree = createEntityAsUser("user2", "password2", "/TestThree", "{ name: \"TestThree\", visibleToAuthenticatedUsers: true }");
			assertNotNull(testThree);

			String testFour  = createEntityAsUser("user1", "password1", "/TestFour", "{ name: \"TestFour\", oneToOneTestThree: \"" + testThree + "\" }");
			assertNotNull(testFour);

			// check association
			RestAssured

				.given()
					.contentType("application/json; charset=UTF-8")
					.filter(ResponseLoggingFilter.logResponseTo(System.out))
					.header("X-User",     "user1")
					.header("X-Password", "password1")
				.expect()
					.statusCode(200)
					.body("result_count",                equalTo(1))
					.body("result.id",                   equalTo(testFour))
					.body("result.oneToOneTestThree.id", equalTo(testThree))
				.when()
					.get("/TestFour/" + testFour + "/test");
		}

		// Associate related object upon CREATION using a JSON object
		{
			String testThree = createEntityAsUser("user2", "password2", "/TestThree", "{ name: \"TestThree\", visibleToAuthenticatedUsers: true }");
			assertNotNull(testThree);

			// Associate related object upon CREATION using a simple string
			String testFour  = createEntityAsUser("user1", "password1", "/TestFour", "{ name: \"TestFour\", oneToOneTestThree: { id: \"" + testThree + "\" } }");
			assertNotNull(testFour);

			// check association
			RestAssured

				.given()
					.contentType("application/json; charset=UTF-8")
					.header("X-User",     "user1")
					.header("X-Password", "password1")
				.expect()
					.statusCode(200)
					.body("result_count",                equalTo(1))
					.body("result.id",                   equalTo(testFour))
					.body("result.oneToOneTestThree.id", equalTo(testThree))
				.when()
					.get("/TestFour/" + testFour + "/test");
		}
	}

	/**
	 * Test the creation of related objects using JSON objecs and plain strings.
	 */
	@Test
	public void testNodeAssociationOnUpdate() {

		this.grant("User",            UiAuthenticator.NON_AUTH_USER_POST, true);
		this.grant("TestThree",       UiAuthenticator.AUTH_USER_POST, false);
		this.grant("TestFour",        UiAuthenticator.AUTH_USER_POST | UiAuthenticator.AUTH_USER_PUT, false);
		this.grant("TestFour/_Test",  UiAuthenticator.AUTH_USER_GET, false);

		createEntity("/User", "{ name: user1, password: password1 }");
		createEntity("/User", "{ name: user2, password: password2 }");

		// Associate related object upon UPDATE using a simple string
		{
			String testThree = createEntityAsUser("user2", "password2", "/TestThree", "{ name: \"TestThree\", visibleToAuthenticatedUsers: true }");
			assertNotNull(testThree);

			String testFour  = createEntityAsUser("user1", "password1", "/TestFour", "{ name: \"TestFour\" }");
			assertNotNull(testFour);

			// associate objects
			RestAssured

				.given()
					.contentType("application/json; charset=UTF-8")
					.body("{ oneToOneTestThree: \"" + testThree + "\" }")
					.header("X-User",     "user1")
					.header("X-Password", "password1")
				.expect()
					.statusCode(200)
				.when()
					.put("/TestFour/" + testFour);

			// check association
			RestAssured

				.given()
					.contentType("application/json; charset=UTF-8")
					.filter(ResponseLoggingFilter.logResponseTo(System.out))
					.header("X-User",     "user1")
					.header("X-Password", "password1")
				.expect()
					.statusCode(200)
					.body("result_count",                equalTo(1))
					.body("result.id",                   equalTo(testFour))
					.body("result.oneToOneTestThree.id", equalTo(testThree))
				.when()
					.get("/TestFour/" + testFour + "/test");
		}

		// Associate related object upon UPDATE using a simple string
		{
			String testThree = createEntityAsUser("user2", "password2", "/TestThree", "{ name: \"TestThree\", visibleToAuthenticatedUsers: true }");
			assertNotNull(testThree);

			String testFour  = createEntityAsUser("user1", "password1", "/TestFour", "{ name: \"TestFour\" }");
			assertNotNull(testFour);

			// associate objects
			RestAssured

				.given()
					.contentType("application/json; charset=UTF-8")
					.body("{ oneToOneTestThree: { id: \"" + testThree + "\" } }")
					.header("X-User",     "user1")
					.header("X-Password", "password1")
				.expect()
					.statusCode(200)
				.when()
					.put("/TestFour/" + testFour);

			// check association
			RestAssured

				.given()
					.contentType("application/json; charset=UTF-8")
					.filter(ResponseLoggingFilter.logResponseTo(System.out))
					.header("X-User",     "user1")
					.header("X-Password", "password1")
				.expect()
					.statusCode(200)
					.body("result_count",                equalTo(1))
					.body("result.id",                   equalTo(testFour))
					.body("result.oneToOneTestThree.id", equalTo(testThree))
				.when()
					.get("/TestFour/" + testFour + "/test");
		}
	}

	/**
	 * Test the creation of related objects using JSON objecs and plain strings,
	 * including the (failed) modification of a property value on the related
	 * object. This test expects a 403 Forbidden error.
	 */
	@Test
	public void testFailingNodeAssociationOnUpdateWithProperties() {

		this.grant("User",            UiAuthenticator.NON_AUTH_USER_POST, true);
		this.grant("TestThree",       UiAuthenticator.AUTH_USER_POST, false);
		this.grant("TestFour",        UiAuthenticator.AUTH_USER_POST | UiAuthenticator.AUTH_USER_PUT, false);
		this.grant("TestFour/_Test",  UiAuthenticator.AUTH_USER_GET, false);

		createEntity("/User", "{ name: user1, password: password1 }");
		createEntity("/User", "{ name: user2, password: password2 }");

		// Associate related object upon UPDATE using a simple string
		{
			String testThree = createEntityAsUser("user2", "password2", "/TestThree", "{ name: \"TestThree\", visibleToAuthenticatedUsers: true }");
			assertNotNull(testThree);

			String testFour  = createEntityAsUser("user1", "password1", "/TestFour", "{ name: \"TestFour\" }");
			assertNotNull(testFour);

			// associate objects
			RestAssured

				.given()
					.contentType("application/json; charset=UTF-8")
					.body("{ oneToOneTestThree: { id: \"" + testThree + "\", name: \"test123\" } }")
					.header("X-User",     "user1")
					.header("X-Password", "password1")
				.expect()
					.statusCode(403)
				.when()
					.put("/TestFour/" + testFour);
		}
	}

	/**
	 * Test the creation of related objects using JSON objecs and plain strings,
	 * including the modification of a property value on the related object.
	 */
	@Test
	public void testSuccessfulNodeAssociationOnUpdateWithProperties() {

		this.grant("User",            UiAuthenticator.NON_AUTH_USER_POST, true);
		this.grant("TestThree",       UiAuthenticator.AUTH_USER_POST, false);
		this.grant("TestFour",        UiAuthenticator.AUTH_USER_POST | UiAuthenticator.AUTH_USER_PUT, false);
		this.grant("TestFour/_Test",  UiAuthenticator.AUTH_USER_GET, false);

		createEntity("/User", "{ name: user1, password: password1 }");

		// Associate related object upon UPDATE using a simple string
		{
			String testThree = createEntityAsUser("user1", "password1", "/TestThree", "{ name: \"TestThree\", visibleToAuthenticatedUsers: true }");
			assertNotNull(testThree);

			String testFour  = createEntityAsUser("user1", "password1", "/TestFour", "{ name: \"TestFour\" }");
			assertNotNull(testFour);

			// associate objects
			RestAssured

				.given()
					.contentType("application/json; charset=UTF-8")
					.body("{ oneToOneTestThree: { id: \"" + testThree + "\", name: \"test123\" } }")
					.header("X-User",     "user1")
					.header("X-Password", "password1")
				.expect()
					.statusCode(200)
				.when()
					.put("/TestFour/" + testFour);

			// check association
			RestAssured

				.given()
					.contentType("application/json; charset=UTF-8")
					.filter(ResponseLoggingFilter.logResponseTo(System.out))
					.header("X-User",     "user1")
					.header("X-Password", "password1")
				.expect()
					.statusCode(200)
					.body("result_count",                  equalTo(1))
					.body("result.id",                     equalTo(testFour))
					.body("result.oneToOneTestThree.id",   equalTo(testThree))
					.body("result.oneToOneTestThree.name", equalTo("test123"))
				.when()
					.get("/TestFour/" + testFour + "/test");
		}
	}

	@Test
	public void testMultiLevelUpdateOnPost() {

		/**
		 * This test verifies that the modification of existing entities
		 * works when creating a subgraph with mixed (existing and new)
		 * entities.
		 */

		try {

			final JsonSchema schema       = StructrSchema.newInstance(URI.create("http://localhost/test/#"));
			final JsonObjectType document = schema.addType("TestDocument");
			final JsonObjectType version  = schema.addType("TestVersion");
			final JsonObjectType author   = schema.addType("TestAuthor");

			document.relate(version, "VERSION").setCardinality(Cardinality.OneToOne).setTargetPropertyName("hasVersion");
			version.relate(author, "AUTHOR").setCardinality(Cardinality.OneToOne).setTargetPropertyName("hasAuthor");

			// extend public view to make result testable via REST GET
			document.addViewProperty("public", "hasVersion");
			document.addViewProperty("public", "name");
			version.addViewProperty("public", "hasAuthor");
			version.addViewProperty("public", "name");
			author.addViewProperty("public", "name");

			StructrSchema.extendDatabaseSchema(app, schema);

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");

		final String authorId    = createEntityAsUser("admin", "admin", "/TestAuthor", "{ name: 'Tester' }");
		final String versionId   = createEntityAsUser("admin", "admin", "/TestVersion", "{ name: 'TestVersion' }");
		final String documentId  = createEntityAsUser("admin", "admin", "/TestDocument", "{ name: 'TestDocument', hasVersion: { id: '" + versionId + "', hasAuthor: { id: '" + authorId + "' } } } ");


		// check document to have correct associations
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.header("X-User",     "admin")
				.header("X-Password", "admin")
			.expect()
				.statusCode(200)
				.body("result_count",                     equalTo(1))
				.body("result.id",                        equalTo(documentId))
				.body("result.name",                      equalTo("TestDocument"))
				.body("result.hasVersion.id",             equalTo(versionId))
				.body("result.hasVersion.name",           equalTo("TestVersion"))
				.body("result.hasVersion.hasAuthor.id",   equalTo(authorId))
				.body("result.hasVersion.hasAuthor.name", equalTo("Tester"))
			.when()
				.get("/TestDocument/" + documentId);


	}
}
