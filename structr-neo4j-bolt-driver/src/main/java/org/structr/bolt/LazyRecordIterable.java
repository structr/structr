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

import org.neo4j.driver.Record;

import java.util.Iterator;

public class LazyRecordIterable implements Iterable<Record> {

	private CypherQuery query = null;
	private BoltDatabaseService db    = null;

	public LazyRecordIterable(final BoltDatabaseService db, final CypherQuery query) {
		this.query = query;
		this.db    = db;
	}

	@Override
	public Iterator<Record> iterator() {

		final SessionTransaction tx     = db.getCurrentTransaction();
		final Iterable<Record> iterable = tx.newIterable(db, query);

		return iterable.iterator();
	}
}
