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
package org.structr.core.graph.search;

import org.structr.api.Predicate;
import org.structr.api.search.QueryPredicate;
import org.structr.api.search.SortOrder;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Trait;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Wrapper representing a part of a search query. All parts of a search query
 * must have a search operator and a payload. The payload can be either a node
 * attribute oder a group of search attributes.
 *
 * @param <T>
 */
public abstract class SearchAttribute<T> implements Predicate<GraphObject>, QueryPredicate {

 	private Set<GraphObject> result            = new LinkedHashSet<>();
	private Comparator<GraphObject> comparator = null;
	private SortOrder sortOrder                = null;
	protected PropertyKey<T> key               = null;
	protected T value                          = null;

	public abstract boolean includeInResult(final GraphObject entity);

	public SearchAttribute(final PropertyKey<T> key, final T value) {
		this.key    = key;
		this.value  = value;
	}

	@Override
	public String toString() {
		return key.dbName() + "=" + value;
	}

	public void setResult(final Set<GraphObject> result) {
		this.result = result;
	}

	public Set<GraphObject> getResult() {
		return result;
	}

	public void addToResult(final GraphObject graphObject) {
		result.add(graphObject);
	}

	public void setExactMatch(final boolean exact) {
	};

	public void setComparator(final Comparator<GraphObject> comparator) {
		this.comparator = comparator;
	}

	public void setSortOrder(final SortOrder sortOrder) {
		this.sortOrder = sortOrder;
	}

	public PropertyKey<T> getKey() {
		return key;
	}

	public void setValue(final T value) {
		this.value = value;
	}

	// ----- interface Predicate<GraphObject> -----
	@Override
	public boolean accept(final GraphObject obj) {
		return includeInResult(obj);
	}

	@Override
	public Comparator<GraphObject> comparator() {
		return comparator;
	}

	// ----- interface QueryPredicate -----
	@Override
	public String getName() {
		return key.dbName();
	}

	@Override
	public T getValue() {
		return value;
	}

	@Override
	public Class getType() {

		if (key != null) {

			return key.valueType();
		}

		return null;
	}

	@Override
	public String getLabel() {

		if (key != null) {

			final Trait declaringTrait = key.getDeclaringTrait();

			if (declaringTrait != null && !declaringTrait.isRelationship()) {

				final String name = declaringTrait.getLabel();

				if (!StructrTraits.GRAPH_OBJECT.equals(name)) {

					return name;
				}
			}
		}

		return null;
	}

	@Override
	public SortOrder getSortOrder() {
		return this.sortOrder;
	}
}
