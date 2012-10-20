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



package org.structr.core.entity;

import org.structr.common.AccessControllable;
import org.structr.common.Permission;
import org.structr.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.node.CreateRelationshipCommand;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.Property;
import org.structr.common.SecurityContext;

//~--- classes ----------------------------------------------------------------

public abstract class PrincipalImpl extends AbstractNode implements Principal {

	public static final Property<String> realName = new Property<String>("realName");
	public static final Property<String> password = new Property<String>("password");

	//~--- methods --------------------------------------------------------

	@Override
	public void block() throws FrameworkException {

		setBlocked(Boolean.TRUE);

	}

	@Override
	public void grant(Permission permission, AccessControllable obj) {

		AbstractRelationship secRel = obj.getSecurityRelationship(this);

		if (secRel == null) {

			try {

				secRel = createSecurityRelationshipTo(obj);

			} catch (FrameworkException ex) {

				Logger.getLogger(PrincipalImpl.class.getName()).log(Level.SEVERE, "Could not create security relationship!", ex);

			}

		}

		secRel.addPermission(permission);

	}

	@Override
	public void revoke(Permission permission, AccessControllable obj) {

		AbstractRelationship secRel = obj.getSecurityRelationship(this);

		if (secRel == null) {

			try {

				secRel = createSecurityRelationshipTo(obj);

			} catch (FrameworkException ex) {

				Logger.getLogger(PrincipalImpl.class.getName()).log(Level.SEVERE, "Could not create security relationship!", ex);

			}

		}

		secRel.removePermission(permission);

	}

	private AbstractRelationship createSecurityRelationshipTo(final AccessControllable obj) throws FrameworkException {

		return (AbstractRelationship) Services.command(SecurityContext.getSuperUserInstance(), CreateRelationshipCommand.class).execute(this, obj, RelType.SECURITY);

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getEncryptedPassword() {

		boolean dbNodeHasProperty = dbNode.hasProperty(password.name());

		if (dbNodeHasProperty) {

			Object dbValue = dbNode.getProperty(password.name());

			return (String) dbValue;

		} else {

			return null;
		}

	}

	@Override
	public Boolean getBlocked() {

		return (Boolean) getProperty(blocked);

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
	public Boolean isBlocked() {

		return Boolean.TRUE.equals(getBlocked());

	}

	//~--- set methods ----------------------------------------------------

	@Override
	public void setBlocked(final Boolean blocked) throws FrameworkException {}

}
