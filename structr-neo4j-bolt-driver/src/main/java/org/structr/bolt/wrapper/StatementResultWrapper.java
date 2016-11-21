/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.bolt.wrapper;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import org.neo4j.driver.v1.Records;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.exceptions.TransientException;
import org.structr.api.NativeResult;
import org.structr.api.RetryException;
import org.structr.api.util.Iterables;
import org.structr.bolt.BoltDatabaseService;

/**
 *
 */
public class StatementResultWrapper<T> implements NativeResult<T> {

	private MixedResultWrapper wrapper = null;
	private StatementResult result     = null;
	private BoltDatabaseService db     = null;

	public StatementResultWrapper(final BoltDatabaseService db, final StatementResult result) {

		this.wrapper = new MixedResultWrapper<>(db);
		this.result  = result;
		this.db      = db;
	}

	@Override
	public Iterator columnAs(final String name) {

		final Iterator<Value> it = result.list(Records.column(name)).iterator();

		return Iterables.map(new Function<Value, Object>() {

			@Override
			public Object apply(final Value t) {
				return wrapper.apply(t.asObject());
			}

		}, it);
	}

	@Override
	public boolean hasNext() {

		try {
			return result.hasNext();

		} catch (TransientException tex) {
			db.getCurrentTransaction().setClosed(true);
			throw new RetryException(tex);
		}
	}

	@Override
	public Map next() {
		return new MapResultWrapper(db, result.next().asMap());
	}

	@Override
	public void close() {

		/*
		if (result != null) {

			final ResultSummary summary = result.consume();

			if (summary != null && summary.counters().containsUpdates()) {

				db.invalidateQueryCache();
				NodeWrapper.clearCache();
				RelationshipWrapper.clearCache();
			}
		}
		*/
	}
}
