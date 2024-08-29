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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.TransactionConfig;
import org.neo4j.driver.Values;
import org.neo4j.driver.exceptions.*;
import org.neo4j.driver.internal.shaded.reactor.core.publisher.Flux;
import org.neo4j.driver.internal.shaded.reactor.core.publisher.Mono;
import org.neo4j.driver.reactive.RxResult;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.reactive.RxTransaction;
import org.neo4j.driver.types.Entity;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.NetworkException;
import org.structr.api.NotFoundException;
import org.structr.api.RetryException;
import org.structr.api.util.Iterables;

import java.util.*;

/**
 *
 */
class ReactiveSessionTransaction extends SessionTransaction {

	private static final Logger logger = LoggerFactory.getLogger(ReactiveSessionTransaction.class);

	private BoltDatabaseService db = null;
	private RxSession session      = null;
	private RxTransaction tx       = null;
	private boolean closed         = false;

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
	}

	@Override
	public void success() {

		// transaction must be marked successfull explicitly
		success = true;
	}

	@Override
	public void close() {

		if (!success) {

			try {

				Mono.from(tx.rollback()).block();

			} catch (Throwable ignore) {
			}

			for (final EntityWrapper entity : accessedEntities) {

				entity.rollback(transactionKey);
				entity.removeFromCache();
			}

			for (final EntityWrapper entity : modifiedEntities) {
				entity.stale();
			}

		} else {

			try {

				Mono.from(tx.commit()).block();

			} catch (Throwable ignore) {
			}

			RelationshipWrapper.expunge(deletedRels);
			NodeWrapper.expunge(deletedNodes);

			for (final EntityWrapper entity : accessedEntities) {
				entity.commit(transactionKey);
			}

			for (final EntityWrapper entity : modifiedEntities) {
				entity.clearCaches();
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

			// Notify all nodes that are modified in this transaction
			// so that the relationship caches are rebuilt.
			for (final EntityWrapper entity : modifiedEntities) {
				entity.onClose();
			}

			// make sure that the resources are freed
			session.close();
		}
	}

	public boolean isClosed() {
		return closed;
	}

	@Override
	public boolean getBoolean(final String statement) {

		try {

			logQuery(statement);
			return getBoolean(statement, Collections.EMPTY_MAP);

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
	public boolean getBoolean(final String statement, final Map<String, Object> map) {

		try {

			logQuery(statement, map);

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
	public long getLong(final String statement) {

		try {

			logQuery(statement);
			return getLong(statement, Collections.EMPTY_MAP);

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
	public long getLong(final String statement, final Map<String, Object> map) {

		try {

			logQuery(statement, map);

			final RxResult result             = tx.run(statement, map);
			final Publisher<Record> publisher = result.records();
			final Record record               = Mono.from(publisher).block();
			final long value                  = record.get(0).asLong();

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
	public Object getObject(final String statement, final Map<String, Object> map) {

		try {

			logQuery(statement, map);

			final RxResult result             = tx.run(statement, map);
			final Publisher<Record> publisher = result.records();
			final Record record               = Mono.from(publisher).block();
			Object value                      = null;

			if (record != null) {

				value = record.get(0).asObject();
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
	public Entity getEntity(final String statement, final Map<String, Object> map) {

		try {

			logQuery(statement, map);

			final RxResult result             = tx.run(statement, map);
			final Publisher<Record> publisher = result.records();
			final Record record               = Mono.from(publisher).block();
			Entity value                      = null;

			if (record != null) {

				value = record.get(0).asEntity();
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
	public Node getNode(final String statement, final Map<String, Object> map) {

		try {

			logQuery(statement, map);

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
	public Relationship getRelationship(final String statement, final Map<String, Object> map) {

		try {

			logQuery(statement, map);

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
	public Object collectRecords(final String statement, final Map<String, Object> map, final Object flux) {

		try {

			logQuery(statement, map);
			return Flux.from(tx.run(statement, map).records());

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
	public Iterable<String> getStrings(final String statement, final Map<String, Object> map) {

		try {

			logQuery(statement, map);

			final RxResult result             = tx.run(statement, map);
			final Publisher<Record> publisher = result.records();
			final Record record               = Mono.from(publisher).block();
			final List<String> immutable      = record.get(0).asList(Values.ofString());

			logSummary(Mono.from(result.consume()).block());

			return new LinkedList<>(immutable);

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
	public Iterable<Map<String, Object>> run(final String statement, final Map<String, Object> map) {

		try {

			logQuery(statement, map);

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
	public void set(final String statement, final Map<String, Object> map) {

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
	public Iterable<Record> newIterable(final BoltDatabaseService db, final AdvancedCypherQuery query) {
		return new QueryIterable(db, query);
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
				throw ReactiveSessionTransaction.translateClientException(dex);
			} catch (DatabaseException dex) {
				throw ReactiveSessionTransaction.translateDatabaseException(dex);
			}
		}

		@Override
		public T next() {

			try {

				return iterator.next();

			} catch (ClientException dex) {
				throw ReactiveSessionTransaction.translateClientException(dex);
			} catch (DatabaseException dex) {
				throw ReactiveSessionTransaction.translateDatabaseException(dex);
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
