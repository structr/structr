/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with structr. If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.graph.search;

import java.util.LinkedList;
import java.util.List;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

/**
 * Represents a group of search operators, to be used for queries with multiple textual search attributes grouped by parenthesis.
 *
 * @author Axel Morgner
 */
public class SearchAttributeGroup extends SearchAttribute {

	private List<SearchAttribute> searchItems = new LinkedList<SearchAttribute>();

	public SearchAttributeGroup(final Occur occur) {
		super(occur);
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
	public Query getQuery() {
		
		BooleanQuery query = new BooleanQuery();
		
		for (SearchAttribute attr : getSearchAttributes()) {
			
			Query subQuery = attr.getQuery();
			Occur occur    = attr.getOccur();
			
			query.add(subQuery, occur);
		}
		
		return query;
	}

	@Override
	public boolean isExactMatch() {
		
		boolean exactMatch = true;
		
		for (SearchAttribute attr : getSearchAttributes()) {
			
			exactMatch &= attr.isExactMatch();
		}
		
		return exactMatch;
	}
}
