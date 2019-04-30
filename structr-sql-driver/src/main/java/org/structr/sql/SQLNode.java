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

import java.util.LinkedHashMap;
import java.util.Map;
import org.structr.api.NotInTransactionException;
import org.structr.api.graph.Direction;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.api.util.FixedSizeCache;

/**
 */
public class SQLNode extends SQLEntity implements Node {

	private static FixedSizeCache<SQLIdentity, SQLNode> nodeCache = null;
	private boolean isDeleted = false;

	public SQLNode(final SQLDatabaseService db, final SQLIdentity id) {
		this(db, id, null);
	}

	public SQLNode(final SQLDatabaseService db, final SQLIdentity id, final Map<String, Object> data) {
		super(db, id, data);
	}

	@Override
	public String toString() {
		return data.toString();
	}

	@Override
	public Relationship createRelationshipTo(final Node endNode, final RelationshipType relationshipType) {
		return createRelationshipTo(endNode, relationshipType, new LinkedHashMap<>());
	}

	@Override
	public Relationship createRelationshipTo(final Node endNode, final RelationshipType relationshipType, final Map<String, Object> properties) {

		/*

		try {

			final SQLTransaction tx          = db.getCurrentTransaction();
			final PreparedStatement stm      = tx.prepareStatement("INSERT INTO Relationship (source, target, type) values(?, ?, ?)");
			final SQLIdentity targetIdentity = (SQLIdentity)endNode.getId();
			final long sourceId              = id.getId();
			final long targetId              = targetIdentity.getId();

			stm.setLong(1, sourceId);
			stm.setLong(2, targetId);
			stm.setString(3, relationshipType.name());

			final int createNodeResultCount = stm.executeUpdate();
			if (createNodeResultCount == 1) {

				final ResultSet generatedKeys = stm.getGeneratedKeys();
				if (generatedKeys.next()) {

					final PreparedStatement createProperty = tx.prepareStatement(CREATE_REL_PROPERTY_STATEMENT);
					final long newRelationshipId           = generatedKeys.getLong(1);
					final SQLIdentity newId                = SQLIdentity.forId(newRelationshipId);

					for (final Entry<String, Object> entry : properties.entrySet()) {

						SQLEntity.configureStatement(createProperty, newRelationshipId, entry.getKey(), entry.getValue());

						createProperty.executeUpdate();
					}

					//FIXME: this doesnt work: we need (source, target, relType) when creating a new Relationship instance
					return SQLRelationship.newInstance(db, new RelationshipResult(newId, getIdentity(), targetIdentity, relationshipType, properties));
				}
			}

		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		*/

		return null;
	}

	@Override
	public void addLabel(final String label) {

		/*

		try {

			final SQLTransaction tx     = db.getCurrentTransaction();
			final PreparedStatement stm = tx.prepareStatement("INSERT INTO Label (nodeId, name) values(?, ?)");

			stm.setLong(1, id.getId());
			stm.setString(2, label);

			tx.executeUpdate(stm);

		} catch (SQLException ex) {
			ex.printStackTrace();
		}
		*/
	}

	@Override
	public void removeLabel(final String label) {

		/*

		try {

			final SQLTransaction tx     = db.getCurrentTransaction();
			final PreparedStatement stm = tx.prepareStatement("DELETE FROM Label WHERE nodeId = ? AND name = ?");

			stm.setLong(1, id.getId());
			stm.setString(2, label);

			tx.executeUpdate(stm);

		} catch (SQLException ex) {
			ex.printStackTrace();
		}
		*/
	}

	@Override
	public Iterable<String> getLabels() {

		final SQLTransaction tx = db.getCurrentTransaction();

		return tx.getNodeLabels(id);
	}

	public boolean hasLabel(final String label) {

		/*

		try {

			final SQLTransaction tx     = db.getCurrentTransaction();
			final PreparedStatement stm = tx.prepareStatement("SELECT COUNT(*) FROM Label WHERE nodeId = ? AND name = ?");

			stm.setLong(1, id.getId());
			stm.setString(2, label);

			if (stm.execute()) {

				try (final ResultSet result = stm.getResultSet()) {

					if (result.next()) {

						return result.getLong(1) > 0;
					}
				}
			}

		} catch (SQLException ex) {
			ex.printStackTrace();
		}
		*/

		return false;
	}

