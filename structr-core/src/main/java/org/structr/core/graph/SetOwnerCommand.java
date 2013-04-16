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

import org.neo4j.graphdb.Direction;

import org.structr.common.RelType;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;

//~--- classes ----------------------------------------------------------------

/**
 * Sets user as new owner of a node.
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

	public void execute(List<AbstractNode> nodeList, AbstractNode principal) throws FrameworkException {
		setOwner(nodeList, principal);
	}

	public void execute(AbstractNode node, AbstractNode principal) throws FrameworkException {
		setOwner(node, principal);
	}

	//~--- set methods ----------------------------------------------------

	private void setOwner(final List<AbstractNode> nodeList, final AbstractNode user) throws FrameworkException {

		// Create outer transaction to bundle inner transactions
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				for (AbstractNode node : nodeList) {
					setOwner(node, user);
				}

				return null;
			}

		});
	}

	private void setOwner(final AbstractNode node, final AbstractNode user) throws FrameworkException {

		// Create outer transaction to bundle inner transactions
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				DeleteRelationshipCommand delRel = Services.command(securityContext, DeleteRelationshipCommand.class);

				// Remove any existing OWNS relationships
				for (AbstractRelationship s : node.getRelationships(RelType.OWNS, Direction.INCOMING)) {

					delRel.execute(s);
				}
				
				// Create new relationship to user and grant permissions to user or group
				Services.command(securityContext, CreateRelationshipCommand.class).execute(user, node, RelType.OWNS);
				
				return null;
			}

		});
	}
}
