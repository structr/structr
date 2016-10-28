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

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.NotFoundException;
import org.structr.api.NotInTransactionException;
import org.structr.api.graph.RelationshipType;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.DynamicResourceAccess;
import org.structr.core.entity.GenericNode;
import org.structr.core.entity.GenericRelationship;
import org.structr.core.entity.Group;
import org.structr.core.entity.Localization;
import org.structr.core.entity.Location;
import org.structr.core.entity.MailTemplate;
import org.structr.core.entity.Person;
import org.structr.core.entity.PropertyAccess;
import org.structr.core.entity.Relation;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.entity.SixOneOneToOne;
import org.structr.core.entity.TestFour;
import org.structr.core.entity.TestNine;
import org.structr.core.entity.TestOne;
import org.structr.core.entity.TestSeven;
import org.structr.core.entity.TestSix;
import org.structr.core.entity.TestTen;
import org.structr.core.entity.TestThree;
import org.structr.core.entity.TestTwo;
import org.structr.core.entity.relationship.NodeHasLocation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;

/**
 *
 */
public class BasicTest extends StructrTest {

	private static final Logger logger = LoggerFactory.getLogger(BasicTest.class);

	@Test
	public void test00SimpleCreateOperation() {

		try (final Tx tx = app.tx()) {

			final PropertyMap properties = new PropertyMap();

			properties.put(TestSix.name, "name");

			// test null value for a 1:1 related property
			properties.put(TestSix.oneToOneTestThree, null);

			app.create(TestSix.class, properties);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			final TestSix test = app.nodeQuery(TestSix.class).getFirst();

			assertNotNull("Invalid simple object creation result", test);
			assertEquals("Invalid simple object creation result", "name", test.getProperty(AbstractNode.name));
			assertEquals("Invalid simple object creation result", null,   test.getProperty(TestSix.oneToOneTestThree));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception");
		}
	}

