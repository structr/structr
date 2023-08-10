/*
 * Copyright (C) 2010-2023 Structr GmbH
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
import org.structr.api.NotFoundException;
import org.structr.api.config.Settings;
import org.structr.api.graph.Direction;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.api.search.QueryContext;
import org.structr.api.util.FixedSizeCache;
import org.structr.api.util.Iterables;

import java.util.*;

/**
 *
 */
class NodeWrapper extends EntityWrapper<org.neo4j.driver.types.Node> implements Node {

	protected static FixedSizeCache<Long, NodeWrapper> nodeCache                 = null;

	private final Map<String, Map<String, RelationshipResult>> relationshipCache = new HashMap<>();
	private boolean dontUseCache                                                 = false;

	protected NodeWrapper() {
		// nop constructor for cache access
		super();
	}

	private NodeWrapper(final BoltDatabaseService db, final org.neo4j.driver.types.Node node) {
		super(db, node);
	}

	public static void initialize(final int cacheSize) {
		nodeCache = new FixedSizeCache<>("Node cache", cacheSize);
	}

	@Override
	public String toString() {
		return "N" + getId();
	}

	@Override
	protected String getQueryPrefix() {

		return concat("MATCH (n", getTenantIdentifer(db), ")");
	}

	@Override
	public void onRemoveFromCache() {

		relationshipCache.clear();
		this.stale = true;
	}

	@Override
	public void clearCaches() {

		relationshipCache.clear();
	}

	@Override
	public void onClose() {

		dontUseCache = false;
		relationshipCache.clear();
	}

	@Override
	public Relationship createRelationshipTo(final Node endNode, final RelationshipType relationshipType) {
		return createRelationshipTo(endNode, relationshipType, new LinkedHashMap<>());
	}

	@Override
	public Relationship createRelationshipTo(final Node endNode, final RelationshipType relationshipType, final Map<String, Object> properties) {

		dontUseCache = true;

		assertNotStale();

		final SessionTransaction tx   = db.getCurrentTransaction();
		final Map<String, Object> map = new HashMap<>();
		final NodeWrapper otherNode   = (NodeWrapper)endNode;
		final String tenantIdentifier = getTenantIdentifer(db);
		final StringBuilder buf       = new StringBuilder();

		map.put("id1", id);
		map.put("id2", db.unwrap(endNode.getId()));
		map.put("relProperties", properties);

		buf.append("MATCH (n");
		buf.append(tenantIdentifier);
		buf.append("), (m");
		buf.append(tenantIdentifier);
		buf.append(") WHERE ID(n) = $id1 AND ID(m) = $id2 ");
		buf.append("MERGE (n)-[r:");
		buf.append(relationshipType.name());
		buf.append("]->(m)");
		buf.append(" SET r += $relProperties RETURN r");

		final org.neo4j.driver.types.Relationship rel = tx.getRelationship(buf.toString(), map);

		setModified();
		otherNode.setModified();

		// clear caches
		((NodeWrapper)endNode).relationshipCache.clear();
		relationshipCache.clear();

		final RelationshipWrapper createdRelationship = RelationshipWrapper.newInstance(db, rel);

		createdRelationship.setModified();

		return createdRelationship;
	}

	@Override
	public void addLabel(final String label) {

		assertNotStale();

		final SessionTransaction tx   = db.getCurrentTransaction();
		final Map<String, Object> map = new HashMap<>();
		final String tenantIdentifier = getTenantIdentifer(db);

		map.put("id", id);

		tx.set(concat("MATCH (n", tenantIdentifier, ") WHERE ID(n) = $id SET n :", label), map);

		setModified();
	}

	@Override
	public void removeLabel(final String label) {

		assertNotStale();

		final SessionTransaction tx   = db.getCurrentTransaction();
		final Map<String, Object> map = new HashMap<>();
		final String tenantIdentifier = getTenantIdentifer(db);

		map.put("id", id);

		tx.set(concat("MATCH (n", tenantIdentifier, ") WHERE ID(n) = $id REMOVE n:", label), map);
		setModified();
	}

	@Override
	public Iterable<String> getLabels() {

		assertNotStale();

		final SessionTransaction tx   = db.getCurrentTransaction();
		final Map<String, Object> map = new HashMap<>();
		final String tenantIdentifier = getTenantIdentifer(db);

		map.put("id", id);

		// execute query
		return tx.getStrings(concat("MATCH (n", tenantIdentifier, ") WHERE ID(n) = $id RETURN LABELS(n)"), map);
	}

	@Override
	public boolean hasRelationshipTo(final RelationshipType type, final Node targetNode) {

		assertNotStale();

		final SessionTransaction tx      = db.getCurrentTransaction();
		final Map<String, Object> params = new LinkedHashMap<>();
		final String tenantIdentifier    = getTenantIdentifer(db);

		params.put("id1", id);
		params.put("id2", db.unwrap(targetNode.getId()));

		try {

			// try to fetch existing relationship by node ID(s)
			tx.getLong(concat("MATCH (n", tenantIdentifier, ")-[r:", type.name(), "]->(m", tenantIdentifier, ") WHERE id(n) = $id1 AND id(m) = $id2 RETURN id(r)"), params);

			// success
			return true;

		} catch (Throwable t) {

			return false;
		}
	}

