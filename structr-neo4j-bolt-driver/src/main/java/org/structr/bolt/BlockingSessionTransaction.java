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
package org.structr.bolt;

import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.TransactionConfig;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.exceptions.DatabaseException;
import org.neo4j.driver.exceptions.NoSuchRecordException;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.neo4j.driver.exceptions.TransientException;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.NetworkException;
import org.structr.api.NotFoundException;
import org.structr.api.RetryException;
import org.structr.api.util.Iterables;

import java.util.Iterator;
import java.util.Map;

/**
 * A transaction on the blocking API of the Neo4j driver, for Neo4j 4 and later.
 *
 * <p>The blocking API sends a query and the request for its first records (Bolt RUN and PULL) together, so a query
 * costs one network round trip. The reactive API that ReactiveSessionTransaction uses waits for the response to RUN
 * before it sends PULL, which costs a second round trip for every query, and it requests large results in batches of
 * 256 records, which costs one more for every batch. The blocking API still streams results: it requests the records
 * in batches of the driver's fetch size while the caller iterates.
 */
class BlockingSessionTransaction extends SessionTransaction {

	private static final Logger logger = LoggerFactory.getLogger(BlockingSessionTransaction.class);

	private BoltDatabaseService db = null;
	private Session session        = null;
	private Transaction tx         = null;
	private boolean closed         = false;
	private boolean forcedFailure  = false;

	public BlockingSessionTransaction(final BoltDatabaseService db, final Session session) {

		super(db);

		this.session       = session;
		this.tx            = session.beginTransaction(db.getTransactionConfig(transactionId));
		this.db            = db;
	}

	public BlockingSessionTransaction(final BoltDatabaseService db, final Session session, final int timeoutInSeconds) {

		super(db);

		final TransactionConfig config = db.getTransactionConfigForTimeout(timeoutInSeconds, transactionId);

		this.session       = session;
		this.tx            = session.beginTransaction(config);
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

		return success && !forcedFailure && terminated == null;
	}

	@Override
	public void close() {

		try {

			closeTransaction();

		} catch (RuntimeException ex) {

			// a transaction that the database terminated can fail to roll back, the RetryException below is what counts
			if (terminated == null) {

				throw ex;
			}
		}

		throwIfTerminated();
	}

	private void closeTransaction() {

		clearChangeset();

		try {

			if (forcedFailure || !success || terminated != null) {

				tx.rollback();

			} else {

				tx.commit();
			}

		} catch (RuntimeException ex) {

			throw translate(ex);
		}

		// mark this transaction as closed BEFORE trying to actually close it
		// so that it is closed in case of a failure
		closed = true;

		try {

			session.close();

		} catch (TransientException tex) {

			// transient exceptions can be retried
			throw new RetryException(tex);

		} finally {

			// make sure that the resources are freed
			session.close();
		}

		super.close();
	}

	@Override
	public boolean isClosed() {

		return closed;
	}

	@Override
	protected Boolean getBoolean(final CypherQuery query) {

		try {

			logQuery(query);

			final Result result   = tx.run(query.getStatement(), query.getParameters());
			final Record record   = first(result);
			final boolean value   = record.get(0).asBoolean();

			logSummary(result.consume());

			return value;

		} catch (RuntimeException ex) {

			throw translate(ex);
		}
	}

	@Override
	protected Long getLong(final CypherQuery query) {

		try {

			logQuery(query);

			final Result result = tx.run(query.getStatement(), query.getParameters());
			final Record record = first(result);
			Long value          = null;

			if (record != null) {

				value = record.get(0).asLong();
			}

			logSummary(result.consume());

			return value;

		} catch (RuntimeException ex) {

			throw translate(ex);
		}
	}

	@Override
	protected Node getNode(final CypherQuery query) {

		try {

			logQuery(query);

			final Result result = tx.run(query.getStatement(), query.getParameters());
			final Record record = first(result);
			Node value          = null;

			if (record != null) {

				value = record.get(0).asNode();
			}

			logSummary(result.consume());

			return value;

		} catch (RuntimeException ex) {

			throw translate(ex);
		}
	}

	@Override
	protected Relationship getRelationship(final CypherQuery query) {

		try {

			logQuery(query);

			final Result result = tx.run(query.getStatement(), query.getParameters());
			final Record record = first(result);
			Relationship value  = null;

			if (record != null) {

				value = record.get(0).asRelationship();
			}

			logSummary(result.consume());

			return value;

		} catch (TransientException | NoSuchRecordException | ServiceUnavailableException | DatabaseException | ClientException ex) {

			throw translate(ex);

		} catch (Throwable t) {

			// the only branch here that does not rethrow, so the cause would be lost otherwise
			logger.warn("Unable to fetch relationship: {}", t.getMessage(), t);
		}

		return null;
	}

	@Override
	protected Iterable<Record> collectRecords(final CypherQuery query, final IterableQueueingRecordConsumer unused) {

		try {

			logQuery(query);

			final Result result = tx.run(query.getStatement(), query.getParameters());

			// a result can be read only once, like the iterable that ReactiveSessionTransaction returns
			return () -> new TranslatingIterator(result);

		} catch (RuntimeException ex) {

			throw translate(ex);
		}
	}

	@Override
	protected Iterable<Map<String, Object>> run(final CypherQuery query) {

		try {

			logQuery(query);

			final Result result = tx.run(query.getStatement(), query.getParameters());

			return Iterables.toList(Iterables.map(new RecordMapMapper(db), result.list()));

		} catch (RuntimeException ex) {

			throw translate(ex);
		}
	}

	@Override
	protected void set(final String statement, final Map<String, Object> map) {

		try {

			logQuery(statement, map);

			logSummary(tx.run(statement, map).consume());

		} catch (RuntimeException ex) {

			throw translate(ex);
		}
	}

	@Override
	public Iterable<Record> newIterable(final BoltDatabaseService db, final CypherQuery query) {

		return new QueryIterable(db, query);
	}

	// ----- private methods -----
	private Record first(final Result result) {

		if (result.hasNext()) {

			return result.next();
		}

		return null;
	}

	/**
	 * Translates the exceptions of the driver into those of Structr, and marks this transaction as closed or rolled
	 * back where the driver has done so.
	 */
	private RuntimeException translate(final RuntimeException ex) {

		if (ex instanceof TransientException tex) {

			closed = true;
			return terminated(tex);
		}

		if (ex instanceof NoSuchRecordException nex) {

			return new NotFoundException(nex);
		}

		if (ex instanceof ServiceUnavailableException uex) {

			return new NetworkException(uex.getMessage(), uex);
		}

		if (ex instanceof DatabaseException dex) {

			isRolledBack = true;
			return SessionTransaction.translateDatabaseException(dex);
		}

		if (ex instanceof ClientException cex) {

			isRolledBack = true;
			return SessionTransaction.translateClientException(cex);
		}

		return ex;
	}

	// ----- nested classes -----
	/**
	 * Iterates over a result and translates the exceptions that the driver throws while it fetches the next batch.
	 */
	private class TranslatingIterator implements Iterator<Record> {

		private final Result result;

		TranslatingIterator(final Result result) {

			this.result = result;
		}

		@Override
		public boolean hasNext() {

			try {

				return result.hasNext();

			} catch (RuntimeException ex) {

				throw translate(ex);
			}
		}

		@Override
		public Record next() {

			try {

				return result.next();

			} catch (RuntimeException ex) {

				throw translate(ex);
			}
		}
	}
}
