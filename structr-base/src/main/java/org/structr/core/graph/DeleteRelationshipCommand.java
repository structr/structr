/*
 * Copyright (C) 2010-2025 Structr GmbH
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

/**
 * Deletes the relationship supplied as a parameter.
 *
 */
public class DeleteRelationshipCommand extends NodeServiceCommand {

	private static final Logger logger = LoggerFactory.getLogger(DeleteRelationshipCommand.class.getName());

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

		final DatabaseService graphDb = (DatabaseService) arguments.get("graphDb");

		if (graphDb != null && rel != null && !rel.isDeleted()) {

			if (rel.getUuid() == null) {

				logger.warn("Will not delete relationship which has no UUID: {} --[:{}]-->{}", rel.getSourceNode(), rel.getType(), rel.getTargetNode());

				return null;

			}

			final Relationship relToDelete = rel.getRelationship();
			final RelationshipInterface finalRel = rel;

			TransactionCommand.relationshipDeleted(securityContext.getCachedUser(), finalRel, passiveDeletion);

			// delete node in database
			relToDelete.delete(true);
		}

		return null;
	}

	// </editor-fold>
}
