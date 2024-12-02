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
package org.structr.core.traits.operations.nodeinterface;

import org.structr.api.Predicate;
import org.structr.api.graph.RelationshipType;
import org.structr.core.entity.*;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.traits.operations.FrameworkMethod;

public abstract class GetRelationships extends FrameworkMethod<GetRelationships> {

	public abstract boolean hasRelationshipTo(final NodeInterface node, final RelationshipType type, final NodeInterface targetNode);
	public abstract <A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> R getRelationshipTo(final NodeInterface node, final RelationshipType type, final NodeInterface targetNode);

	public abstract <A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> Iterable<R> getRelationships(final NodeInterface node);
	public abstract <A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> Iterable<R> getRelationshipsAsSuperUser(final NodeInterface node);

	public abstract <A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> Iterable<R> getIncomingRelationships(final NodeInterface node);
	public abstract <A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> Iterable<R> getOutgoingRelationships(final NodeInterface node);

	public abstract <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target> boolean hasRelationship(final NodeInterface node, final Class<? extends Relation<A, B, S, T>> type);
	public abstract <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> boolean hasIncomingRelationships(final NodeInterface node, final Class<R> type);
	public abstract <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> boolean hasOutgoingRelationships(final NodeInterface node, final Class<R> type);

	public abstract <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> Iterable<RelationshipInterface<A, B>> getRelationships(final NodeInterface node, final Class<R> type);

	public abstract <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> RelationshipInterface<A, B> getIncomingRelationship(final NodeInterface node, final Class<R> type);
	public abstract <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> RelationshipInterface<A, B> getIncomingRelationshipAsSuperUser(final NodeInterface node, final Class<R> type);
	public abstract <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> Iterable<RelationshipInterface<A, B>> getIncomingRelationships(final NodeInterface node, final Class<R> type);
	public abstract <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> Iterable<RelationshipInterface<A, B>> getIncomingRelationshipsAsSuperUser(final NodeInterface node, final Class<R> type, final Predicate<NodeInterface> predicate);

	public abstract <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, OneEndpoint<B>>> RelationshipInterface<A, B> getOutgoingRelationship(final NodeInterface node, final Class<R> type);
	public abstract <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, OneEndpoint<B>>> RelationshipInterface<A, B> getOutgoingRelationshipAsSuperUser(final NodeInterface node, final Class<R> type);
	public abstract <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, ManyEndpoint<B>>> Iterable<RelationshipInterface<A, B>> getOutgoingRelationships(final NodeInterface node, final Class<R> type);
}
