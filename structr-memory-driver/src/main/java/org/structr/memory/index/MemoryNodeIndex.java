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
package org.structr.memory.index;

import org.structr.api.graph.Node;
import org.structr.api.search.QueryContext;
import org.structr.api.util.Iterables;
import org.structr.api.util.PagingIterable;
import org.structr.memory.MemoryDatabaseService;
import org.structr.memory.index.filter.MemoryLabelFilter;

import java.util.Set;

/**
 *
 */
public class MemoryNodeIndex extends AbstractMemoryIndex<Node> {

	public MemoryNodeIndex(final MemoryDatabaseService db) {

		super(db);
	}

	@Override
	public Iterable<Node> getResult(final MemoryQuery query) {

		final QueryContext queryContext = query.getQueryContext();
		final Set<String> labels        = query.getTypeLabels();
		Iterable<Node> result           = null;

		if (labels.isEmpty()) {

			result = Iterables.filter(query, query.sort(db.getAllNodes()));

		} else {

			result = Iterables.filter(query, query.sort(db.getFilteredNodes(new MemoryLabelFilter<>(labels))));
		}

		if (queryContext.isSliced()) {

			final int pageSize = queryContext.getPageSize();
			final int page     = queryContext.getPage();

			result = new PagingIterable<>(query.toString(), result, pageSize, page);
		}

		return result;
	}
}
