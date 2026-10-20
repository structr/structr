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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;
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
import org.structr.api.util.QueryHistogram;
import org.structr.api.util.QueryTimer;
import org.structr.api.util.TimedIterable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 *
 */
abstract class SessionTransaction implements org.structr.api.Transaction<Long> {

	private static final Logger logger                         = LoggerFactory.getLogger(SessionTransaction.class);
	protected static final AtomicLong ID_SOURCE                = new AtomicLong();

	// a UUID in a prefetch hint, in either format, regardless of application.uuid.allowedformats: a hint names a kind of
	// request, so requests for different objects must share it, otherwise every object gets its own learned patterns
	private static final Pattern UUID_IN_HINT = Pattern.compile("(?<![0-9a-fA-F])([0-9a-fA-F]{32}|[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})(?![0-9a-fA-F])");

	protected static final Map<String, Set<PrefetchInfo>> prefetchInfos = new ConcurrentHashMap<>();
	protected static final Map<String, Boolean> prefetchBlacklist       = new ConcurrentHashMap<>();

	// the failed evaluations in a row of each pattern, by hint and pattern like the blacklist
	protected static final Map<String, Integer> prefetchFailures        = new ConcurrentHashMap<>();

	// the learned patterns whose prefetch query groups by m instead of n, see prefetchLearnedPatternAndCount
	protected static final Set<String> prefetchGroupedByOtherEnd        = ConcurrentHashMap.newKeySet();

	// the moving average of the time of a relationship lookup query, which estimates what the lookups that a prefetch
	// answers would have cost; a new lookup moves it by 1/LOOKUP_AVERAGE_WEIGHT of its difference, so it follows
	// changes of the latency or the load of the database without jumping with every single lookup
	private static final int LOOKUP_AVERAGE_WEIGHT                      = 100;
	private static final AtomicLong averageLookupNanos                  = new AtomicLong();

	protected final Map<String, PrefetchInfo> histogram = new ConcurrentHashMap<>();
	protected final Map<Long, RelationshipWrapper> rels = new HashMap<>();
	protected final Map<Long, NodeWrapper> nodes        = new HashMap<>();
	protected final List<QueryTimer> openTimers           = new ArrayList<>();
	protected final Set<Long> deletedNodes              = new HashSet<>();
	protected final Set<Long> createdNodes              = new HashSet<>();
	protected final Set<Long> deletedRels               = new HashSet<>();
	protected final Set<String> prefetchedOutgoing      = new HashSet<>();
	protected final Set<String> prefetchedIncoming      = new HashSet<>();
	protected final Set<String> prefetchedQueries       = new HashSet<>();
	protected final Set<Long> lockedNodes               = new HashSet<>();

	// set when the database terminated this transaction, so that it cannot commit even if the code that runs in it catches the RetryException
	protected RetryException terminated = null;

	// the nodes whose lookups were answered by a prefetch in this transaction, by typed key (e.g. DOMNode/all/OUTGOING/CONTAINS)
	protected final Map<String, Set<Long>> prefetchHits          = new HashMap<>();
	protected final Map<PrefetchInfo, ExecutedPrefetch> executedPrefetches = new LinkedHashMap<>();
	protected final Set<String> failedPrefetches                 = new HashSet<>();

	protected final Map<Integer, Set<Long>> queryResultCache = new HashMap<>();

	protected final BoltDatabaseService db;
	protected final long transactionId;

	// for the prefetch statistics; the setting is read once, so a transaction runs entirely with or without prefetching
	protected final boolean prefetchingEnabled;
	protected final long startNanos;
	protected long queryCount                           = 0L;
	protected long lookupQueryCount                     = 0L;

	protected String prefetchHint                       = null;
	protected boolean success                           = false;
	protected boolean isPing                            = false;
	protected boolean logPrefetching                    = false;
	protected boolean isRolledBack                      = false;

