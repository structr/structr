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


package org.structr.core.graph;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.tooling.GlobalGraphOperations;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.schema.SchemaHelper;

//~--- classes ----------------------------------------------------------------

/**
 * Rebuild index for nodes or relationships of given type.
 *
 * Use 'type' argument for node type, and 'relType' for relationship type.
 *
 * @author Axel Morgner
 */
public class BulkRebuildIndexCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = Logger.getLogger(BulkRebuildIndexCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void execute(Map<String, Object> attributes) throws FrameworkException {

		final String entityType                = (String) attributes.get("type");
		final String relType                   = (String) attributes.get("relType");
		final GraphDatabaseService graphDb     = (GraphDatabaseService) arguments.get("graphDb");
		final SecurityContext superUserContext = SecurityContext.getSuperUserInstance();
		final NodeFactory nodeFactory          = new NodeFactory(superUserContext);
		final RelationshipFactory relFactory   = new RelationshipFactory(superUserContext);

		Class type = null;
		if (entityType != null) {

			type = SchemaHelper.getEntityClassForRawType(entityType);
		}
		// final Result<AbstractNode> result = StructrApp.getInstance(securityContext).command(SearchNodeCommand.class).execute(true, false, Search.andExactType(type.getSimpleName()));
		final Result<AbstractNode> result = nodeFactory.instantiateAll(GlobalGraphOperations.at(graphDb).getAllNodes());
		final List<AbstractNode> nodes    = new ArrayList<>();

		for (AbstractNode node : result.getResults()) {

			if (type == null || node.getClass().equals(type)) {

				nodes.add(node);
			}

		}

		if (type == null) {
	
			logger.log(Level.INFO, "Node type not set or no entity class found. Starting (re-)indexing all nodes");
			
		} else {
			
			logger.log(Level.INFO, "Starting (re-)indexing all nodes of type {0}", new Object[] { type.getSimpleName() });
		}

		long count = bulkGraphOperation(securityContext, nodes, 1000, "RebuildNodeIndex", new BulkGraphOperation<AbstractNode>() {

			@Override
			public void handleGraphObject(SecurityContext securityContext, AbstractNode node) {

				node.updateInIndex();

			}
			@Override
			public void handleThrowable(SecurityContext securityContext, Throwable t, AbstractNode node) {

				logger.log(Level.WARNING, "Unable to index node {0}: {1}", new Object[] { node, t.getMessage() });

			}
			@Override
			public void handleTransactionFailure(SecurityContext securityContext, Throwable t) {

				logger.log(Level.WARNING, "Unable to index node: {0}", t.getMessage());

			}

		});

		logger.log(Level.INFO, "Done with (re-)indexing {0} nodes", count);

		// final Result<AbstractNode> result = StructrApp.getInstance(securityContext).command(SearchNodeCommand.class).execute(true, false, Search.andExactType(type.getSimpleName()));
		final List<AbstractRelationship> unfilteredRels = relFactory.instantiate(GlobalGraphOperations.at(graphDb).getAllRelationships());
		final List<AbstractRelationship> rels           = new ArrayList<AbstractRelationship>();

		for (AbstractRelationship rel : unfilteredRels) {

			if (relType == null || rel.getType().equals(relType)) {

				rels.add(rel);
			}

		}

		if (relType == null) {

			logger.log(Level.INFO, "Relationship type not set, starting (re-)indexing all relationships");
			
		} else {
		
			logger.log(Level.INFO, "Starting (re-)indexing all relationships of type {0}", new Object[] { relType });
			
		}

		count = bulkGraphOperation(securityContext, rels, 1000, "RebuildRelIndex", new BulkGraphOperation<AbstractRelationship>() {

			@Override
			public void handleGraphObject(SecurityContext securityContext, AbstractRelationship rel) {

				rel.updateInIndex();

			}
			@Override
			public void handleThrowable(SecurityContext securityContext, Throwable t, AbstractRelationship rel) {

				logger.log(Level.WARNING, "Unable to index relationship {0}: {1}", new Object[] { rel, t.getMessage() });

			}
			@Override
			public void handleTransactionFailure(SecurityContext securityContext, Throwable t) {

				logger.log(Level.WARNING, "Unable to index relationship: {0}", t.getMessage());

			}

		});

		logger.log(Level.INFO, "Done with (re-)indexing {0} relationships", count);

		return;


	}

}
