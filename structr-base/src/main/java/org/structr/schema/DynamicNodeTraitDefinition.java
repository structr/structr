/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.schema;

import org.structr.common.Permission;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.*;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.TraitDefinition;
import org.structr.core.traits.operations.accesscontrollable.AllowedBySchema;
import org.structr.core.traits.operations.propertycontainer.GetVisibilityFlags;

import java.util.LinkedHashSet;
import java.util.Set;

public class DynamicNodeTraitDefinition extends AbstractDynamicTraitDefinition<SchemaNode> {

	public DynamicNodeTraitDefinition(final SchemaNode schemaNode) {
		super(schemaNode);
	}

	@Override
	public void initializeFrameworkMethods(final SchemaNode schemaNode) {

		super.initializeFrameworkMethods(schemaNode);

		final Set<String> readPermissions          = new LinkedHashSet<>();
		final Set<String> writePermissions         = new LinkedHashSet<>();
		final Set<String> deletePermissions        = new LinkedHashSet<>();
		final Set<String> accessControlPermissions = new LinkedHashSet<>();
		final boolean visibleToPublic              = schemaNode.defaultVisibleToPublic();
		final boolean visibleToAuth                = schemaNode.defaultVisibleToAuth();
		boolean hasGrants                          = false;

		if (visibleToPublic || visibleToAuth) {

			frameworkMethods.put(GetVisibilityFlags.class, new GetVisibilityFlags() {

				@Override
				public boolean isVisibleToPublicUsers(final GraphObject obj) {
					return visibleToPublic;
				}

				@Override
				public boolean isVisibleToAuthenticatedUsers(final GraphObject obj) {
					return visibleToAuth;
				}
			});
		}

		for (final SchemaGrant grant : schemaNode.getSchemaGrants()) {

			final String principalId = grant.getPrincipal().getUuid();

			if (grant.allowRead()) {
				readPermissions.add(principalId);
			}

			if (grant.allowWrite()) {
				writePermissions.add(principalId);
			}

			if (grant.allowDelete()) {
				deletePermissions.add(principalId);
			}

			if (grant.allowAccessControl()) {
				accessControlPermissions.add(principalId);
			}

			hasGrants = true;
		}

		if (hasGrants) {

			frameworkMethods.put(AllowedBySchema.class, new AllowedBySchema() {

				@Override
				public boolean allowedBySchema(final NodeInterface node, final Principal principal, final Permission permission) {

					final Set<String> ids = principal.getOwnAndRecursiveParentsUuids();

					switch (permission.name()) {

						case "read":          return !org.apache.commons.collections4.SetUtils.intersection(readPermissions, ids).isEmpty();
						case "write":         return !org.apache.commons.collections4.SetUtils.intersection(writePermissions, ids).isEmpty();
						case "delete":        return !org.apache.commons.collections4.SetUtils.intersection(deletePermissions, ids).isEmpty();
						case "accessControl": return !org.apache.commons.collections4.SetUtils.intersection(accessControlPermissions, ids).isEmpty();
					}

					return getSuper().allowedBySchema(node, principal, permission);
				}
			});
		}
	}

	@Override
	public void initializePropertyKeys(final SchemaNode schemaNode) {

		// linked properties
		for (final SchemaRelationshipNode outRel : schemaNode.getRelatedTo()) {

			try {

				propertyKeys.add(outRel.createKey(schemaNode, true));

			} catch (FrameworkException e) {
				e.printStackTrace();
			}
		}

		for (final SchemaRelationshipNode inRel : schemaNode.getRelatedFrom()) {

			try {

				propertyKeys.add(inRel.createKey(schemaNode, false));

			} catch (FrameworkException e) {
				e.printStackTrace();
			}
		}

		// add normal keys after relationship keys
		super.initializePropertyKeys(schemaNode);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	@Override
	public boolean isRelationship() {
		return false;
	}

	@Override
	public int compareTo(final TraitDefinition o) {
		return getName().compareTo(o.getName());
	}
}
