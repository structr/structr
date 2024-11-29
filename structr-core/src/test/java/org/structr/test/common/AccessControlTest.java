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
import org.structr.api.graph.Direction;
import org.structr.api.graph.Relationship;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.api.util.Iterables;
import org.structr.common.AccessMode;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorToken;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.*;
import org.structr.core.entity.relationship.Ownership;
import org.structr.core.entity.relationship.PrincipalOwnsNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.schema.export.StructrSchema;
import org.structr.test.core.entity.TestOne;
import org.structr.web.entity.User;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.AssertJUnit.*;

/**
 * Test access control with different permission levels.
 *
 *
 */
public class AccessControlTest extends StructrTest {

	private static final Logger logger = LoggerFactory.getLogger(AccessControlTest.class.getName());

	@Test
	public void test01PublicAccessToNonPublicNode() {

		final Class principalType = StructrApp.getConfiguration().getNodeEntityClass("User");

		// remove auto-generated resource access objects
		clearResourceAccess();

		try {

			User user = (User)createTestNode(principalType);

			// Create node with user context
			Class type = TestOne.class;
			createTestNode(TestOne.class, user);

			SecurityContext publicContext = SecurityContext.getInstance(null, AccessMode.Frontend);

			try (final Tx tx = app.tx()) {

				List result = StructrApp.getInstance(publicContext).nodeQuery(type).getAsList();

				// Node should not be visible in public context (no user logged in)
				assertTrue(result.isEmpty());
			}

		} catch (FrameworkException ex) {

			logger.warn("", ex);
			fail("Unexpected exception");

		}
	}


	@Test
	public void test02PublicAccessToPublicNode() {

		// remove auto-generated resource access objects
		clearResourceAccess();

		try {

			List<User> users = createTestNodes(User.class, 1);
			User user = (User) users.get(0);

			PropertyMap props = new PropertyMap();
			props.put(AbstractNode.visibleToPublicUsers, true);

			// Create two nodes with user context, one of them is visible to public users
			Class type = TestOne.class;
			TestOne t1 = createTestNode(TestOne.class, props, user);
			TestOne t2 = createTestNode(TestOne.class, user);

			SecurityContext publicContext = SecurityContext.getInstance(null, AccessMode.Frontend);

			try (final Tx tx = app.tx()) {

				List<GraphObject> result = StructrApp.getInstance(publicContext).nodeQuery(type).getAsList();

				assertEquals(1, result.size());
				assertEquals(t1.getUuid(), result.get(0).getUuid());
			}

		} catch (FrameworkException ex) {

			logger.warn("", ex);
			fail("Unexpected exception");

		}

	}

	@Test
	public void test03PublicAccessToProtectedNode() {

		// remove auto-generated resource access objects
		clearResourceAccess();

		try {

			List<User> users = createTestNodes(User.class, 1);
			User user = (User) users.get(0);

			PropertyMap props = new PropertyMap();
			props.put(AbstractNode.visibleToPublicUsers, true);

			// Create two nodes with user context, one of them is visible to public users
			Class type = TestOne.class;
			TestOne t1 = createTestNode(TestOne.class, props, user);

			props = new PropertyMap();
			props.put(AbstractNode.visibleToAuthenticatedUsers, true);

			TestOne t2 = createTestNode(TestOne.class, props, user);

			SecurityContext publicContext = SecurityContext.getInstance(null, AccessMode.Frontend);

			try (final Tx tx = app.tx()) {

				List<GraphObject> result = StructrApp.getInstance(publicContext).nodeQuery(type).getAsList();

				assertEquals(1, result.size());
				assertEquals(t1.getUuid(), result.get(0).getUuid());
			}

		} catch (FrameworkException ex) {

			logger.warn("", ex);
			fail("Unexpected exception");

		}

	}

	@Test
	public void test04BackendUserAccessToProtectedNode() {

		// remove auto-generated resource access objects
		clearResourceAccess();

		try {

			List<User> users = createTestNodes(User.class, 2);
			User user1 = (User) users.get(0);
			User user2 = (User) users.get(1);

			PropertyMap props = new PropertyMap();
			props.put(AbstractNode.visibleToPublicUsers, true);

			// Create two nodes with user context, one of them is visible to public users
			Class type = TestOne.class;
			TestOne t1 = createTestNode(TestOne.class, props, user1);

			props = new PropertyMap();
			props.put(AbstractNode.visibleToAuthenticatedUsers, true);

			TestOne t2 = createTestNode(TestOne.class, props, user1);

			// Let another user search
			SecurityContext user2Context = SecurityContext.getInstance(user2, AccessMode.Backend);

			try (final Tx tx = app.tx()) {
				List<GraphObject> result = StructrApp.getInstance(user2Context).nodeQuery(type).getAsList();

				assertEquals(2, result.size());
			}

		} catch (FrameworkException ex) {

			logger.warn("", ex);
			fail("Unexpected exception");

		}

	}

