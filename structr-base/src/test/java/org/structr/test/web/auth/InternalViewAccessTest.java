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
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.structr.web.auth.UiAuthenticator;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.fail;

/**
 * Ticket 1584: a resource access permission said what a caller may do with /User, and nothing looked
 * at which view the answer was rendered in - so the same permission also served /User/ui, which hands
 * out sessionIds, refreshTokens, twoFactorToken and confirmationKey of every user the caller can read.
 * Copying one sessionIds entry into a JSESSIONID cookie is that user's session.
 *
 * <p>The views Structr uses internally are now administrators-only, whatever the permissions say.
 */
public class InternalViewAccessTest extends StructrUiTest {

	private static final String PASSWORD = "correct-horse-battery-staple";

	/**
	 * The attack from the ticket, as far as it can be carried out: a normal user with a GET grant on
	 * User reads another user's record through the internal view.
	 */
	@Test
	public void testInternalViewsAreRefusedForNonAdmins() {

		createUser("victim", true);
		createUser("attacker", false);

		/* The grants the attack needs, and the reason the ticket asks for a warning next to them: a view
		   other than the default becomes its own resource signature, "User/_Ui", so an administrator has
		   to have created that permission deliberately. People do - it is how an application gets at the
		   richer view - and it reads like a harmless extra rather than "hand out every session id". */
		grant(StructrTraits.USER, UiAuthenticator.AUTH_USER_GET, true);
		grant(StructrTraits.USER + "/_Ui", UiAuthenticator.AUTH_USER_GET, false);
		grant(StructrTraits.USER + "/_All", UiAuthenticator.AUTH_USER_GET, false);

		// the permission itself is intact: the default view still answers
		RestAssured
			.given()
				.headers(X_USER_HEADER, "attacker", X_PASSWORD_HEADER, PASSWORD)
			.expect()
				.statusCode(200)
			.when()
				.get("/User");

		// ... but the granted internal views do not, however explicit the grant was
		for (final String view : new String[] { "ui", "all" }) {

			RestAssured
				.given()
					.headers(X_USER_HEADER, "attacker", X_PASSWORD_HEADER, PASSWORD)
				.expect()
					.statusCode(401)
				.when()
					.get("/User/" + view);
		}
	}

	/**
	 * The counterpart, because a refusal that also hits the back end is not a fix: the back end runs as
	 * an administrator and needs both views.
	 */
	@Test
	public void testInternalViewsStillWorkForAdmins() {

		createUser("someone", true);
		createAdminUser();

		grant(StructrTraits.USER, UiAuthenticator.AUTH_USER_GET, true);
		grant(StructrTraits.USER + "/_Ui", UiAuthenticator.AUTH_USER_GET, false);
		grant(StructrTraits.USER + "/_All", UiAuthenticator.AUTH_USER_GET, false);

		for (final String view : new String[] { "ui", "all" }) {

			RestAssured
				.given()
					.headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
				.expect()
					.statusCode(200)
				.when()
					.get("/User/" + view);
		}
	}

	/**
	 * An application's own views are none of this rule's business - only the two Structr generates or
	 * curates for itself are.
	 */
	@Test
	public void testApplicationViewsAreUnaffected() {

		createUser("bystander", true);
		createUser("reader", false);

		grant(StructrTraits.USER, UiAuthenticator.AUTH_USER_GET, true);

		RestAssured
			.given()
				.headers(X_USER_HEADER, "reader", X_PASSWORD_HEADER, PASSWORD)
			.expect()
				.statusCode(200)
			.when()
				.get("/User/public");
	}

	// ----- private methods -----
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

			fex.printStackTrace();
			fail("Unexpected exception creating the user: " + fex.getMessage());

			return null;
		}
	}
}
