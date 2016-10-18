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
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.StructrAndSpatialPredicate;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;

//~--- classes ----------------------------------------------------------------

/**
 * Delete all nodes which have the 'deleted' property set to true
 *
 *
 */
public class BulkDeleteSoftDeletedNodesCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = LoggerFactory.getLogger(BulkDeleteSoftDeletedNodesCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void execute(final Map<String, Object> properties) throws FrameworkException {

		final DatabaseService graphDb          = (DatabaseService) arguments.get("graphDb");
		final SecurityContext superUserContext = SecurityContext.getSuperUserInstance();
		final NodeFactory nodeFactory          = new NodeFactory(superUserContext, true, false);

		if (graphDb != null) {

			Iterator<AbstractNode> nodeIterator = null;

			try (final Tx tx = StructrApp.getInstance().tx()) {

				nodeIterator = Iterables.map(nodeFactory, Iterables.filter(new StructrAndSpatialPredicate(true, false, false), graphDb.getAllNodes())).iterator();
				tx.success();
			}

			final boolean erase;

			if (properties.containsKey("erase") && properties.get("erase").equals("true")) {
				erase = true;
			} else {
				erase = false;
			}

			bulkGraphOperation(securityContext, nodeIterator, 1000, "DeleteSoftDeletedNodes", new BulkGraphOperation<AbstractNode>() {

				@Override
				public void handleGraphObject(SecurityContext securityContext, AbstractNode node) {

					if (node.isDeleted()) {
						logger.info("Found deleted node: {}", node);

						if (erase) {

							try {
								StructrApp.getInstance(securityContext).delete(node);

							} catch (FrameworkException ex) {
								logger.warn("Could not delete node " + node, ex);
							}
						}
					}

				}

				@Override
				public void handleThrowable(SecurityContext securityContext, Throwable t, AbstractNode node) {
					logger.warn("Unable to set properties of node {}: {}", new Object[] { node.getUuid(), t.getMessage() } );
				}

				@Override
				public void handleTransactionFailure(SecurityContext securityContext, Throwable t) {
					logger.warn("Unable to set node properties: {}", t.getMessage() );
				}
			});

		}

		logger.info("Done");
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}
}
