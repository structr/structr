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
package org.structr.test.web.auth;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.structr.api.config.Settings;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * The letsencrypt action of the configuration interface runs nothing without a session or without domains.
 */
public class ConfigServletLetsEncryptTest extends StructrUiTest {

	private static final String CONFIG_PATH = "/structr/config";

	@Test
	public void testLetsEncryptActionNeedsASession() {

		Settings.SetupWizardCompleted.setValue(true);

		try {

			final Response response = post(null, "action", "letsencrypt", "server", "staging", "reload", "false");

			assertEquals("an unauthenticated request must be redirected to the login page", 302, response.getStatusCode());
			assertFalse("an unauthenticated request must not get a command result", response.getBody().asString().contains("success"));

		} finally {

			Settings.SetupWizardCompleted.setValue(false);
		}
	}

	@Test
	public void testLetsEncryptActionWithoutDomains() {

		Settings.SetupWizardCompleted.setValue(true);
		Settings.LetsEncryptDomains.setValue("");

		try {

			final Response response = post(login(), "action", "letsencrypt", "server", "staging", "reload", "false");

			assertEquals(422, response.getStatusCode());
			assertTrue(response.getContentType().startsWith("application/json"));
			assertEquals(Boolean.FALSE, response.jsonPath().get("success"));
			assertTrue(response.jsonPath().getString("errors[0]").contains(Settings.LetsEncryptDomains.getKey()));

		} finally {

			Settings.SetupWizardCompleted.setValue(false);
		}
	}

	// ----- private methods -----
	private String login() {

		final Response login = post(null, "action", "login", "superuserName", Settings.SuperUserName.getValue(), "superuserPassword", Settings.SuperUserPassword.getValue());

		assertEquals(302, login.getStatusCode());
		assertFalse("login must not fail", login.getHeader("Location").contains("?loginFailed"));

		final String sessionId = login.getSessionId();

		assertNotNull("login must issue a session cookie", sessionId);

		return sessionId;
	}

	private Response post(final String sessionId, final String... formParams) {

		RequestSpecification request = RestAssured
			.given()
				.basePath(CONFIG_PATH)
				.redirects().follow(false)
				.header("Origin", "http://" + host + ":" + httpPort);

		if (sessionId != null) {

			request = request.sessionId(sessionId);
		}

		for (int i = 0; i < formParams.length; i += 2) {

			request = request.formParam(formParams[i], formParams[i + 1]);
		}

		return request.when().post();
	}
}
