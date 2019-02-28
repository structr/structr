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
package org.structr.api.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Custom iterator to allow pagination of query results.
 */
public class PagingIterator<T> implements Iterator<T> {

	private final Iterator<T> iterator;
	private final int page;
	private final int pageSize;
	private int currentIndex;
	private boolean consumed = false;

	public PagingIterator(final Iterator<T> iterator, final int page, final int pageSize) {

		this.currentIndex = 0;
		this.iterator     = iterator;
		this.page         = page;
		this.pageSize     = pageSize;

		//On initialization forward iterator to page offset.
		iterateToOffset();
	}

	private void iterateToOffset() {

		while (currentIndex < getOffset() && iterator.hasNext()) {

			iterator.next();
			currentIndex++;
		}
	}

	private int getOffset() {

		if (page == 0) {

			return 0;

		} else if (page  > 0) {

			return pageSize == Integer.MAX_VALUE ? 0 : (page - 1) * pageSize;
		}

		return 0;
	}

	private int getLimitOffset() {

		//For positive paging, reverse paging needs an alternative implementation
		return getOffset()+pageSize;
	}

	@Override
	public boolean hasNext() {

		//Iterator was exhausted before starting offset was reached.
		if (currentIndex < getOffset() && !iterator.hasNext()) {

			consumed = currentIndex > 0;

			return false;

		} else if (currentIndex >= getOffset() && currentIndex < getLimitOffset()) {

			final boolean hasNext = iterator.hasNext();
			if (!hasNext) {

				consumed = currentIndex > 0;
			}

			return hasNext;

		}

		// we need to make sure that an empty iterable is not marked as "consumed"
		consumed = currentIndex > 0;

		return false;
	}

	@Override
	public T next() {

		if (hasNext()) {

			T next = iterator.next();
			currentIndex++;

			return next;

		}

		throw new NoSuchElementException("No element available for next() call!");
	}

	public int getResultCount() {

		// exhaust iterator and return final result count
		while (iterator.hasNext()) {
			iterator.next();
			currentIndex++;
		}

		consumed = true;

		return currentIndex;
	}

	public int getPageCount() {

		final double resultCount = getResultCount();
		final double pageSize    = this.pageSize;

		return (int) Math.rint(Math.ceil(resultCount / pageSize));
	}

	public int getPageSize() {
		return this.pageSize;
	}

	public int getPage() {
		return this.page;
	}

	public boolean isConsumed() {
		return consumed;
	}
}