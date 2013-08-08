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
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.Principal;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.Permission;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.core.Services;
import org.structr.core.entity.SecurityRelationship;
import org.structr.core.graph.CreateRelationshipCommand;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.CollectionProperty;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 *
 */
public class Group extends AbstractNode implements Principal {
	
	private static final Logger logger = Logger.getLogger(Group.class.getName());

	public static final CollectionProperty<Principal> users = new CollectionProperty("users", Principal.class, RelType.CONTAINS, Direction.OUTGOING, false);
	
	public static final org.structr.common.View uiView = new org.structr.common.View(User.class, PropertyView.Ui,
		type, name, users, blocked
	);
	
	public static final org.structr.common.View publicView = new org.structr.common.View(User.class, PropertyView.Public,
		type, name, users, blocked
	);

	//~--- methods --------------------------------------------------------
	@Override
	public void grant(Permission permission, AbstractNode obj) {

		SecurityRelationship secRel = obj.getSecurityRelationship(this);

		if (secRel == null) {

			try {

				secRel = createSecurityRelationshipTo(obj);

			} catch (FrameworkException ex) {

				logger.log(Level.SEVERE, "Could not create security relationship!", ex);

			}

		}

		secRel.addPermission(permission);

	}

	@Override
	public void revoke(Permission permission, AbstractNode obj) {

		SecurityRelationship secRel = obj.getSecurityRelationship(this);

		if (secRel == null) {

			logger.log(Level.SEVERE, "Could not create revoke permission, no security relationship exists!");

		} else {

			secRel.removePermission(permission);
		}

	}

	private SecurityRelationship createSecurityRelationshipTo(final AbstractNode obj) throws FrameworkException {

		return (SecurityRelationship) Services.command(SecurityContext.getSuperUserInstance(), CreateRelationshipCommand.class).execute(this, obj, org.structr.common.RelType.SECURITY);

	}

	@Override
	public String getEncryptedPassword() {

		// A group has no password
		return null;
	}

	@Override
	public List<Principal> getParents() {

		List<Principal> parents                   = new LinkedList<Principal>();
		Iterable<AbstractRelationship> parentRels = getIncomingRelationships(RelType.CONTAINS);

		for (AbstractRelationship rel : parentRels) {

			AbstractNode node = rel.getEndNode();

			if (node instanceof Principal) {

				parents.add((Principal) node);
			}

		}

		return parents;

	}

	public void addUser(final Principal user) throws FrameworkException {

		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				List<Principal> _users = getProperty(users);
				_users.add(user);

				setProperty(users, _users);
				return null;
			}
		});
		
	}
	
	public void removeUser(final Principal user) throws FrameworkException {

		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				List<Principal> _users = getProperty(users);
				_users.remove(user);

				setProperty(users, _users);
				return null;
			}
		});
		
	}

}
