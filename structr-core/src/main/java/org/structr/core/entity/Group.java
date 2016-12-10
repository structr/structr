/**
 * Copyright (C) 2010-2016 Structr GmbH
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
import java.util.List;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.relationship.Groups;
import org.structr.core.property.ConstantBooleanProperty;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.schema.SchemaService;

public class Group extends AbstractUser implements Principal {

	public static final Property<List<Principal>> members = new EndNodes<>("members", Groups.class);
	public static final Property<Boolean>        isGroup  = new ConstantBooleanProperty("isGroup", true);

	public static final org.structr.common.View uiView = new org.structr.common.View(Group.class, PropertyView.Ui,
		type, name, members, blocked, isGroup
	);

	public static final org.structr.common.View publicView = new org.structr.common.View(Group.class, PropertyView.Public,
		type, name, members, blocked, isGroup
	);

	static {
		SchemaService.registerBuiltinTypeOverride("Group", Group.class.getName());
	}

	public void addMember(final Principal user) throws FrameworkException {

		List<Principal> _users = getProperty(members);
		_users.add(user);

		setProperty(members, _users);
	}

	public void removeMember(final Principal user) throws FrameworkException {

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

	/*
	public void addAllowedPermission(final Permission permission) {
		SecurityDelegate.addPermission(this, Principal.allowed, permission);
	}

	public void removeAllowedPermission(final Permission permission) {
		SecurityDelegate.removePermission(this, Principal.allowed, permission);
	}

	public void addDeniedPermission(final Permission permission) {
		SecurityDelegate.addPermission(this, Principal.denied, permission);
	}

	public void removeDeniedPermission(final Permission permission) {
		SecurityDelegate.removePermission(this, Principal.denied, permission);
	}

	@Override
	public Set<String> getAllowedPermissions() {
		return SecurityDelegate.getPermissionSet(dbNode, Principal.allowed);
	}

	@Override
	public Set<String> getDeniedPermissions() {
		return SecurityDelegate.getPermissionSet(dbNode, Principal.denied);
	}
	*/
}
