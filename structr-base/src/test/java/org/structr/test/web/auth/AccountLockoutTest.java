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
import org.structr.core.traits.definitions.UserTraitDefinition;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.testng.AssertJUnit.fail;

/**
 * Ticket 1598: anybody who knows a user name can take that account out of service.
 *
 * <p>The failed-attempt counter only ever grew, and the block it produced never lifted - so five
 * requests with a wrong password left the account unusable. What clears the counter is a successful
 * login, which is the thing being blocked, or a password reset, and that one only on an instance where
 * jsonrestservlet.user.autologin is switched on. It is off by default, so by default the victim cannot
 * clear it themselves at all and an administrator has to.
 *
 * <p>The second half needs no password at all. A password reset writes a confirmationKey onto the
 * account, and a login was refused for as long as that key was set - a property meant for unconfirmed
 * registrations. POST /reset-password with a stranger's e-mail address therefore blocked their login
 * until they clicked a mail they never asked for, and an expired key was not cleared either.
 *
 * <p>Time is moved by backdating the stored timestamp rather than by waiting, so these tests take
 * milliseconds and still exercise the real expiry arithmetic.
 */
public class AccountLockoutTest extends StructrUiTest {

	private static final String PASSWORD = "correct-horse-battery-staple";
	private static final String WRONG    = "wrong-horse-battery-staple";

	/**
	 * The attack, and the property that ends it: the block has to let go on its own.
	 */
	@Test
	public void testALockedAccountIsReleasedWhenTheWindowHasPassed() {

		createEntityAsSuperUser("/User", "{ 'name': 'victim', 'password': '" + PASSWORD + "' }");

		grant("_login", UiAuthenticator.NON_AUTH_USER_POST, true);

		lockOut("victim");

		assertLogin("victim", PASSWORD, 401, "the account is not locked at all, so the lockout is not being applied");

		// bracketed from both sides, or a window of any length at all would satisfy the assertion below
		backdateLastFailure("victim", 4);

		assertLogin("victim", PASSWORD, 401, "the first window is shorter than the five minutes it is meant to be");

		backdateLastFailure("victim", 6);

		assertLogin("victim", PASSWORD, 200, "the account is still locked after the first window has passed, so the block never expires");
	}

	/**
	 * The third step, and then the point of the whole arrangement: the window stops growing. An hour is
	 * where it stops on purpose - a block that keeps lengthening turns back into the denial of service
	 * this exists to prevent, and a long run of failures is exactly what somebody attacking an account
	 * produces. Without this test the cap could be set to a day and nothing would notice.
	 */
	@Test
	public void testTheWindowStopsGrowingAtAnHour() {

		createEntityAsSuperUser("/User", "{ 'name': 'stubborn', 'password': '" + PASSWORD + "' }");

		grant("_login", UiAuthenticator.NON_AUTH_USER_POST, true);

		// three past the limit: the third step, half an hour
		failLogins("stubborn", Settings.PasswordAttempts.getValue() + 3);

		backdateLastFailure("stubborn", 16);

		assertLogin("stubborn", PASSWORD, 401, "the third window is no longer than the second, so the steps have stopped rising too early");

		backdateLastFailure("stubborn", 31);

		assertLogin("stubborn", PASSWORD, 200, "the third window is longer than the half hour it is meant to be");

		// and far past it: the window must not have grown any further
		failLogins("stubborn", Settings.PasswordAttempts.getValue() + 9);

		backdateLastFailure("stubborn", 61);

		assertLogin("stubborn", PASSWORD, 200, "the window is still growing past an hour, so a long run of failures locks the account out far longer than intended");
	}

