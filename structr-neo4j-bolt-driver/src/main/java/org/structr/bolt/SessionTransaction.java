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

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.neo4j.driver.Record;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.exceptions.DatabaseException;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.summary.SummaryCounters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.*;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang.StringUtils;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Path.Segment;
import org.neo4j.driver.types.Relationship;
import org.structr.api.graph.Identity;
import org.structr.api.util.Iterables;

/**
 *
 */
abstract class SessionTransaction implements org.structr.api.Transaction {

	private static final Logger logger                  = LoggerFactory.getLogger(SessionTransaction.class);
	protected static final AtomicLong ID_SOURCE         = new AtomicLong();
	protected final Map<Long, RelationshipWrapper> rels = new LinkedHashMap<>();
	protected final Map<Long, NodeWrapper> nodes        = new LinkedHashMap<>();
	protected final Set<Long> deletedNodes              = new LinkedHashSet<>();
	protected final Set<Long> deletedRels               = new LinkedHashSet<>();
	protected final Set<String> prefetched              = new LinkedHashSet<>();
	protected final Object transactionKey               = new Object();
	protected BoltDatabaseService db                    = null;
	protected long transactionId                        = 0L;
	protected boolean success                           = false;
	protected boolean isPing                            = false;

	public SessionTransaction(final BoltDatabaseService db) {

		this.transactionId = ID_SOURCE.getAndIncrement();
		this.db            = db;
	}

	public abstract boolean isClosed();
	protected abstract Boolean getBoolean(final String statement);
	protected abstract Boolean getBoolean(final String statement, final Map<String, Object> map);
	protected abstract Long getLong(final String statement);
	protected abstract Long getLong(final String statement, final Map<String, Object> map);
	protected abstract Node getNode(final String statement, final Map<String, Object> map);
	protected abstract Relationship getRelationship(final String statement, final Map<String, Object> map);
	protected abstract Iterable<Record> collectRecords(final String statement, final Map<String, Object> map, final IterableQueueingRecordConsumer consumer);
	protected abstract Iterable<String> getStrings(final String statement, final Map<String, Object> map);
	protected abstract Iterable<Map<String, Object>> run(final String statement, final Map<String, Object> map);
	protected abstract void set(final String statement, final Map<String, Object> map);

	public abstract Iterable<Record> newIterable(final BoltDatabaseService db, final AdvancedCypherQuery query);

	public void delete(final NodeWrapper wrapper) {
		deletedNodes.add(wrapper.getId().getId());
	}

	public void delete(final RelationshipWrapper wrapper) {
		deletedRels.add(wrapper.getId().getId());
	}

	public void setIsPing(final boolean isPing) {
		this.isPing = isPing;
	}

	@Override
	public long getTransactionId() {
		return this.transactionId;
	}

	public Object getTransactionKey() {
		// we need a simple object that can be used in a weak hash map
		return transactionKey;
	}

	public NodeWrapper getNodeWrapper(final org.neo4j.driver.types.Node node) {

		final long id           = node.id();
		NodeWrapper nodeWrapper = nodes.get(id);

		if (nodeWrapper != null) {

			nodeWrapper.updateEntity(node);

		} else {

			nodeWrapper = new NodeWrapper(db, node);
			nodes.put(id, nodeWrapper);
		}

		return nodeWrapper;
	}

	public RelationshipWrapper getRelationshipWrapper(final org.neo4j.driver.types.Relationship relationship) {

		final long id                           = relationship.id();
		RelationshipWrapper relationshipWrapper = rels.get(id);

		if (relationshipWrapper != null) {

			relationshipWrapper.updateEntity(relationship);

		} else {

			relationshipWrapper = new RelationshipWrapper(db, relationship);
			rels.put(id, relationshipWrapper);
		}

		return relationshipWrapper;
	}

