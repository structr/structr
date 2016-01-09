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

import java.util.LinkedList;
import java.util.List;
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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.Result;
import org.structr.core.entity.SixOneOneToOne;
import org.structr.core.entity.TestSix;
import org.structr.core.entity.TestTen;
import org.structr.core.entity.relationship.NodeHasLocation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;

//~--- classes ----------------------------------------------------------------

/**
 * Test basic delete operations with graph objects (nodes, relationships)
 *
 * All tests are executed in superuser context
 *
 *
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
			final String type       = "UnknownTestType";
			final String name       = "GenericNode-name";
			NodeInterface node      = null;
			String uuid             = null;

			props.put(AbstractNode.type, type);
			props.put(AbstractNode.name, name);

			try (final Tx tx = app.tx()) {

				node = app.create(NodeInterface.class, props);
				tx.success();
			}

			assertTrue(node != null);

			try (final Tx tx = app.tx()) {
				uuid = node.getUuid();
			}

			try (final Tx tx = app.tx()) {

				app.delete(node);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				Result result = app.nodeQuery(NodeInterface.class).uuid(uuid).getResult();

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

			final TestOne testOne  = createTestNode(TestOne.class);
			final TestSix testSix  = createTestNode(TestSix.class);
			SixOneOneToOne rel     = null;

			assertNotNull(testOne);
			assertNotNull(testSix);

			try (final Tx tx = app.tx()) {

				rel = app.create(testSix, testOne, SixOneOneToOne.class);
				tx.success();
			}

			assertNotNull(rel);

			try {
				// try to delete relationship
				rel.getRelationship().delete();

				fail("Should have raised an org.neo4j.graphdb.NotInTransactionException");
			} catch (org.neo4j.graphdb.NotInTransactionException e) {}

			// Relationship still there
			assertNotNull(rel);

			try (final Tx tx = app.tx()) {

				app.delete(rel);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				String uuid = rel.getUuid();
				fail("Deleted entity should have thrown an exception on access.");

			} catch (IllegalStateException iex) {
			}


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
			NodeInterface sourceNode;
			NodeInterface targetNode;
			String startNodeId;
			String endNodeId;

			try (final Tx tx = app.tx()) {

				startNodeId = rel.getSourceNode().getUuid();
				endNodeId   = rel.getTargetNode().getUuid();
				sourceNode  = rel.getSourceNode();
			}

			deleteCascade(sourceNode);

			try (final Tx tx = app.tx()) {

				// Start node should not be found after deletion
				assertNodeNotFound(startNodeId);

				// End node should not be found after deletion
				assertNodeNotFound(endNodeId);
			}

			// Create another relationship with DELETE_OUTGOING
			rel = cascadeRel(TestOne.class, TestTwo.class, Relation.SOURCE_TO_TARGET);

			try (final Tx tx = app.tx()) {

				startNodeId = rel.getSourceNode().getUuid();
				endNodeId   = rel.getTargetNode().getUuid();
				targetNode  = rel.getTargetNode();
			}

			deleteCascade(targetNode);

			try (final Tx tx = app.tx()) {

				// End node should not be found after deletion
				assertNodeNotFound(endNodeId);

				// Start node should still exist deletion of end node
				assertNodeExists(startNodeId);
			}

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
			NodeInterface sourceNode;
			NodeInterface targetNode;
			String startNodeId;
			String endNodeId;

			try (final Tx tx = app.tx()) {

				startNodeId = rel.getSourceNode().getUuid();
				endNodeId   = rel.getTargetNode().getUuid();
				sourceNode  = rel.getSourceNode();
			}

			deleteCascade(sourceNode);

			try (final Tx tx = app.tx()) {

				// Start node should not be found after deletion
				assertNodeNotFound(startNodeId);

				// End node should not be found after deletion of start node
				assertNodeNotFound(endNodeId);
			}

			// Create a relationship with DELETE_INCOMING
			rel = cascadeRel(TestOne.class, TestTwo.class, Relation.TARGET_TO_SOURCE | Relation.SOURCE_TO_TARGET);

			try (final Tx tx = app.tx()) {

				startNodeId = rel.getSourceNode().getUuid();
				endNodeId   = rel.getTargetNode().getUuid();
				targetNode  = rel.getTargetNode();
			}

			deleteCascade(targetNode);

			try (final Tx tx = app.tx()) {

				// End node should not be found after deletion
				assertNodeNotFound(endNodeId);

				// Start node should not be found after deletion of end node
				assertNodeNotFound(startNodeId);
			}

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
			NodeInterface sourceNode;
			String startNodeId;
			String endNodeId;

			try (final Tx tx = app.tx()) {

				startNodeId = rel.getSourceNode().getUuid();
				endNodeId   = rel.getTargetNode().getUuid();
				sourceNode  = rel.getSourceNode();
			}

			deleteCascade(sourceNode);

			try (final Tx tx = app.tx()) {

				// Start node should be deleted
				assertNodeNotFound(startNodeId);

				// End node should be deleted
				assertNodeNotFound(endNodeId);
			}

			rel = cascadeRel(TestOne.class, TestThree.class, Relation.CONSTRAINT_BASED);

			try (final Tx tx = app.tx()) {

				startNodeId = rel.getSourceNode().getUuid();
				endNodeId   = rel.getTargetNode().getUuid();
				sourceNode   = rel.getSourceNode();
			}

			deleteCascade(sourceNode);

			try (final Tx tx = app.tx()) {

				// Start node should be deleted
				assertNodeNotFound(startNodeId);

				// End node should still be there
				assertNodeExists(endNodeId);
			}

			rel = cascadeRel(TestOne.class, TestFour.class, Relation.CONSTRAINT_BASED);

			try (final Tx tx = app.tx()) {

				startNodeId = rel.getSourceNode().getUuid();
				endNodeId   = rel.getTargetNode().getUuid();
				sourceNode   = rel.getSourceNode();
			}

			deleteCascade(sourceNode);

			try (final Tx tx = app.tx()) {

				// Start node should be deleted
				assertNodeNotFound(startNodeId);

				// End node should still be there
				assertNodeExists(endNodeId);
			}

		} catch (FrameworkException ex) {

			ex.printStackTrace();

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test08OverlappingDeleteCascades() {

		/*
		 * This test creates a ternary tree of depth 2 (39 nodes)
		 * linked with relationship type "TEN", with two additional
		 * links throughout the tree. It creates a situation where
		 * two delete cascades overlap when a node is deleted and
		 * tests the correct handling of such a situation.
		 *
		 *           1-------+       1->2, 1->3, 1->4, 1->D
		 *         / | \     |
		 *        /  |  \    |
		 *       2   3   4  /
		 *      /|\ /|\ /|\/
		 *      567 89A BCD
		 */

		try {

			final List<TestTen> rootNodes        = new LinkedList<>();
			final List<TestTen> allChildren      = new LinkedList<>();
			final List<TestTen> allGrandChildren = new LinkedList<>();

			try (final Tx tx = app.tx()) {

				// create some nodes..
				rootNodes.addAll(createTestNodes(TestTen.class, 3));

				for (final TestTen node : rootNodes) {

					final List<TestTen> children = createTestNodes(TestTen.class, 3);
					node.setProperty(TestTen.tenTenChildren, children);

					for (final TestTen child : children) {

						final List<TestTen> grandChildren = createTestNodes(TestTen.class, 3);
						child.setProperty(TestTen.tenTenChildren, grandChildren);

						allGrandChildren.addAll(grandChildren);
					}

					allChildren.addAll(children);
				}

				// create some additional links off a different type but with cascading delete
				rootNodes.get(0).setProperty(TestTen.testChild,   allGrandChildren.get(0));
				allChildren.get(0).setProperty(TestTen.testChild, allGrandChildren.get(1));

				tx.success();
			}

			// check preconditions: exactly 39 nodes should exist
			try (final Tx tx = app.tx()) {

				assertEquals("Wrong number of nodes", 39, app.nodeQuery(TestTen.class).getAsList().size());
				tx.success();
			}

			// delete one root node
			try (final Tx tx = app.tx()) {

				app.delete(rootNodes.get(0));
				tx.success();
			}

			// check conditions after deletion, 26 nodes shoud exist
			try (final Tx tx = app.tx()) {

				assertEquals("Wrong number of nodes", 26, app.nodeQuery(TestTen.class).getAsList().size());
				tx.success();
			}


		} catch (FrameworkException ex) {

			ex.printStackTrace();

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	private AbstractRelationship cascadeRel(final Class type1, final Class type2, final int cascadeDeleteFlag) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			AbstractNode start       = createTestNode(type1);
			AbstractNode end         = createTestNode(type2);
			AbstractRelationship rel = createTestRelationship(start, end, NodeHasLocation.class);

			rel.setProperty(AbstractRelationship.cascadeDelete, cascadeDeleteFlag);

			tx.success();

			return rel;
		}
	}

	private void deleteCascade(final NodeInterface node) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			app.delete(node);
			tx.success();
		}
	}
}
