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

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;

import org.structr.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.UnsupportedArgumentError;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Creates a relationship between two AbstractNode instances. The execute
 * method of this command takes the following parameters.
 *
 * @param startNode the start node
 * @param endNode the end node
 * @param type the name of relationship type to create
 *
 * @return the new relationship
 *
 * @author cmorgner
 */
public class CreateRelationshipCommand extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(CreateRelationshipCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public Object execute(Object... parameters) throws FrameworkException {

		GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");

		if ((graphDb != null) && (parameters.length == 3)) {

			Object arg0              = parameters[0];    // start node
			Object arg1              = parameters[1];    // end node
			Object arg2              = parameters[2];    // relationship type
			RelationshipType relType = null;

			if (arg2 instanceof String) {

				relType = DynamicRelationshipType.withName((String) arg2);

			} else if (arg2 instanceof RelationshipType) {

				relType = (RelationshipType) arg2;

			} else {

				throw new UnsupportedArgumentError("Wrong argument type(s).");

			}

			if ((arg0 instanceof AbstractNode) && (arg1 instanceof AbstractNode)) {

				AbstractNode startNode = (AbstractNode) arg0;
				AbstractNode endNode   = (AbstractNode) arg1;

				return createRelationship(startNode, endNode, relType);

			} else {

				throw new UnsupportedArgumentError("Wrong argument type(s).");

			}

		} else if ((graphDb != null) && (parameters.length == 4)) {

			Object arg0              = parameters[0];    // start node
			Object arg1              = parameters[1];    // end node
			Object arg2              = parameters[2];    // relationship type
			Object arg3              = parameters[3];    // check duplicates
			RelationshipType relType = null;

			if (arg2 instanceof String) {

				relType = getRelationshipTypeFor((String) arg2);

			} else if (arg2 instanceof RelationshipType) {

				relType = (RelationshipType) arg2;

			} else {

				throw new UnsupportedArgumentError("Wrong argument type(s).");

			}

			boolean checkDuplicates = false;

			if (arg3 instanceof Boolean) {

				checkDuplicates = ((Boolean) arg3) == true;

			}

			if ((arg0 instanceof AbstractNode) && (arg1 instanceof AbstractNode)) {

				AbstractNode startNode = (AbstractNode) arg0;
				AbstractNode endNode   = (AbstractNode) arg1;

				if (checkDuplicates) {

					List<StructrRelationship> incomingRels = endNode.getIncomingRelationships();

					for (StructrRelationship rel : incomingRels) {

						if (rel.getRelType().equals(relType) && rel.getStartNode().equals(startNode)) {

							logger.log(Level.WARNING, "Creation of duplicate relationship was blocked");

							return null;

						}

					}

				}

				return createRelationship(startNode, endNode, relType);

			} else {

				throw new UnsupportedArgumentError("Wrong argument type(s).");

			}

		}

		return null;
	}

	private StructrRelationship createRelationship(final AbstractNode fromNode, final AbstractNode toNode, final RelationshipType relType) throws FrameworkException {

		final Command transactionCommand    = Services.command(securityContext, TransactionCommand.class);
		StructrRelationship newRelationship = (StructrRelationship) transactionCommand.execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				StructrRelationship relationship = new StructrRelationship(securityContext, fromNode.getNode().createRelationshipTo(toNode.getNode(), relType));

				// EntityContext.getGlobalModificationListener().relationshipCreated(securityContext, fromNode, toNode, relationship);
				return relationship;
			}

		});

		return newRelationship;
	}

	//~--- get methods ----------------------------------------------------

	private RelationshipType getRelationshipTypeFor(final String relTypeString) {

		RelationshipType relType = null;

		try {
			relType = RelType.valueOf(relTypeString);
		} catch (Exception ignore) {}

		if (relType == null) {

			relType = DynamicRelationshipType.withName(relTypeString);

		}

		return relType;
	}
}
