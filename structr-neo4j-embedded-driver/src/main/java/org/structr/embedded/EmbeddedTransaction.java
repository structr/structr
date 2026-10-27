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
package org.structr.embedded;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.RetryException;
import org.structr.api.SchemaChangedException;
import org.structr.api.Transaction;
import org.structr.api.graph.Identity;
import org.structr.api.util.CloseableIterator;
import org.structr.api.util.Iterables;
import org.structr.api.util.QueryHistogram;
import org.structr.api.util.QueryTimer;
import org.structr.api.util.TimedIterable;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
public class EmbeddedTransaction implements Transaction<String> {

	private static final Logger logger                         = LoggerFactory.getLogger(EmbeddedTransaction.class);
	protected static final AtomicLong ID_SOURCE                = new AtomicLong();

	protected final Map<String, RelationshipWrapper> rels = new HashMap<>();
	protected final Map<String, NodeWrapper> nodes        = new HashMap<>();
	protected final List<QueryTimer> openTimers            = new ArrayList<>();
	protected final Set<String> deletedNodes              = new HashSet<>();
	protected final Set<String> createdNodes              = new HashSet<>();
	protected final Set<String> deletedRels               = new HashSet<>();
	protected final Set<String> prefetchedOutgoing        = new HashSet<>();
	protected final Set<String> prefetchedIncoming        = new HashSet<>();
	protected final Set<String> prefetchedQueries         = new HashSet<>();

	protected final EmbeddedDatabaseService db;
	protected final long                    transactionId;

	protected String prefetchHint                       = null;
	protected boolean success                           = false;
	protected boolean isPing                            = false;
	protected boolean                       isRolledBack = false;
	protected org.neo4j.graphdb.Transaction tx           = null;
	private   boolean                       closed       = false;
	private   boolean                       forcedFailure = false;
	private   RetryException                terminated    = null;

	public EmbeddedTransaction(final EmbeddedDatabaseService db, final org.neo4j.graphdb.Transaction tx) {

		this.transactionId = ID_SOURCE.getAndIncrement();
		this.db            = db;
		this.tx            = tx;
	}

	@Override
	public void close() {

		recordOpenTimers();
		clearChangeset();

		closed = true;

		try {

			if (terminated != null) {

				rollbackTerminated();

			} else if (forcedFailure || !success) {

				tx.rollback();

			} else {

				tx.commit();
			}

		} catch (TransientFailureException tex) {

			// the database terminated the transaction, e.g. to resolve a deadlock
			throw terminated(tex);

		} finally {

			tx.close();
		}

		if (terminated != null) {

			// the code that ran in this transaction caught the RetryException, so it is thrown again here
			final String message = "The database terminated the transaction before it was closed";

			if (terminated instanceof SchemaChangedException) {

				throw new SchemaChangedException(message, terminated.getCause());
			}

			throw new RetryException(message, terminated.getCause());
		}
	}

	/**
	 * Records that the database terminated this transaction, so that it cannot commit even if the code that runs in it
	 * catches the RetryException, and returns the exception to throw.
	 */
	public RetryException terminated(final TransientFailureException tex) {

		return terminated(new RetryException(tex));
	}

	private RetryException terminated(final RetryException rex) {

		if (terminated == null) {

			terminated = rex;
		}

		return rex;
	}

	public boolean isClosed() {

		return closed;
	}

	public Node createNode(final Set<String> labels) {

		final List<Label> labelObjects = new LinkedList<>();

		if (db.getTenantIdentifier() != null) {

			labelObjects.add(Label.label(db.getTenantIdentifier()));
		}

		for (final String label : labels) {

			labelObjects.add(Label.label(label));
		}

		try {

			return tx.createNode(labelObjects.toArray(new Label[0]));

		} catch (TransientFailureException tex) {

			throw terminated(tex);
		}
	}

	public void acquireWriteLock(final Entity entity) {

		try {

			tx.acquireWriteLock(entity);

		} catch (TransientFailureException tex) {

			throw terminated(tex);
		}
	}

	public void delete(final NodeWrapper wrapper) {

		deletedNodes.add(wrapper.getId().getId());

		prefetchedOutgoing.clear();
		prefetchedIncoming.clear();
		prefetchedQueries.clear();
	}

