/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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

import java.util.LinkedHashSet;
import java.util.Set;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.Function;
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
 * @author Christian Morgner
 */
public class ManyStartpoint<S extends NodeInterface> extends AbstractEndpoint implements Source<Iterable<Relationship>, Iterable<S>> {

	private Relation<S, ?, ManyStartpoint<S>, ?> relation = null;

	public ManyStartpoint(final Relation<S, ?, ManyStartpoint<S>, ?> relation) {
		this.relation = relation;
	}

	@Override
	public Iterable<S> get(final SecurityContext securityContext, final NodeInterface node, final Predicate<GraphObject> predicate) {

		final NodeFactory<S> nodeFactory  = new NodeFactory<>(securityContext);
		final Iterable<Relationship> rels = getRawSource(securityContext, node.getNode(), predicate);

		if (rels != null) {

			return Iterables.map(nodeFactory, Iterables.map(new Function<Relationship, Node>() {

				@Override
				public Node apply(Relationship from) {
					return from.getStartNode();
				}

			}, rels));
		}

		return null;
	}

	@Override
	public void set(final SecurityContext securityContext, final NodeInterface targetNode, final Iterable<S> collection) throws FrameworkException {

		final App app            = StructrApp.getInstance(securityContext);
		final Set<S> toBeDeleted = new LinkedHashSet<>(Iterables.toList(get(securityContext, targetNode, null)));
		final Set<S> toBeCreated = new LinkedHashSet<>();

		if (collection != null) {
			Iterables.addAll(toBeCreated, collection);
		}

		// create intersection of both sets
		final Set<S> intersection = new LinkedHashSet<>(toBeCreated);
		intersection.retainAll(toBeDeleted);

		// intersection needs no change
		toBeCreated.removeAll(intersection);
		toBeDeleted.removeAll(intersection);

		// remove existing relationships
		for (S sourceNode : toBeDeleted) {

			for (AbstractRelationship rel : targetNode.getIncomingRelationships()) {

				final String relTypeName    = rel.getRelType().name();
				final String desiredRelType = relation.name();

				if (relTypeName.equals(desiredRelType) && rel.getSourceNode().equals(sourceNode)) {

					app.delete(rel);
				}

			}
		}

		// create new relationships
		for (S sourceNode : toBeCreated) {

			if (sourceNode != null && targetNode != null) {

				final String storageKey = sourceNode.getUuid() + relation.getClass() + targetNode.getUuid();

				relation.ensureCardinality(securityContext, sourceNode, targetNode);
				app.create(sourceNode, targetNode, relation.getClass(), getNotionProperties(securityContext, relation.getClass(), storageKey));
			}
		}
	}

	@Override
	public Iterable<Relationship> getRawSource(final SecurityContext securityContext, final Node dbNode, final Predicate<GraphObject> predicate) {
		return getMultiple(securityContext, dbNode, relation, Direction.INCOMING, relation.getSourceType(), predicate);
	}

	@Override
	public boolean hasElements(final SecurityContext securityContext, final Node dbNode, final Predicate<GraphObject> predicate) {
		return getRawSource(securityContext, dbNode, predicate).iterator().hasNext();
	}
}
