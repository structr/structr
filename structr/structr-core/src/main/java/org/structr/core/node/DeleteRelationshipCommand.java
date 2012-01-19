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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.UnsupportedArgumentError;
import org.structr.core.entity.StructrRelationship;

/**
 *
 * @author cmorgner
 */
public class DeleteRelationshipCommand extends NodeServiceCommand {

    private static final Logger logger = Logger.getLogger(DeleteRelationshipCommand.class.getName());

    @Override
    public Object execute(Object... parameters) throws FrameworkException {
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
    private Object handleSingleArgument(GraphDatabaseService graphDb, Object argument) throws FrameworkException {

        setExitCode(exitCode.FAILURE);

        Relationship rel = null;
        if (argument instanceof Long) {

            // single long value: find relationship by id
            long id = ((Long) argument).longValue();

            try {
                rel = graphDb.getRelationshipById(id);
            } catch (NotFoundException nfe) {
                logger.log(Level.SEVERE, "Relationship {0} not found, cannot delete.", id);
            }

        } else if (argument instanceof StructrRelationship) {

            StructrRelationship r = (StructrRelationship) argument;
            rel = r.getRelationship();

        } else if (argument instanceof Relationship) {

            rel = (Relationship) argument;
        }

        if (rel != null) {


            final Relationship relToDelete = rel;


            final Command transactionCommand = Services.command(securityContext, TransactionCommand.class);
            transactionCommand.execute(new StructrTransaction() {

                @Override
                public Object execute() throws FrameworkException {
                    relToDelete.delete();
                    return null;
                }
            });




            setExitCode(exitCode.SUCCESS);
        }

        return null;
    }
    // </editor-fold>
}
