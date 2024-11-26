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
package org.structr.core.traits;

import org.structr.api.graph.Node;
import org.structr.api.graph.RelationshipType;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.*;

import java.util.List;

public interface NodeTrait extends GraphTrait {

	Node getNode();

	String getName();
	String getType();

	boolean isDeleted();

	void onNodeCreation(final SecurityContext securityContext) throws FrameworkException;
	void onNodeDeletion(final SecurityContext securityContext) throws FrameworkException;

	boolean isValid(final ErrorBuffer errorBuffer);

	boolean hasRelationshipTo(final RelationshipType type, final NodeTrait targetNode);
	<R extends RelationshipTrait> R getRelationshipTo(final RelationshipType type, final NodeTrait targetNode);

	<R extends RelationshipTrait> Iterable<R> getRelationships();
	<R extends RelationshipTrait> Iterable<R> getRelationshipsAsSuperUser();

	<R extends RelationshipTrait> Iterable<R> getIncomingRelationships();
	<R extends RelationshipTrait> Iterable<R> getOutgoingRelationships();

	<A extends NodeTrait, B extends NodeTrait, S extends Source, T extends Target> boolean hasRelationship(final Trait<? extends Relation<A, B, S, T>> type);
	<A extends NodeTrait, B extends NodeTrait, S extends Source, T extends Target, R extends Relation<A, B, S, T>> boolean hasIncomingRelationships(final Trait<R> type);
	<A extends NodeTrait, B extends NodeTrait, S extends Source, T extends Target, R extends Relation<A, B, S, T>> boolean hasOutgoingRelationships(final Trait<R> type);

	<A extends NodeTrait, B extends NodeTrait, S extends Source, T extends Target, R extends Relation<A, B, S, T>> Iterable<R> getRelationships(final Trait<R> type);

	<A extends NodeTrait, B extends NodeTrait, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> R getIncomingRelationship(final Trait<R> type);
	<A extends NodeTrait, B extends NodeTrait, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> R getIncomingRelationshipAsSuperUser(final Trait<R> type);
	<A extends NodeTrait, B extends NodeTrait, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> Iterable<R> getIncomingRelationships(final Trait<R> type);

	<A extends NodeTrait, B extends NodeTrait, S extends Source, R extends Relation<A, B, S, OneEndpoint<B>>> R getOutgoingRelationship(final Trait<R> type);
	<A extends NodeTrait, B extends NodeTrait, S extends Source, R extends Relation<A, B, S, OneEndpoint<B>>> R getOutgoingRelationshipAsSuperUser(final Trait<R> type);
	<A extends NodeTrait, B extends NodeTrait, S extends Source, R extends Relation<A, B, S, ManyEndpoint<B>>> Iterable<R> getOutgoingRelationships(final Trait<R> type);

	List<Security> getSecurityRelationships();
}
