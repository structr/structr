/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;

import java.lang.StackWalker.Option;
import java.lang.StackWalker.StackFrame;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Custom iterator to allow pagination of query results.
 */
public class PagingIterator<T> implements Iterator<T>, AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(PagingIterator.class);
	private final long startTimestamp  = System.currentTimeMillis();
	private Iterator<T> iterator       = null;
	private String description         = null;
	private boolean alreadyClosed      = false;
	private boolean consumed           = false;
	private int currentIndex           = 0;
	private int page                   = 0;
	private int pageSize               = 0;

	public PagingIterator(final String description, final Iterator<T> iterator, final int page, final int pageSize) {
		this(description, iterator, page, pageSize, 0);
	}

	public PagingIterator(final String description, final Iterator<T> iterator, final int page, final int pageSize, final int skipped) {

		this.description  = description;
		this.currentIndex = skipped;
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

		if (page > 0) {

			return pageSize == Integer.MAX_VALUE ? 0 : (page - 1) * pageSize;
		}

		return 0;
	}

	private int getLimitOffset() {

		//For positive paging, reverse paging needs an alternative implementation
		return getOffset() + pageSize;
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

	public int getResultCount(final ProgressWatcher watcher, final int softLimit) {

		// exhaust iterator and return final result count
		while (iterator.hasNext()) {

			if (currentIndex >= softLimit) {
				return -1;
			}

			if (watcher != null && !watcher.okToContinue(currentIndex)) {
				return currentIndex;
			}

			iterator.next();
			currentIndex++;
		}

		consumed = true;

		// close iterator (don't fetch more results!)
		try {
			close();

		} catch (Exception ignore) {}

		return currentIndex;
	}

	public int getPageCount(final ProgressWatcher watcher, final int softLimit) {

		final double resultCount = getResultCount(watcher, softLimit);
		final double pageSize    = this.pageSize;

		if (resultCount == -1) {
			return -1;
		}

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

	@Override
	public void close() throws Exception {

		final long queryRuntime = System.currentTimeMillis() - startTimestamp;
		if (!alreadyClosed && queryRuntime > Settings.QueryTimeLoggingThreshold.getValue(3000)) {

			logger.warn("{}: {} ms.", getDescription(description), queryRuntime);
			alreadyClosed = true;
		}

		if (iterator instanceof AutoCloseable) {

			((AutoCloseable)iterator).close();
		}
	}

	// ----- private methods -----
	private String getDescription(final String additional) {

		final Optional<StackFrame> frames = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE).walk(s -> s.dropWhile(f -> !f.getClassName().startsWith("org.structr.")).skip(5).findFirst());
		if (frames.isPresent()) {

			final StringBuilder buf = new StringBuilder();
			final StackFrame frame  = frames.get();

			buf.append(frame.getDeclaringClass().getSimpleName());
			buf.append(".");
			buf.append(frame.getMethodName());
			buf.append("()");

			if (additional != null) {

				buf.append(": ");
				buf.append(additional);
			}

			return buf.toString();
		}

		return null;
	}
}