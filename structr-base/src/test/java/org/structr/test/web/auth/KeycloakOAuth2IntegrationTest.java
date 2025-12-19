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
package org.structr.test.web.auth;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.hamcrest.Matchers;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.rest.auth.AuthHelper;
import org.structr.test.web.StructrUiTest;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.annotations.AfterClass;

import dasniko.testcontainers.keycloak.KeycloakContainer;

import java.time.Duration;
import java.util.Map;

import static org.testng.AssertJUnit.*;

public class KeycloakOAuth2IntegrationTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(KeycloakOAuth2IntegrationTest.class.getName());

	private static final String TEST_REALM = "test-realm";
	private static final String TEST_CLIENT_ID = "structr-test-client";
	private static final String TEST_CLIENT_SECRET = "test-secret";

	private static final String TEST_USERNAME = "testuser";
	private static final String TEST_PASSWORD = "testpass";
	private static final String TEST_EMAIL = "testuser@example.com";

	private KeycloakContainer keycloakContainer;
	private String keycloakUrl;

	@BeforeClass(alwaysRun = true)
	@Override
	public void setup() {

		// Start Keycloak container with realm import
		startKeycloakContainer();

		super.setup();

		logger.info("=== Keycloak OAuth2 Integration Test Setup ===");
		logger.info("Keycloak URL: {}", keycloakUrl);
		logger.info("Realm: {}", TEST_REALM);
		logger.info("Client ID: {}", TEST_CLIENT_ID);
		logger.info("Test User: {} / {}", TEST_USERNAME, TEST_PASSWORD);

		boolean available = verifyKeycloakAvailable();
		assertTrue("Keycloak container not available", available);

		configureKeycloakSettings();
	}

	@AfterClass(alwaysRun = true)
	@Override
	public void stop() throws Exception {

		super.stop();

		if (keycloakContainer != null) {
			keycloakContainer.stop();
		}
	}

	private void startKeycloakContainer() {

		logger.info("Starting Keycloak container...");
		long startTime = System.currentTimeMillis();

		keycloakContainer = new KeycloakContainer("keycloak/keycloak:23.0")
				.withRealmImportFile("keycloak-integration-test-config.json")
				.waitingFor(Wait.forHttp("/realms/master")
					.forPort(8080)
					.withStartupTimeout(Duration.ofMinutes(2))
				);

		keycloakContainer.start();

		keycloakUrl = keycloakContainer.getAuthServerUrl();

		// Remove trailing slash if present for consistency
		if (keycloakUrl.endsWith("/")) {
			keycloakUrl = keycloakUrl.substring(0, keycloakUrl.length() - 1);
		}

		long duration = System.currentTimeMillis() - startTime;
		logger.info("Keycloak container started in {}ms. URL: {}", duration, keycloakUrl);
	}

	@Test
	public void test01LoginRedirectToKeycloak() {

		RestAssured.basePath = "/";

		// Request OAuth login - should redirect to Keycloak
		Response response = RestAssured
				.given()
				.redirects().follow(false)
				.when()
				.get("/oauth/keycloak/login")
				.then()
				.statusCode(302)
				.extract().response();

		String redirectUrl = response.getHeader("Location");

		assertNotNull("Should redirect to Keycloak", redirectUrl);
		assertTrue("Should redirect to Keycloak server", redirectUrl.contains(keycloakUrl));
		assertTrue("Should contain realm", redirectUrl.contains("/realms/" + TEST_REALM));
		assertTrue("Should contain OIDC endpoint", redirectUrl.contains("/protocol/openid-connect/auth"));
		assertTrue("Should contain client_id", redirectUrl.contains("client_id=" + TEST_CLIENT_ID));
		assertTrue("Should contain redirect_uri", redirectUrl.contains("redirect_uri="));
		assertTrue("Should contain scope", redirectUrl.contains("scope="));
	}

	@Test
	public void test02MultipleLoginsNoUserDuplication() {

		RestAssured.basePath = "/";

		try (final Tx tx = app.tx()) {

			// First login
			Principal firstUser = performCompleteLogin();
			assertNotNull("First login should create user", firstUser);

			// Second login with same credentials
			Principal secondUser = performCompleteLogin();
			assertNotNull("Second login should succeed", secondUser);

			// Verify same user
			assertEquals("Should be same user ID", firstUser.getUuid(), secondUser.getUuid());

		} catch (FrameworkException fex) {
			logger.error(fex.getMessage(), fex);
		}
	}

	// ----- Helper Methods -----

	private boolean verifyKeycloakAvailable() {

		logger.info("Verifying Keycloak at: {}/realms/{}", keycloakUrl, TEST_REALM);

		String url = keycloakUrl + "/realms/" + TEST_REALM;

		Response response = RestAssured
				.given()
				.relaxedHTTPSValidation()
				.when()
				.get(url)
				.then()
				.extract().response();

		if (response.getStatusCode() == 200) {
			logger.info("Keycloak realm is accessible");
			return true;
		}

		return false;
	}

	private void configureKeycloakSettings() {
		try (final Tx tx = app.tx()) {

			Settings.getOrCreateStringSetting("oauth", "keycloak", "server_url")
					.setValue(keycloakUrl);

			Settings.getOrCreateStringSetting("oauth", "keycloak", "realm")
					.setValue(TEST_REALM);

			Settings.getOrCreateStringSetting("oauth", "keycloak", "client_id")
					.setValue(TEST_CLIENT_ID);

			Settings.getOrCreateStringSetting("oauth", "keycloak", "client_secret")
					.setValue(TEST_CLIENT_SECRET);

			Settings.RestUserAutocreate.setValue(true);
			Settings.RestUserAutologin.setValue(true);

			tx.success();


		} catch (FrameworkException fex) {
			throw new RuntimeException("Failed to configure Keycloak settings", fex);
		}
	}

	/**
	 * Performs a complete login flow and returns the User.
	 */
	private Principal performCompleteLogin() {

		RestAssured.basePath = "/";

		// Get authorization URL
		Response loginResponse = RestAssured
				.given()
				.redirects().follow(false)
				.when()
				.get("/oauth/keycloak/login")
				.then()
				.statusCode(302)
				.extract().response();

		String keycloakAuthUrl = loginResponse.getHeader("Location");

		// Get login form
		Response loginPage = RestAssured
				.given()
				.redirects().follow(false)
				.urlEncodingEnabled(false)
				.when()
				.get(keycloakAuthUrl)
				.then()
				.statusCode(200)
				.extract().response();

		Map<String, String> cookies = loginPage.getCookies();

		// Submit credentials
		Document doc = Jsoup.parse(loginPage.getBody().asString());
		Element form = doc.select("form").first();
		Assert.assertNotNull(form, "No form found for login page of Keycloak");
		String formAction = form.attr("action");

		Response loginSubmit = RestAssured
				.given()
				.redirects().follow(false)
				.cookies(cookies)  // Include session cookies
				.formParam("username", TEST_USERNAME)
				.formParam("password", TEST_PASSWORD)
				.when()
				.post(formAction)
				.then()
				.statusCode(302)
				.extract().response();

		String callbackUrl = loginSubmit.getHeader("Location");

		RestAssured
				.given()
				.redirects().follow(false)
				.when()
				.get(callbackUrl)
				.then()
				.statusCode(Matchers.anyOf(Matchers.is(302), Matchers.is(200)));

		final PropertyKey credentialKey = Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.EMAIL_PROPERTY);

		// first try: literal, unchanged value from oauth provider
		return AuthHelper.getPrincipalForCredential(credentialKey, TEST_EMAIL);
	}
}