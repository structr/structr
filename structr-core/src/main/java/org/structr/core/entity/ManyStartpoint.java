/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.graph.Direction;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyMap;

/**
 *
 *
 */
public class ManyStartpoint<S extends NodeInterface> extends AbstractEndpoint implements Source<Iterable<Relationship>, Iterable<S>> {

	private static final Logger logger = LoggerFactory.getLogger(ManyStartpoint.class.getName());

	private Relation<S, ?, ManyStartpoint<S>, ?> relation = null;

	public ManyStartpoint(final Relation<S, ?, ManyStartpoint<S>, ?> relation) {
		this.relation = relation;
	}

	@Override
	public Iterable<S> get(final SecurityContext securityContext, final NodeInterface node, final Predicate<GraphObject> predicate) {

		final NodeFactory<S> nodeFactory  = new NodeFactory<>(securityContext);
		final Iterable<Relationship> rels = getRawSource(securityContext, node.getNode(), predicate);

		if (rels != null) {

			return Iterables.map(new Function<Relationship, S>() {

				@Override
				public S apply(Relationship from) throws RuntimeException {
					return nodeFactory.instantiate(from.getStartNode(), from);
				}

			}, sort(rels));
		}

		return null;
	}

	@Override
	public Object set(final SecurityContext securityContext, final NodeInterface targetNode, final Iterable<S> collection) throws FrameworkException {

		final App app                            = StructrApp.getInstance(securityContext);
		final List<Relation> createdRelationship = new LinkedList<>();
		final PropertyMap properties             = new PropertyMap();
		final NodeInterface actualTargetNode     = (NodeInterface)unwrap(securityContext, relation.getClass(), targetNode, properties);
		final Set<S> toBeDeleted                 = new LinkedHashSet<>(Iterables.toList(get(securityContext, actualTargetNode, null)));
		final Set<S> toBeCreated                 = new LinkedHashSet<>();

		if (collection != null) {
			Iterables.addAll(toBeCreated, collection);
		}

		// create intersection of both sets
		final Set<S> intersection = new HashSet<>(toBeCreated);
		intersection.retainAll(toBeDeleted);

		// intersection needs no change
		toBeCreated.removeAll(intersection);
		toBeDeleted.removeAll(intersection);

		if (actualTargetNode != null) {

			// remove existing relationships
			for (S sourceNode : toBeDeleted) {

				for (Iterator<AbstractRelationship> it = actualTargetNode.getIncomingRelationships(relation.getClass()).iterator(); it.hasNext();) {

					final AbstractRelationship rel = it.next();

					if (sourceNode.equals(actualTargetNode)) {

						logger.warn("Preventing deletion of self relationship {}-[{}]->{}. If you experience issue with this, please report to team@structr.com.", new Object[] { sourceNode, rel.getRelType(), actualTargetNode } );

						// skip self relationships
						continue;
					}
					if (rel.getSourceNode().equals(sourceNode)) {
						app.delete(rel);
					}
				}
			}

			// create new relationships
			for (S sourceNode : toBeCreated) {

				if (sourceNode != null) {

					properties.clear();

					final S actualSourceNode           = (S)unwrap(securityContext, relation.getClass(), sourceNode, properties);
					final PropertyMap notionProperties = getNotionProperties(securityContext, relation.getClass(), actualSourceNode.getName() + relation.name() + actualTargetNode.getName());

					if (notionProperties != null) {

						properties.putAll(notionProperties);
					}

					relation.ensureCardinality(securityContext, actualSourceNode, actualTargetNode);

					createdRelationship.add(app.create(actualSourceNode, actualTargetNode, relation.getClass(), properties));
				}
			}
		}

		return createdRelationship;
	}

	@Override
	public Iterable<Relationship> getRawSource(final SecurityContext securityContext, final Node dbNode, final Predicate<GraphObject> predicate) {
		return getMultiple(securityContext, dbNode, relation, Direction.INCOMING, relation.getSourceType(), predicate);
	}

	public Relationship getRawTarget(final SecurityContext securityContext, final Node dbNode, final Predicate<GraphObject> predicate) {
		return getSingle(securityContext, dbNode, relation, Direction.OUTGOING, relation.getTargetType());
	}

	@Override
	public boolean hasElements(final SecurityContext securityContext, final Node dbNode, final Predicate<GraphObject> predicate) {
		return getRawSource(securityContext, dbNode, predicate).iterator().hasNext();
	}
}
