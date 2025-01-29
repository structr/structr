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
import org.neo4j.driver.TransactionConfig;
import org.neo4j.driver.exceptions.*;
import org.neo4j.driver.internal.shaded.reactor.core.publisher.Flux;
import org.neo4j.driver.internal.shaded.reactor.core.publisher.Mono;
import org.neo4j.driver.reactive.RxResult;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.reactive.RxTransaction;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.reactivestreams.Publisher;
import org.structr.api.NetworkException;
import org.structr.api.NotFoundException;
import org.structr.api.RetryException;
import org.structr.api.util.Iterables;

import java.util.Map;

/**
 *
 */
class ReactiveSessionTransaction extends SessionTransaction {

	private BoltDatabaseService db = null;
	private RxSession session      = null;
	private RxTransaction tx       = null;
	private boolean closed         = false;
	private boolean forcedFailure  = false;

	public ReactiveSessionTransaction(final BoltDatabaseService db, final RxSession session) {

		super(db);

		this.transactionId = ID_SOURCE.getAndIncrement();
		this.session       = session;
		this.tx            = Flux.from(session.beginTransaction(db.getTransactionConfig(transactionId))).blockFirst();
		this.db            = db;
	}

	public ReactiveSessionTransaction(final BoltDatabaseService db, final RxSession session, final int timeoutInSeconds) {

		super(db);

		final TransactionConfig config = db.getTransactionConfigForTimeout(timeoutInSeconds, transactionId);

		this.transactionId = ID_SOURCE.getAndIncrement();
		this.session       = session;
		this.tx            = Flux.from(session.beginTransaction(config)).blockFirst();
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

				Mono.from(tx.rollback()).block();

			} catch (TransientException tex) {
				closed = true;
				throw new RetryException(tex);
			} catch (NoSuchRecordException nex) {
				throw new NotFoundException(nex);
			} catch (ServiceUnavailableException ex) {
				throw new NetworkException(ex.getMessage(), ex);
			} catch (DatabaseException dex) {
				throw ReactiveSessionTransaction.translateDatabaseException(dex);
			} catch (ClientException cex) {
				throw ReactiveSessionTransaction.translateClientException(cex);
			}

		} else {

			try {

				Mono.from(tx.commit()).block();

			} catch (TransientException tex) {
				closed = true;
				throw new RetryException(tex);
			} catch (NoSuchRecordException nex) {
				throw new NotFoundException(nex);
			} catch (ServiceUnavailableException ex) {
				throw new NetworkException(ex.getMessage(), ex);
			} catch (DatabaseException dex) {
				throw ReactiveSessionTransaction.translateDatabaseException(dex);
			} catch (ClientException cex) {
				throw ReactiveSessionTransaction.translateClientException(cex);
			}
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

			final String statement            = query.getStatement();
			final Map<String, Object> map     = query.getParameters();
			final RxResult result             = tx.run(statement, map);
			final Publisher<Record> publisher = result.records();
			final Record record               = Mono.from(publisher).block();
			final boolean value               = record.get(0).asBoolean();

			logSummary(Mono.from(result.consume()).block());

			return value;

		} catch (TransientException tex) {
			closed = true;
			throw new RetryException(tex);
		} catch (NoSuchRecordException nex) {
			throw new NotFoundException(nex);
		} catch (ServiceUnavailableException ex) {
			throw new NetworkException(ex.getMessage(), ex);
		} catch (DatabaseException dex) {
			throw ReactiveSessionTransaction.translateDatabaseException(dex);
		} catch (ClientException cex) {
			throw ReactiveSessionTransaction.translateClientException(cex);
		}
	}

	@Override
	protected Long getLong(final CypherQuery query) {

		try {

			logQuery(query);

			final String statement            = query.getStatement();
			final Map<String, Object> map     = query.getParameters();
			final RxResult result             = tx.run(statement, map);
			final Publisher<Record> publisher = result.records();
			final Record record               = Mono.from(publisher).block();
			Long value                        = null;

			if (record != null) {

				value = record.get(0).asLong();
			}

			logSummary(Mono.from(result.consume()).block());

			return value;

		} catch (TransientException tex) {
			closed = true;
			throw new RetryException(tex);
		} catch (NoSuchRecordException nex) {
			throw new NotFoundException(nex);
		} catch (ServiceUnavailableException ex) {
			throw new NetworkException(ex.getMessage(), ex);
		} catch (DatabaseException dex) {
			throw ReactiveSessionTransaction.translateDatabaseException(dex);
		} catch (ClientException cex) {
			throw ReactiveSessionTransaction.translateClientException(cex);
		}
	}

	@Override
	protected Node getNode(final CypherQuery query) {

		try {

			logQuery(query);

			final String statement            = query.getStatement();
			final Map<String, Object> map     = query.getParameters();
			final RxResult result             = tx.run(statement, map);
			final Publisher<Record> publisher = result.records();
			final Record record               = Mono.from(publisher).block();
			Node value                        = null;

			if (record != null) {

				value = record.get(0).asNode();
			}

			logSummary(Mono.from(result.consume()).block());

			return value;

		} catch (TransientException tex) {
			closed = true;
			throw new RetryException(tex);
		} catch (NoSuchRecordException nex) {
			throw new NotFoundException(nex);
		} catch (ServiceUnavailableException ex) {
			throw new NetworkException(ex.getMessage(), ex);
		} catch (DatabaseException dex) {
			throw ReactiveSessionTransaction.translateDatabaseException(dex);
		} catch (ClientException cex) {
			throw ReactiveSessionTransaction.translateClientException(cex);
		}
	}

	@Override
	protected Relationship getRelationship(final CypherQuery query) {

		try {

			logQuery(query);

			final String statement            = query.getStatement();
			final Map<String, Object> map     = query.getParameters();
			final RxResult result             = tx.run(statement, map);
			final Publisher<Record> publisher = result.records();
			final Record record               = Mono.from(publisher).block();
			Relationship value                = null;

			if (record != null) {

				value = record.get(0).asRelationship();
			}

			logSummary(Mono.from(result.consume()).block());

			return value;

		} catch (TransientException tex) {
			closed = true;
			throw new RetryException(tex);
		} catch (NoSuchRecordException nex) {
			throw new NotFoundException(nex);
		} catch (ServiceUnavailableException ex) {
			throw new NetworkException(ex.getMessage(), ex);
		} catch (DatabaseException dex) {
			throw ReactiveSessionTransaction.translateDatabaseException(dex);
		} catch (ClientException cex) {
			throw ReactiveSessionTransaction.translateClientException(cex);
		}
	}

	@Override
	protected Iterable<Record> collectRecords(final CypherQuery query, final IterableQueueingRecordConsumer unused) {

		try {

			logQuery(query);

			final String statement        = query.getStatement();
			final Map<String, Object> map = query.getParameters();

			return Flux.from(tx.run(statement, map).records()).toIterable();

		} catch (TransientException tex) {
			closed = true;
			throw new RetryException(tex);
		} catch (NoSuchRecordException nex) {
			throw new NotFoundException(nex);
		} catch (ServiceUnavailableException ex) {
			throw new NetworkException(ex.getMessage(), ex);
		} catch (DatabaseException dex) {
			throw ReactiveSessionTransaction.translateDatabaseException(dex);
		} catch (ClientException cex) {
			throw ReactiveSessionTransaction.translateClientException(cex);
		}
	}

	@Override
	protected Iterable<Map<String, Object>> run(final CypherQuery query) {

		try {

			logQuery(query);

			final String statement        = query.getStatement();
			final Map<String, Object> map = query.getParameters();

                        return Iterables.toList(Iterables.map(new RecordMapMapper(db), Flux.from(tx.run(statement, map).records()).toIterable()));

		} catch (TransientException tex) {
			closed = true;
			throw new RetryException(tex);
		} catch (NoSuchRecordException nex) {
			throw new NotFoundException(nex);
		} catch (ServiceUnavailableException ex) {
			throw new NetworkException(ex.getMessage(), ex);
		} catch (DatabaseException dex) {
			throw ReactiveSessionTransaction.translateDatabaseException(dex);
		} catch (ClientException cex) {
			throw ReactiveSessionTransaction.translateClientException(cex);
		}
	}

	@Override
	protected void set(final String statement, final Map<String, Object> map) {

		try {

			logQuery(statement, map);

			final RxResult result = tx.run(statement, map);

			logSummary(Mono.from(result.consume()).block());

		} catch (TransientException tex) {
			closed = true;
			throw new RetryException(tex);
		} catch (NoSuchRecordException nex) {
			throw new NotFoundException(nex);
		} catch (ServiceUnavailableException ex) {
			throw new NetworkException(ex.getMessage(), ex);
		} catch (DatabaseException dex) {
			throw ReactiveSessionTransaction.translateDatabaseException(dex);
		} catch (ClientException cex) {
			throw ReactiveSessionTransaction.translateClientException(cex);
		}
	}

	@Override
	public Iterable<Record> newIterable(final BoltDatabaseService db, final CypherQuery query) {
		return new QueryIterable(db, query);
	}
}
