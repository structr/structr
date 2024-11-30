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

import org.structr.api.Predicate;
import org.structr.api.search.Occurrence;
import org.structr.api.search.QueryPredicate;
import org.structr.api.search.SortOrder;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.GraphObject;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Wrapper representing a part of a search query. All parts of a search query
 * must have a search operator and a payload. The payload can be either a node
 * attribute oder a group of serach attributes.
 *
 * @param <T>
 */
public abstract class SearchAttribute<T> extends NodeAttribute<T> implements Predicate<GraphObject>, QueryPredicate {

	public static final String WILDCARD = "*";

	private Comparator<GraphObject> comparator = null;
 	private Set<org.structr.core.GraphObject> result          = new LinkedHashSet<>();
	private Occurrence occur                 = null;
	private SortOrder sortOrder              = null;

	public abstract boolean includeInResult(final GraphObject entity);

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

	public void setResult(final Set<org.structr.core.GraphObject> result) {
		this.result = result;
	}

	public Set<org.structr.core.GraphObject> getResult() {
		return result;
	}

	public void addToResult(final org.structr.core.GraphObject graphObject) {
		result.add(graphObject);
	}

	public void addToResult(final Set<org.structr.core.GraphObject> list) {
		result.addAll(list);
	}

	public void setExactMatch(final boolean exact) {
	};

	public void setComparator(final Comparator<GraphObject> comparator) {
		this.comparator = comparator;
	}

	public void setOccurrence(final Occurrence occurrence) {
		this.occur = occurrence;
	}

	public void setSortOrder(final SortOrder sortOrder) {
		this.sortOrder = sortOrder;
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
	public String getLabel() {

		final PropertyKey key = getKey();
		if (key != null) {

			final Class declaringClass = key.getDeclaringTrait();
			if (declaringClass != null && !org.structr.core.GraphObject.class.equals(declaringClass) && !RelationshipInterface.class.isAssignableFrom(declaringClass)) {

				return declaringClass.getSimpleName();
			}
		}

		return null;
	}

	@Override
	public SortOrder getSortOrder() {
		return this.sortOrder;
	}
}
