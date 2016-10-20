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
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;


//~--- classes ----------------------------------------------------------------

/**
 * Sets a new UUID on each graph object of the given type. For nodes, set type,
 * for relationships relType.
 *
 *
 */
public class BulkSetUuidCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = LoggerFactory.getLogger(BulkSetUuidCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void execute(Map<String, Object> attributes) throws FrameworkException {

		final String nodeType         = (String) attributes.get("type");
		final String relType          = (String) attributes.get("relType");
		final Boolean allNodes        = (Boolean) attributes.get("allNodes");
		final Boolean allRels         = (Boolean) attributes.get("allRels");
		final DatabaseService graphDb = (DatabaseService) arguments.get("graphDb");

		final SecurityContext superUserContext = SecurityContext.getSuperUserInstance();
		final NodeFactory nodeFactory          = new NodeFactory(superUserContext);
		final RelationshipFactory relFactory   = new RelationshipFactory(superUserContext);

		if (nodeType != null || Boolean.TRUE.equals(allNodes)) {

			Iterator<AbstractNode> nodeIterator = null;

			try (final Tx tx = StructrApp.getInstance().tx()) {

				if (Boolean.TRUE.equals(allNodes)) {

					nodeIterator = Iterables.map(nodeFactory, Iterables.filter(new StructrAndSpatialPredicate(false, false, true), graphDb.getAllNodes())).iterator();

					info("Start setting UUID on all nodes");

				} else {

					nodeIterator = Iterables.filter(new TypePredicate<>(nodeType), Iterables.map(nodeFactory, Iterables.filter(new StructrAndSpatialPredicate(false, false, true), graphDb.getAllNodes()))).iterator();

					info("Start setting UUID on nodes of type {}", new Object[] { nodeType });
				}

				tx.success();

			} catch (FrameworkException fex) {
				logger.warn("Exception while creating all nodes iterator.", fex);
			}

			final long count = bulkGraphOperation(securityContext, nodeIterator, 1000, "SetNodeUuid", new BulkGraphOperation<AbstractNode>() {

				@Override
				public void handleGraphObject(final SecurityContext securityContext, final AbstractNode node) {

					try {
						node.unlockSystemPropertiesOnce();
						node.setProperty(GraphObject.id, NodeServiceCommand.getNextUuid());

					} catch (FrameworkException fex) {

						logger.warn("Unable to set UUID of node {}: {}", new Object[] { node, fex.getMessage() });
						fex.printStackTrace();
					}
				}

				@Override
				public void handleThrowable(SecurityContext securityContext, Throwable t, AbstractNode node) {
					logger.warn("Unable to set UUID of node {}: {}", new Object[] { node, t.getMessage() });
					t.printStackTrace();
				}

				@Override
				public void handleTransactionFailure(SecurityContext securityContext, Throwable t) {
					logger.warn("Unable to set UUID on node: {}", t.getMessage());
					t.printStackTrace();
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

			Iterator<AbstractRelationship> relIterator = null;

			try (final Tx tx = StructrApp.getInstance().tx()) {

				if (Boolean.TRUE.equals(allRels)) {

					relIterator = Iterables.map(relFactory,Iterables.filter(new StructrAndSpatialPredicate(false, false, true), graphDb.getAllRelationships())).iterator();

					info("Start setting UUID on all rels", new Object[] { relType });

				} else {

					relIterator = Iterables.filter(new TypePredicate<>(relType), Iterables.map(relFactory,Iterables.filter(new StructrAndSpatialPredicate(false, false, true), graphDb.getAllRelationships()))).iterator();

					info("Start setting UUID on rels of type {}", new Object[] { relType });
				}

				tx.success();

			} catch (FrameworkException fex) {
				logger.warn("Exception while creating all nodes iterator.", fex);
			}

			final long count = bulkGraphOperation(securityContext, relIterator, 1000, "SetRelationshipUuid", new BulkGraphOperation<AbstractRelationship>() {

				@Override
				public void handleGraphObject(SecurityContext securityContext, AbstractRelationship rel) {

					try {
						rel.unlockSystemPropertiesOnce();
						rel.setProperty(GraphObject.id, NodeServiceCommand.getNextUuid());

					} catch (FrameworkException fex) {

						logger.warn("Unable to set UUID of relationship {}: {}", new Object[] { rel, fex.getMessage() });
					}
				}

				@Override
				public void handleThrowable(SecurityContext securityContext, Throwable t, AbstractRelationship rel) {
					logger.warn("Unable to set UUID of relationship {}: {}", new Object[] { rel, t.getMessage() });
				}

				@Override
				public void handleTransactionFailure(SecurityContext securityContext, Throwable t) {
					logger.warn("Unable to set UUID on relationship: {}", t.getMessage());
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
}
