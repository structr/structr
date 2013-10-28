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


package org.structr.web.entity;

import org.neo4j.graphdb.Direction;

import org.structr.web.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.logging.Logger;
import org.structr.common.PropertyView;
import org.structr.core.Services;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.EndNodes;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 *
 */
public class Group extends Principal {
	
	private static final Logger logger = Logger.getLogger(Group.class.getName());

	public static final EndNodes<Principal> members = new EndNodes<>("members", Principal.class, RelType.CONTAINS, Direction.OUTGOING, false);
	
	public static final org.structr.common.View uiView = new org.structr.common.View(User.class, PropertyView.Ui,
		type, name, members, blocked
	);
	
	public static final org.structr.common.View publicView = new org.structr.common.View(User.class, PropertyView.Public,
		type, name, members, blocked
	);

	@Override
	public String getEncryptedPassword() {

		// A group has no password
		return null;
	}

	public void addMember(final Principal user) throws FrameworkException {

		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				List<Principal> _users = getProperty(members);
				_users.add(user);

				setProperty(members, _users);
				return null;
			}
		});
		
	}
	
	public void removeMember(final Principal user) throws FrameworkException {

		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				List<Principal> _users = getProperty(members);
				_users.remove(user);

				setProperty(members, _users);
				return null;
			}
		});
		
	}

}