	public void delete(final RelationshipWrapper wrapper) {

		deletedRels.add(wrapper.getId().getId());

		prefetchedOutgoing.clear();
		prefetchedIncoming.clear();
		prefetchedQueries.clear();
	}

	public Boolean getBoolean(final CypherQuery query) {

		logQuery(query);

		final QueryTimer timer = startTimer(query.getStatement());

		try {

			final String statement        = query.getStatement();
			final Map<String, Object> map    = query.getParameters();
			final Result              result = execute(statement, map);
			final Boolean             value  = getFirstBoolean(result);

			logSummary(result.getQueryStatistics());

			return value;

		} finally {

			timer.finished();
		}
	}

	public Long getLong(final CypherQuery query) {

		logQuery(query);

		final QueryTimer timer = startTimer(query.getStatement());

		try {

			final String statement            = query.getStatement();
			final Map<String, Object> map     = query.getParameters();
			final Result result               = execute(statement, map);
			final Long value                  = getFirstLong(result);

			logSummary(result.getQueryStatistics());

			return value;

		} finally {

			timer.finished();
		}
	}

	public Node getNode(final CypherQuery query) {

		logQuery(query);

		final QueryTimer timer = startTimer(query.getStatement());

		try {

			final String statement            = query.getStatement();
			final Map<String, Object> map     = query.getParameters();
			final Result result               = execute(statement, map);
			final Node value                  = getFirstNode(result);

			logSummary(result.getQueryStatistics());

			return value;

		} finally {

			timer.finished();
		}
	}

	public Relationship getRelationship(final CypherQuery query) {

		logQuery(query);

		final QueryTimer timer = startTimer(query.getStatement());

		try {

			final String statement        = query.getStatement();
			final Map<String, Object> map = query.getParameters();
			final Result result           = execute(statement, map);
			final Relationship value      = getFirstRelationship(result);

			logSummary(result.getQueryStatistics());

			return value;

		} finally {

			timer.finished();
		}
	}

	public Iterable<Map<String, Object>> collectRecords(final CypherQuery query, final IterableQueueingRecordConsumer unused) {

		logQuery(query);

		final String statement        = query.getStatement();
		final Map<String, Object> map = query.getParameters();

		return timed(startTimer(statement), toIterable(execute(statement, map)));
	}

	public Iterable<Map<String, Object>> run(final CypherQuery query) {

		logQuery(query);

		final String statement        = query.getStatement();
		final Map<String, Object> map = query.getParameters();
		final QueryTimer timer         = startTimer(statement);
		final Result result            = execute(statement, map);

		// the caller reads the rows in its own transaction, possibly after this one has closed
		if (db.getCurrentTransaction(false) != this) {

			return new TimedIterable<>(timer, new DetachedResult(db, result));
		}

		return timed(timer, Iterables.map(new RecordMapMapper(db), toIterable(result)));
	}

	public void set(final String statement, final Map<String, Object> map) {

		logQuery(statement, map);

		final QueryTimer timer = startTimer(statement);

		try {

			final Result result = execute(statement, map);

			logSummary(result.getQueryStatistics());

		} finally {

			timer.finished();
		}
	}

	/**
	 * Executes a Cypher statement in this transaction. Neo4j terminates a transaction that would wait for a lock in a
	 * deadlock, and only a repetition of the whole transaction can succeed then.
	 */
	private Result execute(final String statement, final Map<String, Object> map) {

		try {

			return tx.execute(statement, map);

		} catch (TransientFailureException tex) {

			throw terminated(tex);

		} catch (RuntimeException rex) {

			if (isFrequentSchemaChange(rex)) {

				throw terminated(new SchemaChangedException(rex));
			}

			throw rex;
		}
	}

	// Cypher gives up compiling a query while freshly created indexes come online, and only a repetition can succeed
	static boolean isFrequentSchemaChange(final Throwable t) {

		for (Throwable cause = t; cause != null; cause = cause.getCause()) {

			final String message = cause.getMessage();
			if (message != null && message.contains("insanely frequent schema changes")) {

				return true;
			}
		}

		return false;
	}

