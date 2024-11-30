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

import org.structr.api.Predicate;
import org.structr.api.graph.Direction;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.common.SecurityContext;
import org.structr.core.IterableAdapter;
import org.structr.core.entity.*;
import org.structr.core.entity.relationship.PrincipalOwnsNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipFactory;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.*;
import org.structr.core.traits.operations.ComposableOperation;
import org.structr.core.traits.operations.OverwritableOperation;
import org.structr.core.traits.operations.nodeinterface.GetRelationships;

import java.util.Set;

public class NodeInterfaceTraitImplementation extends AbstractTraitImplementation {

	// properties
	private static final PropertyKey<String>  nameProperty                 = new StringProperty("name").indexed().partOfBuiltInSchema();
	private static final PropertyKey<Boolean> hiddenProperty               = new BooleanProperty("hidden").indexed().partOfBuiltInSchema();

	private static final Property<Principal> ownerProperty                 = new StartNode<>("owner", PrincipalOwnsNode.class).partOfBuiltInSchema();
	private static final PropertyKey<String> ownerIdProperty               = new EntityIdProperty("ownerId", ownerProperty).partOfBuiltInSchema();

	private static final PropertyKey<Iterable<Principal>> granteesProperty = new StartNodes<>("grantees", Security.class).partOfBuiltInSchema();
	private static final PropertyKey<String> internalPathProperty          = new InternalPathProperty("internalEntityContextPath").partOfBuiltInSchema();

	@Override
	public Set<ComposableOperation> getComposableOperations() {

		return Set.of(

		);
	}

