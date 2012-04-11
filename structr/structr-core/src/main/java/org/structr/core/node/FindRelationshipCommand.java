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

import org.neo4j.graphdb.*;

import org.structr.common.error.FrameworkException;
import org.structr.common.error.IdNotFoundToken;
import org.structr.core.UnsupportedArgumentError;

//import org.structr.common.xpath.JXPathFinder;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Searches for a relationship in the database and return the result.
 *
 * @author axel
 */
public class FindRelationshipCommand extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(FindRelationshipCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public Object execute(Object... parameters) throws FrameworkException {

		GraphDatabaseService graphDb            = (GraphDatabaseService) arguments.get("graphDb");
		RelationshipFactory relationshipFactory = (RelationshipFactory) arguments.get("relationshipFactory");

		if (graphDb != null) {

			switch (parameters.length) {

				case 0 :
					throw new UnsupportedArgumentError("No arguments supplied");

				case 1 :
					return (handleSingleArgument(graphDb, relationshipFactory, parameters[0]));

				default :
					throw new UnsupportedArgumentError("Too many argmuments supplied");

			}

		}

		return (null);
	}

	// <editor-fold defaultstate="collapsed" desc="private methods">
	private Object handleSingleArgument(GraphDatabaseService graphDb, RelationshipFactory relationshipFactory, Object argument) throws FrameworkException {

		Object result = null;

		if (argument instanceof Relationship) {

			result = relationshipFactory.createRelationship(securityContext, (Relationship) argument);

		} else if (argument instanceof Long) {

			// single long value: find node by id
			long id          = ((Long) argument).longValue();
			Relationship rel = null;

			try {

				rel    = graphDb.getRelationshipById(id);
				result = relationshipFactory.createRelationship(securityContext, rel);

			} catch (NotFoundException nfe) {

				logger.log(Level.WARNING, "Node with id {0} not found in database!", id);

				throw new FrameworkException("FindRelationshipCommand", new IdNotFoundToken(id));
			}
		} else if (argument instanceof String) {

			// single string value, try to parse to long
			try {

				long id          = Long.parseLong((String) argument);
				Relationship rel = graphDb.getRelationshipById(id);

				result = relationshipFactory.createRelationship(securityContext, rel);

			} catch (NumberFormatException ex) {

				// failed :(
				logger.log(Level.FINE, "Could not parse {0} to number", argument);
			}
		}

		return result;
	}

	// </editor-fold>
}
