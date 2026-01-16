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
package org.structr.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * An iterable that supports pagination and result counting.
 */
public class PagingIterable<T> implements ResultStream<T> {

	private static final Logger logger    = LoggerFactory.getLogger(PagingIterable.class);
	private PagingIterator<T> iterator    = null;
	private Iterable<T> source            = null;
	private String queryTimeFormatted     = null;
	private Integer overriddenResultCount = null;
	private String description            = null;
	private int page                      = 1;
	private int pageSize                  = Integer.MAX_VALUE;
	private int skipped                   = 0;

	public PagingIterable(final String description, final Iterable<T> source) {
		this(description, source, Integer.MAX_VALUE, 1);
	}

	public PagingIterable(final String description, final Iterable<T> source, final int pageSize, final int page) {
		this(description, source, pageSize, page, 0);
	}

	public PagingIterable(final String description, final Iterable<T> source, final int pageSize, final int page, final int skipped) {

		this.description = description;
		this.source      = source;
		this.pageSize    = pageSize;
		this.page        = page;
		this.skipped    = skipped;
	}

	@Override
	public Iterator<T> iterator() {

		if (isConsumed()) {

			logger.error("PagingIterable already consumed, please use Iterables.toList() to be able to iterate a streaming result more than once.");
		}

		return getIterator();
	}

	@Override
	public int calculateTotalResultCount(final ProgressWatcher progressConsumer, final int softLimit) {
		return overriddenResultCount != null ? overriddenResultCount : getIterator().getResultCount(progressConsumer, softLimit);
	}

	@Override
	public int calculatePageCount(final ProgressWatcher progressConsumer, final int softLimit) {
		return overriddenResultCount != null && this.getPageSize() != 0 ? (int)Math.ceil( ((double)overriddenResultCount) / ((double)this.getPageSize())) : getIterator().getPageCount(progressConsumer, softLimit);
	}

	@Override
	public int getPageSize() {
		return pageSize;
	}

	@Override
	public int getPage() {
		return page;
	}

	@Override
	public void setQueryTime(String formattedTime) {
		this.queryTimeFormatted = formattedTime;
	}

	@Override
	public String getQueryTime() {
		return queryTimeFormatted;
	}

	public static final PagingIterable EMPTY_ITERABLE = new PagingIterable("EMPTY_ITERABLE", () -> new Iterator() {

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public Object next() {
			throw new IllegalStateException("This iterator is empty.");
		}

	}, Integer.MAX_VALUE, 1, 0);

	public boolean isConsumed() {
		return getIterator().isConsumed();
	}

	@Override
	public void close() {

		if (source instanceof AutoCloseable) {

			try {
				((AutoCloseable)source).close();

			} catch (Exception ex) {
				logger.error("Unable to close iterable", ex);
			}
		}
	}

	public void setOverriddenResultCount(final int resultCount) {
		this.overriddenResultCount = resultCount;
	}

	private PagingIterator<T> getIterator() {

		if (this.iterator == null) {
			this.iterator = new PagingIterator<>(description, source.iterator(), page, pageSize, skipped);
		}

		return this.iterator;
	}
}
