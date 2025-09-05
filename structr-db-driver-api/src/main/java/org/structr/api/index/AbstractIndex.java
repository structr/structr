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
package org.structr.api.index;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.search.QueryContext;
import org.structr.api.search.QueryPredicate;
import org.structr.api.search.SortOrder;
import org.structr.api.search.TypeConverter;

/**
 *
 * @param <Q>
 * @param <R>
 */
public abstract class AbstractIndex<Q extends DatabaseQuery, R extends PropertyContainer> implements Index<R> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractIndex.class.getName());

	public abstract Iterable<R> getResult(final Q query);
	public abstract Q createQuery(final QueryContext context, final int requestedPageSize, final int requestedPage);
	public abstract QueryFactory getFactoryForType(final Class type);
	public abstract TypeConverter getConverterForType(final Class type);
	public abstract DatabaseService getDatabaseService();

	@Override
	public Iterable<R> query(final QueryContext context, final QueryPredicate predicate, final int requestedPageSize, final int requestedPage) {
		return getResult(getQuery(context, predicate, requestedPageSize, requestedPage));
	}

	public boolean createQuery(final QueryPredicate predicate, final Q query, final boolean isFirst) {

		final Class type = predicate.getQueryType();
		if (type != null) {

			final QueryFactory factory = getFactoryForType(type);
			if (factory != null) {

				return factory.createQuery(predicate, query, isFirst);

			} else {

				logger.warn("No query factory registered for type {}", type);
			}
		}

		return false;
	}

	// ----- private methods -----
	private Q getQuery(final QueryContext context, final QueryPredicate predicate, final int requestedPageSize, final int requestedPage) {

		final Q query = createQuery(context, requestedPageSize, requestedPage);

		createQuery(predicate, query, true);

		final SortOrder sortOrder = predicate.getSortOrder();
		if (sortOrder != null) {

			query.sort(sortOrder);
		}

		return query;
	}
}
