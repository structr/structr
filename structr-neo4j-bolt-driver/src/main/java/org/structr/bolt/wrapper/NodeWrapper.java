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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.structr.api.graph.Direction;
import org.structr.api.graph.Label;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.api.util.FixedSizeCache;
import org.structr.api.util.Iterables;
import org.structr.bolt.BoltDatabaseService;
import org.structr.bolt.SessionTransaction;
import org.structr.bolt.mapper.RelationshipRelationshipMapper;

/**
 *
 */
public class NodeWrapper extends EntityWrapper<org.neo4j.driver.v1.types.Node> implements Node {

	private final Map<String, Map<String, List<Relationship>>> relationshipCache = new HashMap<>();
	private static FixedSizeCache<Long, NodeWrapper> nodeCache                   = null;
	private boolean dontUseCache                                                 = false;

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
		return createRelationshipTo(endNode, relationshipType, Collections.EMPTY_MAP);
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

		tx.modified(this);
		tx.modified(otherNode);

		// clear caches
		((NodeWrapper)endNode).relationshipCache.clear();
		relationshipCache.clear();

		return RelationshipWrapper.newInstance(db, rel);
	}

	@Override
	public void addLabel(final Label label) {

		assertNotStale();

		final SessionTransaction tx   = db.getCurrentTransaction();
		final Map<String, Object> map = new HashMap<>();
		final String tenantIdentifier = db.getTenantIdentifier();

		map.put("id", id);

		tx.set("MATCH (n" + (tenantIdentifier != null ? ":" + tenantIdentifier : "") + ") WHERE ID(n) = {id} SET n :" + label.name(), map);

		tx.modified(this);
	}

	@Override
	public void removeLabel(final Label label) {

		assertNotStale();

		final SessionTransaction tx   = db.getCurrentTransaction();
		final Map<String, Object> map = new HashMap<>();
		final String tenantIdentifier = db.getTenantIdentifier();

		map.put("id", id);

		tx.set("MATCH (n" + (tenantIdentifier != null ? ":" + tenantIdentifier : "") + ") WHERE ID(n) = {id} REMOVE n:" + label.name(), map);
		tx.modified(this);
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

		AssociationList list = (AssociationList)getList(Direction.OUTGOING, type);
		if (list != null && !dontUseCache) {

			return list.containsAssociation(this, type, targetNode);

		} else {

			list = (AssociationList)getList(Direction.INCOMING, type);
			if (list != null && !dontUseCache) {

				return list.containsAssociation(targetNode, type, this);

			} else {

				final SessionTransaction tx      = db.getCurrentTransaction();
				final Map<String, Object> params = new LinkedHashMap<>();
				final String tenantIdentifier    = db.getTenantIdentifier();

				params.put("id1", getId());
				params.put("id2", targetNode.getId());

				try {

					// try to fetch existing relationship by node ID(s)
					// FIXME: this call can be very slow when lots of relationships exist
					tx.getLong("MATCH (n" + (tenantIdentifier != null ? ":" + tenantIdentifier : "") + ")-[r:" + type.name() + "]->(m) WHERE id(n) = {id1} AND id(m) = {id2} RETURN id(r)", params);

					// success
					return true;

				} catch (Throwable t) {

					return false;
				}
			}
		}
	}

	@Override
	public Iterable<Relationship> getRelationships() {

		assertNotStale();

		final RelationshipRelationshipMapper mapper = new RelationshipRelationshipMapper(db);
		List<Relationship> list                     = getList(null, null);

		if (list == null || dontUseCache) {

			final SessionTransaction tx   = db.getCurrentTransaction();
			final Map<String, Object> map = new HashMap<>();
			final String tenantIdentifier = db.getTenantIdentifier();

			map.put("id", id);

			list = toList(Iterables.map(mapper, tx.getRelationships("MATCH (n" + (tenantIdentifier != null ? ":" + tenantIdentifier : "") + ")-[r]->() WHERE ID(n) = {id} RETURN r", map)));

			// store in cache
			setList(null, null, list);
		}

		return list;
	}

	@Override
	public Iterable<Relationship> getRelationships(final Direction direction) {

		assertNotStale();

		final RelationshipRelationshipMapper mapper = new RelationshipRelationshipMapper(db);
		List<Relationship> list                     = getList(direction, null);

		if (list == null || dontUseCache) {

			final SessionTransaction tx   = db.getCurrentTransaction();
			final Map<String, Object> map = new HashMap<>();
			final String tenantIdentifier = db.getTenantIdentifier();

			map.put("id", id);

			switch (direction) {

				case BOTH:
					return getRelationships();

				case OUTGOING:
					list = toList(Iterables.map(mapper, tx.getRelationships("MATCH (n" + (tenantIdentifier != null ? ":" + tenantIdentifier : "") + ")-[r]->() WHERE ID(n) = {id} RETURN r", map)));
					break;

				case INCOMING:
					list = toList(Iterables.map(mapper, tx.getRelationships("MATCH (n" + (tenantIdentifier != null ? ":" + tenantIdentifier : "") + ")<-[r]-() WHERE ID(n) = {id} RETURN r", map)));
					break;
			}

			setList(direction, null, list);

		}

		return list;
	}

	@Override
	public Iterable<Relationship> getRelationships(final Direction direction, final RelationshipType relationshipType) {

		assertNotStale();

		final RelationshipRelationshipMapper mapper = new RelationshipRelationshipMapper(db);
		List<Relationship> list                     = getList(direction, relationshipType);

		if (list == null || dontUseCache) {

			final SessionTransaction tx   = db.getCurrentTransaction();
			final Map<String, Object> map = new HashMap<>();
			final String tenantIdentifier = db.getTenantIdentifier();

			map.put("id", id);

			switch (direction) {

				case BOTH:
					list = toList(Iterables.map(mapper, tx.getRelationships("MATCH (n" + (tenantIdentifier != null ? ":" + tenantIdentifier : "") + ")-[r:" + relationshipType.name() + "]-() WHERE ID(n) = {id} RETURN r", map)));
					break;

				case OUTGOING:
					list = toList(Iterables.map(mapper, tx.getRelationships("MATCH (n" + (tenantIdentifier != null ? ":" + tenantIdentifier : "") + ")-[r:" + relationshipType.name() + "]->() WHERE ID(n) = {id} RETURN r", map)));
					break;

				case INCOMING:
					list = toList(Iterables.map(mapper, tx.getRelationships("MATCH (n" + (tenantIdentifier != null ? ":" + tenantIdentifier : "") + ")<-[r:" + relationshipType.name() + "]-() WHERE ID(n) = {id} RETURN r", map)));
					break;
			}

			setList(direction, relationshipType, list);
		}

		return list;
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
		boolean result = false;
		try {
			result = tx.getBoolean(customQuery, parameters);
		} catch (Exception ignore) {}

		return result;
	}

	@Override
	public void delete() {

		super.delete();
		nodeCache.remove(id);
	}

	public static void clearCache() {
		nodeCache.clear();
	}

	// ----- public static methods -----
	public static NodeWrapper newInstance(final BoltDatabaseService db, final org.neo4j.driver.v1.types.Node node) {

		synchronized (nodeCache) {

			NodeWrapper wrapper = nodeCache.get(node.id());
			if (wrapper == null) {

				wrapper = new NodeWrapper(db, node);
				nodeCache.put(node.id(), wrapper);
			}

			return wrapper;
		}
	}

	public static NodeWrapper newInstance(final BoltDatabaseService db, final long id) {

		synchronized (nodeCache) {

			NodeWrapper wrapper = nodeCache.get(id);
			if (wrapper == null) {

				final SessionTransaction tx   = db.getCurrentTransaction();
				final Map<String, Object> map = new HashMap<>();
				final String tenantIdentifier = db.getTenantIdentifier();

				map.put("id", id);

				wrapper = new NodeWrapper(db, tx.getNode("MATCH (n" + (tenantIdentifier != null ? ":" + tenantIdentifier : "") + ") WHERE ID(n) = {id} RETURN n", map));
				nodeCache.put(id, wrapper);
			}

			return wrapper;
		}
	}

	// ----- private methods -----
	private Map<String, List<Relationship>> getCache(final Direction direction) {

		final String key                      = direction != null ? direction.name() : "*";
		Map<String, List<Relationship>> cache = relationshipCache.get(key);

		if (cache == null) {

			cache = new HashMap<>();
			relationshipCache.put(key, cache);
		}

		return cache;
	}

	private List<Relationship> getList(final Direction direction, final RelationshipType relType) {

		final String key                            = relType != null ? relType.name() : "*";
		final Map<String, List<Relationship>> cache = getCache(direction);

		return cache.get(key);
	}

	private void setList(final Direction direction, final RelationshipType relType, final List<Relationship> list) {

		if (dontUseCache) {
			return;
		}

		final String key                            = relType != null ? relType.name() : "*";
		final Map<String, List<Relationship>> cache = getCache(direction);

		cache.put(key, list);
	}

	private AssociationList toList(final Iterable<Relationship> source) {

		final AssociationList list = new AssociationList();

		Iterables.addAll(list, source);

		return list;
	}

	// ----- nested classes -----
	private class AssociationList extends LinkedList<Relationship> {

		final Set<String> associations = new HashSet<>();

		@Override
		public boolean add(final Relationship toAdd) {

			associations.add(cacheKey(toAdd.getStartNode(), toAdd.getType(), toAdd.getEndNode()));

			return super.add(toAdd);
		}

		public boolean containsAssociation(final Node sourceNode, final RelationshipType relType, final Node targetNode) {
			return associations.contains(cacheKey(sourceNode, relType, targetNode));
		}

		// ----- private methods -----
		public String cacheKey(final Node sourceNode, final RelationshipType relType, final Node targetNode) {

			final StringBuilder buf = new StringBuilder();

			if (sourceNode != null && targetNode != null) {

				buf.append(sourceNode.getId());
				buf.append("-");
				buf.append(relType);
				buf.append("-");
				buf.append(targetNode.getId());
			}

			return buf.toString();
		}
	}
}
