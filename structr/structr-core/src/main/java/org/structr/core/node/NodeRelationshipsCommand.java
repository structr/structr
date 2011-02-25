/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.node;

import java.util.LinkedList;
import java.util.List;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.AbstractNode;

/**
 * Returns a List of Relationships for the given node.
 * 
 * @param one or more AbstractNode instances to collect the properties from.
 * @return a list of relationships for the given nodes
 *
 * @author amorgner
 */
public class NodeRelationshipsCommand extends NodeServiceCommand {

    /**
     * First argument is the AbstractNode to get relationships for.
     * Second argument is relationship type {@see RelationshipType} (can be null)
     * Third argument is direction {@see Direction}
     *
     * @param parameters
     * @return list with relationships that match relationship type and direction
     */
    @Override
    public Object execute(Object... parameters) {

        GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");

        // Avoid to return null
        List<StructrRelationship> result = new LinkedList<StructrRelationship>();

        if (parameters.length == 3) {

            Object arg0 = parameters[0];
            Object arg1 = parameters[1];
            Object arg2 = parameters[2];

            AbstractNode sNode = (AbstractNode) arg0;
            RelationshipType relType = (RelationshipType) arg1;
            Direction dir = (Direction) arg2;

            Node node = graphDb.getNodeById(sNode.getId());

            Iterable<Relationship> rels;
            if (arg1 != null) {
                rels = node.getRelationships(relType, dir);
            } else {
                rels = node.getRelationships(dir);
            }

            for (Relationship r : rels) {
                result.add(new StructrRelationship(r));
            }
        }

        return result;
    }
}
