/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.PropertyView;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.schema.export.StructrSchema;
import org.structr.test.web.StructrUiTest;
import org.structr.web.auth.UiAuthenticator;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.testng.Assert.fail;
import static org.testng.AssertJUnit.assertEquals;

/**
 *
 *
 */
public class UserTest extends StructrUiTest {

	@Test
	public void testAdminUserCreation() {

		createEntityAsSuperUser("/User", "{ 'name': 'admin', 'password': 'admin', 'isAdmin': true }");
		createEntityAsSuperUser("/User", "{ 'name': 'user', 'password': 'password'}");

		// anonymous user is not allowed to create admin user
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.body("{ 'name': 'Administrator', 'password': 'test', 'isAdmin': true }")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
				.statusCode(401)
			.when()
				.post("/User");

		grant("User", UiAuthenticator.NON_AUTH_USER_POST | UiAuthenticator.AUTH_USER_POST, true);

		// anonymous user is not allowed to create admin user even with grant because isAdmin is read-only
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.body("{ 'name': 'Administrator', 'password': 'test', 'isAdmin': true }")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
				.statusCode(422)
			.when()
				.post("/User");

		// ordinary user is not allowed to create admin user even with grant because isAdmin is read-only
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.header(X_USER_HEADER, "user")
				.header(X_PASSWORD_HEADER, "password")
				.body("{ 'name': 'Administrator', 'password': 'test', 'isAdmin': true }")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
				.statusCode(422)
			.when()
				.post("/User");

		// admin user is allowed to create admin user
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.header(X_USER_HEADER, ADMIN_USERNAME)
				.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
				.body("{ 'name': 'Administrator', 'password': 'test', 'isAdmin': true }")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
				.statusCode(201)
			.when()
				.post("/User");

	}

	@Test
	public void testSecurityContextInMeResource() {

		final String uuid = createEntityAsSuperUser("/User", "{ 'name': 'user', 'password': 'password'}");

		grant(StructrTraits.USER,  UiAuthenticator.AUTH_USER_GET, true);
		grant("User/_id", UiAuthenticator.AUTH_USER_GET, false);

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final JsonSchema schema  = StructrSchema.createFromDatabase(app);
			final JsonType principal = schema.addType(StructrTraits.USER);

			principal.addFunctionProperty("funcTest", PropertyView.Public, PropertyView.Ui).setReadFunction("(me)");

			StructrSchema.replaceDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}

		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.header(X_USER_HEADER, "user")
			.header(X_PASSWORD_HEADER, "password")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.body("result.funcTest", equalTo(uuid))
			.statusCode(200)
			.when()
			.get("/me");

		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.header(X_USER_HEADER, "user")
			.header(X_PASSWORD_HEADER, "password")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.body("result[0].funcTest", equalTo(uuid))
			.statusCode(200)
			.when()
			.get("/User");

		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.header(X_USER_HEADER, "user")
			.header(X_PASSWORD_HEADER, "password")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.body("result.funcTest", equalTo(uuid))
			.statusCode(200)
			.when()
			.get("/User/" + uuid);


	}

	@Test
	public void testUserNameOrEMailConstraint() {

		createAdminUser();

		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.body("{}")
			.header(X_USER_HEADER, "admin")
			.header(X_PASSWORD_HEADER, "admin")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.statusCode(422)
			.body("code", equalTo(422))
			.body("message", equalTo("Cannot create a user who has neither a name nor an email address"))
			.body("errors[0].token",    equalTo("must_not_be_empty"))
			.body("errors[0].type",     equalTo("User"))
			.body("errors[0].property", equalTo("name"))
			.body("errors[1].token",    equalTo("must_not_be_empty"))
			.body("errors[1].type",     equalTo("User"))
			.body("errors[1].property", equalTo("eMail"))
			.when()
			.post("/User");

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.USER);

			tx.success();

			fail("User without name and eMail address should not be created");

		} catch (FrameworkException fex) {

			final ErrorBuffer buffer = fex.getErrorBuffer();

			assertEquals("Invalid error code", 422, fex.getStatus());
			assertEquals("Invalid error message", "Cannot create a user who has neither a name nor an email address", fex.getMessage());

			assertEquals("Invalid type in error token", "User", buffer.getErrorTokens().get(0).getType());
			assertEquals("Invalid property in error token", "name", buffer.getErrorTokens().get(0).getProperty());
			assertEquals("Invalid token in error token", "must_not_be_empty", buffer.getErrorTokens().get(0).getToken());

			assertEquals("Invalid type in error token", "User", buffer.getErrorTokens().get(1).getType());
			assertEquals("Invalid property in error token", "eMail", buffer.getErrorTokens().get(1).getProperty());
			assertEquals("Invalid token in error token", "must_not_be_empty", buffer.getErrorTokens().get(1).getToken());

		}
	}
}
