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
package org.structr.core.graph;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.tooling.GlobalGraphOperations;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.entity.AbstractNode;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.helpers.collection.Iterables;
import org.structr.common.StructrAndSpatialPredicate;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.schema.SchemaHelper;

//~--- classes ----------------------------------------------------------------
/**
 * Rebuild index for nodes or relationships of given type.
 *
 * Use 'type' argument for node type, and 'relType' for relationship type.
 *
 * @author Axel Morgner
 */
public class BulkCreateLabelsCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = Logger.getLogger(BulkCreateLabelsCommand.class.getName());

	//~--- methods --------------------------------------------------------
	@Override
	public void execute(Map<String, Object> attributes) throws FrameworkException {

		final String entityType = (String) attributes.get("type");
		final GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
		final SecurityContext superUserContext = SecurityContext.getSuperUserInstance();
		final NodeFactory nodeFactory = new NodeFactory(superUserContext);

		Class type = null;
		if (entityType != null) {

			type = SchemaHelper.getEntityClassForRawType(entityType);
		}
		// final Result<AbstractNode> result = StructrApp.getInstance(securityContext).command(SearchNodeCommand.class).execute(true, false, Search.andExactType(type.getSimpleName()));

		final List<AbstractNode> nodes = new LinkedList<>();

		// instantiate all nodes in a single list
		try (final Tx tx = StructrApp.getInstance().tx()) {

			final Result<AbstractNode> result = nodeFactory.instantiateAll(Iterables.filter(new StructrAndSpatialPredicate(true, false, false), GlobalGraphOperations.at(graphDb).getAllNodes()));
			for (AbstractNode node : result.getResults()) {

				if (type == null || node.getClass().equals(type)) {

					nodes.add(node);
				}

			}
		}

		if (type == null) {

			logger.log(Level.INFO, "Node type not set or no entity class found. Starting creation of labels for {0} nodes", nodes.size());

		} else {

			logger.log(Level.INFO, "Starting creation of labels for {0} nodes of type {0}", new Object[]{nodes.size(), type.getSimpleName()});
		}

		long count = bulkGraphOperation(securityContext, nodes, 1000, "CreateLabels", new BulkGraphOperation<AbstractNode>() {

			@Override
			public void handleGraphObject(SecurityContext securityContext, AbstractNode node) {

				final String type = node.getProperty(GraphObject.type);
				if (type != null) {

					try {

						// Since the setProperty method of the TypeProperty
						// overrides the default setProperty behaviour, we
						// do not need to set a different type value first.

						node.unlockReadOnlyPropertiesOnce();
						GraphObject.type.setProperty(securityContext, node, type);

					} catch (FrameworkException fex) {
						// ignore
					}
				}
				node.updateInIndex();

			}

			@Override
			public void handleThrowable(SecurityContext securityContext, Throwable t, AbstractNode node) {

				logger.log(Level.WARNING, "Unable to create labels for node {0}: {1}", new Object[]{node, t.getMessage()});

			}

			@Override
			public void handleTransactionFailure(SecurityContext securityContext, Throwable t) {

				logger.log(Level.WARNING, "Unable to create labels for node: {0}", t.getMessage());

			}

		});

		logger.log(Level.INFO, "Done with creating labels on {0} nodes", count);
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}
}
