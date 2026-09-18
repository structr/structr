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
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.structr.web.auth.UiAuthenticator;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

/**
 * Ticket 1585: DeleteNodeCommand never asked whether the caller may delete. genericDelete() hands it
 * everything doGet() returned, so a DELETE grant on a collection resource deleted every node the
 * caller could READ - being allowed to see something was enough to destroy it.
 */
public class DeletePermissionTest extends StructrUiTest {

	private static final String PASSWORD = "correct-horse-battery-staple";

	/**
	 * "DELETE /Project/<id>/tasks löscht jede lesbare Task, auch nur-lesbare" from the ticket, with a
	 * built-in type: the folder is visible to authenticated users and owned by nobody the attacker is,
	 * so the attacker may read it and nothing more.
	 */
	@Test
	public void testCollectionDeleteDoesNotRemoveNodesTheCallerMayOnlyRead() {

		final String folderId = createReadableFolder("victim-folder");

		createUser("attacker");

		grant(StructrTraits.FOLDER, UiAuthenticator.AUTH_USER_GET | UiAuthenticator.AUTH_USER_DELETE, true);

		// the attacker can see it - that is the whole of their access to it
		RestAssured
			.given()
				.headers(X_USER_HEADER, "attacker", X_PASSWORD_HEADER, PASSWORD)
			.expect()
				.statusCode(200)
			.when()
				.get("/Folder");

		RestAssured
			.given()
				.headers(X_USER_HEADER, "attacker", X_PASSWORD_HEADER, PASSWORD)
			.when()
				.delete("/Folder");

		try (final Tx tx = app.tx()) {

			assertNotNull("A node the caller may only read must survive a DELETE on the collection", app.getNodeById(StructrTraits.FOLDER, folderId));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	/**
	 * "DELETE /<relUuid> entfernt fremde Security-Grants oder Gruppenmitgliedschaften" from the ticket.
	 * UuidResource deletes a relationship without asking anything at all - only its node branch checks -
	 * and DeleteRelationshipCommand does not check either, so a DELETE grant on the uuid resource plus a
	 * relationship id is enough to cut someone out of a group or strip a permission.
	 */
	@Test
	public void testRelationshipDeleteRequiresWriteOnBothEndNodes() {

		final String membershipId = createGroupWithMember("victims", "member");

		createUser("attacker");

		grant("_id", UiAuthenticator.AUTH_USER_DELETE, true);

		RestAssured
			.given()
				.headers(X_USER_HEADER, "attacker", X_PASSWORD_HEADER, PASSWORD)
			.when()
				.delete("/" + membershipId);

		try (final Tx tx = app.tx()) {

			assertNotNull("A group membership must survive a DELETE by someone who may not write either end node", findRelationshipById(membershipId));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	/**
	 * "InstanceRelationshipsResource liefert alle Security/OWNS/CONTAINS-Relationships eines lesbaren
	 * Nodes" from the ticket: relationships were never filtered, so being allowed to read one node told
	 * you about every node it is connected to - group memberships and the whole permission matrix.
	 */
	@Test
	public void testRelationshipListingHidesRelationshipsToUnreadableNodes() {

		final String memberId = createGroupWithVisibleMember("secret-group", "visible-member");

		createUser("snooper");

		grant(StructrTraits.USER + "/_id/in", UiAuthenticator.AUTH_USER_GET, true);

		RestAssured
			.given()
				.headers(X_USER_HEADER, "snooper", X_PASSWORD_HEADER, PASSWORD)
			.expect()
				.statusCode(200)
				.body("result", org.hamcrest.Matchers.hasSize(0))
			.when()
				.get("/User/" + memberId + "/in");
	}

	// ----- private methods -----
	private RelationshipInterface findRelationshipById(final String id) throws FrameworkException {

		return app.relationshipQuery().getAsList().stream()
			.filter(r -> id.equals(r.getUuid()))
			.findFirst()
			.orElse(null);
	}

	private String createGroupWithVisibleMember(final String groupName, final String memberName) {

		try (final Tx tx = app.tx()) {

			final NodeInterface group = app.create(StructrTraits.GROUP, new NodeAttribute<>(Traits.of(StructrTraits.GROUP).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), groupName));

			// the member is readable, the group is not - so the membership must not show up
			final NodeInterface member = app.create(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), memberName),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.PASSWORD_PROPERTY), PASSWORD),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true)
			);

			group.as(Group.class).addMember(securityContext, member.as(Principal.class));

			final String id = member.getUuid();

			tx.success();

			return id;

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception creating the group: " + fex.getMessage());

			return null;
		}
	}

	private String createGroupWithMember(final String groupName, final String memberName) {

		try (final Tx tx = app.tx()) {

			final NodeInterface group = app.create(StructrTraits.GROUP, new NodeAttribute<>(Traits.of(StructrTraits.GROUP).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), groupName));
			final NodeInterface member = app.create(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), memberName),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.PASSWORD_PROPERTY), PASSWORD)
			);

			group.as(Group.class).addMember(securityContext, member.as(Principal.class));

			final RelationshipInterface membership = app.relationshipQuery().getAsList().stream()
				.filter(r -> r.getSourceNode() != null && groupName.equals(r.getSourceNode().getName()))
				.findFirst()
				.orElse(null);

			assertNotNull("The membership relationship must exist", membership);

			final String id = membership.getUuid();

			tx.success();

			return id;

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception creating the group: " + fex.getMessage());

			return null;
		}
	}

	private String createReadableFolder(final String name) {

		final Traits traits = Traits.of(StructrTraits.FOLDER);

		try (final Tx tx = app.tx()) {

			final NodeInterface folder = app.create(StructrTraits.FOLDER,
				new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name),
				new NodeAttribute<>(traits.key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true)
			);

			final String id = folder.getUuid();

			tx.success();

			return id;

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception creating the folder: " + fex.getMessage());

			return null;
		}
	}

	private void createUser(final String name) {

		final Traits traits = Traits.of(StructrTraits.USER);

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.USER,
				new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name),
				new NodeAttribute<>(traits.key(PrincipalTraitDefinition.PASSWORD_PROPERTY), PASSWORD)
			);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception creating the user: " + fex.getMessage());
		}
	}
}
