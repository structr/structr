/**
 * Copyright (C) 2010-2014 Structr, c/o Morgner UG (haftungsbeschränkt) <structr@structr.org>
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.entity;

import java.util.LinkedList;
import org.structr.common.error.FrameworkException;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.logging.Logger;
import org.structr.common.PropertyView;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.entity.relationship.Groups;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 *
 */
public class Group extends AbstractUser implements Principal {
	
	private static final Logger logger = Logger.getLogger(Group.class.getName());

	public static final Property<List<Principal>> members = new EndNodes<>("members", Groups.class);
	
	public static final org.structr.common.View uiView = new org.structr.common.View(Group.class, PropertyView.Ui,
		type, name, members, blocked
	);
	
	public static final org.structr.common.View publicView = new org.structr.common.View(Group.class, PropertyView.Public,
		type, name, members, blocked
	);

	@Override
	public String getEncryptedPassword() {

		// A group has no password
		return null;
	}

	public void addMember(final Principal user) throws FrameworkException {

		final App app = StructrApp.getInstance(securityContext);
		try {
			
			app.beginTx();

			List<Principal> _users = getProperty(members);
			_users.add(user);

			setProperty(members, _users);
			
			app.commitTx();
			
		} finally {
			
			app.finishTx();
		}
		
	}
	
	public void removeMember(final Principal user) throws FrameworkException {

		final App app = StructrApp.getInstance(securityContext);
		try {
			
			app.beginTx();

			List<Principal> _users = getProperty(members);
			_users.remove(user);

			setProperty(members, _users);
			
			app.commitTx();
			
		} finally {
			
			app.finishTx();
		}
	}
	
	@Override
	public List<Principal> getParents() {
		
		final List<Principal> principals = new LinkedList<>();
		for (final Groups groups : getIncomingRelationships(Groups.class)) {
			
			principals.add(groups.getSourceNode());
			
		}
		
		return principals;
	}
}
