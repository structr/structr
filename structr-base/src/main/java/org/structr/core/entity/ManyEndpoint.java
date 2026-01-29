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
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.TypeToken;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.search.SearchCommand;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 *
 */
public class ManyEndpoint extends AbstractEndpoint implements Target<Iterable<Relationship>, Iterable<NodeInterface>> {

	private Relation<?, ManyEndpoint> relation = null;

	public ManyEndpoint(final Relation<?, ManyEndpoint> relation, final String propertyName) {

		super(propertyName);

		this.relation = relation;
	}

	@Override
	public Iterable<NodeInterface> get(final SecurityContext securityContext, final NodeInterface node, final Predicate<GraphObject> predicate) {

		final NodeFactory nodeFactory     = new NodeFactory(securityContext);
		final Iterable<Relationship> rels = getRawSource(securityContext, node.getNode(), predicate);

		if (rels != null) {

			if (predicate != null && predicate.comparator() != null) {

				final List<NodeInterface> result = Iterables.toList(Iterables.map(from -> nodeFactory.instantiate(from.getEndNode(), from.getId()), rels));

				Collections.sort(result, predicate.comparator());

				return result;

			} else {

				// sort relationships by id
				return Iterables.map(from -> nodeFactory.instantiate(from.getEndNode(), from.getId()), rels);
			}
		}

		return null;
	}

	@Override
	public Object set(final SecurityContext securityContext, final NodeInterface sourceNode, final Iterable<NodeInterface> collection) throws FrameworkException {

		final App app                                          = StructrApp.getInstance(securityContext);
		final List<RelationshipInterface> createdRelationships = new LinkedList<>();
		final PropertyMap properties                           = new PropertyMap();
		final NodeInterface actualSourceNode                   = unwrap(securityContext, relation.getType(), sourceNode, properties);
		final Set<NodeInterface> toBeDeleted                   = new LinkedHashSet<>(Iterables.toList(get(securityContext, actualSourceNode, null)));
		final Set<NodeInterface> toBeCreated                   = new LinkedHashSet<>();
		final Traits targetType                                = Traits.of(relation.getTargetType());
		final String relationType                              = relation.getType();

		if (collection != null) {

			for (final NodeInterface n : collection) {

				if (n != null) {

					final Traits type = n.getTraits();

					if (!SearchCommand.isTypeAssignableFromOtherType(targetType, type)) {
						throw new FrameworkException(422, "Node type mismatch", new TypeToken(type.getName(), getPropertyName(), targetType.getName()));
					}

					toBeCreated.add(n);
				}
			}
		}

		// create intersection of both sets
		final Set<NodeInterface> intersection = intersect(toBeCreated, toBeDeleted);
		final Map<String, NodeInterface> map  = intersection.stream().collect(Collectors.toMap(e -> e.getUuid(), e -> e));

		// remove all existing nodes from the list of to be created nodes
		// so we don't delete and re-create the relationship
		toBeCreated.removeAll(intersection);

		if (actualSourceNode != null) {

			// remove existing relationships (or set properties)
			for (NodeInterface targetNode : toBeDeleted) {

				final PropertyMap foreignProperties = new PropertyMap();
				final String uuid                   = targetNode.getUuid();

				if (map.containsKey(uuid)) {

					// if the target node UUID is in the intersection, it should not be deleted, only set properties
					final NodeInterface inputObject = map.get(uuid);

					// extract and set foreign properties from input object
					unwrap(securityContext, relationType, inputObject, foreignProperties);

					if (!foreignProperties.isEmpty()) {

						final RelationshipInterface rel = actualSourceNode.getRelationshipTo(relation, targetNode);
						if (rel != null) {

							rel.setProperties(securityContext, foreignProperties);
						}
					}

				} else {

					// delete this relationship only if the UUID of the target node is not in the intersection!
					final RelationshipInterface rel = actualSourceNode.getRelationshipTo(relation, targetNode);
					if (rel != null) {

						app.delete(rel);
					}
				}
			}

			// create new relationships
			for (NodeInterface targetNode : toBeCreated) {

				if (targetNode != null) {

					properties.clear();

					final NodeInterface actualTargetNode = unwrap(securityContext, relationType, targetNode, properties);

					// this is called in CreateRelationshipCommand as well
					//relation.ensureCardinality(securityContext, actualSourceNode, actualTargetNode);

					final PropertyMap notionProperties = getNotionProperties(securityContext, relationType, actualSourceNode.getName() + relation.name() + actualTargetNode.getName());
					if (notionProperties != null) {

						properties.putAll(notionProperties);
					}

					createdRelationships.add(app.create(actualSourceNode, actualTargetNode, relationType, properties));
				}
			}
		}

		return createdRelationships;
	}

	@Override
	public Iterable<Relationship> getRawSource(final SecurityContext securityContext, final Node dbNode, final Predicate<GraphObject> predicate) {
		return getMultiple(securityContext, dbNode, relation, Direction.OUTGOING, relation.getTargetType(), predicate);
	}

	@Override
	public boolean hasElements(SecurityContext securityContext, Node dbNode, final Predicate<GraphObject> predicate) {
		return getRawSource(securityContext, dbNode, predicate).iterator().hasNext();
	}
}
