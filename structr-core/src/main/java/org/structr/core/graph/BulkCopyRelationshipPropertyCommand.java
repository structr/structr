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

import java.util.LinkedList;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.tooling.GlobalGraphOperations;

import org.structr.common.error.FrameworkException;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.property.PropertyKey;

//~--- classes ----------------------------------------------------------------
/**
 * Sets the properties found in the property set on all nodes matching the type.
 * If no type property is found, set the properties on all nodes.
 *
 * @author Axel Morgner
 */
public class BulkCopyRelationshipPropertyCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = Logger.getLogger(BulkCopyRelationshipPropertyCommand.class.getName());

	@Override
	public void execute(final Map<String, Object> map) throws FrameworkException {

		final GraphDatabaseService graphDb   = (GraphDatabaseService)arguments.get("graphDb");
		final RelationshipFactory relFactory = new RelationshipFactory(securityContext);

		final String sourceKey = (String)map.get("sourceKey");
		final String destKey   = (String)map.get("destKey");

		if(sourceKey == null || destKey == null) {

			throw new IllegalArgumentException("This command requires one argument of type Map. Map must contain values for 'sourceKey' and 'destKey'.");

		}

		if(graphDb != null) {

			List<AbstractRelationship> rels = new LinkedList<>();

			try (final Tx tx = StructrApp.getInstance().tx()) {
				rels.addAll(relFactory.instantiate(GlobalGraphOperations.at(graphDb).getAllRelationships()));
			}

			long count = bulkGraphOperation(securityContext, rels, 1000, "CopyRelationshipProperties", new BulkGraphOperation<AbstractRelationship>() {

				@Override
				public void handleGraphObject(SecurityContext securityContext, AbstractRelationship rel) {

					// Treat only "our" rels
					if(rel.getProperty(GraphObject.id) != null) {

						Class type                    = rel.getClass();
						PropertyKey destPropertyKey   = StructrApp.getConfiguration().getPropertyKeyForDatabaseName(type, destKey);
						PropertyKey sourcePropertyKey = StructrApp.getConfiguration().getPropertyKeyForDatabaseName(type, sourceKey);

						try {
							// copy properties
							rel.setProperty(destPropertyKey, rel.getProperty(sourcePropertyKey));

						} catch (FrameworkException fex) {

							logger.log(Level.WARNING, "Unable to copy relationship property {0} of relationship {1} to {2}: {3}", new Object[] { sourcePropertyKey, rel.getUuid(), destPropertyKey, fex.getMessage() } );
						}
					}
				}

				@Override
				public void handleThrowable(SecurityContext securityContext, Throwable t, AbstractRelationship rel) {
					logger.log(Level.WARNING, "Unable to copy relationship properties of relationship {0}: {1}", new Object[] { rel.getUuid(), t.getMessage() } );
				}

				@Override
				public void handleTransactionFailure(SecurityContext securityContext, Throwable t) {
					logger.log(Level.WARNING, "Unable to copy relationship properties: {0}", t.getMessage() );
				}
			});

			logger.log(Level.INFO, "Finished setting properties on {0} nodes", count);

		}
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}
}
