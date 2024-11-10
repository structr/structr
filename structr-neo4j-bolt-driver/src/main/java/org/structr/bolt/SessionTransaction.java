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

	private static final Logger logger                                  = LoggerFactory.getLogger(SessionTransaction.class);
	private static final Set<String> relationshipTypeBlacklist          = Set.of(); //Set.of("SECURITY", "OWNS");
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
	protected BoltDatabaseService db                    = null;
	protected String prefetchHint                       = null;
	protected long transactionId                        = 0L;
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

	protected abstract void set(final String statement, final Map<String, Object> map);

	public abstract Iterable<Record> newIterable(final BoltDatabaseService db, final CypherQuery query);

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

				logger.info("Query count histogram for transaction {}:", getTransactionId());

				for (final String key : histogram.keySet()) {

					logger.info("{}", histogram.get(key));

				}
			}
		}

		optimizePrefetching();

		if (logPrefetching && prefetchHint != null) {

			final Map<String, PrefetchInfo> infos = prefetchInfos.get(prefetchHint);
			if (infos != null && !infos.isEmpty()) {

				System.out.println("############################################################################### " + prefetchHint);

				for (final PrefetchInfo info : infos.values()) {

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

			final Map<String, PrefetchInfo> infos = prefetchInfos.get(prefetchHint);
			if (infos != null) {

				for (final PrefetchInfo prefetch : infos.values()) {

					final long t0 = System.currentTimeMillis();
					final String pattern = prefetch.getPattern();

					if (!prefetchBlacklist.containsKey(prefetchHint + ": " + pattern)) {

						prefetch(pattern, prefetch.getOutgoingSet(), prefetch.getIncomingSet());

						final long t = System.currentTimeMillis() - t0;

						if (t > Settings.PrefetchingMaxDuration.getValue(500)) {

							if (logPrefetching || db.logQueries()) {

								// blacklist prefetching calls that take too long
								logger.info("Blacklisting prefetching pattern {} because it takes {} ms, {} is {}", pattern, t, Settings.PrefetchingMaxDuration.getKey(), Settings.PrefetchingMaxDuration.getValue(500));
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

			if (!relationshipTypeBlacklist.contains(query.getRelationshipType())) {

				final String statement = query.getStatement();

				final PrefetchInfo info = histogram.get(statement);
				if (info != null) {

					final int count     = info.incrementAndGetCount();
					final int threshold = Settings.PrefetchingThreshold.getValue(100);

					if (count > threshold && !prefetchBlacklist.containsKey(prefetchHint + ": " + info.getPattern())) {

						final Map<String, PrefetchInfo> infos = prefetchInfos.computeIfAbsent(prefetchHint, k -> new LinkedHashMap<>());

						// store prefetching info and log info message
						if (infos.put(statement, info) == null) {

							final String pattern = info.getPattern();

							final long prefetchResultCount = getLong(new SimpleCypherQuery("MATCH p = " + pattern + " RETURN count(p)"));
							if (prefetchResultCount < Settings.PrefetchingMaxCount.getValue(100_000)) {

								if (logPrefetching || db.logQueries()) {

									logger.info("Activating prefetching for {} because it runs more than {} times in a single transaction", pattern, threshold);
								}

							} else {

								if (logPrefetching || db.logQueries()) {

									logger.info("NOT activating prefetching for {} because it returns more than {} results", pattern, prefetchResultCount);
								}

								prefetchBlacklist.put(prefetchHint + ": " + pattern, true);
							}
						}
					}

				} else {

					histogram.put(statement, new PrefetchInfo(query));
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

		final String rawTenantIdentifier = db.getTenantIdentifier();
		final String tenantIdentifier    = StringUtils.isNotBlank(rawTenantIdentifier) ? ":" + rawTenantIdentifier : "";
		final Class type                 = query.getType();
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

			pattern = "(n:" + type.getSimpleName() + tenantIdentifier + ")" + pattern;

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

			final Map<String, PrefetchInfo> infos = prefetchInfos.get(prefetchHint);
			if (infos != null) {

				final String rawTenantIdentifier = db.getTenantIdentifier();
				final String tenantIdentifier = StringUtils.isNotBlank(rawTenantIdentifier) ? ":" + rawTenantIdentifier : "";
				final Map<Class, Set<String>> typesOutgoing = new LinkedHashMap<>();
				final Map<Class, Set<String>> typesIncoming = new LinkedHashMap<>();

				for (final String key : infos.keySet()) {

					final PrefetchInfo info = infos.get(key);
					final Class type = info.getType();

					if (info.isOutgoing()) {

						typesOutgoing.computeIfAbsent(type, k -> new LinkedHashSet<>()).add(key);


					} else {

						typesIncoming.computeIfAbsent(type, k -> new LinkedHashSet<>()).add(key);
					}
				}

				final int combinedSizes = typesOutgoing.size() + typesIncoming.size();

				if (combinedSizes < infos.size()) {

					for (final Class type : typesOutgoing.keySet()) {

						final Set<String> entriesToReplace = typesOutgoing.get(type);
						if (entriesToReplace.size() > 1) {

							final Set<String> combinedOutgoing = new LinkedHashSet<>();
							final Set<String> combinedIncoming = new LinkedHashSet<>();
							final Set<String> relTypes = new LinkedHashSet<>();

							for (final String entryToReplace : entriesToReplace) {

								final PrefetchInfo info = infos.remove(entryToReplace);

								combinedOutgoing.addAll(info.getOutgoingSet());
								combinedIncoming.addAll(info.getIncomingSet());

								relTypes.addAll(info.getRelationshipTypes());

								prefetchBlacklist.put(prefetchHint + ": " + info.getPattern(), true);
							}

							final String pattern = "(n:" + type.getSimpleName() + tenantIdentifier + ")-[r:" + StringUtils.join(relTypes, "|") + "]->(m)";
							final PrefetchInfo newInfo = new PrefetchInfo(pattern, type, true, combinedOutgoing, combinedIncoming, relTypes);

							infos.put(pattern, newInfo);
						}
					}

					for (final Class type : typesIncoming.keySet()) {

						final Set<String> entriesToReplace = typesIncoming.get(type);
						if (entriesToReplace.size() > 1) {

							final Set<String> combinedOutgoing = new LinkedHashSet<>();
							final Set<String> combinedIncoming = new LinkedHashSet<>();
							final Set<String> relTypes = new LinkedHashSet<>();

							for (final String entryToReplace : entriesToReplace) {

								final PrefetchInfo info = infos.remove(entryToReplace);

								combinedOutgoing.addAll(info.getOutgoingSet());
								combinedIncoming.addAll(info.getIncomingSet());

								relTypes.addAll(info.getRelationshipTypes());

								prefetchBlacklist.put(prefetchHint + ": " + info.getPattern(), true);
							}

							final String pattern = "(n:" + type.getSimpleName() + tenantIdentifier + ")<-[r:" + StringUtils.join(relTypes, "|") + "]-(m)";
							final PrefetchInfo newInfo = new PrefetchInfo(pattern, type, false, combinedOutgoing, combinedIncoming, relTypes);

							infos.put(pattern, newInfo);
						}
					}
				}
			}
		}
	}

	private void combinePrefetchingQueriesWithInheritance() {

		// check if we can combine multiple prefetching queries into one
		if (prefetchHint != null) {

			final Map<String, PrefetchInfo> infos = prefetchInfos.get(prefetchHint);
			if (infos != null) {

				final Queue<String> outgoingKeyQueue = new LinkedList<>();
				final Queue<String> incomingKeyQueue = new LinkedList<>();

				for (final String key : infos.keySet()) {

					final PrefetchInfo info = infos.get(key);
					if (info.isOutgoing()) {

						outgoingKeyQueue.add(key);

					} else {

						incomingKeyQueue.add(key);
					}
				}

				handleOutgoing(infos, outgoingKeyQueue);
				handleIncoming(infos, incomingKeyQueue);
			}
		}
	}

	private void handleOutgoing(final Map<String, PrefetchInfo> infos, final Queue<String> keyQueue) {

		final String rawTenantIdentifier = db.getTenantIdentifier();
		final String tenantIdentifier    = StringUtils.isNotBlank(rawTenantIdentifier) ? ":" + rawTenantIdentifier : "";

		boolean hasChanges = true;

		while (keyQueue.size() > 1 && hasChanges) {

			hasChanges = false;

			final String key1 = keyQueue.remove();
			final String key2 = keyQueue.remove();

			final PrefetchInfo info1 = infos.remove(key1);
			final PrefetchInfo info2 = infos.remove(key2);

			if (info1 != null && info2 != null) {

				final Class type1 = info1.getType();
				final Class type2 = info2.getType();

				final Class commonBaseType = getHighestCommonBaseType(type1, type2);
				if (commonBaseType != null) {

					hasChanges = true;

					final Set<String> rels = SetUtils.union(info1.getRelationshipTypes(), info2.getRelationshipTypes());
					final Set<String> set1 = SetUtils.union(info1.getOutgoingSet(), info2.getOutgoingSet());
					final Set<String> set2 = SetUtils.union(info1.getIncomingSet(), info2.getIncomingSet());

					final String pattern = "(n:" + commonBaseType.getSimpleName() + tenantIdentifier + ")-[r:" + StringUtils.join(rels, "|") + "]->(m)";

					// add new key to the key queue
					keyQueue.add(pattern);

					infos.put(pattern, new PrefetchInfo(pattern, commonBaseType, true, set1, set2, rels));

				} else {

					// add second type again
					keyQueue.add(key2);

					infos.put(key1, info1);
					infos.put(key2, info2);
				}
			}
		}
	}

	private void handleIncoming(final Map<String, PrefetchInfo> infos, final Queue<String> keyQueue) {

		final String rawTenantIdentifier = db.getTenantIdentifier();
		final String tenantIdentifier    = StringUtils.isNotBlank(rawTenantIdentifier) ? ":" + rawTenantIdentifier : "";

		boolean hasChanges = true;

		while (keyQueue.size() > 1 && hasChanges) {

			hasChanges = false;

			final String key1 = keyQueue.remove();
			final String key2 = keyQueue.remove();

			final PrefetchInfo info1 = infos.remove(key1);
			final PrefetchInfo info2 = infos.remove(key2);

			if (info1 != null && info2 != null) {

				final Class type1 = info1.getType();
				final Class type2 = info2.getType();

				final Class commonBaseType = getHighestCommonBaseType(type1, type2);
				if (commonBaseType != null) {

					hasChanges = true;

					final Set<String> rels = SetUtils.union(info1.getRelationshipTypes(), info2.getRelationshipTypes());
					final Set<String> set1 = SetUtils.union(info1.getOutgoingSet(), info2.getOutgoingSet());
					final Set<String> set2 = SetUtils.union(info1.getIncomingSet(), info2.getIncomingSet());

					final String pattern = "(n:" + commonBaseType.getSimpleName() + tenantIdentifier + ")<-[r:" + StringUtils.join(rels, "|") + "]->(m)";

					// add new key to the key queue
					keyQueue.add(pattern);

					infos.put(pattern, new PrefetchInfo(pattern, commonBaseType, false, set1, set2, rels));

				} else {

					// add second type again
					keyQueue.add(key2);

					infos.put(key1, info1);
					infos.put(key2, info2);
				}
			}
		}
	}

	private Class getHighestCommonBaseType(final Class type1, final Class type2) {

		final Set<Class> types1 = getBaseTypes(type1);
		final Set<Class> types2 = getBaseTypes(type2);

		types1.retainAll(types2);

		return Iterables.first(types1);
	}

	private Set<Class> getBaseTypes(final Class type) {

		final Set<String> blacklist = Set.of("NodeInterface", "AbstractNode");
		final Set<Class> baseTypes = new LinkedHashSet<>();
		final Queue<Class> queue   = new LinkedList<>();

		queue.add(type);

		while (!queue.isEmpty()) {

			final Class c = queue.remove();

			baseTypes.add(c);

			final Class superClass = c.getSuperclass();
			if (superClass != null && Object.class != superClass && !blacklist.contains(superClass.getSimpleName())) {

				queue.add(superClass);
			}

			// add interfaces as well
			for (final Class iface : c.getInterfaces()) {

				if (!blacklist.contains(iface.getSimpleName())) {

					queue.add(iface);
				}
			}
		}

		return baseTypes;
	}

	private class PrefetchInfo {

		private final Set<String> outgoingSet = new LinkedHashSet<>();
		private final Set<String> incomingSet = new LinkedHashSet<>();
		private final Set<String> relTypes    = new LinkedHashSet<>();
		private Class type                    = null;
		private String pattern                = null;
		private boolean outgoing              = false;
		private int count                     = 1;

		public PrefetchInfo(final CypherQuery query) {

			this.pattern  = SessionTransaction.this.getPattern(query);
			this.type     = query.getType();
			this.outgoing = query.isOutgoing();

			outgoingSet.add(type.getSimpleName() + "/all/OUTGOING/" + query.getRelationshipType());
			incomingSet.add(type.getSimpleName() + "/all/INCOMING/" + query.getRelationshipType());

			relTypes.add(query.getRelationshipType());
		}

		public PrefetchInfo(final String pattern, final Class type, final boolean isOutgoing, final Set<String> outgoingSet, final Set<String> incomingSet, final Set<String> relTypes) {

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

		public Class getType() {
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
			return ((PrefetchInfo)other).hashCode() == this.hashCode();
		}
	}
}