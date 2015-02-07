/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.entity;

import java.util.LinkedList;
import org.structr.common.error.FrameworkException;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import org.structr.common.PropertyView;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.entity.relationship.Groups;
import org.structr.core.notion.PropertySetNotion;
import org.structr.core.property.BooleanProperty;
import org.structr.schema.SchemaService;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 *
 */
public class Group extends AbstractUser implements Principal {

	public static final Property<List<Principal>> members = new EndNodes<>("members", Groups.class, new PropertySetNotion(id, name));
	public static final Property<Boolean>        isGroup  = new BooleanProperty("isGroup").defaultValue(true).readOnly();

	public static final org.structr.common.View uiView = new org.structr.common.View(Group.class, PropertyView.Ui,
		type, name, members, blocked, isGroup
	);

	public static final org.structr.common.View publicView = new org.structr.common.View(Group.class, PropertyView.Public,
		type, name, members, blocked, isGroup
	);

	static {
		SchemaService.registerBuiltinTypeOverride("Group", Group.class.getName());
	}

	@Override
	public String getEncryptedPassword() {

		// A group has no password
		return null;
	}

	public void addMember(final Principal user) throws FrameworkException {

		final App app = StructrApp.getInstance(securityContext);
		List<Principal> _users = getProperty(members);
		_users.add(user);

		setProperty(members, _users);
	}

	public void removeMember(final Principal user) throws FrameworkException {

		final App app = StructrApp.getInstance(securityContext);
		List<Principal> _users = getProperty(members);
		_users.remove(user);

		setProperty(members, _users);
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
