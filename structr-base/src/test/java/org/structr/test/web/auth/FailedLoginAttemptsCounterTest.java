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
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.structr.web.auth.UiAuthenticator;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

/**
 * A wrong password must increment the account's {@code passwordAttempts} counter no matter which
 * entry point rejected it, and the increment has to survive even though the request that triggered
 * it ultimately fails. Header authentication (X-User/X-Password) in particular runs the failed
 * check inside a transaction that the caller's servlet code rolls back once the resulting
 * {@link org.structr.core.auth.exception.AuthenticationException} propagates, so the increment can
 * only survive if it commits independently of that transaction.
 */
public class FailedLoginAttemptsCounterTest extends StructrUiTest {

	private static final String PASSWORD = "correct-horse-battery-staple";
	private static final String WRONG    = "wrong-horse-battery-staple";

	@Test
	public void testHeaderAuthenticationFailureIncrementsPasswordAttempts() {

		createEntityAsSuperUser("/User", "{ 'name': 'headerauthuser', 'password': '" + PASSWORD + "'}");

		grant(StructrTraits.USER, UiAuthenticator.AUTH_USER_GET, true);

		assertEquals("Freshly created user must start with no failed attempts", Integer.valueOf(0), getPasswordAttempts("headerauthuser"));

		// three separate requests with the wrong X-Password header, each must be rejected with 401
		testGet("/User", "headerauthuser", WRONG, 401);
		testGet("/User", "headerauthuser", WRONG, 401);
		testGet("/User", "headerauthuser", WRONG, 401);

		assertEquals("Three failed header-authentication logins must increment passwordAttempts three times", Integer.valueOf(3), getPasswordAttempts("headerauthuser"));

		// a correct request afterwards resets the failed attempt count, exactly like a successful REST login does
		testGet("/User", "headerauthuser", PASSWORD, 200);

		assertEquals("A successful header-authentication request must reset the failed attempt count", Integer.valueOf(0), getPasswordAttempts("headerauthuser"));
	}

	@Test
	public void testRestLoginFailureIncrementsPasswordAttempts() {

		createEntityAsSuperUser("/User", "{ 'name': 'loginuser', 'password': '" + PASSWORD + "'}");

		grant("_login", UiAuthenticator.NON_AUTH_USER_POST, true);

		assertEquals("Freshly created user must start with no failed attempts", Integer.valueOf(0), getPasswordAttempts("loginuser"));

		for (int i = 1; i <= 3; i++) {

			RestAssured.given()
				.contentType("application/json; charset=UTF-8")
				.body("{ 'name': 'loginuser', 'password': '" + WRONG + "' }")
			.expect()
				.statusCode(401)
			.when()
				.post("/login");

			assertEquals("Failed REST login attempt #" + i + " must increment passwordAttempts", Integer.valueOf(i), getPasswordAttempts("loginuser"));
		}

		// a successful login must not add to the failed attempt count, and resets it back to zero
		final String sessionId = RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body("{ 'name': 'loginuser', 'password': '" + PASSWORD + "' }")
		.expect()
			.statusCode(200)
		.when()
			.post("/login")
		.getSessionId();

		assertNotNull(sessionId);

		assertEquals("A successful login must reset the failed attempt count", Integer.valueOf(0), getPasswordAttempts("loginuser"));
	}

	@Test
	public void testHeaderAndLoginFailuresShareTheSameCounter() {

		createEntityAsSuperUser("/User", "{ 'name': 'mixeduser', 'password': '" + PASSWORD + "'}");

		grant(StructrTraits.USER, UiAuthenticator.AUTH_USER_GET, true);
		grant("_login", UiAuthenticator.NON_AUTH_USER_POST, false);

		// fail once via header authentication ...
		testGet("/User", "mixeduser", WRONG, 401);

		assertEquals("Failed header authentication must increment the counter", Integer.valueOf(1), getPasswordAttempts("mixeduser"));

		// ... and once via the REST /login endpoint - both paths go through the same AuthHelper code
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body("{ 'name': 'mixeduser', 'password': '" + WRONG + "' }")
		.expect()
			.statusCode(401)
		.when()
			.post("/login");

		assertEquals("Both authentication paths must accumulate on the same counter", Integer.valueOf(2), getPasswordAttempts("mixeduser"));
	}

	private Integer getPasswordAttempts(final String name) {

		final Traits traits = Traits.of(StructrTraits.USER);

		try (final Tx tx = app.tx()) {

			final NodeInterface user = app.nodeQuery(StructrTraits.USER)
				.key(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name)
				.getFirst();

			assertNotNull("test account " + name + " was not created", user);

			Integer attempts = user.getProperty(traits.key(PrincipalTraitDefinition.PASSWORD_ATTEMPTS_PROPERTY));
			if (attempts == null) {

				attempts = 0;
			}

			tx.success();

			return attempts;

		} catch (FrameworkException fex) {

			fail("Unexpected exception while reading passwordAttempts for " + name + ": " + fex.getMessage());
			return null;
		}
	}
}
