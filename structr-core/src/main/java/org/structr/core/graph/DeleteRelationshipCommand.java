/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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



package org.structr.core.graph;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;

import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.UnsupportedArgumentError;
import org.structr.core.entity.AbstractRelationship;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;

//~--- classes ----------------------------------------------------------------

/**
 * Deletes the relationship supplied as a parameter.
 *
 * @author Christian Morgner
 */
public class DeleteRelationshipCommand extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(DeleteRelationshipCommand.class.getName());

	//~--- methods --------------------------------------------------------

	public Object execute(Object... parameters) throws FrameworkException {

		RelationshipFactory relationshipFactory  = (RelationshipFactory) arguments.get("relationshipFactory");
		GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
		Object ret                   = null;

		if (graphDb != null) {

			switch (parameters.length) {

				case 0 :

					throw new UnsupportedArgumentError("No arguments supplied");

				case 1 :
					return (handleSingleArgument(graphDb, relationshipFactory, parameters[0]));

				default :

					throw new UnsupportedArgumentError("Too many arguments supplied");


			}

		}

		return ret;
	}

	// <editor-fold defaultstate="collapsed" desc="private methods">
	private Object handleSingleArgument(GraphDatabaseService graphDb, RelationshipFactory relationshipFactory, Object argument) throws FrameworkException {


		AbstractRelationship rel = null;

		if (argument instanceof Long) {

			// single long value: find relationship by id
			long id = ((Long) argument).longValue();

			try {
				rel = relationshipFactory.instantiateRelationship(securityContext, (Relationship) graphDb.getRelationshipById(id));
			} catch (NotFoundException nfe) {
				logger.log(Level.SEVERE, "Relationship {0} not found, cannot delete.", id);
			}
		} else if (argument instanceof AbstractRelationship) {

			rel = (AbstractRelationship) argument;

		} else if (argument instanceof Relationship) {

			rel = relationshipFactory.instantiateRelationship(securityContext, (Relationship) argument);

		}

		if (rel != null) {

			if (rel.getProperty(AbstractRelationship.uuid) == null) {

				logger.log(Level.WARNING, "Will not delete relationship which has no UUID: {0} --[:{1}]-->{2}", new Object[] { rel.getStartNode(), rel.getType(), rel.getEndNode() });

				return null;

			}

			final RemoveRelationshipFromIndex removeRel = Services.command(SecurityContext.getSuperUserInstance(), RemoveRelationshipFromIndex.class);
			final Relationship relToDelete              = rel.getRelationship();
			final AbstractRelationship finalRel         = rel;

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					try {

						// remove object from index
						removeRel.execute(finalRel);

						// delete node in database
						relToDelete.delete();
						
					} catch (IllegalStateException ise) {
						logger.log(Level.WARNING, ise.getMessage());
					}

					return null;
				}

			});

		}

		return null;
	}

	// </editor-fold>
}
