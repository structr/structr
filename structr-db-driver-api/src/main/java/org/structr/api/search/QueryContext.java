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
package org.structr.api.search;

/**
 * Context object for index queries.
 */
public class QueryContext {

	private boolean sliced = false;
	private int skip = -1;
	private int limit = -1;

	public QueryContext() {
	}

	public QueryContext slice(final int from, final int to) {

		sliced = true;
		skip = from;
		limit = to - from;

		return this;
	}

	public boolean isSliced () {
		return sliced;
	}

	public int getSkip() {
		return skip;
	}

	public int getLimit() {
		return limit;
	}

}
