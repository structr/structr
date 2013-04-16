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
import org.structr.core.graph.StructrTransaction;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import org.structr.core.Result;
import org.structr.core.TestRelType;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;

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

			final AbstractNode node = transactionCommand.execute(new StructrTransaction<AbstractNode>() {

				@Override
				public AbstractNode execute() throws FrameworkException {

					// Create node with a type which has no entity class => should result in a node of type 'GenericNode'
					return createNodeCommand.execute(props);
				}

			});

			assertTrue(node != null);

			String uuid = node.getUuid();
			
			transactionCommand.execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					deleteNodeCommand.execute(node);
					return null;
				}

			});

			try {

				// Node should not be found after deletion
				List<SearchAttribute> attrs = new LinkedList<SearchAttribute>();
				attrs.add(Search.andExactUuid(uuid));
				
				Result result = searchNodeCommand.execute(attrs);
				
				assertEquals("Node should have been deleted", 0, result.size());
				
			} catch (FrameworkException fe) {}

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test01DeleteRelationship() {

		try {
			
			List<AbstractNode> nodes        = this.createTestNodes("UnknownTestType", 2);

			assertNotNull(nodes);
			assertTrue(nodes.size() == 2);
			
			AbstractRelationship rel = createRelationshipCommand.execute(nodes.get(0), nodes.get(1), TestRelType.ONE_TO_ONE);
			
			assertNotNull(rel);
			
			try {
				// try to delete relationship
				rel.getRelationship().delete();
				
				fail("Should have raised an org.neo4j.graphdb.NotInTransactionException");
			} catch (org.neo4j.graphdb.NotInTransactionException e) {}
			
			// Relationship still there
			assertNotNull(rel);
			
			
			// Delete relationship in transaction
			// This works because structr's DeleteRelationshipCommand creates a tx 
			deleteRelationshipCommand.execute(rel);
			
			try {
				rel.getUuid();
				
				fail("Should have raised an org.neo4j.graphdb.NotFoundException");
			} catch (org.neo4j.graphdb.NotFoundException e) {}
				
			
		
		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}
		
		
	}	
	
	/**
	 * DELETE_NONE should not trigger any delete cascade
	 */
	public void test03CascadeDeleteNone() {

		try {

			// Create a relationship with DELETE_NONE
			AbstractRelationship rel = cascadeRel(TestOne.class, TestTwo.class, Relation.DELETE_NONE);
			AbstractNode startNode   = rel.getStartNode();
			AbstractNode endNode     = rel.getEndNode();
			final String startNodeId = startNode.getUuid();
			final String endNodeId   = endNode.getUuid();

			deleteCascade(startNode);
			assertNodeNotFound(startNodeId);
			assertNodeExists(endNodeId);

			// Create another relationship with DELETE_NONE
			rel = cascadeRel(TestOne.class, TestTwo.class, Relation.DELETE_NONE);

			final String startNodeId2 = rel.getStartNode().getUuid();
			final String endNodeId2   = rel.getEndNode().getUuid();

			deleteCascade(rel.getEndNode());
			assertNodeNotFound(endNodeId2);
			assertNodeExists(startNodeId2);
		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	/**
	 * DELETE_INCOMING should not trigger delete cascade from start to end node,
	 * but from end to start node
	 */
	public void test04CascadeDeleteIncoming() {

		try {

			// Create a relationship with DELETE_INCOMING
			AbstractRelationship rel = cascadeRel(TestOne.class, TestTwo.class, Relation.DELETE_INCOMING);
			final String startNodeId = rel.getStartNode().getUuid();
			final String endNodeId   = rel.getEndNode().getUuid();

			deleteCascade(rel.getStartNode());

			// Start node should not be found after deletion
			assertNodeNotFound(startNodeId);

			// End node should be found after deletion of start node
			assertNodeExists(endNodeId);

			// Create another relationship with DELETE_INCOMING
			rel = cascadeRel(TestOne.class, TestTwo.class, Relation.DELETE_INCOMING);

			final String startNodeId2 = rel.getStartNode().getUuid();
			final String endNodeId2   = rel.getEndNode().getUuid();

			deleteCascade(rel.getEndNode());

			// End node should not be found after deletion
			assertNodeNotFound(endNodeId2);

			// Start node should not be found after deletion of end node
			assertNodeNotFound(startNodeId2);
		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	/**
	 * DELETE_OUTGOING should trigger delete cascade from start to end node,
	 * but not from end to start node.
	 */
	public void test05CascadeDeleteOutgoing() {

		try {

			// Create a relationship with DELETE_OUTGOING
			AbstractRelationship rel = cascadeRel(TestOne.class, TestTwo.class, Relation.DELETE_OUTGOING);
			final String startNodeId = rel.getStartNode().getUuid();
			final String endNodeId   = rel.getEndNode().getUuid();

			deleteCascade(rel.getStartNode());

			// Start node should not be found after deletion
			assertNodeNotFound(startNodeId);

			// End node should not be found after deletion
			assertNodeNotFound(endNodeId);

			// Create another relationship with DELETE_OUTGOING
			rel = cascadeRel(TestOne.class, TestTwo.class, Relation.DELETE_OUTGOING);

			final String startNodeId2 = rel.getStartNode().getUuid();
			final String endNodeId2   = rel.getEndNode().getUuid();

			deleteCascade(rel.getEndNode());

			// End node should not be found after deletion
			assertNodeNotFound(endNodeId2);

			// Start node should still exist deletion of end node
			assertNodeExists(startNodeId2);
		} catch (FrameworkException ex) {

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
			AbstractRelationship rel = cascadeRel(TestOne.class, TestTwo.class, Relation.DELETE_INCOMING | Relation.DELETE_OUTGOING);
			final String startNodeId = rel.getStartNode().getUuid();
			final String endNodeId   = rel.getEndNode().getUuid();

			deleteCascade(rel.getStartNode());

			// Start node should not be found after deletion
			assertNodeNotFound(startNodeId);

			// End node should not be found after deletion of start node
			assertNodeNotFound(endNodeId);

			// Create a relationship with DELETE_INCOMING
			rel = cascadeRel(TestOne.class, TestTwo.class, Relation.DELETE_INCOMING | Relation.DELETE_OUTGOING);

			final String startNodeId2 = rel.getStartNode().getUuid();
			final String endNodeId2   = rel.getEndNode().getUuid();

			deleteCascade(rel.getEndNode());

			// End node should not be found after deletion
			assertNodeNotFound(endNodeId2);

			// Start node should not be found after deletion of end node
			assertNodeNotFound(startNodeId2);
		} catch (FrameworkException ex) {

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

			AbstractRelationship rel = cascadeRel(TestOne.class, TestTwo.class, Relation.DELETE_IF_CONSTRAINT_WOULD_BE_VIOLATED);
			final String startNodeId = rel.getStartNode().getUuid();
			final String endNodeId   = rel.getEndNode().getUuid();

			deleteCascade(rel.getStartNode());

			// Start node should be deleted
			assertNodeNotFound(startNodeId);

			// End node should be deleted
			assertNodeNotFound(endNodeId);

			rel = cascadeRel(TestOne.class, TestThree.class, Relation.DELETE_IF_CONSTRAINT_WOULD_BE_VIOLATED);

			final String startNodeId2 = rel.getStartNode().getUuid();
			final String endNodeId2   = rel.getEndNode().getUuid();

			deleteCascade(rel.getStartNode());

			// Start node should be deleted
			assertNodeNotFound(startNodeId2);

			// End node should still be there
			assertNodeExists(endNodeId2);

			rel = cascadeRel(TestOne.class, TestFour.class, Relation.DELETE_IF_CONSTRAINT_WOULD_BE_VIOLATED);

			final String startNodeId3 = rel.getStartNode().getUuid();
			final String endNodeId3   = rel.getEndNode().getUuid();

			deleteCascade(rel.getStartNode());

			// Start node should be deleted
			assertNodeNotFound(startNodeId3);

			// End node should still be there
			assertNodeExists(endNodeId3);

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	private AbstractRelationship cascadeRel(final Class type1, final Class type2, final int cascadeDeleteFlag) throws FrameworkException {

		return (AbstractRelationship) transactionCommand.execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				AbstractNode start       = createTestNode(type1.getSimpleName());
				AbstractNode end         = createTestNode(type2.getSimpleName());
				AbstractRelationship rel = createTestRelationship(start, end, RelType.IS_AT);

				rel.setProperty(AbstractRelationship.cascadeDelete, cascadeDeleteFlag);

				return rel;

			}

		});

	}

	private void deleteCascade(final AbstractNode node) throws FrameworkException {

		transactionCommand.execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				deleteNodeCommand.execute(node, true);
				return null;
			}

		});

	}

}
