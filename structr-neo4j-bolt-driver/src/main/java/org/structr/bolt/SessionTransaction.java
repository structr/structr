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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.collections4.SetUtils;
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
import org.structr.api.util.Iterables;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
abstract class SessionTransaction implements org.structr.api.Transaction {

	private static final Logger logger                         = LoggerFactory.getLogger(SessionTransaction.class);
	protected static final AtomicLong ID_SOURCE                = new AtomicLong();

	protected static final Map<String, Set<PrefetchInfo>> prefetchInfos = new ConcurrentHashMap<>();
	protected static final Map<String, Boolean> prefetchBlacklist       = new ConcurrentHashMap<>();

	protected final Map<String, PrefetchInfo> histogram = new ConcurrentHashMap<>();
	protected final Map<Long, RelationshipWrapper> rels = new HashMap<>();
	protected final Map<Long, NodeWrapper> nodes        = new HashMap<>();
	protected final Set<Long> deletedNodes              = new HashSet<>();
	protected final Set<Long> createdNodes              = new HashSet<>();
	protected final Set<Long> deletedRels               = new HashSet<>();
	protected final Set<String> prefetchedOutgoing      = new HashSet<>();
	protected final Set<String> prefetchedIncoming      = new HashSet<>();
	protected final Set<String> prefetchedQueries       = new HashSet<>();

	protected final Map<Integer, Set<Long>> queryResultCache = new HashMap<>();

	protected final BoltDatabaseService db;
	protected final long transactionId;

	protected String prefetchHint                       = null;
	protected boolean success                           = false;
	protected boolean isPing                            = false;
	protected boolean logPrefetching                    = false;

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

	public abstract Iterable<Record> newIterable(final BoltDatabaseService db, final CypherQuery query);

	protected void set(final String statement, final Map<String, Object> map) {
		queryResultCache.clear();
	}

	public void delete(final NodeWrapper wrapper) {

		deletedNodes.add(wrapper.getId().getId());

		prefetchedOutgoing.clear();
		prefetchedIncoming.clear();
		prefetchedQueries.clear();
		queryResultCache.clear();
	}

