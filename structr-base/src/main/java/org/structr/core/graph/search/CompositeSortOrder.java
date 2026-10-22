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
package org.structr.core.graph.search;

import org.structr.api.search.SortOrder;
import org.structr.api.search.SortSpec;

import java.util.Collections;
import java.util.List;

/**
 * The sort orders of several sort predicates of one query, applied one after the other: the first
 * one decides, the next one only for objects that are equal in the one before. Sorted in Java,
 * because at least one of them cannot be sorted by the database.
 */
public class CompositeSortOrder implements SortOrder {

	private final SortOrder first;
	private final SortOrder second;

	public CompositeSortOrder(final SortOrder first, final SortOrder second) {

		this.first  = first;
		this.second = second;
	}

	@Override
	public List<SortSpec> getSortElements() {

		return Collections.EMPTY_LIST;
	}

	@Override
	public boolean isEmpty() {

		return false;
	}

	@Override
	public int compare(final Object o1, final Object o2) {

		final int result = first.compare(o1, o2);
		if (result != 0) {

			return result;
		}

		return second.compare(o1, o2);
	}
}
