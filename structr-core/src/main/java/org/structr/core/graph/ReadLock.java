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
import org.neo4j.graphdb.Node;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.kernel.impl.transaction.LockType;

import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;

//~--- classes ----------------------------------------------------------------

/**
 * Obtain a read lock for the specified node. Defaults to a write lock if locks
 * are unavailable.
 *
 * @param node the node, a Long nodeId or a String nodeId
 *
 * @author Axel Morgner
 */
public class ReadLock extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(ReadLock.class.getName());

	//~--- fields ---------------------------------------------------------

	private LockType lockType = LockType.READ;

	//~--- methods --------------------------------------------------------

	public AbstractNode execute(final AbstractNode sourceNode) throws FrameworkException {
		return doLockNode(sourceNode);
	}

	private AbstractNode doLockNode(final AbstractNode structrNode) throws FrameworkException {

		final GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
		final Node node                    = graphDb.getNodeById(structrNode.getId());

		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				if ((lockType == LockType.READ) && (graphDb instanceof AbstractGraphDatabase)) {

					((AbstractGraphDatabase) node.getGraphDatabase()).getLockManager().getReadLock(node);

				}

				return null;
			}

		});

		// setExitCode(Command.ExitCode.SUCCESS);
		return null;
	}
}
