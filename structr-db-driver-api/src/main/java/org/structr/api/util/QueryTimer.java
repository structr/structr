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
package org.structr.api.util;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Times one query, from the moment it is sent until its last record has been read, and hands the
 * duration to the QueryHistogram once, when the query is finished or its results are closed.
 */
public class QueryTimer {

	private final AtomicBoolean recorded = new AtomicBoolean(false);
	private volatile String statement    = null;
	private volatile long start          = 0L;
	private volatile long last           = 0L;

	// prevent access outside of package
	QueryTimer() {}

	public String getStatement() {
		return statement;
	}

	public void started(final String statement) {

		this.statement = statement;
		this.start     = System.nanoTime();
		this.last      = start;
	}

	public void querySent() {
		touch();
	}

	public void nextPage() {
		touch();
	}

	public void finishReceived() {
		touch();
	}

	public void closed() {
		touch();
	}

	/**
	 * Notes that a record was read, so a query whose results are not read to the end is timed until
	 * the last record that was.
	 */
	public void touch() {

		if (start != 0L) {

			last = System.nanoTime();
		}
	}

	public void consumed() {

		touch();
		record();
	}

	public void finished() {

		touch();
		record();
	}

	/**
	 * Records the query as it stands, up to its last activity, for results that were never read to the end.
	 */
	public void record() {

		if (start != 0L && statement != null && recorded.compareAndSet(false, true)) {

			QueryHistogram.record(statement, last - start);
		}
	}
}