	@Override
	public Set<OverwritableOperation> getOverwritableOperations() {

		return Set.of(

			new GetRelationships() {

				@Override
				public boolean hasRelationshipTo(final NodeInterface node, final RelationshipType type, final NodeInterface targetNode) {

					if (node.getNode() != null && type != null && targetNode != null) {
						return node.getNode().hasRelationshipTo(type, targetNode.getNode());
					}

					return false;
				}

				@Override
				public <A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> R getRelationshipTo(NodeInterface node, RelationshipType type, NodeInterface targetNode) {

					if (node.getNode() != null && type != null && targetNode != null) {

						final RelationshipFactory<A, B, R> factory = new RelationshipFactory<>(node.getSecurityContext());
						final Relationship rel = node.getNode().getRelationshipTo(type, targetNode.getNode());

						if (rel != null) {

							return factory.adapt(rel);
						}
					}

					return null;
				}

				@Override
				public <A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> Iterable<R> getRelationships(final NodeInterface node) {
					return new IterableAdapter<>(node.getNode().getRelationships(), new RelationshipFactory<>(node.getSecurityContext()));
				}

				@Override
				public <A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> Iterable<R> getRelationshipsAsSuperUser(final NodeInterface node) {
					return new IterableAdapter<>(node.getNode().getRelationships(), new RelationshipFactory<>(SecurityContext.getSuperUserInstance()));
				}

				@Override
				public <A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> Iterable<R> getIncomingRelationships(final NodeInterface node) {
					return new IterableAdapter<>(node.getNode().getRelationships(Direction.INCOMING), new RelationshipFactory<>(node.getSecurityContext()));
				}

				@Override
				public <A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> Iterable<R> getOutgoingRelationships(final NodeInterface node) {
					return new IterableAdapter<>(node.getNode().getRelationships(Direction.OUTGOING), new RelationshipFactory<>(node.getSecurityContext()));
				}

				@Override
				public <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target> boolean hasRelationship(final NodeInterface node, final Class<? extends Relation<A, B, S, T>> type) {
					return getRelationships(node, type).iterator().hasNext();
				}

				@Override
				public <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> boolean hasIncomingRelationships(final NodeInterface node, final Class<R> type) {
					return AbstractNode.getRelationshipForType(type).getSource().hasElements(node.getSecurityContext(), node.getNode(), null);
				}

				@Override
				public <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> boolean hasOutgoingRelationships(final NodeInterface node, final Class<R> type) {
					return AbstractNode.getRelationshipForType(type).getTarget().hasElements(node.getSecurityContext(), node.getNode(), null);
				}

				@Override
				public <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> Iterable<RelationshipInterface<A, B>> getRelationships(final NodeInterface node, final Class<R> type) {

					final RelationshipFactory<A, B, RelationshipInterface<A, B>> factory = new RelationshipFactory<>(node.getSecurityContext());
					final R template                                                     = getRelationshipForType(type);
					final Direction direction                                            = template.getDirectionForType(entityType);
					final RelationshipType relType                                       = template;

					return new IterableAdapter<>(node.getNode().getRelationships(direction, relType), factory);
				}

				@Override
				public <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> RelationshipInterface<A, B> getIncomingRelationship(final NodeInterface node, final Class<R> type) {

					final RelationshipFactory<A, B, RelationshipInterface<A, B>> factory = new RelationshipFactory<>(node.getSecurityContext());
					final R template                                                     = getRelationshipForType(type);
					final Relationship relationship                                      = template.getSource().getRawSource(node.getSecurityContext(), node.getNode(), null);

					if (relationship != null) {
						return factory.adapt(relationship);
					}

					return null;
				}

				@Override
				public <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> RelationshipInterface<A, B> getIncomingRelationshipAsSuperUser(final NodeInterface node, final Class<R> type) {

					final SecurityContext suContext                                      = SecurityContext.getSuperUserInstance();
					final RelationshipFactory<A, B, RelationshipInterface<A, B>> factory = new RelationshipFactory<>(node.getSecurityContext());
					final R template                                                     = getRelationshipForType(type);
					final Relationship relationship                                      = template.getSource().getRawSource(suContext, node.getNode(), null);

					if (relationship != null) {
						return factory.adapt(relationship);
					}

					return null;
				}

				@Override
				public <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> Iterable<RelationshipInterface<A, B>> getIncomingRelationships(final NodeInterface node, final Class<R> type) {

					final RelationshipFactory<A, B, RelationshipInterface<A, B>> factory = new RelationshipFactory<>(node.getSecurityContext());
					final R template                                                     = getRelationshipForType(type);

					return new IterableAdapter<>(template.getSource().getRawSource(node.getSecurityContext(), node.getNode(), null), factory);
				}

				@Override
				public <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> Iterable<RelationshipInterface<A, B>> getIncomingRelationshipsAsSuperUser(final NodeInterface node, final Class<R> type, final Predicate<NodeInterface> predicate) {

					final SecurityContext suContext                                      = SecurityContext.getSuperUserInstance();
					final RelationshipFactory<A, B, RelationshipInterface<A, B>> factory = new RelationshipFactory<>(securityContext);
					final R template                                                     = getRelationshipForType(type);

					return new IterableAdapter<>(template.getSource().getRawSource(suContext, node.getNode(), predicate), factory);
				}

				@Override
				public <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, OneEndpoint<B>>> RelationshipInterface<A, B> getOutgoingRelationship(final NodeInterface node, final Class<R> type) {

					final RelationshipFactory<A, B, RelationshipInterface<A, B>> factory = new RelationshipFactory<>(node.getSecurityContext());
					final R template                                                     = getRelationshipForType(type);
					final Relationship relationship                                      = template.getTarget().getRawSource(node.getSecurityContext(), node.getNode(), null);

					if (relationship != null) {
						return factory.adapt(relationship);
					}

					return null;
				}

				@Override
				public <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, OneEndpoint<B>>> RelationshipInterface<A, B> getOutgoingRelationshipAsSuperUser(final NodeInterface node, final Class<R> type) {

					final SecurityContext suContext                                      = SecurityContext.getSuperUserInstance();
					final RelationshipFactory<A, B, RelationshipInterface<A, B>> factory = new RelationshipFactory<>(node.getSecurityContext());
					final R template                                                     = getRelationshipForType(type);
					final Relationship relationship                                      = template.getTarget().getRawSource(suContext, node.getNode(), null);

					if (relationship != null) {
						return factory.adapt(relationship);
					}

					return null;
				}

				@Override
				public <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, ManyEndpoint<B>>> Iterable<RelationshipInterface<A, B>> getOutgoingRelationships(final NodeInterface node, final Class<R> type) {

					final RelationshipFactory<A, B, RelationshipInterface<A, B>> factory = new RelationshipFactory<>(node.getSecurityContext());
					final R template                                                     = getRelationshipForType(type);

					return new IterableAdapter<>(template.getTarget().getRawSource(node.getSecurityContext(), node.getNode(), null), factory);
				}

				@Override
				public GetRelationships getSuper() {
					return null;
				}
			}
		);

	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		return Set.of(
			nameProperty,
			hiddenProperty,
			ownerProperty,
			ownerIdProperty,
			granteesProperty,
			internalPathProperty
		);
	}
}
