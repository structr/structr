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
package org.structr.core.graph;

import java.util.Iterator;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.tooling.GlobalGraphOperations;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.Iterables;
import org.structr.common.StructrAndSpatialPredicate;
import org.structr.core.app.StructrApp;

//~--- classes ----------------------------------------------------------------

/**
 * Sets a new UUID on each graph object of the given type. For nodes, set type,
 * for relationships relType.
 *
 *
 */
public class BulkSetUuidCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = Logger.getLogger(BulkSetUuidCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void execute(Map<String, Object> attributes) throws FrameworkException {

		final String nodeType                = (String) attributes.get("type");
		final String relType                   = (String) attributes.get("relType");
		final Boolean allNodes                 = (Boolean) attributes.get("allNodes");
		final Boolean allRels                  = (Boolean) attributes.get("allRels");
		final GraphDatabaseService graphDb     = (GraphDatabaseService) arguments.get("graphDb");

		if (nodeType != null || Boolean.TRUE.equals(allNodes)) {

			Iterator<Node> nodeIterator = null;

			try (final Tx tx = StructrApp.getInstance().tx()) {

				nodeIterator = Iterables.filter(new StructrAndSpatialPredicate(false, false, true), GlobalGraphOperations.at(graphDb).getAllNodes()).iterator();
				tx.success();

			} catch (FrameworkException fex) {
				logger.log(Level.WARNING, "Exception while creating all nodes iterator.");
				fex.printStackTrace();
			}

			logger.log(Level.INFO, "Start setting UUID on all nodes of type {0}", new Object[] { nodeType });

			final long count = bulkGraphOperation(securityContext, nodeIterator, 1000, "SetNodeProperties", new BulkGraphOperation<Node>() {

				@Override
				public void handleGraphObject(final SecurityContext securityContext, final Node node) {

					node.setProperty("id", NodeServiceCommand.getNextUuid());

					if (nodeType != null && !node.hasProperty("type")) {
						node.setProperty("type", nodeType);
					}
				}
				@Override
				public void handleThrowable(SecurityContext securityContext, Throwable t, Node node) {
					logger.log(Level.WARNING, "Unable to set UUID of node {0}: {1}", new Object[] { node, t.getMessage() });
				}

				@Override
				public void handleTransactionFailure(SecurityContext securityContext, Throwable t) {
					logger.log(Level.WARNING, "Unable to set UUID on node: {0}", t.getMessage());
				}
			});

			logger.log(Level.INFO, "Done with setting UUID on {0} nodes", count);

			return;
		}

		if (relType != null || Boolean.TRUE.equals(allRels)) {

			Iterator<Relationship> relIterator = null;

			try (final Tx tx = StructrApp.getInstance().tx()) {

				relIterator = Iterables.filter(new StructrAndSpatialPredicate(false, false, true), GlobalGraphOperations.at(graphDb).getAllRelationships()).iterator();
				tx.success();

			} catch (FrameworkException fex) {
				logger.log(Level.WARNING, "Exception while creating all nodes iterator.");
				fex.printStackTrace();
			}

			logger.log(Level.INFO, "Start setting UUID on all rels of type {0}", new Object[] { relType });

			final long count = bulkGraphOperation(securityContext, relIterator, 1000, "SetRelationshipUuid", new BulkGraphOperation<Relationship>() {

				@Override
				public void handleGraphObject(SecurityContext securityContext, Relationship rel) {
					rel.setProperty("id", NodeServiceCommand.getNextUuid());
				}
				@Override
				public void handleThrowable(SecurityContext securityContext, Throwable t, Relationship rel) {
					logger.log(Level.WARNING, "Unable to set UUID of relationship {0}: {1}", new Object[] { rel, t.getMessage() });
				}
				@Override
				public void handleTransactionFailure(SecurityContext securityContext, Throwable t) {
					logger.log(Level.WARNING, "Unable to set UUID on relationship: {0}", t.getMessage());
				}
			});

			logger.log(Level.INFO, "Done with setting UUID on {0} relationships", count);

			return;
		}

		logger.log(Level.INFO, "Unable to determine entity type to set UUID.");

	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}
}
