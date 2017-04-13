/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.rest;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.common.StructrUiTest;

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
}
