/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.node;

import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.structr.core.entity.StructrNode;

/**
 *
 * @author cmorgner
 */
public class NodeFactoryCommand extends NodeServiceCommand {

    private static final Logger logger = Logger.getLogger(NodeFactoryCommand.class.getName());

    @Override
    public Object execute(Object... parameters) {

        GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");

        StructrNodeFactory nodeFactory = (StructrNodeFactory) arguments.get("nodeFactory");
        Object ret = null;

        if (nodeFactory != null) {
            if (parameters.length > 0) {
                if (parameters.length > 1) {

                    // create multiple nodes and return a Collection of it
                    Collection<StructrNode> collection = new LinkedList<StructrNode>();

                    for (Object o : parameters) {
                        Node node = null;
                        if (o instanceof StructrNode) {
                            node = graphDb.getNodeById(((StructrNode) o).getId());
                        } else if (o instanceof Node) {
                            node = (Node) o;
                        } else {
                            logger.log(Level.WARNING, "Unknown parameter of type {0}", o.getClass().getName());
                            return null;
                        }
                        collection.add(nodeFactory.createNode(node));
                    }

                    ret = collection;

                } else {

                    // create a single node and return it
                    Node node = null;
                    if (parameters[0] instanceof StructrNode) {
                        node = graphDb.getNodeById(((StructrNode) parameters[0]).getId());
                    } else if (parameters[0] instanceof Node) {
                        node = (Node) parameters[0];
                    } else {
                        logger.log(Level.WARNING, "Unknown parameter of type {0}", parameters[0].getClass().getName());
                        return null;
                    }
                    ret = nodeFactory.createNode(node);

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
