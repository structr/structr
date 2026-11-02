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
package org.structr.appmap;

import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SchemaGrant;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;

/**
 * Who may do what: every group with the number of its members, the groups it is a member of and
 * the types it has a schema grant on, with the rights it grants (read, write, delete). Groups answer "who", not "what", so they are no nodes of
 * the map; the client attaches them to the types.
 */
public class GroupMapContributor implements ApplicationMapContributor {

	@Override
	public void contribute(final ApplicationMap map, final App app) throws FrameworkException {

		for (final NodeInterface node : app.nodeQuery(StructrTraits.GROUP).getAsList()) {

			// a node with the label but without a known type comes back as a GenericNode
			if (!node.is(StructrTraits.GROUP)) {
				continue;
			}

			final Group group = node.as(Group.class);

			final ApplicationMap.MapGroup mapGroup = map.addGroup(group.getUuid(), group.getName(), Iterables.count(group.getMembers()));

			for (final Group parent : group.getParents()) {
				mapGroup.addParent(parent.getUuid());
			}
		}

		for (final NodeInterface node : app.nodeQuery(StructrTraits.SCHEMA_GRANT).getAsList()) {

			final SchemaGrant grant      = node.as(SchemaGrant.class);
			final Principal principal    = grant.getPrincipal();
			final SchemaNode schemaNode  = grant.getSchemaNode();
			final boolean allowsAnything = grant.allowRead() || grant.allowWrite() || grant.allowDelete();

			if (principal != null && schemaNode != null && allowsAnything && principal.is(StructrTraits.GROUP)) {
				map.addGroup(principal.getUuid(), principal.getName(), 0).addGrant(schemaNode.getUuid(), (grant.allowRead() ? "r" : "") + (grant.allowWrite() ? "w" : "") + (grant.allowDelete() ? "d" : ""));
			}
		}
	}
}
