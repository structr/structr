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
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyMap;

/**
 *
 *
 */
public class OneEndpoint<T extends NodeInterface> extends AbstractEndpoint implements Target<Relationship, T> {

	private Relation<?, T, ?, OneEndpoint<T>> relation = null;

	public OneEndpoint(final Relation<?, T, ?, OneEndpoint<T>> relation) {
		this.relation = relation;
	}

	@Override
	public T get(final SecurityContext securityContext, final NodeInterface node, final Predicate<NodeInterface> predicate) {

		final NodeFactory<T> nodeFactory = new NodeFactory<>(securityContext);
		final Relationship rel           = getRawSource(securityContext, node.getNode(), predicate);

		if (rel != null) {
			return nodeFactory.instantiate(rel.getEndNode(), rel.getId());
		}

		return null;
	}

	@Override
	public Object set(final SecurityContext securityContext, final NodeInterface sourceNode, final T targetNode) throws FrameworkException {

		final PropertyMap properties         = new PropertyMap();
		final NodeInterface actualTargetNode = (NodeInterface)unwrap(securityContext, relation.getClass(), targetNode, properties);
		final T actualSourceNode             = (T)unwrap(securityContext, relation.getClass(), sourceNode, properties);

		// let relation check multiplicity
		relation.ensureCardinality(securityContext, actualSourceNode, actualTargetNode);

		if (actualSourceNode != null && actualTargetNode != null) {

			final String storageKey            = actualSourceNode.getName() + relation.name() + actualTargetNode.getName();
			final PropertyMap notionProperties = getNotionProperties(securityContext, relation.getClass(), storageKey);

			if (notionProperties != null) {

				properties.putAll(notionProperties);
			}

			// create new relationship
			return StructrApp.getInstance(securityContext).create(actualSourceNode, actualTargetNode, relation.getClass(), properties);
		}

		return null;
	}

	@Override
	public Relationship getRawSource(final SecurityContext securityContext, final Node dbNode, final Predicate<NodeInterface> predicate) {
		return getSingle(securityContext, dbNode, relation, Direction.OUTGOING, relation.getTargetType());
	}

	@Override
	public boolean hasElements(SecurityContext securityContext, Node dbNode, final Predicate<NodeInterface> predicate) {
		return getRawSource(securityContext, dbNode, predicate) != null;
	}
}
