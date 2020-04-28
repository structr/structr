/**
 * Copyright (C) 2010-2020 Structr GmbH
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
import org.structr.api.graph.Node;
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.schema.SchemaHelper;

/**
 * Tries to fix properties in the database that have been stored there with the
 * wrong type.
 *
 *
 */
public class BulkFixNodePropertiesCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = LoggerFactory.getLogger(BulkFixNodePropertiesCommand.class.getName());

	@Override
	public void execute(Map<String, Object> attributes) throws FrameworkException {

		final String propertyName   = (String)attributes.get("name");
		final String entityTypeName = (String)attributes.get("type");

		if (entityTypeName != null) {

			final Class type = SchemaHelper.getEntityClassForRawType(entityTypeName);
			if (type != null) {

				final DatabaseService db                  = StructrApp.getInstance(securityContext).getDatabaseService();
				final NodeFactory factory                 = new NodeFactory(securityContext);
				final Iterable<AbstractNode> nodeIterator = Iterables.map(factory, db.getNodesByLabel(entityTypeName));

				logger.info("Trying to fix properties of all {} nodes", type.getSimpleName() );

				long nodeCount = bulkGraphOperation(securityContext, nodeIterator, 100, "FixNodeProperties", new BulkGraphOperation<AbstractNode>() {

					private void fixProperty(AbstractNode node, Property propertyToFix) {

						Node databaseNode = node.getNode();

						if (databaseNode.hasProperty(propertyToFix.dbName())) {

							// check value with property converter
							PropertyConverter converter = propertyToFix.databaseConverter(securityContext, node);
							if (converter != null) {

								try {
									Object value = databaseNode.getProperty(propertyToFix.dbName());
									converter.revert(value);

								} catch (ClassCastException cce) {

									// exception, needs fix
									String databaseName   = propertyToFix.dbName();
									Object databaseValue  = databaseNode.getProperty(databaseName);
									Object correctedValue = propertyToFix.fixDatabaseProperty(databaseValue);

									if (databaseValue != null && correctedValue != null) {

										try {
											// try to set database value to corrected value
											databaseNode.setProperty(databaseName, correctedValue);

										} catch (Throwable t) {

											logger.warn("Unable to fix property {} of {} with UUID {} which is of type {}", new Object[] {
												propertyToFix.dbName(),
												type.getSimpleName(),
												node.getUuid(),
												databaseValue != null ? databaseValue.getClass() : "null"
											});

										}
									}

								} catch (Throwable t) {

									// log exceptions of other types
									logger.warn("", t);
								}
							}
						}
					}

					@Override
					public boolean handleGraphObject(SecurityContext securityContext, AbstractNode node) {

						if (propertyName != null) {

							PropertyKey key = StructrApp.getConfiguration().getPropertyKeyForDatabaseName(type, propertyName);
							if (key != null) {

								// needs type cast to Property to use fixDatabaseProperty method
								if (key instanceof Property) {
									fixProperty(node, (Property)key);
								}
							}

						} else {

							for(PropertyKey key : node.getPropertyKeys(PropertyView.All)) {

								// needs type cast to Property to use fixDatabaseProperty method
								if (key instanceof Property) {
									fixProperty(node, (Property)key);
								}
							}
						}

						return true;
					}
				});

				logger.info("Fixed {} nodes", nodeCount);

				return;
			}
		}

		logger.info("Unable to determine property and/or entity type to fix.");
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
