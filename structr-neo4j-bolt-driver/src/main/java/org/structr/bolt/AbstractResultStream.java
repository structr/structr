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
package org.structr.bolt;

import java.util.Iterator;
import java.util.Map;

/**
 */
abstract class AbstractResultStream<T> implements Iterable<T> {

	private Iterable<T> result     = null;
	private CypherQuery query      = null;
	private Iterator<T> current    = null;
	private BoltDatabaseService db = null;

	protected abstract Iterable<T> fetchData(final BoltDatabaseService db, final String statement, final Map<String, Object> data);

	public AbstractResultStream(final BoltDatabaseService db, final CypherQuery query) {

		this.query  = query;
		this.db     = db;
	}

	@Override
	public Iterator<T> iterator() {

		return new Iterator<T>() {

			private int remaining = 0;

			@Override
			public boolean hasNext() {

				if (current == null || !current.hasNext()) {

					// fetch more?
					if (remaining == 0) {

						// reset count
						remaining = query.pageSize();

						final String statement            = query.getStatement(true);
						final Map<String, Object> params  = query.getParameters();

						result = fetchData(db, statement, params);
						if (result != null) {

							current = result.iterator();

							// advance page
							query.nextPage();

							// does the next result have elements?
							if (!current.hasNext()) {

								// no more elements
								return false;
							}
						}
					}
				}

				return current != null && current.hasNext();
			}

			@Override
			public T next() {
				remaining--;
				return current.next();
			}
		};
	}

	public CypherQuery getQuery() {
		return query;
	}
}
