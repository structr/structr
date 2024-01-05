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
package org.structr.memory.index.predicate;

import org.structr.api.Predicate;
import org.structr.api.graph.Direction;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.api.search.GraphQuery;
import org.structr.api.util.Iterables;
import org.structr.memory.MemoryNode;
import org.structr.memory.MemoryRelationship;

import java.util.List;

/**
 */
public class NoRelationshipPredicate<T extends PropertyContainer, V> implements Predicate<T> {

	private GraphQuery graphQuery = null;
	private String key = null;

	public NoRelationshipPredicate(final String key, final GraphQuery graphQuery) {

		this.graphQuery = graphQuery;
		this.key        = key;
	}

	@Override
	public String toString() {
		return "NoRelationship(" + key + ")";
	}

	@Override
	public boolean accept(final T entity) {

		if (entity instanceof MemoryNode) {

			final MemoryNode node           = (MemoryNode)entity;
			final Direction direction       = graphQuery.getDirection();
			final String relationship       = graphQuery.getRelationship();
			final String otherLabel         = graphQuery.getOtherLabel();
			final RelationshipType relType  = RelationshipType.forName(relationship);
			final List<Relationship> rels   = Iterables.toList(node.getRelationships(direction, relType));

			if (!rels.isEmpty()) {

				for (final Relationship r : rels) {

					final MemoryRelationship rel = (MemoryRelationship)r;
					final MemoryNode otherNode   = (MemoryNode)rel.getOtherNode(node);

					if (new LabelPredicate<>(otherLabel).accept(otherNode)) {
						return false;
					}

				}
			}

			return true;
		}

		return false;
	}
}
