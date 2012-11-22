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



package org.structr.core.graph;

import java.util.LinkedList;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.tooling.GlobalGraphOperations;

import org.structr.common.error.FrameworkException;
import org.structr.core.Services;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.common.property.GenericProperty;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchRelationshipCommand;

//~--- classes ----------------------------------------------------------------

/**
 * This command takes a property set as parameter.
 * 
 * Sets the properties found in the property set on all relationships matching the type.
 * If no type property is found, set the properties on all relationships.
 *
 * @author Axel Morgner
 */
public class BulkSetRelationshipPropertiesCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = Logger.getLogger(BulkSetRelationshipPropertiesCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void execute(final Map<String, Object> properties) throws FrameworkException {

		final GraphDatabaseService graphDb            = (GraphDatabaseService) arguments.get("graphDb");
		final RelationshipFactory relationshipFactory = new RelationshipFactory(securityContext);
                final SearchRelationshipCommand searchRel     = Services.command(SecurityContext.getSuperUserInstance(), SearchRelationshipCommand.class);
                
		if (graphDb != null) {

			Services.command(securityContext, TransactionCommand.class).execute(new BatchTransaction() {

				@Override
				public Object execute(Transaction tx) throws FrameworkException {

					long n                  = 0L;
                                        List<AbstractRelationship> rels = null;
                                        
                                        if (properties.containsKey(AbstractRelationship.combinedType.dbName())) {
                                                
                                                List<SearchAttribute> attrs = new LinkedList<SearchAttribute>();
                                                attrs.add(Search.andExactType((String) properties.get(AbstractRelationship.combinedType.dbName())));
                                                
                                                rels = (List<AbstractRelationship>) searchRel.execute(attrs);
                                                properties.remove(AbstractRelationship.combinedType.dbName());
                                                
                                        } else {
                                        
                                                rels = (List<AbstractRelationship>) relationshipFactory.instantiateRelationships(securityContext, GlobalGraphOperations.at(graphDb).getAllRelationships());
                                        }

					for (AbstractRelationship rel : rels) {

						// Treat only "our" nodes
						if (rel.getProperty(AbstractRelationship.uuid) != null) {

							for (Entry entry : properties.entrySet()) {
                                                                
                                                                String key = (String) entry.getKey();
                                                                Object val = entry.getValue();
                                                                
								// FIXME: synthetic Property generation
                                                                rel.setProperty(new GenericProperty(key), val);
                                                                
                                                        }

							if (n > 1000 && n % 1000 == 0) {

								logger.log(Level.INFO, "Set properties on {0} relationships, committing results to database.", n);
								tx.success();
								tx.finish();

								tx = graphDb.beginTx();

								logger.log(Level.FINE, "######## committed ########", n);

							}

							n++;

						}
					}
                                        
                                        logger.log(Level.INFO, "Finished setting properties on {0} relationships", n);

					return null;
				}

			});

		}
	}
}
