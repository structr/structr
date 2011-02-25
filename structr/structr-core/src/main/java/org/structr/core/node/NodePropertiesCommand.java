/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.node;

import java.util.LinkedList;
import java.util.List;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.structr.core.entity.Property;
import org.structr.core.entity.AbstractNode;

/**
 * Returns a List of Properties for the given node.
 * 
 * @param one or more AbstractNode instances to collect the properties from.
 * @return a list of Properties for the given nodes
 *
 * @author cmorgner
 */
public class NodePropertiesCommand extends NodeServiceCommand {

    @Override
    public Object execute(Object... parameters) {

        GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");

        List<Property> ret = null;

        if (parameters.length > 0) {
            ret = new LinkedList<Property>();

            for (Object argument : parameters) {
                if (argument instanceof AbstractNode) {
                    AbstractNode structrNode = (AbstractNode) argument;
                    Node node = graphDb.getNodeById(structrNode.getId());

                    for (String key : node.getPropertyKeys()) {
                        // use constructor which gets value from database
                        Property p = new Property(node, key);
                        ret.add(p);
                    }
                }
            }
        }

        return (ret);
    }
}
