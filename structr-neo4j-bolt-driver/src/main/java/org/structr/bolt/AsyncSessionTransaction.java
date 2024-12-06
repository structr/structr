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
import org.neo4j.driver.Result;
import org.neo4j.driver.TransactionConfig;
import org.neo4j.driver.Values;
import org.neo4j.driver.async.AsyncSession;
import org.neo4j.driver.async.AsyncTransaction;
import org.neo4j.driver.async.ResultCursor;
import org.neo4j.driver.exceptions.*;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.structr.api.NetworkException;
import org.structr.api.NotFoundException;
import org.structr.api.RetryException;
import org.structr.api.search.QueryContext;
import org.structr.api.util.Iterables;

import java.util.*;
import java.util.concurrent.CompletionStage;

/**
 *
 */
class AsyncSessionTransaction extends SessionTransaction {

	private AsyncSession session       = null;
	private AsyncTransaction tx        = null;
	private boolean closed             = false;
	private boolean forcedFailure      = false;

	public AsyncSessionTransaction(final BoltDatabaseService db, final AsyncSession session) {

		super(db);

		this.session       = session;
		this.tx            = resolveImmediately(session.beginTransactionAsync(db.getTransactionConfig(transactionId)));
		this.db            = db;
	}

	public AsyncSessionTransaction(final BoltDatabaseService db, final AsyncSession session, final int timeoutInSeconds) {

		super(db);

		final TransactionConfig config = db.getTransactionConfigForTimeout(timeoutInSeconds, transactionId);

		this.transactionId = ID_SOURCE.getAndIncrement();
		this.session       = session;
		this.tx            = resolveImmediately(session.beginTransactionAsync(config));
		this.db            = db;
	}

	@Override
	public void failure() {
		forcedFailure = true;
	}

	@Override
	public void success() {

		// transaction must be marked successfull explicitly
		success = true;
	}

	@Override
	public boolean isSuccessful() {
		return success && !forcedFailure;
	}

