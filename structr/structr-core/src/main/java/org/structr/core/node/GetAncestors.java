/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
import org.structr.core.UnsupportedArgumentError;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 * Get all nodes in the database
 *
 * @author amorgner
 */
public class GetAncestors extends NodeServiceCommand {

    private static final Logger logger = Logger.getLogger(GetAncestors.class.getName());

    @Override
    public Object execute(Object... parameters) {

        GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
        StructrNodeFactory nodeFactory = (StructrNodeFactory) arguments.get("nodeFactory");

        if (graphDb != null) {

            User user = null;

            if (graphDb != null) {
                switch (parameters.length) {
                    case 0:
                        throw new UnsupportedArgumentError("No arguments supplied");

                    case 2:
                        if (parameters[0] instanceof User) {
                            user = (User) parameters[0];
                        }
                        return getAncestors(graphDb, nodeFactory, user, parameters[1]);

                    default:
                        
                }
            }

        }

        return Collections.emptyList();
    }

    private List<AbstractNode> getAncestors(GraphDatabaseService graphDb, StructrNodeFactory nodeFactory, User user, Object argument) {

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

        return nodeFactory.createNodes(nodes, user, false);
    }
}
