/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.common;

import java.util.List;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.entity.TestOne;
import org.structr.core.entity.TestUser;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

//~--- classes ----------------------------------------------------------------

/**
 * Test access control with different permission levels.
 *
 *
 */
public class AccessControlTest extends StructrTest {

	private static final Logger logger = Logger.getLogger(AccessControlTest.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void test00DbAvailable() {

		super.test00DbAvailable();

	}

	public void test01PublicAccessToNonPublicNode() {

		// remove auto-generated resource access objects
		clearResourceAccess();

		try {

			List<TestUser> users = createTestNodes(TestUser.class, 1);
			TestUser user = (TestUser) users.get(0);

			// Create node with user context
			Class type = TestOne.class;
			TestOne t1 = createTestNode(TestOne.class, user);

			SecurityContext publicContext = SecurityContext.getInstance(null, AccessMode.Frontend);

			try (final Tx tx = app.tx()) {

				Result result = StructrApp.getInstance(publicContext).nodeQuery(type).getResult();

				// Node should not be visible in public context (no user logged in)
				assertTrue(result.isEmpty());
			}

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			fail("Unexpected exception");

		}

	}


	public void test02PublicAccessToPublicNode() {

		// remove auto-generated resource access objects
		clearResourceAccess();

		try {

			List<TestUser> users = createTestNodes(TestUser.class, 1);
			TestUser user = (TestUser) users.get(0);

			PropertyMap props = new PropertyMap();
			props.put(AbstractNode.visibleToPublicUsers, true);

			// Create two nodes with user context, one of them is visible to public users
			Class type = TestOne.class;
			TestOne t1 = createTestNode(TestOne.class, props, user);
			TestOne t2 = createTestNode(TestOne.class, user);

			SecurityContext publicContext = SecurityContext.getInstance(null, AccessMode.Frontend);

			try (final Tx tx = app.tx()) {

				Result result = StructrApp.getInstance(publicContext).nodeQuery(type).getResult();

				assertEquals(1, result.size());
				assertEquals(t1.getUuid(), result.get(0).getUuid());
			}

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			fail("Unexpected exception");

		}

	}

	public void test03PublicAccessToProtectedNode() {

		// remove auto-generated resource access objects
		clearResourceAccess();

		try {

			List<TestUser> users = createTestNodes(TestUser.class, 1);
			TestUser user = (TestUser) users.get(0);

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
				Result result = StructrApp.getInstance(publicContext).nodeQuery(type).getResult();

				assertEquals(1, result.size());
				assertEquals(t1.getUuid(), result.get(0).getUuid());
			}

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			fail("Unexpected exception");

		}

	}

	public void test04BackendUserAccessToProtectedNode() {

		// remove auto-generated resource access objects
		clearResourceAccess();

		try {

			List<TestUser> users = createTestNodes(TestUser.class, 2);
			TestUser user1 = (TestUser) users.get(0);
			TestUser user2 = (TestUser) users.get(1);

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
				Result result = StructrApp.getInstance(user2Context).nodeQuery(type).getResult();

				assertEquals(2, result.size());
			}

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			fail("Unexpected exception");

		}

	}

	public void test05FrontendUserAccessToProtectedNode() {

		// remove auto-generated resource access objects
		clearResourceAccess();

		try {

			List<TestUser> users = createTestNodes(TestUser.class, 2);
			TestUser user1 = (TestUser) users.get(0);
			TestUser user2 = (TestUser) users.get(1);

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

				Result result = StructrApp.getInstance(user2Context).nodeQuery(type).getResult();

				assertEquals(2, result.size());
			}

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			fail("Unexpected exception");

		}

	}

	public void test06GrantReadPermission() {

		// remove auto-generated resource access objects
		clearResourceAccess();

		try {

			List<TestUser> users = createTestNodes(TestUser.class, 2);
			TestUser user1 = (TestUser) users.get(0);
			TestUser user2 = (TestUser) users.get(1);
			Result result = null;

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

				result = StructrApp.getInstance(user2Context).nodeQuery(type).getResult();

				assertEquals(1, result.size());
				assertEquals(t1.getUuid(), result.get(0).getUuid());
			}

			try (final Tx tx = app.tx()) {

				// Revoke permission again
				t1.revoke(Permission.read, user2);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				result = StructrApp.getInstance(user2Context).nodeQuery(type).getResult();
				assertTrue(result.isEmpty());
			}

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			fail("Unexpected exception");

		}

	}

	public void test07ResultCount() {

		// remove auto-generated resource access objects
		clearResourceAccess();

		try {

			final Class type = TestOne.class;
			final List<NodeInterface> nodes = createTestNodes(type, 10);

			try (final Tx tx = app.tx()) {
				nodes.get(3).setProperty(AbstractNode.visibleToPublicUsers, true);
				nodes.get(5).setProperty(AbstractNode.visibleToPublicUsers, true);
				nodes.get(7).setProperty(AbstractNode.visibleToPublicUsers, true);
				tx.success();
			}

			SecurityContext publicContext = SecurityContext.getInstance(null, AccessMode.Frontend);

			try (final Tx tx = app.tx()) {

				Result result = StructrApp.getInstance(publicContext).nodeQuery(type).sort(AbstractNode.createdDate).getResult();

				assertEquals(3, result.size());
				assertEquals(3, (int) result.getRawResultCount());

				// do not test order of elements
//				assertEquals(nodes.get(3).getUuid(), result.get(0).getUuid());
//				assertEquals(nodes.get(5).getUuid(), result.get(1).getUuid());
//				assertEquals(nodes.get(7).getUuid(), result.get(2).getUuid());
			}

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			fail("Unexpected exception");

		}

	}

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

				Result result = StructrApp.getInstance(publicContext).nodeQuery(type).sort(sortKey).order(sortDesc).page(page).pageSize(pageSize).getResult();

				assertEquals(2, result.size());
				assertEquals(4, (int) result.getRawResultCount());

				assertEquals(nodes.get(3).getUuid(), result.get(0).getUuid());
				assertEquals(nodes.get(5).getUuid(), result.get(1).getUuid());
			}

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			fail("Unexpected exception");

		}

	}



	public void test08WriteAccess() {

		// remove auto-generated resource access objects
		clearResourceAccess();

		try {

			final TestUser owner = createTestNode(TestUser.class);
			final TestUser user  = createTestNode(TestUser.class);

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

			ex.printStackTrace();
			fail("Unexpected exception");

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

			t.printStackTrace();
		}
	}

}