	public NodeWrapper getNodeWrapper(final long id) {

		NodeWrapper node = nodes.get(id);
		if (node != null) {

			return node;
		}

		final String rawTenantIdentifier = db.getTenantIdentifier();
		final String tenantIdentifier = StringUtils.isNotBlank(rawTenantIdentifier) ? ":" + rawTenantIdentifier : "";

		final Node entity = getNode("MATCH (n" + tenantIdentifier + ") WHERE ID(n) = $id RETURN n", Map.of("id", id));
		if (entity != null) {

			node = new NodeWrapper(db, entity);

			nodes.put(id, node);

			return node;
		}

		throw new NotFoundException("Node with ID " + id + " not found.");
	}

	/*
	public NodeWrapper getNodeWrapper(final long id) {

		NodeWrapper node = nodes.get(id);
		if (node != null) {

			return node;
		}

		final String rawTenantIdentifier             = db.getTenantIdentifier();
		final String tenantIdentifier                = StringUtils.isNotBlank(rawTenantIdentifier) ? ":" + rawTenantIdentifier : "";
		final Iterable<Record> records               = collectRecords("MATCH (n" + tenantIdentifier + ") WHERE ID(n) = $id WITH n OPTIONAL MATCH (n)-[r]-(m" + tenantIdentifier + ") RETURN DISTINCT n, collect(distinct r) AS rels, collect(distinct m) AS nodes", Map.of("id", id), null);
		final List<org.structr.api.graph.Node> nodes = Iterables.toList(Iterables.map(new PrefetchNodeMapper(db), records));

		if (!nodes.isEmpty()) {

			return (NodeWrapper)nodes.get(0);
		}

		throw new NotFoundException("Node with ID " + id + " not found.");
	}
	*/

	public RelationshipWrapper getRelationshipWrapper(final long id) {

		RelationshipWrapper rel = rels.get(id);
		if (rel != null) {

			return rel;
		}

		final SessionTransaction tx   = db.getCurrentTransaction();
		final StringBuilder buf       = new StringBuilder();
		final String tenantIdentifier = db.getTenantIdentifier();

		buf.append("MATCH (");

		if (tenantIdentifier != null) {
			buf.append(":");
			buf.append(tenantIdentifier);
		}

		buf.append(")-[n]-(");

		if (tenantIdentifier != null) {
			buf.append(":");
			buf.append(tenantIdentifier);
		}

		buf.append(") WHERE ID(n) = $id RETURN n");

		final Relationship entity = tx.getRelationship(buf.toString(), Map.of("id", id));
		if (entity != null) {

			rel = new RelationshipWrapper(db, entity);

			rels.put(id, rel);

			return rel;
		}

		throw new NotFoundException("Relationship with ID " + id + " not found.");
	}

	@Override
	public org.structr.api.graph.Node getNode(final Identity id) {
		return getNodeWrapper(id.getId());
	}

	@Override
	public org.structr.api.graph.Relationship getRelationship(Identity id) {
		return getRelationshipWrapper(id.getId());
	}

	@Override
	public boolean isNodeDeleted(final long id) {
		return deletedNodes.contains(id);
	}

	@Override
	public boolean isRelationshipDeleted(final long id) {
		return deletedRels.contains(id);
	}

	@Override
	public void prefetch(final String type1, final String type2, final Set<String> keys) {

		final StringBuilder buf  = new StringBuilder();
		final String tenantId    = db.getTenantIdentifier();

		buf.append("(n");

		if (!StringUtils.isBlank(tenantId)) {

			buf.append(":");
			buf.append(tenantId);
		}

		if (!StringUtils.isBlank(type1)) {

			buf.append(":");
			buf.append(type1);
		}

		buf.append(")-[r]-(m");

		if (!StringUtils.isBlank(tenantId)) {

			buf.append(":");
			buf.append(tenantId);
		}

		if (!StringUtils.isBlank(type2)) {

			buf.append(":");
			buf.append(type2);
		}

		buf.append(")");

		prefetch(buf.toString(), keys);
	}