	private void rollbackTerminated() {

		try {

			tx.rollback();

		} catch (RuntimeException ignore) {

			// the database has already ended the transaction
		}
	}

	// ----- query timing -----
	private QueryTimer startTimer(final String statement) {

		final QueryTimer timer = QueryHistogram.newTimer();

		timer.started(statement);

		return timer;
	}

	private <T> Iterable<T> timed(final QueryTimer timer, final Iterable<T> records) {

		openTimers.add(timer);

		return new TimedIterable<>(timer, records);
	}

	/**
	 * Records the queries whose results were not read to the end, at the time of their last record.
	 */
	private void recordOpenTimers() {

		for (final QueryTimer timer : openTimers) {

			timer.record();
		}

		openTimers.clear();
	}

	public Iterable<Map<String, Object>> newIterable(final EmbeddedDatabaseService db, final CypherQuery query) {

		return new QueryIterable(db, query);
	}

	public void setIsPing(final boolean isPing) {

		this.isPing = isPing;
	}

	@Override
	public long getTransactionId() {

		return this.transactionId;
	}

	@Override
	public boolean isRolledBack() {

		return isRolledBack;
	}

	public static void flushCaches() {
	}

	public NodeWrapper getNodeWrapper(final Node node) {

		final String id         = node.getElementId();
		NodeWrapper nodeWrapper = nodes.get(id);

		if (nodeWrapper == null) {

			nodeWrapper = new NodeWrapper(db, node);
			nodes.put(id, nodeWrapper);
		}

		return nodeWrapper;
	}

	public RelationshipWrapper getRelationshipWrapper(final Relationship relationship) {

		final String id = relationship.getElementId();
		RelationshipWrapper wrapper = rels.get(id);

		if (wrapper == null) {

			wrapper = new RelationshipWrapper(db, relationship);
			rels.put(id, wrapper);
		}

		return wrapper;
	}

	public NodeWrapper getNodeWrapper(final String id) {

		if (nodes.containsKey(id)) {

			return nodes.get(id);
		}

		try {

			final org.neo4j.graphdb.Node node = tx.getNodeByElementId(id);
			if (node != null) {

				final NodeWrapper nodeWrapper = new NodeWrapper(db, node);
				nodes.put(id, nodeWrapper);

				return nodeWrapper;
			}

		} catch (org.neo4j.graphdb.NotFoundException nfe) {

			throw new org.structr.api.NotFoundException("Node with ID " + id + " not found.");
		}

		return null;
	}

	boolean nodeExists(final String id) {

		try {

			return tx.getNodeByElementId(id) != null;

		} catch (org.neo4j.graphdb.NotFoundException nfe) {

			return false;
		}
	}

	boolean relationshipExists(final String id) {

		try {

			return tx.getRelationshipByElementId(id) != null;

		} catch (org.neo4j.graphdb.NotFoundException nfe) {

			return false;
		}
	}

	public RelationshipWrapper getRelationshipWrapper(final String id) {

		if (rels.containsKey(id)) {

			return rels.get(id);
		}

		try {

			final org.neo4j.graphdb.Relationship relationship = tx.getRelationshipByElementId(id);
			if (relationship != null) {

				final RelationshipWrapper relationshipWrapper = new RelationshipWrapper(db, relationship);
				rels.put(id, relationshipWrapper);

				return relationshipWrapper;
			}

		} catch (org.neo4j.graphdb.NotFoundException t) {

			throw new org.structr.api.NotFoundException("Relationship with ID " + id + " not found.");
		}

		return null;
	}

	@Override
	public org.structr.api.graph.Node getNode(final Identity<String> id) {

		return getNodeWrapper(id.getId());
	}

	@Override
	public org.structr.api.graph.Relationship getRelationship(Identity<String> id) {

		return getRelationshipWrapper(id.getId());
	}

	@Override
	public void setNodeIsCreated(final String id) {

		createdNodes.add(id);
	}

	@Override
	public boolean isNodeCreated(final String id) {

		return createdNodes.contains(id);
	}

