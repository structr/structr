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
package org.structr.test.core.service;

import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.auth.DefaultCredentialsCheck;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Whether an admin account left on the built-in default password is reported.
 *
 * The check verifies against the stored hash rather than comparing plaintext, so these tests create real
 * users and let the hashing happen: a test that asserted on the plaintext would pass against an
 * implementation that never hashes anything.
 */
public class DefaultCredentialsCheckTest extends StructrTest {

	@Test
	public void testAnAdminOnTheDefaultPasswordIsReported() {

		createUser("weak-admin", Settings.InitialAdminUserPassword.getDefaultValue(), true);

		assertTrue("an admin with the default password must be reported", check());
	}

	@Test
	public void testAnAdminWithAnOwnPasswordIsNotReported() {

		createUser("strong-admin", "a-password-nobody-can-look-up", true);

		assertFalse("an admin with a chosen password must not be reported", check());
	}

	@Test
	public void testANonAdminOnTheDefaultPasswordIsNotReported() {

		// the risk being reported is administrative access, and an ordinary account with a weak password
		// is a different problem with a different remedy
		createUser("weak-user", Settings.InitialAdminUserPassword.getDefaultValue(), false);

		assertFalse("a non-admin must not be reported", check());
	}

	@Test
	public void testAnInstanceWithNoUsersIsNotReported() {

		assertFalse("an instance with no admin at all has nothing to report", check());
	}

	@Test
	public void testTheWarningClearsWhenThePasswordIsChanged() {

		createUser("weak-admin", Settings.InitialAdminUserPassword.getDefaultValue(), true);

		assertTrue("precondition: the admin must be reported first", check());

		// the recheck is deferred past the commit, so the change has to actually commit
		setPassword("weak-admin", "a-password-nobody-can-look-up");

		assertFalse("changing the password must clear the warning without a restart",
			DefaultCredentialsCheck.isDefaultCredentialsInUse());
	}

	@Test
	public void testTheWarningSurvivesWhileAnotherAdminStillHasTheDefault() {

		createUser("weak-admin",  Settings.InitialAdminUserPassword.getDefaultValue(), true);
		createUser("other-admin", Settings.InitialAdminUserPassword.getDefaultValue(), true);

		assertTrue("precondition", check());

		setPassword("weak-admin", "a-password-nobody-can-look-up");

		// the flag is about the instance, not about the account that changed: clearing it here would hide
		// an administrator who is still reachable with the documented password
		assertTrue("the warning must stand while another admin is still on the default",
			DefaultCredentialsCheck.isDefaultCredentialsInUse());
	}

	@Test
	public void testTheWarningClearsWhenThePasswordIsChangedThroughAPropertyMap() {

		createUser("weak-admin", Settings.InitialAdminUserPassword.getDefaultValue(), true);

		assertTrue("precondition: the admin must be reported first", check());

		// The path the admin UI actually takes. A map-based write goes straight to the PropertyKey through
		// setPropertiesInternal and never reaches the trait's SetProperty operation, so a hook placed there
		// passes this test's single-key sibling and still leaves the dashboard warning standing.
		try (final Tx tx = app.tx()) {

			final Traits traits          = Traits.of(StructrTraits.USER);
			final NodeInterface user     = app.nodeQuery(StructrTraits.USER).name("weak-admin").getFirst();
			final PropertyMap properties = new PropertyMap();

			assertNotNull("user not found", user);

			properties.put(traits.key(PrincipalTraitDefinition.PASSWORD_PROPERTY), "a-password-nobody-can-look-up");

			user.setProperties(user.getSecurityContext(), properties);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unable to change the password: " + fex.getMessage());
		}

		assertFalse("a password changed through a property map must clear the warning too",
			DefaultCredentialsCheck.isDefaultCredentialsInUse());
	}

	// ----- private methods -----
	private void setPassword(final String name, final String password) {

		try (final Tx tx = app.tx()) {

			final Traits traits      = Traits.of(StructrTraits.USER);
			final NodeInterface user = app.nodeQuery(StructrTraits.USER).name(name).getFirst();

			assertNotNull("user " + name + " not found", user);

			user.setProperty(traits.key(PrincipalTraitDefinition.PASSWORD_PROPERTY), password);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unable to change the password of " + name + ": " + fex.getMessage());
		}
	}

	private boolean check() {

		final DefaultCredentialsCheck callback = new DefaultCredentialsCheck();

		callback.initializationDone();

		return DefaultCredentialsCheck.isDefaultCredentialsInUse();
	}

	private void createUser(final String name, final String password, final boolean isAdmin) {

		try (final Tx tx = app.tx()) {

			final Traits traits = Traits.of(StructrTraits.USER);

			app.create(StructrTraits.USER,
				new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),     name),
				new NodeAttribute<>(traits.key(PrincipalTraitDefinition.PASSWORD_PROPERTY),     password),
				new NodeAttribute<>(traits.key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY),     isAdmin)
			);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unable to create " + name + ": " + fex.getMessage());
		}
	}
}
