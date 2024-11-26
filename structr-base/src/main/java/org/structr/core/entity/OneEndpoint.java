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
package org.structr.core.entity;

import org.structr.api.Predicate;
import org.structr.api.graph.Direction;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.GraphTrait;
import org.structr.core.traits.NodeTrait;
import org.structr.core.traits.Trait;
import org.structr.core.traits.Traits;

/**
 *
 *
 */
public class OneEndpoint<T extends NodeTrait> extends AbstractEndpoint implements Target<Relationship, T> {


	private final Trait<? extends Relation<?, T, ?, OneEndpoint<T>>> trait;
	private Relation<?, T, ?, OneEndpoint<T>> relation = null;
	private final Traits traits;

	public OneEndpoint(final Relation<?, T, ?, OneEndpoint<T>> relation) {

		this.relation = relation;
		this.trait    = relation.getTrait();
		this.traits   = relation.getTraits();
	}

	@Override
	public T get(final SecurityContext securityContext, final NodeTrait node, final Predicate<GraphTrait> predicate) {

		final NodeFactory<T> nodeFactory = new NodeFactory<>(securityContext);
		final Relationship rel           = getRawSource(securityContext, node.getNode(), predicate);

		if (rel != null) {
			return nodeFactory.instantiate(rel.getEndNode(), rel.getId());
		}

		return null;
	}

	@Override
	public Object set(final SecurityContext securityContext, final NodeTrait sourceNode, final T targetNode) throws FrameworkException {

		final PropertyMap properties     = new PropertyMap();
		final NodeTrait actualTargetNode = (NodeTrait)unwrap(securityContext, traits, targetNode, properties);
		final T actualSourceNode         = (T)unwrap(securityContext, traits, sourceNode, properties);

		// let relation check multiplicity
		relation.ensureCardinality(securityContext, actualSourceNode, actualTargetNode);

		if (actualSourceNode != null && actualTargetNode != null) {

			final String storageKey            = actualSourceNode.getName() + relation.name() + actualTargetNode.getName();
			final PropertyMap notionProperties = getNotionProperties(securityContext, traits, storageKey);

			if (notionProperties != null) {

				properties.putAll(notionProperties);
			}

			// create new relationship
			return StructrApp.getInstance(securityContext).create(actualSourceNode, actualTargetNode, traits, properties);
		}

		return null;
	}

	@Override
	public Relationship getRawSource(final SecurityContext securityContext, final Node dbNode, final Predicate<GraphTrait> predicate) {
		return getSingle(securityContext, dbNode, relation, Direction.OUTGOING, relation.getTargetType());
	}

	@Override
	public boolean hasElements(SecurityContext securityContext, Node dbNode, final Predicate<GraphTrait> predicate) {
		return getRawSource(securityContext, dbNode, predicate) != null;
	}
}
