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
import org.structr.api.Predicate;
import org.structr.api.SyntaxErrorException;

import java.io.Closeable;
import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class Iterables {

	private static final Logger logger = LoggerFactory.getLogger(Iterables.class);

	/**
	 * Adds all elements of an iterable to an existing collection.
	 *
	 * @param collection The existing collection the elements of the iterable are added to
	 * @param iterable The iterable whose elements are added to the collection
	 * @return The modified collection
	 * @param <T> Type of the elements in the iterable
	 * @param <C> Type of the elements in the existing collection
	 */
	public static <T, C extends Collection<T>> C addAll(final C collection, final Iterable<? extends T> iterable) {

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
				}
			}
		}

		return collection;
	}

	public static int count(final Iterable<?> iterable) {

		int c = 0;

		try {

			for (Object o : iterable) {
				c++;
			}

		} finally {

			if (iterable instanceof AutoCloseable) {

				try {

					((AutoCloseable)iterable).close();

				} catch (Exception ex) {
				}
			}
		}

		return c;
	}

	public static boolean isEmpty(final Iterable<?> iterable) {
		return !iterable.iterator().hasNext();
	}

	public static <T> T first(final Iterable<T> iterable) {

		final Iterator<T> iterator = iterable.iterator();
		if (iterator.hasNext()) {

			return iterator.next();
		}

		return null;
	}

	public static <T> T last(final Iterable<T> iterable) {

		final Iterator<T> iterator = iterable.iterator();
		T tmp                      = null;

		while (iterator.hasNext()) {

			tmp = iterator.next();
		}

		return tmp;
	}

	public static <T> T nth(final Iterable<T> iterable, final int index) {

		final Iterator<T> iterator = iterable.iterator();
		int count                  = 0;
		T tmp                      = null;

		while (iterator.hasNext()) {

			tmp = iterator.next();

			if (count++ == index) {
				return tmp;
			}
		}

		return null;
	}

	public static <T> Iterable<T> filter(final Predicate<? super T> specification, Iterable<T> i) {
		return new FilterIterable<>(i, specification);
	}

	public static <T> Iterator<T> filter(final Predicate<? super T> specification, final Iterator<T> i) {
		return new FilterIterable.FilterIterator<>(i, specification);
	}

	public static <S, T> Iterable<T> map(final Function<? super S, ? extends T> function, final Iterable<S> from) {
		return new FilterIterable<>(new MapIterable<>(from, function), e -> e != null);
	}

	public static <T> Iterable<T> flatten(final Iterable<Iterable<T>> source) {
		return new FlatteningIterable<>(source);
	}

	public static <T> List<T> toList(final Iterable<T> iterable) {

		if (iterable instanceof List) {
			return (List<T>)iterable;
		}

		return addAll(new LinkedList<>(), iterable);
	}

	public static <T> List<T> toList(Iterator<T> iterator) {

		final List<T> list = new LinkedList<>();
		while (iterator.hasNext()) {

			final T value = iterator.next();
			if (value != null) {

				list.add(value);
			}
		}

		return list;
	}

	public static <T> Set<T> toSet(final Iterable<T> iterable) {
		return addAll(new LinkedHashSet<>(), iterable);
	}

	public static <T> Iterable<T> wrap(final Iterable<T> iterable, final UnaryOperator<T> callback) {
		return new ClosingCallbackIterable<>(iterable, callback);
	}

	private static class MapIterable<S, T> implements Iterable<T> {

		private final Iterable<S> from;
		private final Function<? super S, ? extends T> function;
		private Iterator<T> iterator = null;

		public MapIterable(final Iterable<S> from, Function<? super S, ? extends T> function) {
			this.from = from;
			this.function = function;
		}

		@Override
		public Iterator<T> iterator() {

			if (iterator == null) {
				iterator = new MapIterator<>(from.iterator(), function);
			}

			return iterator;
		}

		static class MapIterator<S, T> implements Iterator<T>, AutoCloseable {

			private final Function<? super S, ? extends T> function;
			private final Iterator<S> fromIterator;

			public MapIterator(Iterator<S> fromIterator, Function<? super S, ? extends T> function) {

				this.fromIterator = fromIterator;
				this.function     = function;
			}

			@Override
			public boolean hasNext() {
				return fromIterator.hasNext();
			}

			@Override
			public T next() {

				final S from = fromIterator.next();
				return function.apply(from);
			}

			@Override
			public void remove() {
				fromIterator.remove();
			}

			@Override
			public void close() throws Exception {

				if (fromIterator instanceof AutoCloseable) {

					((AutoCloseable)fromIterator).close();
				}
			}
		}
	}

	private static class FilterIterable<T> implements Iterable<T> {

		private final Predicate<? super T> specification;
		private final Iterable<T> iterable;
		private Iterator<T> iterator = null;

		public FilterIterable(Iterable<T> iterable, Predicate<? super T> specification) {

			this.specification = specification;
			this.iterable      = iterable;
		}

		@Override
		public Iterator<T> iterator() {

			if (iterator == null) {
				iterator = new FilterIterator<>(iterable.iterator(), specification);
			}

			return iterator;
		}

		static class FilterIterator<T> implements Iterator<T>, AutoCloseable {

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

					final T nextValue = iterator.next();

					if (nextValue != null && specification.accept(nextValue)) {

						found             = true;
						this.currentValue = nextValue;
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
				throw new UnsupportedOperationException("This iterator does not support removal of elements");
			}

			@Override
			public void close() throws Exception {

				if (iterator instanceof AutoCloseable) {

					((AutoCloseable)iterator).close();
				}
			}
		}
	}

	private static class FlatteningIterable<T> implements Iterable<T> {

		private Iterator<Iterable<T>> source = null;
		private Iterator<T> current          = null;
		private Iterator<T> iterator         = null;

		public FlatteningIterable(final Iterable<Iterable<T>> source) {
			this.source = source.iterator();
		}

		@Override
		public Iterator<T> iterator() {

			if (iterator == null) {

				iterator = new CloseableIterator<T>() {

					@Override
					public boolean hasNext() {

						if (current == null || !current.hasNext()) {

							// fetch more?
							while (source.hasNext()) {

								current = source.next().iterator();

								// does the next result have elements?
								if (current.hasNext()) {

									break;
								}
							}
						}

						return current != null && current.hasNext();
					}

					@Override
					public T next() {
						return current.next();
					}

					@Override
					public void close() throws Exception {

						if (current instanceof AutoCloseable) {

							((AutoCloseable)current).close();
						}
					}
				};
			}

			return iterator;
		}
	}

	private static class ClosingCallbackIterable<T> implements Iterable<T>, Closeable {

		private Iterable<T> iterable     = null;
		private UnaryOperator<?> callback = null;

		public ClosingCallbackIterable(final Iterable<T> iterable, UnaryOperator<?> callback) {

			this.iterable = iterable;
			this.callback = callback;
		}

		@Override
		public Iterator<T> iterator() {
			return iterable.iterator();
		}

		public void close() {
			callback.apply(null);
		}
	}
}