	/**
	 * A block that always lasted five minutes would be no obstacle to someone working through a list, so
	 * the window grows with every further failure. Five minutes in, the account that failed twice over
	 * the limit is still locked; sixteen minutes in it is not.
	 */
	@Test
	public void testTheWindowGrowsWithFurtherFailures() {

		createEntityAsSuperUser("/User", "{ 'name': 'persistent', 'password': '" + PASSWORD + "' }");

		grant("_login", UiAuthenticator.NON_AUTH_USER_POST, true);

		lockOut("persistent");

		// one more failure than the test above, so the second step of the backoff applies
		assertLogin("persistent", WRONG, 401, "a wrong password was accepted");

		backdateLastFailure("persistent", 6);

		assertLogin("persistent", PASSWORD, 401, "the second failure did not lengthen the window");

		backdateLastFailure("persistent", 16);

		assertLogin("persistent", PASSWORD, 200, "the second window does not expire either");
	}

	/**
	 * The other half of the ticket, and the one that needs no password: a reset request must not be a way
	 * to lock somebody out. The account has been used before, so the confirmationKey on it is a reset key
	 * and not an unconfirmed registration.
	 */
	@Test
	public void testAPasswordResetDoesNotBlockAnAccountThatIsInUse() {

		createEntityAsSuperUser("/User", "{ 'name': 'inuse', 'password': '" + PASSWORD + "' }");

		grant("_login", UiAuthenticator.NON_AUTH_USER_POST, true);

		assertLogin("inuse", PASSWORD, 200, "the account cannot be used at all");

		// what POST /reset-password writes onto the account of whoever's e-mail address was supplied
		setConfirmationKey("inuse", "reset-key-from-a-stranger");

		assertLogin("inuse", PASSWORD, 200, "a reset request somebody else asked for blocks the login of an account in use");
	}

	/**
	 * The counterpart, so the rule above does not turn into "confirmation keys mean nothing": an account
	 * that has never been logged into is an unconfirmed registration, and that is what the key is for.
	 */
	@Test
	public void testAnUnconfirmedRegistrationStillCannotLogIn() {

		createEntityAsSuperUser("/User", "{ 'name': 'unconfirmed', 'password': '" + PASSWORD + "' }");

		grant("_login", UiAuthenticator.NON_AUTH_USER_POST, true);

		setConfirmationKey("unconfirmed", "key-from-the-registration");

		assertLogin("unconfirmed", PASSWORD, 401, "an unconfirmed registration can log in");
	}

	// ----- private methods -----

	/**
	 * Spends the account's budget of failed attempts, one request past the configured maximum, which is
	 * where the lockout begins.
	 */
	private void lockOut(final String name) {

		failLogins(name, Settings.PasswordAttempts.getValue() + 1);
	}

	/** Spends exactly the given number of failed attempts, so a test can name the step it is aiming at. */
	private void failLogins(final String name, final int count) {

		for (int i = 0; i < count; i++) {

			assertLogin(name, WRONG, 401, "a wrong password was accepted");
		}
	}

	private void assertLogin(final String name, final String password, final int expected, final String message) {

		final int statusCode = RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.body("{ 'name': '" + name + "', 'password': '" + password + "' }")
			.when()
				.post("/login")
			.getStatusCode();

		if (statusCode != expected) {

			fail(message + " (expected HTTP " + expected + ", got " + statusCode + ")");
		}
	}

	/** Moves the recorded moment of the last failure into the past, which is how time passes here. */
	private void backdateLastFailure(final String name, final int minutes) {

		try (final Tx tx = app.tx()) {

			principal(name).setProperty(Traits.of(StructrTraits.PRINCIPAL).key(PrincipalTraitDefinition.LAST_FAILED_LOGIN_DATE_PROPERTY),
				new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(minutes)));

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	private void setConfirmationKey(final String name, final String key) {

		try (final Tx tx = app.tx()) {

			principal(name).setProperty(Traits.of(StructrTraits.USER).key(UserTraitDefinition.CONFIRMATION_KEY_PROPERTY), key);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	private NodeInterface principal(final String name) throws FrameworkException {

		final NodeInterface node = app.nodeQuery(StructrTraits.USER).name(name).getFirst();
		if (node == null) {

			fail("user " + name + " was not created");
		}

		return node;
	}
}
