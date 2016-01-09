/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.util.Collection;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Query;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;

/**
 *
 *
 */
public class EmptySearchAttribute<T> extends PropertySearchAttribute<T> {

	public EmptySearchAttribute(PropertyKey<T> key, T value) {
		super(key, value, BooleanClause.Occur.MUST, true);
	}

	@Override
	public String toString() {
		return "EmptySearchAttribute()";
	}

	@Override
	public Query getQuery() {
		return null;
	}

	@Override
	public boolean includeInResult(GraphObject entity) {

		BooleanClause.Occur occur   = getOccur();
		T searchValue = getValue();
		T nodeValue   = entity.getProperty(getKey());

		if (occur.equals(BooleanClause.Occur.MUST_NOT)) {

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

		return true;
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

			if (nodeValue instanceof Collection) {

				return isEmptyOrValue((Collection)nodeValue);

			} else {

				// TODO: check if this is sufficient
				return StringUtils.isBlank(nodeValue.toString());
			}
		}

		// both non-null, compare empty collections
		if (nodeValue instanceof Collection && searchValue instanceof Collection) {

			Collection nodeCollection   = (Collection)nodeValue;
			Collection searchCollection = (Collection)searchValue;

			if (nodeCollection.isEmpty() && searchCollection.isEmpty()) {
				return true;
			}

			if (isEmptyOrValue(nodeCollection) && searchCollection.isEmpty()) {
				return true;
			}

			if (nodeCollection.isEmpty() && isEmptyOrValue(searchCollection)) {
				return true;
			}
		}

		return false;
	}

	private boolean isEmptyOrValue(Collection<T> collection) {

		if (collection == null) {
			return true;
		}

		if (collection.isEmpty()) {
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
