/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.node;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.structr.core.UnsupportedArgumentError;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;

/**
 * Creates a relationship between two AbstractNode instances. The execute
 * method of this command takes the following parameters. Note that this command
 * does not run in a transaction.
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

                return new StructrRelationship(node1.createRelationshipTo(node2, relType));

            } else {
                throw new UnsupportedArgumentError("Wrong argument type(s).");
            }
        }

        return (null);
    }
}