	@Override
	public Relationship getRelationshipTo(final RelationshipType type, final Node targetNode) {

		assertNotStale();

		final SessionTransaction tx                 = db.getCurrentTransaction();
		final Map<String, Object> params            = new LinkedHashMap<>();
		final String tenantIdentifier               = getTenantIdentifer(db);
		final RelationshipRelationshipMapper mapper = new RelationshipRelationshipMapper(db);

		params.put("id1", id);
		params.put("id2", db.unwrap(targetNode.getId()));

		try {

			return mapper.apply(tx.getRelationship(concat("MATCH (n", tenantIdentifier, ")-[r:", type.name(), "]->(m", tenantIdentifier, ") WHERE id(n) = $id1 AND id(m) = $id2 RETURN r"), params));

		} catch (Throwable t) {
		}

		return null;
	}

	@Override
	public Iterable<Relationship> getRelationships() {

		assertNotStale();

		final RelationshipResult cache = getRelationshipCache(null, null, null);
		final String tenantIdentifier  = getTenantIdentifer(db);

		return cache.getResult(db, id, concat("(n", tenantIdentifier, ")-[r]-(o)"), "RETURN r, o ORDER BY r.internalTimestamp");
	}

	@Override
	public Iterable<Relationship> getRelationships(final Direction direction) {

		assertNotStale();

		final RelationshipResult cache = getRelationshipCache(direction, null, null);
		final String tenantIdentifier  = getTenantIdentifer(db);

		switch (direction) {

			case BOTH:
				return getRelationships();

			case OUTGOING:
				return cache.getResult(db, id, concat("(n", tenantIdentifier, ")-[r]->(t)"), "RETURN r, t ORDER BY r.internalTimestamp");

			case INCOMING:
				return cache.getResult(db, id, concat("(n", tenantIdentifier , ")<-[r]-(s)"), "RETURN r, s ORDER BY r.internalTimestamp");
		}

		return null;
	}

	@Override
	public Iterable<Relationship> getRelationships(final Direction direction, final RelationshipType relationshipType) {
		return getRelationships(direction, relationshipType, null);
	}

	@Override
	public Iterable<Relationship> getRelationships(final Direction direction, final RelationshipType relationshipType, final String otherType) {

		assertNotStale();

		final RelationshipResult cache = getRelationshipCache(direction, relationshipType, otherType);
		final String tenantIdentifier  = getTenantIdentifer(db);
		final String rel               = relationshipType.name();
		final String typeLabel         = tenantIdentifier + (otherType != null ? (":" + otherType) : "");

		switch (direction) {

			case BOTH:
				return cache.getResult(db, id, concat("(n", tenantIdentifier, ")-[r:", rel, "]-(o", typeLabel, ")"), "RETURN r, o ORDER BY r.internalTimestamp");

			case OUTGOING:
				return cache.getResult(db, id, concat("(n", tenantIdentifier, ")-[r:", rel, "]->(t", typeLabel, ")"), "RETURN r, t ORDER BY r.internalTimestamp");

			case INCOMING:
				return cache.getResult(db, id, concat("(n", tenantIdentifier, ")<-[r:", rel, "]-(s", typeLabel, ")"), "RETURN r, s ORDER BY r.internalTimestamp");
		}

		return null;
	}

	@Override
	public void delete(final boolean deleteRelationships) {

		super.delete(deleteRelationships);

		final SessionTransaction tx = db.getCurrentTransaction();
		tx.deleted(this);
	}

	/**
	 * Evaluate a custom query and return result as a boolean value
	 *
	 * @param customQuery
	 * @param parameters
	 * @return
	 */
	public boolean evaluateCustomQuery(final String customQuery, final Map<String, Object> parameters) {

		final SessionTransaction tx = db.getCurrentTransaction();
		boolean result              = false;

		try {
			result = tx.getBoolean(customQuery, parameters);

		} catch (Exception ignore) {}

		return result;
	}

	public void addToCache(final RelationshipWrapper rel) {

		synchronized (relationshipCache) {

			final Direction direction   = rel.getDirectionForNode(this);
			final RelationshipType type = rel.getType();
			RelationshipResult list = getRelationshipCache(direction, type, null);

			list.add(rel);
		}
	}

	// ----- protected methods -----
	@Override
	protected boolean isNode() {
		return true;
	}

	// ----- private methods -----
	private Map<String, RelationshipResult> getCache(final Direction direction) {

		synchronized (relationshipCache) {

			final String directionKey             = direction != null ? direction.name() : "*";
			Map<String, RelationshipResult> cache = relationshipCache.get(directionKey);

			if (cache == null) {

				cache = new HashMap<>();
				relationshipCache.put(directionKey, cache);
			}

			return cache;
		}
	}

