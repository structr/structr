/*
 * Copyright (C) 2010-2024 Structr GmbH
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
import org.structr.common.PathResolvingComparator;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;

import java.util.Collections;
import java.util.List;

public class PathPropertySortOrder implements SortOrder {

	final PathResolvingComparator comparator;

	public PathPropertySortOrder(final ActionContext actionContext, final String pathSortKey, final boolean sortDescending) {
		this.comparator = new PathResolvingComparator(actionContext, pathSortKey, sortDescending);
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
	public int compare(Object o1, Object o2) {
		if (o1 instanceof GraphObject && o2 instanceof GraphObject) {
			return this.comparator.compare((GraphObject) o1, (GraphObject) o2);
		}
		return 0;
	}
}
