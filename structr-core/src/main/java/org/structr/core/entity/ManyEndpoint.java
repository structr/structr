/**
 * Copyright (C) 2010-2016 Structr GmbH
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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.function.Function;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.Predicate;
import org.neo4j.helpers.collection.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;

/**
 *
 *
 */
public class ManyEndpoint<T extends NodeInterface> extends AbstractEndpoint implements Target<Iterable<Relationship>, Iterable<T>> {

	private static final Logger logger = Logger.getLogger(ManyEndpoint.class.getName());

	private Relation<?, T, ?, ManyEndpoint<T>> relation = null;

	public ManyEndpoint(final Relation<?, T, ?, ManyEndpoint<T>> relation) {
		this.relation = relation;
	}

	@Override
	public Iterable<T> get(final SecurityContext securityContext, final NodeInterface node, final Predicate<GraphObject> predicate) {

		final NodeFactory<T> nodeFactory  = new NodeFactory<>(securityContext);
		final Iterable<Relationship> rels = getRawSource(securityContext, node.getNode(), predicate);

		if (rels != null) {

			return Iterables.map(new Function<Relationship, T>() {

				@Override
				public T apply(Relationship from) throws RuntimeException {
					return nodeFactory.instantiate(from.getEndNode(), from);
				}

			}, sort(rels));
		}

		return null;
	}

	@Override
	public Object set(final SecurityContext securityContext, final NodeInterface sourceNode, final Iterable<T> collection) throws FrameworkException {

		final App app            = StructrApp.getInstance(securityContext);
		final Set<T> toBeDeleted = new LinkedHashSet<>(Iterables.toList(get(securityContext, sourceNode, null)));
		final Set<T> toBeCreated = new LinkedHashSet<>();

		if (collection != null) {
			Iterables.addAll(toBeCreated, collection);
		}

		// create intersection of both sets
		final Set<T> intersection = new HashSet<>(toBeCreated);
		intersection.retainAll(toBeDeleted);

		// intersection needs no change
		toBeCreated.removeAll(intersection);
		toBeDeleted.removeAll(intersection);

		// remove existing relationships
		for (T targetNode : toBeDeleted) {

			for (AbstractRelationship rel : sourceNode.getOutgoingRelationships()) {

				final String relTypeName    = rel.getRelType().name();
				final String desiredRelType = relation.name();

				if (sourceNode.equals(targetNode)) {

					logger.log(Level.WARNING, "Preventing deletion of self relationship {0}-[{1}]->{2}. If you experience issue with this, please report to team@structr.com.", new Object[] { sourceNode, rel.getRelType().name(), targetNode } );

					// skip self relationships
					continue;
				}

				if (relTypeName.equals(desiredRelType) && rel.getTargetNode().equals(targetNode)) {
					app.delete(rel);
				}
			}
		}

		final List<Relation> createdRelationships = new LinkedList<>();

		// test: obtain properties from notion
		// create new relationships
		for (T targetNode : toBeCreated) {

			if (sourceNode != null && targetNode != null) {

				final String storageKey = sourceNode.getName() + relation.name() + targetNode.getName();

				relation.ensureCardinality(securityContext, sourceNode, targetNode);

				final Relation newRelation = app.create(sourceNode, targetNode, relation.getClass(), getNotionProperties(securityContext, relation.getClass(), storageKey));
				createdRelationships.add(newRelation);
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