	@Override
	public void prefetch(final String query, final Set<String> outgoingKeys, final Set<String> incomingKeys) {

		if (prefetched.containsAll(outgoingKeys) && prefetched.containsAll(incomingKeys) && prefetched.contains(query)) {

			return;
		}

		prefetched.addAll(outgoingKeys);
		prefetched.addAll(incomingKeys);
		prefetched.add(query);

		final long t0             = System.currentTimeMillis();
		final StringBuilder buf   = new StringBuilder();
		final Set<Long> relsSeen  = new HashSet<>();
		long count                = 0L;

		buf.append("MATCH p = ");
		buf.append(query);
		buf.append(" RETURN p");

		for (final org.neo4j.driver.Record r : collectRecords(buf.toString(), Map.of(), null)) {

			final Path p        = r.get("p").asPath();
			NodeWrapper current = null;

			for (final Segment s : p) {

				final org.neo4j.driver.types.Relationship relationship = s.relationship();
				final long relId                                       = relationship.id();
				final boolean alreadySeen                              = relsSeen.contains(relId);
				RelationshipWrapper rel                                = null;

				if (current == null) {

					current = getNodeWrapper(s.start());
					current.storePrefetchInfo(outgoingKeys);
					count++;
				}

				if (!alreadySeen) {

					relsSeen.add(relId);

					rel = getRelationshipWrapper(relationship);

					// store outgoing rel
					current.storeRelationship(rel, true);
					count++;
				}

				// advance
				current = getNodeWrapper(s.end());
				current.storePrefetchInfo(incomingKeys);

				if (!alreadySeen) {

					// store incoming rel as well
					current.storeRelationship(rel, true);
					count++;
				}
			}
		}

		if (db.logQueries()) {

			logger.info(transactionId + ": prefetched {} entities in {} ms", count, (System.currentTimeMillis() - t0));
		}
	}

	@Override
	public void prefetch(final String query, final Set<String> keys) {

		if (prefetched.containsAll(keys) && prefetched.contains(query)) {

			return;
		}

		prefetched.addAll(keys);
		prefetched.add(query);

		final long t0             = System.currentTimeMillis();
		final StringBuilder buf   = new StringBuilder();
		final Set<Long> relsSeen  = new HashSet<>();
		long count                = 0L;

		buf.append("MATCH p = ");
		buf.append(query);
		buf.append(" RETURN p");

		for (final org.neo4j.driver.Record r : collectRecords(buf.toString(), Map.of(), null)) {

			final Path p        = r.get("p").asPath();
			NodeWrapper current = null;

			for (final Segment s : p) {

				final org.neo4j.driver.types.Relationship relationship = s.relationship();
				final long relId                                       = relationship.id();
				final boolean alreadySeen                              = relsSeen.contains(relId);
				RelationshipWrapper rel                                = null;

				if (current == null) {

					current = getNodeWrapper(s.start());
					current.storePrefetchInfo(keys);
					count++;
				}

				if (!alreadySeen) {

					relsSeen.add(relId);

					rel = getRelationshipWrapper(relationship);

					// store outgoing rel
					current.storeRelationship(rel, true);
					count++;
				}

				// advance
				current = getNodeWrapper(s.end());
				current.storePrefetchInfo(keys);

				if (!alreadySeen) {

					// store incoming rel as well
					current.storeRelationship(rel, true);
					count++;
				}
			}
		}

		if (db.logQueries()) {

			logger.info(transactionId + ": prefetched {} entities in {} ms", count, (System.currentTimeMillis() - t0));
		}
	}

