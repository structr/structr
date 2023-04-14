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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.test.web.StructrUiTest;
import org.structr.web.auth.UiAuthenticator;
import org.testng.annotations.Test;

/**
 *
 *
 */
public class UserTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(UserTest.class.getName());

	@Test
	public void testAdminUserCreation() {

		createEntityAsSuperUser("/User", "{ 'name': 'admin', 'password': 'admin', 'isAdmin': true }");
		createEntityAsSuperUser("/User", "{ 'name': 'user', 'password': 'password'}");

		grant("User", UiAuthenticator.NON_AUTH_USER_POST | UiAuthenticator.AUTH_USER_POST, true);

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
				.statusCode(422)
			.when()
				.post("/User");

		// ordinary user is not allowed to create admin user
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.header("X-User", "user")
				.header("X-Password", "password")
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
				.header("X-User", "admin")
				.header("X-Password", "admin")
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
}
