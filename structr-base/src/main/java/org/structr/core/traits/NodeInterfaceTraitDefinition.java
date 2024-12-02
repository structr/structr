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
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.nodeinterface.GetRelationships;

import java.util.Map;
import java.util.Set;

public class NodeInterfaceTraitDefinition extends AbstractTraitDefinition {

	// properties
	private static final PropertyKey<String>  nameProperty                 = new StringProperty("name").indexed().partOfBuiltInSchema();
	private static final PropertyKey<Boolean> hiddenProperty               = new BooleanProperty("hidden").indexed().partOfBuiltInSchema();

	private static final Property<NodeInterface> ownerProperty             = new StartNode("owner", PrincipalOwnsNode.class).partOfBuiltInSchema();
	private static final PropertyKey<String> ownerIdProperty               = new EntityIdProperty("ownerId", ownerProperty).partOfBuiltInSchema();

	private static final PropertyKey<Iterable<NodeInterface>> granteesProperty = new StartNodes<>("grantees", SecurityRelationship.class).partOfBuiltInSchema();
	private static final PropertyKey<String> internalPathProperty          = new InternalPathProperty("internalEntityContextPath").partOfBuiltInSchema();

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(
		);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			GetRelationships.class,
			new GetRelationships() {

				@Override
				public boolean hasRelationshipTo(final NodeInterface node, final RelationshipType type, final NodeInterface targetNode) {

					if (node.getNode() != null && type != null && targetNode != null) {
						return node.getNode().hasRelationshipTo(type, targetNode.getNode());
					}

					return false;
				}

				@Override
				public R getRelationshipTo(NodeInterface node, RelationshipType type, NodeInterface targetNode) {

					if (node.getNode() != null && type != null && targetNode != null) {

						final RelationshipFactory<A, B, R> factory = new RelationshipFactory(node.getSecurityContext());
						final Relationship rel = node.getNode().getRelationshipTo(type, targetNode.getNode());

						if (rel != null) {

							return factory.adapt(rel);
						}
					}

					return null;
				}

				@Override
				public  Iterable<RelationshipInterface> getRelationships(final NodeInterface node) {
					return new IterableAdapter<>(node.getNode().getRelationships(), new RelationshipFactory(node.getSecurityContext()));
				}

				@Override
				public  Iterable<RelationshipInterface> getRelationshipsAsSuperUser(final NodeInterface node) {
					return new IterableAdapter<>(node.getNode().getRelationships(), new RelationshipFactory(SecurityContext.getSuperUserInstance()));
				}

				@Override
				public  Iterable<RelationshipInterface> getIncomingRelationships(final NodeInterface node) {
					return new IterableAdapter<>(node.getNode().getRelationships(Direction.INCOMING), new RelationshipFactory(node.getSecurityContext()));
				}

				@Override
				public  Iterable<RelationshipInterface> getOutgoingRelationships(final NodeInterface node) {
					return new IterableAdapter<>(node.getNode().getRelationships(Direction.OUTGOING), new RelationshipFactory(node.getSecurityContext()));
				}

				@Override
				public  boolean hasRelationship(final NodeInterface node, final String type) {
					return getRelationships(node, type).iterator().hasNext();
				}

				@Override
				public  boolean hasIncomingRelationships(final NodeInterface node, final String type) {
					return AbstractNode.getRelationshipForType(type).getSource().hasElements(node.getSecurityContext(), node.getNode(), null);
				}

				@Override
				public  boolean hasOutgoingRelationships(final NodeInterface node, final String type) {
					return AbstractNode.getRelationshipForType(type).getTarget().hasElements(node.getSecurityContext(), node.getNode(), null);
				}

				@Override
				public  Iterable<RelationshipInterface> getRelationships(final NodeInterface node, final String type) {

					final RelationshipFactory factory = new RelationshipFactory(node.getSecurityContext());
					final Relation template           = getRelationshipForType(type);
					final Direction direction         = template.getDirectionForType(entityType);
					final RelationshipType relType    = template;

					return new IterableAdapter<>(node.getNode().getRelationships(direction, relType), factory);
				}

				@Override
				public  RelationshipInterface getIncomingRelationship(final NodeInterface node, final String type) {

					final RelationshipFactory factory = new RelationshipFactory(node.getSecurityContext());
					final Relation template           = getRelationshipForType(type);
					final Relationship relationship   = template.getSource().getRawSource(node.getSecurityContext(), node.getNode(), null);

					if (relationship != null) {
						return factory.adapt(relationship);
					}

					return null;
				}

				@Override
				public  RelationshipInterface getIncomingRelationshipAsSuperUser(final NodeInterface node, final String type) {

					final SecurityContext suContext   = SecurityContext.getSuperUserInstance();
					final RelationshipFactory factory = new RelationshipFactory(node.getSecurityContext());
					final Relation template           = getRelationshipForType(type);
					final Relationship relationship   = template.getSource().getRawSource(suContext, node.getNode(), null);

					if (relationship != null) {
						return factory.adapt(relationship);
					}

					return null;
				}

				@Override
				public  Iterable<RelationshipInterface> getIncomingRelationships(final NodeInterface node, final String type) {

					final RelationshipFactory factory = new RelationshipFactory(node.getSecurityContext());
					final Relation template           = getRelationshipForType(type);

					return new IterableAdapter<>(template.getSource().getRawSource(node.getSecurityContext(), node.getNode(), null), factory);
				}

				@Override
				public  Iterable<RelationshipInterface> getIncomingRelationshipsAsSuperUser(final NodeInterface node, final String type, final Predicate<NodeInterface> predicate) {

					final SecurityContext suContext   = SecurityContext.getSuperUserInstance();
					final RelationshipFactory factory = new RelationshipFactory(securityContext);
					final Relation template           = getRelationshipForType(type);

					return new IterableAdapter<>(template.getSource().getRawSource(suContext, node.getNode(), predicate), factory);
				}

				@Override
				public  RelationshipInterface getOutgoingRelationship(final NodeInterface node, final String type) {

					final RelationshipFactory factory = new RelationshipFactory(node.getSecurityContext());
					final Relation template                  = getRelationshipForType(type);
					final Relationship relationship   = template.getTarget().getRawSource(node.getSecurityContext(), node.getNode(), null);

					if (relationship != null) {
						return factory.adapt(relationship);
					}

					return null;
				}

				@Override
				public  RelationshipInterface getOutgoingRelationshipAsSuperUser(final NodeInterface node, final String type) {

					final SecurityContext suContext   = SecurityContext.getSuperUserInstance();
					final RelationshipFactory factory = new RelationshipFactory(node.getSecurityContext());
					final Relation template                  = getRelationshipForType(type);
					final Relationship relationship   = template.getTarget().getRawSource(suContext, node.getNode(), null);

					if (relationship != null) {
						return factory.adapt(relationship);
					}

					return null;
				}

				@Override
				public  Iterable<RelationshipInterface> getOutgoingRelationships(final NodeInterface node, final String type) {

					final RelationshipFactory factory = new RelationshipFactory(node.getSecurityContext());
					final Relation template           = getRelationshipForType(type);

					return new IterableAdapter<>(template.getTarget().getRawSource(node.getSecurityContext(), node.getNode(), null), factory);
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
