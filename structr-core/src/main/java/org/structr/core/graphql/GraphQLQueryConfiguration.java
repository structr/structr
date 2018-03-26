/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.core.graphql;

import java.util.LinkedHashSet;
import java.util.Set;
import org.structr.core.property.PropertyKey;

/**
 */
public class GraphQLQueryConfiguration {

	private Set<PropertyKey> propertyKeys = new LinkedHashSet<>();
	private PropertyKey sortKey           = null;
	private int pageSize                  = Integer.MAX_VALUE;
	private int page                      = 1;
	private boolean sortDescending        = false;

	public Set<PropertyKey> getPropertyKeys() {
		return propertyKeys;
	}

	public void addPropertyKey(final PropertyKey key) {
		propertyKeys.add(key);
	}

	public void setPage(final int page) {
		this.page = page;
	}

	public int getPage() {
		return page;
	}

	public void setPageSize(final int pageSize) {
		this.pageSize = pageSize;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setSortKey(final PropertyKey sortKey) {
		this.sortKey = sortKey;
	}

	public PropertyKey getSortKey() {
		return sortKey;
	}

	public void setSortDescending(final boolean sortDescending) {
		this.sortDescending = sortDescending;
	}

	public boolean sortDescending() {
		return this.sortDescending;
	}
}
