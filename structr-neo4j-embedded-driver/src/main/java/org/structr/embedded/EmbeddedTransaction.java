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
import org.structr.api.Transaction;
import org.structr.api.graph.Identity;
import org.structr.api.util.CloseableIterator;
import org.structr.api.util.Iterables;

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

	public EmbeddedTransaction(final EmbeddedDatabaseService db, final org.neo4j.graphdb.Transaction tx) {

		this.transactionId = ID_SOURCE.getAndIncrement();
		this.db            = db;
		this.tx            = tx;
	}

	@Override
	public void close() {

		clearChangeset();

		closed = true;

		try {

			if (forcedFailure || !success) {

				tx.rollback();

			} else {

				tx.commit();
			}

		} finally {

			tx.close();
		}
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

		return tx.createNode(labelObjects.toArray(new Label[0]));
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

		final String statement        = query.getStatement();
		final Map<String, Object> map    = query.getParameters();
		final Result              result = tx.execute(statement, map);
		final Boolean             value  = getFirstBoolean(result);

		logSummary(result.getQueryStatistics());

		return value;
	}

	public Long getLong(final CypherQuery query) {

		logQuery(query);

		final String statement            = query.getStatement();
		final Map<String, Object> map     = query.getParameters();
		final Result result               = tx.execute(statement, map);
		final Long value                  = getFirstLong(result);

		logSummary(result.getQueryStatistics());

		return value;
	}

	public Node getNode(final CypherQuery query) {

		logQuery(query);

		final String statement            = query.getStatement();
		final Map<String, Object> map     = query.getParameters();
		final Result result               = tx.execute(statement, map);
		final Node value                  = getFirstNode(result);

		logSummary(result.getQueryStatistics());

		return value;
	}

	public Relationship getRelationship(final CypherQuery query) {

		logQuery(query);

		final String statement        = query.getStatement();
		final Map<String, Object> map = query.getParameters();
		final Result result           = tx.execute(statement, map);
		final Relationship value      = getFirstRelationship(result);

		logSummary(result.getQueryStatistics());

		return value;
	}

	public Iterable<Map<String, Object>> collectRecords(final CypherQuery query, final IterableQueueingRecordConsumer unused) {

		logQuery(query);

		final String statement        = query.getStatement();
		final Map<String, Object> map = query.getParameters();

		return toIterable(tx.execute(statement, map));
	}

	public Iterable<Map<String, Object>> run(final CypherQuery query) {

		logQuery(query);

		final String statement        = query.getStatement();
		final Map<String, Object> map = query.getParameters();
		final Result result            = tx.execute(statement, map);

		return Iterables.map(new RecordMapMapper(db), toIterable(result));
	}

	public void set(final String statement, final Map<String, Object> map) {

		logQuery(statement, map);

		final Result result = tx.execute(statement, map);

		logSummary(result.getQueryStatistics());
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

		return success && !forcedFailure;
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

					if (query.contains("extractedContent")) {

						logger.info("{}: {}\t\t SET on extractedContent - value suppressed", Thread.currentThread().getId(), query);

					} else {

						logger.info("{}: {} - {}\t\t Parameters: {}", Thread.currentThread().getId(), transactionId, query, stringify(map));
					}

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