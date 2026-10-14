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

import org.apache.commons.lang3.StringUtils;
import org.structr.api.graph.Direction;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.api.util.Iterables;

import java.util.*;
import java.util.function.Supplier;

/**
 *
 */
class NodeWrapper extends EntityWrapper<org.neo4j.driver.types.Node> implements Node<Long> {

	private final TreeCache<Relationship<Long>> relationshipCache;
	private Set<String> prefetched = new LinkedHashSet<>();
	private String cachedTenantId = null;

	public NodeWrapper(final BoltDatabaseService db, final org.neo4j.driver.types.Node entity) {

		super(db, entity);

		this.relationshipCache = new TreeCache<>(entity.id(), "/");
	}

	@Override
	public String toString() {

		return "N" + getId();
	}

	@Override
	protected String getQueryPrefix() {

		return concat("MATCH (n", getTenantIdentifier(db), ")");
	}

	@Override
	public Relationship<Long> createRelationshipTo(final Node<Long> endNode, final RelationshipType relationshipType) {

		return createRelationshipTo(endNode, relationshipType, new LinkedHashMap<>());
	}

	@Override
	public Relationship<Long> createRelationshipTo(final Node<Long> endNode, final RelationshipType relationshipType, final Map<String, Object> properties) {

		db.getCurrentTransaction().queryResultCache.clear();

		final SessionTransaction tx   = db.getCurrentTransaction();
		final Map<String, Object> map = new HashMap<>();
		final String tenantIdentifier = getTenantIdentifier(db);
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

		final org.neo4j.driver.types.Relationship newRelationship = tx.getRelationship(new SimpleCypherQuery(buf, map));
		if (newRelationship != null) {

			final RelationshipWrapper newRel = tx.getRelationshipWrapper(newRelationship);
			final NodeWrapper otherNode = (NodeWrapper) endNode;
			final String relKey1 = createKey(newRel);
			final String relKey2 = otherNode.createKey(newRel);

			// we can't simply add the new relationship to the cache, because we don't know
			// if the cache was initialized before. If it was not initialized, adding the
			// relationship makes the system think there is only one relationship in the db.

			if (this.prefetched.contains(relKey1)) {

				this.storeRelationship(newRel, false);

			} else {

				this.invalidate();
			}

			if (((NodeWrapper) endNode).prefetched.contains(relKey2)) {

				((NodeWrapper) endNode).storeRelationship(newRel, false);

			} else {

				endNode.invalidate();
			}

			// any modification invalidates the transaction prefetched cache
			db.getCurrentTransaction().prefetchedOutgoing.clear();
			db.getCurrentTransaction().prefetchedIncoming.clear();
			db.getCurrentTransaction().prefetchedQueries.clear();

			return newRel;
		}

		return null;
	}

	@Override
	public void addLabels(final Set<String> input) {

		// no-op
		if (input.isEmpty()) {

			return;
		}

		db.getCurrentTransaction().queryResultCache.clear();

		final SessionTransaction tx   = db.getCurrentTransaction();
		final Map<String, Object> map = new HashMap<>();
		final String tenantIdentifier = getTenantIdentifier(db);
		final String labels           = StringUtils.join(input, ":");

		map.put("id", id);

		entity = tx.getNode(new SimpleCypherQuery(concat("MATCH (n", tenantIdentifier, ") WHERE ID(n) = $id SET n :", labels, " RETURN n"), map));
	}

	@Override
	public void removeLabel(final String label) {

		db.getCurrentTransaction().queryResultCache.clear();

		final SessionTransaction tx   = db.getCurrentTransaction();
		final Map<String, Object> map = new HashMap<>();
		final String tenantIdentifier = getTenantIdentifier(db);

		map.put("id", id);

		entity = tx.getNode(new SimpleCypherQuery(concat("MATCH (n", tenantIdentifier, ") WHERE ID(n) = $id REMOVE n:", label, " RETURN n"), map));
	}

	@Override
	public Iterable<String> getLabels() {

		return entity.labels();
	}