	private RelationshipResult getRelationshipCache(final Direction direction, final RelationshipType relType, final String otherType) {

		synchronized (relationshipCache) {

			final String relTypeKey                     = (relType != null ? relType.name() : "*") + (otherType != null ? (":" + otherType) : "");
			final Map<String, RelationshipResult> cache = getCache(direction);

			RelationshipResult count = cache.get(relTypeKey);
			if (count == null) {

				count = new RelationshipResult();
				cache.put(relTypeKey, count);
			}

			// never return null
			return count;
		}
	}

	// ----- public static methods -----
	public static NodeWrapper newInstance(final BoltDatabaseService db, final org.neo4j.driver.types.Node node) {

		NodeWrapper wrapper;

		synchronized (nodeCache) {

			wrapper = nodeCache.get(node.id());
			if (wrapper == null) { // || wrapper.stale) {

				wrapper = new NodeWrapper(db, node);
				nodeCache.put(node.id(), wrapper);
			}
		}

		return wrapper;
	}

	public static NodeWrapper newInstance(final BoltDatabaseService db, final long id) {

		NodeWrapper wrapper;

		synchronized (nodeCache) {

			wrapper = nodeCache.get(id);
			if (wrapper == null) { // || wrapper.stale) {

				final SessionTransaction tx   = db.getCurrentTransaction();
				final String tenantIdentifier = getTenantIdentifer(db);
				final Map<String, Object> map = new HashMap<>();

				map.put("id", id);

				final org.neo4j.driver.types.Node node = tx.getNode(concat("MATCH (n", tenantIdentifier, ") WHERE ID(n) = $id RETURN DISTINCT n"), map);
				if (node != null) {

					wrapper = NodeWrapper.newInstance(db, node);

					nodeCache.put(id, wrapper);

				} else {

					throw new NotFoundException("Node with ID " + id + " not found.");
				}
			}
		}

		return wrapper;
	}

	// ----- package-private static methods
	static FixedSizeCache<Long, NodeWrapper> getCache() {
		return nodeCache;
	}

	public static void expunge(final Set<Long> toRemove) {

		synchronized (nodeCache) {

			for (final Long id : toRemove) {

				expunge(id);
			}
		}
	}

	public static void expunge(final Long toRemove) {

		synchronized (nodeCache) {

			final NodeWrapper node = nodeCache.remove(toRemove);
			if (node != null) {

				node.clearCaches();
			}
		}
	}

	// ----- protected static methods -----
	protected static void clearCache() {

		synchronized (nodeCache) {

			nodeCache.clear();
		}
	}

	// ----- private static methods -----
	private static String concat(final String... parts) {

		final StringBuilder buf = new StringBuilder();

		for (final String part : parts) {

			// handle nulls gracefully (ignore)
			if (part != null) {

				buf.append(part);
			}
		}

		return buf.toString();
	}

	private static String getTenantIdentifer(final BoltDatabaseService db) {

		final String identifier = db.getTenantIdentifier();

		if (StringUtils.isNotBlank(identifier)) {

			return ":" + identifier;
		}

		return "";
	}

	@Override
	public void removeFromCache() {
		NodeWrapper.expunge(id);
		dontUseCache = true;
	}

	// ----- nested classes -----
	private class RelationshipResult {

		private Set<Relationship> set = null;

		public void add(final Relationship rel) {

			if (set != null) {

				set.add(rel);
			}
		}

		public synchronized Iterable<Relationship> getResult(final BoltDatabaseService db, final long id, final String match, final String returnStatement) {

			final String whereStatement         = " WHERE ID(n) = $id ";
			final String statement              = concat("MATCH ", match, whereStatement, returnStatement);
			final CypherRelationshipIndex index = (CypherRelationshipIndex)db.relationshipIndex();
			final AdvancedCypherQuery query     = new RelationshipQuery(new QueryContext(), index, statement);

			query.getParameters().put("id", id);

			if (Settings.ForceResultStreaming.getValue() || dontUseCache) {

				return index.getResult(query);

			} else {

				// else: return cached result
				if (set == null) {

					// create sorted set (important if nodes are added later on)
					set = new TreeSet<>((o1, o2) -> { return compare("internalTimestamp", o1, o2); });

					// add elements
					set.addAll(Iterables.toList(index.getResult(query)));

					if (query.timeoutViolated()) {
						set = null;
					}
				}

				return set;
			}
		}
	}

	private static class RelationshipQuery extends AdvancedCypherQuery {

		private String statement = null;

		public RelationshipQuery(QueryContext queryContext, AbstractCypherIndex<?> index, final String statement) {

			super(queryContext, index, Integer.MAX_VALUE, 1);

			this.statement = statement;
		}

		@Override
		public String getStatement(final boolean paged) {

			return statement;
		}

		@Override
		public int pageSize() {
			return Integer.MAX_VALUE;
		}
	}
}
