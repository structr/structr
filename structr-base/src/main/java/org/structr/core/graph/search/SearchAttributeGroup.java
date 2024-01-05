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

import org.structr.api.search.GroupQuery;
import org.structr.api.search.Occurrence;
import org.structr.api.search.QueryPredicate;
import org.structr.core.GraphObject;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents a group of search operators, to be used for queries with multiple textual search attributes grouped by parenthesis.
 *
 *
 */
public class SearchAttributeGroup extends SearchAttribute implements GroupQuery {

	private List<SearchAttribute> searchItems = new LinkedList<>();
	private SearchAttributeGroup parent       = null;

	public SearchAttributeGroup(final Occurrence occur) {
		this(null, occur);
	}

	public SearchAttributeGroup(final SearchAttributeGroup parent, final Occurrence occur) {

		super(occur);
		this.parent = parent;
	}

	@Override
	public String toString() {

		final StringBuilder buf = new StringBuilder("SearchAttributeGroup(");

		for (final Iterator<SearchAttribute> it = searchItems.iterator(); it.hasNext();) {

			final SearchAttribute item = it.next();

			buf.append(item.toString());

			if (it.hasNext()) {
				buf.append(", ");
			}
		}

		buf.append(")");

		return buf.toString();
	}

	public final SearchAttributeGroup getParent() {
		return parent;
	}

	public final void setSearchAttributes(final List<SearchAttribute> searchItems) {
		this.searchItems = searchItems;
	}

	public List<SearchAttribute> getSearchAttributes() {
		return searchItems;
	}

	public void add(final SearchAttribute searchAttribute) {
		searchItems.add(searchAttribute);
	}

	@Override
	public boolean isExactMatch() {

		boolean exactMatch = true;

		for (SearchAttribute attr : getSearchAttributes()) {

			exactMatch &= attr.isExactMatch();
		}

		return exactMatch;
	}

	@Override
	public boolean includeInResult(GraphObject entity) {

		boolean includeInResult = true;

		for (SearchAttribute attr : getSearchAttributes()) {

			switch (attr.getOccurrence()) {

				case FORBIDDEN:
				case REQUIRED:
					includeInResult &= attr.includeInResult(entity);
					break;

				case OPTIONAL:
					// special behaviour for OR'ed predicates
					if (attr.includeInResult(entity)) {

						// we're in or mode, return
						// immediately
						return true;

					} else {

						// set result flag to false
						// and evaluate next search predicate
						includeInResult = false;
					}
					break;
			}
		}

		return includeInResult;
	}

	@Override
	public void setExactMatch(final boolean exact) {

		for (SearchAttribute attr : getSearchAttributes()) {

			attr.setExactMatch(exact);
		}

	}

	@Override
	public Class getQueryType() {
		return GroupQuery.class;
	}

	@Override
	public List<QueryPredicate> getQueryPredicates() {

		final List<QueryPredicate> predicates = new LinkedList<>();
		for (final SearchAttribute attr : searchItems) {

			predicates.add(attr);
		}

		return predicates;
	}
}
