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

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.structr.api.config.Settings;
import org.structr.test.web.StructrUiTest;
import org.structr.web.auth.OAuth2Flow;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import java.time.Duration;
import java.util.Map;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * A real identity provider in a container, plus the pieces of the OAuth2 round trip as separate steps.
 *
 * <p>The steps are separate on purpose: a test about what happens when the callback reaches the wrong
 * browser has to be able to run the authorization at the provider in one browser and the callback in
 * another, which a single "log in" helper cannot express.
 */
public abstract class KeycloakOAuth2TestBase extends StructrUiTest {

	protected static final String TEST_REALM         = "test-realm";
	protected static final String TEST_CLIENT_ID     = "structr-test-client";
	protected static final String TEST_CLIENT_SECRET = "test-secret";

	protected static final String TEST_USERNAME = "testuser";
	protected static final String TEST_PASSWORD = "testpass";
	protected static final String TEST_EMAIL    = "testuser@example.com";

	protected KeycloakContainer keycloakContainer;
	protected String keycloakUrl;

	@BeforeClass(alwaysRun = true)
	@Parameters("testDatabaseConnection")
	@Override
	public void setup(@Optional String testDatabaseConnection) {

		startKeycloakContainer();

		super.setup(testDatabaseConnection);

		assertTrue("Keycloak container not available", verifyKeycloakAvailable());

		configureKeycloakSettings();
	}

	@AfterClass(alwaysRun = true)
	@Override
	public void teardown() throws Exception {

		super.teardown();

		if (keycloakContainer != null) {

			keycloakContainer.stop();
		}
	}

	/**
	 * One browser's half-finished OAuth2 flow: the URL the user is sent to at the provider, and the
	 * cookie that ties the eventual callback to this browser and to nothing else.
	 */
	protected static class StartedFlow {

		protected final String authorizationUrl;
		protected final String stateCookie;

		protected StartedFlow(final String authorizationUrl, final String stateCookie) {

			this.authorizationUrl = authorizationUrl;
			this.stateCookie      = stateCookie;
		}
	}

	/**
	 * Step one: ask Structr to start a login, as a browser opening /oauth/keycloak/login does.
	 */
	protected StartedFlow startLogin(final String queryString) {

		RestAssured.basePath = "/";

		final Response response = RestAssured
			.given()
				.redirects().follow(false)
			.when()
				.get("/oauth/keycloak/login" + (queryString != null ? "?" + queryString : ""))
			.then()
				.statusCode(302)
			.extract().response();

		return new StartedFlow(response.getHeader("Location"), response.getCookie(OAuth2Flow.STATE_COOKIE_NAME));
	}

	/**
	 * Step two: log in at Keycloak and return the callback URL it redirects to. Nothing has reached
	 * Structr yet at this point - the authorization code exists, and whoever holds this URL holds it.
	 */
	protected String authorizeAtKeycloak(final StartedFlow flow, final String username, final String password) {

		final Response loginPage = RestAssured
			.given()
				.redirects().follow(false)
				.urlEncodingEnabled(false)
			.when()
				.get(flow.authorizationUrl)
			.then()
				.statusCode(200)
			.extract().response();

		final Map<String, String> cookies = loginPage.getCookies();
		final Document doc                = Jsoup.parse(loginPage.getBody().asString());
		final Element form                = doc.select("form").first();

		assertNotNull("No form found for login page of Keycloak", form);

		final Response loginSubmit = RestAssured
			.given()
				.redirects().follow(false)
				.cookies(cookies)
				.formParam("username", username)
				.formParam("password", password)
			.when()
				.post(form.attr("action"))
			.then()
				.statusCode(302)
			.extract().response();

		return loginSubmit.getHeader("Location");
	}

	/**
	 * Step three: hand the callback URL to a browser. Passing the state cookie makes it the browser that
	 * started the flow; passing null makes it any other browser.
	 */
	protected Response deliverCallback(final String callbackUrl, final String stateCookie) {

		RestAssured.basePath = "/";

		RequestSpecification request = RestAssured
			.given()
				.redirects().follow(false);

		if (stateCookie != null) {

			request = request.cookie(OAuth2Flow.STATE_COOKIE_NAME, stateCookie);
		}

		return request
			.when()
				.get(callbackUrl)
			.then()
			.extract().response();
	}

	// ----- private methods -----

	private void startKeycloakContainer() {

		keycloakContainer = new KeycloakContainer("keycloak/keycloak:23.0")
								.withRealmImportFile("keycloak-integration-test-config.json")
								.waitingFor(Wait.forHttp("/realms/master")
									.forPort(8080)
									.withStartupTimeout(Duration.ofMinutes(2)));

		keycloakContainer.start();

		keycloakUrl = keycloakContainer.getAuthServerUrl();

		// Remove trailing slash if present for consistency
		if (keycloakUrl.endsWith("/")) {

			keycloakUrl = keycloakUrl.substring(0, keycloakUrl.length() - 1);
		}
	}

	private boolean verifyKeycloakAvailable() {

		final Response response = RestAssured
			.given()
				.relaxedHTTPSValidation()
			.when()
				.get(keycloakUrl + "/realms/" + TEST_REALM)
			.then()
			.extract().response();

		return response.getStatusCode() == 200;
	}

	private void configureKeycloakSettings() {

		Settings.OAuthKeycloakServerUrl.setValue(keycloakUrl);
		Settings.OAuthKeycloakRealm.setValue(TEST_REALM);
		Settings.OAuthKeycloakClientId.setValue(TEST_CLIENT_ID);
		Settings.OAuthKeycloakClientSecret.setValue(TEST_CLIENT_SECRET);
		Settings.RestUserAutocreate.setValue(true);
		Settings.RestUserAutologin.setValue(true);

		/* These tests speak plain HTTP to a local server, and a Secure cookie is one a client is entitled
		   to drop, so the flag is turned off to keep the state cookie out of the question. That leaves it
		   untested: nothing here asserts that the state cookie carries Secure when the setting is on -
		   only SessionFixationTest does that, and for the session cookie. */
		Settings.CookieSecure.setValue(false);
	}
}