	@Override
	public boolean hasRelationshipTo(final RelationshipType type, final Node targetNode) {

		final String key = createKey(Direction.OUTGOING, type);
		if (prefetched.contains(key)) {

			return getCachedRelationshipTo(key, db.unwrap(targetNode.getId())) != null;
		}

		final SessionTransaction tx      = db.getCurrentTransaction();
		final Map<String, Object> params = new LinkedHashMap<>();
		final String tenantIdentifier    = getTenantIdentifier(db);

		params.put("id1", id);
		params.put("id2", db.unwrap(targetNode.getId()));

		try {

			// try to fetch existing relationship by node ID(s)

			return tx.getLong(new SimpleCypherQuery(concat("MATCH (n", tenantIdentifier, ")-[r:", type.name(), "]->(m", tenantIdentifier, ") WHERE id(n) = $id1 AND id(m) = $id2 RETURN id(r)"), params)) != null;

		} catch (Throwable t) {

			t.printStackTrace();

			return false;
		}
	}

	@Override
	public Relationship<Long> getRelationshipTo(final RelationshipType type, final Node<Long> targetNode) {

		final String key = createKey(Direction.OUTGOING, type);
		if (prefetched.contains(key)) {

			return getCachedRelationshipTo(key, db.unwrap(targetNode.getId()));
		}

		final SessionTransaction tx                 = db.getCurrentTransaction();
		final Map<String, Object> params            = new LinkedHashMap<>();
		final String tenantIdentifier               = getTenantIdentifier(db);

		params.put("id1", id);
		params.put("id2", db.unwrap(targetNode.getId()));

		try {

			final org.neo4j.driver.types.Relationship relationship = tx.getRelationship(new SimpleCypherQuery(concat("MATCH (n", tenantIdentifier, ")-[r:", type.name(), "]->(m", tenantIdentifier, ") WHERE id(n) = $id1 AND id(m) = $id2 RETURN r"), params));
			if (relationship != null) {

				return tx.getRelationshipWrapper(relationship);
			}

		} catch (Throwable t) {

			t.printStackTrace();
		}

		return null;
	}

	@Override
	public Iterable<Relationship<Long>> getRelationships() {

		return fetchAndCacheRelationships(db, id, concat("(n", getTenantIdentifier(db), ")-[r]-(o)"), "RETURN r, o ORDER BY r.internalTimestamp", "all", null, null);
	}

	@Override
	public Iterable<Relationship<Long>> getRelationships(final Direction direction) {

		final String tenantIdentifier = getTenantIdentifier(db);
		final String key              = createKey(direction, null);

		switch (direction) {

			case BOTH:

				return getRelationships();

			case OUTGOING:

				return getRelationshipsFromCache(key, null, true, () -> fetchAndCacheRelationships(db, id, concat("(n", tenantIdentifier, ")-[r]->(t)"), "RETURN r, t ORDER BY r.internalTimestamp", key, null, direction));

			case INCOMING:

				return getRelationshipsFromCache(key, null, false, () -> fetchAndCacheRelationships(db, id, concat("(n", tenantIdentifier , ")<-[r]-(s)"), "RETURN r, s ORDER BY r.internalTimestamp", key, null, direction));
		}

		return null;
	}

