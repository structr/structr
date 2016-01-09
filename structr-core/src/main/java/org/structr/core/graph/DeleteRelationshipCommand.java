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

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;

import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractRelationship;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Deletes the relationship supplied as a parameter.
 *
 *
 */
public class DeleteRelationshipCommand extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(DeleteRelationshipCommand.class.getName());

	//~--- methods --------------------------------------------------------

	public Object execute(final Relationship rel) throws FrameworkException {

		RelationshipFactory relFactory = new RelationshipFactory(securityContext);

		// default is active deletion!
		return execute(relFactory.instantiate(rel), false);
	}

	public Object execute(final RelationshipInterface rel) {

		// default is active deletion!
		return execute(rel, false);
	}

	public Object execute(final RelationshipInterface rel, final boolean passiveDeletion) {

		GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");

		if (graphDb != null && rel != null) {

			if (rel.getProperty(AbstractRelationship.id) == null) {

				logger.log(Level.WARNING, "Will not delete relationship which has no UUID: {0} --[:{1}]-->{2}", new Object[] { rel.getSourceNode(), rel.getType(), rel.getTargetNode() });

				return null;

			}

			final Relationship relToDelete       = rel.getRelationship();
			final RelationshipInterface finalRel = rel;

			TransactionCommand.relationshipDeleted(securityContext.getCachedUser(), finalRel, passiveDeletion);

			// callback
			finalRel.onRelationshipDeletion();

			// remove object from index
			finalRel.removeFromIndex();

			// delete node in database
			relToDelete.delete();
		}

		return null;
	}

	// </editor-fold>
}
