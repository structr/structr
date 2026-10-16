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
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * The Argon2id parameters are settings, and every hash carries the parameters it was made with. A
 * changed setting reaches an existing account by re-hashing its password on the next successful
 * login, which is the only moment the cleartext password is available. A legacy SHA-512 hash is
 * replaced the same way.
 *
 * The re-hash writes the principal node during the login, which then resets the failed-attempt
 * counter of the same node in a transaction of its own. A re-hash that held that node in the
 * login's transaction made the login wait forever, so a hanging test here means exactly that.
 */
public class PasswordRehashTest extends StructrUiTest {

	private static final String CORRECT_PASSWORD = "correct-horse-battery-staple";
	private static final String WRONG_PASSWORD   = "wrong-horse-battery-staple";
	private static final String LEGACY_SALT      = "0123456789abcdef";

	@Test
	public void testChangedHashSettingsApplyOnNextLogin() {

		final Traits traits = Traits.of(StructrTraits.USER);

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.USER,
				new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "rehashuser"),
				new NodeAttribute<>(traits.key(PrincipalTraitDefinition.PASSWORD_PROPERTY),  CORRECT_PASSWORD)
			);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception while creating the test account: " + fex.getMessage());
		}

		final String originalHash = storedHash("rehashuser");
		final int originalMemory  = Settings.PasswordHashArgon2idMemory.getValue();

		assertFalse("a fresh hash must match the current settings", HashHelper.needsRehash(originalHash));

		try {

			// any value other than the default; this is the memory of OWASP's two-pass minimum configuration
			Settings.PasswordHashArgon2idMemory.setValue(19456);

			assertTrue("a hash made with other parameters must be marked for re-hashing", HashHelper.needsRehash(originalHash));

			// a failed login has no cleartext password worth keeping and must leave the hash alone
			login("rehashuser", WRONG_PASSWORD, false);
			assertEquals("a failed login must not re-hash", originalHash, storedHash("rehashuser"));

			login("rehashuser", CORRECT_PASSWORD, true);

			final String newHash = storedHash("rehashuser");

			assertFalse("a successful login must re-hash with the current settings", originalHash.equals(newHash));
			assertTrue("the new hash must carry the changed memory parameter", newHash.contains("m=19456,"));
			assertFalse("the new hash must match the current settings", HashHelper.needsRehash(newHash));

			// and the account still signs in with the same password afterwards
			login("rehashuser", CORRECT_PASSWORD, true);

		} finally {

			Settings.PasswordHashArgon2idMemory.setValue(originalMemory);
		}
	}

	@Test
	public void testLegacyHashIsReplacedOnNextLogin() {

		final Traits traits = Traits.of(StructrTraits.USER);

		try (final Tx tx = app.tx()) {

			/* Planted on the database node directly, because setting the password property is
			   precisely what replaces a legacy hash with an Argon2id one. */
			final NodeInterface legacyUser = app.create(StructrTraits.USER, new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "legacyuser"));
			final Node legacyNode          = legacyUser.getNode();

			legacyNode.setProperty(PrincipalTraitDefinition.SALT_PROPERTY,     LEGACY_SALT);
			legacyNode.setProperty(PrincipalTraitDefinition.PASSWORD_PROPERTY, HashHelper.getHash(CORRECT_PASSWORD, LEGACY_SALT));

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception while creating the test account: " + fex.getMessage());
		}

		assertFalse("legacyuser must start on a legacy hash", HashHelper.isArgon2Hash(storedHash("legacyuser")));

		login("legacyuser", CORRECT_PASSWORD, true);

		final String newHash = storedHash("legacyuser");

		assertTrue("a successful login must replace a legacy hash with an Argon2id one", HashHelper.isArgon2Hash(newHash));
		assertFalse("the new hash must match the current settings", HashHelper.needsRehash(newHash));

		// and the account still signs in with the same password afterwards
		login("legacyuser", CORRECT_PASSWORD, true);
	}

	private void login(final String userName, final String password, final boolean expectSuccess) {

		try (final Tx tx = app.tx()) {

			final NodeInterface principal = AuthHelper.getPrincipalForPassword(Traits.of(StructrTraits.PRINCIPAL).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), userName, password);

			assertNotNull(principal);

			if (!expectSuccess) {

				fail("Signing in with a wrong password must fail");
			}

			tx.success();

		} catch (AuthenticationException aex) {

			if (expectSuccess) {

				fail("Signing in with the correct password must succeed");
			}

		} catch (FrameworkException fex) {

			fail("Unexpected exception while signing in: " + fex.getMessage());
		}
	}

	private String storedHash(final String userName) {

		try (final Tx tx = app.tx()) {

			final NodeInterface user = app.nodeQuery(StructrTraits.USER)
				.key(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), userName)
				.getFirst();

			assertNotNull("test account " + userName + " was not created", user);

			final String hash = (String) user.getNode().getProperty(PrincipalTraitDefinition.PASSWORD_PROPERTY);

			tx.success();

			return hash;

		} catch (FrameworkException fex) {

			fail("Unexpected exception while reading the stored hash: " + fex.getMessage());

			return null;
		}
	}
}
