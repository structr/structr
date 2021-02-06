/*
 * Copyright (C) 2010-2021 Structr GmbH
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

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;

/**
 * Clears database.
 *
 * This command takes no parameters.
 */
public class ClearDatabase extends NodeServiceCommand {

	private static final Logger logger = LoggerFactory.getLogger(ClearDatabase.class.getName());

	public void execute() throws FrameworkException {

		final DatabaseService graphDb = (DatabaseService) arguments.get("graphDb");

		if (graphDb != null) {

			List<NodeInterface> nodes = null;
			final App app = StructrApp.getInstance();

			try (final Tx tx = app.tx()) {

				nodes = app.nodeQuery(NodeInterface.class).getAsList();
				tx.success();

			} catch (FrameworkException fex) {
				logger.warn("Exception while creating all nodes iterator.", fex);
			}

			final long deletedNodes = bulkGraphOperation(securityContext, nodes, 1000, "ClearDatabase", new BulkGraphOperation<NodeInterface>() {

				@Override
				public boolean handleGraphObject(SecurityContext securityContext, NodeInterface node) {

					// Delete only "our" nodes
					if (node.getProperty(GraphObject.id) != null) {

						try {
							app.delete(node);

						} catch (FrameworkException fex) {
							logger.warn("Unable to delete node {}: {}", new Object[] { node.getUuid(), fex.getMessage() } );
						}
					}

					return true;
				}

				@Override
				public void handleThrowable(SecurityContext securityContext, Throwable t, NodeInterface node) {
					logger.warn("Unable to delete node {}: {}", new Object[] { node.getUuid(), t.getMessage() } );
				}

				@Override
				public void handleTransactionFailure(SecurityContext securityContext, Throwable t) {
					logger.warn("Unable to clear database: {}", t.getMessage() );
				}
			});

			logger.info("Finished deleting {} nodes", deletedNodes);

		}
	}
}
