/**
 * Copyright (C) 2010-2019 Structr GmbH
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
package org.structr.sql;

import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.api.util.FixedSizeCache;

/**
 */
class SQLRelationship extends SQLEntity implements Relationship {

	private static FixedSizeCache<SQLIdentity, SQLRelationship> relCache = null;

	private RelationshipType relType = null;
	private SQLIdentity sourceNode   = null;
	private SQLIdentity targetNode   = null;

	public SQLRelationship(final SQLDatabaseService db, final RelationshipResult result) {

		super(db, result.id(), result.data());

		this.relType    = result.getRelType();
		this.sourceNode = result.getSourceNode();
		this.targetNode = result.getTargetNode();
	}

	public SQLRelationship(final SQLIdentity id) {
		super(id);
	}

	public static void initialize(final int cacheSize) {
		relCache = new FixedSizeCache<>(cacheSize);
	}

	@Override
	public String toString() {
		return data.toString();
	}

	@Override
	public boolean isStale() {
		return false;
	}

	@Override
	public boolean isDeleted() {
		return false;
	}

	@Override
	public Node getStartNode() {
		return db.getNodeById(sourceNode);
	}

	@Override
	public Node getEndNode() {
		return db.getNodeById(targetNode);
	}

	@Override
	public Node getOtherNode(final Node node) {

		if (node.getId().equals(sourceNode)) {
			return getEndNode();
		}

		return getStartNode();
	}

	@Override
	public RelationshipType getType() {
		return relType;
	}

	// ----- public static methods -----
	public static SQLRelationship newInstance(final SQLDatabaseService db, final RelationshipResult result) {

		synchronized (relCache) {

			final SQLIdentity id    = result.id();
			SQLRelationship wrapper = relCache.get(id);

			if (wrapper == null || wrapper.stale) {

				wrapper = new SQLRelationship(db, result);
				relCache.put(id, wrapper);
			}

			return wrapper;
		}
	}

	public static SQLRelationship newInstance(final SQLDatabaseService db, final SQLIdentity identity) {

		synchronized (relCache) {

			SQLRelationship wrapper = relCache.get(identity);
			if (wrapper == null || wrapper.stale) {

				final SQLTransaction tx = db.getCurrentTransaction();

				wrapper = SQLRelationship.newInstance(db, tx.getRelationship(identity));

				relCache.put(identity, wrapper);
			}

			return wrapper;
		}
	}
}
