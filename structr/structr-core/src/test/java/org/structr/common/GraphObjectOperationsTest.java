/*
 *  Copyright (C) 2010-2012 Axel Morgner
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


import org.neo4j.graphdb.RelationshipType;

import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.GenericNode;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;



import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Test basic operations with graph objects (nodes, relationships)
 *
 * All tests are executed in superuser context
 *
 * @author Axel Morgner
 */
public class GraphObjectOperationsTest extends StructrTest {

	private static final Logger logger = Logger.getLogger(GraphObjectOperationsTest.class.getName());

	//~--- fields ---------------------------------------------------------


	//~--- methods --------------------------------------------------------

	public void testCreateNode() {

		try {

			AbstractNode node;

			try {

				// Create node out of transaction => should give a NotInTransactionException
				createNodeCommand.execute();
				fail("Should have raised an org.neo4j.graphdb.NotInTransactionException");
			} catch (org.neo4j.graphdb.NotInTransactionException e) {}

			// Create node out of transaction => should give a NotInTransactionException
			final Map<String, Object> props = new HashMap<String, Object>();

			props.put(AbstractNode.Key.type.name(), "UnknownTestTypeÄÖLß");

			try {

				// Try to create node without parameters => should fail
				createNodeCommand.execute();
				fail("Should have raised an org.neo4j.graphdb.NotInTransactionException");
			} catch (org.neo4j.graphdb.NotInTransactionException e) {}

			node = (AbstractNode) transactionCommand.execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					// Create node with a type which has no entity class => should result in a node of type 'GenericNode'
					return (AbstractNode) createNodeCommand.execute(props);
				}

			});

			assertTrue(node != null);
			assertTrue(node instanceof GenericNode);

		} catch (FrameworkException ex) {

			Logger.getLogger(GraphObjectOperationsTest.class.getName()).log(Level.SEVERE, null, ex);

		}

	}

	public void testCreateRelationship() {

		try {

			List<AbstractNode> nodes       = createTestNodes("UnknownTestType", 2);
			final AbstractNode startNode   = nodes.get(0);
			final AbstractNode endNode     = nodes.get(1);
			final RelationshipType relType = RelType.CONTAINS;

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

			Logger.getLogger(GraphObjectOperationsTest.class.getName()).log(Level.SEVERE, null, ex);

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
	public void testDuplicateRelationships() {

		try {

			List<AbstractNode> nodes       = createTestNodes("UnknownTestType", 2);
			final AbstractNode startNode   = nodes.get(0);
			final AbstractNode endNode     = nodes.get(1);
			final RelationshipType relType = RelType.UNDEFINED;
			
			final Map<String, Object> props = new HashMap<String, Object>();
			
			props.put("foo", "bar");
			props.put("bar", 123);

			transactionCommand.execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					AbstractRelationship rel1 = (AbstractRelationship) createRelationshipCommand.execute(startNode, endNode, relType, props, true);
					
					assertTrue(rel1 != null);
					
					AbstractRelationship rel2 = (AbstractRelationship) createRelationshipCommand.execute(startNode, endNode, relType, props, true);
					
					assertTrue(rel2 == null);
					
					return null;

				}

			});


		} catch (FrameworkException ex) {

			Logger.getLogger(GraphObjectOperationsTest.class.getName()).log(Level.SEVERE, null, ex);

		}

	}

	/**
	 * Create a node for each configured entity class and check the type
	 */
	public void testCreateAbstractNodeTypes() {

		final Map<String, Object> props = new HashMap<String, Object>();
		StructrTransaction tx           = new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				// Command getEntities         = Services.command(securityContext, GetEntitiesCommand.class);
				// Map<String, Class> entities = (Map<String, Class>) getEntities.execute();
				Class[] entities = null;

				try {

					entities = getClasses("org.structr.core.entity");

				} catch (ClassNotFoundException ex) {

					Logger.getLogger(GraphObjectOperationsTest.class.getName()).log(Level.SEVERE, null, ex);

				} catch (IOException ex) {

					Logger.getLogger(GraphObjectOperationsTest.class.getName()).log(Level.SEVERE, null, ex);

				}

				for (Class entityClass : entities) {

					// for (Entry<String, Class> entity : entities.entrySet()) {
					// Class entityClass = entity.getValue();
					if (AbstractNode.class.isAssignableFrom(entityClass)) {

						String type = entityClass.getSimpleName();

						logger.log(Level.INFO, "Creating node of type {0}", type);
						props.put(AbstractNode.Key.type.name(), type);

						AbstractNode node = (AbstractNode) createNodeCommand.execute(props);

						assertTrue(type.equals(node.getStringProperty(AbstractNode.Key.type)));

					}
				}

				return null;
			}

		};

		try {

			Services.command(securityContext, TransactionCommand.class).execute(tx);

		} catch (FrameworkException ex) {

			Logger.getLogger(GraphObjectOperationsTest.class.getName()).log(Level.SEVERE, null, ex);

		}

	}

	/**
	 * Create a node for each configured entity class and check the type
	 */
	public void testCreateRelationshipTypes() {

		try {

			transactionCommand.execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					// Command getEntities         = Services.command(securityContext, GetEntitiesCommand.class);
					// Map<String, Class> entities = (Map<String, Class>) getEntities.execute();
					Class[] entities = null;

					try {

						entities = getClasses("org.structr.core.entity");

					} catch (ClassNotFoundException ex) {

						Logger.getLogger(GraphObjectOperationsTest.class.getName()).log(Level.SEVERE, null, ex);

					} catch (IOException ex) {

						Logger.getLogger(GraphObjectOperationsTest.class.getName()).log(Level.SEVERE, null, ex);

					}

					for (Class entityClass : entities) {

						// for (Entry<String, Class> entity : entities.entrySet()) {
						// Class entityClass = entity.getValue();
						if (AbstractRelationship.class.isAssignableFrom(entityClass)) {

							String type = entityClass.getSimpleName();

							logger.log(Level.INFO, "Creating relationship of type {0}", type);

							List<AbstractNode> nodes       = createTestNodes("UnknownTestType", 2);
							final AbstractNode startNode   = nodes.get(0);
							final AbstractNode endNode     = nodes.get(1);
							final RelationshipType relType = RelType.LINK;
							AbstractRelationship rel       = (AbstractRelationship) createRelationshipCommand.execute(startNode, endNode, relType);

							assertTrue(rel != null);
							assertTrue(rel.getType().equals(relType.name()));

						}
					}

					return null;
				}

			});

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, null, ex);

		}

	}


}
