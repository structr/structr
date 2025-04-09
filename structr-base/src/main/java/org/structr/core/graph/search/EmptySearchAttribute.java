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

import org.apache.commons.lang3.StringUtils;
import org.structr.api.search.EmptyQuery;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;

import java.util.Iterator;

/**
 *
 *
 */
public class EmptySearchAttribute<T> extends PropertySearchAttribute<T> {

	private boolean removeFromQuery = false;

	public EmptySearchAttribute(final PropertyKey<T> key, final T value) {
		this(key, value, false);
	}

	public EmptySearchAttribute(final PropertyKey<T> key, final T value, final boolean removeFromQuery) {

		super(key, value, true);

		this.removeFromQuery = removeFromQuery;
	}

	@Override
	public String toString() {
		return "EmptySearchAttribute()";
	}

	@Override
	public Class getQueryType() {
		return EmptyQuery.class;
	}

	@Override
	public boolean includeInResult(final GraphObject entity) {

		/*
		Occurrence occur = getOccurrence();
		T searchValue    = getValue();
		T nodeValue      = entity.getProperty(getKey());

		if (occur.equals(Occurrence.FORBIDDEN)) {

			if ((nodeValue != null) && !equal(nodeValue, searchValue)) {

				// don't add, do not check other results
				return false;
			}

		} else {

			if (nodeValue != null) {

				if (!equal(nodeValue, searchValue)) {
					return false;
				}

			} else {

				if (searchValue != null) {
					return false;
				}
			}
		}
		*/

		return true;
	}

	/**
	 * Indicates whether this search attribute should be removed prior
	 * to sending a query to the backend. This method exists to allow
	 * relationship properties to signal that an empty query attribute
	 * should not modify the query since it will only be used to filter
	 * the query result afterwards.
	 *
	 * @return whether to remove this search attribute before actually querying the database
	 */
	public boolean removeFromQuery() {
		return removeFromQuery;
	}

	private boolean equal(T nodeValue, T searchValue) {

		// easy, both values are null => equal
		if (nodeValue == null && searchValue == null) {
			return true;
		}

		// node value is null, search value is non-null,
		if (nodeValue == null && searchValue != null) {
			return false;
		}

		// node value is non-null, search value is null
		// both can be lists..
		if (nodeValue != null && searchValue == null) {

			if (nodeValue instanceof Iterable) {

				return isEmptyOrValue((Iterable)nodeValue);

			} else {

				// TODO: check if this is sufficient
				return StringUtils.isBlank(nodeValue.toString());
			}
		}

		// both non-null, compare empty collections
		if (nodeValue instanceof Iterable && searchValue instanceof Iterable) {

			Iterable nodeCollection   = (Iterable)nodeValue;
			Iterable searchCollection = (Iterable)searchValue;

			final Iterator nodeIterator   = nodeCollection.iterator();
			final Iterator searchIterator = searchCollection.iterator();

			if (!nodeIterator.hasNext() && !searchIterator.hasNext()) {
				return true;
			}

			if (isEmptyOrValue(nodeCollection) && !searchIterator.hasNext()) {
				return true;
			}

			if (!nodeIterator.hasNext() && isEmptyOrValue(searchCollection)) {
				return true;
			}
		}

		return false;
	}

	private boolean isEmptyOrValue(Iterable<T> collection) {

		if (collection == null) {
			return true;
		}

		for (T t : collection) {

			if (t != null) {
				return false;
			}
		}

		return true;
	}
}
