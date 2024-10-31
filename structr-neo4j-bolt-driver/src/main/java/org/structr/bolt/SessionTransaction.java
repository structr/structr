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

import org.apache.commons.lang.StringUtils;
import org.neo4j.driver.Record;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.exceptions.DatabaseException;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.summary.SummaryCounters;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Path.Segment;
import org.neo4j.driver.types.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.*;
import org.structr.api.config.Settings;
import org.structr.api.graph.Identity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
abstract class SessionTransaction implements org.structr.api.Transaction {

	private static final Logger logger                                  = LoggerFactory.getLogger(SessionTransaction.class);
	private static final Set<String> relationshipTypeBlacklist          = Set.of("SECURITY", "OWNS");
	protected static final AtomicLong ID_SOURCE                         = new AtomicLong();

	protected static final Map<String, Map<String, PrefetchInfo>> prefetchInfos = new ConcurrentHashMap<>();
	protected static final Map<String, Boolean> prefetchBlacklist               = new ConcurrentHashMap<>();

	protected final Map<String, PrefetchInfo> histogram = new LinkedHashMap<>();
	protected final Map<Long, RelationshipWrapper> rels = new LinkedHashMap<>();
	protected final Map<Long, NodeWrapper> nodes        = new LinkedHashMap<>();
	protected final Set<Long> deletedNodes              = new LinkedHashSet<>();
	protected final Set<Long> deletedRels               = new LinkedHashSet<>();
	protected final Set<String> prefetchedOutgoing      = new LinkedHashSet<>();
	protected final Set<String> prefetchedIncoming      = new LinkedHashSet<>();
	protected final Set<String> prefetchedQueries       = new LinkedHashSet<>();
	protected final Object transactionKey               = new Object();
	protected BoltDatabaseService db                    = null;
	protected String prefetchHint                       = null;
	protected long transactionId                        = 0L;
	protected boolean success                           = false;
	protected boolean isPing                            = false;

	public SessionTransaction(final BoltDatabaseService db) {

		this.transactionId = ID_SOURCE.getAndIncrement();
		this.db            = db;
	}

	public abstract boolean isClosed();
	protected abstract Boolean getBoolean(final CypherQuery query);
	protected abstract Long getLong(final CypherQuery query);
	protected abstract Node getNode(final CypherQuery query);
	protected abstract Relationship getRelationship(final CypherQuery query);
	protected abstract Iterable<Record> collectRecords(final CypherQuery query, final IterableQueueingRecordConsumer consumer);
	protected abstract Iterable<Map<String, Object>> run(final CypherQuery query);

	protected abstract void set(final String statement, final Map<String, Object> map);

	public abstract Iterable<Record> newIterable(final BoltDatabaseService db, final CypherQuery query);

	public void delete(final NodeWrapper wrapper) {
		deletedNodes.add(wrapper.getId().getId());
	}

	public void delete(final RelationshipWrapper wrapper) {
		deletedRels.add(wrapper.getId().getId());
	}

	public void setIsPing(final boolean isPing) {
		this.isPing = isPing;
	}

	public int level() {
		return 0;
	}

	@Override
	public long getTransactionId() {
		return this.transactionId;
	}

	public static void flushCaches() {
		prefetchInfos.clear();
		prefetchBlacklist.clear();
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

		final Node entity = getNode(new SimpleCypherQuery("MATCH (n" + tenantIdentifier + ") WHERE ID(n) = $id RETURN n", Map.of("id", id)));
		if (entity != null) {

			node = new NodeWrapper(db, entity);

			nodes.put(id, node);

			return node;
		}

		throw new NotFoundException("Node with ID " + id + " not found.");
	}

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

		final Relationship entity = tx.getRelationship(new SimpleCypherQuery(buf, Map.of("id", id)));
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
	public void close() {

		if (db.logQueries()) {

			if (!histogram.isEmpty()) {

				logger.info("Query count histogram for transaction {}:", getTransactionId());

				for (final String key : histogram.keySet()) {

					logger.info("{}", histogram.get(key));

				}
			}
		}
	}

