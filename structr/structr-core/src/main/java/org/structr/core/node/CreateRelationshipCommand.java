/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.node;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.RelType;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.UnsupportedArgumentError;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;

/**
 * Creates a relationship between two AbstractNode instances. The execute
 * method of this command takes the following parameters.
 * 
 * @param startNode the start node
 * @param endNode the end node
 * @param type the name of relationship type to create
 *
 * @return the new relationship
 *
 * @author cmorgner
 */
public class CreateRelationshipCommand extends NodeServiceCommand {

    private static final Logger logger = Logger.getLogger(CreateRelationshipCommand.class.getName());

    @Override
    public Object execute(Object... parameters) {

        GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");

        if (graphDb != null && parameters.length == 3) {

            Object arg0 = parameters[0]; // start node
            Object arg1 = parameters[1]; // end node
            Object arg2 = parameters[2]; // relationship type

            RelationshipType relType = null;
            if (arg2 instanceof String) {
                relType = DynamicRelationshipType.withName((String) arg2);
            } else if (arg2 instanceof RelationshipType) {
                relType = (RelationshipType) arg2;
            } else {
                throw new UnsupportedArgumentError("Wrong argument type(s).");
            }


            if (arg0 instanceof AbstractNode && arg1 instanceof AbstractNode) {

                AbstractNode startNode = (AbstractNode) arg0;
                AbstractNode endNode = (AbstractNode) arg1;

                Node node1 = graphDb.getNodeById(startNode.getId());
                Node node2 = graphDb.getNodeById(endNode.getId());

                return createRelationship(node1, node2, relType);

            } else {
                throw new UnsupportedArgumentError("Wrong argument type(s).");
            }

        } else if (graphDb != null && parameters.length == 4) {

            Object arg0 = parameters[0]; // start node
            Object arg1 = parameters[1]; // end node
            Object arg2 = parameters[2]; // relationship type
            Object arg3 = parameters[3]; // check duplicates

            RelationshipType relType = null;
            if (arg2 instanceof String) {
                relType = DynamicRelationshipType.withName((String) arg2);
            } else if (arg2 instanceof RelationshipType) {
                relType = (RelationshipType) arg2;
            } else {
                throw new UnsupportedArgumentError("Wrong argument type(s).");
            }

            boolean checkDuplicates = false;
            if (arg3 instanceof Boolean) {
                checkDuplicates = ((Boolean) arg3) == true;
            }

            if (arg0 instanceof AbstractNode && arg1 instanceof AbstractNode) {

                AbstractNode startNode = (AbstractNode) arg0;
                AbstractNode endNode = (AbstractNode) arg1;

                if (checkDuplicates) {

                    List<StructrRelationship> incomingRels = endNode.getIncomingLinkRelationships();

                    for (StructrRelationship rel : incomingRels) {
                        if (rel.getRelType().equals(relType) && rel.getStartNode().equals(startNode)) {
                            logger.log(Level.WARNING, "Creation of duplicate relationship was blocked");
                            return null;
                        }
                    }

                }

                Node node1 = graphDb.getNodeById(startNode.getId());
                Node node2 = graphDb.getNodeById(endNode.getId());

                return createRelationship(node1, node2, relType);

            } else {
                throw new UnsupportedArgumentError("Wrong argument type(s).");
            }
        }

        return null;
    }

    
    private StructrRelationship createRelationship(final Node fromNode, final Node toNode, final RelationshipType relType) {

        final Command transactionCommand = Services.command(TransactionCommand.class);
        StructrRelationship newRelationship = (StructrRelationship) transactionCommand.execute(new StructrTransaction() {

            @Override
            public Object execute() throws Throwable {
                return new StructrRelationship(fromNode.createRelationshipTo(toNode, relType));
            }
        });

        return newRelationship;
    }

}