	@Test
	public void test05FrontendUserAccessToProtectedNode() {

		// remove auto-generated resource access objects
		clearResourceAccess();

		try {

			List<User> users = createTestNodes(User.class, 2);
			User user1 = (User) users.get(0);
			User user2 = (User) users.get(1);

			PropertyMap props = new PropertyMap();
			props.put(AbstractNode.visibleToPublicUsers, true);

			// Create two nodes with user context, one of them is visible to public users
			Class type = TestOne.class;
			TestOne t1 = createTestNode(TestOne.class, props, user1);

			props = new PropertyMap();
			props.put(AbstractNode.visibleToAuthenticatedUsers, true);

			TestOne t2 = createTestNode(TestOne.class, props, user1);

			// Let another user search
			SecurityContext user2Context = SecurityContext.getInstance(user2, AccessMode.Frontend);

			try (final Tx tx = app.tx()) {

				List<GraphObject> result = StructrApp.getInstance(user2Context).nodeQuery(type).getAsList();

				assertEquals(2, result.size());
			}

		} catch (FrameworkException ex) {

			logger.warn("", ex);
			fail("Unexpected exception");

		}

	}

	@Test
	public void test06GrantReadPermission() {

		// remove auto-generated resource access objects
		clearResourceAccess();

		try {

			List<User> users = createTestNodes(User.class, 2);
			User user1 = (User) users.get(0);
			User user2 = (User) users.get(1);
			List<GraphObject> result = null;

			// Let user 1 create a node
			Class type = TestOne.class;
			final TestOne t1 = createTestNode(TestOne.class, user1);

			try (final Tx tx = app.tx()) {

				// Grant read permission to user 2
				t1.grant(Permission.read, user2);
				tx.success();
			}

			// Let user 2 search
			SecurityContext user2Context = SecurityContext.getInstance(user2, AccessMode.Backend);

			try (final Tx tx = app.tx()) {

				result = StructrApp.getInstance(user2Context).nodeQuery(type).getAsList();

				assertEquals(1, result.size());
				assertEquals(t1.getUuid(), result.get(0).getUuid());
			}

			try (final Tx tx = app.tx()) {

				// Revoke permission again
				t1.revoke(Permission.read, user2);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				result = StructrApp.getInstance(user2Context).nodeQuery(type).getAsList();
				assertTrue(result.isEmpty());
			}

		} catch (FrameworkException ex) {

			logger.warn("", ex);
			fail("Unexpected exception");

		}

	}

	@Test
	public void test07ResultCount() {

		// remove auto-generated resource access objects
		clearResourceAccess();

		try {

			final Class type = TestOne.class;
			final List<NodeInterface> nodes = createTestNodes(type, 10, 100);

			try (final Tx tx = app.tx()) {
				nodes.get(3).setProperty(AbstractNode.visibleToPublicUsers, true);
				nodes.get(5).setProperty(AbstractNode.visibleToPublicUsers, true);
				nodes.get(7).setProperty(AbstractNode.visibleToPublicUsers, true);
				tx.success();
			}

			SecurityContext publicContext = SecurityContext.getInstance(null, AccessMode.Frontend);

			try (final Tx tx = app.tx()) {

				List<GraphObject> result = StructrApp.getInstance(publicContext).nodeQuery(type).sort(AbstractNode.createdDate).getAsList();

				assertEquals(3, result.size());

				// test order of elements (works again)
				assertEquals(nodes.get(3).getUuid(), result.get(0).getUuid());
				assertEquals(nodes.get(5).getUuid(), result.get(1).getUuid());
				assertEquals(nodes.get(7).getUuid(), result.get(2).getUuid());
			}

		} catch (FrameworkException ex) {

			logger.warn("", ex);
			fail("Unexpected exception");

		}

	}

	@Test
	public void test07ResultCountWithPaging() {

		// remove auto-generated resource access objects
		clearResourceAccess();

		try {

			final Class type = TestOne.class;
			final List<NodeInterface> nodes = createTestNodes(type, 10);
			int count = 0;

			try (final Tx tx = app.tx()) {

				// add names to make sorting work...
				for (final NodeInterface node : nodes) {
					node.setProperty(AbstractNode.name, "node0" + count++);
				}

				nodes.get(3).setProperty(AbstractNode.visibleToPublicUsers, true);
				nodes.get(5).setProperty(AbstractNode.visibleToPublicUsers, true);
				nodes.get(7).setProperty(AbstractNode.visibleToPublicUsers, true);
				nodes.get(9).setProperty(AbstractNode.visibleToPublicUsers, true);
				tx.success();
			}

			SecurityContext publicContext = SecurityContext.getInstance(null, AccessMode.Frontend);

			PropertyKey sortKey = AbstractNode.name;
			boolean sortDesc    = false;
			int pageSize        = 2;
			int page            = 1;

			try (final Tx tx = app.tx()) {

				List<GraphObject> result = StructrApp.getInstance(publicContext).nodeQuery(type).sort(sortKey, sortDesc).page(page).pageSize(pageSize).getAsList();

				assertEquals(2, result.size());

				assertEquals(nodes.get(3).getUuid(), result.get(0).getUuid());
				assertEquals(nodes.get(5).getUuid(), result.get(1).getUuid());
			}

		} catch (FrameworkException ex) {

			logger.warn("", ex);
			fail("Unexpected exception");

		}

	}

