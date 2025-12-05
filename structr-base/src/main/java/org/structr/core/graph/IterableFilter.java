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
package org.structr.core.graph;

import org.structr.common.Filter;
import org.structr.common.SecurityContext;

import java.util.Iterator;
import java.util.Set;

/**
 * An Iterable implementation that evaluates a set of predicates evaluate for
 * each element in the collection, deciding whether to include the element or
 * not.
 *
 *
 */
public class IterableFilter<T> implements Iterable<T> {

	private final SecurityContext securityContext = null;
	private Iterator<T> sourceIterator      = null;
	private Set<Filter<T>> filters          = null;

	public IterableFilter(SecurityContext securityContext, Iterable<T> source, Set<Filter<T>> filters) {

		this.sourceIterator = source.iterator();
		this.filters = filters;
	}

	@Override
	public Iterator<T> iterator() {

		return(new Iterator<T>() {

			private boolean hasNextCalled = false;
			private T currentElement = null;

			@Override
			public boolean hasNext()
			{
				do {
					if(sourceIterator.hasNext()) {

						currentElement = sourceIterator.next();

					} else {

						currentElement = null;
					}

				} while(currentElement != null && !accept(securityContext, currentElement));

				hasNextCalled = true;

				return(currentElement != null);
			}

			@Override
			public T next()
			{
				// prevent returning the same object over and over again
				// when user doesn't call hasNext()
				if(!hasNextCalled) {

					hasNext();

				} else {

					hasNextCalled = false;
				}

				return(currentElement);
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException("IterableFilterIterator does not support removal of elements");
			}

		});
	}

	// ----- private methods -----
	private boolean accept(SecurityContext securityContext, T element) {

		boolean ret = true;

		for(Filter<T> predicate : filters) {

			predicate.setSecurityContext(securityContext);
			ret &= predicate.accept(element);
		}

		return(ret);
	}
}
