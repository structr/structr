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

import java.util.Iterator;

/**
 * Times the results of a query while they are read: every record moves the end of the query, and
 * reading past the last one finishes it. Results that are not read to the end are recorded when
 * their transaction closes.
 */
public class TimedIterable<T> implements Iterable<T> {

	private final Iterable<T> source;
	private final QueryTimer timer;

	public TimedIterable(final QueryTimer timer, final Iterable<T> source) {

		this.source = source;
		this.timer  = timer;
	}

	public QueryTimer getTimer() {
		return timer;
	}

	@Override
	public Iterator<T> iterator() {

		final Iterator<T> iterator = source.iterator();

		return new Iterator<>() {

			@Override
			public boolean hasNext() {

				final boolean hasNext = iterator.hasNext();
				if (!hasNext) {

					timer.finished();
				}

				return hasNext;
			}

			@Override
			public T next() {

				final T next = iterator.next();

				timer.touch();

				return next;
			}
		};
	}
}
