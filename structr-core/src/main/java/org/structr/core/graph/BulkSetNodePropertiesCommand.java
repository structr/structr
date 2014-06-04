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
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.property.PropertyKey;
import org.structr.schema.SchemaHelper;

//~--- classes ----------------------------------------------------------------

/**
 * Sets the properties found in the property set on all nodes matching the type.
 * If no type property is found, set the properties on all nodes.
 *
 * @author Axel Morgner
 */
public class BulkSetNodePropertiesCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = Logger.getLogger(BulkSetNodePropertiesCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void execute(final Map<String, Object> properties) throws FrameworkException {

		final GraphDatabaseService graphDb     = (GraphDatabaseService) arguments.get("graphDb");
		final SecurityContext superUserContext = SecurityContext.getSuperUserInstance();
		final NodeFactory nodeFactory          = new NodeFactory(superUserContext);

		final String type = (String)properties.get("type");
		if (StringUtils.isBlank(type)) {
			throw new FrameworkException(422, "Type must not be empty");
		}

		final Class cls = SchemaHelper.getEntityClassForRawType(type);
		if (cls == null) {

			throw new FrameworkException(422, "Invalid type " + type);
		}

		// remove "type" so it won't be set later
		properties.remove("type");


		if (graphDb != null) {

			Result<AbstractNode> nodes = null;

			if (properties.containsKey(AbstractNode.type.dbName())) {

				try (final Tx tx = StructrApp.getInstance().tx()) {
					nodes = StructrApp.getInstance(securityContext).nodeQuery(cls).getResult();
				}

				properties.remove(AbstractNode.type.dbName());

			} else {

				try (final Tx tx = StructrApp.getInstance().tx()) {
					nodes = nodeFactory.instantiateAll(GlobalGraphOperations.at(graphDb).getAllNodes());
				}
			}

			long nodeCount  = bulkGraphOperation(securityContext, nodes.getResults(), 1000, "SetNodeProperties", new BulkGraphOperation<AbstractNode>() {

				@Override
				public void handleGraphObject(SecurityContext securityContext, AbstractNode node) {

					// Treat only "our" nodes
					if (node.getProperty(GraphObject.id) != null) {

						for (Entry entry : properties.entrySet()) {

							String key = (String) entry.getKey();
							Object val = null;

							// allow to set new type
							if (key.equals("newType")) {
								key = "type";
							}

							PropertyConverter inputConverter = StructrApp.getConfiguration().getPropertyKeyForJSONName(cls, key).inputConverter(securityContext);


							if (inputConverter != null) {
								try {
									val = inputConverter.convert(entry.getValue());
								} catch (FrameworkException ex) {
									logger.log(Level.SEVERE, null, ex);
								}

							} else {
								val = entry.getValue();
							}

							PropertyKey propertyKey = StructrApp.getConfiguration().getPropertyKeyForDatabaseName(node.getClass(), key);
							if (propertyKey != null) {

								try {
									node.unlockReadOnlyPropertiesOnce();
									node.setProperty(propertyKey, val);

								} catch (FrameworkException fex) {

									logger.log(Level.WARNING, "Unable to set node property {0} of node {1} to {2}: {3}", new Object[] { propertyKey, node.getUuid(), val, fex.getMessage() } );

								}
							}

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
		}

		logger.log(Level.INFO, "Done");
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}
}
