/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.node;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.tooling.GlobalGraphOperations;

import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.property.Property;
import org.structr.core.entity.AbstractRelationship;

//~--- classes ----------------------------------------------------------------
/**
 * This command takes a property set as parameter.
 *
 * Sets the properties found in the property set on all nodes matching the type. If no type property is found, set the properties on all nodes.
 *
 * @author Axel Morgner
 */
public class BulkCopyRelationshipPropertyCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = Logger.getLogger(BulkCopyRelationshipPropertyCommand.class.getName());

	@Override
	public void execute(final Map<String, Object> map) throws FrameworkException {

		final GraphDatabaseService graphDb   = (GraphDatabaseService)arguments.get("graphDb");
		final RelationshipFactory relFactory = (RelationshipFactory)arguments.get("relationshipFactory");

		final String sourceKey = (String)map.get("sourceKey");
		final String destKey   = (String)map.get("destKey");

		if(sourceKey == null || destKey == null) {

			throw new IllegalArgumentException("This command requires one argument of type Map. Map must contain values for 'sourceKey' and 'destKey'.");

		}
		
		if(graphDb != null) {

			Services.command(securityContext, TransactionCommand.class).execute(new BatchTransaction() {

				@Override
				public Object execute(Transaction tx) throws FrameworkException {

					List<AbstractRelationship> rels = relFactory.createRelationships(securityContext, GlobalGraphOperations.at(graphDb).getAllRelationships());
					long n = 0;
					
					for(AbstractRelationship rel : rels) {

						// Treat only "our" rels
						if(rel.getProperty(AbstractNode.uuid) != null) {

							// copy properties
							// FIXME: synthetic Property generation
							rel.setProperty(new Property(destKey), rel.getProperty(new Property(sourceKey)));
							
							if(n > 1000 && n % 1000 == 0) {

								logger.log(Level.INFO, "Set properties on {0} rels, committing results to database.", n);
								tx.success();
								tx.finish();

								tx = graphDb.beginTx();

								logger.log(Level.FINE, "######## committed ########", n);

							}

							n++;

						}
					}

					logger.log(Level.INFO, "Finished setting properties on {0} nodes", n);

					return null;
				}
			});

		}
	}
}
