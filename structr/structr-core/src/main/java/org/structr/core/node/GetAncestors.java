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

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.kernel.Traversal;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.UnsupportedArgumentError;
import org.structr.core.entity.AbstractNode;

/**
 * Get all nodes in the database
 *
 * @author amorgner
 */
public class GetAncestors extends NodeServiceCommand {

    private static final Logger logger = Logger.getLogger(GetAncestors.class.getName());

    @Override
    public Object execute(Object... parameters) throws FrameworkException {

        GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
        StructrNodeFactory nodeFactory = (StructrNodeFactory) arguments.get("nodeFactory");

        if (graphDb != null) {

            SecurityContext context = null;

            if (graphDb != null) {
                switch (parameters.length) {
                    case 0:
                        throw new UnsupportedArgumentError("No arguments supplied");

                    case 2:
                        if (parameters[0] instanceof SecurityContext) {
                            context = (SecurityContext) parameters[0];
                        }
                        return getAncestors(context, graphDb, nodeFactory, parameters[1]);

                    default:
                        
                }
            }

        }

        return Collections.emptyList();
    }

    private List<AbstractNode> getAncestors(SecurityContext securityContext, GraphDatabaseService graphDb, StructrNodeFactory nodeFactory, Object argument) throws FrameworkException {

        Node node = null;
        
        if (argument instanceof Long) {

            // single long value: find node by id
            long id = ((Long) argument).longValue();

            
            try {
                
                node = graphDb.getNodeById(id);

            } catch (NotFoundException nfe) {
                logger.log(Level.WARNING, "Node with id {0} not found in database!", id);
            }

        } else if (argument instanceof String) {
            // single string value, try to parse to long
            try {
                long id = Long.parseLong((String) argument);

                node = graphDb.getNodeById(id);

            } catch (Exception ex) {
                // failed :(
                logger.log(Level.WARNING, "Node with id {0} not found in database! Reason: {1}", new Object[]{argument, ex.getMessage()});
            }
        }

        Iterable<Node> nodes = Traversal.description().relationships(RelType.HAS_CHILD, Direction.INCOMING).traverse(node).nodes();

        return nodeFactory.createNodes(securityContext, nodes, false);
    }
}
