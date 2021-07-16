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

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;

/**
 * Sets a new UUID on each graph object of the given type. For nodes, set type,
 * for relationships relType.
 */
public class BulkSetUuidCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = LoggerFactory.getLogger(BulkSetUuidCommand.class.getName());

	@Override
	public void execute(final Map<String, Object> attributes) throws FrameworkException {

		final String nodeType         = (String) attributes.get("type");
		final String relType          = (String) attributes.get("relType");
		final Boolean allNodes        = (Boolean) attributes.get("allNodes");
		final Boolean allRels         = (Boolean) attributes.get("allRels");
		final DatabaseService graphDb = (DatabaseService) arguments.get("graphDb");

		final SecurityContext superUserContext = SecurityContext.getSuperUserInstance();
		final NodeFactory nodeFactory          = new NodeFactory(superUserContext);
		final RelationshipFactory relFactory   = new RelationshipFactory(superUserContext);

		if (nodeType != null || Boolean.TRUE.equals(allNodes)) {

			Iterable<AbstractNode> nodes = null;

			if (Boolean.TRUE.equals(allNodes)) {

				nodes = Iterables.map(nodeFactory, graphDb.getAllNodes());

				info("Start setting UUID on all nodes");

			} else {

				nodes = Iterables.map(nodeFactory, graphDb.getNodesByTypeProperty(nodeType));

				info("Start setting UUID on nodes of type {}", new Object[] { nodeType });
			}

			final long count = bulkGraphOperation(securityContext, nodes, 1000, "SetNodeUuid", new BulkGraphOperation<AbstractNode>() {

				@Override
				public boolean handleGraphObject(final SecurityContext securityContext, final AbstractNode node) {

					try {

						if (node.getProperty(GraphObject.id) == null) {

							node.unlockSystemPropertiesOnce();
							node.setProperty(GraphObject.id, NodeServiceCommand.getNextUuid());
						}

					} catch (FrameworkException fex) {

						logger.warn("Unable to set UUID of node {}", node, fex);
					}

					return true;
				}

				@Override
				public void handleThrowable(SecurityContext securityContext, Throwable t, AbstractNode node) {
					logger.warn("Unable to set UUID of node {}", node, t);
				}

				@Override
				public void handleTransactionFailure(SecurityContext securityContext, Throwable t) {
					logger.warn("Unable to set UUID on node", t);
				}

				@Override
				public boolean doValidation() {
					return false;
				}
			});

			info("Done with setting UUID on {} nodes", count);

			return;
		}

		if (relType != null || Boolean.TRUE.equals(allRels)) {

			Iterable<AbstractRelationship> relationships = null;

			if (Boolean.TRUE.equals(allRels)) {

				relationships = Iterables.map(relFactory, graphDb.getAllRelationships());

				info("Start setting UUID on all rels");

			} else {

				relationships = Iterables.map(relFactory, graphDb.getRelationshipsByType(relType));

				info("Start setting UUID on rels of type {}", relType);
			}

			final long count = bulkGraphOperation(securityContext, relationships, 1000, "SetRelationshipUuid", new BulkGraphOperation<AbstractRelationship>() {

				@Override
				public boolean handleGraphObject(SecurityContext securityContext, AbstractRelationship rel) {

					try {

						if (rel.getProperty(GraphObject.id) == null) {

							rel.unlockSystemPropertiesOnce();
							rel.setProperty(GraphObject.id, NodeServiceCommand.getNextUuid());
						}

					} catch (FrameworkException fex) {

						logger.warn("Unable to set UUID of relationship {}: {}", new Object[] { rel, fex.getMessage() });
					}

					return true;
				}

				@Override
				public void handleThrowable(SecurityContext securityContext, Throwable t, AbstractRelationship rel) {
					logger.warn("Unable to set UUID of relationship {}: {}", rel, t.getMessage());
				}

				@Override
				public void handleTransactionFailure(SecurityContext securityContext, Throwable t) {
					logger.warn("Unable to set UUID on relationships {}", t.toString());
				}

				@Override
				public boolean doValidation() {
					return false;
				}
			});

			info("Done with setting UUID on {} relationships", count);

			return;
		}

		info("Unable to determine entity type to set UUID.");

	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}

	@Override
	public boolean requiresFlushingOfCaches() {
		return true;
	}
}
