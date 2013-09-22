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


package org.structr.core.entity;

import java.util.LinkedList;
import java.util.List;
import org.structr.common.Permission;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import static org.structr.core.entity.Principal.password;
import org.structr.core.graph.CreateRelationshipCommand;
import org.structr.core.property.PropertyKey;

//~--- interfaces -------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 *
 */
public abstract class AbstractUser extends Person implements Principal {

	private static final Logger logger = Logger.getLogger(AbstractUser.class.getName());
	
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

	//~--- get methods ----------------------------------------------------

	@Override
	public List<Principal> getParents() {

		List<Principal> parents                   = new LinkedList<Principal>();
		Iterable<AbstractRelationship> parentRels = getIncomingRelationships(RelType.CONTAINS);

		for (AbstractRelationship rel : parentRels) {

			AbstractNode node = rel.getStartNode();

			if (node instanceof Principal) {

				parents.add((Principal) node);
			}

		}

		return parents;

	}

	@Override
	public String getEncryptedPassword() {

		boolean dbNodeHasProperty = dbNode.hasProperty(password.dbName());

		if (dbNodeHasProperty) {

			Object dbValue = dbNode.getProperty(password.dbName());

			return (String) dbValue;

		} else {

			return null;
		}

	}

	@Override
	public Object getPropertyForIndexing(final PropertyKey key) {

		if (password.equals(key)) {

			return "";
			
		}

		return super.getPropertyForIndexing(key);

	}

	/**
	 * Intentionally return null.
	 * @return
	 */
	@Override
	public <T> T getProperty(final PropertyKey<T> key) {

		if (password.equals(key)) {
			
			return null;
			
		} else {
			
			return super.getProperty(key);
			
		}

	}

}
