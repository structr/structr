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
package org.structr.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Converts an Iterable of source type S to an Iterable of target type T by
 * passing each element through an {@link Adapter}. This class implements lazy
 * evaluation in the sense that a call to the "next" method causes the next
 * element of the source Iterable to be converted and returned.
 *
 *
 */
public class IterableAdapter<S, T> implements Iterable<T> {

	private static final Logger logger = LoggerFactory.getLogger(IterableAdapter.class);
	private Iterator<S> sourceIterator = null;
	private Adapter<S, T> adapter      = null;
	private int size                   = -1;

	public IterableAdapter(Iterable<S> source, Adapter<S, T> adapter)
	{
		this.sourceIterator = source.iterator();
		this.adapter        = adapter;

		// try to obtain size in advance
		if (source instanceof Collection) {
			size = ((Collection)source).size();
		}
	}

	@Override
	public Iterator<T> iterator() {

		return(new Iterator<T>() {

			private T currentValue = null;
			boolean finished       = false;
			boolean nextConsumed   = true;

			public boolean moveToNextValid() {

				boolean found = false;

				while (!found && sourceIterator.hasNext()) {

					try {

						final T currentTarget = adapter.adapt(sourceIterator.next());
						if (currentTarget != null) {

							found             = true;
							this.currentValue = currentTarget;
							nextConsumed      = false;
						}

					} catch (FrameworkException fex) {
						logger.warn("Exception in iterator.", fex);
					}
				}

				if (!found) {
					finished = true;
				}

				return found;
			}

			@Override
			public T next() {

				if (!nextConsumed) {

					nextConsumed = true;
					return currentValue;

				} else {

					if (!finished) {

						if (moveToNextValid()) {

							nextConsumed = true;
							return currentValue;
						}
					}
				}

				throw new NoSuchElementException("This iterator is exhausted.");
			}

			@Override
			public boolean hasNext() {
				return !finished && (!nextConsumed || moveToNextValid());
			}

			@Override
			public void remove() {
				sourceIterator.remove();
			}

		});
	}

	public int size() {
		return size;
	}
}
