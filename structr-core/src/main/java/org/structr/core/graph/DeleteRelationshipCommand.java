/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.core.graph;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;

import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.AbstractRelationship;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.entity.Relation;

//~--- classes ----------------------------------------------------------------

/**
 * Deletes the relationship supplied as a parameter.
 *
 * @author Christian Morgner
 */
public class DeleteRelationshipCommand extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(DeleteRelationshipCommand.class.getName());

	//~--- methods --------------------------------------------------------

	public Object execute(final Relationship rel) throws FrameworkException {
		
		RelationshipFactory relFactory = new RelationshipFactory(securityContext);
		
		// default is active deletion!
		return execute(relFactory.instantiate(rel), false);
	}
	
	public Object execute(final RelationshipInterface rel) throws FrameworkException {
		
		// default is active deletion!
		return execute(rel, false);
	}
	
	public Object execute(final RelationshipInterface rel, final boolean passiveDeletion) throws FrameworkException {

		GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");

		if (graphDb != null && rel != null) {

			if (rel.getProperty(AbstractRelationship.uuid) == null) {

				logger.log(Level.WARNING, "Will not delete relationship which has no UUID: {0} --[:{1}]-->{2}", new Object[] { rel.getSourceNode(), rel.getType(), rel.getTargetNode() });

				return null;

			}

			final Relationship relToDelete       = rel.getRelationship();
			final RelationshipInterface finalRel = rel;

			// logger.log(Level.INFO, "DELETING relationship {0}-[{1}]->{2}", new Object[] {  rel.getSourceNode().getType(), rel.getRelType(), rel.getTargetNode().getType() } );

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					TransactionCommand.relationshipDeleted(finalRel, passiveDeletion);

					try {

						// remove object from index
						finalRel.removeFromIndex();

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
