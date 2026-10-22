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
import org.structr.common.SortKeysComparator;

import java.util.Collections;
import java.util.List;

/**
 * A sort order of a query that the database cannot sort by, like a path through related objects,
 * natural order or an explicit position of nulls. The result is sorted in Java before it is paged.
 */
public class SortKeysSortOrder implements SortOrder {

	private final SortKeysComparator comparator;

	public SortKeysSortOrder(final SortKeysComparator comparator) {

		this.comparator = comparator;
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

		return comparator.compare(o1, o2);
	}
}
