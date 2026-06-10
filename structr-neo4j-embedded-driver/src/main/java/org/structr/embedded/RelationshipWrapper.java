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
package org.structr.embedded;

import org.structr.api.graph.Direction;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;


/**
 *
 */
class RelationshipWrapper extends EntityWrapper<org.neo4j.graphdb.Relationship> implements Relationship<String> {

	private final String sourceNodeId;
	private final String targetNodeId;
	private final String type;

	public RelationshipWrapper(final EmbeddedDatabaseService db, final org.neo4j.graphdb.Relationship relationship) {

		super(db, relationship);

		this.sourceNodeId = relationship.getStartNode().getElementId();
		this.targetNodeId = relationship.getEndNode().getElementId();
		this.type         = relationship.getType().name();
	}

	@Override
	public String toString() {
		return "R" + getId();
	}

	@Override
	protected String getQueryPrefix() {

		final String tenantIdentifier = database.getTenantIdentifier();
		if (tenantIdentifier != null) {

			return "MATCH (s:" + tenantIdentifier + ")-[n]->(t:" + tenantIdentifier + ")";
		}

		return "MATCH (s)-[n]->(t)";
	}

	@Override
	public Node<String> getStartNode() {

		try {

			return database.getNodeById(sourceNodeId);

		} catch (Throwable t) {
			t.printStackTrace();
		}

		return null;
	}

	@Override
	public Node<String> getEndNode() {

		try {

			return database.getNodeById(targetNodeId);

		} catch (Throwable t) {
			t.printStackTrace();
		}

		return null;
	}

	@Override
	public Node<String> getOtherNode(final Node<String> node) {

		if (database.unwrap(node.getId()).equals(sourceNodeId)) {
			return getEndNode();
		}

		return getStartNode();
	}

	@Override
	public RelationshipType getType() {
		return database.getRelationshipType(type);
	}

	@Override
	public void delete(final boolean deleteRelationships) {

		if (!database.getCurrentTransaction().isNodeDeleted(sourceNodeId)) {
			getStartNode().invalidate();
		}

		if (!database.getCurrentTransaction().isNodeDeleted(targetNodeId)) {
			getEndNode().invalidate();
		}

		super.delete(deleteRelationships);

		final EmbeddedTransaction tx = database.getCurrentTransaction();

		tx.delete(this);
	}

	@Override
	public Direction getDirectionForNode(final Node<String> node) {

		if (database.unwrap(node.getId()) == sourceNodeId) {
			return Direction.OUTGOING;
		}

		return Direction.INCOMING;
	}

	@Override
	public boolean isNode() {
		return false;
	}
}
