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
import org.structr.api.graph.Direction;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;

import java.util.*;
import java.util.function.Supplier;
import java.util.LinkedList;

import org.structr.api.util.Iterables;

/**
 *
 */
class NodeWrapper extends EntityWrapper<org.neo4j.driver.types.Node> implements Node {

	private final TreeCache<Relationship> relationshipCache;
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
	public Relationship createRelationshipTo(final Node endNode, final RelationshipType relationshipType) {
		return createRelationshipTo(endNode, relationshipType, new LinkedHashMap<>());
	}

	@Override
	public Relationship createRelationshipTo(final Node endNode, final RelationshipType relationshipType, final Map<String, Object> properties) {

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

		final RelationshipWrapper newRel = tx.getRelationshipWrapper(tx.getRelationship(new SimpleCypherQuery(buf, map)));
		final NodeWrapper otherNode      = (NodeWrapper)endNode;
		final String relKey1             = createKey(newRel);
		final String relKey2             = otherNode.createKey(newRel);

		// we can't simply add the new relationship to the cache, because we don't know
		// if the cache was initialized before. If it was not initialized, adding the
		// relationship makes the system think there is only one relationship in the db.

		if (this.prefetched.contains(relKey1)) {

			this.storeRelationship(newRel, false);

		} else {

			this.invalidate();
		}

		if (((NodeWrapper) endNode).prefetched.contains(relKey2)) {

			((NodeWrapper)endNode).storeRelationship(newRel, false);

		} else {

			endNode.invalidate();
		}

		// any modification invalidates the transaction prefetched cache
		db.getCurrentTransaction().prefetchedOutgoing.clear();
		db.getCurrentTransaction().prefetchedIncoming.clear();
		db.getCurrentTransaction().prefetchedQueries.clear();

		return newRel;
	}

	@Override
	public void addLabel(final String label) {

		final SessionTransaction tx   = db.getCurrentTransaction();
		final Map<String, Object> map = new HashMap<>();
		final String tenantIdentifier = getTenantIdentifier(db);

		map.put("id", id);

		tx.getNode(new SimpleCypherQuery(concat("MATCH (n", tenantIdentifier, ") WHERE ID(n) = $id SET n :", label, " RETURN n"), map));
	}

	@Override
	public void removeLabel(final String label) {

		final SessionTransaction tx   = db.getCurrentTransaction();
		final Map<String, Object> map = new HashMap<>();
		final String tenantIdentifier = getTenantIdentifier(db);

		map.put("id", id);

		tx.getNode(new SimpleCypherQuery(concat("MATCH (n", tenantIdentifier, ") WHERE ID(n) = $id REMOVE n:", label, " RETURN n"), map));
	}

	@Override
	public Iterable<String> getLabels() {
		return entity.labels();
	}

