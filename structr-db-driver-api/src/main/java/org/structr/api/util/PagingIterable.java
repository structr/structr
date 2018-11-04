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
package org.structr.api.util;

import java.util.Iterator;

/**
 * An iterable that supports pagination and result counting.
 */
public class PagingIterable<T> implements ResultStream<T> {

	private PagingIterator<T> source = null;

	public PagingIterable(final Iterable<T> source) {
		this(source, Integer.MAX_VALUE, 1);
	}

	public PagingIterable(final Iterable<T> source, final int pageSize, final int page) {
		this.source = new PagingIterator<>(source.iterator(), page, pageSize);
	}

	@Override
	public Iterator<T> iterator() {
		return source;
	}

	@Override
	public int calculateTotalResultCount() {
		return source.getResultCount();
	}

	@Override
	public int calculatePageCount() {
		return source.getPageCount();
	}

	public static final PagingIterable EMPTY_ITERABLE = new PagingIterable(() -> new Iterator() {

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public Object next() {
			throw new IllegalStateException("This iterator is empty.");
		}

	}, Integer.MAX_VALUE, 1);
}
