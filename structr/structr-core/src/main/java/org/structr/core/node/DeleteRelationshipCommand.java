/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.node;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.structr.core.UnsupportedArgumentError;
import org.structr.core.entity.StructrRelationship;

/**
 *
 * @author cmorgner
 */
public class DeleteRelationshipCommand extends NodeServiceCommand {

    @Override
    public Object execute(Object... parameters) {
        GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
        Object ret = null;

        if (graphDb != null) {
            switch (parameters.length) {
                case 0:
                    setExitCode(exitCode.FAILURE);
                    String errorMsg = "No arguments supplied";
                    setErrorMessage(errorMsg);
                    throw new UnsupportedArgumentError(errorMsg);

                case 1:
                    return (handleSingleArgument(graphDb, parameters[0]));

            }
        }
        setExitCode(exitCode.FAILURE);
        setErrorMessage("Too many arguments, or database was null");
        
        return ret;
    }

    // <editor-fold defaultstate="collapsed" desc="private methods">
    private Object handleSingleArgument(GraphDatabaseService graphDb, Object argument) {

        if (argument instanceof Long) {

            // single long value: find node by id
            long id = ((Long) argument).longValue();

            Relationship rel = graphDb.getRelationshipById(id);

            if (rel != null) {
                rel.delete();
            }
        } else if (argument instanceof StructrRelationship) {

            StructrRelationship rel = (StructrRelationship) argument;
            rel.getRelationship().delete();
            
        } else if (argument instanceof Relationship) {

            Relationship rel = (Relationship) argument;
            rel.delete();
        }
        setExitCode(exitCode.SUCCESS);

        return null;
    }
    // </editor-fold>
}
