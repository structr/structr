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
/*
*  Copyright (C) 2010-2013 Axel Morgner
*
*  This file is part of structr <http://structr.org>.
*
*  structr is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, either version 3 of the License, or
*  (at your option) any later version.
*
*  structr is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU General Public License for more details.
*
*  You should have received a copy of the GNU General Public License
*  along with structr.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.structr.common;

import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.TestOne;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.Iterables;
import org.structr.core.Ownership;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Group;
import org.structr.core.entity.TestUser;
import org.structr.core.entity.relationship.PrincipalOwnsNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;

//~--- classes ----------------------------------------------------------------

/**
 * Test setting ownership.
 *
 * @author Axel Morgner
 */
public class OwnerTest extends StructrTest {

	private static final Logger logger = Logger.getLogger(OwnerTest.class.getName());
	
	//~--- methods --------------------------------------------------------

	@Override
	public void test00DbAvailable() {

		super.test00DbAvailable();

	}

	public void test01SetOwner() {

		try {
		
			TestUser user1 = null;
			TestUser user2 = null;
			TestOne t1 = null;
			Class type = TestOne.class;

			final App superUserApp = StructrApp.getInstance();
			try (final Tx tx = app.tx()) {

				List<TestUser> users = createTestNodes(TestUser.class, 2);
				user1 = (TestUser) users.get(0);
				user1.setProperty(AbstractNode.name, "user1");
				
				user2 = (TestUser) users.get(1);
				user2.setProperty(AbstractNode.name, "user2");

				t1 = createTestNode(TestOne.class);

				t1.setProperty(AbstractNode.owner, user1);

				tx.success();

			} catch (FrameworkException ex) {
				logger.log(Level.SEVERE, ex.toString());
			}

			try (final Tx tx = app.tx()) {
				
				assertEquals(user1, t1.getProperty(AbstractNode.owner));

				// Switch user context to user1
				final App user1App = StructrApp.getInstance(SecurityContext.getInstance(user1, AccessMode.Backend));

				// Check if user1 can see t1
				assertEquals(t1, user1App.nodeQuery(type, false).getFirst());
			}
			
			try (final Tx tx = app.tx()) {

				// As superuser, make another user the owner
				t1.setProperty(AbstractNode.owner, user2);

				tx.success();
				
			} catch (FrameworkException ex) {
				logger.log(Level.SEVERE, ex.toString());
			}
			
			try (final Tx tx = app.tx()) {
				
				// Switch user context to user2
				final App user2App = StructrApp.getInstance(SecurityContext.getInstance(user2, AccessMode.Backend));

				// Check if user2 can see t1
				assertEquals(t1, user2App.nodeQuery(type, false).getFirst());

				// Check if user2 is owner of t1
				assertEquals(user2, t1.getProperty(AbstractNode.owner));
			}
			
		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	/**
	 * This test fails due to the removal of the interface check in
	 * {@link AbstractRelationProperty#ensureManyToOne} and {@link AbstractRelationProperty#ensureOneToMany}
	 * 
	 * With Structr 1.0, this has to be resolved!!
	 */
	public void test02SetDifferentPrincipalTypesAsOwner() {

		final App superUserApp = StructrApp.getInstance();
		
		try (final Tx tx = app.tx()) {

			List<TestUser> users = createTestNodes(TestUser.class, 2);
			TestUser user1 = (TestUser) users.get(0);

			List<Group> groups = createTestNodes(Group.class, 1);
			Group group1 = (Group) groups.get(0);
			
			TestOne t1 = createTestNode(TestOne.class);
			
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

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");
		}
	}
	
	
}