	@Override
	public void prefetchHint(final String hint) {

		if (this.prefetchHint == null) {
			this.prefetchHint = hint;
		}

		if (hint != null && hint.equals(this.prefetchHint)) {

			final Map<String, PrefetchInfo> infos = prefetchInfos.get(prefetchHint);
			if (infos != null) {

				for (final PrefetchInfo prefetch : infos.values()) {

					final Set<String> outgoingSet = Set.of(prefetch.getType() + "/all/OUTGOING/" + prefetch.relType);
					final Set<String> incomingSet = Set.of(prefetch.getType() + "/all/INCOMING/" + prefetch.relType);

					final long t0        = System.currentTimeMillis();
					final String pattern = prefetch.getPattern();

					if (!prefetchBlacklist.containsKey(pattern)) {

						prefetch(pattern, outgoingSet, incomingSet);

						final long t = System.currentTimeMillis() - t0;

						if (t > Settings.PrefetchingMaxDuration.getValue(500)) {

							if (db.logQueries()) {

								// blacklist prefetching calls that take too long
								logger.info("Blacklisting prefetching pattern {} because it takes {} ms, {} is {}", pattern, t, Settings.PrefetchingMaxDuration.getKey(), Settings.PrefetchingMaxDuration.getValue(500));
							}

							prefetchBlacklist.put(pattern, true);
						}
					}
				}
			}
		}
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

		if (prefetchedOutgoing.containsAll(outgoingKeys) && prefetchedIncoming.containsAll(incomingKeys) && prefetchedQueries.contains(query)) {

			return;
		}

		prefetchedOutgoing.addAll(outgoingKeys);
		prefetchedIncoming.addAll(incomingKeys);
		prefetchedQueries.add(query);

		final long t0             = System.currentTimeMillis();
		final StringBuilder buf   = new StringBuilder();
		final Set<Long> relsSeen  = new HashSet<>();
		long count                = 0L;

		buf.append("MATCH p = ");
		buf.append(query);
		buf.append(" RETURN p");

		for (final org.neo4j.driver.Record r : collectRecords(new SimpleCypherQuery(buf), null)) {

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

			logger.info(transactionId + ": prefetched {} entities in {} ms with {}", count, (System.currentTimeMillis() - t0), buf);
		}
	}

	@Override
	public void prefetch(final String query, final Set<String> keys) {

		if (prefetchedOutgoing.containsAll(keys) && prefetchedIncoming.containsAll(keys) && prefetchedQueries.contains(query)) {

			return;
		}

		prefetchedOutgoing.addAll(keys);
		prefetchedIncoming.addAll(keys);
		prefetchedQueries.add(query);

		final long t0             = System.currentTimeMillis();
		final StringBuilder buf   = new StringBuilder();
		final Set<Long> relsSeen  = new HashSet<>();
		long count                = 0L;

		buf.append("MATCH p = ");
		buf.append(query);
		buf.append(" RETURN p");

		for (final org.neo4j.driver.Record r : collectRecords(new SimpleCypherQuery(buf), null)) {

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

			logger.info(transactionId + ": prefetched {} entities in {} ms with {}", count, (System.currentTimeMillis() - t0), buf);
		}
	}

