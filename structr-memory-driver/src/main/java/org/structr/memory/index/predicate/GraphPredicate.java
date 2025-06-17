/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 */
public class GraphPredicate<T extends PropertyContainer> implements Predicate<T> {

	private GraphQuery graphQuery = null;

	public GraphPredicate(final GraphQuery graphQuery) {
		this.graphQuery = graphQuery;
	}

	@Override
	public String toString() {
		return "GRAPH(" + graphQuery + ")";
	}

	@Override
	public boolean accept(final T entity) {

		if (entity instanceof MemoryNode) {

			final MemoryNode node           = (MemoryNode)entity;
			final Direction direction       = graphQuery.getDirection();
			final String notionPropertyName = graphQuery.getNotionPropertyName();
			final String relationship       = graphQuery.getRelationship();
			final String otherLabel         = graphQuery.getOtherLabel();
			final Set<Object> actual        = new LinkedHashSet<>();
			final Set<Object> expected      = graphQuery.getValues();
			final RelationshipType relType  = RelationshipType.forName(relationship);
			final List<Relationship> rels   = Iterables.toList(node.getRelationships(direction, relType));

			if (!rels.isEmpty()) {

				if (graphQuery.isExactMatch()) {

					for (final Relationship r : rels) {

						final MemoryRelationship rel = (MemoryRelationship)r;
						final MemoryNode otherNode   = (MemoryNode)rel.getOtherNode(node);

						if (new LabelPredicate<>(otherLabel).accept(otherNode)) {

							actual.add(otherNode.getProperty(notionPropertyName));
						}
					}

					return actual.containsAll(expected) && actual.size() == expected.size();

				} else {

					for (final Relationship r : rels) {

						final MemoryRelationship rel = (MemoryRelationship)r;
						final MemoryNode otherNode   = (MemoryNode)rel.getOtherNode(node);

						if (new LabelPredicate<>(otherLabel).accept(otherNode)) {

							actual.add(otherNode.getProperty(notionPropertyName));
						}
					}

					boolean accept = true;

					for (final Object expectedValue : expected) {

						if (expectedValue instanceof String) {

							String expectedString = (String)expectedValue;

							// convert to lower case
							expectedString = expectedString.toLowerCase();

							for (final Object actualValue : actual) {

								if (actualValue instanceof String) {

									final String actualString = (String)actualValue;
									final String lowerValue   = actualString.toLowerCase();
									final boolean contains    = lowerValue.contains(expectedString);

									if (contains) {

										/*
										switch (graphQuery.getOperation()) {

											case AND:
											case OR:
												return true;
										}
										*/
									}

									accept &= contains;
								}
							}
						}
					}

					return accept;
				}
			}
		}

		return false;
	}
}