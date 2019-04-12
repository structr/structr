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
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 */
class NodeResult {

	private Map<String, Object> data = new LinkedHashMap<>();
	private SQLIdentity id           = null;

	public NodeResult(final ResultSet result) throws SQLException {

		this.id = new SQLIdentity(result.getLong("id"));

		read(result);
	}

	SQLIdentity id() {
		return id;
	}

	Map<String, Object> data() {
		return data;
	}

	// ----- private methods -----
	private void read(final ResultSet result) throws SQLException {

		while (result.next()) {

			// The type column contains the column index
			// of the actual value in this property row.
			final String name  = result.getString("name");
			final int type     = result.getInt("type");
			final Object value = result.getObject(type);

			if (name != null && value != null) {

				data.put(name, value);
			}
		}
	}
}
