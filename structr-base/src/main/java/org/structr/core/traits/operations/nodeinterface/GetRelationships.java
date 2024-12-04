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
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.traits.operations.FrameworkMethod;

public abstract class GetRelationships extends FrameworkMethod<GetRelationships> {

	public abstract boolean hasRelationshipTo(final NodeInterface node, final RelationshipType type, final NodeInterface targetNode);
	public abstract RelationshipInterface getRelationshipTo(final NodeInterface node, final RelationshipType type, final NodeInterface targetNode);

	public abstract Iterable<RelationshipInterface> getRelationships(final NodeInterface node);
	public abstract Iterable<RelationshipInterface> getRelationshipsAsSuperUser(final NodeInterface node);

	public abstract Iterable<RelationshipInterface> getIncomingRelationships(final NodeInterface node);
	public abstract Iterable<RelationshipInterface> getOutgoingRelationships(final NodeInterface node);

	public abstract boolean hasRelationship(final NodeInterface node, final String type);
	public abstract boolean hasIncomingRelationships(final NodeInterface node, final String type);
	public abstract boolean hasOutgoingRelationships(final NodeInterface node, final String type);

	public abstract Iterable<RelationshipInterface> getRelationships(final NodeInterface node, final String type);

	public abstract RelationshipInterface getIncomingRelationship(final NodeInterface node, final String type);
	public abstract RelationshipInterface getIncomingRelationshipAsSuperUser(final NodeInterface node, final String type);
	public abstract Iterable<RelationshipInterface> getIncomingRelationships(final NodeInterface node, final String type);
	public abstract Iterable<RelationshipInterface> getIncomingRelationshipsAsSuperUser(final NodeInterface node, final String type, final Predicate<GraphObject> predicate);

	public abstract RelationshipInterface getOutgoingRelationship(final NodeInterface node, final String type);
	public abstract RelationshipInterface getOutgoingRelationshipAsSuperUser(final NodeInterface node, final String type);
	public abstract Iterable<RelationshipInterface> getOutgoingRelationships(final NodeInterface node, final String type);
}
