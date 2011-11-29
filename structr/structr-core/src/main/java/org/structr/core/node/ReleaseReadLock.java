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
import org.neo4j.graphdb.Node;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.kernel.impl.transaction.LockType;

import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Release read lock for the specified node
 *
 * @param node the node, a Long nodeId or a String nodeId
 *
 * @author amorgner
 */
public class ReleaseReadLock extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(ReleaseReadLock.class.getName());

	//~--- fields ---------------------------------------------------------

	private LockType lockType = LockType.READ;

	//~--- methods --------------------------------------------------------

	@Override
	public Object execute(Object... parameters) {

		AbstractNode node = null;
		Command findNode  = Services.command(securityContext, FindNodeCommand.class);

		switch (parameters.length) {

			case 1 :
				if (parameters[0] instanceof Long) {

					long id = ((Long) parameters[0]).longValue();

					node = (AbstractNode) findNode.execute(id);

				} else if (parameters[0] instanceof AbstractNode) {

					node = ((AbstractNode) parameters[0]);

				} else if (parameters[0] instanceof String) {

					long id = Long.parseLong((String) parameters[0]);

					node = (AbstractNode) findNode.execute(id);

				}

				break;

			default :
				break;

		}

		if (node == null) {

			setExitCode(Command.exitCode.FAILURE);
			setErrorMessage("Could not lock node null");
			logger.log(Level.WARNING, getErrorMessage());

			return null;

		}

		if (node.getId() == 0) {

			setExitCode(Command.exitCode.FAILURE);
			setErrorMessage("Locking the root node is not allowed.");
			logger.log(Level.WARNING, getErrorMessage());

			return null;

		}

		return doLockNode(node);
	}

	private AbstractNode doLockNode(final AbstractNode structrNode) {

		final GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
		final Node node                    = graphDb.getNodeById(structrNode.getId());

		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws Throwable {

				if ((lockType == LockType.READ) && (graphDb instanceof AbstractGraphDatabase)) {

					((AbstractGraphDatabase) graphDb).getConfig().getLockManager().releaseReadLock(node, null);

				}

				return null;
			}

		});

		// setExitCode(Command.exitCode.SUCCESS);
		return null;
	}
}
