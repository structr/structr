/**
 * Copyright (C) 2010-2019 Structr GmbH
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

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import org.structr.api.Predicate;
import org.structr.api.search.Occurrence;
import org.structr.api.search.QueryPredicate;
import org.structr.api.search.SortType;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.property.PropertyKey;

/**
 * Wrapper representing a part of a search query. All parts of a search query must have a search operator and a payload. The payload can be either a node attribute oder a group of serach attributes.
 *
 *
 * @param <T>
 */
public abstract class SearchAttribute<T> extends NodeAttribute<T> implements Predicate<GraphObject>, QueryPredicate {

	public static final String WILDCARD = "*";

	private Comparator<GraphObject> comparator = null;
 	private Set<GraphObject> result            = new LinkedHashSet<>();
	private Occurrence occur                   = null;
	private PropertyKey sortKey                = null;
	private boolean sortDescending             = false;

	public abstract boolean includeInResult(GraphObject entity);

	public SearchAttribute() {
		this(null, null);
	}

	public SearchAttribute(Occurrence occur) {
		this(occur, null, null);
	}

	public SearchAttribute(PropertyKey<T> key, T value) {
		this(null, key, value);
	}

	public SearchAttribute(Occurrence occur, PropertyKey<T> key, T value) {

		super(key, value);
		this.occur = occur;
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

	public void addToResult(final Set<GraphObject> list) {
		result.addAll(list);
	}

	public void setExactMatch(final boolean exact) {};

	public void setSortKey(final PropertyKey sortKey) {
		this.sortKey = sortKey;
	}

	public void sortDescending(final boolean sortDescending) {
		this.sortDescending = sortDescending;
	}

	public void setComparator(final Comparator<GraphObject> comparator) {
		this.comparator = comparator;
	}

	public void setOccurrence(final Occurrence occurrence) {
		this.occur = occurrence;
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
	public Occurrence getOccurrence() {
		return occur;
	}

	@Override
	public String getName() {
		return getKey().dbName();
	}

	@Override
	public Class getType() {

		final PropertyKey key = getKey();
		if (key != null) {

			return key.valueType();
		}

		return null;
	}

	@Override
	public String getSortKey() {

		if (sortKey != null) {
			return sortKey.jsonName();
		}

		return null;
	}

	@Override
	public SortType getSortType() {

		if (sortKey != null) {
			return sortKey.getSortType();
		}

		return SortType.Default;
	}

	@Override
	public boolean sortDescending() {
		return sortDescending;
	}
}
