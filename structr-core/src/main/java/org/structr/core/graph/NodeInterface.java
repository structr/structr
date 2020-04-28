/**
 * Copyright (C) 2010-2020 Structr GmbH
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

import java.util.List;
import java.util.Map;
import org.structr.api.graph.Identity;
import org.structr.api.graph.Node;
import org.structr.api.graph.RelationshipType;
import org.structr.common.AccessControllable;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.core.GraphObject;
import static org.structr.core.GraphObject.id;
import static org.structr.core.GraphObject.type;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.ManyEndpoint;
import org.structr.core.entity.ManyStartpoint;
import org.structr.core.entity.OneEndpoint;
import org.structr.core.entity.OneStartpoint;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Relation;
import org.structr.core.entity.Security;
import org.structr.core.entity.Source;
import org.structr.core.entity.Target;
import org.structr.core.entity.relationship.PrincipalOwnsNode;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.EntityIdProperty;
import org.structr.core.property.InternalPathProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StartNodes;
import org.structr.core.property.StringProperty;

public interface NodeInterface extends GraphObject, Comparable, AccessControllable {

	// properties
	public static final Property<String>              name         = new StringProperty("name").indexed().partOfBuiltInSchema();
	public static final Property<Boolean>             hidden       = new BooleanProperty("hidden").indexed().partOfBuiltInSchema();

	public static final Property<Principal>           owner        = new StartNode<>("owner", PrincipalOwnsNode.class).partOfBuiltInSchema();
	public static final Property<String>              ownerId      = new EntityIdProperty("ownerId", owner).partOfBuiltInSchema();

	public static final Property<Iterable<Principal>> grantees     = new StartNodes<>("grantees", Security.class).partOfBuiltInSchema();
	public static final Property<String>              internalPath = new InternalPathProperty("internalEntityContextPath").partOfBuiltInSchema();

	public static final View graphView = new View(NodeInterface.class, View.INTERNAL_GRAPH_VIEW,
		id, name, type
	);

	void init(final SecurityContext securityContext, final Node dbNode, final Class type, final long sourceTransactionId);

	void onNodeCreation();
	void onNodeInstantiation(final boolean isCreation);
	void onNodeDeletion();

	Node getNode();

	String getName();

	boolean hasRelationshipTo(final RelationshipType type, final NodeInterface targetNode);
	<R extends AbstractRelationship> R getRelationshipTo(final RelationshipType type, final NodeInterface targetNode);

	<R extends AbstractRelationship> Iterable<R> getRelationships();
	<R extends AbstractRelationship> Iterable<R> getRelationshipsAsSuperUser();

	<R extends AbstractRelationship> Iterable<R> getIncomingRelationships();
	<R extends AbstractRelationship> Iterable<R> getOutgoingRelationships();

	<A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target> boolean hasRelationship(final Class<? extends Relation<A, B, S, T>> type);
	<A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> boolean hasIncomingRelationships(final Class<R> type);
	<A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> boolean hasOutgoingRelationships(final Class<R> type);

	<A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> Iterable<R> getRelationships(final Class<R> type);

	<A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> R getIncomingRelationship(final Class<R> type);
	<A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> R getIncomingRelationshipAsSuperUser(final Class<R> type);
	<A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> Iterable<R> getIncomingRelationships(final Class<R> type);

	<A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, OneEndpoint<B>>> R getOutgoingRelationship(final Class<R> type);
	<A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, OneEndpoint<B>>> R getOutgoingRelationshipAsSuperUser(final Class<R> type);
	<A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, ManyEndpoint<B>>> Iterable<R> getOutgoingRelationships(final Class<R> type);

	void setRawPathSegmentId(final Identity pathSegmentId);

	List<Security> getSecurityRelationships();

	public Map<String, Object> getTemporaryStorage();
}
