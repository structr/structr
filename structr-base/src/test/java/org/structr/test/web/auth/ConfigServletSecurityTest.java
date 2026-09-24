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
import org.structr.rest.servlet.ConfigServlet;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Ticket 1581: the configuration servlet changed state on GET parameters (?reset=key, ?stop=service,
 * ?finish, ...) behind an origin check that let a request without Origin header pass. Browsers send no
 * Origin on a top-level navigation and do send the SameSite=Lax session cookie with it, so a link that a
 * logged-in superuser clicks anywhere on the web could reset setup.wizard.completed - and the servlet,
 * which treated every request before the wizard as authenticated, was then open to anyone who could
 * reach the port.
 *
 * <p>State changes are POST actions now, a POST without Origin is rejected, the wizard asks for a setup
 * token that is only written to the server log, and a config session ends with the session timeout.
 */
public class ConfigServletSecurityTest extends StructrUiTest {

	private static final String CONFIG_PATH = "/structr/config";

	@Test
	public void testStateChangingGetParametersAreIgnored() {

		Settings.SetupWizardCompleted.setValue(false);

		try {

			// before the wizard: the request is not authenticated and ?finish does not complete the wizard
			assertLoginPage(get(null, "finish", ""));
			assertFalse("a GET parameter completed the wizard", Settings.SetupWizardCompleted.getValue());

			// with an authenticated session, the GET parameters that used to be actions change nothing either
			final String sessionId = authenticateWithSetupToken(ConfigServlet.getSetupToken());
			final String title     = Settings.ApplicationTitle.getValue();

			assertConfigPage(get(sessionId, "reset", Settings.ApplicationTitle.getKey()));
			assertEquals("a GET parameter reset a setting", title, Settings.ApplicationTitle.getValue());

			assertConfigPage(get(sessionId, "setMaintenance", "true"));
			assertFalse("a GET parameter enabled maintenance mode", Settings.MaintenanceModeEnabled.getValue(false));

			assertConfigPage(get(sessionId, "finish", ""));
			assertFalse("a GET parameter completed the wizard", Settings.SetupWizardCompleted.getValue());

		} finally {

			Settings.SetupWizardCompleted.setValue(false);
		}
	}

	@Test
	public void testPostWithoutOriginIsRejected() {

		Settings.SetupWizardCompleted.setValue(false);

		final Response response = RestAssured
			.given()
				.basePath(CONFIG_PATH)
				.redirects().follow(false)
				.formParam("action", "setup")
				.formParam("setupToken", ConfigServlet.getSetupToken())
			.when()
				.post();

		assertEquals("a POST without Origin header must be rejected", 403, response.getStatusCode());

		final String sessionId = response.getSessionId();
		if (sessionId != null) {

			assertLoginPage(get(sessionId));
		}
	}

	@Test
	public void testPostFromForeignOriginIsRejected() {

		Settings.SetupWizardCompleted.setValue(false);

		final Response response = RestAssured
			.given()
				.basePath(CONFIG_PATH)
				.redirects().follow(false)
				.header("Origin", "http://evil.example")
				.formParam("action", "setup")
				.formParam("setupToken", ConfigServlet.getSetupToken())
			.when()
				.post();

		assertEquals("a POST from another origin must be rejected", 403, response.getStatusCode());
	}

	@Test
	public void testSetupTokenGuardsTheWizard() {

		Settings.SetupWizardCompleted.setValue(false);

		try {

			// no session: login page, not the wizard
			assertLoginPage(get(null));

			// wrong token: still the login page
			final Response failed = postSameOrigin(null, "action", "setup", "setupToken", "not-the-token");

			assertEquals(302, failed.getStatusCode());
			assertTrue("a failed setup must redirect to the login page with ?loginFailed", failed.getHeader("Location").contains("?loginFailed"));
			assertLoginPage(get(failed.getSessionId()));

			// right token: the wizard
			final String sessionId = authenticateWithSetupToken(ConfigServlet.getSetupToken());

			assertConfigPage(get(sessionId));

		} finally {

			Settings.SetupWizardCompleted.setValue(false);
		}
	}

	@Test
	public void testSetupTokenIsWorthlessAfterTheWizard() {

		Settings.SetupWizardCompleted.setValue(true);

		try {

			final Response response = postSameOrigin(null, "action", "setup", "setupToken", ConfigServlet.getSetupToken());

			assertEquals(302, response.getStatusCode());
			assertLoginPage(get(response.getSessionId()));

		} finally {

			Settings.SetupWizardCompleted.setValue(false);
		}
	}

	@Test
	public void testConfigSessionEndsWithTheSessionTimeout() throws InterruptedException {

		Settings.SetupWizardCompleted.setValue(true);

		try {

			final Response login = postSameOrigin(null, "action", "login", "superuserName", Settings.SuperUserName.getValue(), "superuserPassword", Settings.SuperUserPassword.getValue());

			assertEquals(302, login.getStatusCode());

			final String sessionId = login.getSessionId();

			assertNotNull("login must issue a session cookie", sessionId);
			assertConfigPage(get(sessionId));

			Settings.SessionTimeout.setValue(1);

			Thread.sleep(1500);

			assertLoginPage(get(sessionId));

		} finally {

			Settings.SessionTimeout.setValue(Settings.SessionTimeout.getDefaultValue());
			Settings.SetupWizardCompleted.setValue(false);
		}
	}

	// ----- private methods -----
	private String authenticateWithSetupToken(final String token) {

		final Response response = postSameOrigin(null, "action", "setup", "setupToken", token);

		assertEquals(302, response.getStatusCode());
		assertFalse("setup with the right token must not fail", response.getHeader("Location").contains("?loginFailed"));

		final String sessionId = response.getSessionId();

		assertNotNull("setup must issue a session cookie", sessionId);

		return sessionId;
	}

	private Response postSameOrigin(final String sessionId, final String... formParams) {

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

	private Response get(final String sessionId) {

		return request(sessionId).when().get();
	}

	private Response get(final String sessionId, final String parameterName, final String parameterValue) {

		return request(sessionId).queryParam(parameterName, parameterValue).when().get();
	}

	private RequestSpecification request(final String sessionId) {

		RequestSpecification request = RestAssured
			.given()
				.basePath(CONFIG_PATH)
				.redirects().follow(false);

		if (sessionId != null) {

			request = request.sessionId(sessionId);
		}

		return request;
	}

	private void assertLoginPage(final Response response) {

		assertEquals(200, response.getStatusCode());

		final String body = response.getBody().asString();

		assertTrue("expected the login page", body.contains("class=\"login"));
		assertFalse("the configuration form must not be shown to an unauthenticated request", body.contains("config-form"));
	}

	private void assertConfigPage(final Response response) {

		assertEquals(200, response.getStatusCode());

		final String body = response.getBody().asString();

		assertTrue("expected the configuration page", body.contains("config-form"));
		assertFalse("the login page must not be shown to an authenticated session", body.contains("class=\"login"));
	}
}