	@Test
	public void test08WriteAccess() {

		// remove auto-generated resource access objects
		clearResourceAccess();

		try {

			final User owner = createTestNode(User.class);

			// create new node
			createTestNode(TestOne.class, owner);


			final SecurityContext userContext = SecurityContext.getInstance(owner, AccessMode.Frontend);
			final App userApp                 = StructrApp.getInstance(userContext);

			try (final Tx tx = userApp.tx()) {

				final TestOne t = StructrApp.getInstance(userContext).nodeQuery(TestOne.class).getFirst();

				assertNotNull(t);

				t.setProperty(TestOne.aString, "aString");

				assertEquals("aString", t.getProperty(TestOne.aString));

				tx.success();
			}

		} catch (FrameworkException ex) {

			logger.warn("", ex);
			fail("Unexpected exception");

		}

	}

	@Test
	public void test01WriteAccess() {

		// remove auto-generated resource access objects
		clearResourceAccess();

		try {

			final User owner = createTestNode(User.class);
			final User user  = createTestNode(User.class);

			// create new node
			final TestOne t1     = createTestNode(TestOne.class, owner);

			final SecurityContext ownerContext = SecurityContext.getInstance(owner, AccessMode.Frontend);
			final SecurityContext userContext  = SecurityContext.getInstance(user, AccessMode.Frontend);
			final App ownerAppContext          = StructrApp.getInstance(ownerContext);
			final App userAppContext           = StructrApp.getInstance(userContext);

			// test with owner, expect success
			try (final Tx tx = ownerAppContext.tx()) {

				final TestOne t = StructrApp.getInstance(ownerContext).nodeQuery(TestOne.class).getFirst();

				assertNotNull(t);

				t.setProperty(TestOne.aString, "aString");
				assertEquals("aString", t.getProperty(TestOne.aString));

				tx.success();
			}

			// test with foreign user, expect failure, node should not be found
			try (final Tx tx = userAppContext.tx()) {

				// node should not be found
				assertNull(StructrApp.getInstance(userContext).nodeQuery(TestOne.class).getFirst());

				tx.success();
			}

			// test with foreign user, expect failure, node should not be found
			try (final Tx tx = ownerAppContext.tx()) {

				// make node visible to user
				t1.grant(Permission.read, user);

				tx.success();
			}

			// try to grant read permissions in user context, should fail because user doesn't have access control permission
			try (final Tx tx = userAppContext.tx()) {

				try {
					final TestOne t = StructrApp.getInstance(userContext).nodeQuery(TestOne.class).getFirst();
					t.grant(Permission.read, user);

					fail("Non-owner should not be allowed to change permissions on object");

				} catch (FrameworkException fex) {

					// expect status 403 forbidden
					assertEquals(fex.getStatus(), 403);

				}

				tx.success();
			}

			// try to grant read permissions in owner context, should succeed (?)
			try (final Tx tx = ownerAppContext.tx()) {

				// important lesson here: the context under which the node is constructed defines the security context
				final TestOne t = StructrApp.getInstance(ownerContext).nodeQuery(TestOne.class).getFirst();
				t.grant(Permission.accessControl, user);
				tx.success();
			}

			// test with foreign user, expect failure
			try (final Tx tx = userAppContext.tx()) {

				final TestOne t = StructrApp.getInstance(userContext).nodeQuery(TestOne.class).getFirst();

				// node should be found because it's public
				assertNotNull(t);

				// setProperty should fail because of missing write permissions
				try {

					t.setProperty(TestOne.aString, "aString");
					fail("setProperty should not be allowed for non-owner on publicly visible nodes");

				} catch (FrameworkException fex) {

					// expect status 403 forbidden
					assertEquals(fex.getStatus(), 403);
				}

				tx.success();
			}

			// grant write
			try (final Tx tx = app.tx()) {

				// make t1 visible to public users explicitely
				t1.setProperty(GraphObject.visibleToPublicUsers, true);

				tx.success();
			}

		} catch (FrameworkException ex) {

			logger.warn("", ex);
			fail("Unexpected exception");

		}

	}

