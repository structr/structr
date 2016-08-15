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
package org.structr.bolt;

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

/**
 *
 * @author Christian Morgner
 */
public class NodeWrapper extends EntityWrapper<org.neo4j.driver.v1.types.Node> implements Node {

	private NodeWrapper(final BoltDatabaseService db, final org.neo4j.driver.v1.types.Node node) {
		super(db, node);
	}

	@Override
	protected String getQueryPrefix() {
		return "MATCH (n)";
	}

	@Override
	public Relationship createRelationshipTo(final Node endNode, final RelationshipType relationshipType) {

		assertNotStale();

		final SessionTransaction tx   = db.getCurrentTransaction();
		final Map<String, Object> map = new HashMap<>();

		map.put("id1", entity.id());
		map.put("id2", endNode.getId());

		final org.neo4j.driver.v1.types.Relationship rel = tx.getRelationship("MATCH (n), (m) WHERE ID(n) = {id1} AND ID(m) = {id2} CREATE (n)-[r:" + relationshipType.name() + "]->(m) RETURN r", map);

		tx.modified(this);

		return RelationshipWrapper.newInstance(db, rel);
	}

	@Override
	public void addLabel(final Label label) {

		assertNotStale();

		final SessionTransaction tx   = db.getCurrentTransaction();
		final Map<String, Object> map = new HashMap<>();

		map.put("id", entity.id());

		entity = tx.getNode("MATCH (n) WHERE ID(n) = {id} SET n :" + label.name() + " RETURN n", map);
		tx.modified(this);
	}

	@Override
	public void removeLabel(Label label) {

		assertNotStale();

		final SessionTransaction tx   = db.getCurrentTransaction();
		final Map<String, Object> map = new HashMap<>();

		map.put("id", entity.id());

		entity = tx.getNode("MATCH (n) WHERE ID(n) = {id} REMOVE n:" + label.name() + " RETURN n", map);
		tx.modified(this);
	}

	@Override
	public Iterable<Label> getLabels() {

		assertNotStale();

		final List<Label> result = new LinkedList<>();

		// execute query
		for (final String label : entity.labels()) {
			result.add(db.forName(Label.class, label));
		}

		return result;
	}

	@Override
	public Iterable<Relationship> getRelationships() {

		assertNotStale();

		final SessionTransaction tx     = db.getCurrentTransaction();
		final Map<String, Object> map   = new HashMap<>();
		final List<Relationship> result = new LinkedList<>();

		map.put("id", entity.id());

		for (final org.neo4j.driver.v1.types.Relationship rel : tx.getRelationshipList("MATCH (n)-[r]-(m) WHERE ID(n) = {id} RETURN r", map)) {

			result.add(RelationshipWrapper.newInstance(db, rel));
		}

		return result;
	}

	@Override
	public Iterable<Relationship> getRelationships(Direction direction) {

		assertNotStale();

		final SessionTransaction tx     = db.getCurrentTransaction();
		final Map<String, Object> map   = new HashMap<>();
		final List<Relationship> result = new LinkedList<>();

		map.put("id", entity.id());

		switch (direction) {

			case BOTH:
				return getRelationships();

			case OUTGOING:
				for (final org.neo4j.driver.v1.types.Relationship rel : tx.getRelationshipList("MATCH (n)-[r]->(m) WHERE ID(n) = {id} RETURN r", map)) {
					result.add(RelationshipWrapper.newInstance(db, rel));
				}
				break;

			case INCOMING:
				for (final org.neo4j.driver.v1.types.Relationship rel : tx.getRelationshipList("MATCH (n)<-[r]-(m) WHERE ID(n) = {id} RETURN r", map)) {
					result.add(RelationshipWrapper.newInstance(db, rel));
				}
				break;
		}

		return result;
	}

	@Override
	public Iterable<Relationship> getRelationships(Direction direction, RelationshipType relationshipType) {

		assertNotStale();

		final SessionTransaction tx     = db.getCurrentTransaction();
		final Map<String, Object> map   = new HashMap<>();
		final List<Relationship> result = new LinkedList<>();

		map.put("id", entity.id());

		switch (direction) {

			case BOTH:
				return getRelationships();

			case OUTGOING:
				for (final org.neo4j.driver.v1.types.Relationship rel : tx.getRelationshipList("MATCH (n)-[r:" + relationshipType.name() + "]->(m) WHERE ID(n) = {id} RETURN r", map)) {
					result.add(RelationshipWrapper.newInstance(db, rel));
				}
				break;

			case INCOMING:
				for (final org.neo4j.driver.v1.types.Relationship rel : tx.getRelationshipList("MATCH (n)<-[r:" + relationshipType.name() + "]-(m) WHERE ID(n) = {id} RETURN r", map)) {
					result.add(RelationshipWrapper.newInstance(db, rel));
				}
				break;
		}

		return result;
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

	private static final FixedSizeCache<Long, NodeWrapper> nodeCache = new FixedSizeCache<>(10000);
}
