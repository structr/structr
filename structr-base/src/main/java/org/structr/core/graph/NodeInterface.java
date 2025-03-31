/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.core.graph;

import org.structr.api.Predicate;
import org.structr.api.graph.Identity;
import org.structr.api.graph.Node;
import org.structr.api.graph.RelationshipType;
import org.structr.common.AccessControllable;
import org.structr.common.Permission;
import org.structr.common.Permissions;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Security;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public interface NodeInterface extends GraphObject, Comparable<NodeInterface> {

	void onNodeCreation(final SecurityContext securityContext) throws FrameworkException;
	void onNodeInstantiation(final boolean isCreation);
	void onNodeDeletion(SecurityContext securityContext) throws FrameworkException;

	Node getNode();

	boolean isDeleted();

	String getName();
	void setName(final String name) throws FrameworkException;
	Object getPath(final SecurityContext securityContext);

	boolean hasRelationshipTo(final RelationshipType type, final NodeInterface targetNode);
	RelationshipInterface getRelationshipTo(final RelationshipType type, final NodeInterface targetNode);

	Iterable<RelationshipInterface> getRelationships();
	Iterable<RelationshipInterface> getRelationshipsAsSuperUser();
	Iterable<RelationshipInterface> getRelationshipsAsSuperUser(final String type);

	Iterable<RelationshipInterface> getIncomingRelationships();
	Iterable<RelationshipInterface> getOutgoingRelationships();

	boolean hasRelationship(final String type);
	boolean hasIncomingRelationships(final String type);
	boolean hasOutgoingRelationships(final String type);

	Iterable<RelationshipInterface> getRelationships(final String type);

	RelationshipInterface getIncomingRelationship(final String type);
	RelationshipInterface getIncomingRelationshipAsSuperUser(final String type);
	Iterable<RelationshipInterface> getIncomingRelationships(final String type);
	Iterable<RelationshipInterface> getIncomingRelationshipsAsSuperUser(final String type, final Predicate<GraphObject> predicate);

	RelationshipInterface getOutgoingRelationship(final String type);
	RelationshipInterface getOutgoingRelationshipAsSuperUser(final String type);
	Iterable<RelationshipInterface> getOutgoingRelationships(final String type);

	void setRawPathSegmentId(final Identity pathSegmentId);

	Map<String, Object> getTemporaryStorage();

	void visitForUsage(final Map<String, Object> data);

	default void copyPermissionsTo(final SecurityContext ctx, final NodeInterface targetNode, final boolean overwrite) throws FrameworkException {

		for (final RelationshipInterface rel : this.getIncomingRelationships(StructrTraits.SECURITY)) {

			final Set<Permission> permissions = new HashSet();
			final Security security           = rel.as(Security.class);
			final NodeInterface sourceNode    = rel.getSourceNode();
			final Principal principal         = sourceNode.as(Principal.class);

			for (final String perm : security.getPermissions()) {

				permissions.add(Permissions.valueOf(perm));
			}

			if (overwrite) {

				targetNode.as(AccessControllable.class).setAllowed(permissions, principal, ctx);

			} else {

				targetNode.as(AccessControllable.class).grant(permissions, principal, ctx);
			}
		}
	}

	default void prefetchPropertySet(final Iterable<PropertyKey> keys) {

		/* disabled because it's buggy and doesn't improve the performance much
		final Set<String> outgoingKeys     = new LinkedHashSet<>();
		final Set<String> incomingKeys     = new LinkedHashSet<>();
		final Set<String> outgoingRelTypes = new LinkedHashSet<>();
		final Set<String> incomingRelTypes = new LinkedHashSet<>();
		final Class type                   = getClass();
		final String uuid                  = getUuid();

		for (final PropertyKey key : keys) {

			if (key instanceof RelationProperty<?> r) {

				final Relation rel   = r.getRelation();
				final Direction dir  = rel.getDirectionForType(type);
				final String relType = rel.name();

				switch (dir) {

					case OUTGOING -> {
						outgoingKeys.add("all/OUTGOING/" + relType);
						outgoingRelTypes.add(relType);
					}

					case INCOMING -> {
						incomingKeys.add("all/INCOMING/" + relType);
						incomingRelTypes.add(relType);
					}
				}
			}
		}

		if (outgoingRelTypes.size() > 1) {

			TransactionCommand.getCurrentTransaction().prefetch2(
				"MATCH (n:NodeInterface { id: $id })-[r:" + StringUtils.join(outgoingRelTypes, "|") + "*0..1]->(x) WITH collect(DISTINCT x) AS nodes, collect(DISTINCT last(r)) AS rels RETURN nodes, rels",
				outgoingKeys,
				outgoingKeys,
				uuid
			);

		}

		if (incomingRelTypes.size() > 1) {

			TransactionCommand.getCurrentTransaction().prefetch2(
				"MATCH (n:NodeInterface { id: $id })<-[r:" + StringUtils.join(incomingRelTypes, "|") + "*0..1]-(x) WITH collect(DISTINCT x) AS nodes, collect(DISTINCT last(r)) AS rels RETURN nodes, rels",
				incomingKeys,
				incomingKeys,
				uuid
			);

		}
		*/
	}

}