	@Override
	public Iterable<Relationship<Long>> getRelationships(final Direction direction, final RelationshipType relationshipType) {

		final String type             = Direction.OUTGOING.equals(direction) ? relationshipType.getSourceType() : relationshipType.getTargetType();
		final String tenantIdentifier = getTenantIdentifier(db);
		final String rel              = relationshipType.name();
		final String key              = createKey(direction, relationshipType);

		switch (direction) {

			case BOTH:
				final String key1 = createKey(Direction.OUTGOING, relationshipType);
				final String key2 = createKey(Direction.INCOMING, relationshipType);

				// No type: this node can be at either end, so neither the source nor the target type is known to be its
				// type, and a pattern prefetched for one of them says nothing about this node. Each lookup is counted with
				// its own direction, so that what is learned from it is the query it actually runs.
				return Iterables.flatten(
					List.of(
						getRelationshipsFromCache(key1, null, true, () -> fetchAndCacheRelationships(db, id, concat("(n", tenantIdentifier, ")-[r:", rel, "]->(t", tenantIdentifier, ")"), "RETURN r, t ORDER BY r.internalTimestamp", key1, relationshipType, Direction.OUTGOING)),
						getRelationshipsFromCache(key2, null, false, () -> fetchAndCacheRelationships(db, id, concat("(n", tenantIdentifier, ")<-[r:", rel, "]-(s", tenantIdentifier, ")"), "RETURN r, s ORDER BY r.internalTimestamp", key2, relationshipType, Direction.INCOMING))
					)
				);

			case OUTGOING:

				return getRelationshipsFromCache(key, type, true, () -> fetchAndCacheRelationships(db, id, concat("(n", tenantIdentifier, ")-[r:", rel, "]->(s", tenantIdentifier, ")"), "RETURN r, s ORDER BY r.internalTimestamp", key, relationshipType, direction));

			case INCOMING:

				return getRelationshipsFromCache(key, type, false, () -> fetchAndCacheRelationships(db, id, concat("(n", tenantIdentifier, ")<-[r:", rel, "]-(s", tenantIdentifier, ")"), "RETURN r, s ORDER BY r.internalTimestamp", key, relationshipType, direction));
		}

		return null;
	}

	@Override
	public Map<String, Long> getDegree() {

		final SimpleCypherQuery query  = new SimpleCypherQuery("MATCH (n)-[r]-(m) WHERE ID(n) = $id WITH COUNT(r) AS c, r.type AS t return { type: t, count: c }", Map.of("id", this.getId().getId()));
		final Map<String, Long> degree = new LinkedHashMap<>();

		for (final org.neo4j.driver.Record r : db.getCurrentTransaction().collectRecords(query, null)) {

			final Map<String, Object> map = r.get(0).asMap();
			final String type             = (String)map.get("type");
			final long count              = (Long)map.get("count");

			degree.put(type, count);
		}

		return degree;
	}

	@Override
	public void delete(final boolean deleteRelationships) {

		db.getCurrentTransaction().delete(this);

		super.delete(deleteRelationships);
	}

	public void storeRelationship(final RelationshipWrapper rel, final boolean prefetched) {

		final String key = createKey(rel);

		relationshipCache.insert(key, rel);

		if (prefetched) {

			this.prefetched.add(key);
		}
	}

	/**
	 * Removes a deleted relationship from the relationship cache. A key that is marked as prefetched stays complete
	 * without it, so the next lookup of the key needs no query.
	 */
	public void removeRelationship(final RelationshipWrapper rel) {

		relationshipCache.remove(createKey(rel), rel);
	}

	/**
	 * Notifies this NodeWrapper that all keys contained in the keys parameter
	 * can be considered prefetched, so no additional database query needs to
	 * be made in the current transaction.
	 *
	 * @param keys
	 */
	public void storePrefetchInfo(final Set<String> keys) {

		this.prefetched.addAll(keys);
	}

	@Override
	public void invalidate() {

		relationshipCache.clear();
		prefetched.clear();
	}

	@Override
	public boolean isNode() {

		return true;
	}

	// ----- private methods -----
	private Iterable<Relationship<Long>> fetchAndCacheRelationships(final BoltDatabaseService db, final long id, final String match, final String returnStatement, final String key, final RelationshipType relType, final Direction direction) {

		// fetch relationships
		final String whereStatement         = " WHERE ID(n) = $id ";
		final String statement              = concat("MATCH ", match, whereStatement, returnStatement);
		final CypherRelationshipIndex index = (CypherRelationshipIndex)db.relationshipIndex();
		final SimpleCypherQuery query       = new SimpleCypherQuery(statement);

		if (relType != null) {

			final String type = Direction.OUTGOING.equals(direction) ? relType.getSourceType() : relType.getTargetType();

			// store relationship infos for statistics
			query.storeRelationshipInfo(type, relType, direction);
		}

		query.getParameters().put("id", id);

		final List<Relationship<Long>> list = new LinkedList<>();
		final long t0                       = System.nanoTime();

		// store rels in cache
		for (final Relationship<Long> rel : index.getResult(query)) {

			final String relKey = createKey(rel);

			relationshipCache.insert(relKey, rel);
			list.add(rel);

			// The result is complete for the keys in the subtree of the queried key only. A relationship from this node
			// to itself is stored under the OUTGOING key, but an INCOMING query does not return the other outgoing ones.
			if (relKey.equals(key) || relKey.startsWith(key + "/")) {

				prefetched.add(relKey);
			}
		}

		prefetched.add(key);

		// the time of a lookup that a prefetch would save, for the evaluation of prefetches and the prefetch statistics
		if (relType != null) {

			SessionTransaction.lookupExecuted(System.nanoTime() - t0);
		}

		return list;
	}

