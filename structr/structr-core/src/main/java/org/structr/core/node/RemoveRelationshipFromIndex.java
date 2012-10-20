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



package org.structr.core.node;

import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;

import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractRelationship;

//~--- JDK imports ------------------------------------------------------------

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Command for removing relationship from the index
 *
 * @author axel
 */
public class RemoveRelationshipFromIndex extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(RemoveRelationshipFromIndex.class.getName());

	//~--- fields ---------------------------------------------------------

	private Map<Enum, Index> indices = new HashMap<Enum, Index>();

	//~--- methods --------------------------------------------------------

	@Override
	public Object execute(Object... parameters) throws FrameworkException {

		Command findRel = Services.command(securityContext, FindRelationshipCommand.class);

		for (Enum indexName : (NodeService.RelationshipIndex[]) arguments.get("relationshipIndices")) {

			indices.put(indexName, (Index<Relationship>) arguments.get(indexName.name()));

		}

		long id                  = 0;
		AbstractRelationship rel = null;

		switch (parameters.length) {

			case 1 :

				// remove this node from index
				if (parameters[0] instanceof Long) {

					id  = ((Long) parameters[0]).longValue();
					rel = (AbstractRelationship) findRel.execute(id);

					removeRelationshipFromAllIndices(rel);

				} else if (parameters[0] instanceof String) {

					id  = Long.parseLong((String) parameters[0]);
					rel = (AbstractRelationship) findRel.execute(id);

					removeRelationshipFromAllIndices(rel);

				} else if (parameters[0] instanceof AbstractRelationship) {

					rel = (AbstractRelationship) parameters[0];

					removeRelationshipFromAllIndices(rel);

				} else if (parameters[0] instanceof List) {

					removeNodesFromAllIndices((List<AbstractRelationship>) parameters[0]);

				}

				break;

			default :
				logger.log(Level.SEVERE, "Wrong number of parameters for the remove relationship from index command: {0}", parameters);

				return null;

		}

		return null;
	}

	private void removeNodesFromAllIndices(final List<AbstractRelationship> rels) {

		for (AbstractRelationship rel : rels) {

			removeRelationshipFromAllIndices(rel);

		}
	}

	private void removeRelationshipFromAllIndices(final AbstractRelationship rel) {

		if (rel.getStringProperty(AbstractRelationship.uuid) == null) {

			logger.log(Level.WARNING, "Will not remove relationship from indices which has no UUID");

			return;

		}

		for (Enum indexName : indices.keySet()) {

			Index<Relationship> index = indices.get(indexName);
			synchronized(index) {
				index.remove(rel.getRelationship());
			}
		}
	}
}
