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

import org.structr.common.error.FrameworkException;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.rest.auth.AuthHelper;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

import java.util.Set;

import static org.testng.AssertJUnit.*;

/**
 * Principal names are not unique, so the password decides which of several principals sharing a name logs in.
 */
public class DuplicateNameLoginTest extends StructrUiTest {

	private static final String FIRST  = "first-twin-password-1";
	private static final String SECOND = "second-twin-password-2";
	private static final String WRONG  = "neither-twin-password-3";

	@Test
	public void testThePasswordPicksTheUser() {

		final String first  = createEntityAsSuperUser("/User", "{ 'name': 'twin', 'password': '" + FIRST + "' }");
		final String second = createEntityAsSuperUser("/User", "{ 'name': 'twin', 'password': '" + SECOND + "' }");

		createEntityAsSuperUser("/Group", "{ 'name': 'twin' }");

		assertEquals("the first password must log in the first user", first, login("twin", FIRST));
		assertEquals("the second password must log in the second user", second, login("twin", SECOND));
	}

	@Test
	public void testAGroupOfTheSameNameDoesNotHideTheUser() {

		createEntityAsSuperUser("/Group", "{ 'name': 'solo' }");

		final String user = createEntityAsSuperUser("/User", "{ 'name': 'solo', 'password': '" + FIRST + "' }");

		for (int i = 0; i < 5; i++) {

			assertEquals("the user must log in whatever order the lookup returns", user, login("solo", FIRST));
		}
	}

	@Test
	public void testAWrongPasswordCountsOnEveryUserOfThatName() {

		final String first  = createEntityAsSuperUser("/User", "{ 'name': 'pair', 'password': '" + FIRST + "' }");
		final String second = createEntityAsSuperUser("/User", "{ 'name': 'pair', 'password': '" + SECOND + "' }");

		assertNull("a password neither user has must be refused", login("pair", WRONG));

		assertEquals(Integer.valueOf(1), getPasswordAttempts(first));
		assertEquals(Integer.valueOf(1), getPasswordAttempts(second));

		assertEquals("a successful login must still work afterwards", first, login("pair", FIRST));
		assertEquals("and reset only the counter of the user who logged in", Integer.valueOf(0), getPasswordAttempts(first));
		assertEquals(Integer.valueOf(1), getPasswordAttempts(second));
	}

	@Test
	public void testTheSamePasswordOnBothIsRefused() {

		createEntityAsSuperUser("/User", "{ 'name': 'clone', 'password': '" + FIRST + "' }");
		createEntityAsSuperUser("/User", "{ 'name': 'clone', 'password': '" + FIRST + "' }");

		assertNull("two users of the same name with the same password cannot be told apart", login("clone", FIRST));
	}

	@Test
	public void testANameMatchHidesAnEMailMatch() {

		final String byName = createEntityAsSuperUser("/User", "{ 'name': 'mail@example.com', 'password': '" + FIRST + "' }");

		createEntityAsSuperUser("/User", "{ 'name': 'someone', 'eMail': 'mail@example.com', 'password': '" + SECOND + "' }");

		// the first key with hits wins, as before: one query per login in the common case
		assertEquals(byName, login("mail@example.com", FIRST));
		assertNull(login("mail@example.com", SECOND));
	}

	@Test
	public void testABlockedUserWithTheRightPasswordCountsNothingOnTheOthers() {

		createEntityAsSuperUser("/User", "{ 'name': 'duo', 'password': '" + FIRST + "', 'blocked': true }");

		final String active = createEntityAsSuperUser("/User", "{ 'name': 'duo', 'password': '" + SECOND + "' }");

		assertNull("a blocked user must not log in", login("duo", FIRST));
		assertEquals("the blocked user's attempt must not count against the other user", Integer.valueOf(0), getPasswordAttempts(active));
		assertEquals("the other user must still log in", active, login("duo", SECOND));
	}

	@Test
	@SuppressWarnings("deprecation")
	public void testTheDeprecatedPrincipalLookupIgnoresGroupsWhenCounting() {

		final PropertyKey<String> nameKey = Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
		final String group                = createEntityAsSuperUser("/Group", "{ 'name': 'crew' }");
		final String user                 = createEntityAsSuperUser("/User", "{ 'name': 'crew', 'password': '" + FIRST + "' }");

		try (final Tx tx = app.tx()) {

			assertEquals("the password must pick the user beside a group of the same name", user, AuthHelper.getPrincipalForPassword(nameKey, "crew", FIRST).getUuid());

			tx.success();

		} catch (Exception ex) {

			fail("Unexpected exception: " + ex.getMessage());
		}

		try (final Tx tx = app.tx()) {

			AuthHelper.getPrincipalForPassword(nameKey, "crew", WRONG);

			fail("A wrong password must be refused");

		} catch (AuthenticationException expected) {

		} catch (Exception ex) {

			fail("Unexpected exception: " + ex.getMessage());
		}

		assertEquals("the user counts the failed attempt", Integer.valueOf(1), getPasswordAttempts(user));
		assertEquals("the group does not", Integer.valueOf(0), getPasswordAttempts(group));
	}

	// ----- private methods -----
	private String login(final String value, final String password) {

		final PropertyKey<String> nameKey  = Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
		final PropertyKey<String> eMailKey = Traits.of(StructrTraits.PRINCIPAL).key(PrincipalTraitDefinition.EMAIL_PROPERTY);

		try (final Tx tx = app.tx()) {

			final Principal principal = AuthHelper.getUserForPassword(Set.of(nameKey, eMailKey), value, password);

			tx.success();

			return principal != null ? principal.getUuid() : null;

		} catch (AuthenticationException expected) {

			return null;

		} catch (Exception ex) {

			fail("Unexpected exception: " + ex.getMessage());

			return null;
		}
	}

	private Integer getPasswordAttempts(final String id) {

		try (final Tx tx = app.tx()) {

			final Integer attempts = app.getNodeById(id).getProperty(Traits.of(StructrTraits.PRINCIPAL).key(PrincipalTraitDefinition.PASSWORD_ATTEMPTS_PROPERTY));

			tx.success();

			return attempts != null ? attempts : 0;

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());

			return null;
		}
	}
}
