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
import org.structr.api.graph.PropagationDirection;
import org.structr.api.graph.PropagationMode;
import org.structr.common.AccessControllable;
import org.structr.common.AccessMode;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.ServicePrincipal;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SchemaGrant;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
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

		NodeInterface rel   = null;
		PropertyKey key     = null;
		Principal user1     = null;
		String type1        = null;
		String type2        = null;

		try (final Tx tx = app.tx()) {

			// create a test user
			user1 = app.create("User", "user1").as(Principal.class);

			// create schema setup with permission propagation
			final NodeInterface t1 = app.create("SchemaNode", "Type1");
			final NodeInterface t2 = app.create("SchemaNode", "Type2");

			rel = app.create("SchemaRelationshipNode",
				new NodeAttribute<>(Traits.of("SchemaRelationshipNode").key("sourceNode"), t1),
				new NodeAttribute<>(Traits.of("SchemaRelationshipNode").key("targetNode"), t2),
				new NodeAttribute<>(Traits.of("SchemaRelationshipNode").key("relationshipType"), "RELATED"),
				new NodeAttribute<>(Traits.of("SchemaRelationshipNode").key("sourceMultiplicity"), "1"),
				new NodeAttribute<>(Traits.of("SchemaRelationshipNode").key("targetMultiplicity"), "1"),
				new NodeAttribute<>(Traits.of("SchemaRelationshipNode").key("sourceJsonName"), "source"),
				new NodeAttribute<>(Traits.of("SchemaRelationshipNode").key("targetJsonName"), "target")
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

			type1 = "Type1";
			type2 = "Type2";
			key   = Traits.of(type1).key("target");

			assertNotNull("Node type Type1 should exist.", Traits.of(type1));
			assertNotNull("Node type Type2 should exist.", Traits.of(type2));
			assertNotNull("Property key \"target\" should exist.", key);

			final NodeInterface instance1 = app.create(type1, "instance1OfType1");
			final NodeInterface instance2 = app.create(type2, "instance1OfType2");

			assertNotNull("Instance of type Type1 should exist", instance1);
			assertNotNull("Instance of type Type2 should exist", instance2);

			instance1.setProperty(key, instance2);

			// make instance1 visible to user1
			instance1.as(AccessControllable.class).grant(Permission.read, user1);

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

			rel.setProperty(Traits.of("SchemaRelationshipNode").key("permissionPropagation"), PropagationDirection.In);
			rel.setProperty(Traits.of("SchemaRelationshipNode").key("readPropagation"), PropagationMode.Add);

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

			rel.setProperty(Traits.of("SchemaRelationshipNode").key("permissionPropagation"), PropagationDirection.Out);
			rel.setProperty(Traits.of("SchemaRelationshipNode").key("readPropagation"), PropagationMode.Add);

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

			rel.setProperty(Traits.of("SchemaRelationshipNode").key("permissionPropagation"), PropagationDirection.Both);
			rel.setProperty(Traits.of("SchemaRelationshipNode").key("readPropagation"), PropagationMode.Add);

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

			rel.setProperty(Traits.of("SchemaRelationshipNode").key("permissionPropagation"), PropagationDirection.None);
			rel.setProperty(Traits.of("SchemaRelationshipNode").key("readPropagation"), PropagationMode.Add);

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
			final NodeInterface type = app.create("SchemaNode", "Project");

			uuid = app.create("SchemaRelationshipNode",
				new NodeAttribute<>(Traits.of("SchemaRelationshipNode").key("sourceNode"), type),
				new NodeAttribute<>(Traits.of("SchemaRelationshipNode").key("targetNode"), type),
				new NodeAttribute<>(Traits.of("SchemaRelationshipNode").key("relationshipType"), "NEXT"),
				new NodeAttribute<>(Traits.of("SchemaRelationshipNode").key("sourceMultiplicity"), "1"),
				new NodeAttribute<>(Traits.of("SchemaRelationshipNode").key("targetMultiplicity"), "1"),
				new NodeAttribute<>(Traits.of("SchemaRelationshipNode").key("sourceJsonName"), "prev"),
				new NodeAttribute<>(Traits.of("SchemaRelationshipNode").key("targetJsonName"), "next")
			).getUuid();

			app.create("User", "tester");

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final String projectType = "Project";
		final PropertyKey key    = Traits.of(projectType).key("prev");

		try (final Tx tx = app.tx()) {

			final Principal tester = app.nodeQuery("User").getFirst().as(Principal.class);

			final NodeInterface p1 = app.create(projectType,
				new NodeAttribute<>(Traits.of("AbstractNode").key("name"), "Project1"),
				new NodeAttribute<>(Traits.of("AbstractNode").key("owner"), tester)
			);

			final NodeInterface p2 = app.create(projectType,
				new NodeAttribute<>(Traits.of("AbstractNode").key("name"), "Project2"),
				new NodeAttribute<>(key, p1)
			);

			final NodeInterface p3 = app.create(projectType,
				new NodeAttribute<>(Traits.of("AbstractNode").key("name"), "Project3"),
				new NodeAttribute<>(key, p2)
			);

			final NodeInterface p4 = app.create(projectType,
				new NodeAttribute<>(Traits.of("AbstractNode").key("name"), "Project4"),
				new NodeAttribute<>(key, p3)
			);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		testGranted(projectType, new boolean[]{false, false, false, false});
		setPermissionResolution(uuid, Traits.of("SchemaRelationshipNode").key("readPropagation"), PropagationMode.Add);
		testGranted(projectType, new boolean[]{true, false, false, false});
		setPermissionResolution(uuid, Traits.of("SchemaRelationshipNode").key("writePropagation"), PropagationMode.Add);
		testGranted(projectType, new boolean[]{true, true, false, false});
		setPermissionResolution(uuid, Traits.of("SchemaRelationshipNode").key("deletePropagation"), PropagationMode.Add);
		testGranted(projectType, new boolean[]{true, true, true, false});
		setPermissionResolution(uuid, Traits.of("SchemaRelationshipNode").key("accessControlPropagation"), PropagationMode.Add);
		testGranted(projectType, new boolean[]{true, true, true, true});

		setPermissionResolution(uuid, Traits.of("SchemaRelationshipNode").key("readPropagation"), PropagationMode.Remove);
		testGranted(projectType, new boolean[]{false, true, true, true});
		setPermissionResolution(uuid, Traits.of("SchemaRelationshipNode").key("writePropagation"), PropagationMode.Remove);
		testGranted(projectType, new boolean[]{false, false, true, true});
		setPermissionResolution(uuid, Traits.of("SchemaRelationshipNode").key("deletePropagation"), PropagationMode.Remove);
		testGranted(projectType, new boolean[]{false, false, false, true});
		setPermissionResolution(uuid, Traits.of("SchemaRelationshipNode").key("accessControlPropagation"), PropagationMode.Remove);
		testGranted(projectType, new boolean[]{false, false, false, false});
	}

	@Test
	public void testPermissionResolutionWithSelfRelationshipAndInheritance() {

		final App app = StructrApp.getInstance();
		String uuid   = null;

		try (final Tx tx = app.tx()) {

			// create schema setup with permission propagation
			final NodeInterface type = app.create("SchemaNode", "Project");

			uuid = app.create("SchemaRelationshipNode",
				new NodeAttribute<>(Traits.of("SchemaRelationshipNode").key("sourceNode"), type),
				new NodeAttribute<>(Traits.of("SchemaRelationshipNode").key("targetNode"), type),
				new NodeAttribute<>(Traits.of("SchemaRelationshipNode").key("relationshipType"), "NEXT"),
				new NodeAttribute<>(Traits.of("SchemaRelationshipNode").key("sourceMultiplicity"), "1"),
				new NodeAttribute<>(Traits.of("SchemaRelationshipNode").key("targetMultiplicity"), "1"),
				new NodeAttribute<>(Traits.of("SchemaRelationshipNode").key("sourceJsonName"), "prev"),
				new NodeAttribute<>(Traits.of("SchemaRelationshipNode").key("targetJsonName"), "next")
			).getUuid();

			final NodeInterface moo  = app.create("SchemaNode", "Moo");
			final NodeInterface test = app.create("SchemaNode", "Test");

			moo.setProperty(Traits.of("SchemaNode").key("extendsClass"), type);
			test.setProperty(Traits.of("SchemaNode").key("extendsClass"), type);

			app.create("User", "tester");

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final String projectType = "Project";
		final String mooType     = "Moo";
		final String testType    = "Test";
		final PropertyKey key    = Traits.of(mooType).key("prev");

		try (final Tx tx = app.tx()) {

			final Principal tester = app.nodeQuery("User").getFirst().as(Principal.class);

			final NodeInterface p1 = app.create(mooType,
				new NodeAttribute<>(Traits.of("AbstractNode").key("name"), "Project1"),
				new NodeAttribute<>(Traits.of("AbstractNode").key("owner"), tester)
			);

			final NodeInterface p2 = app.create(testType,
				new NodeAttribute<>(Traits.of("AbstractNode").key("name"), "Project2"),
				new NodeAttribute<>(key, p1)
			);

			final NodeInterface p3 = app.create(mooType,
				new NodeAttribute<>(Traits.of("AbstractNode").key("name"), "Project3"),
				new NodeAttribute<>(key, p2)
			);

			final NodeInterface p4 = app.create(testType,
				new NodeAttribute<>(Traits.of("AbstractNode").key("name"), "Project4"),
				new NodeAttribute<>(key, p3)
			);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		testGranted(projectType, new boolean[] { false, false, false, false });
		setPermissionResolution(uuid, Traits.of("SchemaRelationshipNode").key("readPropagation,        "), PropagationMode.Add);
		testGranted(projectType, new boolean[] { true, false, false, false });
		setPermissionResolution(uuid, Traits.of("SchemaRelationshipNode").key("writePropagation,       "), PropagationMode.Add);
		testGranted(projectType, new boolean[] { true, true, false, false });
		setPermissionResolution(uuid, Traits.of("SchemaRelationshipNode").key("deletePropagation,      "), PropagationMode.Add);
		testGranted(projectType, new boolean[] { true, true, true, false });
		setPermissionResolution(uuid, Traits.of("SchemaRelationshipNode").key("accessControlPropagation"), PropagationMode.Add);
		testGranted(projectType, new boolean[] { true, true, true, true });

		setPermissionResolution(uuid, Traits.of("SchemaRelationshipNode").key("readPropagation,        "), PropagationMode.Remove);
		testGranted(projectType, new boolean[] { false, true, true, true });
		setPermissionResolution(uuid, Traits.of("SchemaRelationshipNode").key("writePropagation,       "), PropagationMode.Remove);
		testGranted(projectType, new boolean[] { false, false, true, true });
		setPermissionResolution(uuid, Traits.of("SchemaRelationshipNode").key("deletePropagation,      "), PropagationMode.Remove);
		testGranted(projectType, new boolean[] { false, false, false, true });
		setPermissionResolution(uuid, Traits.of("SchemaRelationshipNode").key("accessControlPropagation"), PropagationMode.Remove);
		testGranted(projectType, new boolean[] { false, false, false, false });
	}

	@Test
	public void testSchemaGrants() {

		final App app    = StructrApp.getInstance();
		String uuid      = null;

		try (final Tx tx = app.tx()) {

			// create schema setup with permission propagation
			app.create("SchemaNode", "Project");
			app.create("User", "tester");

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		// setup 2 - schema grant
		try (final Tx tx = app.tx()) {

			final Group testGroup1 = app.create("Group", "Group1").as(Group.class);
			final Group testGroup2 = app.create("Group", "Group2").as(Group.class);
			final Group testGroup3 = app.create("Group", "Group3").as(Group.class);
			final Principal tester = app.nodeQuery("User").andName("tester").getFirst().as(Principal.class);

			// create group hierarchy for test user
			testGroup1.addMember(securityContext, testGroup2);
			testGroup2.addMember(securityContext, testGroup3);
			testGroup3.addMember(securityContext, tester);

			// create grant
			final NodeInterface projectNode = app.nodeQuery("SchemaNode").andName("Project").getFirst();
			final NodeInterface grant      = app.create("SchemaGrant",
				new NodeAttribute<>(Traits.of("SchemaGrant").key("schemaNode"),          projectNode),
				new NodeAttribute<>(Traits.of("SchemaGrant").key("principal"),           testGroup1),
				new NodeAttribute<>(Traits.of("SchemaGrant").key("allowRead"),           false),
				new NodeAttribute<>(Traits.of("SchemaGrant").key("allowWrite"),          false),
				new NodeAttribute<>(Traits.of("SchemaGrant").key("allowDelete"),         false),
				new NodeAttribute<>(Traits.of("SchemaGrant").key("allowAccessControl"),  false)
			);

			uuid = grant.getUuid();

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		final String projectType = "Project";

		try (final Tx tx = app.tx()) {

			final Principal tester = app.nodeQuery("User").andName("tester").getFirst().as(Principal.class);

			app.create(projectType, new NodeAttribute<>(Traits.of("AbstractNode").key("name"), "Project1"), new NodeAttribute<>(Traits.of("AbstractNode").key("owner"), tester));
			app.create(projectType, new NodeAttribute<>(Traits.of("AbstractNode").key("name"), "Project2"));
			app.create(projectType, new NodeAttribute<>(Traits.of("AbstractNode").key("name"), "Project3"));
			app.create(projectType, new NodeAttribute<>(Traits.of("AbstractNode").key("name"), "Project4"));

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		testGranted(projectType, new boolean[] { false, false, false, false });
		configureSchemaGrant(uuid, Traits.of("SchemaGrant").key("allowRead"), true);
		testGranted(projectType, new boolean[] { true, false, false, false });
		configureSchemaGrant(uuid, Traits.of("SchemaGrant").key("allowWrite"), true);
		testGranted(projectType, new boolean[] { true, true, false, false });
		configureSchemaGrant(uuid, Traits.of("SchemaGrant").key("allowDelete"), true);
		testGranted(projectType, new boolean[] { true, true, true, false });
		configureSchemaGrant(uuid, Traits.of("SchemaGrant").key("allowAccessControl"), true);
		testGranted(projectType, new boolean[] { true, true, true, true });

		configureSchemaGrant(uuid, Traits.of("SchemaGrant").key("allowRead"), false);
		testGranted(projectType, new boolean[] { false, true, true, true });
		configureSchemaGrant(uuid, Traits.of("SchemaGrant").key("allowWrite"), false);
		testGranted(projectType, new boolean[] { false, false, true, true });
		configureSchemaGrant(uuid, Traits.of("SchemaGrant").key("allowDelete"), false);
		testGranted(projectType, new boolean[] { false, false, false, true });

		// allow all, but remove group link
		configureSchemaGrant(uuid, Traits.of("SchemaGrant").key("allowRead"), true);
		configureSchemaGrant(uuid, Traits.of("SchemaGrant").key("allowWrite"), true);
		configureSchemaGrant(uuid, Traits.of("SchemaGrant").key("allowDelete"), true);
		configureSchemaGrant(uuid, Traits.of("SchemaGrant").key("allowAccessControl"), true);

		try (final Tx tx = app.tx()) {

			// delete Group2 which links tester to granted Group1
			app.delete(app.nodeQuery("Group").andName("Group2").getFirst());
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

			final Group group1 = app.create("Group", new NodeAttribute<>(Traits.of("Group").key("name"), "Group1"));
			final Group group2 = app.create("Group", new NodeAttribute<>(Traits.of("Group").key("name"), "Group2"));
			final Group group3 = app.create("Group", new NodeAttribute<>(Traits.of("Group").key("name"), "Group3"));

			// Group1 is the admin group
			group1.setIsAdmin(true);

			// group hierarchy: Group1 -> Group2 -> Group3
			group1.addMember(ctx, group2);
			group2.addMember(ctx, group3);

			final Principal tester = app.create("User", "tester");

			group3.addMember(ctx, tester);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final Principal user = app.nodeQuery("User").andName("tester").getFirst();

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

			final Group group1 = app.create("Group", new NodeAttribute<>(Traits.of("Group").key("name"), "Group1")).as(Group.class);
			final Group group2 = app.create("Group", new NodeAttribute<>(Traits.of("Group").key("name"), "Group2")).as(Group.class);
			final Group group3 = app.create("Group", new NodeAttribute<>(Traits.of("Group").key("name"), "Group3")).as(Group.class);

			// Group1 is the admin group
			group1.setIsAdmin(true);

			// group hierarchy: Group1 -> Group2 -> Group3
			group1.addMember(ctx, group2);
			group2.addMember(ctx, group3);

			// Group3 has jwksReferenceId for ServicePrincipal
			group3.setProperty(Traits.of("StructrApp").key("jwksReferenceId"), "admin_group");

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

			for (final NodeInterface access : app.nodeQuery("ResourceAccess").getAsList()) {
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

			final NodeInterface rel = app.getNodeById("SchemaRelationshipNode", uuid);

			rel.setProperty(Traits.of("SchemaRelationshipNode").key("permissionPropagation"), PropagationDirection.Both);
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

			final SchemaGrant grant = app.getNodeById("SchemaGrant", uuid).as(SchemaGrant.class);

			grant.setProperty(key, value);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	private void testGranted(final String projectType, final boolean[] expected) {

		try (final Tx tx = app.tx()) {

			final Principal tester            = app.nodeQuery("User").andName("tester").getFirst().as(Principal.class);
			final SecurityContext userContext = SecurityContext.getInstance(tester, AccessMode.Backend);
			final List<NodeInterface> result  = app.nodeQuery(projectType).sort(Traits.nameProperty()).getAsList();

			assertEquals("Invalid permission resolution result",  true, result.get(0).isGranted(Permission.read,          userContext));
			assertEquals("Invalid permission resolution result",  true, result.get(0).isGranted(Permission.write,         userContext));
			assertEquals("Invalid permission resolution result",  true, result.get(0).isGranted(Permission.delete,        userContext));
			assertEquals("Invalid permission resolution result",  true, result.get(0).isGranted(Permission.accessControl, userContext));

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
