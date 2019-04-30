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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicLong;
import org.structr.api.Transaction;

/**
 */
public class SQLTransaction implements Transaction {

	private static final AtomicLong transactionIdCounter = new AtomicLong();
	private SQLDatabaseService db                        = null;
	private long transactionId                           = transactionIdCounter.get();
	private Connection connection                        = null;
	private boolean success                              = false;

	public SQLTransaction(final SQLDatabaseService db, final Connection connection) {

		this.connection = connection;
		this.db         = db;

		try {

			this.connection.setAutoCommit(false);

		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void failure() {
	}

	@Override
	public void success() {
		success = true;
	}

	@Override
	public long getTransactionId() {
		return transactionId;
	}

	@Override
	public void close() {

		try {

			if (success) {

				connection.commit();

			} else {

				connection.rollback();
			}

		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	// ----- package-private methods -----
	NodeResult getNode(final SQLIdentity id) {

		try {

			final PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + id.getType() + " WHERE id = ?");

			statement.setString(1, id.getId());

			if (statement.execute()) {

				try (final ResultSet result = statement.getResultSet()) {

					final NodeResultStreamIterator iterator = new NodeResultStreamIterator(db, result);

					if (iterator.hasNext()) {

						return iterator.next();
					}
				}
			}

		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return null;
	}

	RelationshipResult getRelationship(final SQLIdentity id) {

		return null;
	}

	StringStream getNodeLabels(final SQLIdentity id) {

		try {

			final PreparedStatement statement = connection.prepareStatement("SELECT name FROM Label WHERE nodeType = ? AND nodeId = ?");

			statement.setString(1, id.getType());
			statement.setString(2, id.getId());

			if (statement.execute()) {

				return new StringStream(statement.getResultSet(), 1);
			}

		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return null;
	}

	PreparedStatement prepareStatement(final String sql) throws SQLException {
		return connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
	}

	boolean executeUpdate(final PreparedStatement statement) {

		try {

			return statement.executeUpdate() > 0;

		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return false;
	}

	ResultSet executeQuery(final PreparedStatement statement) {

		try {

			return statement.executeQuery();

		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return null;
	}

	void deleteNode(final SQLIdentity identity, final boolean deleteRelationships) {

		try {

			final String nodeId          = identity.getId();
			final String type            = identity.getType();
			final PreparedStatement stm1 = prepareStatement("DELETE FROM " + type + " WHERE n.id = ?");
			final PreparedStatement stm2 = prepareStatement("DELETE FROM Label WHERE nodeId = ?");

			stm1.setString(1, nodeId);
			stm2.setString(1, nodeId);

			stm1.executeUpdate();
			stm2.executeUpdate();

			/*
			if (deleteRelationships) {

				final PreparedStatement stm3 = prepareStatement("DELETE r, p FROM Relationship r JOIN RelationshipProperty p ON o.relationshipId = r.id WHERE r.source = ? OR r.target = ?");

				stm3.setString(1, nodeId);
				stm3.setString(2, nodeId);

				stm3.executeUpdate();
			}
			*/

		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	void deleteRelationship(final SQLIdentity identity) {

		/*

		try {

			final PreparedStatement stm = prepareStatement("DELETE r, p FROM Relationship r JOIN RelationshipProperty p ON o.relationshipId = r.id WHERE r.source = ? OR r.target = ?");
			final long nodeId           = identity.getId();

			stm.setLong(1, nodeId);
			stm.setLong(2, nodeId);

			stm.executeUpdate();

		} catch (SQLException ex) {
			ex.printStackTrace();
		}
		*/
	}
}
