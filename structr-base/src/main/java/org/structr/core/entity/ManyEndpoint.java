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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.graph.Direction;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeFactory;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 *
 */
public class ManyEndpoint<T extends NodeTrait> extends AbstractEndpoint implements Target<Iterable<Relationship>, Iterable<T>> {

	private static final Logger logger = LoggerFactory.getLogger(ManyEndpoint.class.getName());

	private final Trait<? extends Relation<?, T, ?, ManyEndpoint<T>>> trait;
	private final Relation<?, T, ?, ManyEndpoint<T>> relation;
	private final Traits traits;

	public ManyEndpoint(final Relation<?, T, ?, ManyEndpoint<T>> relation) {

		this.relation = relation;
		this.trait    = relation.getTrait();
		this.traits   = relation.getTraits();
	}

	@Override
	public Iterable<T> get(final SecurityContext securityContext, final NodeTrait node, final Predicate<GraphTrait> predicate) {

		final NodeFactory<T> nodeFactory  = new NodeFactory<>(securityContext);
		final Iterable<Relationship> rels = getRawSource(securityContext, node.getNode(), predicate);

		if (rels != null) {

			if (predicate != null && predicate.comparator() != null) {

				final List<T> result = Iterables.toList(Iterables.map(from -> nodeFactory.instantiate(from.getEndNode(), from.getId()), rels));

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
	public Object set(final SecurityContext securityContext, final NodeTrait sourceNode, final Iterable<T> collection) throws FrameworkException {

		final App app                                                           = StructrApp.getInstance(securityContext);
		final List<Relation> createdRelationships                               = new LinkedList<>();
		final PropertyMap properties                                            = new PropertyMap();
		final T actualSourceNode                                                = (T) unwrap(securityContext, relation.getTraits(), sourceNode, properties);
		final Set<T> toBeDeleted                                                = new LinkedHashSet<>(Iterables.toList(get(securityContext, actualSourceNode, null)));
		final Set<T> toBeCreated                                                = new LinkedHashSet<>();

		if (collection != null) {
			Iterables.addAll(toBeCreated, collection);
		}

		// create intersection of both sets
		final Set<T> intersection         = intersect(toBeCreated, toBeDeleted);
		final Map<String, GraphTrait> map = intersection.stream().collect(Collectors.toMap(e -> e.getUuid(), e -> e));

		// remove all existing nodes from the list of to be created nodes
		// so we don't delete and re-create the relationship
		toBeCreated.removeAll(intersection);

		if (actualSourceNode != null) {

			// remove existing relationships (or set properties)
			for (T targetNode : toBeDeleted) {

				final PropertyMap foreignProperties = new PropertyMap();
				final String uuid                   = targetNode.getUuid();

				if (map.containsKey(uuid)) {

					// if the target node UUID is in the intersection, it should not be deleted, only set properties
					final GraphTrait inputObject = map.get(uuid);

					// extract and set foreign properties from input object
					unwrap(securityContext, traits, inputObject, foreignProperties);

					if (!foreignProperties.isEmpty()) {

						final RelationshipTrait rel = actualSourceNode.getRelationshipTo(relation, targetNode);
						if (rel != null) {

							rel.setProperties(securityContext, foreignProperties);
						}
					}

				} else {

					// delete this relationship only if the UUID of the target node is not in the intersection!
					final RelationshipTrait rel = actualSourceNode.getRelationshipTo(relation, targetNode);
					if (rel != null) {

						app.delete(rel);
					}
				}
			}

			// create new relationships
			for (T targetNode : toBeCreated) {

				if (targetNode != null) {

					properties.clear();

					final NodeTrait actualTargetNode = (NodeTrait)unwrap(securityContext, traits, targetNode, properties);

					relation.ensureCardinality(securityContext, actualSourceNode, actualTargetNode);

					final PropertyMap notionProperties = getNotionProperties(securityContext, traits, actualSourceNode.getName() + relation.name() + actualTargetNode.getName());
					if (notionProperties != null) {

						properties.putAll(notionProperties);
					}

					createdRelationships.add(app.create(actualSourceNode, actualTargetNode, traits, properties));
				}
			}
		}

		return createdRelationships;
	}

	@Override
	public Iterable<Relationship> getRawSource(final SecurityContext securityContext, final Node dbNode, final Predicate<GraphTrait> predicate) {
		return getMultiple(securityContext, dbNode, relation, Direction.OUTGOING, relation.getTargetType(), predicate);
	}

	@Override
	public boolean hasElements(SecurityContext securityContext, Node dbNode, final Predicate<GraphTrait> predicate) {
		return getRawSource(securityContext, dbNode, predicate).iterator().hasNext();
	}
}
