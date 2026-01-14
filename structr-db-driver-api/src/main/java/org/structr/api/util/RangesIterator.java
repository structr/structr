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
import java.util.NoSuchElementException;

/**
*/
public class RangesIterator<T> implements Iterator<T> {

	private final Iterator<T> iterator;
	private final Ranges ranges;
	private int currentIndex = 1;

	public RangesIterator(final Iterator<T> iterator, final String ranges) {

		this.ranges   = new Ranges(ranges);
		this.iterator = iterator;
	}

	@Override
	public boolean hasNext() {

		while (iterator.hasNext() && !ranges.contains(currentIndex)) {

			// check next?
			currentIndex++;

			// skip
			iterator.next();
		}

		return iterator.hasNext();
	}

	@Override
	public T next() {

		if (hasNext()) {

			T next = iterator.next();

			// consume next
			currentIndex++;

			return next;

		}

		throw new NoSuchElementException("No element available for next() call!");
	}
}