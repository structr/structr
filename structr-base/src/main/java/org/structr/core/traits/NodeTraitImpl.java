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
import org.structr.api.graph.*;
import org.structr.api.util.Iterables;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.core.IterableAdapter;
import org.structr.core.entity.*;
import org.structr.core.graph.RelationshipFactory;
import org.structr.core.property.PropertyKey;

import java.util.Collections;
import java.util.List;

public class NodeTraitImpl<T> extends GraphTraitImpl implements NodeTrait {

	protected final PropertyKey<String> nameProperty;

	protected NodeTraitImpl(final PropertyContainer node) {
		super(node);

		nameProperty = traits.get("NodeTrait").key("name");
	}

	@Override
	public Node getNode() {
		//return TransactionCommand.getCurrentTransaction().getNode(nodeId);
		return (Node)getPropertyContainer();
	}

	@Override
	public String getName() {
		return getProperty(nameProperty);
	}

	@Override
	public void onNodeCreation(SecurityContext securityContext) {

		// check all traits
	}

	@Override
	public void onNodeDeletion(SecurityContext securityContext) {

		// check all traits
	}

	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {

		// check all traits
		return false;
	}

	@Override
	public boolean hasRelationshipTo(final RelationshipType type, final NodeTrait targetNode) {

		if (getNode() != null && type != null && targetNode != null) {
			return getNode().hasRelationshipTo(type, targetNode.getNode());
		}

		return false;
	}

	@Override
	public <R extends RelationshipTrait> R getRelationshipTo(final RelationshipType type, final NodeTrait targetNode) {

		if (getNode() != null && type != null && targetNode != null) {

			final RelationshipFactory<R> factory = new RelationshipFactory<>(securityContext);
			final Relationship rel               = getNode().getRelationshipTo(type, targetNode.getNode());

			if (rel != null) {

				return factory.adapt(rel);
			}
		}

		return null;
	}

	@Override
	public final <R extends RelationshipTrait> Iterable<R> getRelationships() {
		return new IterableAdapter<>(getNode().getRelationships(), new RelationshipFactory<>(securityContext));
	}

	@Override
	public final <A extends NodeTrait, B extends NodeTrait, S extends Source, T extends Target, R extends Relation<A, B, S, T>> Iterable<R> getRelationships(final Trait<R> type) {

		final RelationshipFactory<R> factory = new RelationshipFactory<>(securityContext);
		final R template                     = type.getImplementation();
		final Direction direction            = template.getDirectionForType(type);
		final RelationshipType relType       = template;

		return new IterableAdapter<>(getNode().getRelationships(direction, relType), factory);
	}

	@Override
	public final <A extends NodeTrait, B extends NodeTrait, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> R getIncomingRelationship(final Trait<R> type) {

		final RelationshipFactory<R> factory = new RelationshipFactory<>(securityContext);
		final OneStartpoint<A> source        = type.getImplementation().getSource();
		final Relationship relationship      = source.getRawSource(securityContext, getNode(), null);

		if (relationship != null) {
			return factory.adapt(relationship);
		}

		return null;
	}

	@Override
	public final <A extends NodeTrait, B extends NodeTrait, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> Iterable<R> getIncomingRelationships(final Trait<R> type) {

		final RelationshipFactory<R> factory = new RelationshipFactory<>(securityContext);
		final ManyStartpoint<A> source       = type.getImplementation().getSource();

		return new IterableAdapter<>(source.getRawSource(securityContext, getNode(), null), factory);
	}

	@Override
	public final <A extends NodeTrait, B extends NodeTrait, S extends Source, R extends Relation<A, B, S, OneEndpoint<B>>> R getOutgoingRelationship(final Trait<R> type) {

		final RelationshipFactory<R> factory = new RelationshipFactory<>(securityContext);
		final OneEndpoint<B> target          = type.getImplementation().getTarget();
		final Relationship relationship      = target.getRawSource(securityContext, getNode(), null);

		if (relationship != null) {
			return factory.adapt(relationship);
		}

		return null;
	}

	@Override
	public final <A extends NodeTrait, B extends NodeTrait, S extends Source, R extends Relation<A, B, S, ManyEndpoint<B>>> Iterable<R> getOutgoingRelationships(final Trait<R> type) {

		final RelationshipFactory<R> factory = new RelationshipFactory<>(securityContext);
		final ManyEndpoint<B> target         = type.getImplementation().getTarget();

		return new IterableAdapter<>(target.getRawSource(securityContext, getNode(), null), factory);
	}

	@Override
	public final <R extends RelationshipTrait> Iterable<R> getIncomingRelationships() {
		return new IterableAdapter<>(getNode().getRelationships(Direction.INCOMING), new RelationshipFactory<>(securityContext));
	}

	@Override
	public final <R extends RelationshipTrait> Iterable<R> getOutgoingRelationships() {
		return new IterableAdapter<>(getNode().getRelationships(Direction.OUTGOING), new RelationshipFactory<>(securityContext));
	}

