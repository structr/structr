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
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.graph.Node;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.PropertyKey;

/**
 * Change the property key from the old to the new value on all nodes matching the type.
 *
 * Example: "email":"foo@bar.com" => "eMail":"foo@bar.com"
 *
 * If no type property is found, change the property key on all nodes.
 * If a property with the new key is already present, the command will abort.
 *
 *
 */
public class BulkChangeNodePropertyKeyCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = LoggerFactory.getLogger(BulkChangeNodePropertyKeyCommand.class.getName());

	@Override
	public void execute(final Map<String, Object> properties) throws FrameworkException {

		final DatabaseService graphDb          = (DatabaseService) arguments.get("graphDb");
		final SecurityContext superUserContext = SecurityContext.getSuperUserInstance();
		final NodeFactory nodeFactory          = new NodeFactory(superUserContext);

		String type		= null;
		final String oldKey	= (String) properties.get("oldKey");
		final String newKey	= (String) properties.get("newKey");

		if (graphDb != null && StringUtils.isNotBlank(oldKey) && StringUtils.isNotBlank(newKey)) {

			Iterable<AbstractNode> nodes = null;

			if (properties.containsKey(AbstractNode.type.dbName())) {

				type = (String) properties.get(AbstractNode.type.dbName());

				nodes = Iterables.map(nodeFactory, graphDb.getNodesByLabel(type));

				properties.remove(AbstractNode.type.dbName());

			} else {

				nodes = Iterables.map(nodeFactory, graphDb.getAllNodes());
			}

			final long count = bulkGraphOperation(securityContext, nodes, 1000, "ChangeNodePropertyKey", new BulkGraphOperation<AbstractNode>() {

				@Override
				public boolean handleGraphObject(SecurityContext securityContext, AbstractNode node) {

					for (Entry entry : properties.entrySet()) {

						String key = (String) entry.getKey();

						PropertyKey propertyKey = StructrApp.getConfiguration().getPropertyKeyForDatabaseName(node.getClass(), key);
						if (propertyKey != null) {

							Node dbNode = node.getNode();

							if (dbNode.hasProperty(newKey)) {

								logger.error("Node {} has already a property with key {}", new Object[] { node, newKey });
								throw new IllegalStateException("Node has already a property of the new key");

							}

							if (dbNode.hasProperty(oldKey)) {

								dbNode.setProperty(newKey, dbNode.getProperty(oldKey));
								dbNode.removeProperty(oldKey);

							}
						}
					}

					return true;
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


			logger.info("Fixed {} nodes ...", count);

		} else {

			logger.info("No values for oldKey and/or newKey found, aborting.");

		}

		logger.info("Done");
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}

	@Override
	public boolean requiresFlushingOfCaches() {
		return false;
	}
}