	@Override
	public boolean hasRelationshipTo(final RelationshipType relationshipType, final Node targetNode) {

		/*

		try {

			final SQLTransaction tx     = db.getCurrentTransaction();
			final PreparedStatement stm = tx.prepareStatement("SELECT COUNT(*) FROM Relationship WHERE type = ? AND source = ? AND target = ?");
			final long idValue          = getIdentity().getId();

			stm.setString(1, relationshipType.name());
			stm.setLong(2, idValue);
			stm.setLong(3, idValue);

			if (stm.execute()) {

				try (final ResultSet result = stm.getResultSet()) {

					if (result.next()) {

						return result.getLong(1) > 0;
					}
				}
			}

		} catch (SQLException ex) {
			ex.printStackTrace();
		}
		*/

		return false;
	}

	@Override
	public Iterable<Relationship> getRelationships() {

		/*

		try {

			final SQLTransaction tx     = db.getCurrentTransaction();
			final PreparedStatement stm = tx.prepareStatement("SELECT id FROM Relationship WHERE source = ? OR target = ?");
			final long idValue          = getIdentity().getId();

			stm.setLong(1, idValue);
			stm.setLong(2, idValue);

			return Iterables.map(r -> SQLRelationship.newInstance(db, r), new IdentityStream(stm.executeQuery()));

		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		*/

		return null;
	}

	@Override
	public Iterable<Relationship> getRelationships(final Direction direction) {

		/*

		final StringBuilder buf = new StringBuilder("SELECT * FROM Relationship WHERE ");

		switch (direction) {

			case OUTGOING:
				buf.append("source = ?");
				break;

			case INCOMING:
				buf.append("target = ?");
				break;

			case BOTH:
				return getRelationships();
		}

		try {

			final SQLTransaction tx     = db.getCurrentTransaction();
			final PreparedStatement stm = tx.prepareStatement(buf.toString());
			final long idValue          = getIdentity().getId();

			stm.setLong(1, idValue);

			return Iterables.map(r -> SQLRelationship.newInstance(db, r), new IdentityStream(stm.executeQuery()));

		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		*/

		return null;
	}

	@Override
	public Iterable<Relationship> getRelationships(final Direction direction, final RelationshipType relationshipType) {

		/*

		final StringBuilder buf = new StringBuilder("SELECT * FROM Relationship WHERE type = ? AND ");

		switch (direction) {

			case OUTGOING:
				buf.append("source = ?");
				break;

			case INCOMING:
				buf.append("target = ?");
				break;

			case BOTH:
				buf.append("source = ? OR target = ?");
				break;
		}

		try {

			final SQLTransaction tx     = db.getCurrentTransaction();
			final PreparedStatement stm = tx.prepareStatement(buf.toString());
			final long idValue          = getIdentity().getId();

			stm.setString(1, relationshipType.name());
			stm.setLong(2, idValue);

			// optionally set third parameter
			if (Direction.BOTH.equals(direction)) {
				stm.setLong(3, idValue);
			}

			return Iterables.map(r -> SQLRelationship.newInstance(db, r), new IdentityStream(stm.executeQuery()));

		} catch (SQLException ex) {
			ex.printStackTrace();
		}
		*/

		return null;
	}

	@Override
	public void delete(final boolean deleteRelationships) throws NotInTransactionException {

		final SQLTransaction tx = db.getCurrentTransaction();

		tx.deleteNode(getIdentity(), deleteRelationships);

		isDeleted = true;
	}

	// ----- public static methods -----
	public static void initialize(final int cacheSize) {
		nodeCache = new FixedSizeCache<>(cacheSize);
	}

	public static SQLNode newInstance(final SQLDatabaseService db, final NodeResult result) {

		synchronized (nodeCache) {

			final SQLIdentity identity = result.id();
			SQLNode wrapper = nodeCache.get(identity);

			if (wrapper == null || wrapper.stale) {

				wrapper = new SQLNode(db, identity, result.data());
				nodeCache.put(identity, wrapper);
			}

			return wrapper;
		}
	}

	public static SQLNode newInstance(final SQLDatabaseService db, final SQLIdentity identity) {

		synchronized (nodeCache) {

			SQLNode wrapper = nodeCache.get(identity);
			if (wrapper == null || wrapper.stale) {

				final SQLTransaction tx = db.getCurrentTransaction();

				wrapper = SQLNode.newInstance(db, tx.getNode(identity));

				nodeCache.put(identity, wrapper);
			}

			return wrapper;
		}
	}

	@Override
	public boolean isStale() {
		return false;
	}

	@Override
	public boolean isDeleted() {
		return isDeleted;
	}
}
