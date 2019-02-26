/**
 * Copyright (C) 2010-2019 Structr GmbH
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
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.PropertyKey;
import org.structr.schema.SchemaHelper;

/**
 * Sets the properties found in the property set on all nodes matching the type.
 * If no type property is found, set the properties on all nodes.
 *
 *
 */
public class BulkSetNodePropertiesCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = LoggerFactory.getLogger(BulkSetNodePropertiesCommand.class.getName());

	@Override
	public void execute(final Map<String, Object> properties) throws FrameworkException {

		final DatabaseService graphDb          = (DatabaseService) arguments.get("graphDb");
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

		if (graphDb != null) {

			final App app                       = StructrApp.getInstance(securityContext);
			Iterator<AbstractNode> nodeIterator = null;

			if (properties.containsKey(AbstractNode.type.dbName())) {

				try (final Tx tx = app.tx()) {

					nodeIterator = app.nodeQuery(cls).getResultStream().iterator();
					properties.remove(AbstractNode.type.dbName());

					tx.success();
				}

			} else {

				nodeIterator = Iterables.map(nodeFactory, graphDb.getAllNodes()).iterator();
			}

			// remove "type" so it won't be set later
			properties.remove("type");

			final long count = bulkGraphOperation(securityContext, nodeIterator, 1000, "SetNodeProperties", new BulkGraphOperation<AbstractNode>() {

				@Override
				public boolean handleGraphObject(SecurityContext securityContext, AbstractNode node) {

					// Treat only "our" nodes
					if (node.getProperty(GraphObject.id) != null) {

						for (Entry entry : properties.entrySet()) {

							String key = (String) entry.getKey();
							Object val = null;

							// allow to set new type
							if (key.equals("newType")) {
								key = "type";
							}

							PropertyConverter inputConverter = StructrApp.key(cls, key).inputConverter(securityContext);


							if (inputConverter != null) {
								try {
									val = inputConverter.convert(entry.getValue());
								} catch (FrameworkException ex) {
									logger.error("", ex);
								}

							} else {
								val = entry.getValue();
							}

							PropertyKey propertyKey = StructrApp.getConfiguration().getPropertyKeyForDatabaseName(node.getClass(), key);
							if (propertyKey != null) {

								try {
									node.unlockSystemPropertiesOnce();
									node.setProperty(propertyKey, val);

								} catch (FrameworkException fex) {

									logger.warn("Unable to set node property {} of node {} to {}: {}", new Object[] { propertyKey, node.getUuid(), val, fex.getMessage() } );

								}
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
