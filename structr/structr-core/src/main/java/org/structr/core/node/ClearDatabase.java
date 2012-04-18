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

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.tooling.GlobalGraphOperations;

import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;

//~--- classes ----------------------------------------------------------------

/**
 * Clear database.
 *
 * This command takes no parameters.
 *
 * @author Axel Morgner
 */
public class ClearDatabase extends NodeServiceCommand {

	@Override
	public Object execute(Object... parameters) throws FrameworkException {

		GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
		NodeFactory nodeFactory      = (NodeFactory) arguments.get("nodeFactory");

		if (graphDb != null) {

			final List<AbstractNode> allNodes = (List<AbstractNode>) nodeFactory.createNodes(securityContext,
								    GlobalGraphOperations.at(graphDb).getAllNodes());
			final Command transactionCommand = Services.command(securityContext, TransactionCommand.class);
			final Command delRel             = Services.command(securityContext, DeleteRelationshipCommand.class);
			final Command delNode            = Services.command(securityContext, DeleteNodeCommand.class);

			transactionCommand.execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					for (AbstractNode node : allNodes) {

						delNode.execute(node);

					}

					return null;
				}

			});

		}

		return null;
	}
}
