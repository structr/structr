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
package org.structr.core.graph.search;

import org.apache.commons.lang3.StringUtils;
import org.structr.api.graph.Direction;
import org.structr.api.graph.Identity;
import org.structr.api.graph.Node;
import org.structr.api.search.GraphQuery;
import org.structr.core.GraphObject;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.RelationProperty;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 *
 *
 */
public class GraphSearchAttribute<T> extends PropertySearchAttribute<T> implements GraphQuery {

	private final PropertyKey notionKey;
	private Set<Object> values;
	private boolean byId = false;

	public GraphSearchAttribute(final PropertyKey<T> key, final T value, final boolean exact) {

		this(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.ID_PROPERTY), key, value, exact);

		byId = true;
	}

	public GraphSearchAttribute(final PropertyKey notionKey, final PropertyKey<T> key, final T value, final boolean exact) {

		super(key, value, exact);

		this.notionKey = notionKey;
	}

	@Override
	public String toString() {
		return key.jsonName() + "." + notionKey.jsonName() + "=" + value;
	}

	@Override
	public Class getQueryType() {
		return GraphQuery.class;
	}

	@Override
	public Class getType() {

		if (notionKey != null) {

			return notionKey.valueType();
		}

		return super.getType();
	}

	@Override
	public boolean includeInResult(final GraphObject entity) {

		boolean includeInResult = true;

		if (isExactMatch) {

			final Set<Object> entityValues = new LinkedHashSet<>();
			final Set<Object> searchValues = getValues();

			collect(entityValues, entity.getProperty(getKey()));

			//System.out.println("Comparing entity values " + entityValues + " to search values " + searchValues + " , exactly");

			if (searchValues.isEmpty() && entityValues.isEmpty()) {
				return true;
			}

			return searchValues.containsAll(entityValues) && searchValues.size() == entityValues.size();

		} else {

			final Set<Object> entityValues = new LinkedHashSet<>();
			final Set<Object> searchValues = getValues();

			collect(entityValues, entity.getProperty(getKey()));

			/**
			 * if the notion property key is NOT the ID key, we need to be
			 * able to differentiate between two kinds of "contains" predicates..
			 */
			if (byId) {

				//System.out.println("Comparing entity values " + entityValues + " to search values " + searchValues + " by ID, inexactly");

				includeInResult =  entityValues.containsAll(searchValues);

			} else {

				for (final Object entityValue : entityValues) {

					for (final Object searchString : searchValues) {

						if (entityValue != null && searchString != null) {

							final String entityString = entityValue.toString();
							final String givenString  = searchString.toString();

							//System.out.println("Comparing entity value " + entityString + " to search value " + searchString + " by string, inexactly");

							if (entityString.contains(givenString)) {

								return true;
							}
						}
					}
				}
			}
		}

		return includeInResult;
	}

	@Override
	public String getRelationship() {

		final PropertyKey<T> key = getKey();
		final Relation rel       = ((RelationProperty)key).getRelation(); // provoke ClassCastException if type doesn't match

		return rel.name();
	}

	@Override
	public Direction getDirection() {

		final PropertyKey<T> key = getKey();
		final RelationProperty p = (RelationProperty)key;

		switch (p.getDirectionKey()) {

			case "in":
				return Direction.INCOMING;

			case "out":
				return Direction.OUTGOING;
		}

		return Direction.BOTH;
	}

	@Override
	public String getOtherLabel() {

		final PropertyKey<T> key = getKey();

		return key.relatedType();
	}

	@Override
	public Set<Object> getValues() {

		if (values == null) {

			values = new LinkedHashSet<>();
			final Object value = getValue();

			if (!(value instanceof String s) || StringUtils.isNotBlank(s)) {

				collect(values, value);
			}
		}

		return values;
	}

	@Override
	public String getNotionPropertyName() {
		return notionKey.dbName();
	}

	@Override
	public Identity getIdentity() {

		final Object value = getValue();

		if (value instanceof NodeInterface) {

			final NodeInterface g = (NodeInterface)value;
			final Node node       = g.getNode();

			return node.getId();
		}

		if (value instanceof List) {

			final List collection = (List)value;
			if (collection.size() == 1) {

				final Object item = collection.get(0);
				if (item instanceof NodeInterface) {

					final NodeInterface g = (NodeInterface)item;
					final Node node       = g.getNode();

					return node.getId();
				}
			}
		}

		return null;
	}

	// ----- private methods -----
	private void collect(final Set<Object> values, final Object value) {

		if (value instanceof Iterable) {

			for (final Object o : (Iterable)value) {

				collect(values, o);
			}

		} else if (value instanceof GraphObject) {

			values.add(((GraphObject)value).getProperty(notionKey));

		} else if (value != null) {

			values.add(value);
		}
	}
}