	@Override
	public boolean hasRelationshipTo(final RelationshipType type, final Node targetNode) {

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
	public Relationship getRelationshipTo(final RelationshipType type, final Node targetNode) {

		final SessionTransaction tx                 = db.getCurrentTransaction();
		final Map<String, Object> params            = new LinkedHashMap<>();
		final String tenantIdentifier               = getTenantIdentifier(db);

		params.put("id1", id);
		params.put("id2", db.unwrap(targetNode.getId()));

		try {

			return tx.getRelationshipWrapper(tx.getRelationship(new SimpleCypherQuery(concat("MATCH (n", tenantIdentifier, ")-[r:", type.name(), "]->(m", tenantIdentifier, ") WHERE id(n) = $id1 AND id(m) = $id2 RETURN r"), params)));

		} catch (Throwable t) {
			t.printStackTrace();
		}

		return null;
	}

	@Override
	public Iterable<Relationship> getRelationships() {
		return fetchAndCacheRelationships(db, id, concat("(n", getTenantIdentifier(db), ")-[r]-(o)"), "RETURN r, o ORDER BY r.internalTimestamp", "all", null, null);
	}

	@Override
	public Iterable<Relationship> getRelationships(final Direction direction) {

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
	public Iterable<Relationship> getRelationships(final Direction direction, final RelationshipType relationshipType) {

		//final Class clazz             = Direction.OUTGOING.equals(direction) ? relationshipType.getSourceType() : relationshipType.getTargetType();
		//final String type             = clazz != null ? clazz.getSimpleName() : null;
		final String tenantIdentifier = getTenantIdentifier(db);
		final String rel              = relationshipType.name();
		final String key              = createKey(direction, relationshipType);

		switch (direction) {

			case BOTH:
				final String key1 = createKey(Direction.OUTGOING, relationshipType);
				final String key2 = createKey(Direction.INCOMING, relationshipType);
				return Iterables.flatten(
					List.of(
						getRelationshipsFromCache(key1, null, true, () -> fetchAndCacheRelationships(db, id, concat("(n", tenantIdentifier, ")-[r:", rel, "]->(t", tenantIdentifier, ")"), "RETURN r, t ORDER BY r.internalTimestamp", key1, relationshipType, direction)),
						getRelationshipsFromCache(key2, null, false, () -> fetchAndCacheRelationships(db, id, concat("(n", tenantIdentifier, ")<-[r:", rel, "]-(s", tenantIdentifier, ")"), "RETURN r, s ORDER BY r.internalTimestamp", key2, relationshipType, direction))
					)
				);

			case OUTGOING:
				return getRelationshipsFromCache(key, null, true, () -> fetchAndCacheRelationships(db, id, concat("(n", tenantIdentifier, ")-[r:", rel, "]->(s", tenantIdentifier, ")"), "RETURN r, s ORDER BY r.internalTimestamp", key, relationshipType, direction));

			case INCOMING:
				return getRelationshipsFromCache(key, null, false, () -> fetchAndCacheRelationships(db, id, concat("(n", tenantIdentifier, ")<-[r:", rel, "]-(s", tenantIdentifier, ")"), "RETURN r, s ORDER BY r.internalTimestamp", key, relationshipType, direction));
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
	private Iterable<Relationship> fetchAndCacheRelationships(final BoltDatabaseService db, final long id, final String match, final String returnStatement, final String key, final RelationshipType relType, final Direction direction) {

		// fetch relationships
		final String whereStatement         = " WHERE ID(n) = $id ";
		final String statement              = concat("MATCH ", match, whereStatement, returnStatement);

		final CypherRelationshipIndex index = (CypherRelationshipIndex)db.relationshipIndex();
		final SimpleCypherQuery query       = new SimpleCypherQuery(statement);

		if (relType != null) {

			final Class type = null;//Direction.OUTGOING.equals(direction) ? relType.getSourceType() : relType.getTargetType();

			// store relationship infos for statistics
			query.storeRelationshipInfo(type, relType, direction);
		}

		query.getParameters().put("id", id);

		final List<Relationship> list = new LinkedList<>();

		// store rels in cache
		for (final Relationship rel : index.getResult(query)) {

			final String relKey = createKey(rel);

			relationshipCache.insert(relKey, rel);
			list.add(rel);

			prefetched.add(relKey);
		}

		prefetched.add(key);

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

	private Iterable<Relationship> getRelationshipsFromCache(final String key, final String type, final boolean outgoing, final Supplier<Iterable<Relationship>> valueSupplier) {

		final Iterable<Relationship> relationships = relationshipCache.get(key);
		if (relationships == null) {

			if (prefetched.contains(key)) {

				return List.of();
			}

			if (type != null) {

				if (outgoing && db.getCurrentTransaction().prefetchedOutgoing.contains(type + "/" + key)) {

					return List.of();
				}

				if (!outgoing && db.getCurrentTransaction().prefetchedIncoming.contains(type + "/" + key)) {

					return List.of();
				}
			}

			// make query
			return valueSupplier.get();
		}

		return relationships;
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
