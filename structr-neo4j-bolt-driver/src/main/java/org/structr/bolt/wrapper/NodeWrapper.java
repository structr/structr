/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.bolt.wrapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.structr.api.NotFoundException;
import org.structr.api.QueryResult;
import org.structr.api.graph.Direction;
import org.structr.api.graph.Label;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.api.util.FixedSizeCache;
import org.structr.api.util.Iterables;
import org.structr.bolt.BoltDatabaseService;
import org.structr.bolt.SessionTransaction;
import org.structr.bolt.mapper.PathRelationshipMapper;

/**
 *
 */
public class NodeWrapper extends EntityWrapper<org.neo4j.driver.v1.types.Node> implements Node {

	private final Map<String, Map<String, Set<Relationship>>> relationshipCache = new HashMap<>();
	private static FixedSizeCache<Long, NodeWrapper> nodeCache                  = null;
	private boolean dontUseCache                                                = false;

	private NodeWrapper(final BoltDatabaseService db, final org.neo4j.driver.v1.types.Node node) {
		super(db, node);
	}

	public static void initialize(final int cacheSize) {
		nodeCache = new FixedSizeCache<>(cacheSize);
	}

	@Override
	protected String getQueryPrefix() {

		final String tenantIdentifier = db.getTenantIdentifier();
		if (tenantIdentifier != null) {

			return "MATCH (n:" + tenantIdentifier + ")";
		}

		return "MATCH (n)";
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

		assertNotStale();

		dontUseCache = true;

		final SessionTransaction tx   = db.getCurrentTransaction();
		final Map<String, Object> map = new HashMap<>();
		final NodeWrapper otherNode   = (NodeWrapper)endNode;
		final String tenantIdentifier = db.getTenantIdentifier();
		final StringBuilder buf       = new StringBuilder();

		map.put("id1", id);
		map.put("id2", endNode.getId());
		map.put("relProperties", properties);

		buf.append("MATCH (n");

		if (tenantIdentifier != null) {

			buf.append(":");
			buf.append(tenantIdentifier);
		}

		buf.append("), (m");

		if (tenantIdentifier != null) {

			buf.append(":");
			buf.append(tenantIdentifier);
		}

		buf.append(") WHERE ID(n) = {id1} AND ID(m) = {id2} ");
		buf.append("MERGE (n)-[r:");
		buf.append(relationshipType.name());
		buf.append("]->(m)");
		buf.append(" SET r += {relProperties} RETURN r");

		final org.neo4j.driver.v1.types.Relationship rel = tx.getRelationship(buf.toString(), map);

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
	public void addLabel(final Label label) {

		assertNotStale();

		final SessionTransaction tx   = db.getCurrentTransaction();
		final Map<String, Object> map = new HashMap<>();
		final String tenantIdentifier = db.getTenantIdentifier();

		map.put("id", id);

		tx.set("MATCH (n" + (tenantIdentifier != null ? ":" + tenantIdentifier : "") + ") WHERE ID(n) = {id} SET n :" + label.name(), map);

		setModified();
	}

	@Override
	public void removeLabel(final Label label) {

		assertNotStale();

		final SessionTransaction tx   = db.getCurrentTransaction();
		final Map<String, Object> map = new HashMap<>();
		final String tenantIdentifier = db.getTenantIdentifier();

		map.put("id", id);

		tx.set("MATCH (n" + (tenantIdentifier != null ? ":" + tenantIdentifier : "") + ") WHERE ID(n) = {id} REMOVE n:" + label.name(), map);
		setModified();
	}

	@Override
	public Iterable<Label> getLabels() {

		assertNotStale();

		final SessionTransaction tx   = db.getCurrentTransaction();
		final Map<String, Object> map = new HashMap<>();
		final List<Label> result      = new LinkedList<>();
		final String tenantIdentifier = db.getTenantIdentifier();

		map.put("id", id);

		// execute query
		for (final String label : tx.getStrings("MATCH (n" + (tenantIdentifier != null ? ":" + tenantIdentifier : "") + ") WHERE ID(n) = {id} RETURN LABELS(n)", map)) {
			result.add(db.forName(Label.class, label));
		}

		return result;
	}

	@Override
	public boolean hasRelationshipTo(final RelationshipType type, final Node targetNode) {

		assertNotStale();

		final SessionTransaction tx      = db.getCurrentTransaction();
		final Map<String, Object> params = new LinkedHashMap<>();
		final String tenantIdentifier    = db.getTenantIdentifier();

		params.put("id1", getId());
		params.put("id2", targetNode.getId());

		try {

			// try to fetch existing relationship by node ID(s)
			// FIXME: this call can be very slow when lots of relationships exist
			tx.getLong(
				"MATCH (n" +
				(tenantIdentifier != null ? ":" + tenantIdentifier : "") +
				")-[r:" + type.name() +
				"]->(m" + (tenantIdentifier != null ? ":" + tenantIdentifier : "") +
				") WHERE id(n) = {id1} AND id(m) = {id2} RETURN id(r)",
				params
			);

			// success
			return true;

		} catch (Throwable t) {

			return false;
		}
	}

	@Override
	public Iterable<Relationship> getRelationships() {

		assertNotStale();

		final PathRelationshipMapper mapper = new PathRelationshipMapper(db);
		final SessionTransaction tx         = db.getCurrentTransaction();
		Set<Relationship> list              = getRelationshipCache(null, null);

		if (list == null || dontUseCache) {

			final Map<String, Object> map = new HashMap<>();
			final String tenantIdentifier = db.getTenantIdentifier();

			map.put("id", id);

			list = toSet(Iterables.map(mapper, tx.getRelationshipsPrefetchable("MATCH (n" + (tenantIdentifier != null ? ":" + tenantIdentifier : "") + ")-[r]-(o) WHERE ID(n) = {id} RETURN DISTINCT r, o", map)));

			// store in cache
			setRelationshipCache(null, null, list);
		}

		return list;
	}

	@Override
	public Iterable<Relationship> getRelationships(final Direction direction) {

		assertNotStale();

		final PathRelationshipMapper mapper = new PathRelationshipMapper(db);
		final SessionTransaction tx         = db.getCurrentTransaction();
		Set<Relationship> list              = getRelationshipCache(direction, null);

		if (list == null || dontUseCache) {

			final Map<String, Object> map = new HashMap<>();
			final String tenantIdentifier = db.getTenantIdentifier();

			map.put("id", id);

			switch (direction) {

				case BOTH:
					return getRelationships();

				case OUTGOING:
					list = toSet(Iterables.map(mapper, tx.getRelationshipsPrefetchable("MATCH (n" + (tenantIdentifier != null ? ":" + tenantIdentifier : "") + ")-[r]->(t) WHERE ID(n) = {id} RETURN DISTINCT r, t", map)));
					break;

				case INCOMING:
					list = toSet(Iterables.map(mapper, tx.getRelationshipsPrefetchable("MATCH (n" + (tenantIdentifier != null ? ":" + tenantIdentifier : "") + ")<-[r]-(s) WHERE ID(n) = {id} RETURN DISTINCT r, s", map)));
					break;
			}

			setRelationshipCache(direction, null, list);

		}

		return list;
	}

	@Override
	public Iterable<Relationship> getRelationships(final Direction direction, final RelationshipType relationshipType) {

		assertNotStale();

		final PathRelationshipMapper mapper = new PathRelationshipMapper(db);
		final SessionTransaction tx         = db.getCurrentTransaction();
		Set<Relationship> list              = getRelationshipCache(direction, relationshipType);

		if (list == null || dontUseCache) {

			final Map<String, Object> map = new HashMap<>();
			final String tenantIdentifier = db.getTenantIdentifier();

			map.put("id", id);

			switch (direction) {

				case BOTH:
					list = toSet(Iterables.map(mapper, tx.getRelationshipsPrefetchable("MATCH (n" + (tenantIdentifier != null ? ":" + tenantIdentifier : "") + ")-[r:" + relationshipType.name() + "]-(o) WHERE ID(n) = {id} RETURN DISTINCT r, o", map)));
					break;

				case OUTGOING:
					list = toSet(Iterables.map(mapper, tx.getRelationshipsPrefetchable("MATCH (n" + (tenantIdentifier != null ? ":" + tenantIdentifier : "") + ")-[r:" + relationshipType.name() + "]->(t) WHERE ID(n) = {id} RETURN DISTINCT r, t", map)));
					break;

				case INCOMING:
					list = toSet(Iterables.map(mapper, tx.getRelationshipsPrefetchable("MATCH (n" + (tenantIdentifier != null ? ":" + tenantIdentifier : "") + ")<-[r:" + relationshipType.name() + "]-(s) WHERE ID(n) = {id} RETURN DISTINCT r, s", map)));
					break;
			}

			setRelationshipCache(direction, relationshipType, list);
		}

		return list;
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

		final Direction direction   = rel.getDirectionForNode(this);
		final RelationshipType type = rel.getType();
		Set<Relationship> list      = getRelationshipCache(direction, type);

		if (list == null) {

			list = new TreeSet<>();
			setRelationshipCache(direction, type, list, true);
		}

		list.add(rel);
	}

	// ----- public static methods -----
	public static FixedSizeCache<Long, NodeWrapper> getCache() {
		return nodeCache;
	}

	public static void expunge(final Set<Long> toRemove) {

		synchronized (nodeCache) {

			nodeCache.removeAll(toRemove);
		}
	}

	public static void clearCache() {

		synchronized (nodeCache) {

			nodeCache.clear();
		}
	}

	public static NodeWrapper newInstance(final BoltDatabaseService db, final org.neo4j.driver.v1.types.Node node) {

		synchronized (nodeCache) {

			NodeWrapper wrapper = nodeCache.get(node.id());
			if (wrapper == null || wrapper.stale) {

				wrapper = new NodeWrapper(db, node);
				nodeCache.put(node.id(), wrapper);
			}

			return wrapper;
		}
	}

	public static NodeWrapper newInstance(final BoltDatabaseService db, final long id) {

		synchronized (nodeCache) {

			NodeWrapper wrapper = nodeCache.get(id);
			if (wrapper == null || wrapper.stale) {

				final SessionTransaction tx   = db.getCurrentTransaction();
				final Map<String, Object> map = new HashMap<>();

				map.put("id", id);

				final QueryResult<org.neo4j.driver.v1.types.Node> result = tx.getNodes("MATCH (n) WHERE ID(n) = {id} RETURN DISTINCT n", map);
				final Iterator<org.neo4j.driver.v1.types.Node> iterator  = result.iterator();

				if (iterator.hasNext()) {

					wrapper = NodeWrapper.newInstance(db, iterator.next());

					nodeCache.put(id, wrapper);

				} else {

					throw new NotFoundException("Node with ID " + id + " not found.");
				}
			}

			return wrapper;
		}
	}

	// ----- protected methods -----
	@Override
	protected boolean isNode() {
		return true;
	}

	// ----- private methods -----
	private Map<String, Set<Relationship>> getCache(final Direction direction) {

		final String directionKey            = direction != null ? direction.name() : "*";
		Map<String, Set<Relationship>> cache = relationshipCache.get(directionKey);

		if (cache == null) {

			cache = new HashMap<>();
			relationshipCache.put(directionKey, cache);
		}

		return cache;
	}

	private Set<Relationship> getRelationshipCache(final Direction direction, final RelationshipType relType) {

		final String relTypeKey                    = relType != null ? relType.name() : "*";
		final Map<String, Set<Relationship>> cache = getCache(direction);

		return cache.get(relTypeKey);
	}

	private void setRelationshipCache(final Direction direction, final RelationshipType relType, final Set<Relationship> list) {
		setRelationshipCache(direction, relType, list, false);
	}

	private void setRelationshipCache(final Direction direction, final RelationshipType relType, final Set<Relationship> list, final boolean force) {

		if (dontUseCache && !force) {
			return;
		}

		final String key                           = relType != null ? relType.name() : "*";
		final Map<String, Set<Relationship>> cache = getCache(direction);

		cache.put(key, list);
	}

	private Set toSet(final Iterable<Relationship> source) {

		final List<Relationship> sorted = Iterables.toList(source);

		final TreeSet<Relationship> set = new TreeSet<>((o1, o2) -> { return compare("internalTimestamp", o1, o2); });
		set.addAll(sorted);
		
		return set;
	}
}
