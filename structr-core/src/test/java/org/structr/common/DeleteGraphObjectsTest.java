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


package org.structr.common;

import org.structr.core.property.PropertyMap;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.Relation;
import org.structr.core.entity.TestFour;
import org.structr.core.entity.TestOne;
import org.structr.core.entity.TestThree;
import org.structr.core.entity.TestTwo;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import org.structr.core.Result;
import org.structr.core.entity.GenericNode;
import org.structr.core.entity.SixOneOneToOne;
import org.structr.core.entity.relationship.LocationRelationship;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;

//~--- classes ----------------------------------------------------------------

/**
 * Test basic delete operations with graph objects (nodes, relationships)
 *
 * All tests are executed in superuser context
 *
 * @author Axel Morgner
 */
public class DeleteGraphObjectsTest extends StructrTest {

	private static final Logger logger = Logger.getLogger(DeleteGraphObjectsTest.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void test00DbAvailable() {

		super.test00DbAvailable();

	}

	/**
	 * Test successful deletion of a node.
	 *
	 * The node shouldn't be found afterwards.
	 * Creation and deletion are executed in two different transactions.
	 *
	 */
	public void test01DeleteNode() {

		try {

			final PropertyMap props = new PropertyMap();
			String type             = "UnknownTestType";
			String name             = "GenericNode-name";

			props.put(AbstractNode.type, type);
			props.put(AbstractNode.name, name);

			app.beginTx();
			final NodeInterface node = app.create(NodeInterface.class, props);
			app.commitTx();

			assertTrue(node != null);

			String uuid = node.getUuid();
			
			app.beginTx();
			app.delete(node);
			app.commitTx();

			try {

				Result result = app.nodeQuery().uuid(uuid).getResult();
				
				assertEquals("Node should have been deleted", 0, result.size());
				
			} catch (FrameworkException fe) {}

		} catch (FrameworkException ex) {

			ex.printStackTrace();

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test01DeleteRelationship() {

		try {
			
			List<NodeInterface> nodes = this.createTestNodes(GenericNode.class, 2);

			assertNotNull(nodes);
			assertTrue(nodes.size() == 2);
			
			app.beginTx();
			RelationshipInterface rel = app.create(nodes.get(0), nodes.get(1), SixOneOneToOne.class);
			app.commitTx();
			
			assertNotNull(rel);
			
			try {
				// try to delete relationship
				rel.getRelationship().delete();
				
				fail("Should have raised an org.neo4j.graphdb.NotInTransactionException");
			} catch (org.neo4j.graphdb.NotInTransactionException e) {}
			
			// Relationship still there
			assertNotNull(rel);
			
			
			app.beginTx();
			app.delete(rel);
			app.commitTx();
			
			String uuid = rel.getUuid();
			assertNull("UUID of deleted relationship should be null", uuid);
			
		
		} catch (FrameworkException ex) {

			ex.printStackTrace();

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}
		
		
	}	
	
	/**
	 * DELETE_NONE should not trigger any delete cascade
	 */
	public void test03CascadeDeleteNone() {

		/* this test is flawed in that it expects the cascading
		 * not to take place but expects to run without an
		 * exception, but deleting one node of the two will leave
		 * the other (TestTwo) in an invalid state according
		 * to its isValid() method!
		try {

			// Create a relationship with DELETE_NONE
			AbstractRelationship rel = cascadeRel(TestOne.class, TestTwo.class, Relation.DELETE_NONE);
			AbstractNode startNode   = rel.getStartNode();
			AbstractNode endNode     = rel.getEndNode();
			final String startNodeId = startNode.getUuid();
			final String endNodeId   = endNode.getUuid();
			boolean exception        = false;

			deleteCascade(startNode);
			assertNodeNotFound(startNodeId);
			assertNodeExists(endNodeId);

			// Create another relationship with DELETE_NONE
			rel = cascadeRel(TestOne.class, TestTwo.class, Relation.DELETE_NONE);

			try {
				deleteCascade(rel.getEndNode());
				
			} catch (FrameworkException fex) {
				
				assertEquals(422, fex.getStatus());
				exception = true;
			}
			
			assertTrue("Exception should be raised", exception);
			
		} catch (FrameworkException ex) {

			ex.printStackTrace();

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}
		 */
	}

	/**
	 * DELETE_INCOMING should not trigger delete cascade from start to end node,
	 * but from end to start node
	 */
	public void test04CascadeDeleteIncoming() {

		/* this test is flawed in that it expects the cascading
		 * not to take place but expectes to run without an
		 * exception, but deleting one node of the two will leave
		 * the other (TestTwo) in an invalid state according
		 * to its isValid() method!
		try {

			// Create a relationship with DELETE_INCOMING
			AbstractRelationship rel = cascadeRel(TestOne.class, TestTwo.class, Relation.DELETE_INCOMING);
			final String startNodeId = rel.getStartNode().getUuid();
			final String endNodeId   = rel.getEndNode().getUuid();
			boolean exception        = false;

			deleteCascade(rel.getStartNode());

			// Start node should not be found after deletion
			assertNodeNotFound(startNodeId);

			// End node should be found after deletion of start node
			assertNodeExists(endNodeId);

			// Create another relationship with DELETE_INCOMING
			rel = cascadeRel(TestOne.class, TestTwo.class, Relation.DELETE_INCOMING);

			try {
				deleteCascade(rel.getEndNode());
				
			} catch (FrameworkException fex) {
				
				assertEquals(422, fex.getStatus());
				exception = true;
			}
			
			assertTrue("Exception should be raised", exception);
			
		} catch (FrameworkException ex) {

			ex.printStackTrace();
			
			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}
		*/
	}

	/**
	 * DELETE_OUTGOING should trigger delete cascade from start to end node,
	 * but not from end to start node.
	 */
	public void test05CascadeDeleteOutgoing() {

		try {

			// Create a relationship with DELETE_OUTGOING
			AbstractRelationship rel = cascadeRel(TestOne.class, TestTwo.class, Relation.SOURCE_TO_TARGET);
			final String startNodeId = rel.getSourceNode().getUuid();
			final String endNodeId   = rel.getTargetNode().getUuid();

			deleteCascade(rel.getSourceNode());

			// Start node should not be found after deletion
			assertNodeNotFound(startNodeId);

			// End node should not be found after deletion
			assertNodeNotFound(endNodeId);

			// Create another relationship with DELETE_OUTGOING
			rel = cascadeRel(TestOne.class, TestTwo.class, Relation.SOURCE_TO_TARGET);

			final String startNodeId2 = rel.getSourceNode().getUuid();
			final String endNodeId2   = rel.getTargetNode().getUuid();

			deleteCascade(rel.getTargetNode());

			// End node should not be found after deletion
			assertNodeNotFound(endNodeId2);

			// Start node should still exist deletion of end node
			assertNodeExists(startNodeId2);
		} catch (FrameworkException ex) {

			ex.printStackTrace();

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	/**
	 * DELETE_INCOMING + DELETE_OUTGOING should trigger delete cascade from start to end node
	 * and from end node to start node
	 */
	public void test06CascadeDeleteBidirectional() {

		try {

			// Create a relationship with DELETE_INCOMING
			AbstractRelationship rel = cascadeRel(TestOne.class, TestTwo.class, Relation.TARGET_TO_SOURCE | Relation.SOURCE_TO_TARGET);
			final String startNodeId = rel.getSourceNode().getUuid();
			final String endNodeId   = rel.getTargetNode().getUuid();

			deleteCascade(rel.getSourceNode());

			// Start node should not be found after deletion
			assertNodeNotFound(startNodeId);

			// End node should not be found after deletion of start node
			assertNodeNotFound(endNodeId);

			// Create a relationship with DELETE_INCOMING
			rel = cascadeRel(TestOne.class, TestTwo.class, Relation.TARGET_TO_SOURCE | Relation.SOURCE_TO_TARGET);

			final String startNodeId2 = rel.getSourceNode().getUuid();
			final String endNodeId2   = rel.getTargetNode().getUuid();

			deleteCascade(rel.getTargetNode());

			// End node should not be found after deletion
			assertNodeNotFound(endNodeId2);

			// Start node should not be found after deletion of end node
			assertNodeNotFound(startNodeId2);
		} catch (FrameworkException ex) {

			ex.printStackTrace();

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	/**
	 * DELETE_IF_CONSTRAINT_WOULD_BE_VIOLATED should
	 * trigger delete cascade from start to end node only
	 * if the remote node would not be valid afterwards
	 */
	public void test07CascadeDeleteConditional() {

		try {

			AbstractRelationship rel = cascadeRel(TestOne.class, TestTwo.class, Relation.CONSTRAINT_BASED);
			final String startNodeId = rel.getSourceNode().getUuid();
			final String endNodeId   = rel.getTargetNode().getUuid();

			deleteCascade(rel.getSourceNode());

			// Start node should be deleted
			assertNodeNotFound(startNodeId);

			// End node should be deleted
			assertNodeNotFound(endNodeId);

			rel = cascadeRel(TestOne.class, TestThree.class, Relation.CONSTRAINT_BASED);

			final String startNodeId2 = rel.getSourceNode().getUuid();
			final String endNodeId2   = rel.getTargetNode().getUuid();

			deleteCascade(rel.getSourceNode());

			// Start node should be deleted
			assertNodeNotFound(startNodeId2);

			// End node should still be there
			assertNodeExists(endNodeId2);

			rel = cascadeRel(TestOne.class, TestFour.class, Relation.CONSTRAINT_BASED);

			final String startNodeId3 = rel.getSourceNode().getUuid();
			final String endNodeId3   = rel.getTargetNode().getUuid();

			deleteCascade(rel.getSourceNode());

			// Start node should be deleted
			assertNodeNotFound(startNodeId3);

			// End node should still be there
			assertNodeExists(endNodeId3);

		} catch (FrameworkException ex) {

			ex.printStackTrace();

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	private AbstractRelationship cascadeRel(final Class type1, final Class type2, final int cascadeDeleteFlag) throws FrameworkException {

		app.beginTx();

		AbstractNode start       = createTestNode(type1);
		AbstractNode end         = createTestNode(type2);
		AbstractRelationship rel = createTestRelationship(start, end, LocationRelationship.class);

		rel.setProperty(AbstractRelationship.cascadeDelete, cascadeDeleteFlag);
		
		app.commitTx();
		
		return rel;
	}

	private void deleteCascade(final NodeInterface node) throws FrameworkException {

		app.beginTx();
		app.delete(node);
		app.commitTx();
	}
}
