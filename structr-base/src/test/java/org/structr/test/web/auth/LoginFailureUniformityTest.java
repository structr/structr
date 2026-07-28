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

import org.structr.api.config.Settings;
import org.structr.api.graph.Node;
import org.structr.common.error.FrameworkException;
import org.structr.core.auth.HashHelper;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.rest.auth.AuthHelper;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * A failed sign-in must be indistinguishable from every other failed sign-in, so that a caller
 * cannot learn whether an account exists. Two things are asserted for each way of failing:
 *
 * <ul>
 * <li>The ANSWER is always the same generic {@link AuthenticationException}. A locked out account in
 *     particular must not answer {@code TooManyFailedLoginAttemptsException} to a caller who did not
 *     supply the right password: only an account that exists can be locked out, so that answer, and
 *     the "reason" header the login endpoints build from it, confirm the account is real.</li>
 * <li>The WORK is always exactly one Argon2 verification. Any branch that answers without checking a
 *     password has to spend that work itself, see {@link HashHelper#spendVerificationTime(String)},
 *     because Argon2id is deliberately expensive and its absence is measurable.</li>
 * </ul>
 *
 * The work is asserted by COUNTING verifications rather than by measuring elapsed time: the count is
 * deterministic, while a timing assertion would be flaky, and a flaky security test ends up ignored.
 */
public class LoginFailureUniformityTest extends StructrUiTest {

	private static final String LEGACY_SALT      = "0123456789abcdef";
	private static final String CORRECT_PASSWORD = "correct-horse-battery-staple";
	private static final String WRONG_PASSWORD   = "wrong-horse-battery-staple";

	@Test
	public void testFailedLoginsAreIndistinguishable() {

		final Traits traits = Traits.of(StructrTraits.USER);

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.USER,
				new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "argon2user"),
				new NodeAttribute<>(traits.key(PrincipalTraitDefinition.PASSWORD_PROPERTY),  CORRECT_PASSWORD)
			);

			app.create(StructrTraits.USER,
				new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "blockeduser"),
				new NodeAttribute<>(traits.key(PrincipalTraitDefinition.PASSWORD_PROPERTY),  CORRECT_PASSWORD),
				new NodeAttribute<>(traits.key(PrincipalTraitDefinition.BLOCKED_PROPERTY),   true)
			);

			app.create(StructrTraits.USER,
				new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),        "lockedoutuser"),
				new NodeAttribute<>(traits.key(PrincipalTraitDefinition.PASSWORD_PROPERTY),         CORRECT_PASSWORD),
				new NodeAttribute<>(traits.key(PrincipalTraitDefinition.PASSWORD_ATTEMPTS_PROPERTY), Settings.PasswordAttempts.getValue() + 10)
			);

			// an external account, from LDAP or single sign-on, has no stored hash at all
			app.create(StructrTraits.USER,
				new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "nopassworduser")
			);

			/* An account left on a pre-Argon2id hash. It has to be planted on the database node
			   directly, because setting the password property is precisely what replaces a legacy
			   hash with an Argon2id one. */
			final NodeInterface legacyUser = app.create(StructrTraits.USER,
				new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "legacyuser")
			);

			final Node legacyNode = legacyUser.getNode();
			legacyNode.setProperty(PrincipalTraitDefinition.SALT_PROPERTY,     LEGACY_SALT);
			legacyNode.setProperty(PrincipalTraitDefinition.PASSWORD_PROPERTY, HashHelper.getHash(CORRECT_PASSWORD, LEGACY_SALT));

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception while creating the test accounts: " + fex.getMessage());
		}

		// the fixture has to be right, or several cases below quietly collapse into the same one
		assertFixture();

		assertUniformFailure("a user name that does not exist", "nosuchuser");
		assertUniformFailure("a wrong password",                "argon2user");
		assertUniformFailure("a blocked account",               "blockeduser");
		assertUniformFailure("a locked out account",            "lockedoutuser");
		assertUniformFailure("an account without a password",   "nopassworduser");
		assertUniformFailure("an account on a legacy hash",     "legacyuser");
	}

	/**
	 * One failed sign-in with the given user name: the same generic answer, and exactly one password
	 * verification worth of work.
	 */
	private void assertUniformFailure(final String label, final String userName) {

		HashHelper.resetVerificationCount();

		try (final Tx tx = app.tx()) {

			AuthHelper.getPrincipalForPassword(Traits.of(StructrTraits.PRINCIPAL).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), userName, WRONG_PASSWORD);

			fail("Signing in with " + label + " must fail");

			tx.success();

		} catch (AuthenticationException expected) {

			// the single generic answer that every failed attempt has to give

		} catch (FrameworkException fex) {

			fail("Signing in with " + label + " answered " + fex.getClass().getSimpleName()
				+ " instead of the generic AuthenticationException, which tells the caller the account exists");
		}

		assertEquals("Signing in with " + label + " must cost exactly one password verification",
			1L, HashHelper.getVerificationCount());
	}

	private void assertFixture() {

		final Traits traits = Traits.of(StructrTraits.USER);

		try (final Tx tx = app.tx()) {

			final Boolean blocked = user("blockeduser").getProperty(traits.key(PrincipalTraitDefinition.BLOCKED_PROPERTY));
			assertEquals("blockeduser must be blocked", Boolean.TRUE, blocked);

			final Integer attempts = user("lockedoutuser").getProperty(traits.key(PrincipalTraitDefinition.PASSWORD_ATTEMPTS_PROPERTY));
			assertNotNull("lockedoutuser must have a failed attempt count", attempts);
			assertTrue("lockedoutuser must be over the configured threshold", attempts > Settings.PasswordAttempts.getValue());

			assertNull("nopassworduser must have no stored hash",
				user("nopassworduser").getNode().getProperty(PrincipalTraitDefinition.PASSWORD_PROPERTY));

			final String legacyHash = (String) user("legacyuser").getNode().getProperty(PrincipalTraitDefinition.PASSWORD_PROPERTY);
			assertNotNull("legacyuser must have a stored hash", legacyHash);
			assertFalse("legacyuser must still be on a legacy hash", HashHelper.isArgon2Hash(legacyHash));

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception while checking the test accounts: " + fex.getMessage());
		}
	}

	private NodeInterface user(final String name) throws FrameworkException {

		final NodeInterface user = app.nodeQuery(StructrTraits.USER)
			.key(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name)
			.getFirst();

		assertNotNull("test account " + name + " was not created", user);

		return user;
	}
}
