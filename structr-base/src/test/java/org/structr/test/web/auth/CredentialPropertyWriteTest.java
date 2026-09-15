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
import org.structr.common.AccessControllable;
import org.structr.common.Permission;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.rest.auth.AuthHelper;
import org.structr.core.auth.exception.TwoFactorAuthenticationTokenInvalidException;
import org.structr.test.web.StructrUiTest;
import org.structr.web.auth.UiAuthenticator;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.fail;

/**
 * Ticket 1584, the half the view restriction does not reach. The ticket's impact section ends with
 * "Mit Schreibrecht: eigene Session-ID oder Legacy-twoFactorToken eintragen - stiller Hijack", and its
 * proposed solution asked for the credential-carrying properties to be readOnly for non-superusers.
 * What was implemented instead restricts the internal views, which is a read path: a PUT resolves
 * property keys by name, not by view, so it is untouched by it.
 *
 * <p>In PrincipalTraitDefinition.createPropertyKeys() isAdmin carries .readOnly(); sessionIds,
 * refreshTokens and twoFactorToken do not, and neither does User.confirmationKey. Each of them is a
 * credential in its own right:
 *
 * <ul>
 * <li>sessionIds is matched against the session cookie (AuthHelper.getPrincipalForSessionId), so
 * writing one's own session id onto another account is that account.</li>
 * <li>twoFactorToken is matched the same way (AuthHelper:724), and isTwoFactorTokenValid() still
 * accepts the unsigned legacy format "anything!&lt;timestamp&gt;", so a written one needs no
 * signature.</li>
 * <li>confirmationKey logs its holder in through /confirm_registration when
 * registration.autologin is on.</li>
 * </ul>
 *
 * <p>The precondition is the ticket's own: write access to the other account's node, which is a
 * Security relationship plus a PUT grant on User - an ordinary delegation ("user administrators may
 * maintain accounts"), and one that is not supposed to mean "may become any of them".
 */
public class CredentialPropertyWriteTest extends StructrUiTest {

	private static final String PASSWORD     = "correct-horse-battery-staple";

	/** Unsigned legacy shape, with a timestamp far enough out that the validity window cannot save us. */
	private static final String FORGED_TOKEN = "anything!99999999999999";

	@Test
	public void testSessionIdsCannotBeWrittenByANonAdmin() {

		assertNotWritable(PrincipalTraitDefinition.SESSION_IDS_PROPERTY, "[ \"stolen-session-id\" ]", "stolen-session-id");
	}

	@Test
	public void testTwoFactorTokenCannotBeWrittenByANonAdmin() {

		assertNotWritable(PrincipalTraitDefinition.TWO_FACTOR_TOKEN_PROPERTY, "\"" + FORGED_TOKEN + "\"", FORGED_TOKEN);
	}

	/**
	 * What the written value is worth. A two-factor token is not an opaque field the login flow happens
	 * to read - it identifies the account (AuthHelper.getUserForTwoFactorToken) and is accepted in the
	 * unsigned legacy shape "anything!&lt;timestamp&gt;" (isTwoFactorTokenValid), so a value an attacker
	 * chose is a second factor they hold for someone else's account.
	 */
	@Test
	public void testAWrittenTwoFactorTokenDoesNotIdentifyTheVictim() {

		final String victimId = createUser("victim", true);

		createUser("attacker", false);
		letAttackerWriteVictim(victimId);

		grant(StructrTraits.USER, UiAuthenticator.AUTH_USER_GET | UiAuthenticator.AUTH_USER_PUT, true);
		grant(StructrTraits.USER + "/_id", UiAuthenticator.AUTH_USER_GET | UiAuthenticator.AUTH_USER_PUT, false);

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.headers(X_USER_HEADER, "attacker", X_PASSWORD_HEADER, PASSWORD)
				.body("{ \"" + PrincipalTraitDefinition.TWO_FACTOR_TOKEN_PROPERTY + "\": \"" + FORGED_TOKEN + "\" }")
			.when()
				.put("/User/" + victimId);

		try (final Tx tx = app.tx()) {

			final Principal identified = AuthHelper.getUserForTwoFactorToken(FORGED_TOKEN);

			assertNull("a token the attacker chose identifies the victim, and the unsigned legacy format is still accepted",
				identified);

			tx.success();

		} catch (TwoFactorAuthenticationTokenInvalidException expected) {

			// the token was rejected, which is the outcome this test asks for

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testRefreshTokensCannotBeWrittenByANonAdmin() {

		assertNotWritable(PrincipalTraitDefinition.REFRESH_TOKENS_PROPERTY, "[ \"stolen-refresh-token\" ]", "stolen-refresh-token");
	}

	@Test
	public void testConfirmationKeyCannotBeWrittenByANonAdmin() {

		assertNotWritable("confirmationKey", "\"chosen-confirmation-key\"", "chosen-confirmation-key");
	}

	// ----- private methods -----
	/**
	 * Lets the attacker PUT the given value onto the victim's account and checks that it did not land.
	 * The status code is deliberately not asserted: whether the write is refused with 422 or silently
	 * dropped is a design decision, that it must not take effect is not.
	 */
	private void assertNotWritable(final String propertyName, final String jsonValue, final String needle) {

		final String victimId = createUser("victim", true);

		createUser("attacker", false);
		letAttackerWriteVictim(victimId);

		/* "User/_id" is the signature of the single-entity resource; a grant on "User" alone covers the
		   collection and nothing else. */
		grant(StructrTraits.USER, UiAuthenticator.AUTH_USER_GET | UiAuthenticator.AUTH_USER_PUT, true);
		grant(StructrTraits.USER + "/_id", UiAuthenticator.AUTH_USER_GET | UiAuthenticator.AUTH_USER_PUT, false);

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.headers(X_USER_HEADER, "attacker", X_PASSWORD_HEADER, PASSWORD)
				.body("{ \"" + propertyName + "\": " + jsonValue + " }")
			.when()
				.put("/User/" + victimId);

		try (final Tx tx = app.tx()) {

			final NodeInterface victim = app.getNodeById(StructrTraits.USER, victimId);
			final Object written       = victim.getProperty(Traits.of(StructrTraits.USER).key(propertyName));
			final String asText        = String.valueOf(written instanceof Object[] array ? java.util.Arrays.toString(array) : written);

			assertFalse(propertyName + " was written onto another user's account by a non-admin, value is now: " + asText,
				asText.contains(needle));

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	private void letAttackerWriteVictim(final String victimId) {

		try (final Tx tx = app.tx()) {

			final NodeInterface victim   = app.getNodeById(StructrTraits.USER, victimId);
			final Principal attacker     = app.nodeQuery(StructrTraits.USER).name("attacker").getFirst().as(Principal.class);

			victim.as(AccessControllable.class).grant(Permission.write, attacker);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception granting write access: " + fex.getMessage());
		}
	}

	private String createUser(final String name, final boolean visibleToAuthenticated) {

		final Traits traits = Traits.of(StructrTraits.USER);

		try (final Tx tx = app.tx()) {

			final NodeInterface user = app.create(StructrTraits.USER,
				new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name),
				new NodeAttribute<>(traits.key(PrincipalTraitDefinition.PASSWORD_PROPERTY), PASSWORD),
				new NodeAttribute<>(traits.key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), visibleToAuthenticated)
			);

			final String id = user.getUuid();

			tx.success();

			return id;

		} catch (FrameworkException fex) {

			fail("Unexpected exception creating the user: " + fex.getMessage());

			return null;
		}
	}
}
