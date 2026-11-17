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
import org.structr.core.auth.OneTimeTokens;
import org.structr.test.web.StructrUiTest;
import org.structr.web.auth.UiAuthenticator;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * An admin creates a one-time token for a user; a request that carries it, on whatever address, gets a session as
 * that user. The token works once and for a short time, and only an admin can create one.
 */
public class OneTimeTokenTest extends StructrUiTest {

	private static final String PASSWORD = "correct-horse-battery-staple";

	@Test
	public void testATokenOpensASessionAsTheUserOnce() {

		createEntityAsSuperUser("/User", "{ 'name': 'boss', 'password': '" + PASSWORD + "', 'isAdmin': true }");

		final String userId = createEntityAsSuperUser("/User", "{ 'name': 'tester', 'password': '" + PASSWORD + "' }");

		// the user may ask who they are, which is how the test sees whose session it is
		grant("User", UiAuthenticator.AUTH_USER_GET, true);
		final String token  = createToken("boss", userId);

		// any address that carries the token: here a REST endpoint
		final Response first = RestAssured
			.given()
				.queryParam(OneTimeTokens.PARAMETER, token)
			.expect()
				.statusCode(200)
				.body("result.name", equalTo("tester"))
			.when()
				.get("/me")
			.andReturn();

		final String sessionId = first.getSessionId();

		assertNotNull("redeeming the token did not answer with a session", sessionId);

		// the session is the user's, without the token
		RestAssured
			.given()
				.sessionId(sessionId)
			.expect()
				.statusCode(200)
				.body("result.name", equalTo("tester"))
			.when()
				.get("/me");

		// the token is used up
		RestAssured
			.given()
				.queryParam(OneTimeTokens.PARAMETER, token)
			.expect()
				.statusCode(401)
			.when()
				.get("/me");
	}

	@Test
	public void testAPageAnswersWithTheSameAddressWithoutTheToken() {

		createEntityAsSuperUser("/User", "{ 'name': 'boss2', 'password': '" + PASSWORD + "', 'isAdmin': true }");

		final String userId = createEntityAsSuperUser("/User", "{ 'name': 'tester2', 'password': '" + PASSWORD + "' }");

		// the user may ask who they are, which is how the test sees whose session it is
		grant("User", UiAuthenticator.AUTH_USER_GET, true);
		final String token  = createToken("boss2", userId);

		RestAssured.basePath = htmlUrl;

		final Response response = RestAssured
			.given()
				.redirects().follow(false)
				.queryParam("a", "1")
				.queryParam(OneTimeTokens.PARAMETER, token)
				.queryParam("b", "2")
			.expect()
				.statusCode(302)
			.when()
				.get("/some/page")
			.andReturn();

		RestAssured.basePath = restUrl;

		assertEquals("the redirect has to lead to the same address without the token", htmlUrl + "/some/page?a=1&b=2", response.getHeader("Location"));

		RestAssured
			.given()
				.sessionId(response.getSessionId())
			.expect()
				.statusCode(200)
				.body("result.name", equalTo("tester2"))
			.when()
				.get("/me");
	}

	@Test
	public void testATokenExpires() throws InterruptedException {

		createEntityAsSuperUser("/User", "{ 'name': 'boss3', 'password': '" + PASSWORD + "', 'isAdmin': true }");

		final String userId = createEntityAsSuperUser("/User", "{ 'name': 'tester3', 'password': '" + PASSWORD + "' }");
		final long validity = OneTimeTokens.getValidityMillis();

		try {

			OneTimeTokens.setValidityMillis(1);

			final String token = createToken("boss3", userId);

			Thread.sleep(50);

			RestAssured
				.given()
					.queryParam(OneTimeTokens.PARAMETER, token)
				.expect()
					.statusCode(401)
				.when()
					.get("/me");

		} finally {

			OneTimeTokens.setValidityMillis(validity);
		}
	}

