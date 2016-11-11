/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

	private NodeWrapper(final BoltDatabaseService db, final org.neo4j.driver.v1.types.Node node) {
		super(db, node);
	}

	public static void initialize(final int cacheSize) {
		nodeCache = new FixedSizeCache<>(cacheSize);
	}

	@Override
	protected String getQueryPrefix() {
		return "MATCH (n)";
	}

	@Override
	public void invalidate() {
		relationshipCache.clear();
	}

	@Override
	public Relationship createRelationshipTo(final Node endNode, final RelationshipType relationshipType) {

		assertNotStale();

		final SessionTransaction tx   = db.getCurrentTransaction();
		final Map<String, Object> map = new HashMap<>();
		final NodeWrapper otherNode   = (NodeWrapper)endNode;

		tx.modified(this);
		tx.modified(otherNode);

		assertNotStale();

		map.put("id1", id);
		map.put("id2", endNode.getId());

		/**
		 * Neo4j does not seem to lock source and target node when
		 * creating a relationship between the two, so we need to set
		 * a temporary property to enforce locking on the two nodes
		 * for the duration of the transaction.
		 */

		final org.neo4j.driver.v1.types.Relationship rel = tx.getRelationship(
			"MATCH (n), (m) WHERE ID(n) = {id1} AND ID(m) = {id2} "
				+ "SET n.locked = true, m.locked = true "
				+ "MERGE (n)-[r:" + relationshipType.name() + "]->(m) "
				+ "SET n.locked = Null, m.locked = Null RETURN r",
			map);

		tx.modified(this);

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

		map.put("id", id);

		tx.set("MATCH (n) WHERE ID(n) = {id} SET n :" + label.name(), map);
		tx.modified(this);
	}

	@Override
	public void removeLabel(final Label label) {

		assertNotStale();

		final SessionTransaction tx   = db.getCurrentTransaction();
		final Map<String, Object> map = new HashMap<>();

		map.put("id", id);

		tx.set("MATCH (n) WHERE ID(n) = {id} REMOVE n:" + label.name(), map);
		tx.modified(this);
	}

	@Override
	public Iterable<Label> getLabels() {

		assertNotStale();

		final SessionTransaction tx   = db.getCurrentTransaction();
		final Map<String, Object> map = new HashMap<>();
		final List<Label> result      = new LinkedList<>();

		map.put("id", id);

		// execute query
		for (final String label : tx.getStrings("MATCH (n) WHERE ID(n) = {id} RETURN LABELS(n)", map)) {
			result.add(db.forName(Label.class, label));
		}

		return result;
	}

	@Override
	public Iterable<Relationship> getRelationships() {

		assertNotStale();

		final RelationshipRelationshipMapper mapper = new RelationshipRelationshipMapper(db);
		List<Relationship> list                     = getList(null, null);

		if (list == null) {

			final SessionTransaction tx                 = db.getCurrentTransaction();
			final Map<String, Object> map               = new HashMap<>();

			map.put("id", id);

			list = Iterables.toList(Iterables.map(mapper, tx.getRelationships("MATCH (n)-[r]-() WHERE ID(n) = {id} RETURN r", map)));

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

		if (list == null) {

			final SessionTransaction tx   = db.getCurrentTransaction();
			final Map<String, Object> map = new HashMap<>();

			map.put("id", id);

			switch (direction) {

				case BOTH:
					return getRelationships();

				case OUTGOING:
					list = Iterables.toList(Iterables.map(mapper, tx.getRelationships("MATCH (n)-[r]->() WHERE ID(n) = {id} RETURN r", map)));
					break;

				case INCOMING:
					list = Iterables.toList(Iterables.map(mapper, tx.getRelationships("MATCH (n)<-[r]-() WHERE ID(n) = {id} RETURN r", map)));
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

		if (list == null) {

			final SessionTransaction tx   = db.getCurrentTransaction();
			final Map<String, Object> map = new HashMap<>();

			map.put("id", id);

			switch (direction) {

				case BOTH:
					list = Iterables.toList(Iterables.map(mapper, tx.getRelationships("MATCH (n)-[r:" + relationshipType.name() + "]-() WHERE ID(n) = {id} RETURN r", map)));
					break;

				case OUTGOING:
					list = Iterables.toList(Iterables.map(mapper, tx.getRelationships("MATCH (n)-[r:" + relationshipType.name() + "]->() WHERE ID(n) = {id} RETURN r", map)));
					break;

				case INCOMING:
					list = Iterables.toList(Iterables.map(mapper, tx.getRelationships("MATCH (n)<-[r:" + relationshipType.name() + "]-() WHERE ID(n) = {id} RETURN r", map)));
					break;
			}

			setList(direction, relationshipType, list);
		}

		return list;
	}

	@Override
	public void delete() {

		super.delete();
		nodeCache.remove(id);
	}

	public static void shutdownCache() {
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

				map.put("id", id);

				wrapper = new NodeWrapper(db, tx.getNode("MATCH (n) WHERE ID(n) = {id} RETURN n", map));
				nodeCache.put(id, wrapper);
			}

			return wrapper;
		}
	}

	// ----- private methods -----
	private Map<String, List<Relationship>> getCache(final Direction direction) {

		final String key              = direction != null ? direction.name() : "*";
		Map<String, List<Relationship>> cache = relationshipCache.get(key);

		if (cache == null) {

			cache = new HashMap<>();
			relationshipCache.put(key, cache);
		}

		return cache;
	}

	private List<Relationship> getList(final Direction direction, final RelationshipType relType) {

		final String key                    = relType != null ? relType.name() : "*";
		final Map<String, List<Relationship>> cache = getCache(direction);

		return cache.get(key);
	}

	private void setList(final Direction direction, final RelationshipType relType, final List<Relationship> list) {

		final String key                    = relType != null ? relType.name() : "*";
		final Map<String, List<Relationship>> cache = getCache(direction);

		cache.put(key, list);
	}
}
