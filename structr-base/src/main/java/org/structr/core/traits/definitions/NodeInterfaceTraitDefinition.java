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
package org.structr.core.traits.definitions;

import org.structr.api.Predicate;
import org.structr.api.graph.Direction;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.IterableAdapter;
import org.structr.core.entity.*;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipFactory;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.nodeinterface.GetRelationships;
import org.structr.core.traits.operations.nodeinterface.VisitForUsage;

import java.util.Map;
import java.util.Set;

public final class NodeInterfaceTraitDefinition extends AbstractNodeTraitDefinition {

	public NodeInterfaceTraitDefinition() {
		super("NodeInterface");
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
				public RelationshipInterface getRelationshipTo(final NodeInterface node, final RelationshipType type, final NodeInterface targetNode) {

					if (node.getNode() != null && type != null && targetNode != null) {

						final RelationshipFactory factory = new RelationshipFactory(node.getSecurityContext());
						final Relationship rel = node.getNode().getRelationshipTo(type, targetNode.getNode());

						if (rel != null) {

							return factory.adapt(rel);
						}
					}

					return null;
				}

				@Override
				public Iterable<RelationshipInterface> getRelationships(final NodeInterface node) {
					return new IterableAdapter<>(node.getNode().getRelationships(), new RelationshipFactory(node.getSecurityContext()));
				}

				@Override
				public Iterable<RelationshipInterface> getRelationshipsAsSuperUser(final NodeInterface node) {
					return new IterableAdapter<>(node.getNode().getRelationships(), new RelationshipFactory(SecurityContext.getSuperUserInstance()));
				}

				@Override
				public Iterable<RelationshipInterface> getIncomingRelationships(final NodeInterface node) {
					return new IterableAdapter<>(node.getNode().getRelationships(Direction.INCOMING), new RelationshipFactory(node.getSecurityContext()));
				}

				@Override
				public Iterable<RelationshipInterface> getOutgoingRelationships(final NodeInterface node) {
					return new IterableAdapter<>(node.getNode().getRelationships(Direction.OUTGOING), new RelationshipFactory(node.getSecurityContext()));
				}

				@Override
				public boolean hasRelationship(final NodeInterface node, final String type) {
					return getRelationships(node, type).iterator().hasNext();
				}

				@Override
				public boolean hasIncomingRelationships(final NodeInterface node, final String type) {
					return getRelationForType(type).getSource().hasElements(node.getSecurityContext(), node.getNode(), null);
				}

				@Override
				public boolean hasOutgoingRelationships(final NodeInterface node, final String type) {
					return getRelationForType(type).getTarget().hasElements(node.getSecurityContext(), node.getNode(), null);
				}

				@Override
				public Iterable<RelationshipInterface> getRelationships(final NodeInterface node, final String type) {

					final RelationshipFactory factory = new RelationshipFactory(node.getSecurityContext());
					final Relation template = getRelationForType(type);
					final Direction direction = template.getDirectionForType(type);
					final RelationshipType relType = template;

					return new IterableAdapter<>(node.getNode().getRelationships(direction, relType), factory);
				}

				@Override
				public RelationshipInterface getIncomingRelationship(final NodeInterface node, final String type) {

					final RelationshipFactory factory = new RelationshipFactory(node.getSecurityContext());
					final Relation<OneStartpoint, ?> template = getRelationForType(type);
					final Relationship relationship = template.getSource().getRawSource(node.getSecurityContext(), node.getNode(), null);

					if (relationship != null) {
						return factory.adapt(relationship);
					}

					return null;
				}

				@Override
				public RelationshipInterface getIncomingRelationshipAsSuperUser(final NodeInterface node, final String type) {

					final SecurityContext suContext = SecurityContext.getSuperUserInstance();
					final RelationshipFactory factory = new RelationshipFactory(suContext);
					final Relation<OneStartpoint, ?> template = getRelationForType(type);
					final Relationship relationship = template.getSource().getRawSource(suContext, node.getNode(), null);

					if (relationship != null) {
						return factory.adapt(relationship);
					}

					return null;
				}

				@Override
				public Iterable<RelationshipInterface> getIncomingRelationships(final NodeInterface node, final String type) {

					final RelationshipFactory factory = new RelationshipFactory(node.getSecurityContext());
					final Relation<ManyStartpoint, ?> template = getRelationForType(type);

					return new IterableAdapter<>(template.getSource().getRawSource(node.getSecurityContext(), node.getNode(), null), factory);
				}

				@Override
				public Iterable<RelationshipInterface> getIncomingRelationshipsAsSuperUser(final NodeInterface node, final String type, final Predicate<GraphObject> predicate) {

					final SecurityContext suContext = SecurityContext.getSuperUserInstance();
					final RelationshipFactory factory = new RelationshipFactory(suContext);
					final Relation<ManyStartpoint, ?> template = getRelationForType(type);

					return new IterableAdapter<>(template.getSource().getRawSource(suContext, node.getNode(), predicate), factory);
				}

				@Override
				public RelationshipInterface getOutgoingRelationship(final NodeInterface node, final String type) {

					final RelationshipFactory factory = new RelationshipFactory(node.getSecurityContext());
					final Relation<?, OneEndpoint> template = getRelationForType(type);
					final Relationship relationship = template.getTarget().getRawSource(node.getSecurityContext(), node.getNode(), null);

					if (relationship != null) {
						return factory.adapt(relationship);
					}

					return null;
				}

				@Override
				public RelationshipInterface getOutgoingRelationshipAsSuperUser(final NodeInterface node, final String type) {

					final SecurityContext suContext = SecurityContext.getSuperUserInstance();
					final RelationshipFactory factory = new RelationshipFactory(suContext);
					final Relation<?, OneEndpoint> template = getRelationForType(type);
					final Relationship relationship = template.getTarget().getRawSource(suContext, node.getNode(), null);

					if (relationship != null) {
						return factory.adapt(relationship);
					}

					return null;
				}

				@Override
				public Iterable<RelationshipInterface> getOutgoingRelationships(final NodeInterface node, final String type) {

					final RelationshipFactory factory = new RelationshipFactory(node.getSecurityContext());
					final Relation<?, ManyEndpoint> template = getRelationForType(type);

					return new IterableAdapter<>(template.getTarget().getRawSource(node.getSecurityContext(), node.getNode(), null), factory);
				}
			},

			VisitForUsage.class,
			new VisitForUsage() {

				@Override
				public void visitForUsage(final NodeInterface node, final Map<String, Object> data) {

					data.put("id",   node.getUuid());
					data.put("type", node.getType());
				}
			}
		);

	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {
		return Map.of();
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		// properties
		final PropertyKey<String>  nameProperty                     = new StringProperty("name").indexed();
		final PropertyKey<Boolean> hiddenProperty                   = new BooleanProperty("hidden").indexed();
		final Property<NodeInterface> ownerProperty                 = new StartNode("owner", "PrincipalOwnsNode");
		final PropertyKey<String> ownerIdProperty                   = new EntityIdProperty("ownerId", ownerProperty);
		final PropertyKey<Iterable<NodeInterface>> granteesProperty = new StartNodes("grantees", "SecurityRelationship");
		//private static final PropertyKey<String> internalPathProperty              = new InternalPathProperty("internalEntityContextPath");

		return Set.of(
			nameProperty,
			hiddenProperty,
			ownerProperty,
			ownerIdProperty,
			granteesProperty
			//internalPathProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(

			PropertyView.Public,
			Set.of("name"),

			PropertyView.Ui,
			Set.of("name", "owner", "hidden")
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
