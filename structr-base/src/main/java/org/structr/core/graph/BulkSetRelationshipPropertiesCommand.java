/*
 * Copyright (C) 2010-2024 Structr GmbH
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
import org.structr.api.DatabaseService;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Sets the properties found in the property set on all relationships matching the type.
 * If no type property is found, set the properties on all relationships.
 */
public class BulkSetRelationshipPropertiesCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = LoggerFactory.getLogger(BulkSetRelationshipPropertiesCommand.class.getName());

	@Override
	public void execute(final Map<String, Object> properties) throws FrameworkException {

		final DatabaseService graphDb        = (DatabaseService) arguments.get("graphDb");
		final App app                        = StructrApp.getInstance();
		final PropertyKey<String> idProperty = Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.ID_PROPERTY);
		String relationshipTypeClass         = StructrTraits.RELATIONSHIP_INTERFACE;

		if (graphDb != null) {

			final String typeName = "type";

			if (properties.containsKey(typeName)) {

				properties.remove(typeName);

				relationshipTypeClass = typeName;
			}

			final long count = bulkGraphOperation(securityContext, app.relationshipQuery(relationshipTypeClass), 1000, "SetRelationshipProperties", new BulkGraphOperation<AbstractRelationship>() {

				@Override
				public boolean handleGraphObject(SecurityContext securityContext, AbstractRelationship rel) {

					// Treat only "our" nodes
					if (rel.getProperty(idProperty) != null) {

						for (Entry entry : properties.entrySet()) {

							String key = (String) entry.getKey();
							Object val = entry.getValue();

							PropertyKey propertyKey = rel.getTraits().key(key);
							if (propertyKey != null) {

								try {

									rel.setProperty(propertyKey, val);

								} catch (FrameworkException fex) {

									logger.warn("Unable to set relationship property {} of relationship {} to {}: {}", new Object[] { propertyKey, rel.getUuid(), val, fex.getMessage() } );
								}
							}
						}
					}

					return true;
				}

				@Override
				public void handleThrowable(SecurityContext securityContext, Throwable t, AbstractRelationship rel) {
					logger.warn("Unable to set properties of relationship {}: {}", new Object[] { rel.getUuid(), t.getMessage() } );
				}

				@Override
				public void handleTransactionFailure(SecurityContext securityContext, Throwable t) {
					logger.warn("Unable to set relationship properties: {}", t.getMessage() );
				}
			});

			logger.info("Finished setting properties on {} relationships", count);
		}
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
