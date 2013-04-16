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
import org.structr.common.Permission;
import org.structr.common.PropertyView;
import org.structr.core.property.CollectionProperty;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 *
 */
public class Group extends AbstractNode implements Principal {

	public static final CollectionProperty<User> users = new CollectionProperty<User>("users", User.class, RelType.CONTAINS, Direction.OUTGOING, false);
	
	public static final org.structr.common.View uiView = new org.structr.common.View(User.class, PropertyView.Ui,
		type, name, users, blocked
	);
	
	public static final org.structr.common.View publicView = new org.structr.common.View(User.class, PropertyView.Public,
		type, name, users, blocked
	);

	//~--- methods --------------------------------------------------------

	@Override
	public void grant(Permission permission, AbstractNode obj) {

		obj.getSecurityRelationship(this).addPermission(permission);

	}

	@Override
	public void revoke(Permission permission, AbstractNode obj) {

		obj.getSecurityRelationship(this).removePermission(permission);

	}

	@Override
	public void block() throws FrameworkException {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getEncryptedPassword() {

		// A group has no password
		return null;
	}

	@Override
	public List<Principal> getParents() {

		List<Principal> parents               = new LinkedList<Principal>();
		List<AbstractRelationship> parentRels = getIncomingRelationships(RelType.CONTAINS);

		for (AbstractRelationship rel : parentRels) {

			AbstractNode node = rel.getEndNode();

			if (node instanceof Principal) {

				parents.add((Principal) node);
			}

		}

		return parents;

	}

	@Override
	public Boolean getBlocked() {

		return (Boolean) getProperty(Principal.blocked);

	}

	@Override
	public Boolean isBlocked() {

		return getBlocked();

	}

	//~--- set methods ----------------------------------------------------

	@Override
	public void setBlocked(Boolean blocked) throws FrameworkException {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	public void addUser(final User user) throws FrameworkException {
		
		List<User> _users = getProperty(users);
		_users.add(user);
		
		setProperty(users, _users);
		
	}
	
	public void removeUser(final User user) throws FrameworkException {
		
		List<User> _users = getProperty(users);
		_users.remove(user);
		
		setProperty(users, _users);
		
	}

}
