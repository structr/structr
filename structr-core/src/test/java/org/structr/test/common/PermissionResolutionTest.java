/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.test.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.graph.PropagationDirection;
import org.structr.api.graph.PropagationMode;
import org.structr.common.AccessMode;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.ServicePrincipal;
import org.structr.core.entity.*;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.web.entity.User;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.AssertJUnit.*;

/**
 * Test access control with different permission levels.
 */
public class PermissionResolutionTest extends StructrTest {

	private static final Logger logger = LoggerFactory.getLogger(PermissionResolutionTest.class.getName());

	@Test
	public void test01SimplePermissionResolution() {

		SchemaRelationshipNode rel = null;
		PropertyKey key            = null;
		Principal user1            = null;
		Class type1                = null;
		Class type2                = null;

		try (final Tx tx = app.tx()) {

			// create a test user
			user1 = app.create(Principal.class, "user1");

			// create schema setup with permission propagation
			final SchemaNode t1 = app.create(SchemaNode.class, "Type1");
			final SchemaNode t2 = app.create(SchemaNode.class, "Type2");

			rel = app.create(SchemaRelationshipNode.class,
				new NodeAttribute<>(SchemaRelationshipNode.sourceNode, t1),
				new NodeAttribute<>(SchemaRelationshipNode.targetNode, t2),
				new NodeAttribute<>(SchemaRelationshipNode.relationshipType, "RELATED"),
				new NodeAttribute<>(SchemaRelationshipNode.sourceMultiplicity, "1"),
				new NodeAttribute<>(SchemaRelationshipNode.targetMultiplicity, "1"),
				new NodeAttribute<>(SchemaRelationshipNode.sourceJsonName, "source"),
				new NodeAttribute<>(SchemaRelationshipNode.targetJsonName, "target")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		assertNotNull("User should have been created", user1);

		// create and link objects, make object of type 1 visible,
		// expect object of type 2 to be visible as well
		try (final Tx tx = app.tx()) {

			type1 = StructrApp.getConfiguration().getNodeEntityClass("Type1");
			type2 = StructrApp.getConfiguration().getNodeEntityClass("Type2");
			key   = StructrApp.key(type1, "target");

			assertNotNull("Node type Type1 should exist.", type1);
			assertNotNull("Node type Type2 should exist.", type2);
			assertNotNull("Property key \"target\" should exist.", key);

			final NodeInterface instance1 = app.create(type1, "instance1OfType1");
			final NodeInterface instance2 = app.create(type2, "instance1OfType2");

			assertNotNull("Instance of type Type1 should exist", instance1);
			assertNotNull("Instance of type Type2 should exist", instance2);

			instance1.setProperty(key, instance2);

			// make instance1 visible to user1
			instance1.grant(Permission.read, user1);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// check access for user1 on instance1
		final App userApp = StructrApp.getInstance(SecurityContext.getInstance(user1, AccessMode.Backend));
		try (final Tx tx = userApp.tx()) {

			assertNotNull("User1 should be able to find instance of type Type1", userApp.nodeQuery(type1).getFirst());
			assertNull("User1 should NOT be able to find instance of type Type2", userApp.nodeQuery(type2).getFirst());

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// enable permission resolution for Type2->Type1 (should NOT make object visible
		// because the resolution direction is wrong.
		try (final Tx tx = app.tx()) {

			rel.setProperty(SchemaRelationshipNode.permissionPropagation, PropagationDirection.In);
			rel.setProperty(SchemaRelationshipNode.readPropagation, PropagationMode.Add);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// check access for user1 on instance1
		try (final Tx tx = userApp.tx()) {

			assertNotNull("User1 should be able to find instance of type Type1", userApp.nodeQuery(type1).getFirst());
			assertNull("User1 should NOT be able to find instance of type Type2", userApp.nodeQuery(type2).getFirst());

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// enable permission resolution for Type1->Type2 (should make object visible
		// because the resolution direction is correct
		try (final Tx tx = app.tx()) {

			rel.setProperty(SchemaRelationshipNode.permissionPropagation, PropagationDirection.Out);
			rel.setProperty(SchemaRelationshipNode.readPropagation, PropagationMode.Add);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// check access for user1 on instance1
		try (final Tx tx = userApp.tx()) {

			assertNotNull("User1 should be able to find instance of type Type1", userApp.nodeQuery(type1).getFirst());
			assertNotNull("User1 should be able to find instance of type Type2", userApp.nodeQuery(type2).getFirst());

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// enable permission resolution for both directions, should make object visible
		// because both resolution directions are enabled
		try (final Tx tx = app.tx()) {

			rel.setProperty(SchemaRelationshipNode.permissionPropagation, PropagationDirection.Both);
			rel.setProperty(SchemaRelationshipNode.readPropagation, PropagationMode.Add);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// check access for user1 on instance1
		try (final Tx tx = userApp.tx()) {

			assertNotNull("User1 should be able to find instance of type Type1", userApp.nodeQuery(type1).getFirst());
			assertNotNull("User1 should be able to find instance of type Type2", userApp.nodeQuery(type2).getFirst());

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// disable permission resolution for both directions, should make
		// object invisible again.
		try (final Tx tx = app.tx()) {

			rel.setProperty(SchemaRelationshipNode.permissionPropagation, PropagationDirection.None);
			rel.setProperty(SchemaRelationshipNode.readPropagation, PropagationMode.Add);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// check access for user1 on instance1
		try (final Tx tx = userApp.tx()) {

			assertNotNull("User1 should be able to find instance of type Type1", userApp.nodeQuery(type1).getFirst());
			assertNull("User1 should NOT be able to find instance of type Type2", userApp.nodeQuery(type2).getFirst());

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testPermissionResolutionWithSelfRelationship() {

		final App app = StructrApp.getInstance();
		String uuid   = null;

		try (final Tx tx = app.tx()) {

			// create schema setup with permission propagation
			final SchemaNode type = app.create(SchemaNode.class, "Project");

			uuid = app.create(SchemaRelationshipNode.class,
				new NodeAttribute<>(SchemaRelationshipNode.sourceNode, type),
				new NodeAttribute<>(SchemaRelationshipNode.targetNode, type),
				new NodeAttribute<>(SchemaRelationshipNode.relationshipType, "NEXT"),
				new NodeAttribute<>(SchemaRelationshipNode.sourceMultiplicity, "1"),
				new NodeAttribute<>(SchemaRelationshipNode.targetMultiplicity, "1"),
				new NodeAttribute<>(SchemaRelationshipNode.sourceJsonName, "prev"),
				new NodeAttribute<>(SchemaRelationshipNode.targetJsonName, "next")
			).getUuid();

			app.create(Principal.class, "tester");

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final Class projectType = StructrApp.getConfiguration().getNodeEntityClass("Project");
		final PropertyKey key   = StructrApp.getConfiguration().getPropertyKeyForJSONName(projectType, "prev");

		try (final Tx tx = app.tx()) {

			final Principal tester = app.nodeQuery(Principal.class).getFirst();

			final NodeInterface p1 = app.create(projectType,
				new NodeAttribute<>(AbstractNode.name, "Project1"),
				new NodeAttribute<>(AbstractNode.owner, tester)
			);

			final NodeInterface p2 = app.create(projectType,
				new NodeAttribute<>(AbstractNode.name, "Project2"),
				new NodeAttribute<>(key, p1)
			);

			final NodeInterface p3 = app.create(projectType,
				new NodeAttribute<>(AbstractNode.name, "Project3"),
				new NodeAttribute<>(key, p2)
			);

			final NodeInterface p4 = app.create(projectType,
				new NodeAttribute<>(AbstractNode.name, "Project4"),
				new NodeAttribute<>(key, p3)
			);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		try {
			System.out.println("######################################################################################################################## START");

			testGranted(projectType, new boolean[]{false, false, false, false});
			setPermissionResolution(uuid, SchemaRelationshipNode.readPropagation, PropagationMode.Add);
			testGranted(projectType, new boolean[]{true, false, false, false});
			setPermissionResolution(uuid, SchemaRelationshipNode.writePropagation, PropagationMode.Add);
			testGranted(projectType, new boolean[]{true, true, false, false});
			setPermissionResolution(uuid, SchemaRelationshipNode.deletePropagation, PropagationMode.Add);
			testGranted(projectType, new boolean[]{true, true, true, false});
			setPermissionResolution(uuid, SchemaRelationshipNode.accessControlPropagation, PropagationMode.Add);
			testGranted(projectType, new boolean[]{true, true, true, true});

			setPermissionResolution(uuid, SchemaRelationshipNode.readPropagation, PropagationMode.Remove);
			testGranted(projectType, new boolean[]{false, true, true, true});
			setPermissionResolution(uuid, SchemaRelationshipNode.writePropagation, PropagationMode.Remove);
			testGranted(projectType, new boolean[]{false, false, true, true});
			setPermissionResolution(uuid, SchemaRelationshipNode.deletePropagation, PropagationMode.Remove);
			testGranted(projectType, new boolean[]{false, false, false, true});
			setPermissionResolution(uuid, SchemaRelationshipNode.accessControlPropagation, PropagationMode.Remove);
			testGranted(projectType, new boolean[]{false, false, false, false});

		} finally {

			System.out.println("######################################################################################################################## END");
		}

	}

	@Test
	public void testPermissionResolutionWithSelfRelationshipAndInheritance() {

		final App app = StructrApp.getInstance();
		String uuid   = null;

		try (final Tx tx = app.tx()) {

			// create schema setup with permission propagation
			final SchemaNode type = app.create(SchemaNode.class, "Project");

			uuid = app.create(SchemaRelationshipNode.class,
				new NodeAttribute<>(SchemaRelationshipNode.sourceNode, type),
				new NodeAttribute<>(SchemaRelationshipNode.targetNode, type),
				new NodeAttribute<>(SchemaRelationshipNode.relationshipType, "NEXT"),
				new NodeAttribute<>(SchemaRelationshipNode.sourceMultiplicity, "1"),
				new NodeAttribute<>(SchemaRelationshipNode.targetMultiplicity, "1"),
				new NodeAttribute<>(SchemaRelationshipNode.sourceJsonName, "prev"),
				new NodeAttribute<>(SchemaRelationshipNode.targetJsonName, "next")
			).getUuid();

			final SchemaNode moo  = app.create(SchemaNode.class, "Moo");
			final SchemaNode test = app.create(SchemaNode.class, "Test");

			moo.setProperty(SchemaNode.extendsClass, type);
			test.setProperty(SchemaNode.extendsClass, type);

			app.create(Principal.class, "tester");

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final Class projectType = StructrApp.getConfiguration().getNodeEntityClass("Project");
		final Class mooType     = StructrApp.getConfiguration().getNodeEntityClass("Moo");
		final Class testType    = StructrApp.getConfiguration().getNodeEntityClass("Test");
		final PropertyKey key   = StructrApp.getConfiguration().getPropertyKeyForJSONName(mooType, "prev");

		try (final Tx tx = app.tx()) {

			final Principal tester = app.nodeQuery(Principal.class).getFirst();

			final NodeInterface p1 = app.create(mooType,
				new NodeAttribute<>(AbstractNode.name, "Project1"),
				new NodeAttribute<>(AbstractNode.owner, tester)
			);

			final NodeInterface p2 = app.create(testType,
				new NodeAttribute<>(AbstractNode.name, "Project2"),
				new NodeAttribute<>(key, p1)
			);

			final NodeInterface p3 = app.create(mooType,
				new NodeAttribute<>(AbstractNode.name, "Project3"),
				new NodeAttribute<>(key, p2)
			);

			final NodeInterface p4 = app.create(testType,
				new NodeAttribute<>(AbstractNode.name, "Project4"),
				new NodeAttribute<>(key, p3)
			);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		testGranted(projectType, new boolean[] { false, false, false, false });
		setPermissionResolution(uuid, SchemaRelationshipNode.readPropagation,          PropagationMode.Add);
		testGranted(projectType, new boolean[] { true, false, false, false });
		setPermissionResolution(uuid, SchemaRelationshipNode.writePropagation,         PropagationMode.Add);
		testGranted(projectType, new boolean[] { true, true, false, false });
		setPermissionResolution(uuid, SchemaRelationshipNode.deletePropagation,        PropagationMode.Add);
		testGranted(projectType, new boolean[] { true, true, true, false });
		setPermissionResolution(uuid, SchemaRelationshipNode.accessControlPropagation, PropagationMode.Add);
		testGranted(projectType, new boolean[] { true, true, true, true });

		setPermissionResolution(uuid, SchemaRelationshipNode.readPropagation,          PropagationMode.Remove);
		testGranted(projectType, new boolean[] { false, true, true, true });
		setPermissionResolution(uuid, SchemaRelationshipNode.writePropagation,         PropagationMode.Remove);
		testGranted(projectType, new boolean[] { false, false, true, true });
		setPermissionResolution(uuid, SchemaRelationshipNode.deletePropagation,        PropagationMode.Remove);
		testGranted(projectType, new boolean[] { false, false, false, true });
		setPermissionResolution(uuid, SchemaRelationshipNode.accessControlPropagation, PropagationMode.Remove);
		testGranted(projectType, new boolean[] { false, false, false, false });
	}

	@Test
	public void testSchemaGrants() {

		final App app    = StructrApp.getInstance();
		String uuid      = null;

		try (final Tx tx = app.tx()) {

			// create schema setup with permission propagation
			app.create(SchemaNode.class, "Project");
			app.create(Principal.class, "tester");

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		// setup 2 - schema grant
		try (final Tx tx = app.tx()) {

			final Group testGroup1 = app.create(Group.class, "Group1");
			final Group testGroup2 = app.create(Group.class, "Group2");
			final Group testGroup3 = app.create(Group.class, "Group3");
			final Principal tester = app.nodeQuery(Principal.class).andName("tester").getFirst();

			// create group hierarchy for test user
			testGroup1.addMember(securityContext, testGroup2);
			testGroup2.addMember(securityContext, testGroup3);
			testGroup3.addMember(securityContext, tester);

			// create grant
			final SchemaNode projectNode = app.nodeQuery(SchemaNode.class).andName("Project").getFirst();
			final SchemaGrant grant      = app.create(SchemaGrant.class,
				new NodeAttribute<>(SchemaGrant.schemaNode,          projectNode),
				new NodeAttribute<>(SchemaGrant.principal,           testGroup1),
				new NodeAttribute<>(SchemaGrant.allowRead,           false),
				new NodeAttribute<>(SchemaGrant.allowWrite,          false),
				new NodeAttribute<>(SchemaGrant.allowDelete,         false),
				new NodeAttribute<>(SchemaGrant.allowAccessControl,  false)
			);

			uuid = grant.getUuid();

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		final Class projectType = StructrApp.getConfiguration().getNodeEntityClass("Project");

		try (final Tx tx = app.tx()) {

			final Principal tester = app.nodeQuery(Principal.class).andName("tester").getFirst();

			app.create(projectType, new NodeAttribute<>(AbstractNode.name, "Project1"), new NodeAttribute<>(AbstractNode.owner, tester));
			app.create(projectType, new NodeAttribute<>(AbstractNode.name, "Project2"));
			app.create(projectType, new NodeAttribute<>(AbstractNode.name, "Project3"));
			app.create(projectType, new NodeAttribute<>(AbstractNode.name, "Project4"));

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		testGranted(projectType, new boolean[] { false, false, false, false });
		configureSchemaGrant(uuid, SchemaGrant.allowRead, true);
		testGranted(projectType, new boolean[] { true, false, false, false });
		configureSchemaGrant(uuid, SchemaGrant.allowWrite, true);
		testGranted(projectType, new boolean[] { true, true, false, false });
		configureSchemaGrant(uuid, SchemaGrant.allowDelete, true);
		testGranted(projectType, new boolean[] { true, true, true, false });
		configureSchemaGrant(uuid, SchemaGrant.allowAccessControl, true);
		testGranted(projectType, new boolean[] { true, true, true, true });

		configureSchemaGrant(uuid, SchemaGrant.allowRead, false);
		testGranted(projectType, new boolean[] { false, true, true, true });
		configureSchemaGrant(uuid, SchemaGrant.allowWrite, false);
		testGranted(projectType, new boolean[] { false, false, true, true });
		configureSchemaGrant(uuid, SchemaGrant.allowDelete, false);
		testGranted(projectType, new boolean[] { false, false, false, true });

		// allow all, but remove group link
		configureSchemaGrant(uuid, SchemaGrant.allowRead, true);
		configureSchemaGrant(uuid, SchemaGrant.allowWrite, true);
		configureSchemaGrant(uuid, SchemaGrant.allowDelete, true);
		configureSchemaGrant(uuid, SchemaGrant.allowAccessControl, true);

		try (final Tx tx = app.tx()) {

			// delete Group2 which links tester to granted Group1
			app.delete(app.nodeQuery(Group.class).andName("Group2").getFirst());
			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		testGranted(projectType, new boolean[] { false, false, false, false });
	}

	/* disabled for now..
	@Test
	public void testAdminGroupsWithDatabaseUser() {

		final SecurityContext ctx = SecurityContext.getSuperUserInstance();

		try (final Tx tx = app.tx()) {

			final Group group1 = app.create(Group.class, new NodeAttribute<>(Group.name, "Group1"));
			final Group group2 = app.create(Group.class, new NodeAttribute<>(Group.name, "Group2"));
			final Group group3 = app.create(Group.class, new NodeAttribute<>(Group.name, "Group3"));

			// Group1 is the admin group
			group1.setIsAdmin(true);

			// group hierarchy: Group1 -> Group2 -> Group3
			group1.addMember(ctx, group2);
			group2.addMember(ctx, group3);

			final Principal tester = app.create(Principal.class, "tester");

			group3.addMember(ctx, tester);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final Principal user = app.nodeQuery(Principal.class).andName("tester").getFirst();

			assertTrue("Database user doesn't inherit isAdmin flag correctly.", user.isAdmin());

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}
	}
	*/

	@Test
	public void testAdminGroupsWithServicePrincipal() {

		final SecurityContext ctx = SecurityContext.getSuperUserInstance();

		try (final Tx tx = app.tx()) {

			final Group group1 = app.create(Group.class, new NodeAttribute<>(Group.name, "Group1"));
			final Group group2 = app.create(Group.class, new NodeAttribute<>(Group.name, "Group2"));
			final Group group3 = app.create(Group.class, new NodeAttribute<>(Group.name, "Group3"));

			// Group1 is the admin group
			group1.setIsAdmin(true);

			// group hierarchy: Group1 -> Group2 -> Group3
			group1.addMember(ctx, group2);
			group2.addMember(ctx, group3);

			// Group3 has jwksReferenceId for ServicePrincipal
			group3.setProperty(StructrApp.key(Group.class, "jwksReferenceId"), "admin_group");

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final ServicePrincipal principal = new ServicePrincipal("tester", "tester", List.of("admin_group"), false);

			assertTrue("ServicePrincipal doesn't inherit isAdmin flag correctly.", principal.isAdmin());

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testAdminFlagWithServicePrincipal() {

		final SecurityContext ctx = SecurityContext.getSuperUserInstance();

		try (final Tx tx = app.tx()) {

			final ServicePrincipal principal = new ServicePrincipal("tester", "tester", null, true);

			assertTrue("ServicePrincipal doesn't inherit isAdmin flag correctly.", principal.isAdmin());

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	public static void clearResourceAccess() {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final ResourceAccess access : app.nodeQuery(ResourceAccess.class).getAsList()) {
				app.delete(access);
			}

			tx.success();

		} catch (Throwable t) {

			logger.warn("Unable to clear resource access permissions", t);
		}
	}

	// ----- private methods -----
	private void setPermissionResolution(final String uuid, final PropertyKey key, final Object value) {

		// enable permission resolution
		try (final Tx tx = app.tx()) {

			final SchemaRelationshipNode rel = app.get(SchemaRelationshipNode.class, uuid);

			rel.setProperty(SchemaRelationshipNode.permissionPropagation, PropagationDirection.Both);
			rel.setProperty(key, value);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	private void configureSchemaGrant(final String uuid, final PropertyKey key, final boolean value) {

		// enable permission resolution
		try (final Tx tx = app.tx()) {

			final SchemaGrant grant = app.get(SchemaGrant.class, uuid);

			grant.setProperty(key, value);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	private void testGranted(final Class projectType, final boolean[] expected) {

		try (final Tx tx = app.tx()) {

			final Principal tester            = app.nodeQuery(Principal.class).andName("tester").getFirst();
			final SecurityContext userContext = SecurityContext.getInstance(tester, AccessMode.Backend);
			final List<NodeInterface> result  = app.nodeQuery(projectType).sort(AbstractNode.name).getAsList();

			assertEquals("Invalid permission resolution result",  true, result.get(0).isGranted(Permission.read,          userContext));
			assertEquals("Invalid permission resolution result",  true, result.get(0).isGranted(Permission.write,         userContext));
			assertEquals("Invalid permission resolution result",  true, result.get(0).isGranted(Permission.delete,        userContext));
			assertEquals("Invalid permission resolution result",  true, result.get(0).isGranted(Permission.accessControl, userContext));

			System.out.println("#################################################### TEST");

			assertEquals("Invalid permission resolution result",  expected[0], result.get(1).isGranted(Permission.read,          userContext));
			assertEquals("Invalid permission resolution result",  expected[1], result.get(1).isGranted(Permission.write,         userContext));
			assertEquals("Invalid permission resolution result",  expected[2], result.get(1).isGranted(Permission.delete,        userContext));
			assertEquals("Invalid permission resolution result",  expected[3], result.get(1).isGranted(Permission.accessControl, userContext));

			assertEquals("Invalid permission resolution result",  expected[0], result.get(2).isGranted(Permission.read,          userContext));
			assertEquals("Invalid permission resolution result",  expected[1], result.get(2).isGranted(Permission.write,         userContext));
			assertEquals("Invalid permission resolution result",  expected[2], result.get(2).isGranted(Permission.delete,        userContext));
			assertEquals("Invalid permission resolution result",  expected[3], result.get(2).isGranted(Permission.accessControl, userContext));

			assertEquals("Invalid permission resolution result",  expected[0], result.get(3).isGranted(Permission.read,          userContext));
			assertEquals("Invalid permission resolution result",  expected[1], result.get(3).isGranted(Permission.write,         userContext));
			assertEquals("Invalid permission resolution result",  expected[2], result.get(3).isGranted(Permission.delete,        userContext));
			assertEquals("Invalid permission resolution result",  expected[3], result.get(3).isGranted(Permission.accessControl, userContext));

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}
}
