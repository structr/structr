/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.graph;

import org.neo4j.graphdb.*;

import org.structr.common.error.FrameworkException;
import org.structr.common.error.IdNotFoundToken;

//import org.structr.common.xpath.JXPathFinder;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.entity.AbstractRelationship;

//~--- classes ----------------------------------------------------------------

/**
 * Searches for a relationship in the database and return the result.
 *
 *
 */
public class FindRelationshipCommand<T extends AbstractRelationship> extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(FindRelationshipCommand.class.getName());

	//~--- methods --------------------------------------------------------

	public T execute(Relationship relationship) throws FrameworkException {

		GraphDatabaseService graphDb               = (GraphDatabaseService) arguments.get("graphDb");
		RelationshipFactory<T> relationshipFactory = new RelationshipFactory<T>(securityContext);

		if (graphDb != null) {

			return relationshipFactory.instantiate(relationship);
		}

		return (null);
	}

	public T execute(Long id) throws FrameworkException {

		GraphDatabaseService graphDb               = (GraphDatabaseService) arguments.get("graphDb");
		RelationshipFactory<T> relationshipFactory = new RelationshipFactory<T>(securityContext);

		if (graphDb != null) {

			try {

				return relationshipFactory.instantiate(graphDb.getRelationshipById(id));

			} catch (NotFoundException nfe) {

				logger.log(Level.WARNING, "Node with id {0} not found in database!", id);

				throw new FrameworkException(404, new IdNotFoundToken("FindRelationshipCommand", id));
			}
		}

		return null;
	}

	public T execute(String idString) throws FrameworkException {

		GraphDatabaseService graphDb               = (GraphDatabaseService) arguments.get("graphDb");
		RelationshipFactory<T> relationshipFactory = new RelationshipFactory<>(securityContext);

		if (graphDb != null) {

			// single string value, try to parse to long
			try {

				return relationshipFactory.instantiate(graphDb.getRelationshipById(Long.parseLong(idString)));

			} catch (NumberFormatException ex) {

				// failed :(
				logger.log(Level.FINE, "Could not parse {0} to number", idString);
			}
		}

		return null;
	}
}
