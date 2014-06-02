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
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.graphdb.Node;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;
import org.structr.schema.SchemaHelper;

//~--- classes ----------------------------------------------------------------

/**
 * Change the property key from the old to the new value on all nodes matching the type.
 *
 * Example: "email":"foo@bar.com" => "eMail":"foo@bar.com"
 *
 * If no type property is found, change the property key on all nodes.
 * If a property with the new key is already present, the command will abort.
 *
 * @author Axel Morgner
 */
public class BulkChangeNodePropertyKeyCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = Logger.getLogger(BulkChangeNodePropertyKeyCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void execute(final Map<String, Object> properties) throws FrameworkException {

		final GraphDatabaseService graphDb     = (GraphDatabaseService) arguments.get("graphDb");
		final SecurityContext superUserContext = SecurityContext.getSuperUserInstance();
		final NodeFactory nodeFactory          = new NodeFactory(superUserContext);

		String type		= null;
		final String oldKey	= (String) properties.get("oldKey");
		final String newKey	= (String) properties.get("newKey");

		if (graphDb != null && StringUtils.isNotBlank(oldKey) && StringUtils.isNotBlank(newKey)) {

			Result<AbstractNode> nodes = null;

			if (properties.containsKey(AbstractNode.type.dbName())) {

				type = (String) properties.get(AbstractNode.type.dbName());

				try (final Tx tx = StructrApp.getInstance().tx()) {
					nodes = StructrApp.getInstance(securityContext).nodeQuery(SchemaHelper.getEntityClassForRawType(type)).getResult();
				}

				properties.remove(AbstractNode.type.dbName());

			} else {

				try (final Tx tx = StructrApp.getInstance().tx()) {
					nodes = nodeFactory.instantiateAll(GlobalGraphOperations.at(graphDb).getAllNodes());
				}
			}

			long nodeCount = bulkGraphOperation(securityContext, nodes.getResults(), 1000, "ChangeNodePropertyKey", new BulkGraphOperation<AbstractNode>() {

				@Override
				public void handleGraphObject(SecurityContext securityContext, AbstractNode node) {

					for (Entry entry : properties.entrySet()) {

						String key = (String) entry.getKey();

						PropertyKey propertyKey = StructrApp.getConfiguration().getPropertyKeyForDatabaseName(node.getClass(), key);
						if (propertyKey != null) {

							Node dbNode = node.getNode();

							if (dbNode.hasProperty(newKey)) {

								logger.log(Level.SEVERE, "Node {0} has already a property with key {1}", new Object[] { node, newKey });
								throw new IllegalStateException("Node has already a property of the new key");

							}

							if (dbNode.hasProperty(oldKey)) {

								dbNode.setProperty(newKey, dbNode.getProperty(oldKey));
								dbNode.removeProperty(oldKey);

							}

							node.updateInIndex();

						}

					}

				}

				@Override
				public void handleThrowable(SecurityContext securityContext, Throwable t, AbstractNode node) {
					logger.log(Level.WARNING, "Unable to set properties of node {0}: {1}", new Object[] { node.getUuid(), t.getMessage() } );
				}

				@Override
				public void handleTransactionFailure(SecurityContext securityContext, Throwable t) {
					logger.log(Level.WARNING, "Unable to set node properties: {0}", t.getMessage() );
				}
			});


			logger.log(Level.INFO, "Fixed {0} nodes ...", nodeCount);

		} else {

			logger.log(Level.INFO, "No values for oldKey and/or newKey found, aborting.");

		}

		logger.log(Level.INFO, "Done");
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}
}