	@Override
	public boolean isNodeDeleted(final String id) {

		return deletedNodes.contains(id);
	}

	@Override
	public boolean isRelationshipDeleted(final String id) {

		return deletedRels.contains(id);
	}

	@Override
	public void failure() {

		forcedFailure = true;
	}

	@Override
	public void success() {

		// transaction must be marked successful explicitly
		success = true;
	}

	@Override
	public boolean isSuccessful() {

		return success && !forcedFailure && terminated == null;
	}

	@Override
	public void prefetchHint(final String hint) {
	}

	@Override
	public void prefetch(final String type1, final String type2, final Set<String> keys) {
	}

	@Override
	public void prefetch(final String query, final Set<String> outgoingKeys, final Set<String> incomingKeys) {
	}

	@Override
	public void prefetch(final String query, final Set<String> keys) {
	}

	public void prefetch2(final String query, final Set<String> outgoingKeys, final Set<String> incomingKeys, final String id) {
	}

	// ----- protected methods -----
	protected void logQuery(final CypherQuery query) {

		logQuery(query.getStatement(), query.getParameters());
	}

	protected void logQuery(final String query, final Map<String, Object> map) {

		if (db.logQueries()) {

			if (!isPing || db.logPingQueries()) {

				if (map != null && !map.isEmpty()) {

					logger.info("{}: {} - {}\t\t Parameters: {}", Thread.currentThread().getId(), transactionId, query, stringify(map));

				} else {

					logger.info("{}: {} - {}", Thread.currentThread().getId(), transactionId, query);
				}
			}
		}
	}

	protected void logSummary(final QueryStatistics counters) {

		if (db.logQueries()) {

			final int nodesDeleted = counters.getNodesDeleted();
			final int nodesCreated = counters.getNodesCreated();
			final int relsCreated  = counters.getRelationshipsCreated();
			final int relsDeleted  = counters.getRelationshipsDeleted();
			final int sum          = nodesDeleted + nodesCreated + relsCreated + relsDeleted;

			if (sum > 0) {

				logger.info("Query summary: {} / {} nodes created / deleted, {} / {} rels created / deleted", nodesCreated, nodesDeleted, relsCreated, relsDeleted);
			}
		}
	}

	protected void clearChangeset() {
	}

	private String stringify(final Map map) {

		final Gson gson = new GsonBuilder().create();

		return gson.toJson(map);
	}

	// ----- private methods -----
	private Object getFirstObject(final Result result) {

		if (result != null && result.hasNext()) {

			final Map<String, Object> next = result.next();
			if (!next.keySet().isEmpty()) {

				final Object value = next.values().toArray()[0];

				return value;
			}
		}

		return null;
	}

	private Boolean getFirstBoolean(final Result result) {

		final Object value = getFirstObject(result);
		if (value != null) {

			if (value instanceof Boolean b) {

				return b;

			} else {

				throw new IllegalStateException("Expected Boolean, got " + value.getClass());
			}
		}

		return null;
	}

	private Long getFirstLong(final Result result) {

		final Object value = getFirstObject(result);
		if (value != null) {

			if (value instanceof Long l) {

				return l;

			} else {

				throw new IllegalStateException("Expected Long, got " + value.getClass());
			}
		}

		return null;
	}

	private Node getFirstNode(final Result result) {

		final Object value = getFirstObject(result);
		if (value != null) {

			if (value instanceof Node b) {

				return b;

			} else {

				throw new IllegalStateException("Expected Node, got " + value.getClass());
			}
		}

		return null;
	}

	private Relationship getFirstRelationship(final Result result) {

		final Object value = getFirstObject(result);
		if (value != null) {

			if (value instanceof Relationship r) {

				return r;

			} else {

				throw new IllegalStateException("Expected Relationship, got " + value.getClass());
			}
		}

		return null;
	}

	private Iterable<Map<String, Object>> toIterable(final Result iterator) {

		return () -> new CloseableIterator<>() {

			@Override
			public void close() throws Exception {

				iterator.close();
			}

			@Override
			public boolean hasNext() {

				return iterator.hasNext();
			}

			@Override
			public Map<String, Object> next() {

				return iterator.next();
			}
		};
	}
}