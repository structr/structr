/**
 * Copyright (C) 2010-2014 Structr, c/o Morgner UG (haftungsbeschränkt) <structr@structr.org>
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.entity;

import java.util.LinkedHashSet;
import java.util.Set;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.Function;
import org.neo4j.helpers.collection.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;

/**
 *
 * @author Christian Morgner
 */
public class ManyEndpoint<T extends NodeInterface> extends AbstractEndpoint implements Target<Iterable<Relationship>, Iterable<T>> {

	private Relation<?, T, ?, ManyEndpoint<T>> relation = null;
	
	public ManyEndpoint(final Relation<?, T, ?, ManyEndpoint<T>> relation) {
		this.relation = relation;
	}

	@Override
	public Iterable<T> get(SecurityContext securityContext, NodeInterface node) {
		
		final NodeFactory<T> nodeFactory  = new NodeFactory<>(securityContext);
		final Iterable<Relationship> rels = getRawSource(securityContext, node.getNode());
		
		if (rels != null) {
			
			return Iterables.map(nodeFactory, Iterables.map(new Function<Relationship, Node>() {

				@Override
				public Node apply(Relationship from) {
					return from.getEndNode();
				}
				
			}, rels));
		}
		
		return null;
	}

	@Override
	public void set(final SecurityContext securityContext, final NodeInterface sourceNode, final Iterable<T> collection) throws FrameworkException {

		final App app            = StructrApp.getInstance(securityContext);
		final Set<T> toBeDeleted = new LinkedHashSet<>(Iterables.toList(get(securityContext, sourceNode)));
		final Set<T> toBeCreated = new LinkedHashSet<>();

		if (collection != null) {
			Iterables.addAll(toBeCreated, collection);
		}

		// create intersection of both sets
		final Set<T> intersection = new LinkedHashSet<>(toBeCreated);
		intersection.retainAll(toBeDeleted);

		// intersection needs no change
		toBeCreated.removeAll(intersection);
		toBeDeleted.removeAll(intersection);
		
		// remove existing relationships
		for (T targetNode : toBeDeleted) {

			for (AbstractRelationship rel : sourceNode.getOutgoingRelationships()) {

				if (rel.getRelType().equals(relation) && rel.getTargetNode().equals(targetNode)) {

					app.delete(rel);
				}

			}
		}
		
		// test: obtain properties from notion
		// create new relationships
		for (T targetNode : toBeCreated) {

			relation.ensureCardinality(securityContext, sourceNode, targetNode);

			app.create(sourceNode, targetNode, relation.getClass(), getNotionProperties(securityContext, relation.getClass(), targetNode.getUuid()));
		}
	}

	@Override
	public Iterable<Relationship> getRawSource(final SecurityContext securityContext, final Node dbNode) {
		return getMultiple(securityContext, dbNode, relation, Direction.OUTGOING, relation.getTargetType());
	}

	@Override
	public boolean hasElements(SecurityContext securityContext, Node dbNode) {
		return getRawSource(securityContext, dbNode).iterator().hasNext();
	}
}