	/**
	 * Test successful deletion of a node.
	 *
	 * The node shouldn't be found afterwards.
	 * Creation and deletion are executed in two different transactions.
	 *
	 */
	@Test
	public void test01DeleteNode() {

		try {

			final PropertyMap props = new PropertyMap();
			final String type       = "GenericNode";
			final String name       = "GenericNode-name";
			NodeInterface node      = null;
			String uuid             = null;

			props.put(AbstractNode.type, type);
			props.put(AbstractNode.name, name);

			try (final Tx tx = app.tx()) {

				node = app.create(GenericNode.class, props);
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

				Result result = app.nodeQuery().uuid(uuid).getResult();

				assertEquals("Node should have been deleted", 0, result.size());

			} catch (FrameworkException fe) {}

		} catch (FrameworkException ex) {

			logger.warn("", ex);

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
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
			} catch (NotInTransactionException e) {}

			// Relationship still there
			assertNotNull(rel);

			try (final Tx tx = app.tx()) {

				app.delete(rel);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				String uuid = rel.getUuid();
				fail("Deleted entity should have thrown an exception on access.");

			} catch (NotFoundException iex) {
			}


		} catch (FrameworkException ex) {

			logger.warn("", ex);

			logger.error(ex.toString());
			fail("Unexpected exception");

		}


	}

	/**
	 * DELETE_NONE should not trigger any delete cascade
	 */
	@Test
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

			logger.warn("", ex);

			logger.error(ex.toString());
			fail("Unexpected exception");

		}
		 */
	}

	/**
	 * DELETE_INCOMING should not trigger delete cascade from start to end node,
	 * but from end to start node
	 */
	@Test
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

			logger.warn("", ex);

			logger.error(ex.toString());
			fail("Unexpected exception");

		}
		*/
	}

	/**
	 * DELETE_OUTGOING should trigger delete cascade from start to end node,
	 * but not from end to start node.
	 */
	@Test
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

			logger.warn("", ex);

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	/**
	 * DELETE_INCOMING + DELETE_OUTGOING should trigger delete cascade from start to end node
	 * and from end node to start node
	 */
	@Test
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

			logger.warn("", ex);

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	/**
	 * DELETE_IF_CONSTRAINT_WOULD_BE_VIOLATED should
	 * trigger delete cascade from start to end node only
	 * if the remote node would not be valid afterwards
	 */
	@Test
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

			logger.warn("", ex);

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
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

			logger.warn("", ex);

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test01CreateNode() {

		try {

			try {

				// Create node out of transaction => should give a NotInTransactionException
				app.create(TestOne.class);
				fail("Should have raised a NotInTransactionException");
			} catch (NotInTransactionException e) {
			}

			try {

				// Try to create node without parameters => should fail
				app.create(TestOne.class);
				fail("Should have raised a NotInTransactionException");
			} catch (NotInTransactionException e) {}

			AbstractNode node = null;

			try (final Tx tx = app.tx()) {

				node = app.create(TestOne.class);
				tx.success();
			}

			assertTrue(node != null);
			assertTrue(node instanceof TestOne);

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}
	public void test02CreateNodeWithExistingUuid() {

		try {

			final PropertyMap props = new PropertyMap();
			TestOne node            = null;

			final String uuid = StringUtils.replace(UUID.randomUUID().toString(), "-", "");

			props.put(GraphObject.id, uuid);

			try (final Tx tx = app.tx()) {

				node = app.create(TestOne.class, props);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				assertTrue(node != null);
				assertTrue(node instanceof TestOne);
				assertEquals(node.getUuid(), uuid);
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test03CreateRelationship() {

		try {

			final List<GenericNode> nodes = createTestNodes(GenericNode.class, 2);
			final NodeInterface startNode = nodes.get(0);
			final NodeInterface endNode   = nodes.get(1);
			NodeHasLocation rel           = null;

			assertTrue(startNode != null);
			assertTrue(endNode != null);

			try (final Tx tx = app.tx()) {

				rel = app.create(startNode, endNode, NodeHasLocation.class);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				assertEquals(startNode.getUuid(), rel.getSourceNodeId());
				assertEquals(endNode.getUuid(), rel.getTargetNodeId());
				assertEquals(RelType.IS_AT.name(), rel.getType());
				assertEquals(NodeHasLocation.class, rel.getClass());
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	/**
	 * Create a node for each configured entity class and check the type
	 */
	public void test04CheckNodeEntities() {

		AccessControlTest.clearResourceAccess();

		final PropertyMap props = new PropertyMap();

		try (final Tx tx = app.tx()) {

			List<Class> entityList = Collections.EMPTY_LIST;

			try {

				entityList = getClasses("org.structr.core.entity");

			} catch (IOException | ClassNotFoundException ex) {

				logger.error("", ex);
			}

			assertTrue(entityList.contains(AbstractNode.class));
			assertTrue(entityList.contains(GenericNode.class));
			assertTrue(entityList.contains(Location.class));
			assertTrue(entityList.contains(Person.class));
			assertTrue(entityList.contains(ResourceAccess.class));
			assertTrue(entityList.contains(PropertyAccess.class));

			// Don't test these, it would fail due to violated constraints
			entityList.remove(TestTwo.class);
			entityList.remove(TestNine.class);
			entityList.remove(MailTemplate.class);
			entityList.remove(SchemaNode.class);
			entityList.remove(SchemaRelationshipNode.class);

			for (Class type : entityList) {

				// for (Entry<String, Class> entity : entities.entrySet()) {
				// Class entityClass = entity.getValue();
				if (AbstractNode.class.isAssignableFrom(type)) {

					// For TestSeven, fill mandatory fields
					if (type.equals(TestSeven.class)) {

						props.put(TestSeven.name, "TestSeven-0");

					}

					// For ResourceAccess, fill mandatory fields
					if (type.equals(ResourceAccess.class)) {

						props.put(ResourceAccess.signature, "/X");
						props.put(ResourceAccess.flags, 6L);

					}

					// For DynamicResourceAccess, fill mandatory fields
					if (type.equals(DynamicResourceAccess.class)) {

						props.put(DynamicResourceAccess.signature, "/Y");
						props.put(DynamicResourceAccess.flags, 6L);

					}

					// For PropertyAccess, fill mandatory fields
					if (type.equals(PropertyAccess.class)) {

						props.put(PropertyAccess.flags, 6L);

					}

					// For Localization, fill mandatory fields
					if (type.equals(Localization.class)) {

						props.put(Localization.name, "localizationKey");
						props.put(Localization.locale, "de_DE");

					}

					// For Location, set coordinates
					if (type.equals(Location.class)) {

						props.put(Location.latitude, 12.34);
						props.put(Location.longitude, 56.78);

					}

					logger.info("Creating node of type {}", type);

					NodeInterface node = app.create(type, props);

					assertTrue(type.getSimpleName().equals(node.getProperty(AbstractNode.type)));

					// Remove mandatory fields for ResourceAccess from props map
					if (type.equals(ResourceAccess.class)) {

						props.remove(ResourceAccess.signature);
						props.remove(ResourceAccess.flags);

					}

				}
			}

			tx.success();

		} catch (FrameworkException ex) {

			logger.error(ex.toString());

			logger.warn("", ex);

			fail("Unexpected exception");
		}
	}

	/**
	 * Create a node for each configured entity class and check the type
	 */
	public void test05CheckRelationshipEntities() {

		try (final Tx tx = app.tx()) {

			List<Class> entityList = null;

			try {

				entityList = getClasses("org.structr.core.entity");

			} catch (IOException | ClassNotFoundException ex) {

				logger.error("Unable to get list of entity classes", ex);
			}

			assertTrue(entityList.contains(AbstractRelationship.class));
			assertTrue(entityList.contains(GenericRelationship.class));

			for (Class entityClass : entityList) {

				// for (Entry<String, Class> entity : entities.entrySet()) {
				// Class entityClass = entity.getValue();
				if (AbstractRelationship.class.isAssignableFrom(entityClass)) {

					String type = entityClass.getSimpleName();

					logger.info("Creating relationship of type {}", type);

					List<GenericNode> nodes        = createTestNodes(GenericNode.class, 2);
					final NodeInterface startNode  = nodes.get(0);
					final NodeInterface endNode    = nodes.get(1);
					final RelationshipType relType = RelType.IS_AT;
					NodeHasLocation rel       = app.create(startNode, endNode, NodeHasLocation.class);

					assertTrue(rel != null);
					assertTrue(rel.getType().equals(relType.name()));

				}
			}

			tx.success();

		} catch (Throwable ex) {

			logger.warn("", ex);

			logger.error(ex.toString());
			fail("Unexpected exception");
		}
	}

	/**
	 * FIXME: this test is disabled, to be discussed!
	 *
	 * Creation of duplicate relationships is blocked.
	 *
	 * A relationship is considered duplicate if all of the following criteria are met:
	 *
	 * - same start node
	 * - same end node
	 * - same relationship type
	 * - same set of property keys and values
	 *
	public void test06DuplicateRelationships() {

		try {

			List<NodeInterface> nodes      = createTestNodes(GenericNode.class, 2);
			final NodeInterface startNode  = nodes.get(0);
			final NodeInterface endNode    = nodes.get(1);
			final PropertyMap props        = new PropertyMap();

			props.put(new StringProperty("foo"), "bar");
			props.put(new IntProperty("bar"), 123);
			transactionCommand.execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					LocationRelationship rel1 = createRelationshipCommand.execute(startNode, endNode, LocationRelationship.class, props);

					assertTrue(rel1 != null);

					createRelationshipCommand.execute(startNode, endNode, LocationRelationship.class, props);

					return null;

				}

			});

			fail("Creating a duplicate relationship should throw an exception.");


		} catch (FrameworkException ex) {
		}

	}
	 */

	@Test
	public void test01ModifyNode() {

		try {

			final PropertyMap props = new PropertyMap();
			final String type       = "UnknownTestType";
			final String name       = "GenericNode-name";

			NodeInterface node      = null;

			props.put(AbstractNode.type, type);
			props.put(AbstractNode.name, name);

			try (final Tx tx = app.tx()) {

				node = app.create(GenericNode.class, props);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				// Check defaults
				assertEquals(GenericNode.class.getSimpleName(), node.getProperty(AbstractNode.type));
				assertTrue(node.getProperty(AbstractNode.name).equals(name));
				assertTrue(!node.getProperty(AbstractNode.hidden));
				assertTrue(!node.getProperty(AbstractNode.deleted));
				assertTrue(!node.getProperty(AbstractNode.visibleToAuthenticatedUsers));
				assertTrue(!node.getProperty(AbstractNode.visibleToPublicUsers));
			}

			final String name2 = "GenericNode-name-äöüß";

			try (final Tx tx = app.tx()) {

				// Modify values
				node.setProperty(AbstractNode.name, name2);
				node.setProperty(AbstractNode.hidden, true);
				node.setProperty(AbstractNode.deleted, true);
				node.setProperty(AbstractNode.visibleToAuthenticatedUsers, true);
				node.setProperty(AbstractNode.visibleToPublicUsers, true);

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				assertTrue(node.getProperty(AbstractNode.name).equals(name2));
				assertTrue(node.getProperty(AbstractNode.hidden));
				assertTrue(node.getProperty(AbstractNode.deleted));
				assertTrue(node.getProperty(AbstractNode.visibleToAuthenticatedUsers));
				assertTrue(node.getProperty(AbstractNode.visibleToPublicUsers));
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	 /**
	 * Test the results of setProperty and getProperty of a relationship
	 */
	@Test
	public void test02ModifyRelationship() {

		try {

			final NodeHasLocation rel = (createTestRelationships(NodeHasLocation.class, 1)).get(0);
			final PropertyKey key1         = new StringProperty("jghsdkhgshdhgsdjkfgh");
			final String val1              = "54354354546806849870";

			try (final Tx tx = app.tx()) {

				rel.setProperty(key1, val1);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				assertTrue("Expected relationship to have a value for key '" + key1.dbName() + "'", rel.getRelationship().hasProperty(key1.dbName()));

				assertEquals(val1, rel.getRelationship().getProperty(key1.dbName()));

				Object vrfy1 = rel.getProperty(key1);
				assertEquals(val1, vrfy1);
			}

			final String val2 = "öljkhöohü8osdfhoödhi";

			try (final Tx tx = app.tx()) {

				rel.setProperty(key1, val2);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				Object vrfy2 = rel.getProperty(key1);
				assertEquals(val2, vrfy2);
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}
	}

	/**
	 * Test the results of setProperty and getProperty of a node
	 */
	@Test
	public void test03ModifyConstantBooleanProperty() {

		try {

			final PropertyMap props = new PropertyMap();
			final String type       = "Group";
			final String name       = "TestGroup-1";

			NodeInterface node      = null;

			props.put(AbstractNode.type, type);
			props.put(AbstractNode.name, name);

			try (final Tx tx = app.tx()) {

				node = app.create(Group.class, props);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				// Check defaults
				assertEquals(Group.class.getSimpleName(), node.getProperty(AbstractNode.type));
				assertTrue(node.getProperty(AbstractNode.name).equals(name));
				assertTrue(node.getProperty(Group.isGroup));
			}

			final String name2 = "TestGroup-2";

			try (final Tx tx = app.tx()) {

				// Modify values
				node.setProperty(AbstractNode.name, name2);
				node.setProperty(Group.isGroup, false);

				fail("Should have failed with an exception: Group.isGroup is_read_only_property");

				tx.success();

			} catch (FrameworkException expected) {}


		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test01CompareAscending() {

		try {

			TestOne a = createTestNode(TestOne.class);
			TestOne b = createTestNode(TestOne.class);

			try (final Tx tx = app.tx()) {

				GraphObjectComparator comp = new GraphObjectComparator(TestOne.anInt, GraphObjectComparator.ASCENDING);

				try {
					comp.compare(null, null);
					fail("Should have raised an NullPointerException");

				} catch (NullPointerException e) {}

				try {
					comp.compare(a, null);
					fail("Should have raised an NullPointerException");

				} catch (NullPointerException e) {}

				try {
					comp.compare(null, b);
					fail("Should have raised an NullPointerException");

				} catch (NullPointerException e) {}

				try {
					comp.compare(null, b);
					fail("Should have raised an NullPointerException");

				} catch (NullPointerException e) {}

				// a: null
				// b: null
				// a == b => 0
				assertEquals(0, comp.compare(a, b));

				// a: 0
				// b: null
				// a < b => -1
				setPropertyTx(a, TestOne.anInt, 0);
				assertEquals(-1, comp.compare(a, b));

				// a: null
				// b: 0
				// a > b => 1
				setPropertyTx(a, TestOne.anInt, null);
				setPropertyTx(b, TestOne.anInt, 0);
				assertEquals(1, comp.compare(a, b));

				// a: 1
				// b: 2
				// a < b => -1
				setPropertyTx(a, TestOne.anInt, 1);
				setPropertyTx(b, TestOne.anInt, 2);
				assertEquals(-1, comp.compare(a, b));

				// a: 2
				// b: 1
				// a > b => 1
				setPropertyTx(a, TestOne.anInt, 2);
				setPropertyTx(b, TestOne.anInt, 1);
				assertEquals(1, comp.compare(a, b));
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test01CompareDescending() {

		try {

			TestOne a = createTestNode(TestOne.class);
			TestOne b = createTestNode(TestOne.class);

			GraphObjectComparator comp = new GraphObjectComparator(TestOne.anInt, GraphObjectComparator.DESCENDING);

			try {
				comp.compare(null, null);
				fail("Should have raised an NullPointerException");

			} catch (NullPointerException e) {}

			try {
				comp.compare(a, null);
				fail("Should have raised an NullPointerException");

			} catch (NullPointerException e) {}

			try {
				comp.compare(null, b);
				fail("Should have raised an NullPointerException");

			} catch (NullPointerException e) {}

			try {
				comp.compare(null, b);
				fail("Should have raised an NullPointerException");

			} catch (NullPointerException e) {}

			try (final Tx tx = app.tx()) {

				// a: null
				// b: null
				// a == b => 0
				assertEquals(0, comp.compare(a, b));
			}

			// a: 0
			// b: null
			// a > b => 1
			setPropertyTx(a, TestOne.anInt, 0);

			try (final Tx tx = app.tx()) {

				assertEquals(1, comp.compare(a, b));
			}

			// a: null
			// b: 0
			// a < b => -1
			setPropertyTx(a, TestOne.anInt, null);
			setPropertyTx(b, TestOne.anInt, 0);

			try (final Tx tx = app.tx()) {

				assertEquals(-1, comp.compare(a, b));
			}

			// a: 1
			// b: 2
			// a > b => 1
			setPropertyTx(a, TestOne.anInt, 1);
			setPropertyTx(b, TestOne.anInt, 2);

			try (final Tx tx = app.tx()) {

				assertEquals(1, comp.compare(a, b));
			}

			// a: 2
			// b: 1
			// a < b => -1
			setPropertyTx(a, TestOne.anInt, 2);
			setPropertyTx(b, TestOne.anInt, 1);

			try (final Tx tx = app.tx()) {

				assertEquals(-1, comp.compare(a, b));
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}
	}

	// ----- private methods -----
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

	private void setPropertyTx(final GraphObject obj, final PropertyKey key, final Object value) {

		try (final Tx tx = app.tx()) {

			obj.setProperty(key, value);
			tx.success();

		} catch (FrameworkException ex) {
		}
	}
}
