/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
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

import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractRelationship;

/**
 *
 * @author amorgner
 */
public class RelationshipFactoryCommand extends NodeServiceCommand {

    private static final Logger logger = Logger.getLogger(RelationshipFactoryCommand.class.getName());

    @Override
    public Object execute(Object... parameters) throws FrameworkException {

        GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
        RelationshipFactory relFactory = (RelationshipFactory) arguments.get("relationshipFactory");
	
        Object ret = null;

        if (relFactory != null) {
            if (parameters.length > 0) {
                if (parameters.length > 1) {

                    // create multiple relationships and return a Collection of it
                    Collection<AbstractRelationship> collection = new LinkedList<AbstractRelationship>();

                    for (Object o : parameters) {
                        Relationship rel = null;
                        if (o instanceof AbstractRelationship) {
                            rel = graphDb.getRelationshipById(((AbstractRelationship) o).getId());
                        } else if (o instanceof Relationship) {
                            rel = (Relationship) o;
                        } else {
                            logger.log(Level.WARNING, "Unknown parameter of type {0}", o.getClass().getName());
                            return null;
                        }
                        collection.add(relFactory.createRelationship(securityContext, rel));
                    }

                    ret = collection;

                } else {

                    // create a single node and return it
                    Relationship rel = null;
                    if (parameters[0] instanceof AbstractRelationship) {

                        rel = graphDb.getRelationshipById(((AbstractRelationship) parameters[0]).getId());

                    } else if (parameters[0] instanceof Relationship) {

                        rel = (Relationship) parameters[0];
                        
//                    } else if (parameters[0] instanceof RelationshipDataContainer) {
//
//                        RelationshipDataContainer relData = (RelationshipDataContainer) parameters[0];
//                        return relFactory.createRelationship(securityContext, relData);

                    } else {

                        logger.log(Level.WARNING, "Unknown parameter of type {0}", parameters[0].getClass().getName());
                        return null;
                    }
                    ret = relFactory.createRelationship(securityContext, rel);

                }
            } else {
                logger.log(Level.WARNING, "Invalid number of parameters: {0}", parameters.length);
            }

        } else {
            logger.log(Level.WARNING, "RelationshipFactory argument missing from service");
        }

        return (ret);
    }
}