	public SessionTransaction(final BoltDatabaseService db) {

		this.transactionId      = ID_SOURCE.getAndIncrement();
		this.db                 = db;
		this.prefetchingEnabled = Settings.PrefetchingEnabled.getValue();
		this.startNanos         = System.nanoTime();
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

	@Override
	public long getTransactionId() {

		return this.transactionId;
	}

	@Override
	public boolean isRolledBack() {

		return isRolledBack;
	}

	public static void flushCaches() {

		synchronized (prefetchInfos) {

			prefetchInfos.clear();
			prefetchBlacklist.clear();
			prefetchFailures.clear();
			prefetchGroupedByOtherEnd.clear();
		}
	}

	/**
	 * Counts a relationship lookup query, with the time it took to run and read its result.
	 */
	static void lookupExecuted(final long nanos) {

		averageLookupNanos.updateAndGet(average -> average == 0L ? nanos : average + (nanos - average) / LOOKUP_AVERAGE_WEIGHT);

		PrefetchStatistics.lookupExecuted(nanos);
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
	public org.structr.api.graph.Node getNode(final Identity<Long> id) {

		return getNodeWrapper(id.getId());
	}

	@Override
	public org.structr.api.graph.Relationship getRelationship(Identity<Long> id) {

		return getRelationshipWrapper(id.getId());
	}

	@Override
	public void setNodeIsCreated(final Long id) {

		createdNodes.add(id);
	}

	@Override
	public boolean isNodeCreated(final Long id) {

		return createdNodes.contains(id);
	}

	@Override
	public boolean isNodeDeleted(final Long id) {

		return deletedNodes.contains(id);
	}

	@Override
	public boolean isRelationshipDeleted(final Long id) {

		return deletedRels.contains(id);
	}

	/**
	 * Records that the database terminated this transaction, and returns the exception to throw.
	 */
	protected RetryException terminated(final Throwable tex) {

		final RetryException rex = isFrequentSchemaChange(tex) ? new SchemaChangedException(tex) : new RetryException(tex);

		if (terminated == null) {

			terminated = rex;
		}

		return rex;
	}

	/**
	 * Throws a RetryException when the database terminated this transaction earlier and the code that ran in it caught
	 * the exception, so that the caller repeats the transaction instead of taking it as committed.
	 */
	protected void throwIfTerminated() {

		if (terminated != null) {

			final String message = "The database terminated the transaction before it was closed";

			// the type decides how long the caller waits before the repetition, see RetryException.delay()
			if (terminated instanceof SchemaChangedException) {

				throw new SchemaChangedException(message, terminated.getCause());
			}

			throw new RetryException(message, terminated.getCause());
		}
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

		evaluatePrefetching();
		combinePrefetchingQueriesWithIdenticalTypeAndDirection();

		if (prefetchHint != null) {

			PrefetchStatistics.transactionClosed(prefetchHint, prefetchingEnabled, System.nanoTime() - startNanos, queryCount, lookupQueryCount);
		}

		// this is internal, will only be displayed if logPrefetching is set to true at compile time..
		if (logPrefetching && prefetchHint != null) {

			final Set<PrefetchInfo> infos = prefetchInfos.get(prefetchHint);
			if (infos != null && !infos.isEmpty()) {

				logger.info("### prefetch hint: {}", prefetchHint);

				for (final PrefetchInfo info : infos) {

					logger.info("        {}", info.getPattern());

					for (final String rel : info.getOutgoingSet()) {

						logger.info("                OUT: {}", rel);
					}

					for (final String rel : info.getIncomingSet()) {

						logger.info("                IN:  {}", rel);
					}
				}
			}
		}
	}

	@Override
	public void prefetchHint(final String rawHint) {

		final String hint = normalizeHint(rawHint);

		if (this.prefetchHint == null) {

			this.prefetchHint = hint;
		}

		if (!prefetchingEnabled || hint == null || !hint.equals(this.prefetchHint)) {

			return;
		}

		final Set<PrefetchInfo> infos = prefetchInfos.get(prefetchHint);
		if (infos == null) {

			return;
		}

		for (final PrefetchInfo prefetch : infos) {

			final String pattern = prefetch.getPattern();

			// a nested transaction can give the same hint again, and a pattern that already ran must not
			// run again, nor replace the entities it loaded with 0 (which would exempt it from the evaluation)
			if (isBlacklisted(pattern) || isPrefetched(pattern, prefetch.getOutgoingSet(), prefetch.getIncomingSet())) {

				continue;
			}

			final long t0         = System.nanoTime();
			final long entities   = prefetchLearnedPatternAndCount(pattern, prefetch.getOutgoingSet(), prefetch.getIncomingSet());
			final long nanos      = System.nanoTime() - t0;
			final long dt         = nanos / 1_000_000L;
			final int maxDuration = Settings.PrefetchingMaxDuration.getValue();
			final int maxCount    = Settings.PrefetchingMaxCount.getValue();

			// remember for the cost/benefit evaluation when the transaction is closed
			executedPrefetches.put(prefetch, new ExecutedPrefetch(entities, nanos));

			PrefetchStatistics.prefetchExecuted(prefetchHint, pattern, entities, nanos);

			if (dt > maxDuration) {

				// a single slow execution can be a cold cache or a busy database, so it only counts as a failure
				fail(pattern, "it took " + dt + " ms, " + Settings.PrefetchingMaxDuration.getKey() + " is " + maxDuration);

			} else if (entities > maxCount) {

				// the size is checked when a pattern is learned, but combined patterns (see combinePrefetchingQueriesWithIdenticalTypeAndDirection) are not
				blacklist(pattern, "it loads " + entities + " entities, " + Settings.PrefetchingMaxCount.getKey() + " is " + maxCount);
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

		if (!isPrefetched(query, outgoingKeys, incomingKeys)) {

			prefetchAndCount(query, outgoingKeys, incomingKeys);
		}
	}

	/**
	 * Runs the prefetch query of a pattern like (n:Type)-[r:REL]->(m) and stores the relationships it returns in the
	 * relationship cache of n, the start node of each path, as the complete set of n's relationships of that type and
	 * direction.
	 *
	 * The other end m only gets a node wrapper. Its relationships of that type are NOT complete: the pattern loads
	 * those that start at a node labelled Type, but REL may connect m to nodes of other types as well, for example
	 * when the schema uses one relationship type name for Project-[:HAS]->Task and Milestone-[:HAS]->Task. Storing
	 * them at m would answer m's lookup with that partial set (see NodeWrapper.getRelationshipsFromCache).
	 *
	 * @return the number of distinct nodes and relationships that were loaded
	 */
	protected long prefetchAndCount(final String query, final Set<String> outgoingKeys, final Set<String> incomingKeys) {

		prefetchedOutgoing.addAll(outgoingKeys);
		prefetchedIncoming.addAll(incomingKeys);
		prefetchedQueries.add(query);

		final long t0             = System.currentTimeMillis();
		final StringBuilder buf   = new StringBuilder();
		final Set<Long> nodesSeen = new HashSet<>();
		final Set<Long> relsSeen  = new HashSet<>();

		buf.append("MATCH p = ");
		buf.append(query);
		buf.append(" RETURN p");

		for (final org.neo4j.driver.Record r : collectRecords(new SimpleCypherQuery(buf), null)) {

			final Path p = r.get("p").asPath();

			for (final Node n : p.nodes()) {

				getNodeWrapper(n);
				nodesSeen.add(n.id());
			}

			for (final Segment s : p) {

				final org.neo4j.driver.types.Relationship relationship = s.relationship();

				if (relsSeen.add(relationship.id())) {

					getNodeWrapper(s.start()).storeRelationship(getRelationshipWrapper(relationship), true);
				}
			}
		}

		final long count = nodesSeen.size() + relsSeen.size();

		if (logPrefetching || db.logQueries()) {

			logger.info(transactionId + ": prefetched {} entities in {} ms with {}: {} / {}", count, (System.currentTimeMillis() - t0), buf, nodes.size(), rels.size());
		}

		return count;
	}

	/**
	 * Runs the prefetch query of a learned pattern, which always has the form (n:Type)-[r:REL]->(m) or
	 * (n:Type)<-[r:REL]-(m), and stores what it returns like prefetchAndCount does.
	 *
	 * The query returns one row per node of one end, with the relationships and the nodes at the other end collected,
	 * so that the node of that end is transferred once and not once per relationship, as it is in the paths that
	 * prefetchAndCount asks for. The end with fewer distinct nodes is the one that repeats, so the query groups by
	 * it: (n:Project)-[:HAS]->(m) has few projects with many tasks each and groups by n, (n:Task)<-[:HAS]-(m) has the
	 * same projects at the other end and groups by m. Each execution counts both ends and chooses the grouping for
	 * the next one; the first one groups by n.
	 *
	 * @return the number of distinct nodes and relationships that were loaded
	 */
	protected long prefetchLearnedPatternAndCount(final String pattern, final Set<String> outgoingKeys, final Set<String> incomingKeys) {

		prefetchedOutgoing.addAll(outgoingKeys);
		prefetchedIncoming.addAll(incomingKeys);
		prefetchedQueries.add(pattern);

		final boolean groupedByM = prefetchGroupedByOtherEnd.contains(pattern);
		final long t0            = System.currentTimeMillis();
		final StringBuilder buf  = new StringBuilder();
		final Set<Long> nSeen    = new HashSet<>();
		final Set<Long> mSeen    = new HashSet<>();
		final Set<Long> relsSeen = new HashSet<>();

		buf.append("MATCH ");
		buf.append(pattern);

		if (groupedByM) {

			buf.append(" RETURN m AS node, collect([r, n]) AS pairs");

		} else {

			buf.append(" RETURN n AS node, collect([r, m]) AS pairs");
		}

		for (final org.neo4j.driver.Record r : collectRecords(new SimpleCypherQuery(buf), null)) {

			final Node node = r.get("node").asNode();

			getNodeWrapper(node);

			for (final org.neo4j.driver.Value pair : r.get("pairs").values()) {

				final org.neo4j.driver.types.Relationship relationship = pair.get(0).asRelationship();
				final Node other                                       = pair.get(1).asNode();
				final Node n                                           = groupedByM ? other : node;
				final Node m                                           = groupedByM ? node : other;

				getNodeWrapper(other);

				nSeen.add(n.id());
				mSeen.add(m.id());

				if (relsSeen.add(relationship.id())) {

					getNodeWrapper(n).storeRelationship(getRelationshipWrapper(relationship), true);
				}
			}
		}

		// the grouping for the next execution: by the end that has fewer distinct nodes, because that end repeats
		if (mSeen.size() < nSeen.size()) {

			prefetchGroupedByOtherEnd.add(pattern);

		} else {

			prefetchGroupedByOtherEnd.remove(pattern);
		}

		final Set<Long> nodesSeen = new HashSet<>(nSeen);

		nodesSeen.addAll(mSeen);

		final long count = nodesSeen.size() + relsSeen.size();

		if (logPrefetching || db.logQueries()) {

			logger.info(transactionId + ": prefetched {} entities in {} ms with {}: {} / {}", count, (System.currentTimeMillis() - t0), buf, nodes.size(), rels.size());
		}

		return count;
	}

	private boolean isPrefetched(final String query, final Set<String> outgoingKeys, final Set<String> incomingKeys) {

		return prefetchedOutgoing.containsAll(outgoingKeys) && prefetchedIncoming.containsAll(incomingKeys) && prefetchedQueries.contains(query);
	}

	/**
	 * Counts a relationship lookup that was served from a prefetch, so that the cost/benefit
	 * of the prefetch can be evaluated when the transaction is closed. A node counts once per
	 * key: without the prefetch, its first lookup would have been a query, but the repeated
	 * ones would have been answered by the relationship cache of the node anyway.
	 *
	 * @param typedKey the key of the lookup, e.g. DOMNode/all/OUTGOING/CONTAINS
	 * @param nodeId the node whose relationships were looked up
	 */
	public void countPrefetchHit(final String typedKey, final long nodeId) {

		prefetchHits.computeIfAbsent(typedKey, k -> new HashSet<>()).add(nodeId);
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

		final Set<Long> seenRels = new HashSet<>();

		for (final org.neo4j.driver.Record r : collectRecords(new SimpleCypherQuery(query, data), null)) {

			final List<org.neo4j.driver.types.Node> nodes        = (List)r.get("nodes").asList();
			final List<org.neo4j.driver.types.Relationship> rels = (List)r.get("rels").asList();

			for (final org.neo4j.driver.types.Node n : nodes) {

				final NodeWrapper nodeWrapper = getNodeWrapper(n);
				nodeWrapper.storePrefetchInfo(outgoingKeys);

				count++;
			}

			for (final org.neo4j.driver.types.Relationship rel : rels) {

				// a relationship can be returned more than once, storing it twice would duplicate it in the caches
				if (!seenRels.add(rel.id())) {

					continue;
				}

				final NodeWrapper start              = getNodeWrapper(rel.startNodeId());
				final NodeWrapper end                = getNodeWrapper(rel.endNodeId());
				final RelationshipWrapper relWrapper = getRelationshipWrapper(rel);

				// Only populate each node's relationship cache if the caller is claiming
				// that the corresponding direction is fully prefetched. Otherwise the
				// cache would be left with a partial set of relationships (e.g. after an
				// ancestor-chain traversal that loads only one outgoing and one incoming
				// CONTAINS per intermediate node). Later reads from the NodeWrapper's
				// relationship cache would silently return that partial set instead of
				// falling through to a real query.
				if (!outgoingKeys.isEmpty()) {

					start.storeRelationship(relWrapper, true);
					start.storePrefetchInfo(outgoingKeys);
				}

				if (!incomingKeys.isEmpty()) {

					end.storeRelationship(relWrapper, true);
					end.storePrefetchInfo(incomingKeys);
				}

				count++;
			}

			// Optional columns: relationships that are complete for the INCOMING direction of their end
			// node only, with their start nodes. The start nodes lie outside the prefetched structure
			// (only this one relationship of theirs is known), so they are cached but not marked as
			// prefetched in any direction.
			if (r.containsKey("incomingRels") && !incomingKeys.isEmpty()) {

				if (r.containsKey("incomingNodes")) {

					final List<org.neo4j.driver.types.Node> incomingNodes = (List)r.get("incomingNodes").asList();

					for (final org.neo4j.driver.types.Node n : incomingNodes) {

						getNodeWrapper(n);
						count++;
					}
				}

				final List<org.neo4j.driver.types.Relationship> incomingRels = (List)r.get("incomingRels").asList();

				for (final org.neo4j.driver.types.Relationship rel : incomingRels) {

					if (!seenRels.add(rel.id())) {

						continue;
					}

					final NodeWrapper end                = getNodeWrapper(rel.endNodeId());
					final RelationshipWrapper relWrapper = getRelationshipWrapper(rel);

					end.storeRelationship(relWrapper, true);
					end.storePrefetchInfo(incomingKeys);

					count++;
				}
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

		if (isFrequentSchemaChange(dex)) {

			throw new SchemaChangedException(dex);
		}

		switch (dex.code()) {

			case "Neo.DatabaseError.General.UnknownError":
				throw new DataFormatException(dex, dex.code(), dex.getMessage());

			// add handlers / translated exceptions for DatabaseExceptions here..
		}

		// Log the status code: it is the only thing that says whether this error was worth
		// retrying, and UnknownDatabaseException carries it but nobody ever reads it, so an
		// unclassified error reaches the log as a bare message and cannot be classified
		// afterwards. Add a case above once a code shows up here often enough to matter.
		logger.warn("Unhandled database error {}: {}", dex.code(), dex.getMessage());

		// wrap exception if no other cause could be found
		throw new UnknownDatabaseException(dex, dex.code(), dex.getMessage());
	}

	/**
	 * Whether the given error is transient although Neo4j does not report it as a TransientException, so that only a
	 * repetition of the whole transaction can succeed. Cypher gives up compiling a query when the schema changes too often
	 * while it compiles, e.g. while freshly created indexes are populated, and reports that with the generic code
	 * Neo.DatabaseError.General.UnknownError, so only the message identifies it.
	 */
	static boolean isFrequentSchemaChange(final Throwable t) {

		if (!(t instanceof DatabaseException dex)) {

			return false;
		}

		final String message = dex.getMessage();

		return "Neo.DatabaseError.General.UnknownError".equals(dex.code()) && message != null && message.contains("insanely frequent schema changes");
	}

	/**
	 * Replaces the UUIDs in a prefetch hint with a placeholder, so that requests that differ only in the object they
	 * address, like REST GET /structr/rest/Project/{uuid}, learn and use the same prefetch patterns (ticket 1420).
	 */
	static String normalizeHint(final String hint) {

		if (hint == null) {

			return null;
		}

		return UUID_IN_HINT.matcher(hint).replaceAll("<uuid>");
	}

	// ----- protected methods -----
	// ----- query timing -----
	protected QueryTimer startTimer(final String statement) {

		final QueryTimer timer = QueryHistogram.newTimer();

		timer.started(statement);

		return timer;
	}

	protected Iterable<Record> timed(final QueryTimer timer, final Iterable<Record> records) {

		openTimers.add(timer);

		return new TimedIterable<>(timer, records);
	}

	/**
	 * Records the queries whose results were not read to the end, at the time of their last record.
	 */
	protected void recordOpenTimers() {

		for (final QueryTimer timer : openTimers) {

			timer.record();
		}

		openTimers.clear();
	}

	protected void logQuery(final CypherQuery query) {

		if (query.getType() != null && query.getRelationshipType() != null) {

			lookupQueryCount++;

			if (prefetchHint != null && prefetchingEnabled) {

				learn(query);
			}
		}

		logQuery(query.getStatement(), query.getParameters());
	}

	/**
	 * Counts a relationship lookup and learns its pattern for the hint of this transaction once it ran more
	 * often than the threshold.
	 */
	private void learn(final CypherQuery query) {

		// The statement of a lookup does not contain the type of the node, and relationships of different source
		// types can share a relationship type name, so the type is part of the key. Otherwise their lookups would
		// be counted together and learned as a pattern for the type of the first one.
		final String key        = query.getType() + " " + query.getStatement();
		final PrefetchInfo info = histogram.get(key);

		if (info == null) {

			histogram.put(key, new PrefetchInfo(query));

			return;
		}

		final int count      = info.incrementAndGetCount();
		final int threshold  = Settings.PrefetchingThreshold.getValue();
		final String pattern = info.getPattern();

		if (count <= threshold || isBlacklisted(pattern)) {

			return;
		}

		// the set is shared by all transactions with this hint and is iterated in prefetchHint() while other
		// requests learn or combine patterns, so it must allow concurrent modification (ticket 1420)
		final Set<PrefetchInfo> infos = prefetchInfos.computeIfAbsent(prefetchHint, k -> new CopyOnWriteArraySet<>());

		if (infos.contains(info)) {

			return;
		}

		final long prefetchResultCount = getLong(new SimpleCypherQuery("MATCH p = " + pattern + " RETURN count(p)"));
		final int maxCount             = Settings.PrefetchingMaxCount.getValue();

		if (prefetchResultCount < maxCount) {

			if (logPrefetching || db.logQueries()) {

				logger.info("{}: Activating prefetching for {} because it runs more than {} times in a single transaction", transactionId, pattern, threshold);
			}

			infos.add(info);

			PrefetchStatistics.patternLearned(prefetchHint, pattern);

		} else {

			blacklist(pattern, "it returns " + prefetchResultCount + " results, " + Settings.PrefetchingMaxCount.getKey() + " is " + maxCount);
		}
	}

	protected void logQuery(final String query, final Map<String, Object> map) {

		queryCount++;

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

	/**
	 * Deactivates the prefetches of this transaction that did not pay off. A prefetch pattern is
	 * type-wide (all nodes of a type with their relationships of some types), so its cost grows
	 * with the database and not with the request it was learned for. Loading the graph is only
	 * worth it when the request then serves many relationship lookups from it. A prefetch fails
	 * its evaluation when it loads more entities per served lookup than the configured ratio, or
	 * when it took longer than the served lookups would have taken. The second rule follows the
	 * latency of the database: with a remote database, a lookup costs a network round trip, and
	 * a prefetch pays off with far fewer served lookups than with a local one. The ratio limits
	 * the memory a prefetch takes, whatever the latency. A prefetch that fails repeatedly is
	 * deactivated for this hint (see fail), otherwise every request with this hint would pay for
	 * loading the whole graph.
	 */
	private void evaluatePrefetching() {

		if (prefetchHint == null || executedPrefetches.isEmpty() || !success || isRolledBack) {

			return;
		}

		final int costRatio           = Settings.PrefetchingCostRatio.getValue();
		final boolean compareDuration = Settings.PrefetchingCompareDuration.getValue();
		final long lookupNanos        = averageLookupNanos.get();

		for (final Map.Entry<PrefetchInfo, ExecutedPrefetch> entry : executedPrefetches.entrySet()) {

			final PrefetchInfo info = entry.getKey();
			final String pattern    = info.getPattern();
			final long entities     = entry.getValue().entities();
			final long nanos        = entry.getValue().nanos();

			// a modification in this transaction invalidated the prefetched data (and the hit counting with it);
			// a pattern that loaded nothing is still evaluated, it can answer lookups of nodes without relationships
			if (!prefetchedQueries.contains(pattern)) {

				continue;
			}

			long hits = 0;

			for (final String key : info.getOutgoingSet()) {

				hits += prefetchHits.getOrDefault(key, Set.of()).size();
			}

			for (final String key : info.getIncomingSet()) {

				hits += prefetchHits.getOrDefault(key, Set.of()).size();
			}

			PrefetchStatistics.lookupsSaved(prefetchHint, pattern, hits);

			if (entities > hits * costRatio) {

				fail(pattern, "it loaded " + entities + " entities for " + hits + " lookups, " + Settings.PrefetchingCostRatio.getKey() + " is " + costRatio);

			} else if (compareDuration && lookupNanos > 0L && nanos > hits * lookupNanos) {

				fail(pattern, "it took " + millis(nanos) + " ms, the " + hits + " lookups it answered would have taken about " + millis(hits * lookupNanos) + " ms");

			} else if (!failedPrefetches.contains(pattern)) {

				prefetchFailures.remove(prefetchHint + ": " + pattern);
			}
		}
	}

	private boolean isBlacklisted(final String pattern) {

		return prefetchBlacklist.containsKey(prefetchHint + ": " + pattern);
	}

	/**
	 * Counts a failed evaluation of a pattern and deactivates it once it failed in database.prefetching.maxfailures
	 * transactions in a row. A hint names a kind of request, but its requests differ: a list can be asked for an
	 * empty page, a detail request for an object without relationships. Such a request uses little or nothing of
	 * the prefetched data, and deactivating the pattern for good on it would take the prefetch from all the other
	 * requests with this hint, for which it pays off. A transaction without a failure starts the count again.
	 */
	private void fail(final String pattern, final String reason) {

		// a transaction counts once, also when the prefetch was too slow and loaded too much
		if (!failedPrefetches.add(pattern)) {

			return;
		}

		final int maxFailures = Math.max(1, Settings.PrefetchingMaxFailures.getValue());
		final int failures    = prefetchFailures.merge(prefetchHint + ": " + pattern, 1, Integer::sum);

		PrefetchStatistics.evaluationFailed(prefetchHint, pattern);

		if (logPrefetching || db.logQueries()) {

			logger.info("{}: Prefetching pattern {} for {} failed {} of {} times in a row because {}", transactionId, pattern, prefetchHint, failures, maxFailures, reason);
		}

		if (failures >= maxFailures) {

			blacklist(pattern, failures > 1 ? reason + ", in " + failures + " transactions in a row" : reason);
		}
	}

	/**
	 * Deactivates a pattern for the hint of this transaction, until the caches are flushed.
	 */
	private void blacklist(final String pattern, final String reason) {

		if (logPrefetching || db.logQueries()) {

			logger.info("{}: Blacklisting prefetching pattern {} for {} because {}", transactionId, pattern, prefetchHint, reason);
		}

		prefetchBlacklist.put(prefetchHint + ": " + pattern, true);
		prefetchFailures.remove(prefetchHint + ": " + pattern);

		final Set<PrefetchInfo> infos = prefetchInfos.get(prefetchHint);
		if (infos != null) {

			infos.removeIf(info -> pattern.equals(info.getPattern()));
		}

		PrefetchStatistics.patternDeactivated(prefetchHint, pattern, reason);
	}

	private void combinePrefetchingQueriesWithIdenticalTypeAndDirection() {

		// check if we can combine multiple prefetching queries into one
		if (prefetchHint != null) {

			final Set<PrefetchInfo> infos = prefetchInfos.get(prefetchHint);
			if (infos != null) {

				final Map<String, Set<PrefetchInfo>> typesOutgoing = new ConcurrentHashMap<>();
				final Map<String, Set<PrefetchInfo>> typesIncoming = new ConcurrentHashMap<>();

				for (final PrefetchInfo info : infos) {

					// a pattern that was deactivated in a concurrent transaction must not come back as part of a combined one
					if (isBlacklisted(info.getPattern())) {

						continue;
					}

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
						}

						final String pattern       = createPatternFromDetails(type, relTypes, true);
						final PrefetchInfo newInfo = new PrefetchInfo(pattern, type, true, combinedOutgoing, combinedIncoming, relTypes);

						infos.add(newInfo);

						PrefetchStatistics.patternLearned(prefetchHint, pattern);

						for (final PrefetchInfo info : entriesToReplace) {

							blacklist(info.getPattern(), "it is part of " + pattern);
						}
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
						}

						final String pattern       = createPatternFromDetails(type, relTypes, false);
						final PrefetchInfo newInfo = new PrefetchInfo(pattern, type, false, combinedOutgoing, combinedIncoming, relTypes);

						infos.add(newInfo);

						PrefetchStatistics.patternLearned(prefetchHint, pattern);

						for (final PrefetchInfo info : entriesToReplace) {

							blacklist(info.getPattern(), "it is part of " + pattern);
						}
					}
				}
			}
		}
	}

	private String stringify(final Map map) {

		final Gson gson = new GsonBuilder().create();

		return gson.toJson(map);
	}

	public Iterable<org.structr.api.graph.Node<Long>> getCachedResult(final CypherQuery query) {

		final int hashCode = query.hashCode();
		Set<Long> cached   = queryResultCache.get(hashCode);

		if (cached != null) {

			// fetch nodes from cache (ids are cached and mapped to nodes)

			return Iterables.map(id -> nodes.get(id), cached);
		}

		final List<org.structr.api.graph.Node<Long>> nodes = Iterables.toList(Iterables.map(new PrefetchNodeMapper(db), new LazyRecordIterable(db, query)));
		final Set<Long> ids                                = new LinkedHashSet<>();

		queryResultCache.put(hashCode, ids);

		for (final org.structr.api.graph.Node<Long> node : nodes) {

			ids.add(node.getId().getId());
		}

		return nodes;
	}

	private static String millis(final long nanos) {

		return String.format(Locale.ROOT, "%.1f", nanos / 1_000_000.0);
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

			// The pattern loads the relationships of the queried direction only. Marking the other direction as
			// prefetched as well would serve every lookup in it from an empty cache, e.g. a schema reload that
			// runs in a request would see no target node for any schema relationship.
			if (outgoing) {

				outgoingSet.add(type + "/all/OUTGOING/" + query.getRelationshipType());

			} else {

				incomingSet.add(type + "/all/INCOMING/" + query.getRelationshipType());
			}

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

	/**
	 * A learned prefetch that ran in this transaction, with the number of entities it loaded and the time it took.
	 */
	protected record ExecutedPrefetch(long entities, long nanos) {
	}
}