	public void prefetch2(final String query, final Set<String> outgoingKeys, final Set<String> incomingKeys, final String id) {

		if (prefetchedOutgoing.containsAll(outgoingKeys) && prefetchedIncoming.containsAll(incomingKeys) && prefetchedQueries.contains(query + id)) {

			return;
		}

		prefetchedOutgoing.addAll(outgoingKeys);
		prefetchedIncoming.addAll(incomingKeys);
		prefetchedQueries.add(query + id);

		final long t0             = System.currentTimeMillis();
		long count                = 0L;

		for (final org.neo4j.driver.Record r : collectRecords(new SimpleCypherQuery(query, Map.of("id", id)), null)) {

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

			logger.info(transactionId + ": prefetched {} entities in {} ms with {}", count, (System.currentTimeMillis() - t0), query);
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
	protected void logQuery(final CypherQuery query) {

		if (prefetchHint != null && query.getType() != null && query.getRelationshipType() != null) {

			if (!relationshipTypeBlacklist.contains(query.getRelationshipType())) {

				final String statement = query.getStatement();

				final PrefetchInfo info = histogram.get(statement);
				if (info != null) {

					final int count     = info.incrementAndGetCount();
					final int threshold = Settings.PrefetchingThreshold.getValue(100);

					if (count > threshold && !prefetchBlacklist.containsKey(info.getPattern())) {

						final Map<String, PrefetchInfo> infos = prefetchInfos.computeIfAbsent(prefetchHint, k -> new LinkedHashMap<>());

						// store prefetching info and log info message
						if (infos.put(statement, info) == null) {

							final String pattern = info.getPattern();

							final long prefetchResultCount = getLong(new SimpleCypherQuery("MATCH p = " + pattern + " RETURN count(p)"));
							if (prefetchResultCount < Settings.PrefetchingMaxCount.getValue(100_000)) {

								if (db.logQueries()) {

									logger.info("Activating prefetching for {} because it runs more than {} times in a single transaction", pattern, threshold);
								}

							} else {

								if (db.logQueries()) {

									logger.info("NOT activating prefetching for {} because it returns more than {} results", pattern, prefetchResultCount);
								}

								prefetchBlacklist.put(pattern, true);
							}
						}
					}

				} else {

					final String pattern = getPattern(query);

					histogram.put(statement, new PrefetchInfo(pattern, query, prefetchHint));
				}
			}
		}

		logQuery(query.getStatement(), query.getParameters());
	}

	protected void logQuery(final String query, final Map<String, Object> map) {

		if (db.logQueries()) {

			if (!isPing || db.logPingQueries()) {

				if (map != null && !map.isEmpty()) {

					if (query.contains("extractedContent")) {
						logger.info("{}: {}\t\t SET on extractedContent - value suppressed", Thread.currentThread().getId(), query);
					} else {
						logger.info("{}: {} - {}\t\t Parameters: {}", Thread.currentThread().getId(), transactionId + ": " + nodes.size() + "/" + rels.size(), query, map.toString());
					}

				} else {

					logger.info("{}: {} - {}", Thread.currentThread().getId(), transactionId + ": " + nodes.size() + "/" + rels.size(), query);
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

	private String getPattern(final CypherQuery query) {

		final String type        = query.getType();
		final String relType     = query.getRelationshipType();
		final boolean isOutgoing = query.isOutgoing();

		if (type != null && relType != null) {

			String pattern = "-[r:" + relType + "]-";

			if (isOutgoing) {

				pattern = pattern + ">(m)";

			} else {

				pattern = "<" + pattern + "(m)";
			}

			pattern = "(n:" + type + ")" + pattern;

			return pattern;
		}

		return null;
	}

	private class PrefetchInfo {

		private String type      = null;
		private String pattern   = null;
		private String relType   = null;
		private boolean outgoing = false;
		private String hash      = null;
		private int count        = 1;

		public PrefetchInfo(final String pattern, final CypherQuery query, final String hash) {

			this.pattern  = pattern;
			this.type     = query.getType();
			this.relType  = query.getRelationshipType();
			this.outgoing = query.isOutgoing();
			this.hash     = hash;
		}

		@Override
		public String toString() {
			return StringUtils.leftPad(Integer.toString(count), 8, " ") + ": " + pattern;
		}

		public int getCount() {
			return count;
		}

		public int incrementAndGetCount() {
			return ++count;
		}

		public String getHash() {
			return hash;
		}

		public String getType() {
			return type;
		}

		public String getPattern() {
			return pattern;
		}

		public String getRelType() {
			return relType;
		}

		public boolean isOutgoing() {
			return outgoing;
		}

		@Override
		public int hashCode() {
			return pattern.hashCode();
		}

		@Override
		public boolean equals(final Object other) {
			return ((PrefetchInfo)other).hashCode() == this.hashCode();
		}
	}
}