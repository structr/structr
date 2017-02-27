/**
 * Copyright (C) 2010-2017 Structr GmbH
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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.graph.Relationship;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractRelationship;

//~--- classes ----------------------------------------------------------------

/**
 * Searches for a relationship in the database and return the result.
 *
 *
 */
public class FindRelationshipCommand<T extends AbstractRelationship> extends NodeServiceCommand {

	private static final Logger logger = LoggerFactory.getLogger(FindRelationshipCommand.class.getName());

	//~--- methods --------------------------------------------------------

	public T execute(Relationship relationship) throws FrameworkException {

		DatabaseService graphDb                    = (DatabaseService) arguments.get("graphDb");
		RelationshipFactory<T> relationshipFactory = new RelationshipFactory<>(securityContext);

		if (graphDb != null) {

			return relationshipFactory.instantiate(relationship);
		}

		return (null);
	}

	public T execute(Long id) throws FrameworkException {

		DatabaseService graphDb                    = (DatabaseService) arguments.get("graphDb");
		RelationshipFactory<T> relationshipFactory = new RelationshipFactory<>(securityContext);

		if (graphDb != null) {

			return relationshipFactory.instantiate(graphDb.getRelationshipById(id));
		}

		return null;
	}

	public T execute(String idString) throws FrameworkException {

		DatabaseService graphDb                    = (DatabaseService) arguments.get("graphDb");
		RelationshipFactory<T> relationshipFactory = new RelationshipFactory<>(securityContext);

		if (graphDb != null) {

			// single string value, try to parse to long
			try {

				return relationshipFactory.instantiate(graphDb.getRelationshipById(Long.parseLong(idString)));

			} catch (NumberFormatException ex) {

				// failed :(
				logger.debug("Could not parse {} to number", idString);
			}
		}

		return null;
	}
}
