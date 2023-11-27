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
import org.structr.test.web.StructrUiTest;
import org.structr.web.auth.UiAuthenticator;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertNotNull;

/**
 *
 *
 */
public class LoginLogoutTest extends StructrUiTest {

	@Test
	public void testLoginLogout() {

		createEntityAsSuperUser("/User", "{ 'name': 'User1', 'password': 'geheim'}");

		grant("_login", UiAuthenticator.NON_AUTH_USER_POST, true);
		grant("_logout", UiAuthenticator.AUTH_USER_POST, false);
		grant("User", UiAuthenticator.AUTH_USER_GET, false);

		final String sessionId = RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.body("{ 'name': 'User1', 'password': 'geheim'}")
		.expect()
			.statusCode(200)
		.when()
			.post("/login")
		.getSessionId();

		assertNotNull(sessionId);

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.sessionId(sessionId)
		.expect()
			.statusCode(200)
		.when()
			.get("/me");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.sessionId(sessionId)
		.expect()
			.statusCode(200)
		.when()
			.post("/logout");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.sessionId(sessionId)
		.expect()
			.statusCode(401)
		.when()
			.get("/me");
	}
}
