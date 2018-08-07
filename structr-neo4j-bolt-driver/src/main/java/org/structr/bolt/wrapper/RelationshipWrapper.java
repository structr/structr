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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.api.util.FixedSizeCache;
import org.structr.bolt.BoltDatabaseService;
import org.structr.bolt.SessionTransaction;

/**
 *
 */
public class RelationshipWrapper extends EntityWrapper<org.neo4j.driver.v1.types.Relationship> implements Relationship {

	private static FixedSizeCache<Long, RelationshipWrapper> relationshipCache = null;

	private long sourceNodeId = -1L;
	private long targetNodeId = -1L;
	private String type       = null;

	private RelationshipWrapper(final BoltDatabaseService db, final org.neo4j.driver.v1.types.Relationship relationship) {

		super(db, relationship);

		this.sourceNodeId = relationship.startNodeId();
		this.targetNodeId = relationship.endNodeId();
		this.type         = relationship.type();
	}

	public static void initialize(final int cacheSize) {
		relationshipCache = new FixedSizeCache<>(cacheSize);
	}

	@Override
	protected String getQueryPrefix() {

		final String tenantIdentifier = db.getTenantIdentifier();
		if (tenantIdentifier != null) {

			return "MATCH (:" + tenantIdentifier + ")-[n]->(:" + tenantIdentifier + ")";
		}

		return "MATCH ()-[n]-()";
	}

	@Override
	public void onRemoveFromCache() {

		try {

			final NodeWrapper startNode = (NodeWrapper)getStartNode();
			if (startNode != null) {

				startNode.onRemoveFromCache();
			}

			final NodeWrapper endNode = (NodeWrapper)getEndNode();
			if (endNode != null) {

				endNode.onRemoveFromCache();
			}

		} catch (Throwable t) {}

		stale = true;
	}

	public static void expunge(final Set<Long> toRemove) {

		synchronized (relationshipCache) {

			relationshipCache.removeAll(toRemove);
		}
	}

	@Override
	public void clearCaches() {
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

		if (node.getId() == sourceNodeId) {
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

		final NodeWrapper startNode = (NodeWrapper)getStartNode();
		if (startNode != null) {

			startNode.clearCaches();
		}

		final NodeWrapper endNode = (NodeWrapper)getEndNode();
		if (endNode != null) {

			endNode.clearCaches();
		}

		final SessionTransaction tx = db.getCurrentTransaction();
		tx.deleted(this);
	}

	public static void clearCache() {
		relationshipCache.clear();
	}

	// ----- public static methods -----
	public static RelationshipWrapper newInstance(final BoltDatabaseService db, final org.neo4j.driver.v1.types.Relationship relationship) {

		synchronized (relationshipCache) {

			RelationshipWrapper wrapper = relationshipCache.get(relationship.id());
			if (wrapper == null) {

				wrapper = new RelationshipWrapper(db, relationship);
				relationshipCache.put(relationship.id(), wrapper);
			}

			return wrapper;
		}
	}

	public static RelationshipWrapper newInstance(final BoltDatabaseService db, final long id) {

		synchronized (relationshipCache) {

			RelationshipWrapper wrapper = relationshipCache.get(id);
			if (wrapper == null) {

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

				buf.append(") WHERE ID(n) = {id} RETURN n");

				wrapper = new RelationshipWrapper(db, tx.getRelationship(buf.toString(), map));
				relationshipCache.put(id, wrapper);
			}

			return wrapper;
		}
	}
}