	@Test
	public void testOnlyAnAdminCreatesATokenAndNotForABlockedUserOrAnAdmin() {

		createEntityAsSuperUser("/User", "{ 'name': 'boss4', 'password': '" + PASSWORD + "', 'isAdmin': true }");

		final String userId    = createEntityAsSuperUser("/User", "{ 'name': 'tester4', 'password': '" + PASSWORD + "' }");
		final String blockedId = createEntityAsSuperUser("/User", "{ 'name': 'blocked4', 'password': '" + PASSWORD + "', 'blocked': true }");
		final String adminId   = createEntityAsSuperUser("/User", "{ 'name': 'otheradmin4', 'password': '" + PASSWORD + "', 'isAdmin': true }");

		// a user who is no admin
		final int status = RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.headers(X_USER_HEADER, "tester4", X_PASSWORD_HEADER, PASSWORD)
				.body("{ 'user': '" + userId + "' }")
			.when()
				.post("/_oneTimeToken")
			.getStatusCode();

		assertTrue("a user who is no admin must not get a token, but the answer was " + status, status == 401 || status == 403);

		// nobody at all
		final int anonymous = RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.body("{ 'user': '" + userId + "' }")
			.when()
				.post("/_oneTimeToken")
			.getStatusCode();

		assertTrue("an anonymous request must not get a token, but the answer was " + anonymous, anonymous == 401 || anonymous == 403);

		// an admin, for a blocked user
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.headers(X_USER_HEADER, "boss4", X_PASSWORD_HEADER, PASSWORD)
				.body("{ 'user': '" + blockedId + "' }")
			.expect()
				.statusCode(403)
			.when()
				.post("/_oneTimeToken");

		// an admin, for another admin
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.headers(X_USER_HEADER, "boss4", X_PASSWORD_HEADER, PASSWORD)
				.body("{ 'user': '" + adminId + "' }")
			.expect()
				.statusCode(403)
			.when()
				.post("/_oneTimeToken");
	}

	@Test
	public void testATokenIsWorthlessOnceItsUserBecameAnAdmin() {

		createEntityAsSuperUser("/User", "{ 'name': 'boss5', 'password': '" + PASSWORD + "', 'isAdmin': true }");

		final String userId = createEntityAsSuperUser("/User", "{ 'name': 'tester5', 'password': '" + PASSWORD + "' }");
		final String token  = createToken("boss5", userId);

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.headers(X_USER_HEADER, "superadmin", X_PASSWORD_HEADER, "sehrgeheim")
				.body("{ 'isAdmin': true }")
			.expect()
				.statusCode(200)
			.when()
				.put("/User/" + userId);

		RestAssured
			.given()
				.queryParam(OneTimeTokens.PARAMETER, token)
			.expect()
				.statusCode(401)
			.when()
				.get("/me");
	}

	@Test
	public void testTheAddressWithoutTheToken() {

		final String p = OneTimeTokens.PARAMETER;

		assertEquals("/a",         OneTimeTokens.urlWithoutToken("/a", null));
		assertEquals("/a",         OneTimeTokens.urlWithoutToken("/a", p + "=x"));
		assertEquals("/a?b=1",     OneTimeTokens.urlWithoutToken("/a", p + "=x&b=1"));
		assertEquals("/a?b=1&c=2", OneTimeTokens.urlWithoutToken("/a", "b=1&" + p + "=x&c=2"));
		assertEquals("/a?" + p + "x=1", OneTimeTokens.urlWithoutToken("/a", p + "x=1"));

		assertNull("an unknown token must not be redeemed", OneTimeTokens.redeem("no-such-token"));
		assertNull("no token must not be redeemed",         OneTimeTokens.redeem(null));
	}

	private String createToken(final String admin, final String userId) {

		final String token = RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.headers(X_USER_HEADER, admin, X_PASSWORD_HEADER, PASSWORD)
				.body("{ 'user': '" + userId + "' }")
			.expect()
				.statusCode(200)
				.body("result.parameter", equalTo(OneTimeTokens.PARAMETER))
			.when()
				.post("/_oneTimeToken")
			.jsonPath().getString("result.token");

		assertNotNull("the admin did not get a token", token);

		return token;
	}
}
