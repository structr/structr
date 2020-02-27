/**
 * Copyright (C) 2010-2020 Structr GmbH
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

import java.util.Iterator;
import org.structr.api.util.PagingIterator;

public class PagedQueryResult<T> implements Iterable<T> {

	private final Iterable<T> result;
	private final int page;
	private final int pageSize;

	public PagedQueryResult(final Iterable<T> result, final int page, final int pageSize) {
		this.result = result;
		this.page = page;
		this.pageSize = pageSize;
	}

	@Override
	public Iterator<T> iterator() {
		return new PagingIterator(result.iterator(), page, pageSize);
	}
}