	@Override
	public final <R extends RelationshipTrait> Iterable<R> getRelationshipsAsSuperUser() {
		return new IterableAdapter<>(getNode().getRelationships(), new RelationshipFactory<>(SecurityContext.getSuperUserInstance()));
	}

	protected final <A extends NodeTrait, B extends NodeTrait, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> Iterable<R> getIncomingRelationshipsAsSuperUser(final Trait<R> type) {
		return getIncomingRelationshipsAsSuperUser(type, null);
	}

	protected final <A extends NodeTrait, B extends NodeTrait, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> Iterable<R> getIncomingRelationshipsAsSuperUser(final Trait<R> type, final Predicate<GraphTrait> predicate) {

		final SecurityContext suContext      = SecurityContext.getSuperUserInstance();
		final ManyStartpoint<A> source       = type.getImplementation().getSource();
		final RelationshipFactory<R> factory = new RelationshipFactory<>(suContext);

		return new IterableAdapter<>(source.getRawSource(suContext, getNode(), predicate), factory);
	}

	@Override
	public <A extends NodeTrait, B extends NodeTrait, S extends Source, R extends Relation<A, B, S, OneEndpoint<B>>> R getOutgoingRelationshipAsSuperUser(final Trait<R> type) {

		final SecurityContext suContext      = SecurityContext.getSuperUserInstance();
		final OneEndpoint<B> target          = type.getImplementation().getTarget();
		final RelationshipFactory<R> factory = new RelationshipFactory<>(suContext);
		final Relationship relationship      = target.getRawSource(suContext, getNode(), null);

		if (relationship != null) {
			return factory.adapt(relationship);
		}

		return null;
	}

	protected final <A extends NodeTrait, B extends NodeTrait, S extends Source, T extends Target, R extends Relation<A, B, S, T>> Iterable<R> getRelationshipsAsSuperUser(final Trait<R> type) {

		final RelationshipFactory<R> factory = new RelationshipFactory<>(SecurityContext.getSuperUserInstance());
		final R template                     = type.getImplementation();
		final RelationshipType relType       = template.getRelType();
		final Direction direction            = template.getDirectionForType(type);

		return new IterableAdapter<>(getNode().getRelationships(direction, relType), factory);
	}

	/**
	 * Return true if this node has a relationship of given type and
	 * direction.
	 *
	 * @param <A>
	 * @param <B>
	 * @param <S>
	 * @param <T>
	 * @param type
	 * @return relationships
	 */
	public final <A extends NodeTrait, B extends NodeTrait, S extends Source, T extends Target> boolean hasRelationship(final Trait<? extends Relation<A, B, S, T>> type) {
		return this.getRelationships(type).iterator().hasNext();
	}

	@Override
	public final <A extends NodeTrait, B extends NodeTrait, S extends Source, T extends Target, R extends Relation<A, B, S, T>> boolean hasIncomingRelationships(final Trait<R> type) {
		return AbstractNode.getRelationshipForType(type).getSource().hasElements(securityContext, getNode(), null);
	}

	@Override
	public final <A extends NodeTrait, B extends NodeTrait, S extends Source, T extends Target, R extends Relation<A, B, S, T>> boolean hasOutgoingRelationships(final Trait<R> type) {
		return AbstractNode.getRelationshipForType(type).getTarget().hasElements(securityContext, getNode(), null);
	}
	@Override
	public final <A extends NodeTrait, B extends NodeTrait, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> R getIncomingRelationshipAsSuperUser(final Trait<R> type) {

		final SecurityContext suContext      = SecurityContext.getSuperUserInstance();
		final OneStartpoint<A> source        = type.getImplementation().getSource();
		final RelationshipFactory<R> factory = new RelationshipFactory<>(suContext);
		final Relationship relationship      = source.getRawSource(suContext, getNode(), null);

		if (relationship != null) {
			return factory.adapt(relationship);
		}

		return null;
	}

	@Override
	public List<Security> getSecurityRelationships() {

		final List<Security> grants = Iterables.toList(getIncomingRelationshipsAsSuperUser(Trait.of(Security.class)));

		// sort list by principal name (important for diff'able export)
		Collections.sort(grants, (o1, o2) -> {

			final Principal p1 = o1.getSourceNode();
			final Principal p2 = o2.getSourceNode();
			final String n1    = p1 != null ? p1.getProperty(AbstractNode.name) : "empty";
			final String n2    = p2 != null ? p2.getProperty(AbstractNode.name) : "empty";

			if (n1 != null && n2 != null) {
				return n1.compareTo(n2);

			} else if (n1 != null) {

				return 1;

			} else if (n2 != null) {
				return -1;
			}

			return 0;
		});

		return grants;
	}

	@Override
	public boolean isDeleted() {
		return false;
	}

	@Override
	public boolean isNode() {
		return true;
	}

	public boolean allowedBySchema(final Principal principal, final Permission permission) {
		return false;
	}
}
