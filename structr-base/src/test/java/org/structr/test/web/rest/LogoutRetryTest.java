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
package org.structr.test.web.rest;

import io.restassured.RestAssured;
import org.structr.api.Predicate;
import org.structr.api.RetryException;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.StructrTransactionListener;
import org.structr.core.entity.Principal;
import org.structr.core.graph.ModificationEvent;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.rest.auth.JWTHelper;
import org.structr.test.web.StructrUiTest;
import org.structr.web.auth.UiAuthenticator;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

/**
 * A logout clears the refresh tokens of the user. A transaction listener throws a RetryException before the commit of
 * a transaction that clears them, as often as a test asks for, which is what a driver does when the database terminates
 * the transaction at commit. The repeated logout must clear the tokens and end the session like one that was not
 * terminated.
 */
public class LogoutRetryTest extends StructrUiTest {

	private static final String USER_NAME = "User1";
	private static final String PASSWORD  = "geheim";

	// how many of the next commits that clear refresh tokens fail
	private static final AtomicInteger commitFailures = new AtomicInteger();

	private final StructrTransactionListener terminator = new TerminatingListener();
	private String previousSecret                       = null;

	@BeforeMethod
	public void setUpTermination() {

		commitFailures.set(0);

		previousSecret = Settings.JWTSecret.getValue();
		Settings.JWTSecret.setValue("a test secret that is long enough for signing tokens");

		TransactionCommand.registerTransactionListener(terminator);
	}

	@AfterMethod
	public void tearDownTermination() {

		TransactionCommand.removeTransactionListener(terminator);

		Settings.JWTSecret.setValue(previousSecret);
	}

	@Test
	public void testTerminatedLogoutIsRepeated() {

		createUser();

		final String sessionId = RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body("{ 'name': '" + USER_NAME + "', 'password': '" + PASSWORD + "'}")
		.expect()
			.statusCode(200)
		.when()
			.post("/login")
		.getSessionId();

		assertNotNull("The login did not create a session", sessionId);

		createTokens();

		commitFailures.set(1);

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.sessionId(sessionId)
		.expect()
			.statusCode(200)
		.when()
			.post("/logout");

		assertEquals("The terminated logout was not repeated", 0, commitFailures.get());
		assertEquals("The repeated logout left refresh tokens of the user valid", 0, countRefreshTokens());

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.sessionId(sessionId)
		.expect()
			.statusCode(401)
		.when()
			.get("/me");
	}

	@Test
	public void testLogoutWithAccessTokenClearsRefreshTokens() {

		createUser();

		final String accessToken = createTokens().get("access_token");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.header("Authorization", "Bearer " + accessToken)
		.expect()
			.statusCode(200)
		.when()
			.post("/logout");

		assertEquals("A logout without a session left refresh tokens of the user valid", 0, countRefreshTokens());
	}

	// ----- private methods -----
	private void createUser() {

		createEntityAsSuperUser("/User", "{ 'name': '" + USER_NAME + "', 'password': '" + PASSWORD + "'}");

		grant("_login", UiAuthenticator.NON_AUTH_USER_POST, true);
		grant("_logout", UiAuthenticator.AUTH_USER_POST, false);
		grant("User", UiAuthenticator.AUTH_USER_GET, false);
	}

	private Map<String, String> createTokens() {

		try (final Tx tx = app.tx()) {

			final Map<String, String> tokens = JWTHelper.createTokensForUser(getUser());

			tx.success();

			return tokens;

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		return null;
	}

	private int countRefreshTokens() {

		try (final Tx tx = app.tx()) {

			final String[] tokens = getUser().getRefreshTokens();

			tx.success();

			return tokens != null ? tokens.length : 0;

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		return -1;
	}

	private Principal getUser() throws FrameworkException {

		final NodeInterface user = app.nodeQuery(StructrTraits.USER).name(USER_NAME).getFirst();

		assertNotNull("The test user does not exist", user);

		return user.as(Principal.class);
	}

	private static PropertyKey<String[]> refreshTokensKey() {

		return Traits.of(StructrTraits.PRINCIPAL).key(PrincipalTraitDefinition.REFRESH_TOKENS_PROPERTY);
	}

	/**
	 * Consumes one of the failures and returns true if there was one left.
	 */
	private static boolean consumeFailure() {

		return commitFailures.getAndUpdate(value -> value > 0 ? value - 1 : 0) > 0;
	}

	// ----- nested classes -----
	private static class TerminatingListener implements StructrTransactionListener {

		@Override
		public void beforeCommit(final SecurityContext securityContext, final Collection<ModificationEvent> modificationEvents) throws FrameworkException {

			for (final ModificationEvent event : modificationEvents) {

				// only the first key modified in a transaction is in the modified properties, the others are in the new ones
				final boolean clearsTokens = event.getModifiedProperties().containsKey(refreshTokensKey()) || event.getNewProperties().containsKey(refreshTokensKey());

				if (clearsTokens && consumeFailure()) {

					throw new RetryException("Simulated termination of the transaction");
				}
			}
		}

		@Override
		public void afterCommit(final SecurityContext securityContext, final Collection<ModificationEvent> modificationEvents) {
		}

		@Override
		public void simpleBroadcast(final String messageName, final Map<String, Object> data, final Predicate<String> sessionIdPredicate) {
		}
	}
}
