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
import org.neo4j.graphdb.RelationshipType;

import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.Cache;
import org.structr.core.entity.GenericNode;
import org.structr.core.entity.GenericRelationship;
import org.structr.core.entity.Location;
import org.structr.core.entity.Person;
import org.structr.core.entity.PlainText;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.graph.StructrTransaction;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.entity.PropertyAccess;
import org.structr.core.entity.PropertyDefinition;
import org.structr.core.entity.TestSeven;
import org.structr.core.property.IntProperty;
import org.structr.core.property.StringProperty;
import org.structr.core.entity.TestTwo;

//~--- classes ----------------------------------------------------------------

/**
 * Test basic create operations with graph objects (nodes, relationships)
 *
 * All tests are executed in superuser context
 *
 * @author Axel Morgner
 */
public class CreateGraphObjectsTest extends StructrTest {

	private static final Logger logger = Logger.getLogger(CreateGraphObjectsTest.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void test00DbAvailable() {
		super.test00DbAvailable();
	}
	
	public void test01CreateNode() {

		try {

			AbstractNode node;

			try {

				// Create node out of transaction => should give a NotInTransactionException
				createNodeCommand.execute();
				fail("Should have raised an org.neo4j.graphdb.NotInTransactionException");
			} catch (org.neo4j.graphdb.NotInTransactionException e) {}

			final PropertyMap props = new PropertyMap();

			props.put(AbstractNode.type, "UnknownTestTypeÄÖLß");

			try {

				// Try to create node without parameters => should fail
				createNodeCommand.execute();
				fail("Should have raised an org.neo4j.graphdb.NotInTransactionException");
			} catch (org.neo4j.graphdb.NotInTransactionException e) {}

			node = transactionCommand.execute(new StructrTransaction<AbstractNode>() {

				@Override
				public AbstractNode execute() throws FrameworkException {

					// Create node with a type which has no entity class => should result in a node of type 'GenericNode'
					return (AbstractNode) createNodeCommand.execute(props);
				}

			});

			assertTrue(node != null);
			assertTrue(node instanceof GenericNode);

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test02CreateRelationship() {

		try {

			List<AbstractNode> nodes       = createTestNodes("UnknownTestType", 2);
			final AbstractNode startNode   = nodes.get(0);
			final AbstractNode endNode     = nodes.get(1);
			final RelationshipType relType = RelType.IS_AT;

			assertTrue(startNode != null);
			assertTrue(endNode != null);

			AbstractRelationship rel = (AbstractRelationship) transactionCommand.execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					return (AbstractRelationship) createRelationshipCommand.execute(startNode, endNode, relType);

				}

			});

			assertTrue(rel.getStartNodeId().equals(startNode.getUuid()));
			assertTrue(rel.getEndNodeId().equals(endNode.getUuid()));
			assertTrue(rel.getType().equals(relType.name()));

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	/**
	 * Create a node for each configured entity class and check the type
	 */
	public void test03CheckNodeEntities() {

		final PropertyMap props = new PropertyMap();

		try {

			transactionCommand.execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					List<Class> entityList = null;

					try {

						entityList = getClasses("org.structr.core.entity");

					} catch (ClassNotFoundException ex) {

						logger.log(Level.SEVERE, null, ex);

					} catch (IOException ex) {

						logger.log(Level.SEVERE, null, ex);

					}

					assertTrue(entityList.contains(AbstractNode.class));
					assertTrue(entityList.contains(Cache.class));
					assertTrue(entityList.contains(GenericNode.class));
					assertTrue(entityList.contains(Location.class));
					assertTrue(entityList.contains(PlainText.class));
					assertTrue(entityList.contains(Person.class));
					assertTrue(entityList.contains(ResourceAccess.class));
					assertTrue(entityList.contains(PropertyAccess.class));
					
					// Don't test these, it would fail due to violated constraints
					entityList.remove(TestTwo.class);
					entityList.remove(PropertyDefinition.class);

					for (Class entityClass : entityList) {

						// for (Entry<String, Class> entity : entities.entrySet()) {
						// Class entityClass = entity.getValue();
						if (AbstractNode.class.isAssignableFrom(entityClass)) {

							String type = entityClass.getSimpleName();
							
							// For TestSeven, fill mandatory fields
							if (type.equals(TestSeven.class.getSimpleName())) {

								props.put(TestSeven.name, "TestSeven-0");

							}

							// For ResourceAccess, fill mandatory fields
							if (type.equals(ResourceAccess.class.getSimpleName())) {

								props.put(ResourceAccess.signature, "/");
								props.put(ResourceAccess.flags, 6);

							}

							// For PropertyAccess, fill mandatory fields
							if (type.equals(PropertyAccess.class.getSimpleName())) {

								props.put(PropertyAccess.flags, 6);

							}

							// For Location, set coordinates
							if (type.equals(Location.class.getSimpleName())) {

								props.put(Location.latitude, 12.34);
								props.put(Location.longitude, 56.78);

							}
							
							logger.log(Level.INFO, "Creating node of type {0}", type);
							props.put(AbstractNode.type, type);

							AbstractNode node = (AbstractNode) createNodeCommand.execute(props);

							assertTrue(type.equals(node.getProperty(AbstractNode.type)));

							// Remove mandatory fields for ResourceAccess from props map
							if (type.equals(ResourceAccess.class.getSimpleName())) {

								props.remove(ResourceAccess.signature);
								props.remove(ResourceAccess.flags);

							}

						}
					}

					return null;

				}

			});

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			System.out.println(ex.toString());
			fail("Unexpected exception");

		}

	}

	/**
	 * Create a node for each configured entity class and check the type
	 */
	public void test04CheckRelationshipEntities() {

		try {

			transactionCommand.execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					List<Class> entityList = null;

					try {

						entityList = getClasses("org.structr.core.entity");

					} catch (ClassNotFoundException ex) {

						Logger.getLogger(CreateGraphObjectsTest.class.getName()).log(Level.SEVERE, null, ex);

					} catch (IOException ex) {

						Logger.getLogger(CreateGraphObjectsTest.class.getName()).log(Level.SEVERE, null, ex);

					}

					assertTrue(entityList.contains(AbstractRelationship.class));
					assertTrue(entityList.contains(GenericRelationship.class));

					for (Class entityClass : entityList) {

						// for (Entry<String, Class> entity : entities.entrySet()) {
						// Class entityClass = entity.getValue();
						if (AbstractRelationship.class.isAssignableFrom(entityClass)) {

							String type = entityClass.getSimpleName();

							logger.log(Level.INFO, "Creating relationship of type {0}", type);

							List<AbstractNode> nodes       = createTestNodes("UnknownTestType", 2);
							final AbstractNode startNode   = nodes.get(0);
							final AbstractNode endNode     = nodes.get(1);
							final RelationshipType relType = RelType.IS_AT;
							AbstractRelationship rel       = (AbstractRelationship) createRelationshipCommand.execute(startNode, endNode, relType);

							assertTrue(rel != null);
							assertTrue(rel.getType().equals(relType.name()));

						}
					}

					return null;

				}

			});

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	/**
	 * Creation of duplicate relationships is blocked.
	 *
	 * A relationship is considered duplicate if all of the following criteria are met:
	 *
	 * - same start node
	 * - same end node
	 * - same relationship type
	 * - same set of property keys and values
	 *
	 */
	public void test05DuplicateRelationships() {

		try {

			List<AbstractNode> nodes       = createTestNodes("UnknownTestType", 2);
			final AbstractNode startNode   = nodes.get(0);
			final AbstractNode endNode     = nodes.get(1);
			final RelationshipType relType = RelType.IS_AT;
			final PropertyMap props        = new PropertyMap();

			props.put(new StringProperty("foo"), "bar");
			props.put(new IntProperty("bar"), 123);
			transactionCommand.execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					AbstractRelationship rel1 = createRelationshipCommand.execute(startNode, endNode, relType, props, true);

					assertTrue(rel1 != null);

					AbstractRelationship rel2 = createRelationshipCommand.execute(startNode, endNode, relType, props, true);

					assertTrue(rel2 == null);

					return null;

				}

			});

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

}
