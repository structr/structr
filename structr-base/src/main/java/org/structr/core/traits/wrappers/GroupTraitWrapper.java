/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.core.traits.wrappers;

import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GroupTraitDefinition;

import java.util.List;

public class GroupTraitWrapper extends PrincipalTraitWrapper implements Group {

	public GroupTraitWrapper(final Traits traits, final NodeInterface nodeInterface) {
		super(traits, nodeInterface);
	}

	@Override
	public Iterable<Principal> getMembers() {

		final PropertyKey<Iterable<NodeInterface>> members = traits.key(GroupTraitDefinition.MEMBERS_PROPERTY);

		return Iterables.map(n -> n.as(Principal.class), wrappedObject.getProperty(members));
	}

	@Override
	public void addMember(final SecurityContext securityContext, final Principal user) throws FrameworkException {

		if (user == null) {
			throw new FrameworkException(422, "Unable to add user " + user + " to group " + this);
		}

		final PropertyKey<Iterable<NodeInterface>> members = traits.key(GroupTraitDefinition.MEMBERS_PROPERTY);
		final List<NodeInterface> _users = Iterables.toList(wrappedObject.getProperty(members));

		_users.add(user);

		wrappedObject.setProperty(members, _users);
	}

	@Override
	public void removeMember(final SecurityContext securityContext, final Principal member) throws FrameworkException {

		if (member == null) {
			throw new FrameworkException(422, "Unable to remove member " + member + " from group " + this);
		}

		final PropertyKey<Iterable<NodeInterface>> members = traits.key(GroupTraitDefinition.MEMBERS_PROPERTY);
		final List<NodeInterface> _users = Iterables.toList(wrappedObject.getProperty(members));

		_users.remove(member);

		wrappedObject.setProperty(members, _users);
	}
}
