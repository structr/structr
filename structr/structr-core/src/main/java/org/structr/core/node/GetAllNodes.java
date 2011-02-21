/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.node;

import java.util.Collections;
import org.neo4j.graphdb.GraphDatabaseService;

/**
 * Get all nodes in the database
 *
 * @author amorgner
 */
public class GetAllNodes extends NodeServiceCommand {

    @Override
    public Object execute(Object... parameters) {

        GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
        StructrNodeFactory nodeFactory = (StructrNodeFactory) arguments.get("nodeFactory");

        if (graphDb != null) {
            return nodeFactory.createNodes(graphDb.getAllNodes());
        }
        
        return Collections.emptyList();
    }
}