	private String createKey(final Relationship relationship) {

		return createKey(relationship.getDirectionForNode(this), relationship.getType());
	}

	private String createKey(final Direction direction, final RelationshipType type) {

		final StringBuilder buf = new StringBuilder("all");

		if (direction != null) {

			buf.append("/");
			buf.append(direction.name());
		}

		if (type != null) {

			buf.append("/");
			buf.append(type.name());
		}

		return buf.toString();
	}

	/**
	 * The relationship to the target node among the cached relationships of the key. The key must be marked as
	 * prefetched, so that the cache holds all relationships of this node for it, and no cached relationship to the
	 * target node means that there is none in the database.
	 */
	private Relationship<Long> getCachedRelationshipTo(final String key, final long targetNodeId) {

		final Iterable<Relationship<Long>> relationships = relationshipCache.get(key);
		if (relationships != null) {

			for (final Relationship<Long> relationship : relationships) {

				if (((RelationshipWrapper) relationship).getTargetNodeId() == targetNodeId) {

					return relationship;
				}
			}
		}

		return null;
	}

	/**
	 * Answers a lookup from the relationship cache of this node if the cache holds the complete set for the key.
	 *
	 * The cache is a tree (all/DIRECTION/TYPE), so an entry exists for every prefix of a key that holds data:
	 * after a lookup of all/OUTGOING/CONTAINS, there is an entry for all/OUTGOING as well, which holds the CONTAINS
	 * relationships only. Whether a key is complete is therefore not whether it has an entry, but whether it is
	 * marked as prefetched.
	 */
	private Iterable<Relationship<Long>> getRelationshipsFromCache(final String key, final String type, final boolean outgoing, final Supplier<Iterable<Relationship<Long>>> valueSupplier) {

		final Iterable<Relationship<Long>> relationships = relationshipCache.get(key);

		if (prefetched.contains(key)) {

			countPrefetchHit(key, type, outgoing);

			return relationships != null ? relationships : List.of();
		}

		// a prefetch pattern of this transaction loaded the relationships of all nodes of the type, and this node had none
		if (relationships == null && type != null) {

			final SessionTransaction tx = db.getCurrentTransaction();
			final String typedKey       = type + "/" + key;

			if (outgoing ? tx.prefetchedOutgoing.contains(typedKey) : tx.prefetchedIncoming.contains(typedKey)) {

				countPrefetchHit(key, type, outgoing);

				return List.of();
			}
		}

		return valueSupplier.get();
	}

	/**
	 * Counts a lookup that was served without a query if a prefetch pattern of the current
	 * transaction covers it (the cache can also be filled by a query of this node itself).
	 */
	private void countPrefetchHit(final String key, final String type, final boolean outgoing) {

		if (type != null) {

			final SessionTransaction tx = db.getCurrentTransaction();
			final String typedKey       = type + "/" + key;

			if (outgoing ? tx.prefetchedOutgoing.contains(typedKey) : tx.prefetchedIncoming.contains(typedKey)) {

				tx.countPrefetchHit(typedKey, id);
			}
		}
	}

	private String concat(final String... parts) {

		final StringBuilder buf = new StringBuilder();

		for (final String part : parts) {

			// handle nulls gracefully (ignore)
			if (part != null) {

				buf.append(part);
			}
		}

		return buf.toString();
	}

	private String getTenantIdentifier(final BoltDatabaseService db) {

		if (cachedTenantId == null) {

			final String identifier = db.getTenantIdentifier();
			if (StringUtils.isNotBlank(identifier)) {

				cachedTenantId = ":" + identifier;

			} else {

				cachedTenantId = "";
			}
		}

		return cachedTenantId;
	}
}
