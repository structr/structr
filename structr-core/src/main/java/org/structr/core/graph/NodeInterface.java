/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.graph;

import org.neo4j.graphdb.Node;
import org.structr.common.AccessControllable;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.ManyEndpoint;
import org.structr.core.entity.ManyStartpoint;
import org.structr.core.entity.OneEndpoint;
import org.structr.core.entity.OneStartpoint;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Relation;
import org.structr.core.entity.Source;
import org.structr.core.entity.Target;
import org.structr.core.entity.relationship.PrincipalOwnsNode;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.EntityIdProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;

/**
 *
 * @author Christian Morgner
 */
public interface NodeInterface extends GraphObject, Comparable<NodeInterface>, AccessControllable {

	// properties
	public static final Property<String>          name             = new StringProperty("name").indexed();
	public static final Property<String>          createdBy        = new StringProperty("createdBy").readOnly().writeOnce();
	public static final Property<Boolean>         deleted          = new BooleanProperty("deleted").indexed();
	public static final Property<Boolean>         hidden           = new BooleanProperty("hidden").indexed();

	public static final Property<Principal>       owner            = new StartNode<>("owner", PrincipalOwnsNode.class);
	public static final Property<String>          ownerId          = new EntityIdProperty("ownerId", owner);

	public void init(final SecurityContext securityContext, final Node dbNode);

	public void onNodeCreation();
	public void onNodeInstantiation();
	public void onNodeDeletion();

	public Node getNode();

	public String getName();

	public boolean isValid(ErrorBuffer errorBuffer);
	public boolean isDeleted();

	public <R extends AbstractRelationship> Iterable<R> getRelationships();
	public <R extends AbstractRelationship> Iterable<R> getIncomingRelationships();
	public <R extends AbstractRelationship> Iterable<R> getOutgoingRelationships();

	public <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> Iterable<R> getRelationships(final Class<R> type);

	public <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> R getIncomingRelationship(final Class<R> type);
	public <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> Iterable<R> getIncomingRelationships(final Class<R> type);

	public <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, OneEndpoint<B>>> R getOutgoingRelationship(final Class<R> type);
	public <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, ManyEndpoint<B>>> Iterable<R> getOutgoingRelationships(final Class<R> type);
}
