/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.node;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;
import org.structr.core.UnsupportedArgumentError;
import org.structr.core.entity.StructrRelationship;

/**
 *
 * @author cmorgner
 */
public class DeleteRelationshipCommand extends NodeServiceCommand {

    private static final Logger logger = Logger.getLogger(DeleteRelationshipCommand.class.getName());

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

        setExitCode(exitCode.FAILURE);

        Relationship rel = null;
        if (argument instanceof Long) {

            // single long value: find node by id
            long id = ((Long) argument).longValue();

            try {
                rel = graphDb.getRelationshipById(id);
            } catch (NotFoundException nfe) {
                logger.log(Level.SEVERE, "Relationship {0} not found, unable to delete.", id);
            }

        } else if (argument instanceof StructrRelationship) {

            StructrRelationship r = (StructrRelationship) argument;
            rel = r.getRelationship();

        } else if (argument instanceof Relationship) {

            rel = (Relationship) argument;
        }

        if (rel != null) {
            rel.delete();
            setExitCode(exitCode.SUCCESS);
        }

        return null;
    }
    // </editor-fold>
}
