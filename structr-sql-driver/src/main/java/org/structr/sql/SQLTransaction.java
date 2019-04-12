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
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import org.structr.api.Transaction;

/**
 */
public class SQLTransaction implements Transaction {

	private static final AtomicLong transactionIdCounter = new AtomicLong();
	private long transactionId                           = transactionIdCounter.get();
	private Connection connection                        = null;
	private boolean success                              = false;

	public SQLTransaction(final Connection connection) {

		this.connection = connection;

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
	PropertySetResult getProperties(final SQLIdentity id) {

		try {

			final PreparedStatement statement = connection.prepareStatement("SELECT * FROM Property WHERE nodeId = ?");

			statement.setLong(0, id.getId());

			if (statement.execute()) {

				try (final ResultSet result = statement.getResultSet()) {

					return new PropertySetResult(id, result);
				}
			}

		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return null;
	}

	Iterable<PropertySetResult> getNodes(final String query) {

		try {

			final PreparedStatement statement = connection.prepareStatement(query);
			if (statement.execute()) {

				return new ResultSetIterable(statement.getResultSet());
			}

		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return null;
	}

	private class ResultSetIterable implements Iterable<PropertySetResult> {

		private ResultSet resultSet = null;

		public ResultSetIterable(final ResultSet resultSet) {
			this.resultSet = resultSet;
		}

		@Override
		public Iterator<PropertySetResult> iterator() {

			return new Iterator<PropertySetResult>() {

				@Override
				public boolean hasNext() {

					try {
						return resultSet.next();

					} catch (SQLException ex) {
						ex.printStackTrace();
					}

					return false;
				}

				@Override
				public PropertySetResult next() {

					try {
						return new PropertySetResult(resultSet);

					} catch (SQLException ex) {
						ex.printStackTrace();
					}

					return null;
				}
			};
		}
	}
}
