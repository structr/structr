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
import org.structr.core.auth.exception.TwoFactorAuthenticationFailedException;
import org.structr.core.auth.exception.TwoFactorAuthenticationRequiredException;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.core.traits.definitions.UserTraitDefinition;
import org.structr.rest.auth.AuthHelper;
import org.structr.test.web.StructrUiTest;
import org.structr.web.servlet.HtmlServlet;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.testng.AssertJUnit.*;

/**
 * The security properties of two-factor authentication, as opposed to whether the happy path works.
 *
 * <p>Ticket 1583: two-factor authentication could be skipped or worn down in four ways. The IP
 * allowlist is gone entirely and has nothing left to test; the other three are covered here.
 *
 * <p>Ticket 1610 adds the fifth way, found while working on 1583: the scripting function login(),
 * which an application uses to delegate authentication to an external source and which cannot ask
 * for a code at all.
 */
public class TwoFactorAuthenticationTest extends StructrUiTest {

	private static final String USER_AGENT = "TwoFactorAuthenticationTest";
	private static final String PASSWORD   = "correct horse battery staple";

	/**
	 * A wrong code used to cost nothing at all: no counter, and the token stayed usable for the whole
	 * of security.twofactorauthentication.logintimeout, so the window could be guessed out and a fresh
	 * window was one password round away - a round that reset the counter as it went. Six digits and no
	 * limit is a delay, not a factor.
	 */
	@Test
	public void testWrongCodesAreCountedAndUseUpTheToken() {

		final Integer previousLevel = Settings.TwoFactorLevel.getValue();

		Settings.TwoFactorLevel.setValue(2);

		try {

			final int maximumAllowedFailedAttempts = Settings.PasswordAttempts.getValue();
			final String id                        = createTwoFactorUser("counted", true);
			final String token                     = startSecondFactor(id);

			assertNotNull("The password step must issue a token", token);

			// one guess more than the account is allowed
			for (int i = 0; i <= maximumAllowedFailedAttempts; i++) {

				try (final Tx tx = app.tx()) {

					AuthHelper.handleTwoFactorAuthentication(app.getNodeById(id).as(Principal.class), "000000", token, USER_AGENT, null);

					tx.success();

					fail("A wrong two-factor code must be refused");

				} catch (TwoFactorAuthenticationFailedException expected) {

					// that is what a wrong code does

				} catch (FrameworkException fex) {

					fex.printStackTrace();
					fail("Unexpected exception: " + fex.getMessage());
				}
			}

			try (final Tx tx = app.tx()) {

				final Principal user = app.getNodeById(id).as(Principal.class);

				assertTrue("Wrong codes must feed the same counter a wrong password feeds, but the counter is at " + user.getPasswordAttempts(),
					user.getPasswordAttempts() != null && user.getPasswordAttempts() > maximumAllowedFailedAttempts);

				assertNull("The token must be gone once the attempts are used up, otherwise the window can simply be guessed out",
					user.getProperty(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.TWO_FACTOR_TOKEN_PROPERTY)));

				/* Feeding the counter is only half of it: the lockout it stands for has to actually hold.
				   Since ticket 1598 that lockout expires from the moment of the last failure, so a code
				   step that raises the counter without recording that moment would leave a block that
				   reads as one which had already run out - the counter over the limit and the account
				   open. Asserting the counter alone would not notice. */
				assertNotNull("Wrong codes must record when they were entered, or the lockout they add up to expires before it begins",
					user.getProperty(Traits.of(StructrTraits.PRINCIPAL).key(PrincipalTraitDefinition.LAST_FAILED_LOGIN_DATE_PROPERTY)));

				tx.success();

			} catch (FrameworkException fex) {

				fex.printStackTrace();
				fail("Unexpected exception: " + fex.getMessage());
			}

		} finally {

			Settings.TwoFactorLevel.setValue(previousLevel);
		}
	}

	/**
	 * The QR code handed out during enrolment carries the TOTP secret, and a password is all it takes to
	 * ask for it. Two parties who both know the password must not end up holding the same working
	 * secret, because whoever confirms first makes it permanent for both.
	 */
	@Test
	public void testEnrolmentSecretIsRotatedOnEveryHandout() {

		final Integer previousLevel = Settings.TwoFactorLevel.getValue();

		Settings.TwoFactorLevel.setValue(2);

		try {

			final String id = createTwoFactorUser("enrolling", false);
			final String firstSecret = getSecret(id);

			startSecondFactor(id);

			final String secretAfterFirstHandout = getSecret(id);

			startSecondFactor(id);

			final String secretAfterSecondHandout = getSecret(id);

			assertNotNull("The user must have a secret to begin with", firstSecret);

			assertFalse("An unconfirmed enrolment must not hand out the secret it was already asked for: " + firstSecret, firstSecret.equals(secretAfterFirstHandout));

			assertFalse("Every handout must rotate, otherwise an earlier recipient keeps a working secret: " + secretAfterFirstHandout,
				secretAfterFirstHandout.equals(secretAfterSecondHandout));

		} finally {

			Settings.TwoFactorLevel.setValue(previousLevel);
		}
	}

	/**
	 * The counterpart: rotation belongs to enrolment only. A confirmed user has the secret in their
	 * authenticator app, and rotating it there would lock them out on every login.
	 */
	@Test
	public void testConfirmedUserKeepsTheirSecret() {

		final Integer previousLevel = Settings.TwoFactorLevel.getValue();

		Settings.TwoFactorLevel.setValue(2);

		try {

			final String id           = createTwoFactorUser("confirmed", true);
			final String beforeLogin  = getSecret(id);

			startSecondFactor(id);

			assertEquals("A confirmed user's secret must survive the login, it is in their authenticator app", beforeLogin, getSecret(id));

		} finally {

			Settings.TwoFactorLevel.setValue(previousLevel);
		}
	}

	/**
	 * A registration confirmation link proves possession of a mailbox and nothing else, and this path
	 * never went near handleTwoFactorAuthentication(): clicking the link in the e-mail opened a session
	 * for an account the configuration says needs a second factor.
	 */
	@Test
	public void testRegistrationConfirmationDoesNotSkipTheSecondFactor() {

		final Integer previousLevel    = Settings.TwoFactorLevel.getValue();
		final Boolean previousAutologin = Settings.RestUserAutologin.getValue();

		Settings.SmtpTesting.setValue(true);
		Settings.RestUserAutologin.setValue(true);
		Settings.TwoFactorLevel.setValue(2);

		try {

			final String confirmationKey = createUserWithConfirmationKey("confirmme");

			RestAssured.basePath = htmlUrl;

			RestAssured
				.given()
					.redirects().follow(false)
					.param(HtmlServlet.CONFIRMATION_KEY_KEY, confirmationKey)
				.expect()
					.statusCode(302)
					.header("Location", containsString(Settings.TwoFactorLoginPage.getValue()))
				.when()
					.get(HtmlServlet.CONFIRM_REGISTRATION_PAGE);

		} finally {

			Settings.TwoFactorLevel.setValue(previousLevel);
			Settings.RestUserAutologin.setValue(previousAutologin);
			RestAssured.basePath = restUrl;
		}
	}

	/**
	 * The worst of the four, because a password reset is the one flow an attacker reaches without
	 * knowing the password at all: the reset link logged the user straight in.
	 */
	@Test
	public void testPasswordResetLoginDoesNotSkipTheSecondFactor() {

		final Integer previousLevel      = Settings.TwoFactorLevel.getValue();
		final Boolean previousAutologin  = Settings.RestUserAutologin.getValue();
		final Boolean previousResetOnPwr = Settings.PasswordResetFailedCounterOnPWReset.getValue();

		Settings.SmtpTesting.setValue(true);
		Settings.RestUserAutologin.setValue(true);
		Settings.TwoFactorLevel.setValue(2);

		/* Deliberately left at its default: this endpoint calls resetFailedLoginAttemptsCounter() while
		   its own transaction is holding the user node, and that method used to do its write on another
		   thread and join it - the request then waited forever. So this test covers the deadlock as well,
		   and turning the setting off here would take that coverage away. */
		Settings.PasswordResetFailedCounterOnPWReset.setValue(true);

		try {

			final String confirmationKey = createUserWithConfirmationKey("resetme");

			RestAssured.basePath = htmlUrl;

			RestAssured
				.given()
					.redirects().follow(false)
					.param(HtmlServlet.CONFIRMATION_KEY_KEY, confirmationKey)
				.expect()
					.statusCode(302)
					.header("Location", containsString(Settings.TwoFactorLoginPage.getValue()))
				.when()
					.get(HtmlServlet.RESET_PASSWORD_PAGE);

		} finally {

			Settings.TwoFactorLevel.setValue(previousLevel);
			Settings.RestUserAutologin.setValue(previousAutologin);
			Settings.PasswordResetFailedCounterOnPWReset.setValue(previousResetOnPwr);
			RestAssured.basePath = restUrl;
		}
	}

	/**
	 * Ticket 1610: the scripting function login() checks the password and calls doLogin() right after,
	 * with no step in between that could ask for a code. An application with its own login form built
	 * on ${login(user, password)} therefore opens a session on the password alone, while
	 * /structr/rest/login, the websocket LOGIN and the token endpoint all demand the second factor.
	 * Whether the configured level means anything then depends on which login an application happens
	 * to use, and nothing about that is visible from the configuration.
	 *
	 * <p>The function is called through a global schema method, because doLogin() needs the request
	 * and the session that only a real one brings.
	 */
	@Test
	public void testTheLoginFunctionDoesNotSkipTheSecondFactor() {

		final Integer previousLevel = Settings.TwoFactorLevel.getValue();

		// level 1, so only the account flagged as a two-factor user needs a code and the admin this
		// test authenticates with does not
		Settings.TwoFactorLevel.setValue(1);

		try {

			createAdminUserForRestCalls();
			createTwoFactorUser("scripted", true);
			createLoginMethod();

			RestAssured
				.given()
					.contentType("application/json; charset=UTF-8")
					.headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
					.body("{ 'name': 'scripted', 'password': '" + PASSWORD + "' }")
				.expect()
					.statusCode(200)
					.body("result", equalTo(false))
				.when()
					.post("/doScriptedLogin");

			assertTrue("login() opened a session for an account that the configuration says needs a second factor", sessionIdsOf("scripted").isEmpty());

		} finally {

			Settings.TwoFactorLevel.setValue(previousLevel);
		}
	}

	/**
	 * The other direction of the same rule, because refusing too much would be just as wrong: at level 1
	 * an account that is not flagged as a two-factor user is never asked for a code on any login path, so
	 * login() has to let it through and open its session as before.
	 */
	@Test
	public void testTheLoginFunctionStillLogsInAnAccountThatNeedsNoSecondFactor() {

		final Integer previousLevel = Settings.TwoFactorLevel.getValue();

		Settings.TwoFactorLevel.setValue(1);

		try {

			createAdminUserForRestCalls();
			createUser("ordinary");
			createLoginMethod();

			RestAssured
				.given()
					.contentType("application/json; charset=UTF-8")
					.headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
					.body("{ 'name': 'ordinary', 'password': '" + PASSWORD + "' }")
				.expect()
					.statusCode(200)
					.body("result", equalTo(true))
				.when()
					.post("/doScriptedLogin");

			assertFalse("login() did not open a session for an account that needs no second factor", sessionIdsOf("ordinary").isEmpty());

		} finally {

			Settings.TwoFactorLevel.setValue(previousLevel);
		}
	}

	/**
	 * The same function's return value, which its own description promises: "Returns true on successful
	 * login". A wrong password never produces false - getPrincipalForPassword() throws, the exception
	 * runs out of apply() and reaches the caller as a 401, so an application cannot tell a wrong
	 * password from any other failure and can never render its own error.
	 */
	@Test
	public void testTheLoginFunctionReportsAWrongPassword() {

		final Integer previousLevel = Settings.TwoFactorLevel.getValue();

		Settings.TwoFactorLevel.setValue(0);

		try {

			createAdminUserForRestCalls();
			createTwoFactorUser("wrongpassword", true);
			createLoginMethod();

			RestAssured
				.given()
					.contentType("application/json; charset=UTF-8")
					.headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
					.body("{ 'name': 'wrongpassword', 'password': 'not the password' }")
				.expect()
					.statusCode(200)
					.body("result", equalTo(false))
				.when()
					.post("/doScriptedLogin");

			assertTrue("a refused login must not open a session", sessionIdsOf("wrongpassword").isEmpty());

		} finally {

			Settings.TwoFactorLevel.setValue(previousLevel);
		}
	}

	// ----- private methods -----
	/**
	 * A global schema method that does what an application's own login form does: look the account up
	 * and hand it to login().
	 */
	private void createLoginMethod() {

		final Traits traits = Traits.of(StructrTraits.SCHEMA_METHOD);

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),  "doScriptedLogin"),
				new NodeAttribute<>(traits.key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), "{ const params = $.methodParameters; const user = $.find('User', { name: params.name })[0]; return $.login(user, params.password); }")
			);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception creating the login method: " + fex.getMessage());
		}
	}

	private void createUser(final String name) {

		final Traits traits = Traits.of(StructrTraits.USER);

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.USER,
				new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),     name),
				new NodeAttribute<>(traits.key(PrincipalTraitDefinition.PASSWORD_PROPERTY),     PASSWORD)
			);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception creating the user: " + fex.getMessage());
		}
	}

	/**
	 * The account the REST calls authenticate with. createAdminUser() reports a failure by returning
	 * null, and without this check that failure would show up much later as a 401 on the call itself.
	 */
	private void createAdminUserForRestCalls() {

		assertNotNull("could not create the admin user the test authenticates with", createAdminUser());
	}

	private List<String> sessionIdsOf(final String name) {

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.nodeQuery(StructrTraits.USER).name(name).getFirst();
			if (node == null) {

				fail("user " + name + " was not created");
			}

			final String[] sessionIds = node.as(Principal.class).getSessionIds();

			tx.success();

			return sessionIds != null ? Arrays.asList(sessionIds) : List.of();

		} catch (FrameworkException fex) {

			fail("Unexpected exception reading the sessions of " + name + ": " + fex.getMessage());

			return List.of();
		}
	}

	private String createTwoFactorUser(final String name, final boolean confirmed) {

		final Traits traits = Traits.of(StructrTraits.USER);
		String id           = null;

		try (final Tx tx = app.tx()) {

			final NodeInterface user = app.create(StructrTraits.USER,
				new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name),
				new NodeAttribute<>(traits.key(PrincipalTraitDefinition.PASSWORD_PROPERTY), PASSWORD)
			);

			id = user.getUuid();

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception creating the user: " + fex.getMessage());
		}

		/* A second transaction, because UserTraitDefinition.onCreation() runs when the first one commits:
		   it generates a secret for a user that has none and forces both flags to false on its way
		   through, so anything written before that commit is overwritten again. */
		try (final Tx tx = app.tx()) {

			final NodeInterface user = app.getNodeById(id);

			user.setProperty(traits.key(PrincipalTraitDefinition.IS_TWO_FACTOR_USER_PROPERTY), true);
			user.setProperty(traits.key(PrincipalTraitDefinition.TWO_FACTOR_CONFIRMED_PROPERTY), confirmed);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception preparing the user: " + fex.getMessage());
		}

		return id;
	}

	private String createUserWithConfirmationKey(final String name) {

		final Traits traits = Traits.of(StructrTraits.USER);

		try (final Tx tx = app.tx()) {

			final NodeInterface user = app.create(StructrTraits.USER,
				new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name),
				new NodeAttribute<>(traits.key(PrincipalTraitDefinition.EMAIL_PROPERTY), name + "@structr.com"),
				new NodeAttribute<>(traits.key(PrincipalTraitDefinition.PASSWORD_PROPERTY), PASSWORD),
				new NodeAttribute<>(traits.key(UserTraitDefinition.CONFIRMATION_KEY_PROPERTY), AuthHelper.getConfirmationKey())
			);

			final String confirmationKey = user.getProperty(traits.key(UserTraitDefinition.CONFIRMATION_KEY_PROPERTY));

			tx.success();

			assertNotNull("The user needs a confirmation key for this test", confirmationKey);

			return confirmationKey;

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception creating the user: " + fex.getMessage());

			return null;
		}
	}

	/**
	 * Does what the password step does: no token yet, so a code is demanded and a token issued. Returns
	 * that token.
	 */
	private String startSecondFactor(final String id) {

		String token = null;

		try (final Tx tx = app.tx()) {

			try {

				AuthHelper.handleTwoFactorAuthentication(app.getNodeById(id).as(Principal.class), null, null, USER_AGENT, null);

				fail("A user who needs a second factor must not get past the password step");

			} catch (TwoFactorAuthenticationRequiredException expected) {

				/* Caught inside the transaction, exactly as LoginResourceHandler does it, because the
				   issued token and the rotated secret are written before the throw - catching outside the
				   try-with-resources would roll back the very writes this test is about. */
				token = expected.getData().get("token");
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}

		return token;
	}

	private String getSecret(final String id) {

		try (final Tx tx = app.tx()) {

			final String secret = app.getNodeById(id).as(Principal.class).getTwoFactorSecret();

			tx.success();

			return secret;

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());

			return null;
		}
	}
}
