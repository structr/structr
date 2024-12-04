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

import org.structr.api.graph.Direction;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;

import java.util.Map;


/**
 *
 */
class RelationshipWrapper extends EntityWrapper<org.neo4j.driver.types.Relationship> implements Relationship {

	private long sourceNodeId = -1L;
	private long targetNodeId = -1L;
	private String type       = null;

	public RelationshipWrapper(final BoltDatabaseService db, final org.neo4j.driver.types.Relationship relationship) {

		super(db, relationship);

		this.sourceNodeId = relationship.startNodeId();
		this.targetNodeId = relationship.endNodeId();
		this.type         = relationship.type();
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
	public Node getStartNode() {

		try {

			return db.getNodeById(sourceNodeId);

		} catch (Throwable t) {
			t.printStackTrace();
		}

		return null;
	}

	@Override
	public Node getEndNode() {

		try {

			return db.getNodeById(targetNodeId);

		} catch (Throwable t) {
			t.printStackTrace();
		}

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

		if (!db.getCurrentTransaction().isNodeDeleted(sourceNodeId)) {
			getStartNode().invalidate();
		}

		if (!db.getCurrentTransaction().isNodeDeleted(targetNodeId)) {
			getEndNode().invalidate();
		}

		super.delete(deleteRelationships);

		final SessionTransaction tx = db.getCurrentTransaction();

		tx.delete(this);

		// invalidate node caches
		tx.nodes.remove(sourceNodeId);
		tx.nodes.remove(targetNodeId);
	}

	@Override
	public Direction getDirectionForNode(final Node node) {

		if (db.unwrap(node.getId()) == sourceNodeId) {
			return Direction.OUTGOING;
		}

		return Direction.INCOMING;
	}

	@Override
	public boolean isNode() {
		return false;
	}
}
