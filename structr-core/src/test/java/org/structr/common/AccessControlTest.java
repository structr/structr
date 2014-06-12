/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.common;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Person;
import org.structr.core.entity.Principal;
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
 * @author Axel Morgner
 */
public class AccessControlTest extends StructrTest {

	private static final Logger logger = Logger.getLogger(AccessControlTest.class.getName());
	
	//~--- methods --------------------------------------------------------

	@Override
	public void test00DbAvailable() {

		super.test00DbAvailable();

	}

	public void test01PublicAccessToNonPublicNode() {

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

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	
	public void test02PublicAccessToPublicNode() {

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

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test03PublicAccessToProtectedNode() {

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

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test04BackendUserAccessToProtectedNode() {

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

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test05FrontendUserAccessToProtectedNode() {

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

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}
		
	}

	public void test06GrantReadPermission() {

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
				user2.grant(Permission.read, t1);
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
				user2.revoke(Permission.read, t1);
				tx.success();
			}
			
			try (final Tx tx = app.tx()) {
				
				result = StructrApp.getInstance(user2Context).nodeQuery(type).getResult();
				assertTrue(result.isEmpty());
			}

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test07ResultCount() {

		try {

			final List<Person> persons = createTestNodes(Person.class, 1);
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
				
				Result result = StructrApp.getInstance(publicContext).nodeQuery(type).getResult();

				assertEquals(3, result.size());
				assertEquals(3, (int) result.getRawResultCount());

				assertEquals(nodes.get(3).getUuid(), result.get(0).getUuid());
				assertEquals(nodes.get(5).getUuid(), result.get(1).getUuid());
				assertEquals(nodes.get(7).getUuid(), result.get(2).getUuid());
			}

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}
	
	public void test07ResultCountWithPaging() {

		try {

			final List<Person> persons = createTestNodes(Person.class, 1);
			final Class type = TestOne.class;
			final List<NodeInterface> nodes = createTestNodes(type, 10);

			try (final Tx tx = app.tx()) {

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

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	protected <T extends AbstractNode> T createTestNode(final Class<T> type, final Principal user) throws FrameworkException {
		return (T)createTestNode(type, new PropertyMap(), user);
	}

	protected <T extends AbstractNode> T createTestNode(final Class<T> type, final PropertyMap props, final Principal user) throws FrameworkException {

		final App backendApp = StructrApp.getInstance(SecurityContext.getInstance(user, AccessMode.Backend));
		
		try (final Tx tx = backendApp.tx()) {
			
			final T result = backendApp.create(type, props);
			tx.success();

			return result;
		}
	}
	
	
}
