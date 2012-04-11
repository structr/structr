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
import org.neo4j.graphdb.Node;
import org.structr.common.error.FrameworkException;
import org.structr.core.cloud.NodeDataContainer;
import org.structr.core.entity.AbstractNode;

/**
 *
 * @author cmorgner
 */
public class NodeFactoryCommand extends NodeServiceCommand {

    private static final Logger logger = Logger.getLogger(NodeFactoryCommand.class.getName());

    @Override
    public Object execute(Object... parameters) throws FrameworkException {

        GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");

        NodeFactory nodeFactory = (NodeFactory) arguments.get("nodeFactory");
        Object ret = null;

        if (nodeFactory != null) {
            if (parameters.length > 0) {
                if (parameters.length > 1) {

                    // create multiple nodes and return a Collection of it
                    Collection<AbstractNode> collection = new LinkedList<AbstractNode>();

                    for (Object o : parameters) {
                        Node node = null;
                        if (o instanceof AbstractNode) {
                            node = graphDb.getNodeById(((AbstractNode) o).getId());
                        } else if (o instanceof Node) {
                            node = (Node) o;
                        } else {
                            logger.log(Level.WARNING, "Unknown parameter of type {0}", o.getClass().getName());
                            return null;
                        }
                        collection.add(nodeFactory.createNode(securityContext, node));
                    }

                    ret = collection;

                } else {

                    // create a single node and return it
                    Node node = null;
                    if (parameters[0] instanceof AbstractNode) {

                        node = graphDb.getNodeById(((AbstractNode) parameters[0]).getId());

                    } else if (parameters[0] instanceof Node) {

                        node = (Node) parameters[0];
                        
                    } else if (parameters[0] instanceof NodeDataContainer) {

                        NodeDataContainer nodeData = (NodeDataContainer) parameters[0];
                        return nodeFactory.createNode(securityContext, nodeData);

                    } else {

                        logger.log(Level.WARNING, "Unknown parameter of type {0}", parameters[0].getClass().getName());
                        return null;
                    }
                    ret = nodeFactory.createNode(securityContext, node);

                }
            } else {
                logger.log(Level.WARNING, "Invalid number of parameters: {0}", parameters.length);
            }

        } else {
            logger.log(Level.WARNING, "NodeFactory argument missing from service");
        }

        return (ret);
    }
}