	@Test
	public void testGroupMembershipVisibility() {

		String user1Id = null;
		String user2Id = null;
		String groupId = null;

		SecurityContext user1Context = null;
		SecurityContext user2Context = null;

		// ################################################################################################################
		// create two users

		try (final Tx tx = app.tx()) {

			User user1 = createTestNode(User.class, "user1");
			user1Id = user1.getUuid();
			user1Context = SecurityContext.getInstance(user1, AccessMode.Backend);

			User user2 = createTestNode(User.class, "user2");
			user2Id = user2.getUuid();
			user2Context = SecurityContext.getInstance(user2, AccessMode.Backend);

			// Grant user1 read permission on user2
			user2.grant(Permission.read, user1);

			tx.success();

		} catch (FrameworkException t) {

			logger.warn("", t);
			fail("Unexpected exception.");
		}

		App user1App = StructrApp.getInstance(user1Context);

		// ################################################################################################################
		// create a group and a test object that becomes accessible for the second user by group membership

		try (final Tx tx = user1App.tx()) {

			Group group = user1App.create(Group.class, "group");
			groupId = group.getUuid();

			user1App.create(TestOne.class, "testone");

			User user1 = user1App.get(User.class, user1Id);
			assertEquals("Invalid group owner", user1, group.getOwnerNode());

			tx.success();

		} catch (FrameworkException t) {

			logger.warn("", t);
			fail("Unexpected exception.");
		}

		// ################################################################################################################
		// user1 is owner of the test object
		// we now grant the group read access to the test object

		try (final Tx tx = user1App.tx()) {

			final TestOne test = user1App.nodeQuery(TestOne.class).getFirst();

			assertNotNull(test);

			Group group = user1App.get(Group.class, groupId);
			test.grant(Permission.read, group);

			tx.success();

		} catch (FrameworkException t) {

			logger.warn("", t);
			fail("Unexpected exception.");
		}

		// ################################################################################################################
		// user2 is not yet member of the group, so
		// it should not be possible to access the object

		App user2App = StructrApp.getInstance(user2Context);

		try (final Tx tx = user2App.tx()) {

			final TestOne test = user2App.nodeQuery(TestOne.class).getFirst();
			assertNull("TestOne instance should not be visible to user2", test);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// ################################################################################################################
		// now we add user2 to the group

		try (final Tx tx = user1App.tx()) {

			Group group = user1App.get(Group.class, groupId);

			User user2 = user1App.get(User.class, user2Id);
			assertNotNull(user2);

			group.addMember(user1Context, user2);
			tx.success();

		} catch (FrameworkException t) {

			logger.warn("", t);
			fail("Unexpected exception.");

		}

		// ################################################################################################################
		// check parents of user2

		try (final Tx tx = user1App.tx()) {

			User user2 = user1App.get(User.class, user2Id);
			assertEquals("User should have parents", 1, Iterables.count(user2.getParents()));

			tx.success();

		} catch (FrameworkException t) {

			logger.warn("", t);
			fail("Unexpected exception.");
		}

		// ################################################################################################################
		// user2 is now member of the group, so
		// it should be possible to access the object

		try (final Tx tx = user2App.tx()) {

			final TestOne test = user2App.nodeQuery(TestOne.class).getFirst();
			assertNotNull("TestOne instance should be readable for members", test);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// ################################################################################################################
		// user2 should NOT be able to write the object
		String testId = "";
		String testType = "";
		try (final Tx tx = user2App.tx()) {

			final TestOne test = user2App.nodeQuery(TestOne.class).getFirst();
			assertNotNull("Group should be readable for members", test);

			testId = test.getProperty(AbstractNode.id);
			testType = test.getProperty(AbstractNode.typeHandler);

			test.setProperty(TestOne.name, "newname");

			tx.success();

			fail("User should not be able to write an object that it doesn't own.");

		} catch (FrameworkException fex) {
			assertEquals("Invalid group permissions result", 403, fex.getStatus());
			//assertEquals("Modification of node " + testId + " with type " + testType + " by user " + user2Context.getUser(false).getProperty(AbstractNode.id) + " not permitted.", fex.getMessage());
		}

		// ################################################################################################################
		// now we grant write access to the group

		try (final Tx tx = user1App.tx()) {

			final TestOne test = app.nodeQuery(TestOne.class).getFirst();
			assertNotNull("Group should be readable for members", test);

			Group group = user1App.get(Group.class, groupId);
			test.grant(Permission.write, group);

			tx.success();

		} catch (FrameworkException t) {

			logger.warn("", t);
			fail("Unexpected exception.");
		}

		// ################################################################################################################
		// user2 should now be able to write the object

		try (final Tx tx = user2App.tx()) {

			final TestOne test = user2App.nodeQuery(TestOne.class).getFirst();
			assertNotNull("Group should be readable for members", test);

			test.setProperty(TestOne.name, "newname");

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// ################################################################################################################
		// now we remove user2 from the group

		try (final Tx tx = user1App.tx()) {

			Group group     = user1App.get(Group.class, groupId);
			User user2 = user1App.get(User.class, user2Id);

			group.removeMember(user1Context, user2);
			tx.success();

		} catch (FrameworkException t) {

			logger.warn("", t);
			fail("Unexpected exception.");
		}

		// ################################################################################################################
		// check parents of user2

		try (final Tx tx = app.tx()) {

			User user2 = app.get(User.class, user2Id);
			assertEquals("User should not have parents", 0, Iterables.count(user2.getParents()));

			tx.success();

		} catch (FrameworkException t) {

			logger.warn("", t);
			fail("Unexpected exception.");
		}

		// ################################################################################################################
		// user2 is not member of the group anymore, so
		// it should not be possible to access the object

		/*
		try (final Tx tx = app.tx()) {

			user2App = StructrApp.getInstance(SecurityContext.getInstance(app.get(User.class, user2Id), AccessMode.Backend));

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}
		*/

		try (final Tx tx = user2App.tx()) {

			final TestOne test = user2App.nodeQuery(TestOne.class).getFirst();
			assertNull("TestOne instance should not be visible to user2", test);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// ################################################################################################################
		// now add user2 to the group and remove it again in same tx

		try (final Tx tx = user1App.tx()) {

			Group group = user1App.get(Group.class, groupId);

			User user2 = user1App.get(User.class, user2Id);
			assertNotNull(user2);

			group.addMember(user1Context, user2);
			assertTrue("User should be in group", Iterables.toList(user2.getGroups()).contains(group));

			assertNotNull(user2App.nodeQuery(TestOne.class).getFirst());

			group.removeMember(user1Context, user2);
			assertFalse("User should not be in group", Iterables.toList(user2.getGroups()).contains(group));

			assertNull(user2App.nodeQuery(TestOne.class).getFirst());

			tx.success();

		} catch (FrameworkException t) {

			logger.warn("", t);
			fail("Unexpected exception.");

		}

	}

	@Test
	public void testGroupHierarchyMembershipVisibility() {

		String user1Id = null;
		String user2Id = null;
		String group1Id = null;
		String group2Id = null;

		SecurityContext user1Context = null;
		SecurityContext user2Context = null;

		// ################################################################################################################
		// create two users

		try (final Tx tx = app.tx()) {

			User user1 = createTestNode(User.class, "user1");
			user1Id = user1.getUuid();
			user1Context = SecurityContext.getInstance(user1, AccessMode.Backend);

			User user2 = createTestNode(User.class, "user2");
			user2Id = user2.getUuid();
			user2Context = SecurityContext.getInstance(user2, AccessMode.Backend);

			// Grant user1 read permission on user2
			user2.grant(Permission.read, user1);

			tx.success();

		} catch (FrameworkException t) {

			logger.warn("", t);
			fail("Unexpected exception.");
		}

		final App user1App = StructrApp.getInstance(user1Context);

		// ################################################################################################################
		// create a group tree and a test object that becomes accessible for the second user by membership
		// in group2 which is member of group1

		try (final Tx tx = user1App.tx()) {

			Group group1 = user1App.create(Group.class, "group1");
			group1Id = group1.getUuid();

			Group group2 = user1App.create(Group.class, "group2");
			group2Id = group2.getUuid();

			group1.addMember(user1Context, group2);

			user1App.create(TestOne.class, "testone");

			User user1 = user1App.get(User.class, user1Id);
			assertEquals("Invalid group owner", user1, group1.getOwnerNode());

			tx.success();

		} catch (FrameworkException t) {

			logger.warn("", t);
			fail("Unexpected exception.");
		}

		// ################################################################################################################
		// user1 is owner of the test object
		// we now grant group1 read access to the test object

		try (final Tx tx = user1App.tx()) {

			final TestOne test = user1App.nodeQuery(TestOne.class).getFirst();

			assertNotNull(test);

			Group group1 = user1App.get(Group.class, group1Id);
			test.grant(Permission.read, group1);

			tx.success();

		} catch (FrameworkException t) {

			logger.warn("", t);
			fail("Unexpected exception.");
		}

		// ################################################################################################################
		// user2 is not yet member of any group, so
		// it should not be possible to access the object

		final App user2App = StructrApp.getInstance(user2Context);

		try (final Tx tx = user2App.tx()) {

			final TestOne test = user2App.nodeQuery(TestOne.class).getFirst();
			assertNull(test);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// ################################################################################################################
		// now we add user2 to group2

		try (final Tx tx = user1App.tx()) {

			Group group2 = user1App.get(Group.class, group2Id);

			User user2 = user1App.get(User.class, user2Id);
			assertNotNull(user2);

			group2.addMember(user1Context, user2);
			tx.success();

		} catch (FrameworkException t) {

			logger.warn("", t);
			fail("Unexpected exception.");

		}

		// ################################################################################################################
		// check parents of user2

		try (final Tx tx = user1App.tx()) {

			User user2 = user1App.get(User.class, user2Id);
			assertEquals("User should have parents", 1, Iterables.count(user2.getParents()));

			tx.success();

		} catch (FrameworkException t) {

			logger.warn("", t);
			fail("Unexpected exception.");
		}

		// ################################################################################################################
		// user2 is now member of group2, so
		// it should be possible to access the object

		try (final Tx tx = user2App.tx()) {

			final TestOne test = user2App.nodeQuery(TestOne.class).getFirst();
			assertNotNull("Group should be readable for members", test);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// ################################################################################################################
		// now remove group2 from group1

		try (final Tx tx = user1App.tx()) {

			Group group1 = user1App.get(Group.class, group1Id);
			Group group2 = user1App.get(Group.class, group2Id);

			group1.removeMember(user1Context, group2);

			assertNull(user2App.nodeQuery(TestOne.class).getFirst());

			tx.success();

		} catch (FrameworkException t) {

			logger.warn("", t);
			fail("Unexpected exception.");

		}
	}

	@Test
	public void testGroupVisibilityForMembers() {

		String user1Id = null;
		String user2Id = null;
		String groupId  = null;

		SecurityContext user1Context = null;
		SecurityContext user2Context = null;

		// ################################################################################################################
		// create two users

		try (final Tx tx = app.tx()) {

			User user1 = createTestNode(User.class, "user1");
			user1Id = user1.getUuid();
			user1Context = SecurityContext.getInstance(user1, AccessMode.Backend);

			User user2 = createTestNode(User.class, "user2");
			user2Id = user2.getUuid();
			user2Context = SecurityContext.getInstance(user2, AccessMode.Backend);

			// Grant user1 read permissions on user2
			user2.grant(Permission.read, user1);

			tx.success();

		} catch (FrameworkException t) {

			logger.warn("", t);
			fail("Unexpected exception.");
		}

		final App user1App = StructrApp.getInstance(user1Context);
		final App user2App = StructrApp.getInstance(user2Context);

		// ################################################################################################################
		// create a group and add the second user to that group

		try (final Tx tx = user1App.tx()) {

			Group group = user1App.create(Group.class, "group");
			User user1 = user1App.get(User.class, user1Id);
			assertNotNull("User should be readable", user1);

			assertEquals("Invalid group owner", user1, group.getOwnerNode());

			User user2 = user1App.get(User.class, user2Id);
			assertNotNull("User should be readable", user2);

			// add user2 to group
			group.addMember(user1Context, user2);

			tx.success();

		} catch (FrameworkException t) {

			logger.warn("", t);
			fail("Unexpected exception.");
		}


		// ################################################################################################################
		// test read access to group

		try (final Tx tx = user2App.tx()) {

			final Group testGroup = user2App.nodeQuery(Group.class).andName("group").getFirst();

			assertNotNull("Group should be readable for members", testGroup);
			assertEquals("Group name should be readable for members", "group", testGroup.getName());

			tx.success();

		} catch (FrameworkException t) {

			logger.warn("", t);
			fail("Unexpected exception.");
		}

		// ################################################################################################################
		// test write access to group, expected result: 403 Modification of node {id} with type {type} not permitted.
		String testId = "";
		String testType = "";
		try (final Tx tx = user2App.tx()) {

			final Group testGroup = user2App.nodeQuery(Group.class).andName("group").getFirst();

			assertNotNull("Group should be readable for members", testGroup);
			assertEquals("Group name should be readable for members", "group", testGroup.getName());

			testId = testGroup.getProperty(AbstractNode.id);
			testType = testGroup.getProperty(AbstractNode.typeHandler);

			testGroup.setProperty(Group.name, "dontchangeme");

			fail("Group name should not be writable for members");

			tx.success();

		} catch (FrameworkException t) {

			assertEquals(403, t.getStatus());
			//assertEquals("Modification of node " + testId + " with type " + testType + " by user " + user2Context.getUser(false).getProperty(AbstractNode.id) + " not permitted.", t.getMessage());
		}

	}

	@Test
	public void test00CreatePrincipal() {

		final Class type                = StructrApp.getConfiguration().getNodeEntityClass("User");
		final PropertyKey<String> eMail = StructrApp.key(type, "eMail");
		User user1                 = null;

		try (final Tx tx = app.tx()) {

			List<User> users = createTestNodes(type, 1);
			user1 = (User) users.get(0);
			user1.setProperty(AbstractNode.name, "user1");

			tx.success();

		} catch (FrameworkException ex) {
			logger.error(ex.toString());
		}

		try (final Tx tx = app.tx()) {

			List<User> users = createTestNodes(type, 1);
			final User invalidUser = (User) users.get(0);
			invalidUser.setProperty(User.name , "tester");
			invalidUser.setProperty(eMail, "invalid");

			tx.success();

			fail("Invalid e-mail address should have thrown an exception.");

		} catch (FrameworkException ex) {

			final ErrorToken token = ex.getErrorBuffer().getErrorTokens().get(0);

			assertEquals("Invalid error code", 422, ex.getStatus());
			assertEquals("Invalid error code", "User", token.getType());
			assertEquals("Invalid error code", "eMail", token.getProperty());
			assertEquals("Invalid error code", "must_contain_at_character", token.getToken());
			assertEquals("Invalid error code", "invalid", token.getDetail());

		}

		// Switch user context to user1
		final App user1App = StructrApp.getInstance(SecurityContext.getInstance(user1, AccessMode.Frontend));
		try (final Tx tx = user1App.tx()) {

			final User user2 = user1App.create(User.class);

			assertNotNull(user2);

		} catch (FrameworkException ex) {
			logger.error(ex.toString());
			fail("Unexpected exception: " + ex.toString());
		}
	}

	@Test
	public void test01SetOwner() {

		try {

			User user1 = null;
			User user2 = null;
			TestOne t1 = null;
			Class type = TestOne.class;

			try (final Tx tx = app.tx()) {

				List<User> users = createTestNodes(User.class, 2);
				user1 = (User) users.get(0);
				user1.setProperty(AbstractNode.name, "user1");

				user2 = (User) users.get(1);
				user2.setProperty(AbstractNode.name, "user2");

				t1 = createTestNode(TestOne.class);

				t1.setProperty(AbstractNode.owner, user1);

				tx.success();

			} catch (FrameworkException ex) {
				logger.error(ex.toString());
			}

			try (final Tx tx = app.tx()) {

				assertEquals(user1, t1.getProperty(AbstractNode.owner));

				// Switch user context to user1
				final App user1App = StructrApp.getInstance(SecurityContext.getInstance(user1, AccessMode.Backend));

				// Check if user1 can see t1
				assertEquals(t1, user1App.nodeQuery(type).getFirst());
			}

			try (final Tx tx = app.tx()) {

				// As superuser, make another user the owner
				t1.setProperty(AbstractNode.owner, user2);

				tx.success();

			} catch (FrameworkException ex) {
				logger.error(ex.toString());
			}

			try (final Tx tx = app.tx()) {

				// Switch user context to user2
				final App user2App = StructrApp.getInstance(SecurityContext.getInstance(user2, AccessMode.Backend));

				// Check if user2 can see t1
				assertEquals(t1, user2App.nodeQuery(type).getFirst());

				// Check if user2 is owner of t1
				assertEquals(user2, t1.getProperty(AbstractNode.owner));
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test02SetDifferentPrincipalTypesAsOwner() {

		try (final Tx tx = app.tx()) {

			final List<User> users = createTestNodes(User.class, 2);
			final User user1       = (User) users.get(0);
			final Group group1         = createTestNode(Group.class, "test group");
			final TestOne t1           = createTestNode(TestOne.class);

			t1.setProperty(AbstractNode.owner, user1);
			t1.setProperty(AbstractNode.owner, group1);
			assertEquals(group1, t1.getProperty(AbstractNode.owner));

			Ownership ownerRel = t1.getIncomingRelationship(PrincipalOwnsNode.class);
			assertNotNull(ownerRel);

			// Do additional low-level check here to ensure cardinality!
			List<Relationship> incomingRels = Iterables.toList(t1.getNode().getRelationships(Direction.INCOMING, new PrincipalOwnsNode()));
			assertEquals(1, incomingRels.size());

			tx.success();

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}
	}

	@Test
	public void test09PrivilegeEscalation() {

		// remove auto-generated resource access objects
		clearResourceAccess();

		try {

			final Class principalType = StructrApp.getConfiguration().getNodeEntityClass("User");

			User nonAdmin = (User)createTestNode(principalType);

			final PropertyKey<Boolean> isAdminKey = StructrApp.key(principalType, "isAdmin");
			final SecurityContext userContext     = SecurityContext.getInstance(nonAdmin, AccessMode.Frontend);

			nonAdmin.setSecurityContext(userContext);
			App userApp = StructrApp.getInstance(userContext);

			try (final Tx tx = userApp.tx()) {

				assertFalse(nonAdmin.isAdmin());

				nonAdmin.setProperty(isAdminKey, true);

				fail("Privilege escalation using setProperty()-method! Non-admin may not set an admin flag!");

				tx.success();

			} catch (FrameworkException ex) {

				//assertFalse("Privilege escalation using setProperty()-method! Non-admin may not set an admin flag!", nonAdmin.isAdmin());
			}

			try (final Tx tx = userApp.tx()) {

				assertFalse(nonAdmin.isAdmin());

				PropertyMap props = new PropertyMap();
				props.put(isAdminKey, true);
				nonAdmin.setProperties(userContext, props);

				fail("Privilege escalation using setProperties()-method! Non-admin may not set an admin flag!");

				tx.success();

			} catch (FrameworkException ex) {

				//assertFalse("Privilege escalation using setProperties()-method! Non-admin may not set an admin flag!", nonAdmin.isAdmin());

			}

		} catch (FrameworkException ex) {

			fail("Unexpected Exception");
		}
	}

	@Test
	public void test10LowercaseEMail() {

		final Class type                = StructrApp.getConfiguration().getNodeEntityClass("User");
		final PropertyKey<String> eMail = StructrApp.key(type, "eMail");
		User user1                 = null;

		try (final Tx tx = app.tx()) {

			user1 = (User)createTestNode(type);

			user1.setProperty(AbstractNode.name, "user1");
			user1.setProperty(eMail, "LOWERCASE@TEST.com");

			tx.success();

		} catch (FrameworkException ex) {
			logger.error(ex.toString());
		}

		try (final Tx tx = app.tx()) {

			assertEquals("EMail address was not converted to lowercase", "lowercase@test.com", user1.getProperty(eMail));
			tx.success();

		} catch (FrameworkException ex) {
			logger.error(ex.toString());
		}

	}

	/**
	 * Test whether users are allowed to add themselves to a group they don't have write access for (they shouldn't).
	 *
	 * This method uses the {@link Group#addMember} method.
	 */
	@Test
	public void test11GroupMembership() {

		// remove auto-generated resource access objects
		clearResourceAccess();

		User user1 = null;
		User user2 = null;

		// ################################################################################################################
		// create three users

		try (final Tx tx = app.tx()) {

			user1 = createTestNode(User.class, "user1");
			user2 = createTestNode(User.class, "user2");

			tx.success();

		} catch (FrameworkException t) {

			logger.warn("", t);
			fail("Unexpected exception.");
		}

		assertNotNull(user1);
		assertNotNull(user2);

		final SecurityContext user1Context = SecurityContext.getInstance(user1, AccessMode.Backend);
		final App user1App                 = StructrApp.getInstance(user1Context);

		final SecurityContext user2Context = SecurityContext.getInstance(user2, AccessMode.Backend);
		final App user2App                 = StructrApp.getInstance(user2Context);

		String groupId = null;

		// ################################################################################################################
		// Let user2 create a group and grant read permissions to user1

		try (final Tx tx = user2App.tx()) {

			Group group = user2App.create(Group.class, "group");

			assertEquals("Invalid group owner", user2, group.getOwnerNode());

			group.grant(Permission.read, user1);

			groupId = group.getUuid();

			tx.success();

		} catch (FrameworkException t) {

			logger.warn("", t);
			fail("Unexpected exception.");
		}

		// ################################################################################################################
		// Let user1 try to add itself to group

		try (final Tx tx = user1App.tx()) {

			Group group = user1App.get(Group.class, groupId);

			assertNotNull(group);

			// Add user1 to group
			group.addMember(user1Context, user1);

			tx.success();

			fail("Expected a 'Modification not permitted' FrameworkException");

		} catch (FrameworkException t) {

		}


		// ################################################################################################################
		// As admin, grant write access on user1 to user2 so user2 can modify user1

		try (final Tx tx = app.tx()) {

			Group group = app.get(Group.class, groupId);

			// Grant write permission on group to user1
			group.grant(Permission.write, user1);

			tx.success();

		} catch (FrameworkException t) {

			logger.warn("", t);
			fail("Unexpected exception.");
		}


		// ################################################################################################################
		// Try again

		try (final Tx tx = user1App.tx()) {

			Group group = user1App.get(Group.class, groupId);

			// Add user1 to group
			group.addMember(user1Context, user1);

			assertEquals(user1, Iterables.toList(group.getMembers()).get(0));

			tx.success();

		} catch (FrameworkException t) {

			logger.warn("", t);
			fail("Unexpected exception.");

		}
	}

	@Test
	public void testGroupPermissions() {

		User deleter = null;

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.getType("User");

			type.addStringProperty("test");
			type.addMethod("onModification", "set_privileged(this, 'test', now)");

			StructrSchema.replaceDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");

		}

		try (final Tx tx = app.tx()) {

			final Group read        = app.create(Group.class, "READ");
			final Group write       = app.create(Group.class, "WRITE");
			final Group delete      = app.create(Group.class, "DELETE");
			final User owner   = app.create(User.class, "owner");

			deleter = app.create(User.class, "deleter");

			// create object with "owner" as owner
			final MailTemplate test = StructrApp.getInstance(SecurityContext.getInstance(owner, AccessMode.Backend)).create(MailTemplate.class, "testobject");

			test.grant(Permission.read,   read);
			test.grant(Permission.write,  write);
			test.grant(Permission.delete, delete);

			test.grant(Permission.delete, deleter);

			read.addMember(securityContext, deleter);

			tx.success();

		} catch (FrameworkException t) {

			t.printStackTrace();
			fail("Unexpected exception.");

		}

		final App deleterApp = StructrApp.getInstance(SecurityContext.getInstance(deleter, AccessMode.Backend));

		try (final Tx tx = deleterApp.tx()) {

			final MailTemplate test = deleterApp.nodeQuery(MailTemplate.class).getFirst();

			assertNotNull("Test object should be visible to user", test);

			deleterApp.delete(test);

			tx.success();

		} catch (FrameworkException t) {

			t.printStackTrace();
			fail("Unexpected exception.");

		}
	}

	@Test
	public void testSchemaBasedVisibilityFlags() {

		// setup 1 - schema type
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			// add test type
			schema.addType("Public").setVisibleForPublicUsers();
			schema.addType("Authenticated").setVisibleForAuthenticatedUsers();
			schema.addType("Both").setVisibleForAuthenticatedUsers().setVisibleForPublicUsers();

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		final Class anonClass = StructrApp.getConfiguration().getNodeEntityClass("Public");
		final Class authClass = StructrApp.getConfiguration().getNodeEntityClass("Authenticated");
		final Class bothClass = StructrApp.getConfiguration().getNodeEntityClass("Both");
		User user        = null;

		// setup 2 - schema grant
		try (final Tx tx = app.tx()) {

			app.create(anonClass, "anon1");
			app.create(anonClass, "anon2");

			app.create(authClass, "auth1");
			app.create(authClass, "auth2");

			app.create(bothClass, "both1");
			app.create(bothClass, "both2");

			user = app.create(User.class,
				new NodeAttribute<>(AbstractNode.name, "user"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "password")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		final SecurityContext ctx = SecurityContext.getInstance(user, AccessMode.Backend);
		final App userApp         = StructrApp.getInstance(ctx);

		try (final Tx tx = userApp.tx()) {

			assertEquals("Schema-based visibility flags do not work as expected", 2, userApp.nodeQuery(anonClass).getAsList().size());
			assertEquals("Schema-based visibility flags do not work as expected", 2, userApp.nodeQuery(authClass).getAsList().size());
			assertEquals("Schema-based visibility flags do not work as expected", 2, userApp.nodeQuery(bothClass).getAsList().size());

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		final SecurityContext ctx2 = SecurityContext.getInstance(null, AccessMode.Frontend);
		final App anonymousApp     = StructrApp.getInstance(ctx2);

		try (final Tx tx = anonymousApp.tx()) {

			assertEquals("Schema-based visibility flags do not work as expected", 2, anonymousApp.nodeQuery(anonClass).getAsList().size());
			assertEquals("Schema-based visibility flags do not work as expected", 0, anonymousApp.nodeQuery(authClass).getAsList().size());
			assertEquals("Schema-based visibility flags do not work as expected", 2, anonymousApp.nodeQuery(bothClass).getAsList().size());

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}

	// ----- private methods -----
	public static void clearResourceAccess() {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final ResourceAccess access : app.nodeQuery(ResourceAccess.class).getAsList()) {
				app.delete(access);
			}

			tx.success();

		} catch (FrameworkException t) {

			logger.warn("Unable to clear resource access permissions", t);
		}
	}
}
