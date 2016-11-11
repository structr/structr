/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import org.structr.api.Predicate;

public class Iterables {

	public static <T, C extends Collection<T>> C addAll(C collection, Iterable<? extends T> iterable) {

		final Iterator<? extends T> iterator = iterable.iterator();

		try {
			while (iterator.hasNext()) {

				final T next = iterator.next();
				if (next != null) {

					collection.add(next);
				}
			}

		} finally {

			if (iterator instanceof AutoCloseable) {

				try {

					((AutoCloseable)iterator).close();

				} catch (Exception e) {
					// Ignore
				}
			}
		}

		return collection;
	}

	public static long count(Iterable<?> iterable) {

		long c = 0;

		for (Iterator<?> iterator = iterable.iterator(); iterator.hasNext(); iterator.next()) {
			c++;
		}

		return c;
	}

	public static <X> Iterable<X> filter(Predicate<? super X> specification, Iterable<X> i) {
		return new FilterIterable<>(i, specification);
	}

	public static <X> Iterator<X> filter(Predicate<? super X> specification, Iterator<X> i) {
		return new FilterIterable.FilterIterator<>(i, specification);
	}

	public static <FROM, TO> Iterable<TO> map(Function<? super FROM, ? extends TO> function, Iterable<FROM> from) {
		return new MapIterable<>(from, function);
	}

	public static <FROM, TO> Iterator<TO> map(Function<? super FROM, ? extends TO> function, Iterator<FROM> from) {
		return new MapIterable.MapIterator<>(from, function);
	}

	public static <T> List<T> toList(Iterable<T> iterable) {
		return addAll(new ArrayList<T>(), iterable);
	}

	public static <T> List<T> toList(Iterator<T> iterator) {

		final List<T> list = new ArrayList<>();
		while (iterator.hasNext()) {

			final T value = iterator.next();
			if (value != null) {

				list.add(value);
			}
		}

		return list;
	}

	public static <T> Set<T> toSet(Iterable<T> iterable) {
		return addAll(new HashSet<T>(), iterable);
	}

	private static class MapIterable<FROM, TO> implements Iterable<TO> {

		private final Iterable<FROM> from;
		private final Function<? super FROM, ? extends TO> function;

		public MapIterable(Iterable<FROM> from, Function<? super FROM, ? extends TO> function) {
			this.from = from;
			this.function = function;
		}

		@Override
		public Iterator<TO> iterator() {
			return new MapIterator<>(from.iterator(), function);
		}

		static class MapIterator<FROM, TO> implements Iterator<TO> {

			private final Function<? super FROM, ? extends TO> function;
			private final Iterator<FROM> fromIterator;

			public MapIterator(Iterator<FROM> fromIterator, Function<? super FROM, ? extends TO> function) {

				this.fromIterator = fromIterator;
				this.function     = function;
			}

			@Override
			public boolean hasNext() {
				return fromIterator.hasNext();
			}

			@Override
			public TO next() {

				final FROM from = fromIterator.next();
				return function.apply(from);
			}

			@Override
			public void remove() {
				fromIterator.remove();
			}
		}
	}

	private static class FilterIterable<T> implements Iterable<T> {

		private final Predicate<? super T> specification;
		private final Iterable<T> iterable;

		public FilterIterable(Iterable<T> iterable, Predicate<? super T> specification) {

			this.specification = specification;
			this.iterable      = iterable;
		}

		@Override
		public Iterator<T> iterator() {
			return new FilterIterator<>(iterable.iterator(), specification);
		}

		static class FilterIterator<T> implements Iterator<T> {

			private final Predicate<? super T> specification;
			private final Iterator<T> iterator;

			private T currentValue;
			boolean finished = false;
			boolean nextConsumed = true;

			public FilterIterator(Iterator<T> iterator, Predicate<? super T> specification) {
				this.specification = specification;
				this.iterator = iterator;
			}

			public boolean moveToNextValid() {

				boolean found = false;

				while (!found && iterator.hasNext()) {

					final T currentValue = iterator.next();

					if (currentValue != null && specification.accept(currentValue)) {

						found             = true;
						this.currentValue = currentValue;
						nextConsumed      = false;
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
			}
		}
	}
}
