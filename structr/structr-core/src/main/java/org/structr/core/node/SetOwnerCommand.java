/*
 *  Copyright (C) 2011 Axel Morgner
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

import org.neo4j.graphdb.Direction;

import org.structr.common.RelType;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.User;

//~--- JDK imports ------------------------------------------------------------

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Set user as new owner of a node
 *
 * <b>Usage:</b>
 * <pre>
 * Command setOwner = Services.command(securityContext, SetOwnerCommand.class);
 * setOwner.execute(node, user);
 *
 * or
 *
 * Command setOwner = Services.command(securityContext, SetOwnerCommand.class);
 * setOwner.execute(nodeList, user);
 *
 * </pre>
 *
 * @author axel
 */
public class SetOwnerCommand extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(SetOwnerCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public Object execute(Object... parameters) {

		Command findNode            = Services.command(securityContext, FindNodeCommand.class);
		AbstractNode node           = null;
		List<AbstractNode> nodeList = null;
		User user                   = null;
		long id                     = 0;

		switch (parameters.length) {

			case 2 :
				if (parameters[0] instanceof Long) {
					id = ((Long) parameters[0]).longValue();
				} else if (parameters[0] instanceof AbstractNode) {
					id = ((AbstractNode) parameters[0]).getId();
				} else if (parameters[0] instanceof List) {
					nodeList = (List<AbstractNode>) parameters[0];
				} else if (parameters[0] instanceof String) {
					id = Long.parseLong((String) parameters[0]);
				} else {

					throw new IllegalArgumentException("Unable to get node id from "
									   + parameters[0]);
				}

				node = (AbstractNode) findNode.execute(new SuperUser(), id);

				if (parameters[1] instanceof User) {
					user = (User) parameters[1];
				} else {

					throw new IllegalArgumentException("Second parameter is no user: "
									   + parameters[1]);
				}

				break;

			default :
				break;
		}

		if (user != null) {

			if (nodeList != null) {
				setOwner(nodeList, user);
			} else {
				setOwner(node, user);
			}
		}

		return null;
	}

	//~--- set methods ----------------------------------------------------

	private void setOwner(final AbstractNode node, final User user) {

		Command delRel = Services.command(securityContext, DeleteRelationshipCommand.class);

		// Remove any existing OWNS relationships
		for (StructrRelationship s : node.getRelationships(RelType.OWNS, Direction.INCOMING)) {

			long id = s.getId();

			delRel.execute(s);
			logger.log(Level.FINEST, "Old owner relationship removed: {0}", id);
		}

		// Create new relationship to user and grant permissions to user or group
		Command createRel = Services.command(securityContext, CreateRelationshipCommand.class);

		createRel.execute(user, node, RelType.OWNS);
		logger.log(Level.FINEST, "Relationship to owner {0} added", user.getName());
	}

	private void setOwner(final List<AbstractNode> nodeList, final User user) {

		// Create outer transaction to bundle inner transactions
		final Command transactionCommand = Services.command(securityContext, TransactionCommand.class);

		transactionCommand.execute(new StructrTransaction() {

			@Override
			public Object execute() throws Throwable {

				for (AbstractNode node : nodeList) {
					setOwner(node, user);
				}

				return null;
			}

		});
	}
}
