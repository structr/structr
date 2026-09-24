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
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.structr.web.auth.UiAuthenticator;
import org.testng.annotations.Test;

import java.util.function.BooleanSupplier;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Ticket 1544: a login with a wrong password was counted once per authentication key instead of once.
 * AuthHelper.getPrincipalForKeysAndPassword tried the keys - the default eMail plus every key from
 * security.authentication.propertykeys - one after another, and each of those attempts looked the account
 * up by the key OR by name, so each found the same account, verified the same wrong password and
 * incremented the same failedAttempts counter. With one additional key, three wrong passwords blocked an
 * account whose policy allows four failed attempts.
 *
 * <p>The tests only state what the counter has to show after a failed login. How the lookup gets there
 * is the fix's business.
 */
public class FailedLoginAttemptsTest extends StructrUiTest {

	private static final String USER     = "bob";
	private static final String PASSWORD = "correct-horse-battery-staple";

	@Test
	public void testAFailedLoginCountsOnceWithTheDefaultKeys() {

		createUserAndAllowLogin();

		loginWithWrongPassword();
		assertEquals("one failed login must count once", 1, failedAttemptsOf(USER));

		loginWithWrongPassword();
		assertEquals("two failed logins must count twice", 2, failedAttemptsOf(USER));
	}

	@Test
	public void testAFailedLoginCountsOnceWithAnAdditionalAuthenticationKey() {

		// a second key the account can be looked up by, next to the default eMail: a string property on User
		createEntityAsSuperUser("/SchemaProperty", "{ name: \"memberId\", propertyType: \"String\", staticSchemaNodeName: \"User\" }");
		assertEventually("the additional property must be compiled into User", () -> Traits.of(StructrTraits.USER).hasKey("memberId"));

		Settings.AuthenticationPropertyKeys.setValue("User.memberId");

		try {

			createUserAndAllowLogin();

			loginWithWrongPassword();
			assertEquals("one failed login must count once, however many keys it is tried against", 1, failedAttemptsOf(USER));

			loginWithWrongPassword();
			assertEquals("two failed logins must count twice, however many keys they are tried against", 2, failedAttemptsOf(USER));

		} finally {

			Settings.AuthenticationPropertyKeys.setValue(Settings.AuthenticationPropertyKeys.getDefaultValue());
		}
	}

	/**
	 * The configuration the ticket was filed with: a key that is no use for logging in at all.
	 */
	@Test
	public void testAFailedLoginCountsOnceWithTheKeyFromTheTicket() {

		Settings.AuthenticationPropertyKeys.setValue("Principal.createdDate");

		try {

			createUserAndAllowLogin();

			loginWithWrongPassword();
			assertEquals("one failed login must count once, however many keys it is tried against", 1, failedAttemptsOf(USER));

		} finally {

			Settings.AuthenticationPropertyKeys.setValue(Settings.AuthenticationPropertyKeys.getDefaultValue());
		}
	}

	// ----- private methods -----
	private void createUserAndAllowLogin() {

		createEntityAsSuperUser("/User", "{ 'name': '" + USER + "', 'password': '" + PASSWORD + "' }");

		grant("_login", UiAuthenticator.NON_AUTH_USER_POST, true);
	}

	private void loginWithWrongPassword() {

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.body("{ 'name': '" + USER + "', 'password': 'not-the-password' }")
			.expect()
				.statusCode(401)
			.when()
				.post("/login");
	}

	private int failedAttemptsOf(final String name) {

		try (final Tx tx = app.tx()) {

			final NodeInterface user = app.nodeQuery(StructrTraits.USER).name(name).getFirst();
			final Integer attempts   = user.getProperty(Traits.of(StructrTraits.PRINCIPAL).key(PrincipalTraitDefinition.PASSWORD_ATTEMPTS_PROPERTY));

			tx.success();

			return attempts != null ? attempts : 0;

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
		}

		return -1;
	}

	private void assertEventually(final String message, final BooleanSupplier condition) {

		for (int attempt = 0; attempt < 200; attempt++) {

			if (condition.getAsBoolean()) {

				return;
			}

			try { Thread.sleep(100); } catch (InterruptedException ignored) {}
		}

		assertTrue(message, condition.getAsBoolean());
	}
}