	public void prefetch2(final String query, final Set<String> outgoingKeys, final Set<String> incomingKeys, final String id) {

		if (prefetched.containsAll(outgoingKeys) && prefetched.containsAll(incomingKeys) && prefetched.contains(query + id)) {

			return;
		}

		prefetched.addAll(outgoingKeys);
		prefetched.addAll(incomingKeys);
		prefetched.add(query + id);

		final long t0             = System.currentTimeMillis();
		long count                = 0L;

		for (final org.neo4j.driver.Record r : collectRecords(query, Map.of("id", id), null)) {

			final List<org.neo4j.driver.types.Node> nodes        = (List)r.get("nodes").asList();
			final List<org.neo4j.driver.types.Relationship> rels = (List)r.get("rels").asList();

			for (final org.neo4j.driver.types.Node n : nodes) {
				getNodeWrapper(n);
				count++;
			}

			for (final org.neo4j.driver.types.Relationship rel : rels) {

				final NodeWrapper start              = getNodeWrapper(rel.startNodeId());
				final NodeWrapper end                = getNodeWrapper(rel.endNodeId());
				final RelationshipWrapper relWrapper = getRelationshipWrapper(rel);

				start.storeRelationship(relWrapper, true);
				start.storePrefetchInfo(outgoingKeys);

				end.storeRelationship(relWrapper, true);
				end.storePrefetchInfo(incomingKeys);

				count++;
			}
		}

		if (db.logQueries()) {

			logger.info(transactionId + ": prefetched {} entities in {} ms", count, (System.currentTimeMillis() - t0));
		}
	}

	// ----- public static methods -----
	public static RuntimeException translateClientException(final ClientException cex) {

		switch (cex.code()) {

			case "Neo.ClientError.Schema.ConstraintValidationFailed":
				throw new ConstraintViolationException(cex, cex.code(), cex.getMessage());
			case "Neo.ClientError.Statement.SyntaxError":
				throw new SyntaxErrorException(cex, cex.code(), cex.getMessage());
			case "N/A":
				if (cex.getCause() != null && cex.getCause() instanceof ClientException causeCex) {
					throw translateClientException(causeCex);
				}
				throw new UnknownClientException(cex, cex.code(), cex.getMessage());
			default:
				throw new UnknownClientException(cex, cex.code(), cex.getMessage());
		}
	}

	public static RuntimeException translateDatabaseException(final DatabaseException dex) {

		switch (dex.code()) {

			case "Neo.DatabaseError.General.UnknownError":
				throw new DataFormatException(dex, dex.code(), dex.getMessage());

			// add handlers / translated exceptions for DatabaseExceptions here..
		}

		// wrap exception if no other cause could be found
		throw new UnknownDatabaseException(dex, dex.code(), dex.getMessage());
	}

	// ----- protected methods -----
	protected void logQuery(final String statement) {
		logQuery(statement, null);
	}

	protected void logQuery(final String statement, final Map<String, Object> map) {

		if (db.logQueries()) {

			if (!isPing || db.logPingQueries()) {

				if (map != null && !map.isEmpty()) {

					if (statement.contains("extractedContent")) {
						logger.info("{}: {}\t\t SET on extractedContent - value suppressed", Thread.currentThread().getId(), statement);
					} else {
						logger.info("{}: {} - {}\t\t Parameters: {}", Thread.currentThread().getId(), transactionId + ": " + nodes.size() + "/" + rels.size(), statement, map.toString());
					}

				} else {

					logger.info("{}: {} - {}", Thread.currentThread().getId(), transactionId + ": " + nodes.size() + "/" + rels.size(), statement);
				}
			}
		}
	}

	protected void logSummary(final ResultSummary summary) {

		if (db.logQueries()) {

			final SummaryCounters counters = summary.counters();

			final int nodesDeleted = counters.nodesDeleted();
			final int nodesCreated = counters.nodesCreated();
			final int relsCreated  = counters.relationshipsCreated();
			final int relsDeleted  = counters.relationshipsDeleted();
			final int sum          = nodesDeleted + nodesCreated + relsCreated + relsDeleted;

			if (sum > 0) {

				final long availableAfter = summary.resultAvailableAfter(TimeUnit.MILLISECONDS);
				final long consumedAfter  = summary.resultConsumedAfter(TimeUnit.MILLISECONDS);

				logger.info("Query summary: {} / {} nodes created / deleted, {} / {} rels created / deleted, result available after {} ms, consumed after {} ms, notifications: {}, query: {}",
					nodesCreated, nodesDeleted, relsCreated, relsDeleted, availableAfter, consumedAfter, summary.notifications(), summary.query().text()
				);
			}
		}
	}

	protected void clearChangeset() {

		nodes.clear();
		rels.clear();
	}
}