	public void delete(final RelationshipWrapper wrapper) {

		deletedRels.add(wrapper.getId().getId());

		prefetchedOutgoing.clear();
		prefetchedIncoming.clear();
		prefetchedQueries.clear();
		queryResultCache.clear();
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

		synchronized (prefetchInfos) {
			prefetchInfos.clear();
			prefetchBlacklist.clear();
		}
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
	public void setNodeIsCreated(final long id) {
		createdNodes.add(id);
	}

	@Override
	public boolean isNodeCreated(final long id) {
		return createdNodes.contains(id);
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

		if (logPrefetching || db.logQueries()) {

			if (!histogram.isEmpty()) {

				logger.info("{}: Query count histogram", getTransactionId());

				for (final String key : histogram.keySet()) {

					final PrefetchInfo info = histogram.get(key);

					logger.info("        {}: {}", info.getCount(), info);
				}
			}
		}

		optimizePrefetching();

		// this is internal, will only be displayed if logPrefetching is set to true at compile time..
		if (logPrefetching && prefetchHint != null) {

			final Set<PrefetchInfo> infos = prefetchInfos.get(prefetchHint);
			if (infos != null && !infos.isEmpty()) {

				System.out.println("############################################################################### " + prefetchHint);

				for (final PrefetchInfo info : infos) {

					System.out.println("        " + info.getPattern());

					for (final String rel : info.getOutgoingSet()) {

						System.out.println("                OUT: " + rel);
					}

					for (final String rel : info.getIncomingSet()) {

						System.out.println("                IN:  " + rel);
					}
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

			final Set<PrefetchInfo> infos = prefetchInfos.get(prefetchHint);
			if (infos != null) {

				for (final PrefetchInfo prefetch : infos) {

					final long t0 = System.currentTimeMillis();
					final String pattern = prefetch.getPattern();

					if (!prefetchBlacklist.containsKey(prefetchHint + ": " + pattern)) {

						prefetch(pattern, prefetch.getOutgoingSet(), prefetch.getIncomingSet());

						final long dt = System.currentTimeMillis() - t0;

						if (dt > Settings.PrefetchingMaxDuration.getValue(500)) {

							if (logPrefetching || db.logQueries()) {

								// blacklist prefetching calls that take too long
								logger.info("{}: Blacklisting prefetching pattern {} because it takes {} ms, {} is {}", transactionId, pattern, dt, Settings.PrefetchingMaxDuration.getKey(), Settings.PrefetchingMaxDuration.getValue(500));
							}

							prefetchBlacklist.put(prefetchHint + ": " + pattern, true);
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

			if (p.length() == 0) {

				// iterate individual nodes as well
				for (final Node n : p.nodes()) {

					getNodeWrapper(n);
					count++;
				}
			}

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

		if (logPrefetching || db.logQueries()) {

			logger.info(transactionId + ": prefetched {} entities in {} ms with {}: {} / {}", count, (System.currentTimeMillis() - t0), buf, nodes.size(), rels.size());
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

		if (logPrefetching || db.logQueries()) {

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

		final Map<String, Object> data = new ConcurrentHashMap<>();
		final long t0                  = System.currentTimeMillis();
		long count                     = 0L;

		if (id != null) {
			data.put("id", id);
		}

		for (final org.neo4j.driver.Record r : collectRecords(new SimpleCypherQuery(query, data), null)) {

			final List<org.neo4j.driver.types.Node> nodes        = (List)r.get("nodes").asList();
			final List<org.neo4j.driver.types.Relationship> rels = (List)r.get("rels").asList();

			for (final org.neo4j.driver.types.Node n : nodes) {

				final NodeWrapper nodeWrapper = getNodeWrapper(n);
				nodeWrapper.storePrefetchInfo(outgoingKeys);

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

		if (logPrefetching || db.logQueries()) {

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

			final String statement = query.getStatement();

			final PrefetchInfo info = histogram.get(statement);
			if (info != null) {

				final int count     = info.incrementAndGetCount();
				final int threshold = Settings.PrefetchingThreshold.getValue(100);

				if (count > threshold && !prefetchBlacklist.containsKey(prefetchHint + ": " + info.getPattern())) {

					final Set<PrefetchInfo> infos = prefetchInfos.computeIfAbsent(prefetchHint, k -> new LinkedHashSet<>());

					// store prefetching info and log info message
					if (infos.add(info)) {

						final String pattern = info.getPattern();

						final long prefetchResultCount = getLong(new SimpleCypherQuery("MATCH p = " + pattern + " RETURN count(p)"));
						if (prefetchResultCount < Settings.PrefetchingMaxCount.getValue(100_000)) {

							if (logPrefetching || db.logQueries()) {

								logger.info("{}: Activating prefetching for {} because it runs more than {} times in a single transaction", transactionId, pattern, threshold);
							}

						} else {

							if (logPrefetching || db.logQueries()) {

								logger.info("{}: NOT activating prefetching for {} because it returns more than {} results", transactionId, pattern, prefetchResultCount);
							}

							prefetchBlacklist.put(prefetchHint + ": " + pattern, true);
						}
					}
				}

			} else {

				final PrefetchInfo newInfo = new PrefetchInfo(query);
				histogram.put(statement, newInfo);
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
						logger.info("{}: {} - {}\t\t Parameters: {}", Thread.currentThread().getId(), transactionId + ": " + nodes.size() + "/" + rels.size(), query, stringify(map));
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

	private String createPatternFromDetails(final String type, final Set<String> relTypes, final boolean outgoing) {

		final String rawTenantIdentifier = db.getTenantIdentifier();
		final String tenantIdentifier    = StringUtils.isNotBlank(rawTenantIdentifier) ? ":" + rawTenantIdentifier : "";
		final StringBuilder pattern      = new StringBuilder();

		pattern.append("(n:");
		pattern.append(type);
		pattern.append(tenantIdentifier);
		pattern.append(")");

		if (!outgoing) {
			pattern.append("<");
		}

		pattern.append("-[r:");
		pattern.append(StringUtils.join(relTypes, "|"));
		pattern.append("]-");

		if (outgoing) {
			pattern.append(">");
		}

		pattern.append("(m)");

		return pattern.toString();

	}

	private String createPatternFromQuery(final CypherQuery query) {

		final String rawTenantIdentifier = db.getTenantIdentifier();
		final String tenantIdentifier    = StringUtils.isNotBlank(rawTenantIdentifier) ? ":" + rawTenantIdentifier : "";
		final String type                = query.getType();
		final String relType             = query.getRelationshipType();
		final boolean isOutgoing         = query.isOutgoing();
		final StringBuilder buf          = new StringBuilder();

		if (type != null && relType != null) {

			String pattern = "-[r:" + relType + "]-";

			if (isOutgoing) {

				pattern = pattern + ">(m)";

			} else {

				pattern = "<" + pattern + "(m)";
			}

			pattern = "(n:" + type + tenantIdentifier + ")" + pattern;

			return pattern;
		}

		return null;
	}

	private void optimizePrefetching() {

		combinePrefetchingQueriesWithIdenticalTypeAndDirection();
		combinePrefetchingQueriesWithInheritance();
	}

	private void combinePrefetchingQueriesWithIdenticalTypeAndDirection() {

		// check if we can combine multiple prefetching queries into one
		if (prefetchHint != null) {

			final Set<PrefetchInfo> infos = prefetchInfos.get(prefetchHint);
			if (infos != null) {

				final Map<String, Set<PrefetchInfo>> typesOutgoing = new ConcurrentHashMap<>();
				final Map<String, Set<PrefetchInfo>> typesIncoming = new ConcurrentHashMap<>();

				for (final PrefetchInfo info : infos) {

					final String type = info.getType();

					if (info.isOutgoing()) {

						typesOutgoing.computeIfAbsent(type, k -> new LinkedHashSet<>()).add(info);


					} else {

						typesIncoming.computeIfAbsent(type, k -> new LinkedHashSet<>()).add(info);
					}
				}

				for (final String type : typesOutgoing.keySet()) {

					final Set<PrefetchInfo> entriesToReplace = typesOutgoing.get(type);
					if (entriesToReplace.size() > 1) {

						final Set<String> combinedOutgoing = new LinkedHashSet<>();
						final Set<String> combinedIncoming = new LinkedHashSet<>();
						final Set<String> relTypes         = new LinkedHashSet<>();

						for (final PrefetchInfo info : entriesToReplace) {

							combinedOutgoing.addAll(info.getOutgoingSet());
							combinedIncoming.addAll(info.getIncomingSet());

							relTypes.addAll(info.getRelationshipTypes());

							prefetchBlacklist.put(prefetchHint + ": " + info.getPattern(), true);

							infos.remove(info);
						}

						final String pattern       = createPatternFromDetails(type, relTypes, true);
						final PrefetchInfo newInfo = new PrefetchInfo(pattern, type, true, combinedOutgoing, combinedIncoming, relTypes);

						infos.add(newInfo);
					}
				}

				for (final String type : typesIncoming.keySet()) {

					final Set<PrefetchInfo> entriesToReplace = typesIncoming.get(type);
					if (entriesToReplace.size() > 1) {

						final Set<String> combinedOutgoing = new LinkedHashSet<>();
						final Set<String> combinedIncoming = new LinkedHashSet<>();
						final Set<String> relTypes         = new LinkedHashSet<>();

						for (final PrefetchInfo info : entriesToReplace) {

							combinedOutgoing.addAll(info.getOutgoingSet());
							combinedIncoming.addAll(info.getIncomingSet());

							relTypes.addAll(info.getRelationshipTypes());

							prefetchBlacklist.put(prefetchHint + ": " + info.getPattern(), true);

							infos.remove(info);
						}

						final String pattern       = createPatternFromDetails(type, relTypes, false);
						final PrefetchInfo newInfo = new PrefetchInfo(pattern, type, false, combinedOutgoing, combinedIncoming, relTypes);

						infos.add(newInfo);
					}
				}
			}
		}
	}

	private void combinePrefetchingQueriesWithInheritance() {

		// check if we can combine multiple prefetching queries into one
		if (prefetchHint != null) {

			final Set<PrefetchInfo> infos = prefetchInfos.get(prefetchHint);
			if (infos != null) {

				final Queue<PrefetchInfo> outgoingKeyQueue = new LinkedList<>();
				final Queue<PrefetchInfo> incomingKeyQueue = new LinkedList<>();

				for (final PrefetchInfo info : infos) {

					if (info.isOutgoing()) {

						outgoingKeyQueue.add(info);

					} else {

						incomingKeyQueue.add(info);
					}
				}

				handleOutgoing(outgoingKeyQueue);
				handleIncoming(incomingKeyQueue);
			}
		}
	}

	private void handleOutgoing(final Queue<PrefetchInfo> infos) {

		boolean hasChanges = true;

		while (infos.size() > 1 && hasChanges) {

			hasChanges = false;

			final PrefetchInfo info1 = infos.remove();
			final PrefetchInfo info2 = infos.remove();

			if (info1 != null && info2 != null) {

				final String type1 = info1.getType();
				final String type2 = info2.getType();

				final String commonBaseType = getHighestCommonBaseType(type1, type2);
				if (commonBaseType != null) {

					hasChanges = true;

					final Set<String> rels = SetUtils.union(info1.getRelationshipTypes(), info2.getRelationshipTypes());
					final Set<String> set1 = SetUtils.union(info1.getOutgoingSet(), info2.getOutgoingSet());
					final Set<String> set2 = SetUtils.union(info1.getIncomingSet(), info2.getIncomingSet());
					final String pattern   = createPatternFromDetails(commonBaseType, rels, true);

					infos.add(new PrefetchInfo(pattern, commonBaseType, true, set1, set2, rels));

				} else {

					// add types again
					infos.add(info1);
					infos.add(info2);
				}
			}
		}
	}

	private void handleIncoming(final Queue<PrefetchInfo> queue) {

		boolean hasChanges = true;

		while (queue.size() > 1 && hasChanges) {

			hasChanges = false;

			final PrefetchInfo info1 = queue.remove();
			final PrefetchInfo info2 = queue.remove();

			if (info1 != null && info2 != null) {

				final String type1 = info1.getType();
				final String type2 = info2.getType();

				final String commonBaseType = getHighestCommonBaseType(type1, type2);
				if (commonBaseType != null) {

					hasChanges = true;

					final Set<String> rels = SetUtils.union(info1.getRelationshipTypes(), info2.getRelationshipTypes());
					final Set<String> set1 = SetUtils.union(info1.getOutgoingSet(), info2.getOutgoingSet());
					final Set<String> set2 = SetUtils.union(info1.getIncomingSet(), info2.getIncomingSet());
					final String pattern   = createPatternFromDetails(commonBaseType, rels, false);

					queue.add(new PrefetchInfo(pattern, commonBaseType, false, set1, set2, rels));

				} else {

					queue.add(info1);
					queue.add(info2);
				}
			}
		}
	}

	private String getHighestCommonBaseType(final String type1, final String type2) {

		final Set<String> types1 = getBaseTypes(type1);
		final Set<String> types2 = getBaseTypes(type2);

		types1.retainAll(types2);

		return Iterables.first(types1);
	}

	private String stringify(final Map map) {

		final Gson gson = new GsonBuilder().create();

		return gson.toJson(map);
	}

	private Set<String> getBaseTypes(final String type) {

		final Set<String> baseTypes = new LinkedHashSet<>();
		final Queue<String> queue   = new LinkedList<>();

		queue.add(type);

		while (!queue.isEmpty()) {

			final String c = queue.remove();

			baseTypes.add(c);
		}

		return baseTypes;
	}

	public Iterable<org.structr.api.graph.Node> getCachedResult(final CypherQuery query) {

		final int hashCode = query.hashCode();
		Set<Long> cached   = queryResultCache.get(hashCode);

		if (cached != null) {

			// fetch nodes from cache (ids are cached and mapped to nodes)
			return Iterables.map(id -> nodes.get(id), cached);
		}

		final List<org.structr.api.graph.Node> nodes = Iterables.toList(Iterables.map(new PrefetchNodeMapper(db), new LazyRecordIterable(db, query)));
		final Set<Long> ids                          = new LinkedHashSet<>();

		queryResultCache.put(hashCode, ids);

		for (final org.structr.api.graph.Node node : nodes) {
			ids.add(node.getId().getId());
		}

		return nodes;
	}

	private class PrefetchInfo {

		private final Set<String> outgoingSet = new LinkedHashSet<>();
		private final Set<String> incomingSet = new LinkedHashSet<>();
		private final Set<String> relTypes    = new LinkedHashSet<>();
		private String type                   = null;
		private String pattern                = null;
		private boolean outgoing              = false;
		private int count                     = 1;

		public PrefetchInfo(final CypherQuery query) {

			this.pattern  = SessionTransaction.this.createPatternFromQuery(query);
			this.type     = query.getType();
			this.outgoing = query.isOutgoing();

			outgoingSet.add(type + "/all/OUTGOING/" + query.getRelationshipType());
			incomingSet.add(type + "/all/INCOMING/" + query.getRelationshipType());

			relTypes.add(query.getRelationshipType());
		}

		public PrefetchInfo(final String pattern, final String type, final boolean isOutgoing, final Set<String> outgoingSet, final Set<String> incomingSet, final Set<String> relTypes) {

			this.pattern  = pattern;
			this.type     = type;
			this.outgoing = isOutgoing;

			this.outgoingSet.addAll(outgoingSet);
			this.incomingSet.addAll(incomingSet);
			this.relTypes.addAll(relTypes);
		}

		@Override
		public String toString() {
			return pattern + ", " + outgoingSet + ", " + incomingSet;
		}

		public Set<String> getOutgoingSet() {
			return outgoingSet;
		}

		public Set<String> getIncomingSet() {
			return incomingSet;
		}

		public int incrementAndGetCount() {
			return ++count;
		}

		public int getCount() {
			return count;
		}

		public String getType() {
			return type;
		}

		public String getPattern() {
			return pattern;
		}

		public boolean isOutgoing() {
			return outgoing;
		}

		public Set<String> getRelationshipTypes() {
			return relTypes;
		}

		@Override
		public int hashCode() {
			return pattern.hashCode();
		}

		@Override
		public boolean equals(final Object other) {
			return other.hashCode() == this.hashCode();
		}
	}
}