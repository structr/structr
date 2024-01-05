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
package org.structr.memgraph;

import org.structr.api.graph.Direction;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.api.util.FixedSizeCache;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 */
class RelationshipWrapper extends EntityWrapper<org.neo4j.driver.v1.types.Relationship> implements Relationship {

	protected static FixedSizeCache<Long, RelationshipWrapper> relationshipCache = null;

	private long sourceNodeId = -1L;
	private long targetNodeId = -1L;
	private String type       = null;

	protected RelationshipWrapper() {
		// nop constructor for cache access
		super();
	}

	private RelationshipWrapper(final MemgraphDatabaseService db, final org.neo4j.driver.v1.types.Relationship relationship) {

		super(db, relationship);

		this.sourceNodeId = relationship.startNodeId();
		this.targetNodeId = relationship.endNodeId();
		this.type         = relationship.type();
	}

	public static void initialize(final int cacheSize) {
		relationshipCache = new FixedSizeCache<>("Relationship cache", cacheSize);
	}

	@Override
	public String toString() {
		return "R" + getId();
	}

	@Override
	protected String getQueryPrefix() {

		final String tenantIdentifier = db.getTenantIdentifier();
		if (tenantIdentifier != null) {

			return "MATCH (s:" + tenantIdentifier + ")-[n]->(t:" + tenantIdentifier + ")";
		}

		return "MATCH (s)-[n]->(t)";
	}

	@Override
	public void onRemoveFromCache() {
		stale = true;
	}

	public static void expunge(final Set<Long> toRemove) {

		synchronized (relationshipCache) {

			for (final Long id : toRemove) {
				expunge(id);
			}
		}
	}

	public static void expunge(final Long toRemove) {

		synchronized (relationshipCache) {

			final RelationshipWrapper wrapper = relationshipCache.remove(toRemove);
			if (wrapper != null) {

				wrapper.clearCaches();
			}
		}
	}

	@Override
	public void clearCaches() {

		final NodeWrapper startNode = NodeWrapper.getCache().get(sourceNodeId);
		if (startNode != null) {

			startNode.clearCaches();
		}

		final NodeWrapper endNode = NodeWrapper.getCache().get(targetNodeId);
		if (endNode != null) {

			endNode.clearCaches();
		}
	}

	@Override
	public void onClose() {
	}

	@Override
	public Node getStartNode() {

		try {
			return db.getNodeById(sourceNodeId);
		} catch (Throwable t) {}

		return null;
	}

	@Override
	public Node getEndNode() {

		try {
			return db.getNodeById(targetNodeId);
		} catch (Throwable t) {}

		return null;
	}

	@Override
	public Node getOtherNode(final Node node) {

		if (db.unwrap(node.getId()) == sourceNodeId) {
			return getEndNode();
		}

		return getStartNode();
	}

	@Override
	public RelationshipType getType() {
		return db.forName(RelationshipType.class, type);
	}

	@Override
	public void delete(final boolean deleteRelationships) {

		super.delete(deleteRelationships);

		final SessionTransaction tx = db.getCurrentTransaction();
		tx.deleted(this);

		clearCaches();
	}

	public Direction getDirectionForNode(final NodeWrapper node) {

		if (db.unwrap(node.getId()) == sourceNodeId) {
			return Direction.OUTGOING;
		}

		return Direction.INCOMING;
	}

	@Override
	public void removeFromCache() {
		RelationshipWrapper.expunge(id);
	}

	// ----- protected methods -----
	@Override
	protected boolean isNode() {
		return false;
	}

	// ----- public static methods -----
	protected static void clearCache() {
		relationshipCache.clear();
	}

	public static RelationshipWrapper newInstance(final MemgraphDatabaseService db, final org.neo4j.driver.v1.types.Relationship relationship) {

		synchronized (relationshipCache) {

			RelationshipWrapper wrapper = relationshipCache.get(relationship.id());
			if (wrapper == null || wrapper.stale) {

				wrapper = new RelationshipWrapper(db, relationship);
				relationshipCache.put(relationship.id(), wrapper);
			}

			return wrapper;
		}
	}

	public static RelationshipWrapper newInstance(final MemgraphDatabaseService db, final long id) {

		synchronized (relationshipCache) {

			RelationshipWrapper wrapper = relationshipCache.get(id);
			if (wrapper == null || wrapper.stale) {

				final SessionTransaction tx   = db.getCurrentTransaction();
				final Map<String, Object> map = new HashMap<>();
				final StringBuilder buf       = new StringBuilder();
				final String tenantIdentifier = db.getTenantIdentifier();

				map.put("id", id);

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

				wrapper = new RelationshipWrapper(db, tx.getRelationship(buf.toString(), map));

				relationshipCache.put(id, wrapper);
			}

			return wrapper;
		}
	}
}
