/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;

import java.util.Map;

/**
 * Sets a new UUID on each graph object of the given type. For nodes, set type,
 * for relationships relType.
 */
public class BulkSetUuidCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = LoggerFactory.getLogger(BulkSetUuidCommand.class.getName());

	@Override
	public void execute(final Map<String, Object> attributes) throws FrameworkException {
		executeWithCount(attributes);
	}


	public long executeWithCount(final Map<String, Object> attributes) throws FrameworkException {

		final PropertyKey<String> idProperty = Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.ID_PROPERTY);
		final String nodeType  = (String) attributes.get("type");
		final String relType   = (String) attributes.get("relType");
		final Boolean allNodes = (Boolean) attributes.get("allNodes");
		final Boolean allRels  = (Boolean) attributes.get("allRels");

		if (nodeType != null || Boolean.TRUE.equals(allNodes)) {

			if (Boolean.TRUE.equals(allNodes)) {

				info("Start setting UUID on all nodes");

			} else {

				info("Start setting UUID on nodes of type {}", nodeType);
			}

			final long count = bulkGraphOperation(securityContext, getNodeQuery(nodeType, Boolean.TRUE.equals(allNodes)), 1000, "SetNodeUuid", new BulkGraphOperation<NodeInterface>() {

				@Override
				public boolean handleGraphObject(final SecurityContext securityContext, final NodeInterface node) {

					try {

						if (node.getProperty(idProperty) == null) {

							node.unlockSystemPropertiesOnce();
							node.setProperty(idProperty, NodeServiceCommand.getNextUuid());
						}

					} catch (FrameworkException fex) {

						logger.warn("Unable to set UUID of node {}", node, fex);
					}

					return true;
				}

				@Override
				public void handleThrowable(SecurityContext securityContext, Throwable t, NodeInterface node) {
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

			return count;
		}

		if (relType != null || Boolean.TRUE.equals(allRels)) {

			if (Boolean.TRUE.equals(allRels)) {

				info("Start setting UUID on all rels");

			} else {

				info("Start setting UUID on rels of type {}", relType);
			}

			final long count = bulkGraphOperation(securityContext, getRelationshipQuery(relType, Boolean.TRUE.equals(allRels)), 1000, "SetRelationshipUuid", new BulkGraphOperation<AbstractRelationship>() {

				@Override
				public boolean handleGraphObject(SecurityContext securityContext, AbstractRelationship rel) {

					try {

						if (rel.getProperty(idProperty) == null) {

							rel.unlockSystemPropertiesOnce();
							rel.setProperty(idProperty, NodeServiceCommand.getNextUuid());
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

			return count;
		}

		info("Unable to determine entity type to set UUID.");
		return 0;
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
