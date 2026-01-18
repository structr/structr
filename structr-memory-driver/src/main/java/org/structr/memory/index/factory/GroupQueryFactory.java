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
package org.structr.memory.index.factory;

import org.structr.api.index.AbstractIndex;
import org.structr.api.index.AbstractQueryFactory;
import org.structr.api.search.GroupQuery;
import org.structr.api.search.QueryPredicate;
import org.structr.memory.index.MemoryQuery;
import org.structr.memory.index.predicate.Conjunction;

/**
 *
 */
public class GroupQueryFactory extends AbstractQueryFactory<MemoryQuery> {

	public GroupQueryFactory(final AbstractIndex index) {
		super(index);
	}

	@Override
	public boolean createQuery(final QueryPredicate predicate, final MemoryQuery query, final boolean isFirst) {

		if (predicate instanceof GroupQuery group) {

			boolean newGroup = false;

			switch (group.getOperation()) {

				case AND:
					query.beginGroup(Conjunction.And);
					newGroup = true;
					break;
				case OR:
					query.beginGroup(Conjunction.Or);
					newGroup = true;
					break;
				case NOT:
					query.not();
					break;
			}

			// Apply all type queries first as they affect as different part of the query expression
			for (final QueryPredicate p : group.getQueryPredicates()) {

				index.createQuery(p, query, isFirst);
			}

			if (newGroup) {
				query.endGroup();
			}

			return true;
		}

		return false;
	}
}
