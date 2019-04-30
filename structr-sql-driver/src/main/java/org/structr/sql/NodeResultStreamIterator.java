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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 */
class NodeResultStreamIterator implements Iterator<NodeResult> {

	private Set<String> columns   = new LinkedHashSet<>();
	private SQLDatabaseService db = null;
	private ResultSet resultSet   = null;
	private boolean nextCalled    = true;
	private boolean hasNext       = false;

	public NodeResultStreamIterator(final SQLDatabaseService db, final ResultSet resultSet) {

		this.resultSet = resultSet;
		this.db        = db;
	}

	@Override
	public boolean hasNext() {

		try {

			if (nextCalled) {

				hasNext    = resultSet.next();
				nextCalled = false;

				if (columns.isEmpty()) {

					final ResultSetMetaData metaData = resultSet.getMetaData();
					final int count                  = metaData.getColumnCount();

					for (int i=1; i <= count; i++) {

						columns.add(metaData.getColumnName(i));
					}
				}
			}

		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return hasNext;
	}

	@Override
	public NodeResult next() {

		hasNext    = false;
		nextCalled = true;

		try {

			return toNodeResult(resultSet);

		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		// return last result
		throw new IllegalStateException("next() called with hasNext() returning false");
	}

	// ----- private methods -----
	private NodeResult toNodeResult(final ResultSet result) throws SQLException {

		final Map<String, Object> data = new LinkedHashMap<>();

		for (final String column : columns) {

			data.put(column, result.getObject(column));
		}

		final String id            = (String)data.get("id");
		final String type          = (String)data.get("type");
		final SQLIdentity identity = SQLIdentity.getInstance(id, type);

		return new NodeResult(identity, data);
	}
}
