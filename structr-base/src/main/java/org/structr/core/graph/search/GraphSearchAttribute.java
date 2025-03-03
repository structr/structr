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
package org.structr.core.graph.search;

import org.structr.api.graph.Direction;
import org.structr.api.graph.Identity;
import org.structr.api.graph.Node;
import org.structr.api.search.GraphQuery;
import org.structr.api.search.Occurrence;
import org.structr.core.GraphObject;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.RelationProperty;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;

/**
 *
 *
 */
public class GraphSearchAttribute<T> extends PropertySearchAttribute<T> implements GraphQuery {

	private PropertyKey notionKey = null;
	private Set<Object> values    = null;

	public GraphSearchAttribute(PropertyKey<T> key, T value, final Occurrence occurrence, final boolean exact) {
		this(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.ID_PROPERTY), key, value, occurrence, exact);
	}

	public GraphSearchAttribute(final PropertyKey notionKey, final PropertyKey<T> key, final T value, final Occurrence occurrence, final boolean exact) {

		super(key, value, occurrence, exact);
		this.notionKey = notionKey;
	}

	@Override
	public String toString() {
		return "GraphSearchAttribute()";
	}

	@Override
	public Class getQueryType() {
		return GraphQuery.class;
	}

	@Override
	public boolean includeInResult(GraphObject entity) {

		boolean includeInResult = true;

		switch (getOccurrence()) {

			case EXACT:

				{
					final Set<Object> entityValues = new LinkedHashSet<>();
					final Set<Object> givenValues  = getValues();

					collect(entityValues, entity.getProperty(getKey()));

					includeInResult = givenValues.containsAll(entityValues) && givenValues.size() == entityValues.size();
				}
				break;

			case CONTAINS:

				{
					final Set<Object> entityValues = new LinkedHashSet<>();
					final Set<Object> givenValues  = getValues();

					collect(entityValues, entity.getProperty(getKey()));

					includeInResult =  entityValues.containsAll(givenValues);
				}
				break;
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

			collect(values, value);
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

		} else {

			values.add(value);
		}
	}
}
