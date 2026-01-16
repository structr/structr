/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.common.error.TypeToken;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.search.SearchCommand;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;

/**
 *
 *
 */
public class OneEndpoint extends AbstractEndpoint implements Target<Relationship, NodeInterface> {

	private Relation<?, OneEndpoint> relation = null;

	public OneEndpoint(final Relation<?, OneEndpoint> relation, final String propertyName) {

		super(propertyName);

		this.relation = relation;
	}

	@Override
	public NodeInterface get(final SecurityContext securityContext, final NodeInterface node, final Predicate<GraphObject> predicate) {

		final NodeFactory nodeFactory = new NodeFactory(securityContext);
		final Relationship rel        = getRawSource(securityContext, node.getNode(), predicate);

		if (rel != null) {
			return nodeFactory.instantiate(rel.getEndNode(), rel.getId());
		}

		return null;
	}

	@Override
	public Object set(final SecurityContext securityContext, final NodeInterface sourceNode, final NodeInterface targetNode) throws FrameworkException {

		final PropertyMap properties         = new PropertyMap();
		final String relationshipType        = relation.getType();
		final NodeInterface actualTargetNode = unwrap(securityContext, relationshipType, targetNode, properties);
		final NodeInterface actualSourceNode = unwrap(securityContext, relationshipType, sourceNode, properties);

		// let relation check multiplicity
		relation.ensureCardinality(securityContext, actualSourceNode, actualTargetNode);

		if (actualSourceNode != null && actualTargetNode != null) {

			final Traits targetType = Traits.of(relation.getTargetType());
			final Traits type       = actualTargetNode.getTraits();

			if (!SearchCommand.isTypeAssignableFromOtherType(targetType, type)) {
				throw new FrameworkException(422, "Node type mismatch", new TypeToken(type.getName(), getPropertyName(), targetType.getName()));
			}

			final String storageKey            = actualSourceNode.getName() + relation.name() + actualTargetNode.getName();
			final PropertyMap notionProperties = getNotionProperties(securityContext, relationshipType, storageKey);

			if (notionProperties != null) {

				properties.putAll(notionProperties);
			}

			// create new relationship
			return StructrApp.getInstance(securityContext).create(actualSourceNode, actualTargetNode, relationshipType, properties);
		}

		return null;
	}

	@Override
	public Relationship getRawSource(final SecurityContext securityContext, final Node dbNode, final Predicate<GraphObject> predicate) {
		return getSingle(securityContext, dbNode, relation, Direction.OUTGOING, relation.getTargetType());
	}

	@Override
	public boolean hasElements(SecurityContext securityContext, Node dbNode, final Predicate<GraphObject> predicate) {
		return getRawSource(securityContext, dbNode, predicate) != null;
	}
}
