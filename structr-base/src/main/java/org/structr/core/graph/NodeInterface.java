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

import org.apache.commons.lang.StringUtils;
import org.structr.api.Transaction;
import org.structr.api.graph.Direction;
import org.structr.api.graph.Identity;
import org.structr.api.graph.Node;
import org.structr.api.graph.RelationshipType;
import org.structr.api.util.Iterables;
import org.structr.common.*;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.*;
import org.structr.core.entity.relationship.PrincipalOwnsNode;
import org.structr.core.property.*;

import java.util.*;

public interface NodeInterface extends GraphObject<Node>, Comparable, AccessControllable {

	// properties
	public static final Property<String>              name         = new StringProperty("name").indexed().partOfBuiltInSchema();
	public static final Property<Boolean>             hidden       = new BooleanProperty("hidden").indexed().partOfBuiltInSchema();

	public static final Property<PrincipalInterface>  owner        = new StartNode<>("owner", PrincipalOwnsNode.class).partOfBuiltInSchema();
	public static final Property<String>              ownerId      = new EntityIdProperty("ownerId", owner).partOfBuiltInSchema();

	public static final Property<Iterable<PrincipalInterface>> grantees     = new StartNodes<>("grantees", Security.class).partOfBuiltInSchema();
	public static final Property<String>              internalPath = new InternalPathProperty("internalEntityContextPath").partOfBuiltInSchema();


	void onNodeCreation(final SecurityContext securityContext) throws FrameworkException;
	void onNodeInstantiation(final boolean isCreation);
	void onNodeDeletion(SecurityContext securityContext) throws FrameworkException;

	Node getNode();

	boolean isDeleted();

	String getName();

	boolean hasRelationshipTo(final RelationshipType type, final NodeInterface targetNode);
	<A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> R getRelationshipTo(final RelationshipType type, final NodeInterface targetNode);

	<A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> Iterable<R> getRelationships();
	<A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> Iterable<R> getRelationshipsAsSuperUser();

	<A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> Iterable<R> getIncomingRelationships();
	<A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> Iterable<R> getOutgoingRelationships();

	<A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target> boolean hasRelationship(final Class<? extends Relation<A, B, S, T>> type);
	<A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> boolean hasIncomingRelationships(final Class<R> type);
	<A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> boolean hasOutgoingRelationships(final Class<R> type);

	<A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> Iterable<RelationshipInterface<A, B>> getRelationships(final Class<R> type);

	<A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> RelationshipInterface<A, B> getIncomingRelationship(final Class<R> type);
	<A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> RelationshipInterface<A, B> getIncomingRelationshipAsSuperUser(final Class<R> type);
	<A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> Iterable<RelationshipInterface<A, B>> getIncomingRelationships(final Class<R> type);

	<A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, OneEndpoint<B>>> RelationshipInterface<A, B> getOutgoingRelationship(final Class<R> type);
	<A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, OneEndpoint<B>>> RelationshipInterface<A, B> getOutgoingRelationshipAsSuperUser(final Class<R> type);
	<A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, ManyEndpoint<B>>> Iterable<RelationshipInterface<A, B>> getOutgoingRelationships(final Class<R> type);

	void setRawPathSegmentId(final Identity pathSegmentId);

	List<RelationshipInterface<PrincipalInterface, NodeInterface>> getSecurityRelationships();

	Map<String, Object> getTemporaryStorage();

	default void visitForUsage(final Map<String, Object> data) {

		data.put("id",   getUuid());
		data.put("type", getClass().getSimpleName());
	}

	default void copyPermissionsTo(final SecurityContext ctx, final NodeInterface targetNode, final boolean overwrite) throws FrameworkException {

		for (final RelationshipInterface<PrincipalInterface, NodeInterface> security : this.getIncomingRelationships(Security.class)) {

			final Set<Permission> permissions  = new HashSet();
			final PrincipalInterface principal = security.getSourceNode();

			for (final String perm : security.getPermissions()) {

				permissions.add(Permissions.valueOf(perm));
			}

			if (overwrite) {

				targetNode.setAllowed(permissions, principal, ctx);

			} else {

				targetNode.grant(permissions, principal, ctx);
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
