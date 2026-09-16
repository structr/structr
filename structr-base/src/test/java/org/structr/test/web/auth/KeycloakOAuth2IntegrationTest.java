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
import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.rest.auth.AuthHelper;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.*;

public class KeycloakOAuth2IntegrationTest extends KeycloakOAuth2TestBase {

	private static final Logger logger = LoggerFactory.getLogger(KeycloakOAuth2IntegrationTest.class.getName());

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

	/**
	 * Performs a complete login flow and returns the User.
	 */
	private Principal performCompleteLogin() {

		final StartedFlow flow      = startLogin(null);
		final String callbackUrl    = authorizeAtKeycloak(flow, TEST_USERNAME, TEST_PASSWORD);

		deliverCallback(callbackUrl, flow.stateCookie)
			.then()
			.statusCode(Matchers.anyOf(Matchers.is(302), Matchers.is(200)));

		final PropertyKey credentialKey = Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.EMAIL_PROPERTY);

		// first try: literal, unchanged value from oauth provider

		return AuthHelper.getPrincipalForCredential(credentialKey, TEST_EMAIL);
	}
}
