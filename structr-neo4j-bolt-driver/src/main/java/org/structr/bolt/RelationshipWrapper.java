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

import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.api.util.FixedSizeCache;

/**
 *
 * @author Christian Morgner
 */
public class RelationshipWrapper extends EntityWrapper<org.neo4j.driver.v1.types.Relationship> implements Relationship {

	private RelationshipWrapper(final BoltDatabaseService db, final org.neo4j.driver.v1.types.Relationship relationship) {
		super(db, relationship);
	}

	@Override
	protected String getQueryPrefix() {
		return "MATCH ()-[n]-()";
	}

	@Override
	public Node getStartNode() {
		return db.getNodeById(entity.startNodeId());
	}

	@Override
	public Node getEndNode() {
		return db.getNodeById(entity.endNodeId());
	}

	@Override
	public Node getOtherNode(Node node) {

		if (node.getId() == entity.startNodeId()) {
			return getEndNode();
		}

		return getStartNode();
	}

	@Override
	public RelationshipType getType() {
		return db.forName(RelationshipType.class, entity.type());
	}

	public static void shutdownCache() {
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

	private static final FixedSizeCache<Long, RelationshipWrapper> relationshipCache = new FixedSizeCache<>(10000);
}
