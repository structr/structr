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

import java.util.Map;
import org.structr.api.graph.Direction;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.api.util.FixedSizeCache;

/**
 */
class SQLNode extends SQLEntity implements Node {

	private static FixedSizeCache<SQLIdentity, SQLNode> nodeCache = null;


	public SQLNode(final SQLDatabaseService db, final PropertySetResult id) {
		super(db, id);
	}

	public SQLNode(final SQLIdentity id) {
		super(id);
	}

	@Override
	public Relationship createRelationshipTo(final Node endNode, final RelationshipType relationshipType) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Relationship createRelationshipTo(final Node endNode, final RelationshipType relationshipType, final Map<String, Object> properties) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void addLabel(final String label) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void removeLabel(final String label) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Iterable<String> getLabels() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean hasRelationshipTo(final RelationshipType relationshipType, final Node targetNode) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Iterable<Relationship> getRelationships() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Iterable<Relationship> getRelationships(final Direction direction) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Iterable<Relationship> getRelationships(final Direction direction, final RelationshipType relationshipType) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	// ----- public static methods -----
	public static void initialize(final int cacheSize) {
		nodeCache = new FixedSizeCache<>(cacheSize);
	}

	public static SQLNode newInstance(final SQLDatabaseService db, final PropertySetResult result) {

		synchronized (nodeCache) {

			final SQLIdentity id = result.id();
			SQLNode wrapper      = nodeCache.get(id);

			if (wrapper == null || wrapper.stale) {

				wrapper = new SQLNode(db, result);
				nodeCache.put(id, wrapper);
			}

			return wrapper;
		}
	}

	public static SQLNode newInstance(final SQLDatabaseService db, final SQLIdentity identity) {

		synchronized (nodeCache) {

			SQLNode wrapper = nodeCache.get(identity);
			if (wrapper == null || wrapper.stale) {

				final SQLTransaction tx = db.getCurrentTransaction();

				wrapper = SQLNode.newInstance(db, tx.getProperties(identity));

				nodeCache.put(identity, wrapper);
			}

			return wrapper;
		}
	}

	@Override
	public boolean isStale() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean isDeleted() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean isSpatialEntity() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
}
