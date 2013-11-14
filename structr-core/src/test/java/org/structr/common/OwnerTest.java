/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
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
import org.structr.core.Result;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.TestOne;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import org.structr.core.Services;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.Group;
import org.structr.core.entity.User;
import org.structr.core.graph.search.SearchNodeCommand;

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

			List<AbstractNode> users = createTestNodes(User.class, 2);
			User user1 = (User) users.get(0);
			User user2 = (User) users.get(1);
			
			Class type = TestOne.class;
			TestOne t1 = createTestNode(TestOne.class);
			
			t1.setProperty(AbstractNode.owner, user1);
			assertEquals(user1, t1.getProperty(AbstractNode.owner));

			SecurityContext user1Context = SecurityContext.getInstance(user1, AccessMode.Frontend);
			List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
			searchAttributes.add(Search.andExactTypeAndSubtypes(type));

			Result result = Services.command(user1Context, SearchNodeCommand.class).execute(searchAttributes);
			assertEquals(1, result.size());
			assertEquals(t1, result.get(0));

			// Make another user the owner
			t1.setProperty(AbstractNode.owner, user2);
			assertEquals(user2, t1.getProperty(AbstractNode.owner));
			
			// Count incoming OWNS relationships, should be always one!
			Iterable<AbstractRelationship> ownerRels = t1.getIncomingRelationships(RelType.OWNS);
			int count = 0;
			while (ownerRels.iterator().hasNext()) {
				count++;
				ownerRels.iterator().next();
			}
			assertEquals(1, count);

			SecurityContext user2Context = SecurityContext.getInstance(user2, AccessMode.Frontend);

			result = Services.command(user2Context, SearchNodeCommand.class).execute(searchAttributes);
			assertEquals(1, result.size());
			assertEquals(t1, result.get(0));
			
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
//	public void test02SetDifferentPrincipalTypesAsOwner() {
//
//		try {
//
//			List<AbstractNode> users = createTestNodes(User.class, 2);
//			User user1 = (User) users.get(0);
//
//			List<AbstractNode> groups = createTestNodes(Group.class, 1);
//			Group group1 = (Group) groups.get(0);
//			
//			Class type = TestOne.class;
//			TestOne t1 = createTestNode(TestOne.class);
//			
//			t1.setProperty(AbstractNode.owner, user1);
//			t1.setProperty(AbstractNode.owner, group1);
//			assertEquals(group1, t1.getProperty(AbstractNode.owner));
//			
//			// Count incoming OWNS relationships, should be always one!
//			Iterable<AbstractRelationship> ownerRels = t1.getIncomingRelationships(RelType.OWNS);
//			int count = 0;
//			while (ownerRels.iterator().hasNext()) {
//				count++;
//				ownerRels.iterator().next();
//			}
//			assertEquals(1, count);
//			
//		} catch (FrameworkException ex) {
//
//			logger.log(Level.SEVERE, ex.toString());
//			fail("Unexpected exception");
//
//		}
//
//	}
	
	
}
