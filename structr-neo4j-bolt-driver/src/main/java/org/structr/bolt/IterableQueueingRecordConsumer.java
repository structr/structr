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
import org.neo4j.driver.async.ResultCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.QueryTimer;

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 *
 */
public class IterableQueueingRecordConsumer implements Iterable<Record>, Iterator<Record>, AutoCloseable, Consumer<Record> {

	private static final Logger logger        = LoggerFactory.getLogger(IterableQueueingRecordConsumer.class);
	private final BlockingQueue<Record> queue = new LinkedBlockingQueue<>();
	private final AtomicInteger elementCount  = new AtomicInteger(0);
	private final AtomicBoolean aborted       = new AtomicBoolean(false);
	private final AtomicBoolean finished      = new AtomicBoolean(false);
	private final AtomicBoolean added         = new AtomicBoolean(false);
	private final AtomicBoolean started       = new AtomicBoolean(false);
	private QueryTimer queryTimer             = null;
	private BoltDatabaseService db            = null;
	private ResultCursor cursor               = null;
	private AdvancedCypherQuery query         = null;
	private Throwable throwable               = null;
	private boolean isClosed                  = false;

	public IterableQueueingRecordConsumer(final BoltDatabaseService db, final AdvancedCypherQuery query) {

		this.queryTimer = query.getQueryTimer();
		this.query      = query;
		this.db         = db;
	}

	public void start() {

		final String statement = query.getStatement(true);

		if (queryTimer != null) {
			queryTimer.started(statement);
		}

		final SessionTransaction tx = db.getCurrentTransaction();

		tx.setIsPing(query.getQueryContext().isPing());
		tx.collectRecords(statement, query.getParameters(), this);

		started.set(true);

		if (queryTimer != null) {
			queryTimer.querySent();
		}
	}

	@Override
	public Iterator<Record> iterator() {
		return this;
	}

	@Override
	public void close() {

		if (!isClosed) {

			if (queryTimer != null) {
				queryTimer.closed();
			}

			aborted.set(true);
			cursor.consumeAsync();

			if (queryTimer != null) {
				queryTimer.consumed();
			}
		}

		isClosed = true;
	}

	public void finish() {

		if (queryTimer != null) {
			queryTimer.finishReceived();
		}

		// This method will only be called when all the records from the current
		// result cursor have been consumed. We now need to decide whether we want
		// to fetch more.

		// This method will be called from a different thread, so there is no
		// transaction context..

		if (elementCount.get() == query.pageSize() && !aborted.get()) {

			query.nextPage();

			// there are probably more results available
			elementCount.set(0);
			finished.set(false);

			// signal other thread that new results should be fetched
			started.set(false);

			if (queryTimer != null) {
				queryTimer.nextPage();
			}

		} else {

			finished.set(true);
			added.set(true);

			if (queryTimer != null) {
				queryTimer.finished();
			}
		}
	}

	@Override
	public boolean hasNext() {

		// make the consuming thread wait for results util elements have been
		// added OR the producer has no more results (finished == true)
		final long yieldStart = System.currentTimeMillis();

		while (!added.get() || (!finished.get() && queue.isEmpty())) {

			if (throwable != null) {

				if (throwable instanceof RuntimeException) {

					throw (RuntimeException)throwable;

				} else {

					throw new RuntimeException(throwable);
				}
			}

			// start fetching of next result portion while waiting for results
			if (!started.getAndSet(true)) {

				final SessionTransaction tx = db.getCurrentTransaction(false);
				if (tx != null && !tx.isClosed()) {

					tx.collectRecords(query.getStatement(true), query.getParameters(), this);
				}
			}

			// wait for data (or exception)
			try { Thread.yield(); } catch (Throwable t) {}

			if (System.currentTimeMillis() > yieldStart + 60_000) {

				logger.warn("#######################################################################################################");
				logger.warn("IterableQueueingRecordConsumer waited for 1 minute, aborting");
				logger.warn("statement:  {}", query.getStatement(true));
				logger.warn("parameters: {}", query.getParameters());
				logger.warn("throwable:  {}", throwable);
				logger.warn("finished:   {}", finished.get());
				logger.warn("added:      {}", added.get());
				logger.warn("queue:      {}", queue);
				logger.warn("#######################################################################################################");

				query.setTimeoutViolated();

				return false;
			}
		}

		return !queue.isEmpty();
	}

	@Override
	public Record next() {
		return queue.poll();
	}

	@Override
	public void accept(final Record t) {

		if (aborted.get()) {
			return;
		}

		queue.add(t);
		added.set(true);

		elementCount.incrementAndGet();
	}

	public CompletionStage<ResultCursor> start(final ResultCursor cursor) {
		this.cursor = cursor;
		return CompletableFuture.completedFuture(cursor);
	}

	public Void exception(final Throwable t) {
		this.throwable = t;
		return null;
	}
}