	@Override
	public void close() {

		clearChangeset();

		if (forcedFailure || !success) {

			try {
				resolveImmediately(tx.rollbackAsync());

			} catch (TransientException tex) {
				closed = true;
				throw new RetryException(tex);
			} catch (NoSuchRecordException nex) {
				throw new NotFoundException(nex);
			} catch (ServiceUnavailableException ex) {
				throw new NetworkException(ex.getMessage(), ex);
			} catch (DatabaseException dex) {
				throw AsyncSessionTransaction.translateDatabaseException(dex);
			} catch (ClientException cex) {
				throw AsyncSessionTransaction.translateClientException(cex);
			}

		} else {

			try {

				resolveImmediately(tx.commitAsync());

			} catch (TransientException tex) {
				closed = true;
				throw new RetryException(tex);
			} catch (NoSuchRecordException nex) {
				throw new NotFoundException(nex);
			} catch (ServiceUnavailableException ex) {
				throw new NetworkException(ex.getMessage(), ex);
			} catch (DatabaseException dex) {
				throw AsyncSessionTransaction.translateDatabaseException(dex);
			} catch (ClientException cex) {
				throw AsyncSessionTransaction.translateClientException(cex);
			}
		}

		// mark this transaction as closed BEFORE trying to actually close it
		// so that it is closed in case of a failure
		closed = true;

		try {

			//tx.close();
			resolveImmediately(session.closeAsync());

		} catch (TransientException tex) {

			// transient exceptions can be retried
			throw new RetryException(tex);

		} finally {

			resolveImmediately(session.closeAsync());
		}
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	public void setClosed(final boolean closed) {
		this.closed = closed;
	}

	@Override
	protected Boolean getBoolean(final CypherQuery query) {

		try {

			logQuery(query);

			final String statement        = query.getStatement();
			final Map<String, Object> map = query.getParameters();

			final ResultCursor cursor = resolveImmediately(tx.runAsync(statement, map));
			final boolean value       = resolveImmediately(cursor.peekAsync()).get(0).asBoolean();

			resolveImmediately(cursor.consumeAsync());

			return value;

		} catch (TransientException tex) {
			closed = true;
			throw new RetryException(tex);
		} catch (NoSuchRecordException nex) {
			throw new NotFoundException(nex);
		} catch (ServiceUnavailableException ex) {
			throw new NetworkException(ex.getMessage(), ex);
		} catch (DatabaseException dex) {
			throw AsyncSessionTransaction.translateDatabaseException(dex);
		} catch (ClientException cex) {
			throw AsyncSessionTransaction.translateClientException(cex);
		}
	}

	@Override
	protected Long getLong(final CypherQuery query) {

		try {

			logQuery(query);

			final String statement        = query.getStatement();
			final Map<String, Object> map = query.getParameters();

			final ResultCursor cursor = resolveImmediately(tx.runAsync(statement, map));
			final long value          = resolveImmediately(cursor.peekAsync()).get(0).asLong();

			resolveImmediately(cursor.consumeAsync());

			return value;

		} catch (TransientException tex) {
			closed = true;
			throw new RetryException(tex);
		} catch (NoSuchRecordException nex) {
			throw new NotFoundException(nex);
		} catch (ServiceUnavailableException ex) {
			throw new NetworkException(ex.getMessage(), ex);
		} catch (DatabaseException dex) {
			throw AsyncSessionTransaction.translateDatabaseException(dex);
		} catch (ClientException cex) {
			throw AsyncSessionTransaction.translateClientException(cex);
		}
	}

	@Override
	protected Node getNode(final CypherQuery query) {

		try {

			logQuery(query);

			final String statement        = query.getStatement();
			final Map<String, Object> map = query.getParameters();

			final ResultCursor cursor          = resolveImmediately(tx.runAsync(statement, map));
			final CompletionStage<Record> peek = cursor.peekAsync();
			final Record record                = resolveImmediately(peek);
			Node node                          = null;

			if (record != null) {
				node = record.get(0).asNode();
			}

			logSummary(resolveImmediately(cursor.consumeAsync()));

			return node;

		} catch (TransientException tex) {
			closed = true;
			throw new RetryException(tex);
		} catch (NoSuchRecordException nex) {
			throw new NotFoundException(nex);
		} catch (ServiceUnavailableException ex) {
			throw new NetworkException(ex.getMessage(), ex);
		} catch (DatabaseException dex) {
			throw AsyncSessionTransaction.translateDatabaseException(dex);
		} catch (ClientException cex) {
			throw AsyncSessionTransaction.translateClientException(cex);
		}
	}

	@Override
	protected Relationship getRelationship(final CypherQuery query) {

		try {

			logQuery(query);

			final String statement        = query.getStatement();
			final Map<String, Object> map = query.getParameters();

			final ResultCursor cursor = resolveImmediately(tx.runAsync(statement, map));
			final CompletionStage<Record> peek = cursor.peekAsync();
			final Record record                = resolveImmediately(peek);
			Relationship relationship          = null;

			if (record != null) {
				relationship = record.get(0).asRelationship();
			}

			logSummary(resolveImmediately(cursor.consumeAsync()));

			return relationship;

		} catch (TransientException tex) {
			closed = true;
			throw new RetryException(tex);
		} catch (NoSuchRecordException nex) {
			throw new NotFoundException(nex);
		} catch (ServiceUnavailableException ex) {
			throw new NetworkException(ex.getMessage(), ex);
		} catch (DatabaseException dex) {
			throw AsyncSessionTransaction.translateDatabaseException(dex);
		} catch (ClientException cex) {
			throw AsyncSessionTransaction.translateClientException(cex);
		}
	}

	@Override
	protected Iterable<Record> collectRecords(final CypherQuery query, final IterableQueueingRecordConsumer consumer) {

		final String statement        = query.getStatement();
		final Map<String, Object> map = query.getParameters();

		logQuery(query);

		tx.runAsync(statement, map)
			.thenCompose(cursor -> consumer.start(cursor))
			.thenCompose(cursor -> cursor.forEachAsync(consumer::accept))
			.thenAccept(summary -> consumer.finish())
			.exceptionally(t -> consumer.exception(t));

		return consumer;
	}

	@Override
	protected Iterable<Map<String, Object>> run(final CypherQuery query) {

		try {

			final String statement        = query.getStatement();
			final Map<String, Object> map = query.getParameters();

			logQuery(query);

			final ResultCursor cursor  = resolveImmediately(tx.runAsync(statement, map));
			final List<Record> records = resolveImmediately(cursor.listAsync());

			return Iterables.toList(Iterables.map(new RecordMapMapper(db), records));

		} catch (TransientException tex) {
			closed = true;
			throw new RetryException(tex);
		} catch (NoSuchRecordException nex) {
			throw new NotFoundException(nex);
		} catch (ServiceUnavailableException ex) {
			throw new NetworkException(ex.getMessage(), ex);
		} catch (DatabaseException dex) {
			throw AsyncSessionTransaction.translateDatabaseException(dex);
		} catch (ClientException cex) {
			throw AsyncSessionTransaction.translateClientException(cex);
		}
	}

	@Override
	protected void set(final String statement, final Map<String, Object> map) {

		try {

			logQuery(statement, map);

			final ResultCursor cursor   = resolveImmediately(tx.runAsync(statement, map));
			final ResultSummary summary = resolveImmediately(cursor.consumeAsync());

			logSummary(summary);

		} catch (TransientException tex) {
			closed = true;
			throw new RetryException(tex);
		} catch (NoSuchRecordException nex) {
			throw new NotFoundException(nex);
		} catch (ServiceUnavailableException ex) {
			throw new NetworkException(ex.getMessage(), ex);
		} catch (DatabaseException dex) {
			throw AsyncSessionTransaction.translateDatabaseException(dex);
		} catch (ClientException cex) {
			throw AsyncSessionTransaction.translateClientException(cex);
		}
	}

	@Override
	public Iterable<Record> newIterable(final BoltDatabaseService db, final CypherQuery query) {

		final IterableQueueingRecordConsumer consumer = new IterableQueueingRecordConsumer(db, query);
		final QueryContext context                    = query.getQueryContext();

		if (context != null && !context.isDeferred()) {
			consumer.start();
		}

		// return mapped result
		return consumer;
	}

	// ----- private methods -----
	private <T> T resolveImmediately(final CompletionStage<T> stage) {

		try {
			return stage.toCompletableFuture().get();

		} catch (Throwable t) {
			t.printStackTrace();
		}

		return null;
	}

	// ----- nested classes -----
	public class IteratorWrapper<T> implements Iterable<T> {

		private Iterator<T> iterator = null;

		public IteratorWrapper(final Iterator<T> iterator) {
			this.iterator = iterator;
		}

		@Override
		public Iterator<T> iterator() {
			return new CloseableIterator<>(iterator);
		}
	}

	public class CloseableIterator<T> implements Iterator<T>, AutoCloseable {

		private Iterator<T> iterator = null;

		public CloseableIterator(final Iterator<T> iterator) {
			this.iterator = iterator;
		}

		@Override
		public boolean hasNext() {

			try {
				return iterator.hasNext();

			} catch (ClientException dex) {
				throw AsyncSessionTransaction.translateClientException(dex);
			} catch (DatabaseException dex) {
				throw AsyncSessionTransaction.translateDatabaseException(dex);
			}
		}

		@Override
		public T next() {

			try {

				return iterator.next();

			} catch (ClientException dex) {
				throw AsyncSessionTransaction.translateClientException(dex);
			} catch (DatabaseException dex) {
				throw AsyncSessionTransaction.translateDatabaseException(dex);
			}
		}

		@Override
		public void close() throws Exception {

			if (iterator instanceof Result) {

				logSummary(((Result)iterator).consume());
			}
		}
	}
}
