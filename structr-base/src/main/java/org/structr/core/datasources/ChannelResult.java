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
package org.structr.core.datasources;

import org.structr.api.util.Iterables;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.error.FrameworkException;
import org.structr.web.entity.ComponentConfiguration;
import org.structr.web.entity.dom.DOMNode;

import java.util.LinkedList;
import java.util.List;

/**
 * A reusable cached result object with result and page count.
 */
public class ChannelResult<T> {

	private static final int softLimit  = 1_000_000;

	private final List<T> values = new LinkedList<T>();
	private int totalResultCount = 0;

	public Iterable<T> getData() {
		return values;
	}

	public boolean isEmpty() {
		return values.isEmpty();
	}

	public T getFirst() {

		if (!isEmpty()) {
			return values.getFirst();
		}

		return null;
	}

	public static <T> ChannelResult<T> fromObject(final T object) {

		final ChannelResult<T> result = new ChannelResult<>();

		result.values.add(object);
		result.totalResultCount = 1;

		return result;

	}

	public static <T> ChannelResult<T> fromIterable(final Iterable<T> iterable) {

		final ChannelResult<T> result = new ChannelResult<>();

		result.values.addAll(Iterables.toList(iterable));

		if (iterable instanceof PagingIterable p) {

			result.totalResultCount = p.calculateTotalResultCount(null, softLimit);
		}

		return result;

	}

	public static <T> ChannelResult<T> fromStream(final ResultStream<T> query) {

		final ChannelResult<T> result = new ChannelResult<>();

		result.values.addAll(Iterables.toList(query));
		result.totalResultCount = query.calculateTotalResultCount(null, softLimit);

		return result;
	}

	public int getResultCount() {
		return totalResultCount;
	}
}
