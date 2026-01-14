/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.api;

import org.structr.api.util.PagingIterator;

import java.util.Iterator;

public class PagedQueryResult<T> implements Iterable<T> {

	private final String description;
	private final Iterable<T> result;
	private final int page;
	private final int pageSize;

	public PagedQueryResult(final String description, final Iterable<T> result, final int page, final int pageSize) {

		this.description = description;
		this.result      = result;
		this.page        = page;
		this.pageSize    = pageSize;
	}

	@Override
	public Iterator<T> iterator() {
		return new PagingIterator(description, result.iterator(), page, pageSize);
	}
}
