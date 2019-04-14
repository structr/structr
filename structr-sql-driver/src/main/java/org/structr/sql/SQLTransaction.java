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
	PropertySetResult getNode(final SQLIdentity id) {

		try {

			final PreparedStatement statement = connection.prepareStatement("SELECT * FROM NodeProperty WHERE nodeId = ?");

			statement.setLong(1, id.getId());

			if (statement.execute()) {

				try (final ResultSet result = statement.getResultSet()) {

					final PropertySetResult node = new PropertySetResult(id);
					while (result.next()) {

						node.visit(result);
					}
				}
			}

		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return null;
	}

	StringStream getNodeLabels(final SQLIdentity id) {

		try {

			final PreparedStatement statement = connection.prepareStatement("SELECT name FROM Label WHERE nodeId = ?");

			statement.setLong(1, id.getId());

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
}
